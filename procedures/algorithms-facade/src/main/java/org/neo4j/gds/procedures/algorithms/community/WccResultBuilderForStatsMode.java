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
package org.neo4j.gds.procedures.algorithms.community;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.applications.algorithms.machinery.AlgorithmProcessingTimings;
import org.neo4j.gds.applications.algorithms.machinery.ResultBuilder;
import org.neo4j.gds.core.utils.paged.dss.DisjointSetStruct;
import org.neo4j.gds.result.StatisticsComputationInstructions;
import org.neo4j.gds.wcc.WccStatsConfig;

import java.util.Optional;
import java.util.stream.Stream;

class WccResultBuilderForStatsMode implements ResultBuilder<WccStatsConfig, DisjointSetStruct, Stream<WccStatsResult>, Void> {
    private final CommunityStatisticsWithTimingComputer communityStatisticsWithTimingComputer = new CommunityStatisticsWithTimingComputer();

    private final StatisticsComputationInstructions statisticsComputationInstructions;

    WccResultBuilderForStatsMode(StatisticsComputationInstructions statisticsComputationInstructions) {
        this.statisticsComputationInstructions = statisticsComputationInstructions;
    }

    @Override
    public Stream<WccStatsResult> build(
        Graph graph,
        GraphStore graphStore,
        WccStatsConfig configuration,
        Optional<DisjointSetStruct> result,
        AlgorithmProcessingTimings timings,
        Optional<Void> unused
    ) {
        if (result.isEmpty()) return Stream.of(WccStatsResult.emptyFrom(timings, configuration.toMap()));

        var disjointSetStruct = result.get();

        var communityStatisticsWithTiming = communityStatisticsWithTimingComputer.compute(
            configuration,
            statisticsComputationInstructions,
            disjointSetStruct.size(),
            disjointSetStruct::setIdOf
        );

        var wccStatsResult = new WccStatsResult(
            communityStatisticsWithTiming.getLeft(),
            communityStatisticsWithTiming.getMiddle(),
            timings.preProcessingMillis,
            timings.computeMillis,
            communityStatisticsWithTiming.getRight(),
            configuration.toMap()
        );

        return Stream.of(wccStatsResult);
    }
}
