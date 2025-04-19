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
package org.neo4j.gds.config;

import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.annotation.Configuration;
import org.neo4j.gds.api.GraphStore;

import java.util.Collection;
import java.util.Set;

import static org.neo4j.gds.config.ConfigNodesValidations.nodesExistInGraph;
import static org.neo4j.gds.config.ConfigNodesValidations.nodesNotNegative;
import static org.neo4j.gds.config.NodeIdParser.parseToSingleNodeId;

public interface TargetNodeConfig {

    String TARGET_NODE_KEY = "targetNode";

    @Configuration.ConvertWith(method = "org.neo4j.gds.config.TargetNodeConfig#parseTargetNode")
    long targetNode();

    static long parseTargetNode(Object input) {
        var node = parseToSingleNodeId(input, TARGET_NODE_KEY);
        nodesNotNegative(Set.of(node), TARGET_NODE_KEY);
        return node;
    }

    @Configuration.GraphStoreValidationCheck
    default void validateTargetNode(
        GraphStore graphStore,
        Collection<NodeLabel> selectedLabels,
        Collection<RelationshipType> selectedRelationshipTypes
    ) {
        nodesExistInGraph(graphStore, selectedLabels, Set.of(targetNode()), TARGET_NODE_KEY);
    }
}
