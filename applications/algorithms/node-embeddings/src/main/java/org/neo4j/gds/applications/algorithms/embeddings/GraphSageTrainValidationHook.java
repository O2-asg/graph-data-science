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
package org.neo4j.gds.applications.algorithms.embeddings;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.core.concurrency.DefaultPool;
import org.neo4j.gds.core.loading.PostLoadValidationHook;
import org.neo4j.gds.embeddings.graphsage.algo.GraphSageTrainConfig;
import org.neo4j.gds.ml.core.EmbeddingUtils;

class GraphSageTrainValidationHook implements PostLoadValidationHook {
    private final GraphSageTrainConfig configuration;

    GraphSageTrainValidationHook(GraphSageTrainConfig configuration) {
        this.configuration = configuration;
    }

    @Override
    public void onGraphStoreLoaded(GraphStore graphStore) {
        // do nothing, or rather, it is automatic
    }

    @Override
    public void onGraphLoaded(Graph graph) {
        if (!graph.hasRelationshipProperty()) return;

        EmbeddingUtils.validateRelationshipWeightPropertyValue(
            graph,
            configuration.concurrency(),
            DefaultPool.INSTANCE
        );
    }
}
