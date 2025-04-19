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
package org.neo4j.gds.algorithms.community;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.applications.algorithms.machinery.MutateNodeProperty;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.api.properties.nodes.NodePropertyValuesAdapter;
import org.neo4j.gds.collections.ha.HugeLongArray;
import org.neo4j.gds.config.MutateNodePropertyConfig;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.logging.Log;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@GdlExtension
class MutateNodePropertyTest {
    @SuppressWarnings("unused")
    @GdlGraph
    private static final String GRAPH =
        "CREATE " +
            " (:Node), " +
            " (:Node), " +
            " (:Node), " +
            " (:Node), ";

    @SuppressWarnings("unused")
    @Inject
    private Graph graph;

    @SuppressWarnings("unused")
    @Inject
    private GraphStore graphStore;

    @Test
    void shouldMutateTheGraphStore() {
        assertThat(graphStore.hasNodeProperty("bugger-off"))
            .as("The graph store should not contain the node property we are about to add.")
            .isFalse();

        var values = HugeLongArray.newArray(graph.nodeCount());
        values.setAll(graph::toOriginalNodeId);
        var nodePropertyValuesToMutate = NodePropertyValuesAdapter.adapt(values);

        var mutateNodeProperty = new MutateNodeProperty(mock(Log.class));
        var result = mutateNodeProperty.mutateNodeProperties(
            graph,
            graphStore,
            new MutateNodePropertyConfig() {
                @Override
                public String mutateProperty() {
                    return "bugger-off";
                }

                @Override
                public Optional<String> usernameOverride() {
                    throw new UnsupportedOperationException("TODO");
                }
            },
            nodePropertyValuesToMutate
        );

        assertThat(result.value)
            .as("NodeProperties added count don't match")
            .isEqualTo(4);

        assertThat(graphStore.hasNodeProperty("bugger-off"))
            .as("The graph store should contain the node property we just added.")
            .isTrue();

        var addedNodePropertyValues = graphStore.nodeProperty("bugger-off").values();

        graph.forEachNode(nodeId -> {
            assertThat(addedNodePropertyValues.longValue(nodeId))
                .as("Mutated node property for `nodeId %s` doesn't have the expected value", nodeId)
                .isEqualTo(nodePropertyValuesToMutate.longValue(nodeId));
            return true;
        });
    }

}
