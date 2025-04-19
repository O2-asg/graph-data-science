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
package org.neo4j.gds.closeness;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.gds.BaseProcTest;
import org.neo4j.gds.GdsCypher;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.gds.extension.IdFunction;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.extension.Neo4jGraph;

import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

class ClosenessCentralityWriteProcTest extends BaseProcTest {

    @Neo4jGraph
    public static final String DB_CYPHER =
        "CREATE" +
        "  (n0:Node)" +
        ", (n1:Node)" +
        ", (n2:Node)" +
        ", (n3:Node)" +
        ", (n4:Node)" +
        ", (n5:Node)" +
        ", (n6:Node)" +
        ", (n7:Node)" +
        ", (n8:Node)" +
        ", (n9:Node)" +
        ", (n10:Node)" +

        // first ring
        ", (n1)-[:TYPE]->(n2)" +
        ", (n2)-[:TYPE]->(n3)" +
        ", (n3)-[:TYPE]->(n4)" +
        ", (n4)-[:TYPE]->(n5)" +
        ", (n5)-[:TYPE]->(n1)" +

        ", (n0)-[:TYPE]->(n0)" +
        ", (n1)-[:TYPE]->(n0)" +
        ", (n2)-[:TYPE]->(n0)" +
        ", (n3)-[:TYPE]->(n0)" +
        ", (n4)-[:TYPE]->(n0)" +
        ", (n5)-[:TYPE]->(n0)" +

        // second ring
        ", (n6)-[:TYPE]->(n7)" +
        ", (n7)-[:TYPE]->(n8)" +
        ", (n8)-[:TYPE]->(n9)" +
        ", (n9)-[:TYPE]->(n10)" +
        ", (n10)-[:TYPE]->(n6)" +

        ", (n0)-[:TYPE]->(n0)" +
        ", (n0)-[:TYPE]->(n1)" +
        ", (n0)-[:TYPE]->(n2)" +
        ", (n0)-[:TYPE]->(n3)" +
        ", (n0)-[:TYPE]->(n4)" +
        ", (n0)-[:TYPE]->(n5)" +
        ", (n0)-[:TYPE]->(n6)" +
        ", (n0)-[:TYPE]->(n7)" +
        ", (n0)-[:TYPE]->(n8)" +
        ", (n0)-[:TYPE]->(n9)" +
        ", (n0)-[:TYPE]->(n10)";

    @Inject
    private IdFunction idFunction;

    private Map<Long, Double> expectedCentralityResult;

    private static final String WRITE_PROPERTY = "score";

    @BeforeEach
    void setupGraph() throws Exception {
        registerProcedures(
            ClosenessCentralityWriteProc.class,
            GraphProjectProc.class
        );

        expectedCentralityResult = Map.ofEntries(
            entry(idFunction.of("n0"), 1.0),
            entry(idFunction.of("n1"), 0.588),
            entry(idFunction.of("n2"), 0.588),
            entry(idFunction.of("n3"), 0.588),
            entry(idFunction.of("n4"), 0.588),
            entry(idFunction.of("n5"), 0.588),
            entry(idFunction.of("n6"), 0.588),
            entry(idFunction.of("n7"), 0.588),
            entry(idFunction.of("n8"), 0.588),
            entry(idFunction.of("n9"), 0.588),
            entry(idFunction.of("n10"), 0.588)
        );

        loadCompleteGraph(DEFAULT_GRAPH_NAME, Orientation.UNDIRECTED);

    }
    
    @Test
    void shouldWrite() {
        var query = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .algo("closeness")
            .writeMode()
            .addParameter("writeProperty", WRITE_PROPERTY)
            .yields();

        runQueryWithRowConsumer(query, row -> {

            assertThat(row.get("configuration"))
                .isNotNull()
                .isInstanceOf(Map.class);

            assertThat(row.getNumber("writeMillis")).isNotEqualTo(-1L);
            assertThat(row.getNumber("preProcessingMillis")).isNotEqualTo(-1L);
            assertThat(row.getNumber("computeMillis")).isNotEqualTo(-1L);
            assertThat(row.getNumber("nodePropertiesWritten")).isEqualTo(11L);

            assertThat(row.get("configuration")).isNotNull();

            assertThat(row.get("centralityDistribution"))
                .asInstanceOf(MAP)
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                    "max", 1.000007629394531,
                    "mean", 0.6256675720214844,
                    "min", 0.5882339477539062,
                    "p50", 0.5882339477539062,
                    "p75", 0.5882339477539062,
                    "p90", 0.5882339477539062,
                    "p95", 1.0000038146972656,
                    "p99", 1.0000038146972656,
                    "p999", 1.0000038146972656
                ));
        });

        var writeValidationQuery = formatWithLocale(
            "MATCH (n) WHERE n.%1$s IS NOT NULL RETURN id(n) AS nodeId, n.%1$s AS %1$s",
            WRITE_PROPERTY
        );

        var rowCount = runQueryWithRowConsumer(writeValidationQuery, row -> {
                var nodeId = row.getNumber("nodeId").longValue();
                var property = row.getNumber(WRITE_PROPERTY).doubleValue();
                assertThat(expectedCentralityResult.get(nodeId)).isCloseTo(property, Offset.offset(0.01));
            }
        );
        assertThat(rowCount).isEqualTo(11);

    }

    @Test
    void betaShouldWrite() {
        var query = GdsCypher.call(DEFAULT_GRAPH_NAME)
            .algo("gds.beta.closeness")
            .writeMode()
            .addParameter("writeProperty", WRITE_PROPERTY)
            .yields();

        runQueryWithRowConsumer(query, row -> {

            assertThat(row.get("configuration"))
                .isNotNull()
                .isInstanceOf(Map.class);

            assertThat(row.getNumber("writeMillis")).isNotEqualTo(-1L);
            assertThat(row.getNumber("preProcessingMillis")).isNotEqualTo(-1L);
            assertThat(row.getNumber("computeMillis")).isNotEqualTo(-1L);
            assertThat(row.getNumber("nodePropertiesWritten")).isEqualTo(11L);

            assertThat(row.get("configuration")).isNotNull();

            assertThat(row.get("centralityDistribution"))
                .isNotNull()
                .isInstanceOf(Map.class);

            assertThat(row.get("writeProperty")).isEqualTo(WRITE_PROPERTY);

        });

        var writeValidationQuery = formatWithLocale(
            "MATCH (n) WHERE n.%1$s IS NOT NULL RETURN id(n) AS nodeId, n.%1$s AS %1$s",
            WRITE_PROPERTY
        );

        var rowCount = runQueryWithRowConsumer(writeValidationQuery, row -> {
                var nodeId = row.getNumber("nodeId").longValue();
                var property = row.getNumber(WRITE_PROPERTY).doubleValue();
                assertThat(expectedCentralityResult.get(nodeId)).isCloseTo(property, Offset.offset(0.01));
            }
        );
        assertThat(rowCount).isEqualTo(11);
    }

}
