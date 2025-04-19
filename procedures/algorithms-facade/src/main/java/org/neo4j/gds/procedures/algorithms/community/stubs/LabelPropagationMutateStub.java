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
package org.neo4j.gds.procedures.algorithms.community.stubs;

import org.neo4j.gds.api.ProcedureReturnColumns;
import org.neo4j.gds.applications.ApplicationsFacade;
import org.neo4j.gds.applications.algorithms.community.CommunityAlgorithmsEstimationModeBusinessFacade;
import org.neo4j.gds.applications.algorithms.machinery.MemoryEstimateResult;
import org.neo4j.gds.labelpropagation.LabelPropagationMutateConfig;
import org.neo4j.gds.mem.MemoryEstimation;
import org.neo4j.gds.procedures.algorithms.community.LabelPropagationMutateResult;
import org.neo4j.gds.procedures.algorithms.community.ProcedureStatisticsComputationInstructions;
import org.neo4j.gds.procedures.algorithms.stubs.GenericStub;
import org.neo4j.gds.procedures.algorithms.stubs.MutateStub;

import java.util.Map;
import java.util.stream.Stream;

public class LabelPropagationMutateStub implements MutateStub<LabelPropagationMutateConfig, LabelPropagationMutateResult> {
    private final GenericStub genericStub;
    private final ApplicationsFacade applicationsFacade;
    private final ProcedureReturnColumns procedureReturnColumns;

    public LabelPropagationMutateStub(
        GenericStub genericStub,
        ApplicationsFacade applicationsFacade,
        ProcedureReturnColumns procedureReturnColumns
    ) {
        this.genericStub = genericStub;
        this.applicationsFacade = applicationsFacade;
        this.procedureReturnColumns = procedureReturnColumns;
    }

    @Override
    public LabelPropagationMutateConfig parseConfiguration(Map<String, Object> configuration) {
        return genericStub.parseConfiguration(LabelPropagationMutateConfig::of, configuration);
    }

    @Override
    public MemoryEstimation getMemoryEstimation(String username, Map<String, Object> rawConfiguration) {
        return genericStub.getMemoryEstimation(
            username,
            rawConfiguration,
            LabelPropagationMutateConfig::of,
            __ -> estimationMode().labelPropagation()
        );
    }

    @Override
    public Stream<MemoryEstimateResult> estimate(Object graphName, Map<String, Object> rawConfiguration) {
        return genericStub.estimate(
            graphName,
            rawConfiguration,
            LabelPropagationMutateConfig::of,
            __ -> estimationMode().labelPropagation()
        );
    }

    @Override
    public Stream<LabelPropagationMutateResult> execute(
        String graphNameAsString,
        Map<String, Object> rawConfiguration
    ) {
        var statisticsComputationInstructions = ProcedureStatisticsComputationInstructions.forCommunities(
            procedureReturnColumns);
        var resultBuilder = new LabelPropagationResultBuilderForMutateMode(statisticsComputationInstructions);

        return genericStub.execute(
            graphNameAsString,
            rawConfiguration,
            LabelPropagationMutateConfig::of,
            applicationsFacade.community().mutate()::labelPropagation,
            resultBuilder
        );
    }

    private CommunityAlgorithmsEstimationModeBusinessFacade estimationMode() {
        return applicationsFacade.community().estimate();
    }
}
