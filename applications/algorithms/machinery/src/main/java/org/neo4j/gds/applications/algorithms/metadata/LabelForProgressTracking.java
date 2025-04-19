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
package org.neo4j.gds.applications.algorithms.metadata;

public enum LabelForProgressTracking {
    AllShortestPaths("All Shortest Paths"),
    ApproximateMaximumKCut("ApproxMaxKCut"),
    ArticleRank("ArticleRank"),
    ArticulationPoints("Articulation Points"),
    AStar("AStar"),
    BellmanFord("Bellman-Ford"),
    BetaClosenessCentrality("Closeness Centrality (beta)"),
    BetweennessCentrality("Betweenness Centrality"),
    BFS("BFS"),
    BRIDGES("Bridges"),
    CELF("CELF"),
    ClosenessCentrality("Closeness Centrality"),
    CollapsePath("CollapsePath"),
    Conductance("Conductance"),
    DegreeCentrality("DegreeCentrality"),
    DeltaStepping("Delta Stepping"),
    DFS("DFS"),
    Dijkstra("Dijkstra"),
    EigenVector("EigenVector"),
    FastRP("FastRP"),
    FilteredKNN("Filtered K-Nearest Neighbours"),
    FilteredNodeSimilarity("Filtered Node Similarity"),
    GraphSage("GraphSage"),
    GraphSageTrain("GraphSageTrain"),
    HarmonicCentrality("HarmonicCentrality"),
    HashGNN("HashGNN"),
    K1Coloring("K1Coloring"),
    KCore("KCoreDecomposition"),
    KMeans("K-Means"),
    KNN("K-Nearest Neighbours"),
    KSpanningTree("K Spanning Tree"),
    LabelPropagation("Label Propagation"),
    LCC("LocalClusteringCoefficient"),
    Leiden("Leiden"),
    Louvain("Louvain"),
    LongestPath("LongestPath"),
    Modularity("Modularity"),
    ModularityOptimization("ModularityOptimization"),
    NodeSimilarity("Node Similarity"),
    Node2Vec("Node2Vec"),
    PageRank("PageRank"),
    RandomWalk("RandomWalk"),
    ScaleProperties("ScaleProperties"),
    SCC("SCC"),
    SingleSourceDijkstra("All Shortest Paths"),
    SpanningTree("SpanningTree"),
    SteinerTree("SteinerTree"),
    TopologicalSort("TopologicalSort"),
    TriangleCount("TriangleCount"),
    Triangles("Triangles"),
    WCC("WCC"),
    Yens("Yens");

    public final String value;

    LabelForProgressTracking(String value) {this.value = value;}

    public static LabelForProgressTracking from(Algorithm algorithm) {
        return switch (algorithm) {
            case AllShortestPaths -> AllShortestPaths;
            case ApproximateMaximumKCut -> ApproximateMaximumKCut;
            case ArticleRank -> ArticleRank;
            case ArticulationPoints -> ArticulationPoints;
            case AStar -> AStar;
            case BellmanFord -> BellmanFord;
            case BetaClosenessCentrality -> BetaClosenessCentrality;
            case BetweennessCentrality -> BetweennessCentrality;
            case BFS -> BFS;
            case CELF -> CELF;
            case ClosenessCentrality -> ClosenessCentrality;
            case CollapsePath -> CollapsePath;
            case Conductance -> Conductance;
            case DegreeCentrality -> DegreeCentrality;
            case DeltaStepping -> DeltaStepping;
            case DFS -> DFS;
            case Dijkstra -> Dijkstra;
            case EigenVector -> EigenVector;
            case FastRP -> FastRP;
            case FilteredKNN -> FilteredKNN;
            case FilteredNodeSimilarity -> FilteredNodeSimilarity;
            case GraphSage -> GraphSage;
            case GraphSageTrain -> GraphSageTrain;
            case HarmonicCentrality -> HarmonicCentrality;
            case HashGNN -> HashGNN;
            case K1Coloring -> K1Coloring;
            case KCore -> KCore;
            case KMeans -> KMeans;
            case KNN -> KNN;
            case KSpanningTree -> KSpanningTree;
            case LabelPropagation -> LabelPropagation;
            case LCC -> LCC;
            case Leiden -> Leiden;
            case Louvain -> Louvain;
            case LongestPath -> LongestPath;
            case Modularity -> Modularity;
            case ModularityOptimization -> ModularityOptimization;
            case NodeSimilarity -> NodeSimilarity;
            case Node2Vec -> Node2Vec;
            case PageRank -> PageRank;
            case RandomWalk -> RandomWalk;
            case ScaleProperties -> ScaleProperties;
            case SCC -> SCC;
            case SingleSourceDijkstra -> SingleSourceDijkstra;
            case SpanningTree -> SpanningTree;
            case SteinerTree -> SteinerTree;
            case TopologicalSort -> TopologicalSort;
            case TriangleCount -> TriangleCount;
            case Triangles -> Triangles;
            case WCC -> WCC;
            case Yens -> Yens;
        };
    }

    @Override
    public String toString() {
        return value;
    }
}
