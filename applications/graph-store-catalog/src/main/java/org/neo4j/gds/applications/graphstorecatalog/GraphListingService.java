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

import org.neo4j.gds.api.User;
import org.neo4j.gds.core.loading.GraphStoreCatalogEntry;
import org.neo4j.gds.core.loading.GraphStoreCatalogService;

import java.util.List;
import java.util.stream.Stream;

/**
 * This is just an accessor that helps test other things more easily
 */
class GraphListingService {
    private final GraphStoreCatalogService graphStoreCatalogService;

    GraphListingService(GraphStoreCatalogService graphStoreCatalogService) {
        this.graphStoreCatalogService = graphStoreCatalogService;
    }

    List<GraphStoreCatalogEntry> listGraphs(User user) {
        var pairStream = user.isAdmin()
            ? listAll()
            : listForUser(user);

        return pairStream.toList();
    }

    private Stream<GraphStoreCatalogEntry> listAll() {
        return graphStoreCatalogService.getAllGraphStores();
    }

    private Stream<GraphStoreCatalogEntry> listForUser(User user) {
        return graphStoreCatalogService.getGraphStores(user).stream();
    }
}
