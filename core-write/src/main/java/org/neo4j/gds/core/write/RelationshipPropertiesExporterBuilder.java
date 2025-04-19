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
package org.neo4j.gds.core.write;

import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.api.ResultStore;
import org.neo4j.gds.core.utils.progress.JobId;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.termination.TerminationFlag;
import org.neo4j.values.storable.Values;

import java.util.Optional;

public abstract class RelationshipPropertiesExporterBuilder {

    protected TerminationFlag terminationFlag;
    protected GraphStore graphStore;
    protected ProgressTracker progressTracker = ProgressTracker.NULL_TRACKER;
    protected RelationshipPropertyTranslator propertyTranslator = Values::doubleValue;

    // FIXME: These are only used by the Arrow builder; keeping this aligned with the existing builders but has to be changed.
    protected long relationshipCount = -1L;
    protected long batchSize = NativeNodePropertyExporter.MIN_BATCH_SIZE;
    protected Optional<ResultStore> resultStore;
    protected JobId jobId;

    public abstract RelationshipPropertiesExporter build();

    public RelationshipPropertiesExporterBuilder withGraphStore(GraphStore graphStore) {
        this.graphStore = graphStore;
        return this;
    }

    public RelationshipPropertiesExporterBuilder withRelationPropertyTranslator(RelationshipPropertyTranslator propertyTranslator) {
        this.propertyTranslator = propertyTranslator;
        return this;
    }

    public RelationshipPropertiesExporterBuilder withTerminationFlag(TerminationFlag terminationFlag) {
        this.terminationFlag = terminationFlag;
        return this;
    }

    /**
     * Set the {@link ProgressTracker} to use for logging progress during export.
     *
     * If a {@link org.neo4j.gds.core.utils.progress.tasks.TaskProgressTracker} is used, caller must manage beginning and finishing the subtasks.
     * By default, an {@link org.neo4j.gds.core.utils.progress.tasks.ProgressTracker.EmptyProgressTracker} is used. That one doesn't require caller to manage any tasks.
     *
     * @param progressTracker The progress tracker to use for logging progress during export.
     * @return this
     */
    public RelationshipPropertiesExporterBuilder withProgressTracker(ProgressTracker progressTracker) {
        this.progressTracker = progressTracker;
        return this;
    }


    // FIXME: Below are methods that should only be valid for Arrow builder;
    // Putting these here so we don't have to refactor each and every Builder

    public RelationshipPropertiesExporterBuilder withRelationshipCount(long relationshipCount) {
        this.relationshipCount = relationshipCount;
        return this;
    }

    public RelationshipPropertiesExporterBuilder withBatchSize(long batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    public RelationshipPropertiesExporterBuilder withResultStore(Optional<ResultStore> resultStore) {
        this.resultStore = resultStore;
        return this;
    }

    public RelationshipPropertiesExporterBuilder withJobId(JobId jobId){
        this.jobId = jobId;
        return this;
    }
}
