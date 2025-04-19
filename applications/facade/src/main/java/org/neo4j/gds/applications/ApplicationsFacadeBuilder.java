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
package org.neo4j.gds.applications;

import org.neo4j.gds.applications.algorithms.centrality.CentralityApplications;
import org.neo4j.gds.applications.algorithms.community.CommunityApplications;
import org.neo4j.gds.applications.algorithms.embeddings.NodeEmbeddingApplications;
import org.neo4j.gds.applications.algorithms.miscellaneous.MiscellaneousApplications;
import org.neo4j.gds.applications.algorithms.pathfinding.PathFindingApplications;
import org.neo4j.gds.applications.algorithms.similarity.SimilarityApplications;
import org.neo4j.gds.applications.graphstorecatalog.GraphCatalogApplications;
import org.neo4j.gds.applications.modelcatalog.ModelCatalogApplications;
import org.neo4j.gds.applications.operations.OperationsApplications;

/**
 * This is a helper that makes it easy to inject constituents, and to not have to inject all of them.
 */
public class ApplicationsFacadeBuilder {
    private CentralityApplications centralityApplications;
    private CommunityApplications communityApplications;
    private GraphCatalogApplications graphCatalogApplications;
    private MiscellaneousApplications miscellaneousApplications;
    private ModelCatalogApplications modelCatalogApplications;
    private NodeEmbeddingApplications nodeEmbeddingApplications;
    private OperationsApplications operationsApplications;
    private PathFindingApplications pathFindingApplications;
    private SimilarityApplications similarityApplications;

    public ApplicationsFacadeBuilder with(CentralityApplications centralityApplications) {
        this.centralityApplications = centralityApplications;
        return this;
    }

    public ApplicationsFacadeBuilder with(CommunityApplications communityApplications) {
        this.communityApplications = communityApplications;
        return this;
    }

    public ApplicationsFacadeBuilder with(GraphCatalogApplications graphCatalogApplications) {
        this.graphCatalogApplications = graphCatalogApplications;
        return this;
    }

    public ApplicationsFacadeBuilder with(MiscellaneousApplications miscellaneousApplications) {
        this.miscellaneousApplications = miscellaneousApplications;
        return this;
    }

    public ApplicationsFacadeBuilder with(ModelCatalogApplications modelCatalogApplications) {
        this.modelCatalogApplications = modelCatalogApplications;
        return this;
    }

    public ApplicationsFacadeBuilder with(NodeEmbeddingApplications nodeEmbeddingApplications) {
        this.nodeEmbeddingApplications = nodeEmbeddingApplications;
        return this;
    }

    public ApplicationsFacadeBuilder with(OperationsApplications operationsApplications) {
        this.operationsApplications = operationsApplications;
        return this;
    }

    public ApplicationsFacadeBuilder with(PathFindingApplications pathFindingApplications) {
        this.pathFindingApplications = pathFindingApplications;
        return this;
    }

    public ApplicationsFacadeBuilder with(SimilarityApplications similarityApplications) {
        this.similarityApplications = similarityApplications;
        return this;
    }

    public ApplicationsFacade build() {
        return new ApplicationsFacade(
            centralityApplications,
            communityApplications,
            graphCatalogApplications,
            miscellaneousApplications,
            modelCatalogApplications,
            nodeEmbeddingApplications,
            operationsApplications,
            pathFindingApplications,
            similarityApplications
        );
    }
}
