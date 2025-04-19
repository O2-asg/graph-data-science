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
package org.neo4j.gds.triangle;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.core.concurrency.Concurrency;
import org.neo4j.gds.core.concurrency.DefaultPool;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.termination.TerminationFlag;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@GdlExtension
class IntersectingTriangleCountFilteredGraphTest {

    @GdlGraph(orientation = Orientation.UNDIRECTED)
    static final String DB_CYPHER = "CREATE " +
                                    "  (a1:A)" +
                                    ", (a2:A)" +
                                    ", (a3:A)" +
                                    ", (b1:B)" +
                                    ", (b2:B)" +
                                    ", (b3:B)" +

                                    ", (a1)-[:R1]->(a2)" +
                                    ", (a2)-[:R2]->(a3)" +
                                    ", (a1)-[:R1]->(a3)" +

                                    ", (b1)-[:R1]->(b2)" +
                                    ", (b2)-[:R2]->(b3)" +
                                    ", (b1)-[:R1]->(b3)";

    @Inject
    private GraphStore graphStore;


    @Test
    void testUnionGraphWithNodeFilter() {
        var graph = graphStore.getGraph(
            Collections.singletonList(NodeLabel.of("B")),
            List.of(
                RelationshipType.of("R1"),
                RelationshipType.of("R2")
            ),
            Optional.empty()
        );

        var triangleCount = IntersectingTriangleCount.create(
            graph,
            new Concurrency(4),
            Long.MAX_VALUE,
            DefaultPool.INSTANCE,
            ProgressTracker.NULL_TRACKER,
            TerminationFlag.RUNNING_TRUE
        );
        var triangleCountResult = triangleCount.compute();
        assertThat(triangleCountResult.globalTriangles()).isEqualTo(1);
        var triangles = triangleCountResult.localTriangles();
        assertThat(triangles.get(0)).isEqualTo(1);
        assertThat(triangles.get(1)).isEqualTo(1);
        assertThat(triangles.get(2)).isEqualTo(1);
    }
}
