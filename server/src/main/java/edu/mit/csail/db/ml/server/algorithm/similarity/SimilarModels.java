package edu.mit.csail.db.ml.server.algorithm.similarity;

import edu.mit.csail.db.ml.server.algorithm.similarity.comparators.*;
import edu.mit.csail.db.ml.server.storage.TransformerDao;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.LinearmodelRecord;
import modeldb.BadRequestException;
import modeldb.ModelCompMetric;
import modeldb.ResourceNotFoundException;
import org.jooq.DSLContext;
import org.jooq.TableField;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This exposes the similarModels method that allows finding models similar to a model with a given ID.
 */
public class SimilarModels {
  // TODO: Should this go into the conf file?
  private static final int MODEL_LIMIT = 1000;

  /**
   * This creates the appropriate ModelComparator object for a given comparison metric.
   * @param compMetric - The model comparison metric.
   * @return The ModelComparator object that corresponds to the compMetric.
   */
  private static ModelComparator comparatorForCompMetric(ModelCompMetric compMetric) {
    switch (compMetric) {
      case MODEL_TYPE: return new TransformerTypeComparator();
      case PROBLEM_TYPE: return new ProblemTypeComparator();
      case EXPERIMENT_RUN: return new ExperimentRunComparator();
      case PROJECT: return new ProjectComparator();
      case RMSE: return new LinearModelComparator() {
        @Override
        public TableField<LinearmodelRecord, Double> getComparisonField() {
          return Tables.LINEARMODEL.RMSE;
        }
      };
      case EXPLAINED_VARIANCE: return new LinearModelComparator() {
        @Override
        public TableField<LinearmodelRecord, Double> getComparisonField() {
          return Tables.LINEARMODEL.EXPLAINEDVARIANCE;
        }
      };
      case R2: return new LinearModelComparator() {
        @Override
        public TableField<LinearmodelRecord, Double> getComparisonField() {
          return Tables.LINEARMODEL.R2;
        }
      };
      default: return new DummyComparator();
    }
  }

  /**
   * Find the models similar to the model with the given ID.
   * @param modelId - The ID of the given model. This method will find models similar to this model.
   * @param compMetrics - The metrics by which to measure similarity. The first metric is the most important and thus
   *                    models that are most similar according to this metric will be first in the returned list. Each
   *                    successive metric is used to break ties in the previous metric.
   * @param numModels - The maximum number of models that should be returned.
   * @param ctx - The context for accessing the database.
   * @return The IDs of similar models, with the most similar model first and successively less similar
   * models following.
   */
  public static List<Integer> similarModels(
    int modelId,
    List<ModelCompMetric> compMetrics,
    int numModels,
    DSLContext ctx
  ) throws ResourceNotFoundException, BadRequestException {
    // Fail if modelID doesn't exist.
    if (!TransformerDao.exists(modelId, ctx)) {
      throw new ResourceNotFoundException(String.format(
        "Cannot find models similar to Transformer %d because that Transformer does not exist",
        modelId
      ));
    }

    // Fail if number of models is invalid.
    if (numModels <= 0) {
      throw new BadRequestException(String.format(
        "You can't ask for %d similar models, you must request a positive number of models",
        numModels
      ));
    }

    // Find the similar models.
    List<Integer> resultingModels = compMetrics
      .stream()
      .map(SimilarModels::comparatorForCompMetric)
      .reduce(
        Collections.<Integer>emptyList(),
        (partial, comparator) -> comparator.similarModels(modelId, partial, MODEL_LIMIT, ctx),
        (oldModels, newModels) -> newModels
      )
      .stream()
      .filter(s -> !s.equals(modelId))
      .collect(Collectors.toList());

    // Return at most numModels models.
    return resultingModels.subList(0, Math.min(resultingModels.size(), numModels));
  }
}
