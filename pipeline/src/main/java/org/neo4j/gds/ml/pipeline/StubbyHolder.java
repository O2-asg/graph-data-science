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
package org.neo4j.gds.ml.pipeline;

import org.neo4j.gds.applications.algorithms.metadata.Algorithm;
import org.neo4j.gds.ml.pipeline.stubs.ApproximateMaximumKCutStub;
import org.neo4j.gds.ml.pipeline.stubs.ArticleRankStub;
import org.neo4j.gds.ml.pipeline.stubs.ArticulationPointsStub;
import org.neo4j.gds.ml.pipeline.stubs.BellmanFordStub;
import org.neo4j.gds.ml.pipeline.stubs.BetaClosenessCentralityStub;
import org.neo4j.gds.ml.pipeline.stubs.BetweennessCentralityStub;
import org.neo4j.gds.ml.pipeline.stubs.BreadthFirstSearchStub;
import org.neo4j.gds.ml.pipeline.stubs.CelfStub;
import org.neo4j.gds.ml.pipeline.stubs.ClosenessCentralityStub;
import org.neo4j.gds.ml.pipeline.stubs.CollapsePathStub;
import org.neo4j.gds.ml.pipeline.stubs.DegreeCentralityStub;
import org.neo4j.gds.ml.pipeline.stubs.DepthFirstSearchStub;
import org.neo4j.gds.ml.pipeline.stubs.EigenVectorStub;
import org.neo4j.gds.ml.pipeline.stubs.FastRPStub;
import org.neo4j.gds.ml.pipeline.stubs.FilteredKnnStub;
import org.neo4j.gds.ml.pipeline.stubs.FilteredNodeSimilarityStub;
import org.neo4j.gds.ml.pipeline.stubs.GraphSageStub;
import org.neo4j.gds.ml.pipeline.stubs.HarmonicCentralityStub;
import org.neo4j.gds.ml.pipeline.stubs.HashGnnStub;
import org.neo4j.gds.ml.pipeline.stubs.K1ColoringStub;
import org.neo4j.gds.ml.pipeline.stubs.KCoreStub;
import org.neo4j.gds.ml.pipeline.stubs.KMeansStub;
import org.neo4j.gds.ml.pipeline.stubs.KnnStub;
import org.neo4j.gds.ml.pipeline.stubs.LabelPropagationStub;
import org.neo4j.gds.ml.pipeline.stubs.LccStub;
import org.neo4j.gds.ml.pipeline.stubs.LeidenStub;
import org.neo4j.gds.ml.pipeline.stubs.LouvainStub;
import org.neo4j.gds.ml.pipeline.stubs.ModularityOptimizationStub;
import org.neo4j.gds.ml.pipeline.stubs.Node2VecStub;
import org.neo4j.gds.ml.pipeline.stubs.NodeSimilarityStub;
import org.neo4j.gds.ml.pipeline.stubs.PageRankStub;
import org.neo4j.gds.ml.pipeline.stubs.ScalePropertiesStub;
import org.neo4j.gds.ml.pipeline.stubs.SccStub;
import org.neo4j.gds.ml.pipeline.stubs.SinglePairShortestPathAStarStub;
import org.neo4j.gds.ml.pipeline.stubs.SinglePairShortestPathDijkstraStub;
import org.neo4j.gds.ml.pipeline.stubs.SinglePairShortestPathYensStub;
import org.neo4j.gds.ml.pipeline.stubs.SingleSourceShortestPathDeltaStub;
import org.neo4j.gds.ml.pipeline.stubs.SingleSourceShortestPathDijkstraStub;
import org.neo4j.gds.ml.pipeline.stubs.SpanningTreeStub;
import org.neo4j.gds.ml.pipeline.stubs.SteinerTreeStub;
import org.neo4j.gds.ml.pipeline.stubs.TriangleCountStub;
import org.neo4j.gds.ml.pipeline.stubs.WccStub;

/**
 * :flag-au:
 * The mapping of procedure name -> algorithm identifier -> stub
 * NB: only for algorithms that have a mutate mode - otherwise they could not form part of a pipeline
 */
class StubbyHolder {
    /**
     * @return a handy stub, or null if the algorithm does not have a mutate mode
     */
    Stub get(Algorithm algorithm) {
        return switch (algorithm) {
            case AllShortestPaths -> null;
            case ApproximateMaximumKCut -> new ApproximateMaximumKCutStub();
            case ArticleRank -> new ArticleRankStub();
            case ArticulationPoints -> new ArticulationPointsStub();
            case AStar -> new SinglePairShortestPathAStarStub();
            case BellmanFord -> new BellmanFordStub();
            case BetaClosenessCentrality -> new BetaClosenessCentralityStub();
            case BetweennessCentrality -> new BetweennessCentralityStub();
            case BFS -> new BreadthFirstSearchStub();
            case CELF -> new CelfStub();
            case ClosenessCentrality -> new ClosenessCentralityStub();
            case CollapsePath -> new CollapsePathStub();
            case Conductance -> null;
            case DegreeCentrality -> new DegreeCentralityStub();
            case DeltaStepping -> new SingleSourceShortestPathDeltaStub();
            case DFS -> new DepthFirstSearchStub();
            case Dijkstra -> new SinglePairShortestPathDijkstraStub();
            case EigenVector -> new EigenVectorStub();
            case FastRP -> new FastRPStub();
            case FilteredKNN -> new FilteredKnnStub();
            case FilteredNodeSimilarity -> new FilteredNodeSimilarityStub();
            case GraphSage -> new GraphSageStub();
            case GraphSageTrain -> null;
            case HarmonicCentrality -> new HarmonicCentralityStub();
            case HashGNN -> new HashGnnStub();
            case K1Coloring -> new K1ColoringStub();
            case KCore -> new KCoreStub();
            case KMeans -> new KMeansStub();
            case KNN -> new KnnStub();
            case KSpanningTree -> null;
            case LabelPropagation -> new LabelPropagationStub();
            case LCC -> new LccStub();
            case Leiden -> new LeidenStub();
            case Louvain -> new LouvainStub();
            case LongestPath -> null;
            case Modularity -> null;
            case ModularityOptimization -> new ModularityOptimizationStub();
            case NodeSimilarity -> new NodeSimilarityStub();
            case Node2Vec -> new Node2VecStub();
            case PageRank -> new PageRankStub();
            case RandomWalk -> null;
            case ScaleProperties -> new ScalePropertiesStub();
            case SCC -> new SccStub();
            case SingleSourceDijkstra -> new SingleSourceShortestPathDijkstraStub();
            case SpanningTree -> new SpanningTreeStub();
            case SteinerTree -> new SteinerTreeStub();
            case TopologicalSort -> null;
            case TriangleCount -> new TriangleCountStub();
            case Triangles -> null;
            case WCC -> new WccStub();
            case Yens -> new SinglePairShortestPathYensStub();
        };
    }
}
