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
package org.neo4j.gds.procedures.algorithms.similarity;

import org.neo4j.gds.applications.algorithms.machinery.AlgorithmProcessingTimings;

import java.util.Collections;
import java.util.Map;

public class SimilarityStatsResult {
    public long preProcessingMillis;
    public long computeMillis;
    public long postProcessingMillis;

    public long nodesCompared;
    public long similarityPairs;
    public Map<String, Object> similarityDistribution;
    public Map<String, Object> configuration;

    public SimilarityStatsResult(
        long preProcessingMillis,
        long computeMillis,
        long postProcessingMillis,
        long nodesCompared,
        long similarityPairs,
        Map<String, Object> similarityDistribution,
        Map<String, Object> configuration

    ) {
        this.preProcessingMillis = preProcessingMillis;
        this.computeMillis = computeMillis;
        this.postProcessingMillis = postProcessingMillis;
        this.nodesCompared = nodesCompared;
        this.similarityPairs = similarityPairs;
        this.similarityDistribution = similarityDistribution;
        this.configuration = configuration;
    }

    static SimilarityStatsResult emptyFrom(AlgorithmProcessingTimings timings, Map<String, Object> configurationMap) {
        return new SimilarityStatsResult(
            timings.preProcessingMillis,
            timings.computeMillis,
            timings.mutateOrWriteMillis,
            0,
            0,
            Collections.emptyMap(),
            configurationMap
        );
    }
}
