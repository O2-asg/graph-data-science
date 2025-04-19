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
package org.neo4j.gds.core.write.resultstore;

import org.neo4j.gds.api.ResultStore;
import org.neo4j.gds.api.ResultStoreEntry;
import org.neo4j.gds.core.utils.progress.JobId;
import org.neo4j.gds.core.write.NodeLabelExporter;

import java.util.function.LongUnaryOperator;

public class ResultStoreNodeLabelExporter implements NodeLabelExporter {

    private final JobId jobId;
    private final ResultStore resultStore;
    private final long nodeCount;
    private final LongUnaryOperator toOriginalId;

    ResultStoreNodeLabelExporter(JobId jobId, ResultStore resultStore, long nodeCount, LongUnaryOperator toOriginalId) {
        this.jobId = jobId;
        this.resultStore = resultStore;
        this.nodeCount = nodeCount;
        this.toOriginalId = toOriginalId;
    }

    @Override
    public void write(String nodeLabel) {
        resultStore.add(jobId, new ResultStoreEntry.NodeLabel(nodeLabel, nodeCount, toOriginalId));
    }

    @Override
    public long nodeLabelsWritten() {
        return nodeCount;
    }
}
