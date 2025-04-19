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
package org.neo4j.gds.embeddings.graphsage.algo;


import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.api.tuple.primitive.LongLongPair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.gds.InspectableTestProgressTracker;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.collections.ha.HugeObjectArray;
import org.neo4j.gds.core.GraphDimensions;
import org.neo4j.gds.core.ImmutableGraphDimensions;
import org.neo4j.gds.core.concurrency.Concurrency;
import org.neo4j.gds.mem.MemoryRange;
import org.neo4j.gds.mem.MemoryTree;
import org.neo4j.gds.embeddings.graphsage.Aggregator;
import org.neo4j.gds.embeddings.graphsage.GraphSageTestGraph;
import org.neo4j.gds.embeddings.graphsage.LayerConfig;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongUnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.neo4j.gds.assertj.Extractors.keepingFixedNumberOfDecimals;
import static org.neo4j.gds.assertj.Extractors.removingThreadId;
import static org.neo4j.gds.compat.TestLog.INFO;
import static org.neo4j.gds.mem.MemoryEstimations.RESIDENT_MEMORY;
import static org.neo4j.gds.mem.MemoryEstimations.TEMPORARY_MEMORY;
import static org.neo4j.gds.embeddings.graphsage.GraphSageTestGraph.DUMMY_PROPERTY;
import static org.neo4j.gds.mem.Estimate.sizeOfDoubleArray;
import static org.neo4j.gds.mem.Estimate.sizeOfIntArray;
import static org.neo4j.gds.mem.Estimate.sizeOfLongArray;
import static org.neo4j.gds.mem.Estimate.sizeOfObjectArray;
import static org.neo4j.gds.mem.Estimate.sizeOfOpenHashContainer;
import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

@GdlExtension
class GraphSageTrainAlgorithmFactoryTest {

    private static final int SOME_REASONABLE_VALUE = 100;

    @GdlGraph
    private static final String GDL = GraphSageTestGraph.GDL;

    @Inject
    private Graph graph;

    @SuppressWarnings("UnnecessaryLocalVariable")
    @ParameterizedTest(name = "{0}")
    @MethodSource("parameters")
    void memoryEstimation(
        @SuppressWarnings("unused") String testName,
        GraphSageTrainConfig config,
        GraphDimensions graphDimensions,
        LongUnaryOperator hugeObjectArraySize
    ) {
        var parameters = config.toMemoryEstimateParameters();
        var nodeCount = graphDimensions.nodeCount();
        var concurrency = config.concurrency();
        var layerConfigs = parameters.layerConfigs();
        var weightsPerLabel = MemoryRange.empty();

        if (parameters.isMultiLabel()) {
            var minNumProperties = 1;
            var maxNumProperties = parameters.numberOfFeatureProperties();
            maxNumProperties += 1; // Add one for the label
            var minWeightsMemory = sizeOfDoubleArray(parameters.estimationFeatureDimension() * minNumProperties);
            var maxWeightsMemory = sizeOfDoubleArray(parameters.estimationFeatureDimension() * maxNumProperties);

            weightsPerLabel = weightsPerLabel
                .add(MemoryRange.of(minWeightsMemory, maxWeightsMemory))
                .times(graphDimensions.estimationNodeLabelCount());
        }

        // initial layers
        // rows - embeddingDimension, cols - feature size (first layer), embedding size every other layer
        //  applies to the weights
        //  weights = double[rows * cols], 1x for mean, 2x for maxpooling, double[row * row] for maxpooling, double[rows] bias for maxpooling
        var layersMemory = layerConfigs.stream().mapToLong(layerConfig -> {
            var weightDimensions = layerConfig.rows() * layerConfig.cols();
            var weightsMemory = sizeOfDoubleArray(weightDimensions);
            Aggregator.AggregatorType aggregatorType = layerConfig.aggregatorType();
            if (aggregatorType == Aggregator.AggregatorType.POOL) {
                // selfWeights
                weightsMemory += sizeOfDoubleArray(layerConfig.rows() * layerConfig.rows());
                // neighborsWeights
                weightsMemory += sizeOfDoubleArray(layerConfig.rows() * layerConfig.rows());
                // bias
                weightsMemory += sizeOfDoubleArray(layerConfig.rows());
            }
            return weightsMemory;
        }).sum();

        // features: HugeOA[nodeCount * double[featureSize]]
        long minInitialFeaturesArray = parameters.isMultiLabel()
            ? sizeOfDoubleArray(1)
            : sizeOfDoubleArray(parameters.estimationFeatureDimension());
        long maxInitialFeaturesArray = sizeOfDoubleArray(parameters.estimationFeatureDimension());
        var minInitialFeaturesMemory = hugeObjectArraySize.applyAsLong(minInitialFeaturesArray);
        var maxInitialFeaturesMemory = hugeObjectArraySize.applyAsLong(maxInitialFeaturesArray);
        var initialFeaturesMemory = MemoryRange.of(minInitialFeaturesMemory, maxInitialFeaturesMemory);

        var batchSize = parameters.batchSize();
        var totalBatchSize = 3 * batchSize;

        // embeddings
        // subgraphs

        var minBatchNodeCount = (long) totalBatchSize;
        var maxBatchNodeCount = (long) totalBatchSize;

        var batchSizes = new ArrayList<LongLongPair>();
        // additional final layer size
        batchSizes.add(pair(minBatchNodeCount, maxBatchNodeCount));
        var subGraphMemories = new ArrayList<MemoryRange>();
        for (LayerConfig layerConfig : layerConfigs) {
            var sampleSize = layerConfig.sampleSize();

            // int[3bs] selfAdjacency
            var minSelfAdjacencyMemory = sizeOfIntArray(minBatchNodeCount);
            var maxSelfAdjacencyMemory = sizeOfIntArray(maxBatchNodeCount);

            // int[3bs][0-sampleSize] adjacency
            var minAdjacencyMemory = sizeOfObjectArray(minBatchNodeCount);
            var maxAdjacencyMemory = sizeOfObjectArray(maxBatchNodeCount);

            var minInnerAdjacencyMemory = sizeOfIntArray(0);
            var maxInnerAdjacencyMemory = sizeOfIntArray(sampleSize);

            minAdjacencyMemory += minBatchNodeCount * minInnerAdjacencyMemory;
            maxAdjacencyMemory += maxBatchNodeCount * maxInnerAdjacencyMemory;

            // 3bs -> [min(3bs, nodeCount) .. min(3bs * (sampleSize + 1), nodeCount)]
            var minNextNodeCount = Math.min(minBatchNodeCount, nodeCount);
            var maxNextNodeCount = Math.min(maxBatchNodeCount * (sampleSize + 1), nodeCount);

            // nodeIds long[3bs]
            var minNextNodesMemory = sizeOfLongArray(minNextNodeCount);
            var maxNextNodesMemory = sizeOfLongArray(maxNextNodeCount);

            var minSubGraphMemory = minSelfAdjacencyMemory + minAdjacencyMemory + minNextNodesMemory;
            var maxSubGraphMemory = maxSelfAdjacencyMemory + maxAdjacencyMemory + maxNextNodesMemory;

            // LongIntHashMap toInternalId;
            // This is a local peak; will be GC'd before the global peak
            @SuppressWarnings("unused")
            var minLocalIdMapMemory = sizeOfOpenHashContainer(minNextNodeCount) + sizeOfLongArray(minNextNodeCount);
            @SuppressWarnings("unused")
            var maxLocalIdMapMemory = sizeOfOpenHashContainer(maxNextNodeCount) + sizeOfLongArray(maxNextNodeCount);

            subGraphMemories.add(MemoryRange.of(minSubGraphMemory, maxSubGraphMemory));

            minBatchNodeCount = minNextNodeCount;
            maxBatchNodeCount = maxNextNodeCount;

            // add next layer's sizes
            batchSizes.add(pair(minBatchNodeCount, maxBatchNodeCount));
        }

        Collections.reverse(batchSizes);

        var nextLayerIndex = new AtomicInteger();
        var aggregatorMemories = layerConfigs.stream().map(layerConfig -> {
            var layerIndex = nextLayerIndex.getAndIncrement();

            var nodeCounts = batchSizes.get(layerIndex + 1);
            var minNodeCount = nodeCounts.getOne();
            var maxNodeCount = nodeCounts.getTwo();

            var previousNodeCounts = batchSizes.get(layerIndex);

            long minAggregatorMemory;
            long maxAggregatorMemory;
            if (layerConfig.aggregatorType() == Aggregator.AggregatorType.MEAN) {
                var featureOrEmbeddingSize = layerConfig.cols();

                //   [Weighted]MultiMean - new double[[..adjacency.length(=iterNodeCount)] * featureSize];
                var minMeans = sizeOfDoubleArray(minNodeCount * featureOrEmbeddingSize);
                var maxMeans = sizeOfDoubleArray(maxNodeCount * featureOrEmbeddingSize);

                //   MatrixMultiplyWithTransposedSecondOperand - new double[iterNodeCount * embeddingDimension]
                var minProduct = sizeOfDoubleArray(minNodeCount * parameters.embeddingDimension());
                var maxProduct = sizeOfDoubleArray(maxNodeCount * parameters.embeddingDimension());

                //   activation function = same as input
                var minActivation = minProduct;
                var maxActivation = maxProduct;

                minAggregatorMemory = minMeans + minProduct + minActivation;
                maxAggregatorMemory = maxMeans + maxProduct + maxActivation;
            } else if (layerConfig.aggregatorType() == Aggregator.AggregatorType.POOL) {
                var minPreviousNodeCount = previousNodeCounts.getOne();
                var maxPreviousNodeCount = previousNodeCounts.getTwo();

                //  MatrixMultiplyWithTransposedSecondOperand - new double[iterNodeCount(-1) * embeddingDimension]
                var minWeightedPreviousLayer = sizeOfDoubleArray(minPreviousNodeCount * parameters.embeddingDimension());
                var maxWeightedPreviousLayer = sizeOfDoubleArray(maxPreviousNodeCount * parameters.embeddingDimension());

                // MatrixVectorSum = shape is matrix input == weightedPreviousLayer
                var minBiasedWeightedPreviousLayer = minWeightedPreviousLayer;
                var maxBiasedWeightedPreviousLayer = maxWeightedPreviousLayer;

                //   activation function = same as input
                var minNeighborhoodActivations = minBiasedWeightedPreviousLayer;
                var maxNeighborhoodActivations = maxBiasedWeightedPreviousLayer;

                //  [Weighted]ElementwiseMax - double[iterNodeCount * embeddingDimension]
                var minElementwiseMax = sizeOfDoubleArray(minNodeCount * parameters.embeddingDimension());
                var maxElementwiseMax = sizeOfDoubleArray(maxNodeCount * parameters.embeddingDimension());

                //  Slice - double[iterNodeCount * embeddingDimension]
                var minSelfPreviousLayer = sizeOfDoubleArray(minNodeCount * parameters.embeddingDimension());
                var maxSelfPreviousLayer = sizeOfDoubleArray(maxNodeCount * parameters.embeddingDimension());

                //  MatrixMultiplyWithTransposedSecondOperand - new double[iterNodeCount * embeddingDimension]
                var minSelf = sizeOfDoubleArray(minNodeCount * parameters.embeddingDimension());
                var maxSelf = sizeOfDoubleArray(maxNodeCount * parameters.embeddingDimension());

                //  MatrixMultiplyWithTransposedSecondOperand - new double[iterNodeCount * embeddingDimension]
                var minNeighbors = sizeOfDoubleArray(minNodeCount * parameters.embeddingDimension());
                var maxNeighbors = sizeOfDoubleArray(maxNodeCount * parameters.embeddingDimension());

                //  MatrixSum - new double[iterNodeCount * embeddingDimension]
                var minSum = minSelf;
                var maxSum = maxSelf;

                //  activation function = same as input
                var minActivation = minSum;
                var maxActivation = maxSum;

                minAggregatorMemory = minWeightedPreviousLayer + minBiasedWeightedPreviousLayer + minNeighborhoodActivations + minElementwiseMax + minSelfPreviousLayer + minSelf + minNeighbors + minSum + minActivation;
                maxAggregatorMemory = maxWeightedPreviousLayer + maxBiasedWeightedPreviousLayer + maxNeighborhoodActivations + maxElementwiseMax + maxSelfPreviousLayer + maxSelf + maxNeighbors + maxSum + maxActivation;
            } else {
                // never happens
                minAggregatorMemory = 0;
                maxAggregatorMemory = 0;
            }

            return MemoryRange.of(minAggregatorMemory, maxAggregatorMemory);
        }).collect(toList());

        // normalize rows = same as input (output of aggregator)
        var lastLayerBatchNodeCount = batchSizes.get(batchSizes.size() - 1);
        var minNormalizeRows = sizeOfDoubleArray(lastLayerBatchNodeCount.getOne() * parameters.embeddingDimension());
        var maxNormalizeRows = sizeOfDoubleArray(lastLayerBatchNodeCount.getTwo() * parameters.embeddingDimension());
        aggregatorMemories.add(MemoryRange.of(minNormalizeRows, maxNormalizeRows));

        // previous layer representation = parent = local features: double[(bs..3bs) * featureSize]
        var firstLayerBatchNodeCount = batchSizes.get(0);
        var featureSize = parameters.estimationFeatureDimension();
        var minFirstLayerMemory = sizeOfDoubleArray(firstLayerBatchNodeCount.getOne() * featureSize);
        var maxFirstLayerMemory = sizeOfDoubleArray(firstLayerBatchNodeCount.getTwo() * featureSize);
        if (parameters.isMultiLabel()) {
            // For every node, 1 row of features is multiplied with the transposed matrix of weights-rows x 1 col
            // creating a new array of `projectedFeatureDimension` length, which is the same as parameters.featuresSize()
            minFirstLayerMemory += sizeOfDoubleArray(featureSize);
            maxFirstLayerMemory += sizeOfDoubleArray(featureSize);
        }
        aggregatorMemories.add(0, MemoryRange.of(minFirstLayerMemory, maxFirstLayerMemory));

        var featureFunctionMemory = MemoryRange.empty();
        if (parameters.isMultiLabel()) {
            long minFeatureFunction, maxFeatureFunction;
            minFeatureFunction = sizeOfObjectArray(firstLayerBatchNodeCount.getOne());
            maxFeatureFunction = sizeOfObjectArray(firstLayerBatchNodeCount.getTwo());
            featureFunctionMemory = featureFunctionMemory
                .add(MemoryRange.of(minFeatureFunction, maxFeatureFunction))
                .add(MemoryRange.of(sizeOfObjectArray(graphDimensions.estimationNodeLabelCount())));
        }

        var lossFunctionMemory = Stream.concat(
            subGraphMemories.stream(),
            aggregatorMemories.stream()
        ).reduce(MemoryRange.empty(), MemoryRange::add)
            .add(featureFunctionMemory);

        var evaluateLossMemory = lossFunctionMemory.times(concurrency.value());

        // adam optimizer
        //  copy of weight for every layer
        var initialAdamMemory = layerConfigs.stream().mapToLong(layerConfig -> {
            var weightDimensions = layerConfig.rows() * layerConfig.cols();
            var momentumTermsMemory = sizeOfDoubleArray(weightDimensions);
            var velocityTermsMemory = momentumTermsMemory;

            return momentumTermsMemory + velocityTermsMemory;
        }).sum();

        var updateAdamMemory = layerConfigs.stream().mapToLong(layerConfig -> {
            var weightDimensions = layerConfig.rows() * layerConfig.cols();
            // adam update

            //  new momentum
            //   1 copy of weights
            //   1 copy of momentum terms (same dim as weights)
            // not part of peak usage as update peak is larger
            @SuppressWarnings("unused")
            var newMomentum = 2 * weightDimensions;

            //  new velocity
            //   2 copies of weights
            //   1 copy of momentum terms (same dim as weights)
            // not part of peak usage as update peak is larger
            @SuppressWarnings("unused")
            var newVelocity = 3 * weightDimensions;

            //  mCaps
            //   copy of momentumTerm (size of weights)
            var mCaps = weightDimensions;

            //  vCaps
            //   copy of velocityTerm (size of weights)
            var vCaps = weightDimensions;

            //  updating weights
            //   2 copies of mCap, 1 copy of vCap
            var updateWeights = 2 * mCaps + vCaps;

            var updateMemory = updateWeights + mCaps + vCaps;

            return updateMemory;
        }).sum();

        var backwardsLossFunctionMemory =
            aggregatorMemories.stream().reduce(MemoryRange.empty(), MemoryRange::add);

        var trainOnBatchMemory =
            lossFunctionMemory
                .add(backwardsLossFunctionMemory)
                .add(MemoryRange.of(updateAdamMemory));

        var trainOnEpoch = trainOnBatchMemory
            .times(concurrency.value())
            .add(MemoryRange.of(initialAdamMemory));

        var trainMemory =
            trainOnEpoch
                .max(evaluateLossMemory)
                .add(initialFeaturesMemory)
                .add(weightsPerLabel);

        var expectedResidentMemory = MemoryRange.of(layersMemory);
        var expectedPeakMemory = trainMemory
            .add(expectedResidentMemory)
            .add(MemoryRange.of(40L)); // For GraphSage.class

        var actualEstimation = new GraphSageTrainAlgorithmFactory()
            .memoryEstimation(parameters)
            .estimate(graphDimensions, concurrency);

        assertEquals(expectedPeakMemory, actualEstimation.memoryUsage());
        assertThat(actualEstimation.residentMemory())
            .isPresent()
            .map(MemoryTree::memoryUsage)
            .contains(expectedResidentMemory);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void memoryEstimationTreeStructure(boolean isMultiLabel) {
        var builder = GraphSageTrainConfigImpl
            .builder()
            .modelUser("userName")
            .modelName("modelName")
            .featureProperties(List.of("a"))
            .sampleSizes(List.of(1, 2))
            .aggregator(Aggregator.AggregatorType.MEAN);
        var config = isMultiLabel
            ? builder.projectedFeatureDimension(SOME_REASONABLE_VALUE).build()
            : builder.build();

        var actualEstimation = new GraphSageTrainAlgorithmFactory()
            .memoryEstimation(config)
            .estimate(GraphDimensions.of(1337), new Concurrency(42));

        var expectedTreeStructure = Stream.<IntObjectPair<String>>builder()
            .add(pair(0, "GraphSageTrain"))
            .add(pair(1, RESIDENT_MEMORY))
            .add(pair(2, "weights"))
            .add(pair(3, "layer 1"))
            .add(pair(3, "layer 2"))
            .add(pair(1, TEMPORARY_MEMORY))
            .add(pair(2, "this.instance"));

        if (isMultiLabel) {
            expectedTreeStructure.accept(pair(2, "weightsByLabel"));
        }

        expectedTreeStructure
            .add(pair(2, "initialFeatures"))
            .add(pair(2, "trainOnEpoch"))
            .add(pair(3, "initialAdamOptimizer"))
            .add(pair(3, "concurrentBatches"))
            .add(pair(4, "trainOnBatch"))
            .add(pair(5, "computationGraph"))
            .add(pair(6, "subgraphs"))
            .add(pair(7, "subgraph 1"))
            .add(pair(7, "subgraph 2"));

        if (isMultiLabel) {
            expectedTreeStructure.accept(pair(6, "multiLabelFeatureFunction"));
        }

        expectedTreeStructure
            .add(pair(6, "forward"))
            .add(pair(7, "firstLayer"))
            .add(pair(7, "MEAN 1"))
            .add(pair(7, "MEAN 2"))
            .add(pair(7, "normalizeRows"))
            .add(pair(6, "backward"))
            .add(pair(7, "firstLayer"))
            .add(pair(7, "MEAN 1"))
            .add(pair(7, "MEAN 2"))
            .add(pair(7, "normalizeRows"))
            .add(pair(5, "updateAdamOptimizer"));

        assertThat(flatten(actualEstimation)).containsExactlyElementsOf(expectedTreeStructure.build().collect(toList()));
    }

    @Test
    void testLogging() {
        var config = GraphSageTrainConfigImpl.builder()
            .modelUser("DUMMY")
            .featureProperties(List.of(DUMMY_PROPERTY))
            .embeddingDimension(12)
            .aggregator(Aggregator.AggregatorType.POOL)
            .tolerance(1e-10)
            .sampleSizes(List.of(5, 3))
            .batchSize(4)
            .randomSeed(42L)
            .modelName("model")
            .relationshipWeightProperty("times")
            .epochs(2)
            .maxIterations(2)
            .build();

        var factory = new GraphSageTrainAlgorithmFactory();

        var progressTracker = new InspectableTestProgressTracker(
            factory.progressTask(graph, config),
            config.username(),
            config.jobId()
        );

        var algo = factory.build(
            graph,
            config,
            progressTracker
        );

        algo.compute();

        assertThat(progressTracker.log().getMessages(INFO))
            // avoid asserting on the thread id
            .extracting(removingThreadId())
            .extracting(keepingFixedNumberOfDecimals(2))
            .containsExactly(
                "GraphSageTrain :: Start",
                "GraphSageTrain :: Prepare batches :: Start",
                "GraphSageTrain :: Prepare batches 20%",
                "GraphSageTrain :: Prepare batches 40%",
                "GraphSageTrain :: Prepare batches 60%",
                "GraphSageTrain :: Prepare batches 80%",
                "GraphSageTrain :: Prepare batches 100%",
                "GraphSageTrain :: Prepare batches :: Finished",
                "GraphSageTrain :: Train model :: Start",
                "GraphSageTrain :: Train model :: Epoch 1 of 2 :: Start",
                "GraphSageTrain :: Train model :: Epoch 1 of 2 :: Iteration 1 of 2 :: Start",
                "GraphSageTrain :: Train model :: Epoch 1 of 2 :: Iteration 1 of 2 25%",
                "GraphSageTrain :: Train model :: Epoch 1 of 2 :: Iteration 1 of 2 50%",
                "GraphSageTrain :: Train model :: Epoch 1 of 2 :: Iteration 1 of 2 75%",
                "GraphSageTrain :: Train model :: Epoch 1 of 2 :: Iteration 1 of 2 100%",
                "GraphSageTrain :: Train model :: Epoch 1 of 2 :: Iteration 1 of 2 :: Average loss per node: 26.52",
                "GraphSageTrain :: Train model :: Epoch 1 of 2 :: Iteration 1 of 2 :: Finished",
                "GraphSageTrain :: Train model :: Epoch 1 of 2 :: Iteration 2 of 2 :: Start",
                "GraphSageTrain :: Train model :: Epoch 1 of 2 :: Iteration 2 of 2 25%",
                "GraphSageTrain :: Train model :: Epoch 1 of 2 :: Iteration 2 of 2 50%",
                "GraphSageTrain :: Train model :: Epoch 1 of 2 :: Iteration 2 of 2 75%",
                "GraphSageTrain :: Train model :: Epoch 1 of 2 :: Iteration 2 of 2 100%",
                "GraphSageTrain :: Train model :: Epoch 1 of 2 :: Iteration 2 of 2 :: Average loss per node: 22.35",
                "GraphSageTrain :: Train model :: Epoch 1 of 2 :: Iteration 2 of 2 :: Finished",
                "GraphSageTrain :: Train model :: Epoch 1 of 2 :: Finished",
                "GraphSageTrain :: Train model :: Epoch 2 of 2 :: Start",
                "GraphSageTrain :: Train model :: Epoch 2 of 2 :: Iteration 1 of 2 :: Start",
                "GraphSageTrain :: Train model :: Epoch 2 of 2 :: Iteration 1 of 2 25%",
                "GraphSageTrain :: Train model :: Epoch 2 of 2 :: Iteration 1 of 2 50%",
                "GraphSageTrain :: Train model :: Epoch 2 of 2 :: Iteration 1 of 2 75%",
                "GraphSageTrain :: Train model :: Epoch 2 of 2 :: Iteration 1 of 2 100%",
                "GraphSageTrain :: Train model :: Epoch 2 of 2 :: Iteration 1 of 2 :: Average loss per node: 22.43",
                "GraphSageTrain :: Train model :: Epoch 2 of 2 :: Iteration 1 of 2 :: Finished",
                "GraphSageTrain :: Train model :: Epoch 2 of 2 :: Iteration 2 of 2 :: Start",
                "GraphSageTrain :: Train model :: Epoch 2 of 2 :: Iteration 2 of 2 25%",
                "GraphSageTrain :: Train model :: Epoch 2 of 2 :: Iteration 2 of 2 50%",
                "GraphSageTrain :: Train model :: Epoch 2 of 2 :: Iteration 2 of 2 75%",
                "GraphSageTrain :: Train model :: Epoch 2 of 2 :: Iteration 2 of 2 100%",
                "GraphSageTrain :: Train model :: Epoch 2 of 2 :: Iteration 2 of 2 :: Average loss per node: 25.86",
                "GraphSageTrain :: Train model :: Epoch 2 of 2 :: Iteration 2 of 2 :: Finished",
                "GraphSageTrain :: Train model :: Epoch 2 of 2 :: Finished",
                "GraphSageTrain :: Train model :: Finished",
                "GraphSageTrain :: Finished"
            );

        progressTracker.assertValidProgressEvolution();
    }

    private static List<IntObjectPair<String>> flatten(MemoryTree memoryTree) {
        return leaves(0, memoryTree).collect(toList());
    }

    private static Stream<IntObjectPair<String>> leaves(int depth, MemoryTree memoryTree) {
        return Stream.concat(
            Stream.of(pair(depth, memoryTree.description())),
            memoryTree.components().stream().flatMap(tree -> leaves(depth + 1, tree))
        );
    }

    static Stream<Arguments> parameters() {
        var smallNodeCounts = List.of(1L, 100L, 10_000L);
        var largeNodeCounts = List.of(100_000_000_000L);
        var nodeCounts = Stream.concat(
            smallNodeCounts.stream().map(nc -> Tuples.pair(
                nc,
                (LongUnaryOperator) new LongUnaryOperator() {
                    @Override
                    public long applyAsLong(long innerSize) {
                        return HugeObjectArray.memoryEstimation(nc, innerSize);
                    }

                    @Override
                    public String toString() {
                        return "single page";
                    }
                }
            )),
            largeNodeCounts.stream().map(nc -> Tuples.pair(
                nc,
                (LongUnaryOperator) new LongUnaryOperator() {
                    @Override
                    public long applyAsLong(long innerSize) {
                        return HugeObjectArray.memoryEstimation(nc, innerSize);
                    }

                    @Override
                    public String toString() {
                        return "multiple pages";
                    }
                }
            ))
        );

        var userName = "userName";
        var modelName = "modelName";

        var concurrencies = List.of(1, 4);
        var batchSizes = List.of(1, 100, 10_000);
        var featurePropertySizes = List.of(1, 9, 42);
        var embeddingDimensions = List.of(64, 256);
        var aggregators = List.of(Aggregator.AggregatorType.MEAN, Aggregator.AggregatorType.POOL);
        var degreesAsProperty = List.of(true, false);
        var projectedFeatureDimensions = List.of(
            /* single label */ Optional.<Integer>empty(),
            /* multi label  */ Optional.of(42)
        );
        var labelCounts = List.of(1, 42);
        var sampleSizesList = List.of(List.of(5, 100));

        return nodeCounts.flatMap(nodeCountPair -> {
            var nodeCount = nodeCountPair.getOne();
            var hugeObjectArraySize = nodeCountPair.getTwo();

            return concurrencies.stream().flatMap(concurrency ->
                sampleSizesList.stream().flatMap(sampleSizes ->
                    batchSizes.stream().flatMap(batchSize ->
                        aggregators.stream().flatMap(aggregator ->
                            embeddingDimensions.stream().flatMap(embeddingDimension ->
                                degreesAsProperty.stream().flatMap(degreeAsProperty ->
                                    projectedFeatureDimensions.stream().flatMap(projectedFeatureDimension ->
                                        labelCounts.stream()
                                            // For the single-label case we don't care about the label count
                                            .limit(projectedFeatureDimension.isEmpty() ? 1 : labelCounts.size())
                                            .flatMap(labelCount ->
                                                featurePropertySizes.stream().map(featurePropertySize -> {
                                                    var testName = formatWithLocale(
                                                        "concurrency: %s, batchSize: %s, aggregator: %s, embeddings: %s, degreeAsProp: %s, projected: %s, labels: %s, features: %s, nodes: %s",
                                                        concurrency,
                                                        batchSize,
                                                        aggregator,
                                                        embeddingDimension,
                                                        degreeAsProperty,
                                                        projectedFeatureDimension,
                                                        labelCount,
                                                        featurePropertySize,
                                                        nodeCount
                                                    );
                                                    var config = GraphSageTrainConfigImpl
                                                        .builder()
                                                        .modelName(modelName)
                                                        .modelUser(userName)
                                                        .concurrency(concurrency)
                                                        .sampleSizes(sampleSizes)
                                                        .batchSize(batchSize)
                                                        .aggregator(aggregator)
                                                        .embeddingDimension(embeddingDimension)
                                                        .projectedFeatureDimension(projectedFeatureDimension)
                                                        .featureProperties(
                                                            IntStream.range(0, featurePropertySize)
                                                                .mapToObj(i -> String.valueOf('a' + i))
                                                                .collect(toList())
                                                        )
                                                        .build();

                                                var dimensions = ImmutableGraphDimensions
                                                    .builder()
                                                    .nodeCount(nodeCount)
                                                    .estimationNodeLabelCount(labelCount)
                                                    .build();

                                                return arguments(
                                                    testName,
                                                    config,
                                                    dimensions,
                                                    hugeObjectArraySize
                                                );
                                            })
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            );
        });
    }
}
