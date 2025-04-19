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

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.api.GraphName;
import org.neo4j.gds.api.ResultStore;
import org.neo4j.gds.api.User;
import org.neo4j.gds.core.loading.GraphStoreCatalogEntry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListGraphApplicationTest {
    @Test
    void shouldListGraphs() {
        var graphListingService = mock(GraphListingService.class);
        var degreeDistributionApplier = mock(DegreeDistributionApplier.class);
        var application = new ListGraphApplication(graphListingService, degreeDistributionApplier);

        var config1 = new StubGraphProjectConfig();
        var config2 = new StubGraphProjectConfig();
        var config3 = new StubGraphProjectConfig();
        var graphStore1 = new StubGraphStore();
        var graphStore2 = new StubGraphStore();
        var graphStore3 = new StubGraphStore();
        List<GraphStoreCatalogEntry> listOfConfigsWithStores = List.of(
            new GraphStoreCatalogEntry(graphStore1, config1, ResultStore.EMPTY),
            new GraphStoreCatalogEntry(graphStore2, config2, ResultStore.EMPTY),
            new GraphStoreCatalogEntry(graphStore3, config3, ResultStore.EMPTY)
        );
        when(graphListingService.listGraphs(new User("foo", false))).thenReturn(listOfConfigsWithStores);
        var gswc1 = new GraphStoreCatalogEntry(graphStore1, config1, ResultStore.EMPTY);
        var gswc2 = new GraphStoreCatalogEntry(graphStore2, config2, ResultStore.EMPTY);
        var gswc3 = new GraphStoreCatalogEntry(graphStore3, config3, ResultStore.EMPTY);
        List<Pair<GraphStoreCatalogEntry, Map<String, Object>>> listOfGraphStoresWithConfigsSansDegreeDistributions = List.of(
            Pair.of(gswc1, null),
            Pair.of(gswc2, null),
            Pair.of(gswc3, null)
        );
        when(degreeDistributionApplier.process(listOfConfigsWithStores, false, null)).thenReturn(
            listOfGraphStoresWithConfigsSansDegreeDistributions);
        var result = application.list(
            new User("foo", false),
            Optional.empty(),
            false,
            null
        );

        assertThat(result).isEqualTo(listOfGraphStoresWithConfigsSansDegreeDistributions);
    }

    @Test
    void shouldFilterByName() {
        var graphListingService = mock(GraphListingService.class);
        var degreeDistributionApplier = mock(DegreeDistributionApplier.class);
        var application = new ListGraphApplication(graphListingService, degreeDistributionApplier);

        var config1 = new StubGraphProjectConfig("user1", "bar");
        var config2 = new StubGraphProjectConfig("user2", "bar");
        var config3 = new StubGraphProjectConfig("user3", "baz");
        var graphStore1 = new StubGraphStore();
        var graphStore2 = new StubGraphStore();
        var graphStore3 = new StubGraphStore();
        List<GraphStoreCatalogEntry> listOfConfigsWithStores = List.of(
            new GraphStoreCatalogEntry(graphStore1, config1, ResultStore.EMPTY),
            new GraphStoreCatalogEntry(graphStore2, config2, ResultStore.EMPTY),
            new GraphStoreCatalogEntry(graphStore3, config3, ResultStore.EMPTY)
        );
        when(graphListingService.listGraphs(new User("foo", false))).thenReturn(listOfConfigsWithStores);
        var gswc1 = new GraphStoreCatalogEntry(graphStore1, config1, ResultStore.EMPTY);
        var gswc2 = new GraphStoreCatalogEntry(graphStore2, config2, ResultStore.EMPTY);
        List<Pair<GraphStoreCatalogEntry, Map<String, Object>>> listOfGraphStoresWithConfigsSansDegreeDistributions = List.of(
            Pair.of(gswc1, null),
            Pair.of(gswc2, null)
        );
        when(degreeDistributionApplier.process(List.of(
            new GraphStoreCatalogEntry(graphStore1, config1, ResultStore.EMPTY),
            new GraphStoreCatalogEntry(graphStore2, config2, ResultStore.EMPTY)
        ), false, null)).thenReturn(
            listOfGraphStoresWithConfigsSansDegreeDistributions);
        var result = application.list(
            new User("foo", false),
            Optional.of(GraphName.parse("bar")),
            false,
            null
        );

        assertThat(result).isEqualTo(listOfGraphStoresWithConfigsSansDegreeDistributions);
    }
}
