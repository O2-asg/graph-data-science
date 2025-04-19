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
package org.neo4j.gds.scc;

import com.carrotsearch.hppc.BitSet;
import org.neo4j.gds.Algorithm;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.collections.ha.HugeLongArray;
import org.neo4j.gds.core.utils.paged.HugeLongArrayStack;
import org.neo4j.gds.core.utils.paged.PagedLongStack;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.termination.TerminationFlag;

/**
 * huge iterative (non recursive) sequential strongly connected components algorithm.
 *
 * specified in:  http://code.activestate.com/recipes/578507-strongly-connected-components-of-a-directed-graph/
 */
public class Scc extends Algorithm<HugeLongArray> {
    public static final int UNORDERED = -1;
    public static final String SCC_DESCRIPTION = "The SCC algorithm finds sets of connected nodes in an directed graph, " +
                                                 "where all nodes in the same set form a connected component.";
    private final Graph graph;
    private final HugeLongArrayStack boundaries;
    private final HugeLongArray connectedComponents;
    private final HugeLongArray index;
    private final HugeLongArrayStack stack;
    private final PagedLongStack todo; // stores nodeIds either positive (edge visit) or negative (node visit)
    private final BitSet visited;

    public Scc(
        Graph graph,
        ProgressTracker progressTracker,
        TerminationFlag terminationFlag
    ) {
        super(progressTracker);

        this.graph = graph;
        var nodeCount = this.graph.nodeCount();

        this.boundaries = HugeLongArrayStack.newStack(nodeCount);
        this.connectedComponents = HugeLongArray.newArray(nodeCount);
        this.index = HugeLongArray.newArray(nodeCount);
        this.stack = HugeLongArrayStack.newStack(nodeCount);
        this.todo = new PagedLongStack(nodeCount); //can be as high as `graph.relationshipsCount()` if we are unlucky...
        this.visited = new BitSet(nodeCount);

        this.terminationFlag = terminationFlag;
    }

    /**
     * compute scc
     */
    public HugeLongArray compute() {
        progressTracker.beginSubTask();
        index.fill(UNORDERED);
        connectedComponents.fill(UNORDERED);

        graph.forEachNode(this::computePerNode); //this will visit 0 first
        progressTracker.endSubTask();
        return connectedComponents;
    }

    private boolean computePerNode(long nodeId) {
        if (!terminationFlag.running()) {
            return false;
        }

        if (index.get(nodeId) != UNORDERED) {
            return true;
        }

        todo.push(-nodeId); //push nodeId as a node visit

        while (!todo.isEmpty()) {
            var node = todo.pop();

            if (node < 0) { // if the node is <0, we know we are going to visit a node as a node
                distinguishNodeVisitType(-node);
            } else if (node > 0) { //otherwise  if it's positive, then it 's an edge
                visitEdge(node);
            } else { //the 0 case
                //-0 = 0 , so a 0 can indicate two things:
                // (i) either a visit edge to 0
                // (ii) or a node visit to 0 (here stuck must be empty: either it's the first action or the last)
                if (todo.isEmpty()) {
                    distinguishNodeVisitType(0);
                } else {    //otherwise, it's an edge action, do so
                    visitEdge(0);
                }
            }
        }
        return true;
    }

    private void distinguishNodeVisitType(long node) {
        if (index.get(node) != UNORDERED) { //last visit
            postVisitNode(node);
        } else {            //first visit
            visitNode(node);
        }
    }

    private void visitNode(long nodeId) {
        final long stackSize = stack.size();
        index.set(nodeId, stackSize);
        stack.push(nodeId); // push to stack (at most one entry per vertex)
        boundaries.push(stackSize); // push to stack (at most one entry per vertex)
        todo.push(-nodeId);
        graph.forEachRelationship(nodeId, (s, t) -> {
            todo.push(t);
            return true;
        });
    }

    private void visitEdge(long nodeId) {
        if (index.get(nodeId) == UNORDERED) {
            todo.push(-nodeId); //organize a first visit to nodeId
        } else if (!visited.get(nodeId)) {          //skip nodes already in a component
            while (index.get(nodeId) < boundaries.peek()) {
                boundaries.pop();
            }
        }
    }

    private void postVisitNode(long nodeId) {
        if (boundaries.peek() == index.get(nodeId)) {
            boundaries.pop();
            long element;
            do {
                element = stack.pop(); //pop to stack
                connectedComponents.set(element, nodeId);
                visited.set(element);
            } while (element != nodeId);
        }
        progressTracker.logProgress();
    }


}
