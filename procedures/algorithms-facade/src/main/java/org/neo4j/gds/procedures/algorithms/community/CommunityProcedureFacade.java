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
package org.neo4j.gds.procedures.algorithms.community;

import org.neo4j.gds.api.CloseableResourceRegistry;
import org.neo4j.gds.api.ProcedureReturnColumns;
import org.neo4j.gds.applications.ApplicationsFacade;
import org.neo4j.gds.applications.algorithms.community.CommunityAlgorithmsEstimationModeBusinessFacade;
import org.neo4j.gds.applications.algorithms.community.CommunityAlgorithmsStatsModeBusinessFacade;
import org.neo4j.gds.applications.algorithms.community.CommunityAlgorithmsStreamModeBusinessFacade;
import org.neo4j.gds.applications.algorithms.community.CommunityAlgorithmsWriteModeBusinessFacade;
import org.neo4j.gds.applications.algorithms.machinery.MemoryEstimateResult;
import org.neo4j.gds.approxmaxkcut.config.ApproxMaxKCutStreamConfig;
import org.neo4j.gds.conductance.ConductanceStreamConfig;
import org.neo4j.gds.k1coloring.K1ColoringStatsConfig;
import org.neo4j.gds.k1coloring.K1ColoringStreamConfig;
import org.neo4j.gds.k1coloring.K1ColoringWriteConfig;
import org.neo4j.gds.kcore.KCoreDecompositionStatsConfig;
import org.neo4j.gds.kcore.KCoreDecompositionStreamConfig;
import org.neo4j.gds.kcore.KCoreDecompositionWriteConfig;
import org.neo4j.gds.kmeans.KmeansStatsConfig;
import org.neo4j.gds.kmeans.KmeansStreamConfig;
import org.neo4j.gds.kmeans.KmeansWriteConfig;
import org.neo4j.gds.labelpropagation.LabelPropagationStatsConfig;
import org.neo4j.gds.labelpropagation.LabelPropagationStreamConfig;
import org.neo4j.gds.labelpropagation.LabelPropagationWriteConfig;
import org.neo4j.gds.leiden.LeidenStatsConfig;
import org.neo4j.gds.leiden.LeidenStreamConfig;
import org.neo4j.gds.leiden.LeidenWriteConfig;
import org.neo4j.gds.louvain.LouvainStatsConfig;
import org.neo4j.gds.louvain.LouvainStreamConfig;
import org.neo4j.gds.louvain.LouvainWriteConfig;
import org.neo4j.gds.modularity.ModularityStatsConfig;
import org.neo4j.gds.modularity.ModularityStreamConfig;
import org.neo4j.gds.modularityoptimization.ModularityOptimizationStatsConfig;
import org.neo4j.gds.modularityoptimization.ModularityOptimizationStreamConfig;
import org.neo4j.gds.modularityoptimization.ModularityOptimizationWriteConfig;
import org.neo4j.gds.procedures.algorithms.community.stubs.ApproximateMaximumKCutMutateStub;
import org.neo4j.gds.procedures.algorithms.community.stubs.K1ColoringMutateStub;
import org.neo4j.gds.procedures.algorithms.community.stubs.KCoreMutateStub;
import org.neo4j.gds.procedures.algorithms.community.stubs.KMeansMutateStub;
import org.neo4j.gds.procedures.algorithms.community.stubs.LabelPropagationMutateStub;
import org.neo4j.gds.procedures.algorithms.community.stubs.LccMutateStub;
import org.neo4j.gds.procedures.algorithms.community.stubs.LeidenMutateStub;
import org.neo4j.gds.procedures.algorithms.community.stubs.LouvainMutateStub;
import org.neo4j.gds.procedures.algorithms.community.stubs.ModularityOptimizationMutateStub;
import org.neo4j.gds.procedures.algorithms.community.stubs.SccMutateStub;
import org.neo4j.gds.procedures.algorithms.community.stubs.TriangleCountMutateStub;
import org.neo4j.gds.procedures.algorithms.community.stubs.WccMutateStub;
import org.neo4j.gds.procedures.algorithms.runners.AlgorithmExecutionScaffolding;
import org.neo4j.gds.procedures.algorithms.runners.EstimationModeRunner;
import org.neo4j.gds.procedures.algorithms.stubs.GenericStub;
import org.neo4j.gds.result.StatisticsComputationInstructions;
import org.neo4j.gds.scc.SccAlphaWriteConfig;
import org.neo4j.gds.scc.SccStatsConfig;
import org.neo4j.gds.scc.SccStreamConfig;
import org.neo4j.gds.scc.SccWriteConfig;
import org.neo4j.gds.triangle.LocalClusteringCoefficientStatsConfig;
import org.neo4j.gds.triangle.LocalClusteringCoefficientStreamConfig;
import org.neo4j.gds.triangle.LocalClusteringCoefficientWriteConfig;
import org.neo4j.gds.triangle.TriangleCountBaseConfig;
import org.neo4j.gds.triangle.TriangleCountStatsConfig;
import org.neo4j.gds.triangle.TriangleCountStreamConfig;
import org.neo4j.gds.triangle.TriangleCountWriteConfig;
import org.neo4j.gds.triangle.TriangleStreamResult;
import org.neo4j.gds.wcc.WccStatsConfig;
import org.neo4j.gds.wcc.WccStreamConfig;
import org.neo4j.gds.wcc.WccWriteConfig;

import java.util.Map;
import java.util.stream.Stream;

public final class CommunityProcedureFacade {
    private final CloseableResourceRegistry closeableResourceRegistry;
    private final ProcedureReturnColumns procedureReturnColumns;

    private final ApproximateMaximumKCutMutateStub approximateMaximumKCutMutateStub;
    private final K1ColoringMutateStub k1ColoringMutateStub;
    private final KCoreMutateStub kCoreMutateStub;
    private final KMeansMutateStub kMeansMutateStub;
    private final LabelPropagationMutateStub labelPropagationMutateStub;
    private final LccMutateStub lccMutateStub;
    private final LeidenMutateStub leidenMutateStub;
    private final LouvainMutateStub louvainMutateStub;
    private final ModularityOptimizationMutateStub modularityOptimizationMutateStub;
    private final SccMutateStub sccMutateStub;
    private final TriangleCountMutateStub triangleCountMutateStub;
    private final WccMutateStub wccMutateStub;

    private final ApplicationsFacade applicationsFacade;

    private final EstimationModeRunner estimationMode;
    private final AlgorithmExecutionScaffolding algorithmExecutionScaffolding;
    private final AlgorithmExecutionScaffolding algorithmExecutionScaffoldingForStreamMode;

    private CommunityProcedureFacade(
        CloseableResourceRegistry closeableResourceRegistry,
        ProcedureReturnColumns procedureReturnColumns,
        ApproximateMaximumKCutMutateStub approximateMaximumKCutMutateStub,
        K1ColoringMutateStub k1ColoringMutateStub,
        KCoreMutateStub kCoreMutateStub,
        KMeansMutateStub kMeansMutateStub,
        LabelPropagationMutateStub labelPropagationMutateStub,
        LccMutateStub lccMutateStub,
        LeidenMutateStub leidenMutateStub,
        LouvainMutateStub louvainMutateStub,
        ModularityOptimizationMutateStub modularityOptimizationMutateStub,
        SccMutateStub sccMutateStub,
        TriangleCountMutateStub triangleCountMutateStub,
        WccMutateStub wccMutateStub,
        ApplicationsFacade applicationsFacade,
        EstimationModeRunner estimationMode,
        AlgorithmExecutionScaffolding algorithmExecutionScaffolding,
        AlgorithmExecutionScaffolding algorithmExecutionScaffoldingForStreamMode
    ) {
        this.closeableResourceRegistry = closeableResourceRegistry;
        this.procedureReturnColumns = procedureReturnColumns;
        this.approximateMaximumKCutMutateStub = approximateMaximumKCutMutateStub;
        this.k1ColoringMutateStub = k1ColoringMutateStub;
        this.kCoreMutateStub = kCoreMutateStub;
        this.kMeansMutateStub = kMeansMutateStub;
        this.labelPropagationMutateStub = labelPropagationMutateStub;
        this.leidenMutateStub = leidenMutateStub;
        this.louvainMutateStub = louvainMutateStub;
        this.modularityOptimizationMutateStub = modularityOptimizationMutateStub;
        this.sccMutateStub = sccMutateStub;
        this.lccMutateStub = lccMutateStub;
        this.triangleCountMutateStub = triangleCountMutateStub;
        this.wccMutateStub = wccMutateStub;
        this.applicationsFacade = applicationsFacade;
        this.estimationMode = estimationMode;
        this.algorithmExecutionScaffolding = algorithmExecutionScaffolding;
        this.algorithmExecutionScaffoldingForStreamMode = algorithmExecutionScaffoldingForStreamMode;
    }

    public static CommunityProcedureFacade create(
        GenericStub genericStub,
        ApplicationsFacade applicationsFacade,
        CloseableResourceRegistry closeableResourceRegistry,
        ProcedureReturnColumns procedureReturnColumns,
        EstimationModeRunner estimationModeRunner,
        AlgorithmExecutionScaffolding algorithmExecutionScaffolding,
        AlgorithmExecutionScaffolding algorithmExecutionScaffoldingForStreamMode
    ) {
        var approximateMaximumKCutMutateStub = new ApproximateMaximumKCutMutateStub(genericStub, applicationsFacade);
        var k1ColoringMutateStub = new K1ColoringMutateStub(genericStub, applicationsFacade, procedureReturnColumns);
        var kCoreMutateStub = new KCoreMutateStub(genericStub, applicationsFacade);
        var kMeansMutateStub = new KMeansMutateStub(genericStub, applicationsFacade, procedureReturnColumns);
        var labelPropagationMutateStub = new LabelPropagationMutateStub(
            genericStub,
            applicationsFacade,
            procedureReturnColumns
        );
        var lccMutateStub = new LccMutateStub(genericStub, applicationsFacade);
        var leidenMutateStub = new LeidenMutateStub(genericStub, applicationsFacade, procedureReturnColumns);
        var louvainMutateStub = new LouvainMutateStub(genericStub, applicationsFacade, procedureReturnColumns);
        var modularityOptimizationMutateStub = new ModularityOptimizationMutateStub(
            genericStub,
            applicationsFacade,
            procedureReturnColumns
        );
        var sccMutateStub = new SccMutateStub(genericStub, applicationsFacade, procedureReturnColumns);
        var triangleCountMutateStub = new TriangleCountMutateStub(
            genericStub,
            applicationsFacade
        );
        var wccMutateStub = new WccMutateStub(genericStub, applicationsFacade, procedureReturnColumns);

        return new CommunityProcedureFacade(
            closeableResourceRegistry,
            procedureReturnColumns,
            approximateMaximumKCutMutateStub,
            k1ColoringMutateStub,
            kCoreMutateStub,
            kMeansMutateStub,
            labelPropagationMutateStub,
            lccMutateStub,
            leidenMutateStub,
            louvainMutateStub,
            modularityOptimizationMutateStub,
            sccMutateStub,
            triangleCountMutateStub,
            wccMutateStub,
            applicationsFacade,
            estimationModeRunner,
            algorithmExecutionScaffolding,
            algorithmExecutionScaffoldingForStreamMode
        );
    }

    public ApproximateMaximumKCutMutateStub approximateMaximumKCutMutateStub() {
        return approximateMaximumKCutMutateStub;
    }

    public Stream<ApproxMaxKCutStreamResult> approxMaxKCutStream(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new ApproxMaxKCutResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            ApproxMaxKCutStreamConfig::of,
            streamMode()::approximateMaximumKCut,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> approxMaxKCutStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            ApproxMaxKCutStreamConfig::of,
            configuration -> estimationMode().approximateMaximumKCut(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<ConductanceStreamResult> conductanceStream(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new ConductanceResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            ConductanceStreamConfig::of,
            streamMode()::conductance,
            resultBuilder
        );
    }

    public K1ColoringMutateStub k1ColoringMutateStub() {
        return k1ColoringMutateStub;
    }

    public Stream<K1ColoringStatsResult> k1ColoringStats(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new K1ColoringResultBuilderForStatsMode(procedureReturnColumns.contains("colorCount"));

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            K1ColoringStatsConfig::of,
            statsMode()::k1Coloring,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> k1ColoringStatsEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            K1ColoringStatsConfig::of,
            configuration -> estimationMode().k1Coloring(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<K1ColoringStreamResult> k1ColoringStream(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new K1ColoringResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            K1ColoringStreamConfig::of,
            streamMode()::k1Coloring,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> k1ColoringStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            K1ColoringStreamConfig::of,
            configuration -> estimationMode().k1Coloring(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<K1ColoringWriteResult> k1ColoringWrite(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new K1ColoringResultBuilderForWriteMode(procedureReturnColumns.contains("colorCount"));

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            K1ColoringWriteConfig::of,
            writeMode()::k1Coloring,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> k1ColoringWriteEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            K1ColoringWriteConfig::of,
            configuration -> estimationMode().k1Coloring(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public KCoreMutateStub kCoreMutateStub() {
        return kCoreMutateStub;
    }

    public Stream<KCoreDecompositionStatsResult> kCoreStats(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new KCoreResultBuilderForStatsMode();

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            KCoreDecompositionStatsConfig::of,
            statsMode()::kCore,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> kCoreStatsEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            KCoreDecompositionStatsConfig::of,
            configuration -> estimationMode().kCore(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<KCoreDecompositionStreamResult> kCoreStream(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new KCoreResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            KCoreDecompositionStreamConfig::of,
            streamMode()::kCore,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> kCoreStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            KCoreDecompositionStreamConfig::of,
            configuration -> estimationMode().kCore(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<KCoreDecompositionWriteResult> kCoreWrite(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new KCoreResultBuilderForWriteMode();

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            KCoreDecompositionWriteConfig::of,
            writeMode()::kCore,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> kCoreWriteEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            KCoreDecompositionWriteConfig::of,
            configuration -> estimationMode().kCore(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public KMeansMutateStub kMeansMutateStub() {
        return kMeansMutateStub;
    }

    public Stream<KmeansStatsResult> kmeansStats(String graphName, Map<String, Object> configuration) {
        var statisticsComputationInstructions = ProcedureStatisticsComputationInstructions.forCommunities(
            procedureReturnColumns);
        var shouldComputeListOfCentroids = procedureReturnColumns.contains("centroids");
        var resultBuilder = new KMeansResultBuilderForStatsMode(
            statisticsComputationInstructions,
            shouldComputeListOfCentroids
        );

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            KmeansStatsConfig::of,
            statsMode()::kMeans,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> kmeansStatsEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            KmeansStatsConfig::of,
            configuration -> estimationMode().kMeans(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<KmeansStreamResult> kmeansStream(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new KMeansResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            KmeansStreamConfig::of,
            streamMode()::kMeans,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> kmeansStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            KmeansStreamConfig::of,
            configuration -> estimationMode().kMeans(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<KmeansWriteResult> kmeansWrite(String graphName, Map<String, Object> configuration) {
        var statisticsComputationInstructions = ProcedureStatisticsComputationInstructions.forCommunities(
            procedureReturnColumns);
        var shouldComputeListOfCentroids = procedureReturnColumns.contains("centroids");
        var resultBuilder = new KMeansResultBuilderForWriteMode(
            statisticsComputationInstructions,
            shouldComputeListOfCentroids
        );

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            KmeansWriteConfig::of,
            writeMode()::kMeans,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> kmeansWriteEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            KmeansWriteConfig::of,
            configuration -> estimationMode().kMeans(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public LabelPropagationMutateStub labelPropagationMutateStub() {
        return labelPropagationMutateStub;
    }

    public Stream<LabelPropagationStatsResult> labelPropagationStats(
        String graphName,
        Map<String, Object> configuration
    ) {
        var statisticsComputationInstructions = ProcedureStatisticsComputationInstructions.forCommunities(
            procedureReturnColumns);
        var resultBuilder = new LabelPropagationResultBuilderForStatsMode(statisticsComputationInstructions);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            LabelPropagationStatsConfig::of,
            statsMode()::labelPropagation,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> labelPropagationStatsEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            LabelPropagationStatsConfig::of,
            configuration -> estimationMode().labelPropagation(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<LabelPropagationStreamResult> labelPropagationStream(
        String graphName, Map<String, Object> configuration
    ) {
        var resultBuilder = new LabelPropagationResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            LabelPropagationStreamConfig::of,
            streamMode()::labelPropagation,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> labelPropagationStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            LabelPropagationStreamConfig::of,
            configuration -> estimationMode().labelPropagation(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<LabelPropagationWriteResult> labelPropagationWrite(
        String graphName,
        Map<String, Object> configuration
    ) {
        var statisticsComputationInstructions = ProcedureStatisticsComputationInstructions.forCommunities(
            procedureReturnColumns);
        var resultBuilder = new LabelPropagationResultBuilderForWriteMode(statisticsComputationInstructions);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            LabelPropagationWriteConfig::of,
            writeMode()::labelPropagation,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> labelPropagationWriteEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            LabelPropagationWriteConfig::of,
            configuration -> estimationMode().labelPropagation(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public LccMutateStub lccMutateStub() {
        return lccMutateStub;
    }

    public Stream<LocalClusteringCoefficientStatsResult> localClusteringCoefficientStats(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new LccResultBuilderForStatsMode();

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            LocalClusteringCoefficientStatsConfig::of,
            statsMode()::lcc,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> localClusteringCoefficientStatsEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            LocalClusteringCoefficientStatsConfig::of,
            configuration -> estimationMode().lcc(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<LocalClusteringCoefficientStreamResult> localClusteringCoefficientStream(
        String graphName, Map<String, Object> configuration
    ) {
        var resultBuilder = new LccResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            LocalClusteringCoefficientStreamConfig::of,
            streamMode()::lcc,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> localClusteringCoefficientStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            LocalClusteringCoefficientStreamConfig::of,
            configuration -> estimationMode().lcc(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<LocalClusteringCoefficientWriteResult> localClusteringCoefficientWrite(
        String graphName, Map<String, Object> configuration
    ) {
        var resultBuilder = new LccResultBuilderForWriteMode();

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            LocalClusteringCoefficientWriteConfig::of,
            writeMode()::lcc,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> localClusteringCoefficientWriteEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            LocalClusteringCoefficientWriteConfig::of,
            configuration -> estimationMode().lcc(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public LeidenMutateStub leidenMutateStub() {
        return leidenMutateStub;
    }

    public Stream<LeidenStatsResult> leidenStats(String graphName, Map<String, Object> configuration) {
        var statisticsComputationInstructions = ProcedureStatisticsComputationInstructions.forCommunities(
            procedureReturnColumns);
        var resultBuilder = new LeidenResultBuilderForStatsMode(statisticsComputationInstructions);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            LeidenStatsConfig::of,
            statsMode()::leiden,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> leidenStatsEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            LeidenStatsConfig::of,
            configuration -> estimationMode().leiden(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<LeidenStreamResult> leidenStream(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new LeidenResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            LeidenStreamConfig::of,
            streamMode()::leiden,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> leidenStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            LeidenStreamConfig::of,
            configuration -> estimationMode().leiden(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<LeidenWriteResult> leidenWrite(
        String graphName,
        Map<String, Object> configuration
    ) {
        var statisticsComputationInstructions = ProcedureStatisticsComputationInstructions.forCommunities(
            procedureReturnColumns);
        var resultBuilder = new LeidenResultBuilderForWriteMode(statisticsComputationInstructions);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            LeidenWriteConfig::of,
            writeMode()::leiden,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> leidenWriteEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            LeidenWriteConfig::of,
            configuration -> estimationMode().leiden(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public LouvainMutateStub louvainMutateStub() {
        return louvainMutateStub;
    }

    public Stream<LouvainStatsResult> louvainStats(String graphName, Map<String, Object> configuration) {
        var statisticsComputationInstructions = ProcedureStatisticsComputationInstructions.forCommunities(
            procedureReturnColumns);
        var resultBuilder = new LouvainResultBuilderForStatsMode(statisticsComputationInstructions);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            LouvainStatsConfig::of,
            statsMode()::louvain,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> louvainStatsEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            LouvainStatsConfig::of,
            configuration -> estimationMode().louvain(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<LouvainStreamResult> louvainStream(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new LouvainResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            LouvainStreamConfig::of,
            streamMode()::louvain,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> louvainStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            LouvainStreamConfig::of,
            configuration -> estimationMode().louvain(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<LouvainWriteResult> louvainWrite(
        String graphName,
        Map<String, Object> configuration
    ) {
        var statisticsComputationInstructions = ProcedureStatisticsComputationInstructions.forCommunities(
            procedureReturnColumns);
        var resultBuilder = new LouvainResultBuilderForWriteMode(statisticsComputationInstructions);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            LouvainWriteConfig::of,
            writeMode()::louvain,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> louvainWriteEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            LouvainWriteConfig::of,
            configuration -> estimationMode().louvain(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<ModularityStatsResult> modularityStats(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new ModularityResultBuilderForStatsMode();

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            ModularityStatsConfig::of,
            statsMode()::modularity,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> modularityStatsEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            ModularityStatsConfig::of,
            configuration -> estimationMode().modularity(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<ModularityStreamResult> modularityStream(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new ModularityResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            ModularityStreamConfig::of,
            streamMode()::modularity,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> modularityStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            ModularityStreamConfig::of,
            configuration -> estimationMode().modularity(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public ModularityOptimizationMutateStub modularityOptimizationMutateStub() {
        return modularityOptimizationMutateStub;
    }

    public Stream<ModularityOptimizationStatsResult> modularityOptimizationStats(
        String graphName,
        Map<String, Object> configuration
    ) {
        StatisticsComputationInstructions statisticsComputationInstructions = ProcedureStatisticsComputationInstructions.forCommunities(
            procedureReturnColumns);
        var resultBuilder = new ModularityOptimizationResultBuilderForStatsMode(statisticsComputationInstructions);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            ModularityOptimizationStatsConfig::of,
            statsMode()::modularityOptimization,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> modularityOptimizationStatsEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            ModularityOptimizationStatsConfig::of,
            configuration -> estimationMode().modularityOptimization(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<ModularityOptimizationStreamResult> modularityOptimizationStream(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new ModularityOptimizationResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            ModularityOptimizationStreamConfig::of,
            streamMode()::modularityOptimization,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> modularityOptimizationStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            ModularityOptimizationStreamConfig::of,
            configuration -> estimationMode().modularityOptimization(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<ModularityOptimizationWriteResult> modularityOptimizationWrite(
        String graphName, Map<String, Object> configuration
    ) {
        var statisticsComputationInstructions = ProcedureStatisticsComputationInstructions.forCommunities(
            procedureReturnColumns);
        var resultBuilder = new ModularityOptimizationResultBuilderForWriteMode(statisticsComputationInstructions);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            ModularityOptimizationWriteConfig::of,
            writeMode()::modularityOptimization,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> modularityOptimizationWriteEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            ModularityOptimizationWriteConfig::of,
            configuration -> estimationMode().modularityOptimization(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public SccMutateStub sccMutateStub() {
        return sccMutateStub;
    }

    public Stream<SccStatsResult> sccStats(
        String graphName,
        Map<String, Object> configuration
    ) {
        var statisticsComputationInstructions = ProcedureStatisticsComputationInstructions.forComponents(
            procedureReturnColumns);
        var resultBuilder = new SccResultBuilderForStatsMode(statisticsComputationInstructions);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            SccStatsConfig::of,
            statsMode()::scc,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> sccStatsEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            SccStatsConfig::of,
            configuration -> estimationMode().scc(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<SccStreamResult> sccStream(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new SccResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            SccStreamConfig::of,
            streamMode()::scc,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> sccStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            SccStreamConfig::of,
            configuration -> estimationMode().scc(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<SccWriteResult> sccWrite(
        String graphName,
        Map<String, Object> configuration
    ) {
        var statisticsComputationInstructions = ProcedureStatisticsComputationInstructions.forComponents(
            procedureReturnColumns);
        var resultBuilder = new SccResultBuilderForWriteMode(statisticsComputationInstructions);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            SccWriteConfig::of,
            writeMode()::scc,
            resultBuilder
        );
    }

    public Stream<AlphaSccWriteResult> sccWriteAlpha(
        String graphName,
        Map<String, Object> configuration
    ) {
        var statisticsComputationInstructions = new ProcedureStatisticsComputationInstructions(true, true);
        var resultBuilder = new SccAlphaResultBuilderForWriteMode(statisticsComputationInstructions);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            SccAlphaWriteConfig::of,
            writeMode()::sccAlpha,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> sccWriteEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            SccWriteConfig::of,
            configuration -> estimationMode().scc(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public TriangleCountMutateStub triangleCountMutateStub() {
        return triangleCountMutateStub;
    }

    public Stream<TriangleCountStatsResult> triangleCountStats(String graphName, Map<String, Object> configuration) {
        var resultBuilder = new TriangleCountResultBuilderForStatsMode();

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            TriangleCountStatsConfig::of,
            statsMode()::triangleCount,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> triangleCountStatsEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            TriangleCountStatsConfig::of,
            configuration -> estimationMode().triangleCount(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<TriangleCountStreamResult> triangleCountStream(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new TriangleCountResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            TriangleCountStreamConfig::of,
            streamMode()::triangleCount,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> triangleCountStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            TriangleCountStreamConfig::of,
            configuration -> estimationMode().triangleCount(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<TriangleCountWriteResult> triangleCountWrite(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new TriangleCountResultBuilderForWriteMode();

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            TriangleCountWriteConfig::of,
            writeMode()::triangleCount,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> triangleCountWriteEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            TriangleCountWriteConfig::of,
            configuration -> estimationMode().triangleCount(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<TriangleStreamResult> trianglesStream(String graphName, Map<String, Object> configuration) {
        var resultBuilder = new TrianglesResultBuilderForStreamMode(closeableResourceRegistry);

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            TriangleCountBaseConfig::of,
            streamMode()::triangles,
            resultBuilder
        );
    }

    public WccMutateStub wccMutateStub() {
        return wccMutateStub;
    }

    public Stream<WccStatsResult> wccStats(String graphName, Map<String, Object> configuration) {
        var statisticsComputationInstructions = ProcedureStatisticsComputationInstructions.forComponents(
            procedureReturnColumns);
        var resultBuilder = new WccResultBuilderForStatsMode(statisticsComputationInstructions);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            WccStatsConfig::of,
            statsMode()::wcc,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> wccStatsEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            WccStatsConfig::of,
            configuration -> estimationMode().wcc(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<WccStreamResult> wccStream(String graphName, Map<String, Object> configuration) {
        var resultBuilder = new WccResultBuilderForStreamMode();

        return algorithmExecutionScaffoldingForStreamMode.runAlgorithm(
            graphName,
            configuration,
            WccStreamConfig::of,
            streamMode()::wcc,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> wccStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            WccStreamConfig::of,
            configuration -> estimationMode().wcc(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    public Stream<WccWriteResult> wccWrite(String graphName, Map<String, Object> configuration) {
        var statisticsComputationInstructions = ProcedureStatisticsComputationInstructions.forComponents(
            procedureReturnColumns);
        var resultBuilder = new WccResultBuilderForWriteMode(statisticsComputationInstructions);

        return algorithmExecutionScaffolding.runAlgorithm(
            graphName,
            configuration,
            WccWriteConfig::of,
            writeMode()::wcc,
            resultBuilder
        );
    }

    public Stream<MemoryEstimateResult> wccWriteEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationMode.runEstimation(
            algorithmConfiguration,
            WccWriteConfig::of,
            configuration -> estimationMode().wcc(configuration, graphNameOrConfiguration)
        );

        return Stream.of(result);
    }

    private CommunityAlgorithmsEstimationModeBusinessFacade estimationMode() {
        return applicationsFacade.community().estimate();
    }

    private CommunityAlgorithmsStatsModeBusinessFacade statsMode() {
        return applicationsFacade.community().stats();
    }

    private CommunityAlgorithmsStreamModeBusinessFacade streamMode() {
        return applicationsFacade.community().stream();
    }

    private CommunityAlgorithmsWriteModeBusinessFacade writeMode() {
        return applicationsFacade.community().write();
    }
}
