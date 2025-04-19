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
package org.neo4j.gds.config;

import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.annotation.Configuration;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.api.ResultStore;
import org.neo4j.gds.concurrency.ConcurrencyValidatorService;
import org.neo4j.gds.core.concurrency.Concurrency;

import java.util.Collection;
import java.util.Optional;

public interface WriteConfig extends ConcurrencyConfig {

    String WRITE_CONCURRENCY_KEY = "writeConcurrency";

    @Configuration.Key(WRITE_CONCURRENCY_KEY)
    @Configuration.ConvertWith(method = "org.neo4j.gds.config.ConcurrencyConfig#parse")
    @Configuration.ToMapValue("org.neo4j.gds.config.ConcurrencyConfig#render")
    default Concurrency writeConcurrency() {
        return concurrency();
    }

    @Configuration.Check
    default void validateWriteConcurrency() {
        ConcurrencyValidatorService
            .validator()
            .validate(writeConcurrency().value(), WRITE_CONCURRENCY_KEY, ConcurrencyConfig.CONCURRENCY_LIMITATION);
    }

    default boolean writeToResultStore() {
        return false;
    }

    @Configuration.Ignore
    default Optional<ResultStore> resolveResultStore(ResultStore resultStore) {
        return writeToResultStore()
            ? Optional.of(resultStore)
            : Optional.empty();
    }

    @Configuration.GraphStoreValidationCheck
    default void validateGraphIsSuitableForWrite(
        GraphStore graphStore,
        @SuppressWarnings("unused") Collection<NodeLabel> selectedLabels,
        @SuppressWarnings("unused") Collection<RelationshipType> selectedRelationshipTypes
    ) {
        if (!graphStore.capabilities().canWriteToLocalDatabase() && !graphStore.capabilities()
            .canWriteToRemoteDatabase()) {
            throw new IllegalArgumentException("The provided graph does not support `write` execution mode.");
        }
    }
}
