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
package org.neo4j.gds.similarity.filterednodesim;

import org.neo4j.gds.applications.algorithms.machinery.MemoryEstimateResult;
import org.neo4j.gds.procedures.GraphDataScienceProcedures;
import org.neo4j.gds.similarity.SimilarityResult;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Internal;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.gds.procedures.ProcedureConstants.MEMORY_ESTIMATION_DESCRIPTION;
import static org.neo4j.gds.similarity.filterednodesim.Constants.FILTERED_NODE_SIMILARITY_DESCRIPTION;
import static org.neo4j.procedure.Mode.READ;

public class FilteredNodeSimilarityStreamProc {
    @Context
    public GraphDataScienceProcedures facade;

    @Procedure(value = "gds.nodeSimilarity.filtered.stream", mode = READ)
    @Description(FILTERED_NODE_SIMILARITY_DESCRIPTION)
    public Stream<SimilarityResult> stream(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return facade.algorithms().similarity().filteredNodeSimilarityStream(graphName, configuration);
    }

    @Procedure(value = "gds.nodeSimilarity.filtered.stream.estimate", mode = READ)
    @Description(MEMORY_ESTIMATION_DESCRIPTION)
    public Stream<MemoryEstimateResult> estimate(
        @Name(value = "graphNameOrConfiguration") Object graphNameOrConfiguration,
        @Name(value = "algoConfiguration") Map<String, Object> algoConfiguration
    ) {
        return facade.algorithms().similarity().filteredNodeSimilarityStreamEstimate(graphNameOrConfiguration, algoConfiguration);
    }

    @Deprecated(forRemoval = true)
    @Internal
    @Procedure(value = "gds.alpha.nodeSimilarity.filtered.stream", mode = READ, deprecatedBy = "gds.nodeSimilarity.filtered.stream")
    @Description(FILTERED_NODE_SIMILARITY_DESCRIPTION)
    public Stream<SimilarityResult> streamAlpha(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        facade.deprecatedProcedures().called("gds.alpha.nodeSimilarity.filtered.stream");
        facade
            .log()
            .warn("Procedure `gds.alpha.nodeSimilarity.filtered.stream` has been deprecated, please use `gds.nodeSimilarity.filtered.stream`.");

        return stream(graphName, configuration);
    }

    @Deprecated(forRemoval = true)
    @Internal
    @Procedure(value = "gds.alpha.nodeSimilarity.filtered.stream.estimate", mode = READ, deprecatedBy = "gds.nodeSimilarity.filtered.stream.estimate")
    @Description(MEMORY_ESTIMATION_DESCRIPTION)
    public Stream<MemoryEstimateResult> estimateAlpha(
        @Name(value = "graphNameOrConfiguration") Object graphNameOrConfiguration,
        @Name(value = "algoConfiguration") Map<String, Object> algoConfiguration
    ) {
        facade.deprecatedProcedures().called("gds.alpha.nodeSimilarity.filtered.stream.estimate");
        facade
            .log()
            .warn("Procedure `gds.alpha.nodeSimilarity.filtered.stream.estimate` has been deprecated, please use `gds.nodeSimilarity.filtered.stream.estimate`.");

        return estimate(graphNameOrConfiguration, algoConfiguration);
    }
}
