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
package org.neo4j.gds.paths.dijkstra;

import java.util.List;

public interface Targets {

    TraversalState apply(long nodeId);

    static Targets of(List<Long> targetNodeIds){

        if (!targetNodeIds.isEmpty()){
                if (targetNodeIds.size()==1){
                    return  new SingleTarget(targetNodeIds.get(0));
                }
                return new ManyTargets(targetNodeIds);
        }
        return  new AllTargets();

    }
}
