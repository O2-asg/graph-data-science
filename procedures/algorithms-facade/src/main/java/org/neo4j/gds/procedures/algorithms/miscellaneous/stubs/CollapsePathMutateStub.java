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
package org.neo4j.gds.procedures.algorithms.miscellaneous.stubs;

import org.neo4j.gds.applications.ApplicationsFacade;
import org.neo4j.gds.applications.algorithms.machinery.MemoryEstimateResult;
import org.neo4j.gds.applications.algorithms.miscellaneous.MiscellaneousApplicationsEstimationModeBusinessFacade;
import org.neo4j.gds.mem.MemoryEstimation;
import org.neo4j.gds.procedures.algorithms.miscellaneous.CollapsePathMutateResult;
import org.neo4j.gds.procedures.algorithms.stubs.GenericStub;
import org.neo4j.gds.procedures.algorithms.stubs.MutateStub;
import org.neo4j.gds.walking.CollapsePathConfig;

import java.util.Map;
import java.util.stream.Stream;

public class CollapsePathMutateStub implements MutateStub<CollapsePathConfig, CollapsePathMutateResult> {
    private final GenericStub genericStub;
    private final ApplicationsFacade applicationsFacade;

    public CollapsePathMutateStub(GenericStub genericStub, ApplicationsFacade applicationsFacade) {
        this.genericStub = genericStub;
        this.applicationsFacade = applicationsFacade;
    }

    @Override
    public CollapsePathConfig parseConfiguration(Map<String, Object> configuration) {
        return genericStub.parseConfiguration(CollapsePathConfig::of, configuration);
    }

    @Override
    public MemoryEstimation getMemoryEstimation(String username, Map<String, Object> rawConfiguration) {
        return genericStub.getMemoryEstimation(
            username,
            rawConfiguration,
            CollapsePathConfig::of,
            __ -> estimationMode().collapsePath()
        );
    }

    @Override
    public Stream<MemoryEstimateResult> estimate(Object graphNameAsString, Map<String, Object> rawConfiguration) {
        return genericStub.estimate(
            graphNameAsString,
            rawConfiguration,
            CollapsePathConfig::of,
            __ -> estimationMode().collapsePath()
        );
    }

    @Override
    public Stream<CollapsePathMutateResult> execute(
        String graphNameAsString,
        Map<String, Object> rawConfiguration
    ) {
        var resultBuilder = new CollapsePathResultBuilderForMutateMode();

        return genericStub.execute(
            graphNameAsString,
            rawConfiguration,
            CollapsePathConfig::of,
            applicationsFacade.miscellaneous().mutate()::collapsePath,
            resultBuilder
        );
    }

    private MiscellaneousApplicationsEstimationModeBusinessFacade estimationMode() {
        return applicationsFacade.miscellaneous().estimate();
    }
}
