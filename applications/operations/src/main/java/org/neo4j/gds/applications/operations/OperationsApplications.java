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
package org.neo4j.gds.applications.operations;

import org.neo4j.gds.applications.algorithms.machinery.RequestScopedDependencies;
import org.neo4j.gds.core.utils.progress.JobId;
import org.neo4j.gds.core.utils.progress.TaskStore;
import org.neo4j.gds.core.utils.warnings.UserLogEntry;

import java.util.stream.Stream;

public final class OperationsApplications {
    private final RequestScopedDependencies requestScopedDependencies;

    private OperationsApplications(RequestScopedDependencies requestScopedDependencies) {
        this.requestScopedDependencies = requestScopedDependencies;
    }

    public static OperationsApplications create(RequestScopedDependencies requestScopedDependencies) {
        return new OperationsApplications(requestScopedDependencies);
    }

    /**
     * List progress for the given user, or for all users if in administrator mode
     */
    public Stream<TaskStore.UserTask> listProgress() {
        var taskStore = requestScopedDependencies.getTaskStore();
        var user = requestScopedDependencies.getUser();

        if (user.isAdmin()) return taskStore.query();

        return taskStore.query(user.getUsername());
    }

    /**
     * List progress for the given job id, subject to user permissions.
     */
    public <RESULT> Stream<RESULT> listProgress(JobId jobId, ResultRenderer<RESULT> resultRenderer) {
        var taskStore = requestScopedDependencies.getTaskStore();
        var user = requestScopedDependencies.getUser();

        if (user.isAdmin()) {
            var results = taskStore.query(jobId);

            return resultRenderer.renderAdministratorView(results);
        }

        var results = taskStore.query(user.getUsername(), jobId);

        return resultRenderer.render(results);
    }

    /**
     * Huh, we never did jobId filtering...
     */
    public Stream<UserLogEntry> queryUserLog(String jobId) {
        var user = requestScopedDependencies.getUser();
        var userLogStore = requestScopedDependencies.getUserLogStore();

        return userLogStore.query(user.getUsername());
    }
}
