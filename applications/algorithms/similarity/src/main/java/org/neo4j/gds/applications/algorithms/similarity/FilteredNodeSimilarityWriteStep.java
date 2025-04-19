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
package org.neo4j.gds.applications.algorithms.similarity;

import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.gds.algorithms.similarity.WriteRelationshipService;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.api.ResultStore;
import org.neo4j.gds.applications.algorithms.machinery.MutateOrWriteStep;
import org.neo4j.gds.applications.algorithms.metadata.RelationshipsWritten;
import org.neo4j.gds.core.utils.progress.JobId;
import org.neo4j.gds.similarity.filterednodesim.FilteredNodeSimilarityWriteConfig;
import org.neo4j.gds.similarity.nodesim.NodeSimilarityResult;

import java.util.Map;

import static org.neo4j.gds.applications.algorithms.metadata.LabelForProgressTracking.FilteredNodeSimilarity;

final class FilteredNodeSimilarityWriteStep implements MutateOrWriteStep<NodeSimilarityResult, Pair<RelationshipsWritten, Map<String, Object>>> {
    private final SimilarityWrite similarityWrite;
    private final FilteredNodeSimilarityWriteConfig configuration;
    private final boolean shouldComputeSimilarityDistribution;

    private FilteredNodeSimilarityWriteStep(
        SimilarityWrite similarityWrite,
        FilteredNodeSimilarityWriteConfig configuration,
        boolean shouldComputeSimilarityDistribution
    ) {
        this.similarityWrite = similarityWrite;
        this.configuration = configuration;
        this.shouldComputeSimilarityDistribution = shouldComputeSimilarityDistribution;
    }

    static FilteredNodeSimilarityWriteStep create(
        WriteRelationshipService writeRelationshipService,
        FilteredNodeSimilarityWriteConfig configuration,
        boolean shouldComputeSimilarityDistribution
    ) {
        var similarityWrite = new SimilarityWrite(writeRelationshipService);

        return new FilteredNodeSimilarityWriteStep(
            similarityWrite,
            configuration,
            shouldComputeSimilarityDistribution
        );
    }

    @Override
    public Pair<RelationshipsWritten, Map<String, Object>> execute(
        Graph graph,
        GraphStore graphStore,
        ResultStore resultStore,
        NodeSimilarityResult result,
        JobId jobId
    ) {
        return similarityWrite.execute(
            graphStore,
            configuration,
            configuration,
            configuration,
            shouldComputeSimilarityDistribution,
            configuration.resolveResultStore(resultStore),
            FilteredNodeSimilarity,
            result.graphResult(),
            configuration.jobId()
        );
    }
}
