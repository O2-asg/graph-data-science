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
package org.neo4j.gds.ml.kge;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.Inject;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.neo4j.gds.ml.kge.ScoreFunction.TRANSE;

@GdlExtension
class KGEPredictMutateConfigTest {

    @GdlGraph(orientation = Orientation.NATURAL)
    static final String GDL_GRAPH = "(:A {embedding: [0.1, 0.9]})" +
        "-[:REL]->" +
        "(:B {embedding: [0.1, 0.5]})" +
        "-[:CONTEXT]->" +
        "(:C)";

    @Inject
    private GraphStore graphStore;

    @Test
    void scoringFunctionMustMatch() {
        var wrongConfig = KGEPredictMutateConfigImpl.builder()
            .nodeEmbeddingProperty("embedding")
            .relationshipTypeEmbedding(List.of(0.3, 0.3))
            .scoringFunction("ComplexE")
            .mutateRelationshipType("PREDICTED_REL")
            .topK(3);

        assertThatThrownBy(wrongConfig::build)
            .hasMessage("Score function `COMPLEXE` is not supported. Must be one of: [TRANSE, DISTMULT].");

        var config = KGEPredictMutateConfigImpl.builder()
            .nodeEmbeddingProperty("embedding")
            .relationshipTypeEmbedding(List.of(0.3, 0.3))
            .scoringFunction("traNsE")
            .mutateRelationshipType("PREDICTED_REL")
            .topK(3);

        assertThatNoException().isThrownBy(config::build);
        assertThat(config.build().scoringFunction()).isEqualTo(TRANSE);
    }


}
