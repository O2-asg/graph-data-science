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
package org.neo4j.gds.procedures.algorithms.pathfinding;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.applications.algorithms.machinery.AlgorithmProcessingTimings;
import org.neo4j.gds.applications.algorithms.machinery.ResultBuilder;
import org.neo4j.gds.applications.algorithms.metadata.RelationshipsWritten;
import org.neo4j.gds.steiner.SteinerTreeResult;
import org.neo4j.gds.steiner.SteinerTreeWriteConfig;

import java.util.Optional;

class SteinerTreeResultBuilderForWriteMode implements ResultBuilder<SteinerTreeWriteConfig, SteinerTreeResult, SteinerWriteResult, RelationshipsWritten> {
    @Override
    public SteinerWriteResult build(
        Graph graph,
        GraphStore graphStore,
        SteinerTreeWriteConfig steinerTreeWriteConfig,
        Optional<SteinerTreeResult> steinerTreeResult,
        AlgorithmProcessingTimings timings,
        Optional<RelationshipsWritten> metadata
    ) {
        var builder = new SteinerWriteResult.Builder();
        builder.withConfig(steinerTreeWriteConfig);

        builder.withPreProcessingMillis(timings.preProcessingMillis);
        builder.withComputeMillis(timings.computeMillis);
        builder.withWriteMillis(timings.mutateOrWriteMillis);

        metadata.ifPresent(rw -> builder.withRelationshipsWritten(rw.value));

        steinerTreeResult.ifPresent(result -> {
            builder.withEffectiveNodeCount(result.effectiveNodeCount());
            builder.withEffectiveTargetNodeCount(result.effectiveTargetNodesCount());
            builder.withTotalWeight(result.totalCost());
        });

        return builder.build();
    }
}
