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

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.applications.algorithms.similarity.SimilarityResultStreamDelegate;
import org.neo4j.gds.config.ConcurrencyConfig;
import org.neo4j.gds.result.SimilarityStatistics;
import org.neo4j.gds.similarity.SimilarityGraphResult;
import org.neo4j.gds.similarity.SimilarityResult;

import java.util.Map;
import java.util.stream.Stream;

class SimilarityStatsProcessor {
    final SimilarityResultStreamDelegate similarityResultStreamDelegate = new SimilarityResultStreamDelegate();

    Map<String, Object> computeSimilarityDistribution(
        Graph graph,
        ConcurrencyConfig concurrencyConfiguration,
        Stream<SimilarityResult> similarityResultStream,
        boolean shouldComputeSimilarityDistribution
    ) {
        var similarityGraphResult = similarityResultStreamDelegate.computeSimilarityGraph(
            graph,
            concurrencyConfiguration.concurrency(),
            similarityResultStream
        );

        return computeSimilarityDistribution(shouldComputeSimilarityDistribution, similarityGraphResult);
    }

    Map<String, Object> computeSimilarityDistribution(
        boolean shouldComputeSimilarityDistribution,
        SimilarityGraphResult similarityGraphResult
    ) {
        var similarityStatistics = computeSimilarityStatistics(
            similarityGraphResult,
            shouldComputeSimilarityDistribution
        );

        return SimilarityStatistics.similaritySummary(similarityStatistics.histogram(),similarityStatistics.success());
    }

    SimilarityStatistics.SimilarityStats computeSimilarityStatistics(
        SimilarityGraphResult graphResult,
        boolean shouldComputeSimilarityDistribution
    ) {
        return SimilarityStatistics.similarityStats(
            graphResult::similarityGraph,
            shouldComputeSimilarityDistribution
        );
    }
}
