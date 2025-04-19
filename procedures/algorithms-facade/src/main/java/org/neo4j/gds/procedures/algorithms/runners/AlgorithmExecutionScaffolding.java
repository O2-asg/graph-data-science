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
package org.neo4j.gds.procedures.algorithms.runners;

import org.neo4j.gds.applications.algorithms.machinery.ResultBuilder;
import org.neo4j.gds.config.AlgoBaseConfig;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.procedures.algorithms.AlgorithmHandle;
import org.neo4j.gds.procedures.algorithms.configuration.ConfigurationValidationHook;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public interface AlgorithmExecutionScaffolding {
    /**
     * A convenient overload for the 99% of cases where we do not do extra configuration validation
     */
    <CONFIGURATION extends AlgoBaseConfig, RESULT_FROM_ALGORITHM, RESULT_TO_CALLER, MUTATE_OR_WRITE_METADATA> RESULT_TO_CALLER runAlgorithm(
        String graphNameAsString,
        Map<String, Object> rawConfiguration,
        Function<CypherMapWrapper, CONFIGURATION> configurationParser,
        AlgorithmHandle<CONFIGURATION, RESULT_FROM_ALGORITHM, RESULT_TO_CALLER, MUTATE_OR_WRITE_METADATA> algorithm,
        ResultBuilder<CONFIGURATION, RESULT_FROM_ALGORITHM, RESULT_TO_CALLER, MUTATE_OR_WRITE_METADATA> resultBuilder
    );

    /**
     * Just some scaffolding: parsing graph name, parsing configuration, then delegating
     */
    <CONFIGURATION extends AlgoBaseConfig, RESULT_FROM_ALGORITHM, RESULT_TO_CALLER, MUTATE_OR_WRITE_METADATA> RESULT_TO_CALLER runAlgorithmWithValidation(
        String graphNameAsString,
        Map<String, Object> rawConfiguration,
        Function<CypherMapWrapper, CONFIGURATION> configurationParser,
        Optional<ConfigurationValidationHook<CONFIGURATION>> configurationValidation,
        AlgorithmHandle<CONFIGURATION, RESULT_FROM_ALGORITHM, RESULT_TO_CALLER, MUTATE_OR_WRITE_METADATA> algorithm,
        ResultBuilder<CONFIGURATION, RESULT_FROM_ALGORITHM, RESULT_TO_CALLER, MUTATE_OR_WRITE_METADATA> resultBuilder
    );
}
