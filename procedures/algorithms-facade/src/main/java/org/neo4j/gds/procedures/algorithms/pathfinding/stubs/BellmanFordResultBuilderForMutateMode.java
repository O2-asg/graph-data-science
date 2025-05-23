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
package org.neo4j.gds.procedures.algorithms.pathfinding.stubs;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.applications.algorithms.machinery.AlgorithmProcessingTimings;
import org.neo4j.gds.applications.algorithms.machinery.ResultBuilder;
import org.neo4j.gds.applications.algorithms.metadata.RelationshipsWritten;
import org.neo4j.gds.paths.bellmanford.AllShortestPathsBellmanFordMutateConfig;
import org.neo4j.gds.paths.bellmanford.BellmanFordResult;
import org.neo4j.gds.procedures.algorithms.pathfinding.BellmanFordMutateResult;

import java.util.Optional;

public class BellmanFordResultBuilderForMutateMode implements ResultBuilder<AllShortestPathsBellmanFordMutateConfig, BellmanFordResult, BellmanFordMutateResult, RelationshipsWritten> {
    @Override
    public BellmanFordMutateResult build(
        Graph graph,
        GraphStore graphStore,
        AllShortestPathsBellmanFordMutateConfig configuration,
        Optional<BellmanFordResult> result,
        AlgorithmProcessingTimings timings,
        Optional<RelationshipsWritten> metadata
    ) {
        var resultBuilder = new BellmanFordMutateResult.Builder();
        resultBuilder.withConfig(configuration);

        resultBuilder.withPreProcessingMillis(timings.preProcessingMillis);
        resultBuilder.withComputeMillis(timings.computeMillis);
        resultBuilder.withMutateMillis(timings.mutateOrWriteMillis);

        metadata.ifPresent(rw -> resultBuilder.withRelationshipsWritten(rw.value));

        //noinspection OptionalIsPresent
        if (result.isPresent()) resultBuilder.withContainsNegativeCycle(result.get().containsNegativeCycle());

        return resultBuilder.build();
    }
}
