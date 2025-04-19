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

import org.neo4j.gds.NullComputationResultConsumer;
import org.neo4j.gds.executor.AlgorithmSpec;
import org.neo4j.gds.executor.ComputationResultConsumer;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.procedures.algorithms.configuration.NewConfigFunction;
import org.neo4j.gds.procedures.algorithms.miscellaneous.ScalePropertiesStatsResult;
import org.neo4j.gds.scaleproperties.ScaleProperties;
import org.neo4j.gds.scaleproperties.ScalePropertiesFactory;
import org.neo4j.gds.scaleproperties.ScalePropertiesResult;
import org.neo4j.gds.scaleproperties.ScalePropertiesStatsConfig;

import java.util.stream.Stream;

import static org.neo4j.gds.executor.ExecutionMode.STREAM;
import static org.neo4j.gds.scaling.ScalePropertiesProc.SCALE_PROPERTIES_DESCRIPTION;
import static org.neo4j.gds.scaling.ScalePropertiesProc.validateLegacyScalers;

@GdsCallable(name = "gds.scaleProperties.stats", description = SCALE_PROPERTIES_DESCRIPTION, executionMode = STREAM)
public class ScalePropertiesStatsSpec implements AlgorithmSpec<ScaleProperties, ScalePropertiesResult, ScalePropertiesStatsConfig, Stream<ScalePropertiesStatsResult>, ScalePropertiesFactory<ScalePropertiesStatsConfig>> {
    @Override
    public String name() {
        return "ScalePropertiesStats";
    }

    @Override
    public ScalePropertiesFactory<ScalePropertiesStatsConfig> algorithmFactory(ExecutionContext executionContext) {
        return new ScalePropertiesFactory<>();
    }

    @Override
    public NewConfigFunction<ScalePropertiesStatsConfig> newConfigFunction() {
        return (__, userInput) -> {
            var config = ScalePropertiesStatsConfig.of(userInput);
            validateLegacyScalers(config, false);
            return config;
        };
    }

    @Override
    public ComputationResultConsumer<ScaleProperties, ScalePropertiesResult, ScalePropertiesStatsConfig, Stream<ScalePropertiesStatsResult>> computationResultConsumer() {
        return new NullComputationResultConsumer<>();
    }
}
