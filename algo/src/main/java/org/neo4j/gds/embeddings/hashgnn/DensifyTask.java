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
package org.neo4j.gds.embeddings.hashgnn;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.collections.ha.HugeObjectArray;
import org.neo4j.gds.core.concurrency.Concurrency;
import org.neo4j.gds.core.concurrency.RunWithConcurrency;
import org.neo4j.gds.core.utils.paged.HugeAtomicBitSet;
import org.neo4j.gds.core.utils.partition.Partition;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.termination.TerminationFlag;

import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.Collectors;

class DensifyTask implements Runnable {
    private static final int SPARSITY = 3;
    private static final double ENTRY_PROBABILITY = 1.0 / (2 * SPARSITY);
    private final Partition partition;
    private final int outputDimension;
    private final HugeObjectArray<double[]> denseFeatures;
    private final HugeObjectArray<HugeAtomicBitSet> binaryFeatures;
    private final float[][] projectionMatrix;
    private final ProgressTracker progressTracker;

    DensifyTask(
        Partition partition,
        int outputDimension,
        HugeObjectArray<double[]> denseFeatures,
        HugeObjectArray<HugeAtomicBitSet> binaryFeatures,
        float[][] projectionMatrix,
        ProgressTracker progressTracker
    ) {
        this.partition = partition;
        this.outputDimension = outputDimension;
        this.denseFeatures = denseFeatures;
        this.binaryFeatures = binaryFeatures;
        this.projectionMatrix = projectionMatrix;
        this.progressTracker = progressTracker;
    }

    static HugeObjectArray<double[]> compute(
        Graph graph,
        List<Partition> partition,
        Concurrency concurrency,
        int outputDimension,
        SplittableRandom rng,
        HugeObjectArray<HugeAtomicBitSet> binaryFeatures,
        ProgressTracker progressTracker,
        TerminationFlag terminationFlag
    ) {
        progressTracker.beginSubTask("Densify output embeddings");

        var denseFeatures = HugeObjectArray.newArray(double[].class, graph.nodeCount());

        var projectionMatrix = projectionMatrix(
            rng,
            outputDimension,
            (int) binaryFeatures.get(0).size()
        );

        var tasks = partition.stream()
            .map(p -> new DensifyTask(
                p,
                outputDimension,
                denseFeatures,
                binaryFeatures,
                projectionMatrix,
                progressTracker
            ))
            .collect(Collectors.toList());
        RunWithConcurrency.builder()
            .concurrency(concurrency)
            .tasks(tasks)
            .terminationFlag(terminationFlag)
            .run();

        progressTracker.endSubTask("Densify output embeddings");

        return denseFeatures;
    }

    private static float[][] projectionMatrix(SplittableRandom rng, int denseDimension, int binaryDimension) {
        float entryValue = (float) Math.sqrt(SPARSITY) / (float) Math.sqrt(denseDimension);
        var matrix = new float[binaryDimension][denseDimension];
        for (int i = 0; i < binaryDimension; i++) {
            matrix[i] = new float[denseDimension];
            for (int d = 0; d < denseDimension; d++) {
                matrix[i][d] = computeRandomEntry(rng, entryValue);
            }
        }

        return matrix;
    }

    private static float computeRandomEntry(SplittableRandom random, float entryValue) {
        double randomValue = random.nextDouble();

        if (randomValue < ENTRY_PROBABILITY) {
            return entryValue;
        } else if (randomValue < ENTRY_PROBABILITY * 2.0) {
            return -entryValue;
        } else {
            return 0.0f;
        }
    }

    @Override
    public void run() {
        int denseLength = projectionMatrix[0].length;

        partition.consume(nodeId -> {
            var binaryVector = binaryFeatures.get(nodeId);
            var denseVector = new double[outputDimension];

            binaryVector.forEachSetBit(bit -> {
                final float[] row = projectionMatrix[(int) bit];
                for (int i = 0; i < denseLength; i++) {
                    denseVector[i] += row[i];
                }
            });

            denseFeatures.set(nodeId, denseVector);
        });

        progressTracker.logProgress(partition.nodeCount());
    }
}
