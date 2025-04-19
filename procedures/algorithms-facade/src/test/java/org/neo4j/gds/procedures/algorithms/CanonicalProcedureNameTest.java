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
package org.neo4j.gds.procedures.algorithms;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CanonicalProcedureNameTest {
    @Test
    void shouldNormalizeInput() {
        assertNormalizesProperly("gds.shortestpath.dijkstra");
        assertNormalizesProperly("gds.shortestPath.dijkstra.mutate");
        assertNormalizesProperly("GDS.SHORTESTPATH.DiJkStRa");
        assertNormalizesProperly("shortestPath.dijkstra");
        assertNormalizesProperly("gds.shortestPath.dijkstra");
    }

    @Test
    void shouldRetainRawInput() {
        assertEquals(CanonicalProcedureName.parse("gds.shortestpath.dijkstra").getRawForm(), "gds.shortestpath.dijkstra");
        assertEquals(CanonicalProcedureName.parse("gds.shortestPath.dijkstra.mutate").getRawForm(), "gds.shortestPath.dijkstra.mutate");
        assertEquals(CanonicalProcedureName.parse("GDS.SHORTESTPATH.DiJkStRa").getRawForm(), "GDS.SHORTESTPATH.DiJkStRa");
        assertEquals(CanonicalProcedureName.parse("shortestPath.dijkstra").getRawForm(), "shortestPath.dijkstra");
        assertEquals(CanonicalProcedureName.parse("gds.shortestPath.dijkstra").getRawForm(), "gds.shortestPath.dijkstra");
    }

    private static void assertNormalizesProperly(String input) {
        assertThat(CanonicalProcedureName.parse(input).getNormalisedForm()).isEqualTo("gds.shortestpath.dijkstra");
    }
}
