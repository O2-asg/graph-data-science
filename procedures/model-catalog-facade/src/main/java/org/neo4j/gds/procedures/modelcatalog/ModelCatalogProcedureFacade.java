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
package org.neo4j.gds.procedures.modelcatalog;

import org.neo4j.gds.applications.ApplicationsFacade;
import org.neo4j.gds.applications.modelcatalog.ModelExistsResult;

import java.util.stream.Stream;

public class ModelCatalogProcedureFacade {
    public static final String NO_VALUE = "__NO_VALUE";

    private final ModelNameValidationService modelNameValidationService = new ModelNameValidationService();

    private final ApplicationsFacade applicationsFacade;

    public ModelCatalogProcedureFacade(ApplicationsFacade applicationsFacade) {
        this.applicationsFacade = applicationsFacade;
    }

    public Stream<ModelCatalogResult> drop(String modelNameAsString, boolean failIfMissing) {
        var modelName = modelNameValidationService.validate(modelNameAsString);

        var model = applicationsFacade.modelCatalog().drop(modelName, failIfMissing);

        return Stream.ofNullable(model).map(ModelCatalogResult::new);
    }

    public Stream<ModelExistsResult> exists(String modelNameAsString) {
        var modelName = modelNameValidationService.validate(modelNameAsString);

        var result = applicationsFacade.modelCatalog().exists(modelName);

        return Stream.of(result);
    }

    public Stream<ModelCatalogResult> list(String modelName) {
        if (modelName == null || modelName.equals(NO_VALUE)) return list();

        return lookup(modelName);
    }

    private Stream<ModelCatalogResult> list() {
        var models = applicationsFacade.modelCatalog().list();

        return models.stream().map(ModelCatalogResult::new);
    }

    private Stream<ModelCatalogResult> lookup(String modelNameAsString) {
        var modelName = modelNameValidationService.validate(modelNameAsString);

        var model = applicationsFacade.modelCatalog().lookup(modelName);

        if (model == null) return Stream.empty();

        var result = new ModelCatalogResult(model);

        return Stream.of(result);
    }
}
