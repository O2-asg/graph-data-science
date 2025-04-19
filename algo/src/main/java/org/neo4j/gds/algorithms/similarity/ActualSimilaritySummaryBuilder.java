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
package org.neo4j.gds.algorithms.similarity;

import org.HdrHistogram.DoubleHistogram;
import org.neo4j.gds.api.RelationshipWithPropertyConsumer;
import org.neo4j.gds.result.SimilarityStatistics;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.neo4j.gds.core.ProcedureConstants.HISTOGRAM_PRECISION_DEFAULT;

public class ActualSimilaritySummaryBuilder implements SimilaritySummaryBuilder {

    private final DoubleHistogram histogram;
    private final AtomicBoolean crashed = new AtomicBoolean(false);

    ActualSimilaritySummaryBuilder() {
        this.histogram = new DoubleHistogram(HISTOGRAM_PRECISION_DEFAULT);
    }

    ActualSimilaritySummaryBuilder(DoubleHistogram histogram ) {
        this.histogram = histogram;
    }

    @Override
    public RelationshipWithPropertyConsumer similarityConsumer() {
            return  (node1,node2, similarity) -> {
                similarityConsume(similarity);
                return true;
            };

}

    @Override
    public Map<String,Object> similaritySummary(){

     return  SimilarityStatistics.similaritySummary(Optional.of(histogram), !crashed.get());
    }


    private  void similarityConsume(double similarity){
        try {
            if (!crashed.get()) {
                histogram.recordValue(similarity);
            }
        } catch (ArrayIndexOutOfBoundsException e) {

            if (e.getMessage().contains("is out of bounds for histogram, current covered range")) {
                this.crashed.set(true);
            }else{
                throw  e;
            }
        }
    }

}


