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

import com.carrotsearch.hppc.AbstractIterator;
import org.neo4j.gds.Algorithm;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.IntersectionConsumer;
import org.neo4j.gds.api.RelationshipIntersect;
import org.neo4j.gds.core.concurrency.Concurrency;
import org.neo4j.gds.core.concurrency.ParallelUtil;
import org.neo4j.gds.termination.TerminationFlag;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.triangle.intersect.ImmutableRelationshipIntersectConfig;
import org.neo4j.gds.triangle.intersect.RelationshipIntersectConfig;
import org.neo4j.gds.triangle.intersect.RelationshipIntersectFactory;
import org.neo4j.gds.triangle.intersect.RelationshipIntersectFactoryLocator;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterators;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * TriangleCount counts the number of triangles in the Graph as well
 * as the number of triangles that passes through a node. Instead of
 * emitting the nodeId and the number of triangles the node is part of,
 * this impl. streams the actual nodeIds of each triangle once.
 */
public final class TriangleStream extends Algorithm<Stream<TriangleStreamResult>> {

    private final Graph graph;
    private final RelationshipIntersectFactory intersectFactory;
    private final RelationshipIntersectConfig intersectConfig;
    private final ExecutorService executorService;
    private final AtomicInteger queue;
    private final Concurrency concurrency;
    private final int nodeCount;
    private final AtomicInteger runningThreads;
    private final BlockingQueue<TriangleStreamResult> resultQueue;

    public static TriangleStream create(
        Graph graph,
        ExecutorService executorService,
        Concurrency concurrency,
        TerminationFlag terminationFlag
    ) {
        var factory = RelationshipIntersectFactoryLocator
            .lookup(graph)
            .orElseThrow(
                () -> new IllegalArgumentException("No relationship intersect factory registered for graph: " + graph.getClass())
            );
        return new TriangleStream(graph, factory, executorService, concurrency, terminationFlag);
    }

    private TriangleStream(
        Graph graph,
        RelationshipIntersectFactory intersectFactory,
        ExecutorService executorService,
        Concurrency concurrency,
        TerminationFlag terminationFlag
    ) {
        super(ProgressTracker.NULL_TRACKER);
        this.graph = graph;
        this.intersectFactory = intersectFactory;
        this.intersectConfig = ImmutableRelationshipIntersectConfig.builder().build();
        this.executorService = executorService;
        this.concurrency = concurrency;
        this.nodeCount = Math.toIntExact(graph.nodeCount());
        this.resultQueue = new ArrayBlockingQueue<>(concurrency.value() << 10);
        this.runningThreads = new AtomicInteger();
        this.queue = new AtomicInteger();

        this.terminationFlag = terminationFlag;
    }

    @Override
    public Stream<TriangleStreamResult> compute() {
        progressTracker.beginSubTask(graph.nodeCount());
        submitTasks();
        final TerminationFlag flag = getTerminationFlag();
        final Iterator<TriangleStreamResult> it = new AbstractIterator<>() {

            @Override
            protected TriangleStreamResult fetch() {
                TriangleStreamResult result = null;
                while (result == null && flag.running() && (runningThreads.get() > 0 || !resultQueue.isEmpty())) {
                    result = resultQueue.poll();
                }
                return result != null ? result : done();
            }
        };

        return StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(it, 0), false)
                .filter(Objects::nonNull)
                .onClose(progressTracker::endSubTask);
    }

    private void submitTasks() {
        queue.set(0);
        runningThreads.set(0);
        final Collection<Runnable> tasks;
        tasks = ParallelUtil.tasks(concurrency, () -> new IntersectTask(intersectFactory.load(graph, intersectConfig)));
        ParallelUtil.run(tasks, false, executorService, null);
    }

    private abstract class BaseTask implements Runnable {

        BaseTask() {
            runningThreads.incrementAndGet();
        }

        @Override
        public final void run() {
            try {
                int node;
                while ((node = queue.getAndIncrement()) < nodeCount && terminationFlag.running()) {
                    evaluateNode(node);
                    progressTracker.logProgress();
                }
            } finally {
                runningThreads.decrementAndGet();
            }
        }

        abstract void evaluateNode(int nodeId);

        void emit(long nodeA, long nodeB, long nodeC) {
            var result = new TriangleStreamResult(
                    graph.toOriginalNodeId(nodeA),
                    graph.toOriginalNodeId(nodeB),
                    graph.toOriginalNodeId(nodeC));
            resultQueue.offer(result);
        }
    }

    private final class IntersectTask extends BaseTask implements IntersectionConsumer {

        private final RelationshipIntersect intersect;

        IntersectTask(RelationshipIntersect intersect) {
            this.intersect = intersect;
        }

        @Override
        void evaluateNode(final int nodeId) {
            intersect.intersectAll(nodeId, this);
        }

        @Override
        public void accept(final long nodeA, final long nodeB, final long nodeC) {
            emit(nodeA, nodeB, nodeC);
        }
    }

}
