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
package org.neo4j.gds.procedures.integration;

import org.neo4j.function.ThrowingFunction;
import org.neo4j.gds.core.utils.progress.TaskRegistryFactory;
import org.neo4j.gds.procedures.TaskRegistryFactoryService;
import org.neo4j.gds.procedures.DatabaseIdAccessor;
import org.neo4j.gds.procedures.UserAccessor;
import org.neo4j.internal.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.api.procedure.Context;

/**
 * @deprecated Needed until we strangle the last context-injected usages of {@link org.neo4j.gds.core.utils.progress.TaskRegistryFactory}
 */
@Deprecated
public class TaskRegistryFactoryProvider implements ThrowingFunction<Context, TaskRegistryFactory, ProcedureException> {
    private final DatabaseIdAccessor databaseIdAccessor = new DatabaseIdAccessor();
    private final UserAccessor userAccessor = new UserAccessor();

    private final TaskRegistryFactoryService taskRegistryFactoryService;

    TaskRegistryFactoryProvider(TaskRegistryFactoryService taskRegistryFactoryService) {
        this.taskRegistryFactoryService = taskRegistryFactoryService;
    }

    @Override
    public TaskRegistryFactory apply(Context context) {
        var databaseId = databaseIdAccessor.getDatabaseId(context.graphDatabaseAPI());
        var user = userAccessor.getUser(context.securityContext());

        return taskRegistryFactoryService.getTaskRegistryFactory(databaseId, user);
    }
}
