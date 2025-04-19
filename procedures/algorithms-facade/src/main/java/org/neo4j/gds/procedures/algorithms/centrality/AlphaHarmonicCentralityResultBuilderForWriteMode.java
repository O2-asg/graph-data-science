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
package org.neo4j.gds.procedures.algorithms.centrality;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.applications.algorithms.machinery.AlgorithmProcessingTimings;
import org.neo4j.gds.applications.algorithms.machinery.ResultBuilder;
import org.neo4j.gds.applications.algorithms.metadata.NodePropertiesWritten;
import org.neo4j.gds.harmonic.DeprecatedTieredHarmonicCentralityWriteConfig;
import org.neo4j.gds.harmonic.HarmonicResult;

import java.util.Optional;
import java.util.stream.Stream;

class AlphaHarmonicCentralityResultBuilderForWriteMode implements ResultBuilder<DeprecatedTieredHarmonicCentralityWriteConfig, HarmonicResult, Stream<AlphaHarmonicWriteResult>, NodePropertiesWritten> {
    private final CentralityDistributionComputer centralityDistributionComputer = new CentralityDistributionComputer();

    private final boolean shouldComputeCentralityDistribution;

    AlphaHarmonicCentralityResultBuilderForWriteMode(boolean shouldComputeCentralityDistribution) {
        this.shouldComputeCentralityDistribution = shouldComputeCentralityDistribution;
    }

    @Override
    public Stream<AlphaHarmonicWriteResult> build(
        Graph graph,
        GraphStore graphStore,
        DeprecatedTieredHarmonicCentralityWriteConfig configuration,
        Optional<HarmonicResult> result,
        AlgorithmProcessingTimings timings,
        Optional<NodePropertiesWritten> metadata
    ) {
        if (result.isEmpty()) return Stream.of(AlphaHarmonicWriteResult.emptyFrom(timings, configuration));

        var centralityDistributionAndTiming = centralityDistributionComputer.compute(
            graph,
            result.get().centralityScoreProvider(),
            configuration,
            shouldComputeCentralityDistribution
        );

        var alphaHarmonicWriteResult = new AlphaHarmonicWriteResult(
            graph.nodeCount(),
            timings.preProcessingMillis,
            timings.computeMillis,
            timings.mutateOrWriteMillis,
            configuration.writeProperty(),
            centralityDistributionAndTiming.getLeft()
        );

        return Stream.of(alphaHarmonicWriteResult);
    }
}
