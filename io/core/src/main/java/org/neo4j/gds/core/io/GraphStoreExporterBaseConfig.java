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
package org.neo4j.gds.core.io;

import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.annotation.Configuration;
import org.neo4j.gds.config.BaseConfig;
import org.neo4j.gds.config.ConcurrencyConfig;
import org.neo4j.gds.core.concurrency.Concurrency;
import org.neo4j.gds.core.concurrency.ParallelUtil;

public interface GraphStoreExporterBaseConfig extends BaseConfig {

    default String defaultRelationshipType() {
        return RelationshipType.ALL_RELATIONSHIPS.name;
    }

    default int writeConcurrency() {
        return ConcurrencyConfig.DEFAULT_CONCURRENCY;
    }

    default Concurrency typedWriteConcurrency() {
        return new Concurrency(writeConcurrency());
    }

    default int batchSize() {
        return ParallelUtil.DEFAULT_BATCH_SIZE;
    }

    @Configuration.ConvertWith(method = "org.neo4j.gds.PropertyMappings#fromObject")
    @Configuration.ToMapValue("org.neo4j.gds.PropertyMappings#toObject")
    default org.neo4j.gds.PropertyMappings additionalNodeProperties() {
        return org.neo4j.gds.PropertyMappings.of();
    }
}
