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

import org.neo4j.common.Validator;
import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.api.IdMap;
import org.neo4j.gds.core.concurrency.Concurrency;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongFunction;
import java.util.stream.Collectors;

public abstract class GraphStoreExporter {

    private final GraphStore graphStore;
    private final Map<String, LongFunction<Object>> neoNodeProperties;
    private final IdentifierMapper<NodeLabel> nodeLabelMapping;
    private final RelationshipType defaultRelationshipType;
    protected final Concurrency concurrency;
    private final int batchSize;
    private final IdentifierMapper<RelationshipType> relationshipTypeMapping;

    public enum IdMappingType implements IdMapFunction {
        MAPPED {
            @Override
            public long getId(IdMap idMap, long id) {
                return id;
            }

            @Override
            public long highestId(IdMap idMap) {
                return idMap.nodeCount() - 1;
            }

            @Override
            public boolean contains(IdMap idMap, long id) {
                return highestId(idMap) >= id;
            }
        },
        ORIGINAL {
            @Override
            public long getId(IdMap idMap, long id) {
                return idMap.toOriginalNodeId(id);
            }

            @Override
            public long highestId(IdMap idMap) {
                return idMap.highestOriginalId();
            }

            @Override
            public boolean contains(IdMap idMap, long id) {
                return idMap.containsOriginalId(id);
            }
        }
    }

    interface IdMapFunction {
        long getId(IdMap idMap, long id);
        long highestId(IdMap idMap);
        boolean contains(IdMap idMap, long id);
    }

    protected GraphStoreExporter(
        GraphStore graphStore,
        Optional<NeoNodeProperties> neoNodeProperties,
        IdentifierMapper<NodeLabel> nodeLabelMapping,
        IdentifierMapper<RelationshipType> relationshipTypeMapping,
        RelationshipType defaultRelationshipType,
        Concurrency concurrency,
        int batchSize
    ) {
        this.graphStore = graphStore;
        this.defaultRelationshipType = defaultRelationshipType;
        this.concurrency = concurrency;
        this.batchSize = batchSize;
        this.neoNodeProperties = neoNodeProperties
            .map(NeoNodeProperties::neoNodeProperties)
            .orElse(Map.of());
        this.nodeLabelMapping = nodeLabelMapping;
        this.relationshipTypeMapping = relationshipTypeMapping;
    }

    protected abstract void export(GraphStoreInput graphStoreInput);

    protected abstract IdMappingType idMappingType();

    public ExportedProperties run() {
        var metaDataStore = MetaDataStore.of(graphStore);
        var nodeStore = NodeStore.of(
            graphStore,
            neoNodeProperties,
            nodeLabelMapping
        );
        var relationshipStore = RelationshipStore.of(graphStore, defaultRelationshipType, relationshipTypeMapping);
        var graphProperties = graphStore
            .graphPropertyKeys()
            .stream()
            .map(graphStore::graphProperty)
            .collect(Collectors.toSet());

        var graphStoreInput = GraphStoreInput.of(
            metaDataStore,
            nodeStore,
            relationshipStore,
            graphStore.capabilities(),
            graphProperties,
            batchSize,
            concurrency,
            idMappingType()
        );

        export(graphStoreInput);

        long importedNodeProperties = (nodeStore.propertyCount() + neoNodeProperties.size()) * graphStore.nodeCount();
        long importedRelationshipProperties = relationshipStore.propertyCount();
        return new ExportedProperties(importedNodeProperties, importedRelationshipProperties);
    }

    public record ExportedProperties(long nodePropertyCount, long relationshipPropertyCount) {}

    public static final Validator<Path> DIRECTORY_IS_WRITABLE = value -> {
        try {
            // TODO: A validator should only validate, not create the directory as well
            Files.createDirectories(value);
            if (!Files.isDirectory(value)) {
                throw new IllegalArgumentException("'" + value + "' is not a directory");
            }
            if (!Files.isWritable(value)) {
                throw new IllegalArgumentException("Directory '" + value + "' not writable");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Directory '" + value + "' not writable: ", e);
        }
    };
}
