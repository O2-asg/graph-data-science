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

import org.neo4j.gds.Algorithm;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.properties.nodes.NodePropertyValuesAdapter;
import org.neo4j.gds.beta.pregel.Pregel;
import org.neo4j.gds.beta.pregel.PregelComputation;
import org.neo4j.gds.core.concurrency.RunWithConcurrency;
import org.neo4j.gds.termination.TerminationFlag;
import org.neo4j.gds.collections.ha.HugeDoubleArray;
import org.neo4j.gds.core.utils.partition.PartitionUtils;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.scaling.L2Norm;
import org.neo4j.gds.scaling.NoneScaler;

import java.util.Optional;
import java.util.concurrent.ExecutorService;


public class PageRankAlgorithm extends Algorithm<PageRankResult> {

    private final Pregel<PageRankConfig> pregelJob;
    private final Graph graph;
    private final PageRankAlgorithmFactory.Mode mode;
    private final PageRankConfig config;
    private final ExecutorService executorService;

    /**
     * @deprecated Use the variant  that does proper injection of termination flag instead
     */
    @Deprecated
    public PageRankAlgorithm(
        Graph graph,
        PageRankConfig config,
        PregelComputation<PageRankConfig> pregelComputation,
        PageRankAlgorithmFactory.Mode mode,
        ExecutorService executorService,
        ProgressTracker progressTracker
    ) {
        this(
            graph,
            config,
            pregelComputation,
            mode,
            executorService,
            progressTracker,
            TerminationFlag.RUNNING_TRUE
        );
    }

    public PageRankAlgorithm(
        Graph graph,
        PageRankConfig config,
        PregelComputation<PageRankConfig> pregelComputation,
        PageRankAlgorithmFactory.Mode mode,
        ExecutorService executorService,
        ProgressTracker progressTracker,
        TerminationFlag terminationFlag
    ) {
        super(progressTracker);
        this.pregelJob = Pregel.create(graph, config, pregelComputation, executorService, progressTracker, terminationFlag);
        this.mode = mode;
        this.executorService = executorService;
        this.config = config;
        this.graph = graph;
        this.terminationFlag = terminationFlag;
    }

    @Override
    public void setTerminationFlag(TerminationFlag terminationFlag) {
        super.setTerminationFlag(terminationFlag);
        pregelJob.setTerminationFlag(terminationFlag);
    }

    @Override
    public PageRankResult compute() {
        var pregelResult = pregelJob.run();

        var scores = pregelResult.nodeValues().doubleProperties(PageRankComputation.PAGE_RANK);

        scaleScores(scores);

        return new PageRankResult(
            scores,
            pregelResult.ranIterations(),
            pregelResult.didConverge()
        );
    }

    private void scaleScores(HugeDoubleArray scores) {
        var scalerFactory = config.scaler();
        var concurrency = config.concurrency();

        // Eigenvector produces L2NORM-scaled results by default.
        if (scalerFactory.type().equals(NoneScaler.TYPE) || (scalerFactory.type().equals(L2Norm.TYPE) && mode == PageRankAlgorithmFactory.Mode.EIGENVECTOR)) {
            return;
        }

        var scaler = scalerFactory.create(
            NodePropertyValuesAdapter.adapt(scores),
            graph.nodeCount(),
            concurrency,
            ProgressTracker.NULL_TRACKER,
            executorService
        );

        var tasks = PartitionUtils.rangePartition(concurrency, graph.nodeCount(),
            partition -> (Runnable) () -> partition.consume(nodeId -> scores.set(nodeId, scaler.scaleProperty(nodeId))),
            Optional.empty()
        );

        RunWithConcurrency.builder()
            .concurrency(concurrency)
            .tasks(tasks)
            .executor(executorService)
            .run();
    }

}
