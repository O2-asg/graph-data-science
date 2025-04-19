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
package org.neo4j.gds.ml.linkmodels.pipeline.predict;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.concurrency.DefaultPool;
import org.neo4j.gds.mem.MemoryEstimation;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.ml.linkmodels.LinkPredictionResult;
import org.neo4j.gds.ml.linkmodels.PredictedLink;
import org.neo4j.gds.ml.models.Classifier;
import org.neo4j.gds.ml.pipeline.linkPipeline.LinkFeatureExtractor;
import org.neo4j.gds.similarity.knn.ImmutableKnnContext;
import org.neo4j.gds.similarity.knn.Knn;
import org.neo4j.gds.similarity.knn.KnnParameters;
import org.neo4j.gds.similarity.knn.KnnResult;
import org.neo4j.gds.termination.TerminationFlag;

import java.util.Map;
import java.util.stream.Stream;

public class ApproximateLinkPrediction extends LinkPrediction {
    private final KnnParameters knnParameters;
    private final TerminationFlag terminationFlag;

    public ApproximateLinkPrediction(
        Classifier classifier,
        LinkFeatureExtractor linkFeatureExtractor,
        Graph graph,
        LPNodeFilter sourceNodeFilter,
        LPNodeFilter targetNodeFilter,
        KnnParameters knnParameters,
        ProgressTracker progressTracker,
        TerminationFlag terminationFlag
    ) {
        super(
            classifier,
            linkFeatureExtractor,
            graph,
            sourceNodeFilter,
            targetNodeFilter,
            knnParameters.concurrency(),
            progressTracker
        );
        this.knnParameters = knnParameters;
        this.terminationFlag = terminationFlag;
    }

    public static MemoryEstimation estimate(LinkPredictionPredictPipelineBaseConfig config) {
        return new ApproximateLinkPredictionEstimateDefinition(config).memoryEstimation();
    }

    @Override
    LinkPredictionResult predictLinks(LinkPredictionSimilarityComputer linkPredictionSimilarityComputer) {
        var knn = Knn.create(
            graph,
            knnParameters,
            linkPredictionSimilarityComputer,
            new LinkPredictionSimilarityComputer.LinkFilterFactory(
                graph,
                sourceNodeFilter,
                targetNodeFilter
            ),
            ImmutableKnnContext.of(
                DefaultPool.INSTANCE,
                progressTracker
            ),
            TerminationFlag.RUNNING_TRUE
        );

        knn.setTerminationFlag(terminationFlag);
        var knnResult = knn.compute();

        return new Result(knnResult);
    }

    static class Result implements LinkPredictionResult {
        private final KnnResult predictions;
        private final Map<String, Object> samplingStats;

        Result(KnnResult knnResult) {
            this.predictions = knnResult;
            this.samplingStats = Map.of(
                "strategy", "approximate",
                "linksConsidered", knnResult.nodePairsConsidered(),
                "ranIterations", knnResult.ranIterations(),
                "didConverge", knnResult.didConverge()
            );
        }

        @Override
        public Stream<PredictedLink> stream() {
            return predictions
                .streamSimilarityResult()
                .map(i -> PredictedLink.of(i.sourceNodeId(), i.targetNodeId(), i.similarity));
        }

        @Override
        public Map<String, Object> samplingStats() {
            return samplingStats;
        }
    }
}
