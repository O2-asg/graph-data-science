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
package org.neo4j.gds.bridges;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.Inject;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@GdlExtension
class BridgesStreamConfigTest {

    @GdlGraph(orientation = Orientation.NATURAL)
    private static final String GRAPH =
        """
                CREATE
                    (a:N),
                    (b:N),
                    (a)-[:R]->(b)
            """;

    @Inject
    private GraphStore graphStore;

    @Test
    void shouldRaiseAnExceptionIfGraphIsNotUndirected() {
        var bridgesConfiguration = BridgesStreamConfig.of(CypherMapWrapper.empty());

        assertThatIllegalArgumentException()
            .isThrownBy(() ->
                bridgesConfiguration.validateTargetRelIsUndirected(
                    graphStore,
                    List.of(),
                    List.of(RelationshipType.of("R"))
                ))
            .withMessageContaining("Bridges requires relationship projections to be UNDIRECTED.");
    }
}
