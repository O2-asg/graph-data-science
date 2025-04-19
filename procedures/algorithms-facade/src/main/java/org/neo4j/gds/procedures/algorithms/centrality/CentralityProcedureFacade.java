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
package org.neo4j.gds.procedures.algorithms.centrality;

import org.neo4j.gds.api.ProcedureReturnColumns;
import org.neo4j.gds.applications.ApplicationsFacade;
import org.neo4j.gds.applications.algorithms.centrality.CentralityAlgorithmsEstimationModeBusinessFacade;
import org.neo4j.gds.applications.algorithms.centrality.CentralityAlgorithmsStatsModeBusinessFacade;
import org.neo4j.gds.applications.algorithms.centrality.CentralityAlgorithmsStreamModeBusinessFacade;
import org.neo4j.gds.applications.algorithms.centrality.CentralityAlgorithmsWriteModeBusinessFacade;
import org.neo4j.gds.applications.algorithms.machinery.MemoryEstimateResult;
import org.neo4j.gds.articulationpoints.ArticulationPointsMutateConfig;
import org.neo4j.gds.articulationpoints.ArticulationPointsStatsConfig;
import org.neo4j.gds.articulationpoints.ArticulationPointsStreamConfig;
import org.neo4j.gds.articulationpoints.ArticulationPointsWriteConfig;
import org.neo4j.gds.betweenness.BetweennessCentralityStatsConfig;
import org.neo4j.gds.betweenness.BetweennessCentralityStreamConfig;
import org.neo4j.gds.betweenness.BetweennessCentralityWriteConfig;
import org.neo4j.gds.bridges.Bridge;
import org.neo4j.gds.bridges.BridgesStreamConfig;
import org.neo4j.gds.closeness.ClosenessCentralityStatsConfig;
import org.neo4j.gds.closeness.ClosenessCentralityStreamConfig;
import org.neo4j.gds.closeness.ClosenessCentralityWriteConfig;
import org.neo4j.gds.degree.DegreeCentralityStatsConfig;
import org.neo4j.gds.degree.DegreeCentralityStreamConfig;
import org.neo4j.gds.degree.DegreeCentralityWriteConfig;
import org.neo4j.gds.harmonic.DeprecatedTieredHarmonicCentralityWriteConfig;
import org.neo4j.gds.harmonic.HarmonicCentralityStatsConfig;
import org.neo4j.gds.harmonic.HarmonicCentralityStreamConfig;
import org.neo4j.gds.harmonic.HarmonicCentralityWriteConfig;
import org.neo4j.gds.influenceMaximization.InfluenceMaximizationStatsConfig;
import org.neo4j.gds.influenceMaximization.InfluenceMaximizationStreamConfig;
import org.neo4j.gds.influenceMaximization.InfluenceMaximizationWriteConfig;
import org.neo4j.gds.pagerank.PageRankMutateConfig;
import org.neo4j.gds.pagerank.PageRankStatsConfig;
import org.neo4j.gds.pagerank.PageRankStreamConfig;
import org.neo4j.gds.pagerank.PageRankWriteConfig;
import org.neo4j.gds.procedures.algorithms.centrality.stubs.ArticulationPointsMutateStub;
import org.neo4j.gds.procedures.algorithms.centrality.stubs.BetaClosenessCentralityMutateStub;
import org.neo4j.gds.procedures.algorithms.centrality.stubs.BetweennessCentralityMutateStub;
import org.neo4j.gds.procedures.algorithms.centrality.stubs.CelfMutateStub;
import org.neo4j.gds.procedures.algorithms.centrality.stubs.ClosenessCentralityMutateStub;
import org.neo4j.gds.procedures.algorithms.centrality.stubs.DegreeCentralityMutateStub;
import org.neo4j.gds.procedures.algorithms.centrality.stubs.HarmonicCentralityMutateStub;
import org.neo4j.gds.procedures.algorithms.centrality.stubs.PageRankMutateStub;
import org.neo4j.gds.procedures.algorithms.runners.AlgorithmExecutionScaffolding;
import org.neo4j.gds.procedures.algorithms.runners.EstimationModeRunner;
import org.neo4j.gds.procedures.algorithms.stubs.GenericStub;
import org.neo4j.gds.procedures.algorithms.stubs.MutateStub;

import java.util.Map;
import java.util.stream.Stream;

public final class CentralityProcedureFacade {
    private final ProcedureReturnColumns procedureReturnColumns;

    private final ArticulationPointsMutateStub articulationPointsMutateStub;
    private final PageRankMutateStub articleRankMutateStub;
    private final BetaClosenessCentralityMutateStub betaClosenessCentralityMutateStub;
    private final BetweennessCentralityMutateStub betweennessCentralityMutateStub;
    private final CelfMutateStub celfMutateStub;
    private final ClosenessCentralityMutateStub closenessCentralityMutateStub;
    private final DegreeCentralityMutateStub degreeCentralityMutateStub;
    private final MutateStub<PageRankMutateConfig, PageRankMutateResult> eigenVectorMutateStub;
    private final HarmonicCentralityMutateStub harmonicCentralityMutateStub;
    private final PageRankMutateStub pageRankMutateStub;

    private final ApplicationsFacade applicationsFacade;

    private final EstimationModeRunner estimationMode;
    private final AlgorithmExecutionScaffolding algorithmExecutionScaffolding;
    private final AlgorithmExecutionScaffolding algorithmExecutionScaffoldingForStreamMode;

    private CentralityProcedureFacade(
        ProcedureReturnColumns procedureReturnColumns,
        PageRankMutateStub articleRankMutateStub,
        BetaClosenessCentralityMutateStub betaClosenessCentralityMutateStub,
        BetweennessCentralityMutateStub betweennessCentralityMutateStub,
        CelfMutateStub celfMutateStub,
        ClosenessCentralityMutateStub closenessCentralityMutateStub,
        DegreeCentralityMutateStub degreeCentralityMutateStub,
        MutateStub<PageRankMutateConfig, PageRankMutateResult> eigenVectorMutateStub,
        HarmonicCentralityMutateStub harmonicCentralityMutateStub,
        ArticulationPointsMutateStub articulationPointsMutateStub,
        PageRankMutateStub pageRankMutateStub,
        ApplicationsFacade applicationsFacade,
        EstimationModeRunner estimationMode,
        AlgorithmExecutionScaffolding algorithmExecutionScaffolding,
        AlgorithmExecutionScaffolding algorithmExecutionScaffoldingForStreamMode
    ) {
        this.procedureReturnColumns = procedureReturnColumns;
        this.articleRankMutateStub = articleRankMutateStub;
        this.articulationPointsMutateStub = articulationPointsMutateStub;
        this.betaClosenessCentralityMutateStub = betaClosenessCentralityMutateStub;
        this.betweennessCentralityMutateStub = betweennessCentralityMutateStub;
        this.celfMutateStub = celfMutateStub;
        this.closenessCentralityMutateStub = closenessCentralityMutateStub;
        this.degreeCentralityMutateStub = degreeCentralityMutateStub;
        this.eigenVectorMutateStub = eigenVectorMutateStub;
        this.harmonicCentralityMutateStub = harmonicCentralityMutateStub;
        this.pageRankMutateStub = pageRankMutateStub;
        this.applicationsFacade = applicationsFacade;
        this.estimationMode = estimationMode;
        this.algorithmExecutionScaffolding = algorithmExecutionScaffolding;
        this.algorithmExecutionScaffoldingForStreamMode = algorithmExecutionScaffoldingForStreamMode;
    }

    public static CentralityProcedureFacade create(
        GenericStub genericStub,
        ApplicationsFacade applicationsFacade,
        ProcedureReturnColumns procedureReturnColumns,
        EstimationModeRunner estimationModeRunner,
        AlgorithmExecutionScaffolding algorithmExecutionScaffolding,
        AlgorithmExecutionScaffolding algorithmExecutionScaffoldingForStreamMode
    ) {
        var articleRankMutateStub = new PageRankMutateStub(
            genericStub,
            applicationsFacade,
            procedureReturnColumns,
            applicationsFacade.centrality().mutate()::articleRank
        );
        var betaClosenessCentralityMutateStub = new BetaClosenessCentralityMutateStub(
            genericStub,
            applicationsFacade,
            procedureReturnColumns
        );
        var betweennessCentralityMutateStub = new BetweennessCentralityMutateStub(
            genericStub,
            applicationsFacade,
            procedureReturnColumns
        );
        var celfMutateStub = new CelfMutateStub(genericStub, applicationsFacade);
        var closenessCentralityMutateStub = new ClosenessCentralityMutateStub(
            genericStub,
            applicationsFacade,
            procedureReturnColumns
        );
        var degreeCentralityMutateStub = new DegreeCentralityMutateStub(
            genericStub,
            applicationsFacade,
            procedureReturnColumns
        );
        var eigenVectorMutateStub = new MutateStubConfigurationValidationDecorator<>(
            new PageRankMutateStub(
                genericStub,
                applicationsFacade,
                procedureReturnColumns,
                applicationsFacade.centrality().mutate()::eigenVector
            ),
            "dampingFactor"
        );
        var harmonicCentralityMutateStub = new HarmonicCentralityMutateStub(
            genericStub,
            applicationsFacade,
            procedureReturnColumns
        );
        var pageRankMutateStub = new PageRankMutateStub(
            genericStub,
            applicationsFacade,
            procedureReturnColumns,
            applicationsFacade.centrality().mutate()::pageRank
        );

        var articulationPointsMutateStub  = new ArticulationPointsMutateStub(
            genericStub,
            applicationsFacade
        );

        return new CentralityProcedureFacade(
            procedureReturnColumns,
            articleRankMutateStub,
            betaClosenessCentralityMutateStub,
            betweennessCentralityMutateStub,
            celfMutateStub,
            closenessCentralityMutateStub,
            degreeCentralityMutateStub,
            eigenVectorMutateStub,
            harmonicCentralityMutateStub,
            articulationPointsMutateStub,
            pageRankMutateStub,
            applicationsFacade,
            estimationModeRunner,
            algorithmExecutionScaffolding,
            algorithmExecutionScaffoldingForStreamMode
        );
    }

    public Stream<AlphaHarmonicStreamResult> alphaHarmonicCentralityStream(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new AlphaHarmonicCentralityResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            HarmonicCentralityStreamConfig::of,
            streamMode()::harmonicCentrality,
            resultBuilder
        );
    }

    public Stream<AlphaHarmonicWriteResult> alphaHarmonicCentralityWrite(
        String graphName,
        Map<String, Object> configuration
    ) {
        var shouldComputeCentralityDistribution = procedureReturnColumns.contains("centralityDistribution");
        var resultBuilder = new AlphaHarmonicCentralityResultBuilderForWriteMode(shouldComputeCentralityDistribution);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            DeprecatedTieredHarmonicCentralityWriteConfig::of,
            writeMode()::harmonicCentrality,
            resultBuilder
        );
    }

    public PageRankMutateStub articleRankMutateStub() {
        return articleRankMutateStub;
    }

    public Stream<PageRankStatsResult> articleRankStats(String graphName, Map<String, Object> configuration) {
        var shouldComputeSimilarityDistribution = procedureReturnColumns.contains("centralityDistribution");
        var resultBuilder = new PageRankResultBuilderForStatsMode(shouldComputeSimilarityDistribution);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            PageRankStatsConfig::of,
            statsMode()::articleRank,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> articleRankStatsEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            PageRankStatsConfig::of,
            configuration -> estimationMode().pageRank(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<CentralityStreamResult> articleRankStream(String graphName, Map<String, Object> configuration) {
        var resultBuilder = new PageRankResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            PageRankStreamConfig::of,
            streamMode()::articleRank,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> articleRankStreamEstimate(
        Object graphNameOrConfiguration, Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            PageRankStreamConfig::of,
            configuration -> estimationMode().pageRank(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<PageRankWriteResult> articleRankWrite(
        String graphName,
        Map<String, Object> configuration
    ) {
        var shouldComputeCentralityDistribution = procedureReturnColumns.contains("centralityDistribution");
        var resultBuilder = new PageRankResultBuilderForWriteMode(shouldComputeCentralityDistribution);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            PageRankWriteConfig::of,
            writeMode()::articleRank,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> articleRankWriteEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            PageRankWriteConfig::of,
            configuration -> estimationMode().pageRank(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public BetaClosenessCentralityMutateStub betaClosenessCentralityMutateStub() {
        return betaClosenessCentralityMutateStub;
    }

    public Stream<BetaClosenessCentralityWriteResult> betaClosenessCentralityWrite(
        String graphName,
        Map<String, Object> configuration
    ) {
        var shouldComputeCentralityDistribution = procedureReturnColumns.contains("centralityDistribution");
        var resultBuilder = new BetaClosenessCentralityResultBuilderForWriteMode(shouldComputeCentralityDistribution);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            ClosenessCentralityWriteConfig::of,
            writeMode()::closenessCentrality,
            resultBuilder
        );
    }

    public BetweennessCentralityMutateStub betweennessCentralityMutateStub() {
        return betweennessCentralityMutateStub;
    }

    public Stream<CentralityStatsResult> betweennessCentralityStats(
        String graphName,
        Map<String, Object> configuration
    ) {
        var shouldComputeSimilarityDistribution = procedureReturnColumns.contains("centralityDistribution");
        var resultBuilder = new BetweennessCentralityResultBuilderForStatsMode(shouldComputeSimilarityDistribution);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            BetweennessCentralityStatsConfig::of,
            statsMode()::betweennessCentrality,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> betweennessCentralityStatsEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            BetweennessCentralityStatsConfig::of,
            configuration -> estimationMode().betweennessCentrality(
                configuration,
                graphNameOrConfiguration
            )
        );

        return Stream.of(result);
    }

    public Stream<CentralityStreamResult> betweennessCentralityStream(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new BetweennessCentralityResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            BetweennessCentralityStreamConfig::of,
            streamMode()::betweennessCentrality,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> betweennessCentralityStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            BetweennessCentralityStreamConfig::of,
            configuration -> estimationMode().betweennessCentrality(
                configuration,
                graphNameOrConfiguration
            )
        );

        return Stream.of(result);
    }



    public Stream<CentralityWriteResult> betweennessCentralityWrite(
        String graphNameAsString,
        Map<String, Object> rawConfiguration
    ) {
        var shouldComputeCentralityDistribution = procedureReturnColumns.contains("centralityDistribution");
        var resultBuilder = new BetweennessCentralityResultBuilderForWriteMode(shouldComputeCentralityDistribution);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphNameAsString,
            rawConfiguration,
            BetweennessCentralityWriteConfig::of,
            writeMode()::betweennessCentrality,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> betweennessCentralityWriteEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            BetweennessCentralityWriteConfig::of,
            configuration -> estimationMode().betweennessCentrality(
                configuration,
                graphNameOrConfiguration
            )
        );

        return Stream.of(result);
    }


    public Stream<ArticulationPoint> articulationPointsStream(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new ArticulationPointsResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            ArticulationPointsStreamConfig::of,
            streamMode()::articulationPoints,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> articulationPointsStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            ArticulationPointsStreamConfig::of,
            configuration -> estimationMode().articulationPoints(
                configuration,
                graphNameOrConfiguration
            )
        );

        return Stream.of(result);
    }
    public ArticulationPointsMutateStub articulationPointsMutateStub(){return articulationPointsMutateStub;}

    public Stream<MemoryEstimateResult> articulationPointsMutateEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            ArticulationPointsMutateConfig::of,
            configuration -> estimationMode().articulationPoints(
                configuration,
                graphNameOrConfiguration
            )
        );

        return Stream.of(result);
    }

    public Stream<ArticulationPointsStatsResult> articulationPointsStats(
        String graphNameAsString,
        Map<String, Object> rawConfiguration
    ) {
        return algorithmExecutionScaffolding.runAlgorithm(
            graphNameAsString,
            rawConfiguration,
            ArticulationPointsStatsConfig::of,
            statsMode()::articulationPoints,
            new ArticulationPointsResultBuilderForStatsMode()
        );
    }

    public Stream<MemoryEstimateResult> articulationPointsStatsEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            ArticulationPointsStatsConfig::of,
            configuration -> estimationMode().articulationPoints(
                configuration,
                graphNameOrConfiguration
            )
        );

        return Stream.of(result);
    }

    public Stream<ArticulationPointsWriteResult> articulationPointsWrite(
        String graphNameAsString,
        Map<String, Object> rawConfiguration
    ) {
        return algorithmExecutionScaffolding.runAlgorithm(
            graphNameAsString,
            rawConfiguration,
            ArticulationPointsWriteConfig::of,
            writeMode()::articulationPoints,
            new ArticulationPointsResultBuilderForWriteMode()
        );
    }

    public Stream<MemoryEstimateResult> articulationPointsWriteEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            ArticulationPointsWriteConfig::of,
            configuration -> estimationMode().articulationPoints(
                configuration,
                graphNameOrConfiguration
            )
        );

        return Stream.of(result);
    }


    public Stream<Bridge> bridgesStream(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new BridgesResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            BridgesStreamConfig::of,
            streamMode()::bridges,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> bridgesStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            BridgesStreamConfig::of,
            configuration -> estimationMode().bridges(
                configuration,
                graphNameOrConfiguration
            )
        );

        return Stream.of(result);
    }

    public CelfMutateStub celfMutateStub() {
        return celfMutateStub;
    }

    public Stream<CELFStatsResult> celfStats(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new CelfResultBuilderForStatsMode();

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            InfluenceMaximizationStatsConfig::of,
            statsMode()::celf,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> celfStatsEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            InfluenceMaximizationStatsConfig::of,
            configuration -> estimationMode().celf(
                configuration,
                graphNameOrConfiguration
            )
        );

        return Stream.of(result);
    }

    public Stream<CELFStreamResult> celfStream(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new CelfResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            InfluenceMaximizationStreamConfig::of,
            streamMode()::celf,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> celfStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            InfluenceMaximizationStreamConfig::of,
            configuration -> estimationMode().celf(
                configuration,
                graphNameOrConfiguration
            )
        );

        return Stream.of(result);
    }

    public Stream<CELFWriteResult> celfWrite(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new CelfResultBuilderForWriteMode();

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            InfluenceMaximizationWriteConfig::of,
            writeMode()::celf,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> celfWriteEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            InfluenceMaximizationWriteConfig::of,
            configuration -> estimationMode().celf(
                configuration,
                graphNameOrConfiguration
            )
        );

        return Stream.of(result);
    }

    public ClosenessCentralityMutateStub closenessCentralityMutateStub() {
        return closenessCentralityMutateStub;
    }

    public Stream<CentralityStatsResult> closenessCentralityStats(String graphName, Map<String, Object> configuration) {
        var shouldComputeSimilarityDistribution = procedureReturnColumns.contains("centralityDistribution");
        var resultBuilder = new ClosenessCentralityResultBuilderForStatsMode(shouldComputeSimilarityDistribution);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            ClosenessCentralityStatsConfig::of,
            statsMode()::closenessCentrality,
            resultBuilder
        );
    }

    public Stream<CentralityStreamResult> closenessCentralityStream(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new ClosenessCentralityResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            ClosenessCentralityStreamConfig::of,
            streamMode()::closenessCentrality,
            resultBuilder
        );
    }

    public Stream<CentralityWriteResult> closenessCentralityWrite(String graphName, Map<String, Object> configuration) {
        var shouldComputeCentralityDistribution = procedureReturnColumns.contains("centralityDistribution");
        var resultBuilder = new ClosenessCentralityResultBuilderForWriteMode(shouldComputeCentralityDistribution);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            ClosenessCentralityWriteConfig::of,
            writeMode()::closenessCentrality,
            resultBuilder
        );
    }

    public DegreeCentralityMutateStub degreeCentralityMutateStub() {
        return degreeCentralityMutateStub;
    }

    public Stream<CentralityStatsResult> degreeCentralityStats(String graphName, Map<String, Object> configuration) {
        var shouldComputeSimilarityDistribution = procedureReturnColumns.contains("centralityDistribution");
        var resultBuilder = new DegreeCentralityResultBuilderForStatsMode(shouldComputeSimilarityDistribution);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            DegreeCentralityStatsConfig::of,
            statsMode()::degreeCentrality,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> degreeCentralityStatsEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            DegreeCentralityStatsConfig::of,
            configuration -> estimationMode().degreeCentrality(
                configuration,
                graphNameOrConfiguration
            )
        );

        return Stream.of(result);
    }

    public Stream<CentralityStreamResult> degreeCentralityStream(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new DegreeCentralityResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            DegreeCentralityStreamConfig::of,
            streamMode()::degreeCentrality,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> degreeCentralityStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            DegreeCentralityStreamConfig::of,
            configuration -> estimationMode().degreeCentrality(
                configuration,
                graphNameOrConfiguration
            )
        );

        return Stream.of(result);
    }

    public Stream<CentralityWriteResult> degreeCentralityWrite(String graphName, Map<String, Object> configuration) {
        var shouldComputeCentralityDistribution = procedureReturnColumns.contains("centralityDistribution");
        var resultBuilder = new DegreeCentralityResultBuilderForWriteMode(shouldComputeCentralityDistribution);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            DegreeCentralityWriteConfig::of,
            writeMode()::degreeCentrality,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> degreeCentralityWriteEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            DegreeCentralityWriteConfig::of,
            configuration -> estimationMode().degreeCentrality(
                configuration,
                graphNameOrConfiguration
            )
        );

        return Stream.of(result);
    }

    public MutateStub<PageRankMutateConfig, PageRankMutateResult> eigenVectorMutateStub() {
        return eigenVectorMutateStub;
    }

    public Stream<PageRankStatsResult> eigenvectorStats(String graphName, Map<String, Object> configuration) {
        validateKeyNotPresent(configuration, "dampingFactor");

        var shouldComputeSimilarityDistribution = procedureReturnColumns.contains("centralityDistribution");
        var resultBuilder = new PageRankResultBuilderForStatsMode(shouldComputeSimilarityDistribution);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            PageRankStatsConfig::of,
            statsMode()::eigenVector,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> eigenvectorStatsEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        validateKeyNotPresent(algorithmConfiguration, "dampingFactor");

        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            PageRankStatsConfig::of,
            configuration -> estimationMode().pageRank(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<CentralityStreamResult> eigenvectorStream(String graphName, Map<String, Object> configuration) {
        validateKeyNotPresent(configuration, "dampingFactor");

        var resultBuilder = new PageRankResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            PageRankStreamConfig::of,
            streamMode()::eigenvector,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> eigenvectorStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        validateKeyNotPresent(algorithmConfiguration, "dampingFactor");

        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            PageRankStreamConfig::of,
            configuration -> estimationMode().pageRank(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<PageRankWriteResult> eigenvectorWrite(String graphName, Map<String, Object> configuration) {
        validateKeyNotPresent(configuration, "dampingFactor");

        var shouldComputeCentralityDistribution = procedureReturnColumns.contains("centralityDistribution");
        var resultBuilder = new PageRankResultBuilderForWriteMode(shouldComputeCentralityDistribution);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            PageRankWriteConfig::of,
            writeMode()::eigenvector,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> eigenvectorWriteEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        validateKeyNotPresent(algorithmConfiguration, "dampingFactor");

        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            PageRankWriteConfig::of,
            configuration -> estimationMode().pageRank(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public HarmonicCentralityMutateStub harmonicCentralityMutateStub() {
        return harmonicCentralityMutateStub;
    }

    public Stream<CentralityStatsResult> harmonicCentralityStats(String graphName, Map<String, Object> configuration) {
        var shouldComputeSimilarityDistribution = procedureReturnColumns.contains("centralityDistribution");
        var resultBuilder = new HarmonicCentralityResultBuilderForStatsMode(shouldComputeSimilarityDistribution);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            HarmonicCentralityStatsConfig::of,
            statsMode()::harmonicCentrality,
            resultBuilder
        );
    }

    public Stream<CentralityStreamResult> harmonicCentralityStream(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new HarmonicCentralityResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            HarmonicCentralityStreamConfig::of,
            streamMode()::harmonicCentrality,
            resultBuilder
        );
    }

    public Stream<CentralityWriteResult> harmonicCentralityWrite(String graphName, Map<String, Object> configuration) {
        var shouldComputeCentralityDistribution = procedureReturnColumns.contains("centralityDistribution");
        var resultBuilder = new HarmonicCentralityResultBuilderForWriteMode(shouldComputeCentralityDistribution);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            HarmonicCentralityWriteConfig::of,
            writeMode()::harmonicCentrality,
            resultBuilder
        );
    }

    public PageRankMutateStub pageRankMutateStub() {
        return pageRankMutateStub;
    }

    public Stream<PageRankStatsResult> pageRankStats(String graphName, Map<String, Object> configuration) {
        var shouldComputeSimilarityDistribution = procedureReturnColumns.contains("centralityDistribution");
        var resultBuilder = new PageRankResultBuilderForStatsMode(shouldComputeSimilarityDistribution);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            PageRankStatsConfig::of,
            statsMode()::pageRank,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> pageRankStatsEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            PageRankStatsConfig::of,
            configuration -> estimationMode().pageRank(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<CentralityStreamResult> pageRankStream(String graphName, Map<String, Object> configuration) {
        var resultBuilder = new PageRankResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            PageRankStreamConfig::of,
            streamMode()::pageRank,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> pageRankStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            PageRankStreamConfig::of,
            configuration -> estimationMode().pageRank(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<PageRankWriteResult> pageRankWrite(String graphName, Map<String, Object> configuration) {
        var shouldComputeCentralityDistribution = procedureReturnColumns.contains("centralityDistribution");
        var resultBuilder = new PageRankResultBuilderForWriteMode(shouldComputeCentralityDistribution);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            PageRankWriteConfig::of,
            writeMode()::pageRank,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> pageRankWriteEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            PageRankWriteConfig::of,
            configuration -> estimationMode().pageRank(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    private CentralityAlgorithmsEstimationModeBusinessFacade estimationMode() {
        return applicationsFacade.centrality().estimate();
    }

    private CentralityAlgorithmsStatsModeBusinessFacade statsMode() {
        return applicationsFacade.centrality().stats();
    }

    private CentralityAlgorithmsStreamModeBusinessFacade streamMode() {
        return applicationsFacade.centrality().stream();
    }

    private CentralityAlgorithmsWriteModeBusinessFacade writeMode() {
        return applicationsFacade.centrality().write();
    }

    private void validateKeyNotPresent(Map<String, Object> configuration, String key) {
        if (configuration.containsKey(key)) {
            throw new IllegalArgumentException("Unexpected configuration key: " + key);
        }
    }
}
