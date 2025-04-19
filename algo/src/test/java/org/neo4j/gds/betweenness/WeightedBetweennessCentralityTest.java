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
package org.neo4j.gds.betweenness;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.concurrency.Concurrency;
import org.neo4j.gds.core.concurrency.DefaultPool;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.IdFunction;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.extension.TestGraph;
import org.neo4j.gds.termination.TerminationFlag;

import java.util.Optional;

@GdlExtension
class WeightedBetweennessCentralityTest {

    // (a1)  (a2)
    //   \   /
    //    (b)
    //    / \
    //  (c) (d)
    //   \  /
    //    (e)
    //     |
    //    (f)
    @GdlGraph(graphNamePrefix = "equallyWeighted")
    private static final String equallyWeightedGdl =
        "CREATE " +
        "  (a1)-[:REL {weight: 1.0}]->(b)" +
        ", (a2)-[:REL {weight: 1.0}]->(b)" +
        ", (b) -[:REL {weight: 1.0}]->(c)" +
        ", (b) -[:REL {weight: 1.0}]->(d)" +
        ", (c) -[:REL {weight: 1.0}]->(e)" +
        ", (d) -[:REL {weight: 1.0}]->(e)" +
        ", (e) -[:REL {weight: 1.0}]->(f)";

    @Inject
    private Graph equallyWeightedGraph;

    @GdlGraph(graphNamePrefix = "weighted")
    private static final String weightedGdl =
        "CREATE " +
        "  (a1)-[:REL {weight: 1.0}]->(b)" +
        ", (a2)-[:REL {weight: 1.0}]->(b)" +
        ", (b) -[:REL {weight: 1.0}]->(c)" +
        ", (b) -[:REL {weight: 1.3}]->(d)" +
        ", (c) -[:REL {weight: 1.0}]->(e)" +
        ", (d) -[:REL {weight: 0.2}]->(e)" +
        ", (e) -[:REL {weight: 1.0}]->(f)";
    @Inject
    private TestGraph weightedGraph;

    @Test
    void shouldEqualWithUnweightedWhenWeightsAreEqual() {
        var algoWeighted = new BetweennessCentrality(
            equallyWeightedGraph,
            new RandomDegreeSelectionStrategy(7, Optional.of(42L)),
            ForwardTraverser.Factory.weighted(),
            DefaultPool.INSTANCE,
            new Concurrency(8),
            ProgressTracker.NULL_TRACKER,
            TerminationFlag.RUNNING_TRUE
        );
        var algoUnweighted = new BetweennessCentrality(
            equallyWeightedGraph,
            new RandomDegreeSelectionStrategy(7, Optional.of(42L)),
            ForwardTraverser.Factory.unweighted(),
            DefaultPool.INSTANCE,
            new Concurrency(8),
            ProgressTracker.NULL_TRACKER,
            TerminationFlag.RUNNING_TRUE
        );
        var resultWeighted = algoWeighted.compute().centralities();
        var resultUnweighted = algoUnweighted.compute().centralities();

        SoftAssertions softAssertions = new SoftAssertions();
        equallyWeightedGraph.forEachNode(nodeId -> {
            softAssertions.assertThat(resultWeighted.get(nodeId))
                .isEqualTo(resultUnweighted.get(nodeId));
                return true;
            }
        );

        softAssertions.assertAll();
    }

    @Test
    void shouldComputeWithWeights() {
        IdFunction weightedIdFunction = weightedGraph::toMappedNodeId;

         var bc = new BetweennessCentrality(
             weightedGraph,
             new RandomDegreeSelectionStrategy(7, Optional.of(42L)),
             ForwardTraverser.Factory.weighted(),
             DefaultPool.INSTANCE,
             new Concurrency(8),
             ProgressTracker.NULL_TRACKER,
             TerminationFlag.RUNNING_TRUE
         );
        var result = bc.compute().centralities();
        var softAssertions = new SoftAssertions();
        softAssertions.assertThat(result.get(weightedIdFunction.of("a1"))).isEqualTo(0.0D);
        softAssertions.assertThat(result.get(weightedIdFunction.of("a2"))).isEqualTo(0.0D);
        softAssertions.assertThat(result.get(weightedIdFunction.of("b"))).isEqualTo(8.0D);
        softAssertions.assertThat(result.get(weightedIdFunction.of("c"))).isEqualTo(0.0D);
        softAssertions.assertThat(result.get(weightedIdFunction.of("d"))).isEqualTo(6.0D);
        softAssertions.assertThat(result.get(weightedIdFunction.of("e"))).isEqualTo(5.0D);
        softAssertions.assertThat(result.get(weightedIdFunction.of("f"))).isEqualTo(0.0D);
        softAssertions.assertAll();
    }

}
