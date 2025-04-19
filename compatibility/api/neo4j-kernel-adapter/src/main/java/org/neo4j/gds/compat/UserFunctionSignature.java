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
package org.neo4j.gds.compat;

import org.jetbrains.annotations.NotNull;
import org.neo4j.gds.annotation.GenerateBuilder;
import org.neo4j.internal.kernel.api.procs.FieldSignature;
import org.neo4j.internal.kernel.api.procs.Neo4jTypes;
import org.neo4j.internal.kernel.api.procs.QualifiedName;

import java.util.List;
import java.util.Optional;

@GenerateBuilder
public record UserFunctionSignature(
    @NotNull QualifiedName name,
    @NotNull List<FieldSignature> inputField,
    @NotNull Neo4jTypes.AnyType returnType,
    @NotNull String description,
    Optional<String> deprecatedBy,
    boolean internal,
    boolean threadSafe,
    @NotNull Neo4jProxyApi compat
) {
    public org.neo4j.internal.kernel.api.procs.UserFunctionSignature toNeo() {
        return this.compat.userFunctionSignature(
            this.name,
            this.inputField,
            this.returnType,
            this.description,
            this.deprecatedBy,
            this.internal,
            this.threadSafe
        );
    }
}
