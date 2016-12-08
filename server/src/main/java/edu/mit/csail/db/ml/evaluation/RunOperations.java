package edu.mit.csail.db.ml.evaluation;

import edu.mit.csail.db.ml.conf.ModelDbConfig;
import edu.mit.csail.db.ml.server.algorithm.*;
import edu.mit.csail.db.ml.server.algorithm.similarity.SimilarModels;
import edu.mit.csail.db.ml.util.ContextFactory;
import edu.mit.csail.db.ml.util.Timer;
import modeldb.LinearModel;
import modeldb.ModelCompMetric;
import modeldb.ModelRankMetric;
import org.jooq.DSLContext;

import java.util.Arrays;
import java.util.Collections;

public class RunOperations {
  /**
   * There should be one command line argument - the path to write the output to.
   */
  public static void main(String[] args) throws Exception {
    ModelDbConfig config = ModelDbConfig.parse(new String[] {});
    DSLContext ctx = ContextFactory.create(config.dbUser, config.dbPassword, config.jbdcUrl, config.dbType);
    Timer.clear();

    // Ancestry algorithms.
    Timer.time("Ancestry", () -> DataFrameAncestryComputer.compute(140, ctx));
    Timer.time("Common Ancestor", () -> DataFrameAncestryComputer.computeCommonAncestor(140, 141, ctx));
    Timer.time("Model Ancestry", () -> DataFrameAncestryComputer.computeModelAncestry(92, ctx));
    Timer.time("Common Ancestor Models", () -> DataFrameAncestryComputer.computeCommonAncestorForModels(92, 99, ctx));
    Timer.time("Descendent Models", () -> DataFrameAncestryComputer.descendentModels(1, ctx));

    // Feature algorithms.
    Timer.time("Models with Features", () -> Feature.modelsWithFeatures(Collections.singletonList("isFilm-Noir"), ctx));
    Timer.time("Original Features", () -> Feature.originalFeatures(92, ctx));
    Timer.time("Compare Features", () -> Feature.compareFeatures(92, 99, ctx));

    // Hyperparameter algorithms.
    Timer.time("Compare Hyperparameters", () -> HyperparameterComparison.compareHyperParameters(92, 99, ctx));

    // Linear and Tree Model algorithms.
    Timer.time("Importances", () -> LinearModelAlgorithms.featureImportances(22, ctx));
    Timer.time("Compare Importances", () -> LinearModelAlgorithms.featureImportances(22, 142, ctx));

    // Rank Models.
    Timer.time("Rank Models", () -> LinearModelAlgorithms.rankModels(
      Arrays.asList(22, 142),
      ModelRankMetric.EXPLAINED_VARIANCE,
      ctx
    ));

    // Similar Models.
    Timer.time("Similar Models", () -> SimilarModels.similarModels(
      92,
      Collections.singletonList(ModelCompMetric.EXPERIMENT_RUN),
      2,
      ctx
    ));

    // Group Models
    Timer.time("Group Models", () -> ProblemTypeGrouper.groupByProblemType(Arrays.asList(92, 99), ctx));

    // Write the results to the given file.
    Timer.writeToFile(args[0]);
  }
}
