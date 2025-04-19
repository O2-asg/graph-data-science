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
package org.neo4j.gds.procedures.algorithms.embeddings;

import org.neo4j.gds.applications.algorithms.machinery.AlgorithmProcessingTimings;
import org.neo4j.gds.result.AbstractResultBuilder;

import java.util.Map;

public final class DefaultNodeEmbeddingsWriteResult {
    public final long nodeCount;
    public final long nodePropertiesWritten;
    public final long preProcessingMillis;
    public final long computeMillis;
    public final long writeMillis;
    public final Map<String, Object> configuration;

    public DefaultNodeEmbeddingsWriteResult(
        long nodeCount,
        long nodePropertiesWritten,
        long preProcessingMillis,
        long computeMillis,
        long writeMillis,
        Map<String, Object> configuration
    ) {
        this.nodeCount = nodeCount;
        this.nodePropertiesWritten = nodePropertiesWritten;
        this.preProcessingMillis = preProcessingMillis;
        this.computeMillis = computeMillis;
        this.writeMillis = writeMillis;
        this.configuration = configuration;
    }

    static DefaultNodeEmbeddingsWriteResult emptyFrom(
        AlgorithmProcessingTimings timings,
        Map<String, Object> configurationMap
    ) {
        return new DefaultNodeEmbeddingsWriteResult(
            0,
            0,
            timings.preProcessingMillis,
            timings.computeMillis,
            timings.mutateOrWriteMillis,
            configurationMap
        );
    }

    public static class Builder extends AbstractResultBuilder<DefaultNodeEmbeddingsWriteResult> {
        @Override
        public DefaultNodeEmbeddingsWriteResult build() {
            return new DefaultNodeEmbeddingsWriteResult(
                nodeCount,
                nodePropertiesWritten,
                preProcessingMillis,
                computeMillis,
                writeMillis,
                config.toMap()
            );
        }
    }
}
