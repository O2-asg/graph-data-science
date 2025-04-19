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
package org.neo4j.gds.wcc;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.gds.BaseProcTest;
import org.neo4j.gds.GdsCypher;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.gds.catalog.GraphWriteNodePropertiesProc;
import org.neo4j.gds.core.loading.GraphStoreCatalog;
import org.neo4j.gds.extension.Neo4jGraph;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LONG;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

class WccWriteProcTest extends BaseProcTest {
    @Neo4jGraph
    static final @Language("Cypher") String DB_CYPHER =
        "CREATE" +
        " (nA:Label {nodeId: 0, seedId: 42})" +
        ",(nB:Label {nodeId: 1, seedId: 42})" +
        ",(nC:Label {nodeId: 2, seedId: 42})" +
        ",(nD:Label {nodeId: 3, seedId: 42})" +
        ",(nE:Label2 {nodeId: 4})" +
        ",(nF:Label2 {nodeId: 5})" +
        ",(nG:Label2 {nodeId: 6})" +
        ",(nH:Label2 {nodeId: 7})" +
        ",(nI:Label2 {nodeId: 8})" +
        ",(nJ:Label2 {nodeId: 9})" +
        // {A, B, C, D}
        ",(nA)-[:TYPE]->(nB)" +
        ",(nB)-[:TYPE]->(nC)" +
        ",(nC)-[:TYPE]->(nD)" +
        ",(nD)-[:TYPE {cost:4.2}]->(nE)" + // threshold UF should split here
        // {E, F, G}
        ",(nE)-[:TYPE]->(nF)" +
        ",(nF)-[:TYPE]->(nG)" +
        // {H, I}
        ",(nH)-[:TYPE]->(nI)";
    private static final String WRITE_PROPERTY = "componentId";
    private static final String SEED_PROPERTY = "seedId";

    @BeforeEach
    void setupGraph() throws Exception {
        registerProcedures(
            WccWriteProc.class,
            GraphProjectProc.class,
            GraphWriteNodePropertiesProc.class
        );
    }

    @AfterEach
    void removeAllLoadedGraphs() {
        GraphStoreCatalog.removeAllLoadedGraphs();
    }

    @Test
    void testWriteYields() {
        var projectQuery = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .graphProject()
            .loadEverything(Orientation.NATURAL)
            .yields();
        runQuery(projectQuery);
        String query = GdsCypher
            .call(DEFAULT_GRAPH_NAME)
            .algo("wcc")
            .writeMode()
            .addParameter("writeProperty", WRITE_PROPERTY)
            .yields(
                "nodePropertiesWritten",
                "preProcessingMillis",
                "computeMillis",
                "writeMillis",
                "postProcessingMillis",
                "componentCount",
                "componentDistribution",
                "configuration"
            );

        runQueryWithRowConsumer(
            query,
            row -> {
                assertUserInput(row, "writeProperty", WRITE_PROPERTY);
                assertUserInput(row, "seedProperty", null);
                assertUserInput(row, "threshold", 0D);
                assertUserInput(row, "consecutiveIds", false);

                assertThat(row.getNumber("nodePropertiesWritten"))
                    .asInstanceOf(LONG)
                    .isEqualTo(10L);

                assertThat(row.getNumber("preProcessingMillis"))
                    .asInstanceOf(LONG)
                    .isGreaterThan(-1L);

                assertThat(row.getNumber("computeMillis"))
                    .asInstanceOf(LONG)
                    .isGreaterThan(-1L);

                assertThat(row.getNumber("postProcessingMillis"))
                    .asInstanceOf(LONG)
                    .isGreaterThan(-1L);

                assertThat(row.getNumber("writeMillis"))
                    .asInstanceOf(LONG)
                    .isGreaterThan(-1L);

                assertThat(row.getNumber("componentCount"))
                    .asInstanceOf(LONG)
                    .as("wrong component count")
                    .isEqualTo(3L);

                assertThat(row.get("componentDistribution"))
                    .isInstanceOf(Map.class)
                    .asInstanceOf(MAP)
                    .containsKeys("p1", "p5", "p10", "p25")
                    .containsAllEntriesOf(
                        Map.of(
                            "p99", 7L,
                            "min", 1L,
                            "max", 7L,
                            "mean", 3.3333333333333335D,
                            "p999", 7L,
                            "p95", 7L,
                            "p90", 7L,
                            "p75", 7L,
                            "p50", 2L
                        )
                    );
            }
        );
    }

    @Test
    void testWrite() {
        var projectQuery = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .graphProject()
            .loadEverything(Orientation.NATURAL)
            .yields();
        runQuery(projectQuery);
        String query = GdsCypher
            .call(DEFAULT_GRAPH_NAME)
            .algo("wcc")
            .writeMode()
            .addParameter("writeProperty", WRITE_PROPERTY)
            .yields("componentCount");

        runQueryWithRowConsumer(query, row -> {
            assertThat(row.getNumber("componentCount"))
                .asInstanceOf(LONG)
                .isEqualTo(3L);
        });
    }

    @Test
    void testWriteWithNodeLabelFilter() {
        clearDb();
        runQuery("CREATE (nX:Ignore {nodeId: 42}) " + DB_CYPHER + " CREATE (nX)-[:X]->(nA), (nA)-[:X]->(nX), (nX)-[:X]->(nE), (nE)-[:X]->(nX)");

        String graphCreateQuery = GdsCypher
            .call("nodeFilterGraph")
            .graphProject()
            .withNodeLabels("Label", "Label2", "Ignore")
            .withAnyRelationshipType()
            .yields("nodeCount", "relationshipCount");

        runQueryWithRowConsumer(graphCreateQuery, row -> {
            assertThat(row.getNumber("nodeCount"))
                .asInstanceOf(LONG)
                .isEqualTo(11L);
            assertThat(row.getNumber("relationshipCount"))
                .asInstanceOf(LONG)
                .isEqualTo(11L);
        });

        String query = GdsCypher
            .call("nodeFilterGraph")
            .algo("wcc")
            .writeMode()
            .addParameter("writeProperty", WRITE_PROPERTY)
            .addParameter("nodeLabels", Arrays.asList("Label", "Label2"))
            .yields("componentCount");

        runQueryWithRowConsumer(query, row -> {
            assertThat(row.getNumber("componentCount"))
                .asInstanceOf(LONG)
                .isEqualTo(3L);
        });
    }

    @Test
    void testWriteWithLabel() {
        var createQuery = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .graphProject()
            .withNodeLabel("Label")
            .withAnyRelationshipType()
            .yields();
        runQuery(createQuery);

        String query = GdsCypher
            .call(DEFAULT_GRAPH_NAME)
            .algo("wcc")
            .writeMode()
            .addParameter("writeProperty", WRITE_PROPERTY)
            .yields("componentCount");

        runQueryWithRowConsumer(query, row -> {
            assertThat(row.getNumber("componentCount"))
                .asInstanceOf(LONG)
                .isEqualTo(1L);
        });
    }

    @Test
    void testWriteWithSeed() {
        var createQuery = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .graphProject()
            .withAnyLabel()
            .withNodeProperty("seedId")
            .withAnyRelationshipType()
            .yields();
        runQuery(createQuery);

        String query = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .algo("wcc")
            .writeMode()
            .addParameter("writeProperty", WRITE_PROPERTY)
            .addParameter("seedProperty", SEED_PROPERTY)
            .yields("componentCount");

        assertForSeedTests(query, WRITE_PROPERTY);
    }

    @Test
    void testWriteWithSeedAndSameWriteProperty() {
        var createQuery = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .graphProject()
            .withAnyLabel()
            .withNodeProperty("seedId")
            .withAnyRelationshipType()
            .yields();
        runQuery(createQuery);

        String query = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .algo("wcc")
            .writeMode()
            .addParameter("writeProperty", SEED_PROPERTY)
            .addParameter("seedProperty", SEED_PROPERTY)
            .yields("componentCount");

        assertForSeedTests(query, SEED_PROPERTY);
    }

    @Test
    void testWriteWithSeedOnExplicitGraph() {
        String graphName = "seedGraph";
        String loadQuery = "CALL gds.graph.project(" +
                           "   $graphName, " +
                           "   '*', '*', {nodeProperties: ['seedId']}  " +
                           ")";
        runQuery(loadQuery, Map.of("graphName", graphName));

        String query = GdsCypher
            .call(graphName)
            .algo("wcc")
            .writeMode()
            .addParameter("writeProperty", WRITE_PROPERTY)
            .addParameter("seedProperty", SEED_PROPERTY)
            .yields("componentCount");

        assertForSeedTests(query, WRITE_PROPERTY);
    }

    static Stream<Arguments> componentSizeInputs() {
        return Stream.of(
            Arguments.of(Map.of("minComponentSize", 1), List.of(0L, 7L, 9L)),
            Arguments.of(Map.of("minComponentSize", 2), List.of(0L, 7L)),
            Arguments.of(Map.of("minComponentSize", 1, "consecutiveIds", true), List.of(0L, 1L, 2L)),
            Arguments.of(Map.of("minComponentSize", 2, "consecutiveIds", true), List.of(0L, 1L)),
            Arguments.of(Map.of("minComponentSize", 1, "seedProperty", SEED_PROPERTY), List.of(42L, 46L, 48L)),
            Arguments.of(Map.of("minComponentSize", 2, "seedProperty", SEED_PROPERTY), List.of(42L, 46L))
        );
    }

    @ParameterizedTest
    @MethodSource("componentSizeInputs")
    void testWriteWithMinComponentSize(Map<String, Object> parameters, List<Long> expectedComponentIds) {
        var createQuery = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .graphProject()
            .withAnyLabel()
            .withAnyRelationshipType()
            .withNodeProperty(SEED_PROPERTY)
            .yields();
        runQuery(createQuery);
        var query = GdsCypher
            .call(DEFAULT_GRAPH_NAME)
            .algo("wcc")
            .writeMode()
            .addParameter("writeProperty", WRITE_PROPERTY)
            .addAllParameters(parameters)
            .yields("componentCount");

        runQueryWithRowConsumer(query, row -> {
            assertThat(row.getNumber("componentCount"))
                .asInstanceOf(LONG)
                .isEqualTo(3L);
        });

        runQueryWithRowConsumer(
            "MATCH (n) RETURN collect(DISTINCT n." + WRITE_PROPERTY + ") AS components ",
            row -> {
                assertThat(row.get("components"))
                    .asList()
                    .containsExactlyInAnyOrderElementsOf(expectedComponentIds);
            }
        );
    }

    @Test
    void testWriteWithConsecutiveIds() {
        var projectQuery = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .graphProject()
            .loadEverything(Orientation.NATURAL)
            .yields();
        runQuery(projectQuery);
        String query = GdsCypher
            .call(DEFAULT_GRAPH_NAME)
            .algo("wcc")
            .writeMode()
            .addParameter("writeProperty", WRITE_PROPERTY)
            .addParameter("consecutiveIds", true)
            .yields("componentCount");

        runQueryWithRowConsumer(query, row -> {
            assertThat(row.getNumber("componentCount"))
                .asInstanceOf(LONG)
                .isEqualTo(3L);

        });

        runQueryWithRowConsumer(
            "MATCH (n) RETURN collect(DISTINCT n." + WRITE_PROPERTY + ") AS components ",
            row -> assertThat(row.get("components")).asList().containsExactlyInAnyOrder(0L, 1L, 2L)
        );
    }

    @Test
    void zeroCommunitiesInEmptyGraph() {
        runQuery("CALL db.createLabel('VeryTemp')");
        runQuery("CALL db.createRelationshipType('VERY_TEMP')");

        var createQuery = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .graphProject()
            .withNodeLabel("VeryTemp")
            .withRelationshipType("VERY_TEMP")
            .yields();
        runQuery(createQuery);
        String query = GdsCypher
            .call(DEFAULT_GRAPH_NAME)
            .algo("wcc")
            .writeMode()
            .addParameter("writeProperty", "foo")
            .yields("componentCount");

        assertCypherResult(query, List.of(Map.of("componentCount", 0L)));
    }

    private void assertForSeedTests(String query, String writeProperty) {
        runQueryWithRowConsumer(query, row -> {
            assertThat(row.getNumber("componentCount"))
                .asInstanceOf(LONG)
                .isEqualTo(3L);
        });

        runQueryWithRowConsumer(
            formatWithLocale("MATCH (n) RETURN n.%s AS %s", writeProperty, writeProperty),
            row -> {
                assertThat(row.getNumber(writeProperty))
                    .asInstanceOf(LONG)
                    .isGreaterThanOrEqualTo(42L);
            }
        );

        runQueryWithRowConsumer(
            formatWithLocale("MATCH (n) RETURN n.nodeId AS nodeId, n.%s AS %s", writeProperty, writeProperty),
            row -> {
                final long nodeId = row.getNumber("nodeId").longValue();
                final long componentId = row.getNumber(writeProperty).longValue();
                if (nodeId >= 0 && nodeId <= 6) {
                    assertThat(componentId).isEqualTo(42L);
                } else {
                    assertThat(componentId).isNotEqualTo(42L);
                }
            }
        );
    }
}
