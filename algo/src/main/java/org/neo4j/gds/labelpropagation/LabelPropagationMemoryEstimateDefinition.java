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
package org.neo4j.gds.labelpropagation;

import com.carrotsearch.hppc.LongDoubleScatterMap;
import org.neo4j.gds.mem.MemoryEstimateDefinition;
import org.neo4j.gds.collections.ha.HugeLongArray;
import org.neo4j.gds.mem.MemoryEstimation;
import org.neo4j.gds.mem.MemoryEstimations;
import org.neo4j.gds.mem.MemoryRange;
import org.neo4j.gds.mem.Estimate;

import static org.neo4j.gds.mem.Estimate.sizeOfDoubleArray;
import static org.neo4j.gds.mem.Estimate.sizeOfLongArray;

public class LabelPropagationMemoryEstimateDefinition implements MemoryEstimateDefinition {

    @Override
    public MemoryEstimation memoryEstimation() {
        return MemoryEstimations.builder(LabelPropagation.class)
            .perNode("labels", HugeLongArray::memoryEstimation)
            .perThread("votes", MemoryEstimations.builder()
                .field("init step", InitStep.class)
                .field("compute step", ComputeStep.class)
                .field("step runner", StepRunner.class)
                .field("compute step consumer", ComputeStepConsumer.class)
                .field("votes container", LongDoubleScatterMap.class)
                .rangePerNode("votes", nodeCount -> {
                    long minBufferSize = Estimate.sizeOfEmptyOpenHashContainer();
                    long maxBufferSize = Estimate.sizeOfOpenHashContainer(nodeCount);
                    if (maxBufferSize < minBufferSize) {
                        maxBufferSize = minBufferSize;
                    }
                    long min = sizeOfLongArray(minBufferSize) + sizeOfDoubleArray(minBufferSize);
                    long max = sizeOfLongArray(maxBufferSize) + sizeOfDoubleArray(maxBufferSize);
                    return MemoryRange.of(min, max);
                }).build())
            .build();
    }

}
