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
package org.neo4j.gds.undirected;

import org.neo4j.gds.BaseProc;
import org.neo4j.gds.core.loading.SingleTypeRelationships;
import org.neo4j.gds.executor.MemoryEstimationExecutor;
import org.neo4j.gds.executor.ProcedureExecutor;
import org.neo4j.gds.executor.ProcedureExecutorSpec;
import org.neo4j.gds.applications.algorithms.machinery.MemoryEstimateResult;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Internal;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.procedure.Mode.READ;

public class ToUndirectedProc extends BaseProc {

    @Internal
    @Deprecated(forRemoval = true)
    @Procedure(
        value = "gds.beta.graph.relationships.toUndirected", mode = READ,
        deprecatedBy = ToUndirectedSpec.CALLABLE_NAME
    )
    @Description(ToUndirectedSpec.DESCRIPTION)
    public Stream<ToUndirectedSpec.MutateResult> mutateDeprecated(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        executionContext()
            .metricsFacade()
            .deprecatedProcedures().called("gds.beta.graph.relationships.toUndirected");
        executionContext()
            .log()
            .warn("Procedure `gds.beta.graph.relationships.toUndirected` has been deprecated, please use `gds.graph.relationships.toUndirected`.");
        return mutate(graphName, configuration);
    }

    @Procedure(value = ToUndirectedSpec.CALLABLE_NAME, mode = READ)
    @Description(ToUndirectedSpec.DESCRIPTION)
    public Stream<ToUndirectedSpec.MutateResult> mutate(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        var mutateSpec = new ToUndirectedSpec();
        var pipelineSpec = new ProcedureExecutorSpec<ToUndirected, SingleTypeRelationships, ToUndirectedConfig>();

        return new ProcedureExecutor<>(
            mutateSpec,
            pipelineSpec,
            executionContext()
        ).compute(graphName, configuration);
    }

    @Internal
    @Deprecated(forRemoval = true)
    @Procedure(
        value = "gds.beta.graph.relationships.toUndirected.estimate", mode = READ,
        deprecatedBy = ToUndirectedSpec.CALLABLE_NAME + ".estimate"
    )
    @Description(ESTIMATE_DESCRIPTION)
    public Stream<MemoryEstimateResult> estimateDeprecated(
        @Name(value = "graphNameOrConfiguration") Object graphNameOrConfiguration,
        @Name(value = "algoConfiguration") Map<String, Object> algoConfiguration
    ) {
        executionContext()
            .metricsFacade()
            .deprecatedProcedures().called("gds.beta.graph.relationships.toUndirected.estimate");
        executionContext()
            .log()
            .warn("Procedure `gds.beta.graph.relationships.toUndirected.estimate` has been deprecated, please use `gds.graph.relationships.toUndirected.estimate`.");
        return estimate(graphNameOrConfiguration, algoConfiguration);
    }

    @Procedure(value = ToUndirectedSpec.CALLABLE_NAME + ".estimate", mode = READ)
    @Description(ESTIMATE_DESCRIPTION)
    public Stream<MemoryEstimateResult> estimate(
        @Name(value = "graphNameOrConfiguration") Object graphNameOrConfiguration,
        @Name(value = "algoConfiguration") Map<String, Object> algoConfiguration
    ) {
        var mutateSpec = new ToUndirectedSpec();
        var pipelineSpec = new ProcedureExecutorSpec<ToUndirected, SingleTypeRelationships, ToUndirectedConfig>();

        return new MemoryEstimationExecutor<>(
            mutateSpec,
            pipelineSpec,
            executionContext(),
            transactionContext()
        ).computeEstimate(graphNameOrConfiguration, algoConfiguration);
    }
}
