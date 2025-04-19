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
package org.neo4j.gds.procedures.algorithms.community;

import org.neo4j.gds.applications.algorithms.machinery.AlgorithmProcessingTimings;
import org.neo4j.gds.result.AbstractResultBuilder;
import org.neo4j.gds.procedures.algorithms.results.StandardWriteResult;

import java.util.Map;

public class KCoreDecompositionWriteResult extends StandardWriteResult {
    public final long nodePropertiesWritten;
    public final long degeneracy;

    KCoreDecompositionWriteResult(
        long nodePropertiesWritten,
        long degeneracy,
        long preProcessingMillis,
        long computeMillis,
        long postProcessingMillis,
        long writeMillis,
        Map<String, Object> configuration
    ) {
        super(preProcessingMillis, computeMillis, postProcessingMillis, writeMillis, configuration);
        this.nodePropertiesWritten = nodePropertiesWritten;
        this.degeneracy = degeneracy;
    }

    static KCoreDecompositionWriteResult emptyFrom(
        AlgorithmProcessingTimings timings,
        Map<String, Object> configurationMap
    ) {
        return new KCoreDecompositionWriteResult(
            0,
            0,
            timings.preProcessingMillis,
            timings.computeMillis,
            0,
            timings.mutateOrWriteMillis,
            configurationMap
        );
    }

    public static final class Builder extends AbstractResultBuilder<KCoreDecompositionWriteResult> {
        private long degeneracy;

        public Builder withDegeneracy(long degeneracy) {
            this.degeneracy = degeneracy;
            return this;
        }

        public KCoreDecompositionWriteResult build() {
            return new KCoreDecompositionWriteResult(
                nodePropertiesWritten,
                degeneracy,
                preProcessingMillis,
                computeMillis,
                -1L,
                writeMillis,
                config.toMap()
            );
        }
    }
}
