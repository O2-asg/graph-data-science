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

import org.neo4j.gds.Algorithm;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.IntersectionConsumer;
import org.neo4j.gds.api.RelationshipIntersect;
import org.neo4j.gds.collections.haa.HugeAtomicLongArray;
import org.neo4j.gds.core.concurrency.Concurrency;
import org.neo4j.gds.core.concurrency.ParallelUtil;
import org.neo4j.gds.core.utils.paged.ParalleLongPageCreator;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.termination.TerminationFlag;
import org.neo4j.gds.triangle.intersect.ImmutableRelationshipIntersectConfig;
import org.neo4j.gds.triangle.intersect.RelationshipIntersectConfig;
import org.neo4j.gds.triangle.intersect.RelationshipIntersectFactory;
import org.neo4j.gds.triangle.intersect.RelationshipIntersectFactoryLocator;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * TriangleCount counts the number of triangles in the Graph as well
 * as the number of triangles that passes through a node.
 *
 * This impl uses another approach where all the triangles can be calculated
 * using set intersection methods of the graph itself.
 *
 * https://epubs.siam.org/doi/pdf/10.1137/1.9781611973198.1
 * http://www.cse.cuhk.edu.hk/~jcheng/papers/triangle_kdd11.pdf
 * https://i11www.iti.kit.edu/extra/publications/sw-fclt-05_t.pdf
 * http://www.math.cmu.edu/~ctsourak/tsourICDM08.pdf
 */
@SuppressWarnings("FieldCanBeLocal")
public final class IntersectingTriangleCount extends Algorithm<TriangleCountResult> {

    static final int EXCLUDED_NODE_TRIANGLE_COUNT = -1;

    private final Graph graph;
    private final RelationshipIntersectFactory intersectFactory;
    private final RelationshipIntersectConfig intersectConfig;
    private final ExecutorService executorService;
    private final AtomicLong queue;

    // results
    private final HugeAtomicLongArray triangleCounts;
    private final long maxDegree;
    private final Concurrency concurrency;
    private long globalTriangleCount;

    private final LongAdder globalTriangleCounter;

    public static IntersectingTriangleCount create(
        Graph graph,
        Concurrency concurrency,
        long maxDegree,
        ExecutorService executorService,
        ProgressTracker progressTracker,
        TerminationFlag terminationFlag
    ) {
        var factory = RelationshipIntersectFactoryLocator
            .lookup(graph)
            .orElseThrow(
                () -> new IllegalArgumentException("No relationship intersect factory registered for graph: " + graph.getClass())
            );
        return new IntersectingTriangleCount(graph, factory, concurrency, maxDegree, executorService, progressTracker, terminationFlag);
    }

    private IntersectingTriangleCount(
        Graph graph,
        RelationshipIntersectFactory intersectFactory,
        Concurrency concurrency,
        long maxDegree,
        ExecutorService executorService,
        ProgressTracker progressTracker,
        TerminationFlag terminationFlag
    ) {
        super(progressTracker);
        this.graph = graph;
        this.intersectFactory = intersectFactory;
        this.concurrency = concurrency;
        this.maxDegree = maxDegree;
        this.intersectConfig = ImmutableRelationshipIntersectConfig.of(maxDegree);
        this.triangleCounts = HugeAtomicLongArray.of(graph.nodeCount(), ParalleLongPageCreator.passThrough(concurrency));
        this.executorService = executorService;
        this.globalTriangleCounter = new LongAdder();
        this.queue = new AtomicLong();

        this.terminationFlag = terminationFlag;
    }

    @Override
    public TriangleCountResult compute() {
        progressTracker.beginSubTask();
        queue.set(0);
        globalTriangleCounter.reset();
        // create tasks
        final Collection<? extends Runnable> tasks = ParallelUtil.tasks(
            concurrency,
            () -> new IntersectTask(intersectFactory.load(graph, intersectConfig))
        );
        // run
        ParallelUtil.run(tasks, executorService);

        globalTriangleCount = globalTriangleCounter.longValue();

        progressTracker.endSubTask();
        return TriangleCountResult.of(
            triangleCounts,
            globalTriangleCount
        );
    }

    private class IntersectTask implements Runnable, IntersectionConsumer {

        private final RelationshipIntersect intersect;

        IntersectTask(RelationshipIntersect relationshipIntersect) {
            intersect = relationshipIntersect;
        }

        @Override
        public void run() {
            long node;
            while ((node = queue.getAndIncrement()) < graph.nodeCount() && terminationFlag.running()) {
                if (graph.degree(node) <= maxDegree) {
                    intersect.intersectAll(node, this);
                } else {
                    triangleCounts.set(node, EXCLUDED_NODE_TRIANGLE_COUNT);
                }
                progressTracker.logProgress();
            }
        }

        @Override
        public void accept(final long nodeA, final long nodeB, final long nodeC) {
            triangleCounts.getAndAdd(nodeA, 1);
            triangleCounts.getAndAdd(nodeB, 1);
            triangleCounts.getAndAdd(nodeC, 1);
            globalTriangleCounter.increment();
        }
    }

}
