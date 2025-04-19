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
package org.neo4j.gds.procedures;

import org.neo4j.gds.api.AlgorithmMetaDataSetter;
import org.neo4j.gds.api.ProcedureReturnColumns;
import org.neo4j.gds.applications.ApplicationsFacade;
import org.neo4j.gds.applications.modelcatalog.ModelRepository;
import org.neo4j.gds.applications.algorithms.machinery.AlgorithmProcessingTemplate;
import org.neo4j.gds.applications.algorithms.machinery.MemoryGuard;
import org.neo4j.gds.applications.algorithms.machinery.RequestScopedDependencies;
import org.neo4j.gds.applications.algorithms.machinery.WriteContext;
import org.neo4j.gds.applications.graphstorecatalog.GraphCatalogApplications;
import org.neo4j.gds.applications.modelcatalog.ModelCatalogApplications;
import org.neo4j.gds.configuration.DefaultsConfiguration;
import org.neo4j.gds.configuration.LimitsConfiguration;
import org.neo4j.gds.core.loading.GraphStoreCatalogService;
import org.neo4j.gds.core.model.ModelCatalog;
import org.neo4j.gds.logging.Log;
import org.neo4j.gds.metrics.algorithms.AlgorithmMetricsService;
import org.neo4j.gds.metrics.procedures.DeprecatedProceduresMetricService;
import org.neo4j.gds.metrics.projections.ProjectionMetricsService;
import org.neo4j.gds.procedures.algorithms.AlgorithmsProcedureFacade;
import org.neo4j.gds.procedures.algorithms.configuration.ConfigurationCreator;
import org.neo4j.gds.procedures.algorithms.configuration.ConfigurationParser;
import org.neo4j.gds.procedures.catalog.GraphCatalogProcedureFacade;
import org.neo4j.gds.procedures.modelcatalog.ModelCatalogProcedureFacade;
import org.neo4j.gds.procedures.operations.OperationsProcedureFacade;
import org.neo4j.gds.procedures.pipelines.PipelinesProcedureFacade;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.api.KernelTransaction;

import java.util.Optional;
import java.util.function.Function;

public class GraphDataScienceProcedures {
    private final Log log;

    private final AlgorithmsProcedureFacade algorithmsProcedureFacade;
    private final GraphCatalogProcedureFacade graphCatalogProcedureFacade;
    private final ModelCatalogProcedureFacade modelCatalogProcedureFacade;
    private final OperationsProcedureFacade operationsProcedureFacade;
    private final PipelinesProcedureFacade pipelinesProcedureFacade;

    private final DeprecatedProceduresMetricService deprecatedProceduresMetricService;

    /**
     * Keeping this package private to encourage use of @{@link GraphDataScienceProceduresBuilder}
     */
    GraphDataScienceProcedures(
        Log log,
        AlgorithmsProcedureFacade algorithmsProcedureFacade,
        GraphCatalogProcedureFacade graphCatalogProcedureFacade,
        ModelCatalogProcedureFacade modelCatalogProcedureFacade,
        OperationsProcedureFacade operationsProcedureFacade,
        PipelinesProcedureFacade pipelinesProcedureFacade,
        DeprecatedProceduresMetricService deprecatedProceduresMetricService
    ) {
        this.log = log;
        this.algorithmsProcedureFacade = algorithmsProcedureFacade;
        this.graphCatalogProcedureFacade = graphCatalogProcedureFacade;
        this.modelCatalogProcedureFacade = modelCatalogProcedureFacade;
        this.operationsProcedureFacade = operationsProcedureFacade;
        this.pipelinesProcedureFacade = pipelinesProcedureFacade;
        this.deprecatedProceduresMetricService = deprecatedProceduresMetricService;
    }

    public static GraphDataScienceProcedures create(
        Log log,
        DefaultsConfiguration defaultsConfiguration,
        LimitsConfiguration limitsConfiguration,
        Optional<Function<AlgorithmProcessingTemplate, AlgorithmProcessingTemplate>> algorithmProcessingTemplateDecorator,
        Optional<Function<GraphCatalogApplications, GraphCatalogApplications>> graphCatalogApplicationsDecorator,
        Optional<Function<ModelCatalogApplications, ModelCatalogApplications>> modelCatalogApplicationsDecorator,
        GraphStoreCatalogService graphStoreCatalogService,
        MemoryGuard memoryGuard,
        AlgorithmMetricsService algorithmMetricsService,
        ProjectionMetricsService projectionMetricsService,
        AlgorithmMetaDataSetter algorithmMetaDataSetter,
        KernelTransaction kernelTransaction,
        RequestScopedDependencies requestScopedDependencies,
        WriteContext writeContext,
        ProcedureReturnColumns procedureReturnColumns,
        GraphCatalogProcedureFacadeFactory graphCatalogProcedureFacadeFactory,
        GraphDatabaseService graphDatabaseService,
        Transaction transaction,
        AlgorithmProcedureFacadeBuilderFactory algorithmProcedureFacadeBuilderFactory,
        DeprecatedProceduresMetricService deprecatedProceduresMetricService,
        ModelCatalog modelCatalog,
        ModelRepository modelRepository
    ) {
        var applicationsFacade = ApplicationsFacade.create(
            log,
            algorithmProcessingTemplateDecorator,
            graphCatalogApplicationsDecorator,
            modelCatalogApplicationsDecorator,
            graphStoreCatalogService,
            memoryGuard,
            algorithmMetricsService,
            projectionMetricsService,
            requestScopedDependencies,
            writeContext,
            modelCatalog,
            modelRepository
        );

        var graphCatalogProcedureFacade = graphCatalogProcedureFacadeFactory.createGraphCatalogProcedureFacade(
            applicationsFacade,
            graphDatabaseService,
            kernelTransaction,
            transaction,
            requestScopedDependencies,
            writeContext,
            procedureReturnColumns
        );

        var modelCatalogProcedureFacade = new ModelCatalogProcedureFacade(applicationsFacade);

        var configurationParser = new ConfigurationParser(defaultsConfiguration, limitsConfiguration);
        var configurationCreator = new ConfigurationCreator(configurationParser, requestScopedDependencies.getUser());

        var algorithmProcedureFacadeBuilder = algorithmProcedureFacadeBuilderFactory.create(
            configurationParser,
            configurationCreator,
            requestScopedDependencies,
            kernelTransaction,
            algorithmMetaDataSetter,
            applicationsFacade,
            procedureReturnColumns
        );

        var centralityProcedureFacade = algorithmProcedureFacadeBuilder.createCentralityProcedureFacade();
        var communityProcedureFacade = algorithmProcedureFacadeBuilder.createCommunityProcedureFacade();
        var miscellaneousProcedureFacade = algorithmProcedureFacadeBuilder.createMiscellaneousProcedureFacade();
        var nodeEmbeddingsProcedureFacade = algorithmProcedureFacadeBuilder.createNodeEmbeddingsProcedureFacade();
        var pathFindingProcedureFacade = algorithmProcedureFacadeBuilder.createPathFindingProcedureFacade();
        var similarityProcedureFacade = algorithmProcedureFacadeBuilder.createSimilarityProcedureFacade();

        var operationsProcedureFacade = new OperationsProcedureFacade(applicationsFacade);

        var pipelinesProcedureFacade = new PipelinesProcedureFacade(requestScopedDependencies.getUser());

        return new GraphDataScienceProceduresBuilder(log)
            .with(centralityProcedureFacade)
            .with(communityProcedureFacade)
            .with(graphCatalogProcedureFacade)
            .with(miscellaneousProcedureFacade)
            .with(modelCatalogProcedureFacade)
            .with(nodeEmbeddingsProcedureFacade)
            .with(operationsProcedureFacade)
            .with(pathFindingProcedureFacade)
            .with(pipelinesProcedureFacade)
            .with(similarityProcedureFacade)
            .with(deprecatedProceduresMetricService)
            .build();
    }

    public Log log() {
        return log;
    }

    public AlgorithmsProcedureFacade algorithms() {
        return algorithmsProcedureFacade;
    }

    public GraphCatalogProcedureFacade graphCatalog() {
        return graphCatalogProcedureFacade;
    }

    public ModelCatalogProcedureFacade modelCatalog() {
        return modelCatalogProcedureFacade;
    }

    public OperationsProcedureFacade operations() {
        return operationsProcedureFacade;
    }

    public PipelinesProcedureFacade pipelines() {
        return pipelinesProcedureFacade;
    }

    public DeprecatedProceduresMetricService deprecatedProcedures() {
        return deprecatedProceduresMetricService;
    }
}
