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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.NodeProjection;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.RelationshipProjection;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.api.GraphName;
import org.neo4j.gds.api.User;
import org.neo4j.gds.core.Aggregation;
import org.neo4j.gds.core.concurrency.Concurrency;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.fail;

class CatalogConfigurationServiceTest {
    /*
     * This does not capture every element and variation;
     * it is here as a placeholder for when you have tricky stuff and want to iterate locally
     */
    @Test
    void shouldParseNativeProjectConfiguration() {
        var service = new CatalogConfigurationService();

        var configuration = service.parseNativeProjectConfiguration(
            new User("some user", false),
            GraphName.parse("some graph"),
            "some label",
            "some relationship type",
            emptyMap()
        );

        assertThat(configuration.creationTime()).isCloseTo(ZonedDateTime.now(), within(2, ChronoUnit.SECONDS));
        assertThat(configuration.graphName()).isEqualTo("some graph");
        assertThat(configuration.nodeCount()).isEqualTo(-1);
        assertThat(configuration.nodeProjections().projections()).containsExactlyInAnyOrderEntriesOf(Map.of(
            NodeLabel.of("some label"), NodeProjection.of("some label")
        ));
        assertThat(configuration.logProgress()).isEqualTo(true);
        assertThat(configuration.readConcurrency()).isEqualTo(new Concurrency(4));
        assertThat(configuration.relationshipCount()).isEqualTo(-1);
        assertThat(configuration.relationshipProjections().projections()).containsExactlyInAnyOrderEntriesOf(Map.of(
            RelationshipType.of("some relationship type"),
            RelationshipProjection.of(
                "some relationship type", Orientation.NATURAL, Aggregation.DEFAULT
            )
        ));
        assertThat(configuration.username()).isEqualTo("some user");
    }

    @ParameterizedTest
    @ValueSource(strings = {"nodeProjection", "relationshipProjection", "nodeQuery", "relationshipQuery"})
    void shouldDisallowNativeProjectConfigurationWithConfigurationCertainKeywordsInIt(String key) {
        var service = new CatalogConfigurationService();

        try {
            service.parseNativeProjectConfiguration(
                new User("some user", false),
                GraphName.parse("some graph"),
                "some label",
                "some relationship type",
                Map.of(key, "yeah, that's not allowed")
            );
            fail();
        } catch (RuntimeException e) {
            assertThat(e).hasMessageContaining("Unexpected configuration key: " + key);
        }
    }

    @Test
    void shouldParseCypherProjectConfiguration() {
        var service = new CatalogConfigurationService();

        var configuration = service.parseCypherProjectConfiguration(
            new User("some user", false),
            GraphName.parse("some graph"),
            "some node query",
            "some relationship query",
            emptyMap()
        );

        assertThat(configuration.creationTime()).isCloseTo(ZonedDateTime.now(), within(2, ChronoUnit.SECONDS));
        assertThat(configuration.graphName()).isEqualTo("some graph");
        assertThat(configuration.nodeCount()).isEqualTo(-1);
        assertThat(configuration.nodeQuery()).isEqualTo("some node query");
        assertThat(configuration.logProgress()).isEqualTo(true);
        assertThat(configuration.readConcurrency()).isEqualTo(new Concurrency(4));
        assertThat(configuration.relationshipCount()).isEqualTo(-1);
        assertThat(configuration.relationshipQuery()).isEqualTo("some relationship query");
        assertThat(configuration.username()).isEqualTo("some user");
    }

    // interesting how native and Cypher are not symmetric
    @ParameterizedTest
    @ValueSource(strings = {"nodeQuery", "relationshipQuery"})
    void shouldDisallowCypherProjectConfigurationWithConfigurationCertainKeywordsInIt(String key) {
        var service = new CatalogConfigurationService();

        try {
            service.parseCypherProjectConfiguration(
                new User("some user", false),
                GraphName.parse("some graph"),
                "some node query",
                "some relationship query",
                Map.of(key, "yeah, that's not allowed")
            );
            fail();
        } catch (RuntimeException e) {
            assertThat(e).hasMessageContaining("Unexpected configuration key: " + key);
        }
    }

    // here it is construction of the configuration that has some early validation logic
    @ParameterizedTest
    @ValueSource(strings = {"nodeProjection", "relationshipProjection"})
    void shouldDisallowCypherProjectConfigurationWithConfigurationCertainKeywordsInItButDifferent(String key) {
        var service = new CatalogConfigurationService();

        try {
            service.parseCypherProjectConfiguration(
                new User("some user", false),
                GraphName.parse("some graph"),
                "some node query",
                "some relationship query",
                Map.of(key, "yeah, that's not allowed")
            );
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageContaining("Invalid key: " + key);
        }
    }

    @ParameterizedTest
    @MethodSource("stuffThatIsNotStringOrListOfString")
    void shouldRejectNodeLabelsThatAreNotStringOrListOfString(Object notStringOrListOfString) {
        var service = new CatalogConfigurationService();

        assertThatIllegalArgumentException().isThrownBy(() -> {
            service.parseGraphStreamNodePropertiesConfiguration(
                GraphName.parse("some graph"),
                "foo",
                notStringOrListOfString,
                Map.of()
            );
        }).withMessageStartingWith("Type mismatch for nodeLabels: expected List<String> or String, but found");
    }

    private static Stream<Object> stuffThatIsNotStringOrListOfString() {
        return Stream.of(42, 3.14, true, List.of(87, 23), Map.of("some", "entry"));
    }
}
