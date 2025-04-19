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
import java.util.Collections;
import java.util.List;

import static org.neo4j.gds.config.ConfigNodesValidations.nodesExistInGraph;
import static org.neo4j.gds.config.ConfigNodesValidations.nodesNotNegative;

public interface TargetNodesConfig {

    String TARGET_NODES_KEY = "targetNodes";

    @Configuration.ConvertWith(method = "org.neo4j.gds.config.TargetNodesConfig#parseTargetNodes")
    default List<Long> targetNodes() {
        return Collections.emptyList();
    }

    @Configuration.Ignore
    default boolean hasTargetNodes() {
        return !targetNodes().isEmpty();
    }

    static List<Long> parseTargetNodes(Object input) {
        var nodes = NodeIdParser.parseToListOfNodeIds(input, TARGET_NODES_KEY);
        nodesNotNegative(nodes, TARGET_NODES_KEY);
        return nodes;
    }

    @Configuration.GraphStoreValidationCheck
    default void validateTargetNodes(
        GraphStore graphStore,
        Collection<NodeLabel> selectedLabels,
        Collection<RelationshipType> selectedRelationshipTypes
    ) {
        nodesExistInGraph(graphStore, selectedLabels, targetNodes(), TARGET_NODES_KEY);
    }
}
