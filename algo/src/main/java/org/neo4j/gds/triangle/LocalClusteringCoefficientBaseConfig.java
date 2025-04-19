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

import org.jetbrains.annotations.Nullable;
import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.annotation.Configuration;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.config.AlgoBaseConfig;
import org.neo4j.gds.config.ConfigNodesValidations;
import org.neo4j.gds.config.SeedConfig;
import org.neo4j.gds.core.StringIdentifierValidations;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

@Configuration
public interface LocalClusteringCoefficientBaseConfig extends AlgoBaseConfig, SeedConfig {

    String SEED_PROPERTY_KEY = "triangleCountProperty";

    @Override
    @Configuration.ConvertWith(method = "validateProperty")
    @Configuration.Key(SEED_PROPERTY_KEY)
    default @Nullable String seedProperty() {
        return null;
    }

    static @Nullable String validateProperty(String input) {
        return StringIdentifierValidations.validateNoWhiteCharacter(input, SEED_PROPERTY_KEY);
    }

    @Override
    @Configuration.GraphStoreValidationCheck
    default void validateSeedProperty(
        GraphStore graphStore,
        Collection<NodeLabel> selectedLabels,
        Collection<RelationshipType> selectedRelationshipTypes
    ) {
        String seedProperty = seedProperty();
        if (seedProperty != null) {
            ConfigNodesValidations.validateNodePropertyExists(graphStore, selectedLabels, SEED_PROPERTY_KEY, seedProperty);
        }
    }

    @Configuration.GraphStoreValidationCheck
    default void validateUndirectedGraph(
        GraphStore graphStore,
        Collection<NodeLabel> ignored,
        Collection<RelationshipType> selectedRelationshipTypes
    ) {
        if (!graphStore.schema().filterRelationshipTypes(Set.copyOf(selectedRelationshipTypes)).isUndirected()) {
            throw new IllegalArgumentException(formatWithLocale(
                "LocalClusteringCoefficient requires relationship projections to be UNDIRECTED. " +
                "Selected relationships `%s` are not all undirected.",
                selectedRelationshipTypes.stream().map(RelationshipType::name).collect(Collectors.toSet())
            ));
        }
    }

    @Configuration.Ignore
    default LocalClusteringCoefficientParameters toParameters() {
        return new LocalClusteringCoefficientParameters(concurrency(), Long.MAX_VALUE, seedProperty());
    }
}
