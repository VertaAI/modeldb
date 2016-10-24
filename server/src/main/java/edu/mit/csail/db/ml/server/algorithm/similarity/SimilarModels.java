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

public class SimilarModels {
  // TODO: Should this go into the conf file?
  private static final int MODEL_LIMIT = 1000;

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

  public static List<Integer> similarModels(
    int modelId,
    List<ModelCompMetric> compMetrics,
    int numModels,
    DSLContext ctx
  ) throws ResourceNotFoundException, BadRequestException {
    if (!TransformerDao.exists(modelId, ctx)) {
      throw new ResourceNotFoundException(String.format(
        "Cannot find models similar to Transformer %d because that Transformer does not exist",
        modelId
      ));
    }
    if (numModels <= 0) {
      throw new BadRequestException(String.format(
        "You can't ask for %d similar models, you must request a positive number of models",
        numModels
      ));
    }
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
    return resultingModels.subList(0, Math.min(resultingModels.size(), numModels));
  }
}
