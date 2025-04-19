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
package org.neo4j.gds.applications.algorithms.machinery;

/**
 * This guy gathers timings, builder-stylee.
 */
class AlgorithmProcessingTimingsBuilder {
    // This is a marker
    private static final int NOT_AVAILABLE = -1;

    // timings
    protected long preProcessingMillis = NOT_AVAILABLE;
    protected long computeMillis = NOT_AVAILABLE;
    protected long mutateOrWriteMillis = NOT_AVAILABLE;

    public void withPreProcessingMillis(long preProcessingMillis) {
        this.preProcessingMillis = preProcessingMillis;
    }

    public void withComputeMillis(long computeMillis) {
        this.computeMillis = computeMillis;
    }

    public void withMutateOrWriteMillis(long mutateOrWriteMillis) {
        this.mutateOrWriteMillis = mutateOrWriteMillis;
    }

    AlgorithmProcessingTimings build() {
        return new AlgorithmProcessingTimings(
            preProcessingMillis,
            computeMillis,
            mutateOrWriteMillis
        );
    }
}
