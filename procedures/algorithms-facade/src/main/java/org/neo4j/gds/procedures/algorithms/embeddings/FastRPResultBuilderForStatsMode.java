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
package org.neo4j.gds.procedures.algorithms.embeddings;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.applications.algorithms.machinery.AlgorithmProcessingTimings;
import org.neo4j.gds.applications.algorithms.machinery.ResultBuilder;
import org.neo4j.gds.embeddings.fastrp.FastRPResult;
import org.neo4j.gds.embeddings.fastrp.FastRPStatsConfig;

import java.util.Optional;
import java.util.stream.Stream;

class FastRPResultBuilderForStatsMode implements ResultBuilder<FastRPStatsConfig, FastRPResult, Stream<FastRPStatsResult>, Void> {
    @Override
    public Stream<FastRPStatsResult> build(
        Graph graph,
        GraphStore graphStore,
        FastRPStatsConfig configuration,
        Optional<FastRPResult> result,
        AlgorithmProcessingTimings timings,
        Optional<Void> unused
    ) {
        if (result.isEmpty()) return Stream.of(FastRPStatsResult.emptyFrom(timings, configuration.toMap()));

        var fastRPStatsResult = new FastRPStatsResult(
            graph.nodeCount(),
            timings.preProcessingMillis,
            timings.computeMillis,
            configuration.toMap()
        );

        return Stream.of(fastRPStatsResult);
    }
}
