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
package org.neo4j.gds.articulationpoints;

import com.carrotsearch.hppc.BitSet;
import org.neo4j.gds.Algorithm;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.collections.ha.HugeLongArray;
import org.neo4j.gds.collections.ha.HugeObjectArray;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;

public class ArticulationPoints extends Algorithm<BitSet> {
    private final Graph graph;

    private final BitSet visited;
    private final HugeLongArray tin;
    private final HugeLongArray low;
    private final HugeLongArray children;
    private long timer;
    private long stackIndex = -1;

    private final BitSet articulationPoints;

    public ArticulationPoints(Graph graph, ProgressTracker progressTracker) {
        super(progressTracker);

        this.graph = graph;

        this.visited = new BitSet(graph.nodeCount());
        this.tin = HugeLongArray.newArray(graph.nodeCount());
        this.low = HugeLongArray.newArray(graph.nodeCount());
        this.children = HugeLongArray.newArray(graph.nodeCount());

        this.articulationPoints = new BitSet(graph.nodeCount());
    }

    @Override
    public BitSet compute() {
        timer = 0;
        visited.clear();
        tin.setAll(__ -> -1);
        low.setAll(__ -> -1);
        progressTracker.beginSubTask("ArticulationPoints");
        //each edge may have at most one event to the stack at the same time
        var stack = HugeObjectArray.newArray(StackEvent.class, graph.relationshipCount());

        var n = graph.nodeCount();
        for (int i = 0; i < n; ++i) {
            if (!visited.get(i)) {
                dfs(i, stack);
            }
        }
        progressTracker.endSubTask("ArticulationPoints");
        return this.articulationPoints;
    }

    private void dfs(long node, HugeObjectArray<StackEvent> stack) {
        stack.set(++stackIndex, StackEvent.upcomingVisit(node,-1));
        while (stackIndex >= 0) {
            var stackEvent = stack.get(stackIndex--);
            visitEvent(stackEvent, stack);
        }
        if (children.get(node) > 1) {
            articulationPoints.set(node);
        } else {
            articulationPoints.clear(node);
        }
        progressTracker.logProgress();
    }

    private void visitEvent(StackEvent event, HugeObjectArray<StackEvent> stack) {
        if (event.lastVisit()) {
            var to = event.eventNode();
            var v = event.triggerNode();
            var lowV = low.get(v);
            var lowTo = low.get(to);
            low.set(v, Math.min(lowV, lowTo));
            var tinV = tin.get(v);
            if (lowTo >= tinV) {
                articulationPoints.set(v);
            }
            children.addTo(v, 1);
            progressTracker.logProgress();
            return;
        }

        if (!visited.get(event.eventNode())) {
            var v = event.eventNode();
            visited.set(v);
            children.set(v, 0);
            var p = event.triggerNode();
            tin.set(v, timer);
            low.set(v, timer++);
            ///add post event (Should be before everything)
            if (p != -1) {
                stack.set(++stackIndex, StackEvent.lastVisit(v, p));
            }
            graph.forEachRelationship(v, (s, to) -> {
                if (to == p) {
                    return true;
                }
                stack.set(++stackIndex,  StackEvent.upcomingVisit(to, v));

                return true;
            });

        } else {
            long v = event.triggerNode();
            long to = event.eventNode();
            var lowV = low.get(v);
            var tinTo = tin.get(to);
            low.set(v, Math.min(lowV, tinTo));
        }
    }


    record StackEvent(long eventNode, long triggerNode, boolean lastVisit) {
        static StackEvent upcomingVisit(long node, long triggerNode) {
            return new StackEvent(node, triggerNode, false);
        }

        static StackEvent lastVisit(long node, long triggerNode) {
            return new StackEvent(node, triggerNode, true);
        }
    }
}
