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
import org.neo4j.gds.procedures.algorithms.pathfinding.SpanningTreeWriteResult;
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
import static org.neo4j.procedure.Mode.WRITE;

public class SpanningTreeWriteProc {
    @Context
    public GraphDataScienceProcedures facade;

    @Procedure(value = "gds.spanningTree.write", mode = WRITE)
    @Description(SPANNING_TREE_DESCRIPTION)
    public Stream<SpanningTreeWriteResult> spanningTree(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return facade.algorithms().pathFinding().spanningTreeWrite(graphName, configuration);
    }

    @Procedure(value = "gds.spanningTree.write" + ".estimate", mode = READ)
    @Description(MEMORY_ESTIMATION_DESCRIPTION)
    public Stream<MemoryEstimateResult> estimate(
        @Name(value = "graphNameOrConfiguration") Object graphName,
        @Name(value = "algoConfiguration") Map<String, Object> configuration
    ) {
        return facade.algorithms().pathFinding().spanningTreeWriteEstimate(graphName, configuration);
    }

    @Procedure(value = "gds.beta.spanningTree.write", mode = WRITE, deprecatedBy = "gds.spanningTree.write")
    @Description(SPANNING_TREE_DESCRIPTION)
    @Internal
    @Deprecated
    public Stream<SpanningTreeWriteResult> betaSpanningTree(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        facade.deprecatedProcedures().called("gds.beta.spanningTree.write");
        facade
            .log()
            .warn("Procedure `gds.beta.spanningTree.write` has been deprecated, please use `gds.spanningTree.write`.");
        return spanningTree(graphName, configuration);
    }

    @Procedure(value = "gds.beta.spanningTree.write" + ".estimate", mode = READ, deprecatedBy = "gds.spanningTree.write" + ".estimate")
    @Internal
    @Deprecated
    @Description(MEMORY_ESTIMATION_DESCRIPTION)
    public Stream<MemoryEstimateResult> betaEstimate(
        @Name(value = "graphNameOrConfiguration") Object graphName,
        @Name(value = "algoConfiguration") Map<String, Object> configuration
    ) {
        facade.deprecatedProcedures().called("gds.beta.spanningTree.write" + ".estimate");
        facade
            .log()
            .warn(
                "Procedure `gds.beta.spanningTree.write.estimate` has been deprecated, please use `gds.spanningTree.write.estimate`.");
        return estimate(graphName, configuration);
    }
}
