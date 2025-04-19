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
package org.neo4j.gds.wcc;

import org.neo4j.gds.MutateNodePropertyListFunction;
import org.neo4j.gds.MutatePropertyComputationResultConsumer;
import org.neo4j.gds.core.utils.paged.dss.DisjointSetStruct;
import org.neo4j.gds.core.write.ImmutableNodeProperty;
import org.neo4j.gds.executor.AlgorithmSpec;
import org.neo4j.gds.executor.ComputationResult;
import org.neo4j.gds.executor.ComputationResultConsumer;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.procedures.algorithms.community.WccMutateResult;
import org.neo4j.gds.procedures.algorithms.configuration.NewConfigFunction;
import org.neo4j.gds.result.AbstractCommunityResultBuilder;

import java.util.List;
import java.util.stream.Stream;

import static org.neo4j.gds.executor.ExecutionMode.MUTATE_NODE_PROPERTY;
import static org.neo4j.gds.wcc.WccSpecification.WCC_DESCRIPTION;

@GdsCallable(name = "gds.wcc.mutate", description = WCC_DESCRIPTION, executionMode = MUTATE_NODE_PROPERTY)
public class WccMutateSpecification implements AlgorithmSpec<Wcc, DisjointSetStruct, WccMutateConfig, Stream<WccMutateResult>, WccAlgorithmFactory<WccMutateConfig>> {

    public WccMutateSpecification() {}

    @Override
    public String name() {
        return "WccMutate";
    }

    @Override
    public WccAlgorithmFactory<WccMutateConfig> algorithmFactory(ExecutionContext executionContext) {
        return new WccAlgorithmFactory<>();
    }

    @Override
    public NewConfigFunction<WccMutateConfig> newConfigFunction() {
        return (__, config) -> WccMutateConfig.of(config);
    }

    @Override
    public ComputationResultConsumer<Wcc, DisjointSetStruct, WccMutateConfig, Stream<WccMutateResult>> computationResultConsumer() {
        MutateNodePropertyListFunction<Wcc, DisjointSetStruct, WccMutateConfig> mutateConfigNodePropertyListFunction = (computationResult) -> List.of(
            ImmutableNodeProperty.of(
                computationResult.config().mutateProperty(),
                WccSpecification.mutateNodeProperties(
                    computationResult
                )
            )
        );
        return new MutatePropertyComputationResultConsumer<>(mutateConfigNodePropertyListFunction, this::resultBuilder);
    }

    private AbstractCommunityResultBuilder<WccMutateResult> resultBuilder(
        ComputationResult<Wcc, DisjointSetStruct, WccMutateConfig> computationResult,
        ExecutionContext executionContext
    ) {
        return WccSpecification.resultBuilder(
            new WccMutateResult.Builder(
                executionContext.returnColumns(),
                computationResult.config().concurrency()
            ),
            computationResult
        );
    }

}
