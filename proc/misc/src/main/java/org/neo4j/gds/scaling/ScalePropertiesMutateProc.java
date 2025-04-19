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
package org.neo4j.gds.scaling;

import org.neo4j.gds.applications.algorithms.machinery.MemoryEstimateResult;
import org.neo4j.gds.procedures.GraphDataScienceProcedures;
import org.neo4j.gds.procedures.algorithms.miscellaneous.ScalePropertiesMutateResult;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Internal;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.gds.procedures.ProcedureConstants.MEMORY_ESTIMATION_DESCRIPTION;
import static org.neo4j.gds.scaling.ScalePropertiesProc.SCALE_PROPERTIES_DESCRIPTION;
import static org.neo4j.procedure.Mode.READ;

public class ScalePropertiesMutateProc {
    @Context
    public GraphDataScienceProcedures facade;

    @Procedure("gds.scaleProperties.mutate")
    @Description(SCALE_PROPERTIES_DESCRIPTION)
    public Stream<ScalePropertiesMutateResult> mutate(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return facade.algorithms().miscellaneous().scalePropertiesMutateStub().execute(graphName, configuration);
    }

    @Procedure(value = "gds.scaleProperties.mutate.estimate", mode = READ)
    @Description(MEMORY_ESTIMATION_DESCRIPTION)
    public Stream<MemoryEstimateResult> estimate(
        @Name(value = "graphNameOrConfiguration") Object graphName,
        @Name(value = "algoConfiguration") Map<String, Object> configuration
    ) {
        return facade.algorithms().miscellaneous().scalePropertiesMutateStub().estimate(graphName, configuration);
    }

    @Internal
    @Deprecated(forRemoval = true)
    @Procedure(value = "gds.alpha.scaleProperties.mutate", deprecatedBy = "gds.scaleProperties.mutate")
    @Description(SCALE_PROPERTIES_DESCRIPTION)
    public Stream<ScalePropertiesMutateResult> alphaMutate(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        facade.deprecatedProcedures().called("gds.alpha.scaleProperties.mutate");

        return facade.algorithms().miscellaneous().alphaScalePropertiesMutateStub().execute(graphName, configuration);
    }
}
