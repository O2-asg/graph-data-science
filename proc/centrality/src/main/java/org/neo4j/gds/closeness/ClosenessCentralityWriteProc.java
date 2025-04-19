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
package org.neo4j.gds.closeness;

import org.neo4j.gds.procedures.GraphDataScienceProcedures;
import org.neo4j.gds.procedures.algorithms.centrality.BetaClosenessCentralityWriteResult;
import org.neo4j.gds.procedures.algorithms.centrality.CentralityWriteResult;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Internal;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.gds.closeness.Constants.CLOSENESS_DESCRIPTION;
import static org.neo4j.procedure.Mode.WRITE;

public class ClosenessCentralityWriteProc {
    @Context
    public GraphDataScienceProcedures facade;

    @Procedure(value = "gds.closeness.write", mode = WRITE)
    @Description(CLOSENESS_DESCRIPTION)
    public Stream<CentralityWriteResult> write(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return facade.algorithms().centrality().closenessCentralityWrite(graphName, configuration);
    }

    @Deprecated(forRemoval = true)
    @Internal
    @Procedure(value = "gds.beta.closeness.write", mode = WRITE, deprecatedBy = "gds.closeness.write")
    @Description(CLOSENESS_DESCRIPTION)
    public Stream<BetaClosenessCentralityWriteResult> writeBeta(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        facade.deprecatedProcedures().called("gds.beta.closeness.write");
        facade
            .log()
            .warn("Procedure `gds.beta.closeness.write` has been deprecated, please use `gds.closeness.write`.");

        return facade.algorithms().centrality().betaClosenessCentralityWrite(graphName, configuration);
    }
}
