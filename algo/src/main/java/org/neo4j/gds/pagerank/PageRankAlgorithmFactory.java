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
package org.neo4j.gds.pagerank;

import com.carrotsearch.hppc.LongScatterSet;
import org.jetbrains.annotations.NotNull;
import org.neo4j.gds.GraphAlgorithmFactory;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.beta.pregel.Pregel;
import org.neo4j.gds.beta.pregel.PregelComputation;
import org.neo4j.gds.core.concurrency.Concurrency;
import org.neo4j.gds.core.concurrency.DefaultPool;
import org.neo4j.gds.core.concurrency.ParallelUtil;
import org.neo4j.gds.mem.MemoryEstimation;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.core.utils.progress.tasks.Task;
import org.neo4j.gds.degree.DegreeCentrality;
import org.neo4j.gds.termination.TerminationFlag;

import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongToDoubleFunction;

import static org.neo4j.gds.pagerank.PageRankAlgorithmFactory.Mode.ARTICLE_RANK;
import static org.neo4j.gds.pagerank.PageRankAlgorithmFactory.Mode.EIGENVECTOR;

public class PageRankAlgorithmFactory<CONFIG extends PageRankConfig> extends GraphAlgorithmFactory<PageRankAlgorithm, CONFIG> {


    private static double averageDegree(Graph graph, Concurrency concurrency) {
        var degreeSum = new LongAdder();
        ParallelUtil.parallelForEachNode(
            graph.nodeCount(),
            concurrency,
            TerminationFlag.RUNNING_TRUE,
            nodeId -> degreeSum.add(graph.degree(nodeId))
        );
        return (double) degreeSum.sum() / graph.nodeCount();
    }

    public enum Mode {
        PAGE_RANK("PageRank"),
        ARTICLE_RANK("ArticleRank"),
        EIGENVECTOR("EigenVector");

        private final String taskName;

        Mode(String taskName) {
            this.taskName = taskName;
        }

        String taskName() {
            return taskName;
        }
    }

    private final Mode mode;

    public PageRankAlgorithmFactory() {
        this(Mode.PAGE_RANK);
    }

    public PageRankAlgorithmFactory(Mode mode) {
        this.mode = mode;
    }

    @Override
    public String taskName() {
        return mode.taskName();
    }

    @Override
    public PageRankAlgorithm build(
        Graph graph,
        CONFIG configuration,
        ProgressTracker progressTracker
    ) {
        PregelComputation<PageRankConfig> computation;

        var degreeFunction = degreeFunction(
            graph,
            configuration
        );

        var mappedSourceNodes = new LongScatterSet(configuration.sourceNodes().size());
        configuration.sourceNodes().stream()
            .mapToLong(graph::toMappedNodeId)
            .forEach(mappedSourceNodes::add);

        if (mode == ARTICLE_RANK) {
            double avgDegree = averageDegree(graph, configuration.concurrency());
            computation = new ArticleRankComputation(configuration, mappedSourceNodes, degreeFunction, avgDegree);
        } else if (mode == EIGENVECTOR) {
            // Degrees are generally not respected in eigenvector centrality.
            //
            // However, relationship weights need to be normalized by the weighted degree.
            // The score is divided by the weighted degree before being sent to the neighbors.
            // For the unweighted case, we want a no-op and divide by 1.
            degreeFunction = configuration.hasRelationshipWeightProperty()
                ? degreeFunction
                : (nodeId) -> 1;

            computation = new EigenvectorComputation(graph.nodeCount(), configuration, mappedSourceNodes, degreeFunction);
        } else {
            computation = new PageRankComputation(configuration, mappedSourceNodes, degreeFunction);
        }

        return new PageRankAlgorithm(
            graph,
            configuration,
            computation,
            mode,
            DefaultPool.INSTANCE,
            progressTracker
        );
    }

    @Override
    public Task progressTask(Graph graph, CONFIG config) {
        return Pregel.progressTask(graph, config, taskName());
    }

    @NotNull
    private LongToDoubleFunction degreeFunction(
        Graph graph,
        CONFIG configuration
    ) {
        var degreeCentrality = new DegreeCentrality(
            graph,
            DefaultPool.INSTANCE,
            configuration.concurrency(),
            Orientation.NATURAL,
            configuration.hasRelationshipWeightProperty(),
            10_000,
            ProgressTracker.NULL_TRACKER
        );

        var degrees = degreeCentrality.compute().degreeFunction();
        return degrees::get;
    }

    @Override
    public MemoryEstimation memoryEstimation(PageRankConfig configuration) {
        return new PageRankMemoryEstimateDefinition().memoryEstimation();
    }
}
