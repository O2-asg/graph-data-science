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
package org.neo4j.gds.similarity.knn;

import com.carrotsearch.hppc.LongArrayList;
import org.neo4j.gds.mem.MemoryEstimateDefinition;
import org.neo4j.gds.collections.ha.HugeObjectArray;
import org.neo4j.gds.mem.MemoryEstimation;
import org.neo4j.gds.mem.MemoryEstimations;
import org.neo4j.gds.mem.MemoryRange;

import java.util.function.LongFunction;

import static org.neo4j.gds.mem.Estimate.sizeOfInstance;
import static org.neo4j.gds.mem.Estimate.sizeOfIntArray;
import static org.neo4j.gds.mem.Estimate.sizeOfLongArray;
import static org.neo4j.gds.mem.Estimate.sizeOfOpenHashContainer;

public class KnnMemoryEstimateDefinition implements MemoryEstimateDefinition {

    private final KnnMemoryEstimationParametersBuilder parametersSansNodeCount;

    public KnnMemoryEstimateDefinition(KnnMemoryEstimationParametersBuilder parametersSansNodeCount) {
        this.parametersSansNodeCount = parametersSansNodeCount;
    }

    @Override
    public MemoryEstimation memoryEstimation() {
        return MemoryEstimations.setup(
            "Knn",
            (dim, concurrency) -> {
                var parameters = parametersSansNodeCount.build(dim.nodeCount());
                var k = parameters.k();
                LongFunction<MemoryRange> tempListEstimation = nodeCount -> MemoryRange.of(
                    HugeObjectArray.memoryEstimation(nodeCount, 0),
                    HugeObjectArray.memoryEstimation(
                        nodeCount,
                        sizeOfInstance(LongArrayList.class) + sizeOfLongArray(k.sampledValue)
                    )
                );

                var neighborListEstimate = NeighborList.memoryEstimation(k.value)
                    .estimate(dim, concurrency)
                    .memoryUsage();

                LongFunction<MemoryRange> perNodeNeighborListEstimate = nodeCount -> MemoryRange.of(
                    HugeObjectArray.memoryEstimation(nodeCount, neighborListEstimate.min),
                    HugeObjectArray.memoryEstimation(nodeCount, neighborListEstimate.max)
                );

                return MemoryEstimations
                    .builder(Knn.class)
                    .rangePerNode("top-k-neighbors-list", perNodeNeighborListEstimate)
                    .rangePerNode("old-neighbors", tempListEstimation)
                    .rangePerNode("new-neighbors", tempListEstimation)
                    .rangePerNode("old-reverse-neighbors", tempListEstimation)
                    .rangePerNode("new-reverse-neighbors", tempListEstimation)
                    .fixed(
                        "initial-random-neighbors (per thread)",
                        KnnFactory
                            .initialSamplerMemoryEstimation(parameters.samplerType(), k.value)
                            .times(concurrency.value())
                    )
                    .fixed(
                        "sampled-random-neighbors (per thread)",
                        MemoryRange.of(
                            sizeOfIntArray(sizeOfOpenHashContainer(k.sampledValue)) * concurrency.value()
                        )
                    )
                    .build();
            }
        );
    }
}
