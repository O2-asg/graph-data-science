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

import org.neo4j.gds.GraphAlgorithmFactory;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.concurrency.Concurrency;
import org.neo4j.gds.core.concurrency.DefaultPool;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.termination.TerminationFlag;

public class TriangleStreamFactory extends GraphAlgorithmFactory<TriangleStream, TriangleCountBaseConfig> {

    @Override
    public String taskName() {
        return "TriangleStream";
    }

    public TriangleStream build(Graph graph, Concurrency concurrency) {
        return TriangleStream.create(graph, DefaultPool.INSTANCE, concurrency, TerminationFlag.RUNNING_TRUE);
    }

    @Override
    public TriangleStream build(Graph graph, TriangleCountBaseConfig configuration, ProgressTracker progressTracker) {
        return build(graph, configuration.concurrency());
    }
}
