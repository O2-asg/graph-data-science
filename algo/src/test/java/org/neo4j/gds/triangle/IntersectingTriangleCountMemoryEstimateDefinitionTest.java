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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.gds.assertions.MemoryEstimationAssert;
import org.neo4j.gds.core.GraphDimensions;
import org.neo4j.gds.core.ImmutableGraphDimensions;
import org.neo4j.gds.core.concurrency.Concurrency;
import org.neo4j.gds.mem.MemoryEstimation;

class IntersectingTriangleCountMemoryEstimateDefinitionTest {

    @ValueSource(longs = {1L, 10L, 100L, 10_000L})
    @ParameterizedTest
    void memoryEstimation(long nodeCount) {
        MemoryEstimation memoryEstimation =
            new IntersectingTriangleCountMemoryEstimateDefinition().memoryEstimation();

        GraphDimensions graphDimensions = ImmutableGraphDimensions.builder().nodeCount(nodeCount).build();

        long hugeAtomicLongArray = 24 + nodeCount * 8 + 16;
        long expected = 72 + hugeAtomicLongArray;

        MemoryEstimationAssert.assertThat(memoryEstimation)
            .memoryRange(graphDimensions, new Concurrency(1))
            .hasSameMinAndMaxEqualTo(expected);
    }

    @CsvSource({"1000000000, 8001220736", "100000000000, 800122070336"})
    @ParameterizedTest
    void memoryEstimationLargePages(long nodeCount, long sizeOfHugeArray) {
        MemoryEstimation memoryEstimation =
            new IntersectingTriangleCountMemoryEstimateDefinition().memoryEstimation();

        GraphDimensions graphDimensions = ImmutableGraphDimensions.builder().nodeCount(nodeCount).build();

        long hugeAtomicLongArray = 32 + sizeOfHugeArray;
        long expected = 72 + hugeAtomicLongArray;

        MemoryEstimationAssert.assertThat(memoryEstimation)
            .memoryRange(graphDimensions, new Concurrency(1))
            .hasSameMinAndMaxEqualTo(expected);

    }
}
