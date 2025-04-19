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

import org.neo4j.gds.applications.algorithms.machinery.AlgorithmEstimationTemplate;
import org.neo4j.gds.applications.algorithms.machinery.MemoryEstimateResult;
import org.neo4j.gds.mem.MemoryEstimation;
import org.neo4j.gds.similarity.filteredknn.FilteredKnnBaseConfig;
import org.neo4j.gds.similarity.filteredknn.FilteredKnnMemoryEstimateDefinition;
import org.neo4j.gds.similarity.filterednodesim.FilteredNodeSimilarityBaseConfig;
import org.neo4j.gds.similarity.filterednodesim.FilteredNodeSimilarityMemoryEstimateDefinition;
import org.neo4j.gds.similarity.knn.KnnBaseConfig;
import org.neo4j.gds.similarity.knn.KnnMemoryEstimateDefinition;
import org.neo4j.gds.similarity.nodesim.NodeSimilarityBaseConfig;
import org.neo4j.gds.similarity.nodesim.NodeSimilarityMemoryEstimateDefinition;

public class SimilarityAlgorithmsEstimationModeBusinessFacade {
    private final AlgorithmEstimationTemplate algorithmEstimationTemplate;

    public SimilarityAlgorithmsEstimationModeBusinessFacade(AlgorithmEstimationTemplate algorithmEstimationTemplate) {
        this.algorithmEstimationTemplate = algorithmEstimationTemplate;
    }

    public MemoryEstimation filteredKnn(KnnBaseConfig configuration) {
        return new FilteredKnnMemoryEstimateDefinition(configuration.toMemoryEstimationParameters()).memoryEstimation();
    }

    public MemoryEstimateResult filteredKnn(FilteredKnnBaseConfig configuration, Object graphNameOrConfiguration) {
        var memoryEstimation = filteredKnn(configuration);

        return algorithmEstimationTemplate.estimate(
            configuration,
            graphNameOrConfiguration,
            memoryEstimation
        );
    }

    public MemoryEstimation filteredNodeSimilarity(NodeSimilarityBaseConfig configuration) {
        return new FilteredNodeSimilarityMemoryEstimateDefinition(configuration.toParameters()).memoryEstimation();
    }

    public MemoryEstimateResult filteredNodeSimilarity(
        FilteredNodeSimilarityBaseConfig configuration,
        Object graphNameOrConfiguration
    ) {
        var memoryEstimation = filteredNodeSimilarity(configuration);

        return algorithmEstimationTemplate.estimate(
            configuration,
            graphNameOrConfiguration,
            memoryEstimation
        );
    }

    public MemoryEstimation knn(KnnBaseConfig knnMutateConfig) {
        return new KnnMemoryEstimateDefinition(knnMutateConfig.toMemoryEstimationParameters()).memoryEstimation();
    }

    public MemoryEstimateResult knn(KnnBaseConfig configuration, Object graphNameOrConfiguration) {
        var memoryEstimation = knn(configuration);

        return algorithmEstimationTemplate.estimate(
            configuration,
            graphNameOrConfiguration,
            memoryEstimation
        );
    }

    public MemoryEstimation nodeSimilarity(NodeSimilarityBaseConfig configuration) {
        return new NodeSimilarityMemoryEstimateDefinition(configuration.toMemoryEstimateParameters()).memoryEstimation();
    }

    public MemoryEstimateResult nodeSimilarity(
        NodeSimilarityBaseConfig configuration,
        Object graphNameOrConfiguration
    ) {
        var memoryEstimation = nodeSimilarity(configuration);

        return algorithmEstimationTemplate.estimate(
            configuration,
            graphNameOrConfiguration,
            memoryEstimation
        );
    }
}
