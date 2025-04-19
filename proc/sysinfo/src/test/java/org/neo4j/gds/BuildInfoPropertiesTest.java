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
package org.neo4j.gds;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class BuildInfoPropertiesTest {

    @Test
    void shouldReturnGradleVersion() throws IOException {
        // we find the current version in the gradle file
        var file = Paths.get("../../gradle/version.gradle");
        var maybeVersion = findVersion(file);
        var expectedVersion = maybeVersion.orElseGet(() ->
            fail("Could not find version in file: " + file.toAbsolutePath()));

        var buildInfo = BuildInfoProperties.get();

        // strip Build identifier (for AuraDS releases)
        // as the aurads flag is only a gradle property we cannot read the value here
        String[] splits = buildInfo.gdsVersion().split("\\+");
        assertThat(splits).hasSizeLessThanOrEqualTo(2);

        var actualBaseVersion = splits[0];
        var actualBuildLabel = splits.length > 1 ? splits[1] : "";

        if (!actualBuildLabel.isEmpty()) {
            var expectedQualifier = findAuraDSBuildLabel(file).orElseGet(() ->
                fail("Could not find AuraDS qualifier in file: " + file.toAbsolutePath()));
            assertEquals(expectedQualifier, actualBuildLabel);
        }

        assertEquals(expectedVersion, actualBaseVersion);
    }

    @Test
    void loadFromProperties() {
        var properties = new Properties();
        var version = "42.1.33.7";
        var buildDate = LocalDateTime.now().toString();
        var buildHash = "34973274ccef6ab4dfaaf86599792fa9c3fe4689";
        var buildJavaVersion = "13.3.7";
        var buildJdk = buildJavaVersion + " (GdsJvm)";
        var minimumRequiredJavaVersion = "11";
        properties.putAll(Map.of(
            "Implementation-Version", version,
            "Build-Date", buildDate,
            "Full-Change",buildHash,
            "Build-Java-Version", buildJavaVersion,
            "Created-By", buildJdk,
            "X-Compile-Target-JDK", minimumRequiredJavaVersion
        ));

        var buildInfoProperties = BuildInfoProperties.from(properties);
        assertEquals(version, buildInfoProperties.gdsVersion());
        assertEquals(buildDate, buildInfoProperties.buildDate());
        assertEquals(buildHash, buildInfoProperties.buildHash());
        assertEquals(buildJavaVersion, buildInfoProperties.buildJavaVersion());
        assertEquals(buildJdk, buildInfoProperties.buildJdk());
        assertEquals(minimumRequiredJavaVersion, buildInfoProperties.minimumRequiredJavaVersion());
    }

    @Test
    void loadFromPropertiesWithDefaults() {
        var properties = new Properties();
        var version = "42.1.33.7";
        properties.setProperty("Implementation-Version", version);

        var buildInfoProperties = BuildInfoProperties.from(properties);
        assertEquals(version, buildInfoProperties.gdsVersion());
        assertEquals("unknown", buildInfoProperties.buildDate());
        assertEquals("unknown", buildInfoProperties.buildHash());
        assertEquals("unknown", buildInfoProperties.buildJavaVersion());
        assertEquals("unknown", buildInfoProperties.buildJdk());
        assertEquals("unknown", buildInfoProperties.minimumRequiredJavaVersion());
    }

    @Test
    void loadFromPropertiesRequiresVersion() {
        var properties = new Properties();
        var exception = assertThrows(
            NullPointerException.class,
            () -> BuildInfoProperties.from(properties)
        );
        assertThat(exception).hasMessage("gdsVersion");
    }

    private Optional<String> findVersion(Path file) throws IOException {
        Pattern pattern = Pattern.compile(".*gdsBaseVersion = '(\\d\\.\\d\\.\\d+(-alpha\\d+|-beta\\d+)?)'.*");
        try(var lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return lines
                .flatMap(line -> pattern.matcher(line).results())
                .map(i -> i.group(1))
                .findFirst();
        }
    }

    private Optional<String> findAuraDSBuildLabel(Path file) throws IOException {
        Pattern pattern = Pattern.compile(".*gdsAuraDSVersion = '(\\d+)'");
        try(var lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return lines
                .flatMap(line -> pattern.matcher(line).results())
                .map(i -> i.group(1))
                .findFirst();
        }
    }
}
