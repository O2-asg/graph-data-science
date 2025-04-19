/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gds.embeddings.node2vec;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.TestProgressTracker;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.collections.ha.HugeLongArray;
import org.neo4j.gds.collections.ha.HugeObjectArray;
import org.neo4j.gds.collections.hsa.HugeSparseLongArray;
import org.neo4j.gds.compat.TestLog;
import org.neo4j.gds.core.concurrency.Concurrency;
import org.neo4j.gds.core.concurrency.DefaultPool;
import org.neo4j.gds.core.loading.ArrayIdMap;
import org.neo4j.gds.core.loading.LabelInformationBuilders;
import org.neo4j.gds.core.loading.construction.GraphFactory;
import org.neo4j.gds.core.loading.construction.RelationshipsBuilder;
import org.neo4j.gds.core.utils.Intersections;
import org.neo4j.gds.core.utils.progress.EmptyTaskRegistryFactory;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.core.utils.shuffle.ShuffleUtil;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.gdl.GdlFactory;
import org.neo4j.gds.logging.GdsTestLog;
import org.neo4j.gds.ml.core.tensor.FloatVector;
import org.neo4j.gds.termination.TerminationFlag;

import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.neo4j.gds.assertj.Extractors.removingThreadId;

@ExtendWith(SoftAssertionsExtension.class)
@GdlExtension
class Node2VecTest {

    private static final List<Long> NO_SOURCE_NODES = List.of();
    private static final Optional<Long> NO_RANDOM_SEED = Optional.empty();

    @GdlGraph
    private static final String DB_CYPHER =
        "CREATE" +
        "  (a:Node1)" +
        ", (b:Node1)" +
        ", (c:Node2)" +
        ", (d:Isolated)" +
        ", (e:Isolated)" +
        ", (a)-[:REL {prop: 1.0}]->(b)" +
        ", (b)-[:REL {prop: 1.0}]->(a)" +
        ", (a)-[:REL {prop: 1.0}]->(c)" +
        ", (c)-[:REL {prop: 1.0}]->(a)" +
        ", (b)-[:REL {prop: 1.0}]->(c)" +
        ", (c)-[:REL {prop: 1.0}]->(b)";

    @Inject
    private Graph graph;

    @Inject
    private GraphStore graphStore;

    @ParameterizedTest(name = "{0}")
    @MethodSource("graphs")
    void embeddingsShouldHaveTheConfiguredDimension(String msg, List<NodeLabel> nodeLabels) {

        var currentGraph = graphStore.getGraph(nodeLabels);
        int embeddingDimension = 128;

        var trainParameters = new TrainParameters(
            0.025,
            0.0001,
            1,
            10,
            5,
            embeddingDimension,
            EmbeddingInitializer.NORMALIZED
        );

        HugeObjectArray<FloatVector> node2Vec = new Node2Vec(
            currentGraph,
            new Concurrency(4),
            NO_SOURCE_NODES,
            NO_RANDOM_SEED,
            1000,
            new Node2VecParameters(
                new SamplingWalkParameters(10, 80, 1.0, 1.0, 0.001, 0.75),
                trainParameters
            ),
            ProgressTracker.NULL_TRACKER,
            TerminationFlag.RUNNING_TRUE
        ).compute().embeddings();

        currentGraph.forEachNode(node -> {
                assertEquals(embeddingDimension, node2Vec.get(node).data().length);
                return true;
            }
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
        "true,4",
        "false,3"
    })
    void shouldLogProgress(boolean relationshipWeights, int expectedProgresses) {
        Graph currentGraph;
        if (relationshipWeights) {
            currentGraph = graph;
        } else {
            currentGraph = graphStore.getGraph(RelationshipType.of("REL"), Optional.empty());
        }

        int embeddingDimension = 128;
        Node2VecStreamConfig config = Node2VecStreamConfigImpl
            .builder()
            .embeddingDimension(embeddingDimension)
            .build();
        var progressTask = new Node2VecAlgorithmFactory<>().progressTask(currentGraph, config);

        var walkParameters = new SamplingWalkParameters(10, 80, 1.0, 1.0, 0.001, 0.75);
        var trainParameters = new TrainParameters(
            0.025,
            0.0001,
            1,
            10,
            5,
            embeddingDimension,
            EmbeddingInitializer.NORMALIZED
        );
        var concurrency = new Concurrency(4);
        var log = new GdsTestLog();
        var progressTracker = new TestProgressTracker(progressTask, log, concurrency, EmptyTaskRegistryFactory.INSTANCE);
        new Node2Vec(
            currentGraph,
            concurrency,
            NO_SOURCE_NODES,
            NO_RANDOM_SEED,
            1000,
            new Node2VecParameters(walkParameters, trainParameters),
            progressTracker,
            TerminationFlag.RUNNING_TRUE
        ).compute();

        assertThat(log.getMessages(TestLog.INFO))
            .extracting(removingThreadId())
            .contains(
                "Node2Vec :: Start",
                "Node2Vec :: RandomWalk :: Start",
                "Node2Vec :: RandomWalk :: create walks :: Start",
                "Node2Vec :: RandomWalk :: create walks 100%",
                "Node2Vec :: RandomWalk :: create walks :: Finished",
                "Node2Vec :: RandomWalk :: Finished",
                "Node2Vec :: train :: Start",
                "Node2Vec :: train :: iteration 1 of 1 :: Start",
                "Node2Vec :: train :: iteration 1 of 1 100%",
                "Node2Vec :: train :: iteration 1 of 1 :: Finished",
                "Node2Vec :: train :: Finished",
                "Node2Vec :: Finished"
            );

        if (relationshipWeights) {
            assertThat(log.getMessages(TestLog.INFO))
                .extracting(removingThreadId())
                .contains(
                    "Node2Vec :: RandomWalk :: DegreeCentrality :: Start",
                    "Node2Vec :: RandomWalk :: DegreeCentrality :: Finished"
                );
        }
    }

    @Test
    void failOnNegativeWeights() {
        var negativeGraph = GdlFactory.of("CREATE (a)-[:REL {weight: -1}]->(b)").build().getUnion();

        var walkParameters = new SamplingWalkParameters(10, 80, 1.0, 1.0, 0.001, 0.75);
        var trainParameters = new TrainParameters(0.025, 0.0001, 1, 1, 1, 128, EmbeddingInitializer.NORMALIZED);

        var node2Vec = new Node2Vec(
            negativeGraph,
            new Concurrency(4),
            NO_SOURCE_NODES,
            NO_RANDOM_SEED,
            1000,
            new Node2VecParameters(walkParameters, trainParameters),
            ProgressTracker.NULL_TRACKER,
            TerminationFlag.RUNNING_TRUE
        );

        assertThatThrownBy(node2Vec::compute)
            .isInstanceOf(RuntimeException.class)
            .hasMessage(
                "Found an invalid relationship weight between nodes `0` and `1` with the property value of `-1.000000`." +
                " Node2Vec only supports non-negative weights.");

    }

    @Disabled("The order of the randomWalks + its usage in the training is not deterministic yet.")
    @Test
    void randomSeed(SoftAssertions softly) {


        int embeddingDimension = 2;
        var walkParameters = new SamplingWalkParameters(1, 20, 1.0, 1.0,  0.001, 0.75);
        var trainParameters = new TrainParameters(0.025, 0.0001, 1, 1, 1, embeddingDimension, EmbeddingInitializer.NORMALIZED);

        var embeddings = new Node2Vec(
            graph,
            new Concurrency(4),
            NO_SOURCE_NODES,
            Optional.of(1337L),
            1000,
            new Node2VecParameters(walkParameters, trainParameters),
            ProgressTracker.NULL_TRACKER,
            TerminationFlag.RUNNING_TRUE
        ).compute().embeddings();

        var otherEmbeddings = new Node2Vec(
            graph,
            new Concurrency(4),
            NO_SOURCE_NODES,
            Optional.of(1337L),
            1000,
            new Node2VecParameters(walkParameters, trainParameters),
            ProgressTracker.NULL_TRACKER,
            TerminationFlag.RUNNING_TRUE
        ).compute().embeddings();

        for (long node = 0; node < graph.nodeCount(); node++) {
            softly.assertThat(otherEmbeddings.get(node)).isEqualTo(embeddings.get(node));
        }
    }

    static Stream<Arguments> graphs() {
        return Stream.of(
            Arguments.of("All Labels", List.of(NodeLabel.of("Node1"), NodeLabel.of("Node2"), NodeLabel.of("Isolated"))),
            Arguments.of("Non Consecutive Original IDs", List.of(NodeLabel.of("Node2"), NodeLabel.of("Isolated")))
        );
    }

    // Run the algorithm with the exact same configuration on two graphs that are exactly the same, except their node id map
    // has been shuffled. The results should still be similar.
    @ParameterizedTest
    @EnumSource(value = EmbeddingInitializer.class)
    void shouldBeFairlyConsistentUnderOriginalIds(EmbeddingInitializer embeddingInitializer) {
        long nodeCount = 1000;
        int embeddingDimension = 32;
        long degree = 4;

        var firstMappedToOriginal = HugeLongArray.newArray(nodeCount);
        firstMappedToOriginal.setAll(nodeId -> nodeId);
        var firstOriginalToMappedBuilder = HugeSparseLongArray.builder(nodeCount);
        for (long nodeId = 0; nodeId < nodeCount; nodeId++) {
            firstOriginalToMappedBuilder.set(nodeId, nodeId);
        }
        // We create an IdMap explicitly instead of using a NodesBuilder in order to be sure that the id maps of the
        // graphs we produce are very different.
        var firstIdMap = new ArrayIdMap(
            firstMappedToOriginal,
            firstOriginalToMappedBuilder.build(),
            LabelInformationBuilders.singleLabel(NodeLabel.of("hello")).build(nodeCount, firstMappedToOriginal::get),
            nodeCount,
            nodeCount - 1
        );
        RelationshipsBuilder firstRelationshipsBuilder = GraphFactory.initRelationshipsBuilder()
            .nodes(firstIdMap)
            .relationshipType(RelationshipType.of("REL"))
            .orientation(Orientation.UNDIRECTED)
            .executorService(DefaultPool.INSTANCE)
            .build();

        var secondMappedToOriginal = HugeLongArray.newArray(nodeCount);
        secondMappedToOriginal.setAll(nodeId -> nodeId);
        var gen = ShuffleUtil.createRandomDataGenerator(Optional.of(42L));
        ShuffleUtil.shuffleArray(secondMappedToOriginal, gen);
        var secondOriginalToMappedBuilder = HugeSparseLongArray.builder(nodeCount);
        for (long nodeId = 0; nodeId < nodeCount; nodeId++) {
            secondOriginalToMappedBuilder.set(secondMappedToOriginal.get(nodeId), nodeId);
        }
        var secondIdMap = new ArrayIdMap(
            secondMappedToOriginal,
            secondOriginalToMappedBuilder.build(),
            LabelInformationBuilders.singleLabel(NodeLabel.of("hello")).build(nodeCount, secondMappedToOriginal::get),
            nodeCount,
            nodeCount - 1
        );
        RelationshipsBuilder secondRelationshipsBuilder = GraphFactory.initRelationshipsBuilder()
            .nodes(secondIdMap)
            .relationshipType(RelationshipType.of("REL"))
            .orientation(Orientation.UNDIRECTED)
            .executorService(DefaultPool.INSTANCE)
            .build();

        var random = new SplittableRandom(42);
        for (long sourceNodeId = 0; sourceNodeId < nodeCount; sourceNodeId++) {
            for (int j = 0; j < degree; j++) {
                long targetNodeId = random.nextLong(nodeCount);
                firstRelationshipsBuilder.add(sourceNodeId, targetNodeId);
                secondRelationshipsBuilder.add(sourceNodeId, targetNodeId);
            }
        }
        var firstRelationships = firstRelationshipsBuilder.build();
        var secondRelationships = secondRelationshipsBuilder.build();

        var firstGraph = GraphFactory.create(firstIdMap, firstRelationships);
        var secondGraph = GraphFactory.create(secondIdMap, secondRelationships);

        var walkParameters = new SamplingWalkParameters(10, 80, 1.0, 1.0, 0.01, 0.75);
        var trainParameters = new TrainParameters(0.025, 0.0001, 1, 10, 5, embeddingDimension, embeddingInitializer);

        var firstEmbeddings = new Node2Vec(
            firstGraph,
            new Concurrency(4),
            NO_SOURCE_NODES,
            Optional.of(1337L),
            1000,
            new Node2VecParameters(walkParameters, trainParameters),
            ProgressTracker.NULL_TRACKER,
            TerminationFlag.RUNNING_TRUE
        ).compute().embeddings();

        var secondEmbeddings = new Node2Vec(
            secondGraph,
            new Concurrency(4),
            NO_SOURCE_NODES,
            Optional.of(1337L),
            1000,
            new Node2VecParameters(walkParameters, trainParameters),
            ProgressTracker.NULL_TRACKER,
            TerminationFlag.RUNNING_TRUE
        ).compute().embeddings();

        double cosineSum = 0;
        for (long originalNodeId = 0; originalNodeId < nodeCount; originalNodeId++) {
            var firstVector = firstEmbeddings.get(firstGraph.toMappedNodeId(originalNodeId));
            var secondVector = secondEmbeddings.get(secondGraph.toMappedNodeId(originalNodeId));
            double cosine = Intersections.cosine(firstVector.data(), secondVector.data(), secondVector.data().length);
            cosineSum += cosine;
        }
        //There's no hard cutoff on the average cosineSim.
        //We just want to assert different randomly initialized embeddings produce 'relatively similar' embeddings.
        assertThat(cosineSum / nodeCount).isCloseTo(1, Offset.offset(0.6));
    }
}
