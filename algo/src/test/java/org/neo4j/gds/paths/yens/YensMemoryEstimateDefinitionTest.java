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
package org.neo4j.gds.paths.yens;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.gds.assertions.MemoryEstimationAssert;
import org.neo4j.gds.core.concurrency.Concurrency;

import java.util.stream.Stream;

class YensMemoryEstimateDefinitionTest {

    static Stream<Arguments> expectedMemoryEstimation() {
        return Stream.of(
            Arguments.of(1_000, 3, 1, 56976),
            Arguments.of(1_000, 3, 4, 227784),

            Arguments.of(1_000_000, 3, 1, 56_125_848L),
            Arguments.of(1_000_000, 3, 4, 224_503_272L),

            Arguments.of(1_000_000_000, 3, 1, 56_133_545_944L),
            Arguments.of(1_000_000_000, 3, 4, 224_534_183_656L)

        );
    }

    @ParameterizedTest
    @MethodSource("expectedMemoryEstimation")
    void shouldComputeMemoryEstimation(
        int nodeCount,
        int numberOfShortestPathsToFind,
        int concurrency,
        long expectedBytes
    ) {
        var memoryEstimation= new YensMemoryEstimateDefinition(numberOfShortestPathsToFind)
            .memoryEstimation();

        MemoryEstimationAssert.assertThat(memoryEstimation)
            .memoryRange(nodeCount, new Concurrency(concurrency))
            .hasSameMinAndMaxEqualTo(expectedBytes);
    }

}
