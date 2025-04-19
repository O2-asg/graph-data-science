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
package org.neo4j.gds.applications.graphstorecatalog;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.beta.filter.GraphFilterResult;
import org.neo4j.gds.beta.filter.GraphStoreFilterService;
import org.neo4j.gds.config.GraphProjectFromGraphConfig;
import org.neo4j.gds.core.concurrency.DefaultPool;
import org.neo4j.gds.core.loading.GraphStoreCatalogService;
import org.neo4j.gds.core.utils.ProgressTimer;
import org.neo4j.gds.core.utils.progress.TaskRegistryFactory;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.core.utils.progress.tasks.TaskProgressTracker;
import org.neo4j.gds.core.utils.warnings.UserLogRegistryFactory;
import org.neo4j.gds.logging.Log;

import java.util.concurrent.atomic.AtomicLong;

public class SubGraphProjectApplication {
    private final GraphStoreFilterService graphStoreFilterService = new GraphStoreFilterService();

    private final Log log;
    private final GraphStoreCatalogService graphStoreCatalogService;

    SubGraphProjectApplication(
        Log log,
        GraphStoreCatalogService graphStoreCatalogService
    ) {
        this.log = log;
        this.graphStoreCatalogService = graphStoreCatalogService;
    }

    public GraphFilterResult project(
        TaskRegistryFactory taskRegistryFactory,
        UserLogRegistryFactory userLogRegistryFactory,
        GraphProjectFromGraphConfig configuration,
        GraphStore originGraphStore
    ) {
        return projectWithErrorsHandled(taskRegistryFactory, userLogRegistryFactory, configuration, originGraphStore);
    }

    private GraphFilterResult projectWithErrorsHandled(
        TaskRegistryFactory taskRegistryFactory,
        UserLogRegistryFactory userLogRegistryFactory,
        GraphProjectFromGraphConfig configuration,
        GraphStore originGraphStore
    ) {
        try {
            var countsAndTiming = projectTimed(
                taskRegistryFactory,
                userLogRegistryFactory,
                configuration,
                originGraphStore
            );

            return new GraphFilterResult(
                configuration.graphName(),
                configuration.fromGraphName(),
                configuration.nodeFilter(),
                configuration.relationshipFilter(),
                countsAndTiming.getLeft(),
                countsAndTiming.getMiddle(),
                countsAndTiming.getRight()
            );
        } catch (RuntimeException e) {
            log.warn("Graph creation failed", e);
            throw e;
        }
    }

    /**
     * @return triplet of {nodeCount, relationshipCount, duration in milliseconds},
     *     the summary result of the projection with timing
     */
    private Triple<Long, Long, Long> projectTimed(
        TaskRegistryFactory taskRegistryFactory,
        UserLogRegistryFactory userLogRegistryFactory,
        GraphProjectFromGraphConfig configuration,
        GraphStore originGraphStore
    ) {
        Pair<Long, Long> entityCounts;
        var durationInMilliSeconds = new AtomicLong();

        try (var ignored = ProgressTimer.start(durationInMilliSeconds::set)) {
            entityCounts = projectWithProgressTracker(
                taskRegistryFactory,
                userLogRegistryFactory,
                configuration,
                originGraphStore
            );
        }

        return Triple.of(entityCounts.getLeft(), entityCounts.getRight(), durationInMilliSeconds.get());
    }

    /**
     * @return pair of {nodeCount, relationshipCount}, the summary result of the projection
     */
    private Pair<Long, Long> projectWithProgressTracker(
        TaskRegistryFactory taskRegistryFactory,
        UserLogRegistryFactory userLogRegistryFactory,
        GraphProjectFromGraphConfig configuration,
        GraphStore originGraphStore
    ) {
        var task = graphStoreFilterService.progressTask(originGraphStore);

        var progressTracker = new TaskProgressTracker(
            task,
            log,
            configuration.typedConcurrency(),
            configuration.jobId(),
            taskRegistryFactory,
            userLogRegistryFactory
        );

        return projectAndStore(configuration, originGraphStore, progressTracker);
    }

    /**
     * @return pair of {nodeCount, relationshipCount}, the summary result of the projection
     */
    private Pair<Long, Long> projectAndStore(
        GraphProjectFromGraphConfig configuration,
        GraphStore originGraphStore,
        ProgressTracker progressTracker
    ) {
        var graphStore = graphStoreFilterService.filter(
            originGraphStore,
            configuration,
            DefaultPool.INSTANCE,
            progressTracker
        );

        graphStoreCatalogService.set(configuration, graphStore);

        return Pair.of(graphStore.nodeCount(), graphStore.relationshipCount());
    }
}
