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
package org.neo4j.gds.paths.spanningtree;

import org.neo4j.gds.procedures.GraphDataScienceProcedures;
import org.neo4j.gds.procedures.algorithms.pathfinding.SpanningTreeMutateResult;
import org.neo4j.gds.applications.algorithms.machinery.MemoryEstimateResult;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Internal;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.gds.paths.spanningtree.Constants.SPANNING_TREE_DESCRIPTION;
import static org.neo4j.gds.procedures.ProcedureConstants.MEMORY_ESTIMATION_DESCRIPTION;
import static org.neo4j.procedure.Mode.READ;

public class SpanningTreeMutateProc {
    @Context
    public GraphDataScienceProcedures facade;

    @Procedure(value = "gds.spanningTree.mutate", mode = READ)
    @Description(SPANNING_TREE_DESCRIPTION)
    public Stream<SpanningTreeMutateResult> spanningTree(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return facade.algorithms().pathFinding().spanningTreeMutateStub().execute(graphName, configuration);
    }

    @Procedure(value = "gds.spanningTree.mutate" + ".estimate", mode = READ)
    @Description(MEMORY_ESTIMATION_DESCRIPTION)
    public Stream<MemoryEstimateResult> estimate(
        @Name(value = "graphNameOrConfiguration") Object graphName,
        @Name(value = "algoConfiguration") Map<String, Object> configuration
    ) {
        return facade.algorithms().pathFinding().spanningTreeMutateStub().estimate(graphName, configuration);
    }

    @Procedure(value = "gds.beta.spanningTree.mutate", mode = READ, deprecatedBy = "gds.spanningTree.mutate")
    @Description(SPANNING_TREE_DESCRIPTION)
    @Internal
    @Deprecated
    public Stream<SpanningTreeMutateResult> betaSpanningTree(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        facade.deprecatedProcedures().called("gds.beta.spanningTree.mutate");

        facade
            .log()
            .warn("Procedure `gds.beta.spanningTree.mutate` has been deprecated, please use `gds.spanningTree.mutate`.");
        return spanningTree(graphName, configuration);
    }

    @Procedure(value = "gds.beta.spanningTree.mutate" + ".estimate", mode = READ, deprecatedBy = "gds.spanningTree.mutate" + ".estimate")
    @Description(MEMORY_ESTIMATION_DESCRIPTION)
    @Internal
    @Deprecated
    public Stream<MemoryEstimateResult> betaEstimate(
        @Name(value = "graphNameOrConfiguration") Object graphName,
        @Name(value = "algoConfiguration") Map<String, Object> configuration
    ) {
        facade.deprecatedProcedures().called("gds.beta.spanningTree.mutate" + ".estimate");
        facade
            .log()
            .warn(
                "Procedure `gds.beta.spanningTree.mutate.estimate` has been deprecated, please use `gds.spanningTree.mutate.estimate`.");
        return estimate(graphName, configuration);
    }
}
