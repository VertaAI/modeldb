package edu.mit.csail.db.ml.server.algorithm;

import edu.mit.csail.db.ml.server.storage.TransformerDao;
import edu.mit.csail.db.ml.util.Pair;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.LinearmodelRecord;
import modeldb.*;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.TDistribution;
import org.apache.commons.math.distribution.TDistributionImpl;
import org.jooq.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class contains methods that operate on linear models.
 */
public class LinearModelAlgorithms {
  /**
   * Standardized linear models are assumed to have a hyperparameter called standardization which has a corresponding
   * value of "true".
   */
  private final static String STANDARDIZATION = "standardization";

  /**
   * Checks whether the given model is a linear model.
   * @param modelId - The ID of the model.
   * @param ctx - The database context.
   * @return Whether the model is a linear model (has entry in LinearModel table).
   */
  private static boolean isLinearModel(int modelId, DSLContext ctx) {
    return ctx.selectFrom(Tables.LINEARMODEL).where(Tables.LINEARMODEL.MODEL.eq(modelId)).fetchOne() != null;
  }

  /**
   * Checks whether the given model is a standardized model.
   * @param modelId - The ID of the model.
   * @param ctx - The database context.
   * @return Whether the model is standardized (has a hyperparameter called "standardization" that takes on the value
   * "true".
   */
  private static boolean isStandardized(int modelId, DSLContext ctx) {
    Record1<String> rec = ctx
      .select(Tables.HYPERPARAMETER.PARAMVALUE)
      .from(
        Tables.HYPERPARAMETER.join(Tables.FITEVENT).on(Tables.HYPERPARAMETER.SPEC.eq(Tables.FITEVENT.TRANSFORMERSPEC))
      )
      .where(
        Tables.FITEVENT.TRANSFORMER.eq(modelId)
          .and(Tables.HYPERPARAMETER.PARAMNAME.eq(STANDARDIZATION))
      )
      .fetchOne();
    return rec != null && rec.value1().equals("true");
  }

  /**
   * Order the features of a linear model by importance. This assumes that the model is standardized.
   * @param modelId - The ID of the mdoel.
   * @param ctx - The database context.
   * @return The feature indices (i.e. index in feature vector), ordered from most important to least important.
   */
  private static List<Integer> orderedFeatures(int modelId, DSLContext ctx) {
    return ctx
      .select(Tables.FEATURE.FEATUREINDEX)
      .from(Tables.FEATURE)
      .where(Tables.FEATURE.TRANSFORMER.eq(modelId))
      .orderBy(Tables.FEATURE.IMPORTANCE.desc())
      .fetch()
      .map(Record1::value1);
  }

  /**
   * Get the names of the features used in the given model.
   * @param modelId - The ID of the model.
   * @param ctx - The database context.
   * @return A map from the feature index (in feature vector) to the name of the feature.
   */
  private static Map<Integer, String> nameForFeature(int modelId, DSLContext ctx) {
    return ctx.select(Tables.FEATURE.FEATUREINDEX, Tables.FEATURE.NAME)
      .from(Tables.FEATURE)
      .where(Tables.FEATURE.TRANSFORMER.eq(modelId))
      .orderBy(Tables.FEATURE.FEATUREINDEX)
      .fetch()
      .stream()
      .collect(Collectors.toMap(Record2::value1, Record2::value2));
  }

  /**
   * Given a sequence of objective function values, count the number of iterations until convergence is achieved.
   * We say that a sequence, S (zero-indexed), has converged at iteration i + 1 if |S[i] - S[i - 1]| < tolerance.
   * @param objectiveHistory - The sequence of objective function values.
   * @param tolerance - The tolerance level to determine convergence. That is, if the objective function changes by
   *                  less than the tolerance level in consecutive iterations, then it is assumed to have converged.
   * @return The number of iterations until convergence.
   */
  private static int iterationsUntilConvergence(List<Double> objectiveHistory, double tolerance) {
    for (int i = 1; i < objectiveHistory.size(); i++) {
      if (Math.abs(objectiveHistory.get(i) - objectiveHistory.get(i - 1)) < tolerance) {
        return i + 1;
      }
    }
    return objectiveHistory.size();
  }

  /**
   * Determine t-distribution confidence intervals for each coefficient of linear model.
   * @param modelId - The ID of the model.
   * @param significanceLevel - The confidence level (should be greater than 0 and below 1).
   * @param ctx - The database context.
   * @return The confidence interval for each coefficient.
   */
  public static List<ConfidenceInterval> confidenceIntervals(int modelId, double significanceLevel, DSLContext ctx)
    throws BadRequestException, IllegalOperationException, ResourceNotFoundException {
    // Ensure that the significance level lies in (0, 1).
    if (!(significanceLevel > 0 && significanceLevel < 1)) {
      throw new BadRequestException(String.format(
        "Can't make linear model confidence intervals for Transformer %d because significance level %.4f " +
          "must be between 0 and 1 (exclusive both sides)",
        modelId,
        significanceLevel
      ));
    }

    // Ensure the model exists.
    if (!TransformerDao.exists(modelId, ctx)) {
      throw new ResourceNotFoundException(String.format(
        "Can't make linear model confidence intervals for Transformer %d because it doesn't exist",
        modelId
      ));
    }

    // Ensure that the model is linear.
    if (!isLinearModel(modelId, ctx)) {
      throw new IllegalOperationException(String.format(
        "Can't make linear model confidence intervals for Transformer %d because it's not a linear model",
        modelId
      ));
    }

    // Get the number of rows.
    Record1<Integer> rec = ctx
      .select(Tables.DATAFRAME.NUMROWS)
      .from(Tables.DATAFRAME.join(Tables.FITEVENT).on(Tables.DATAFRAME.ID.eq(Tables.FITEVENT.DF)))
      .where(Tables.FITEVENT.TRANSFORMER.eq(modelId))
      .fetchOne();
    if (rec == null) {
      throw new ResourceNotFoundException(String.format(
        "Can't make linear model confidence intervals for Transformer %d - it doesn't have an associated DataFrame",
        modelId
      ));
    }
    int numRows = rec.value1();

    // Get the standard errors and coefficients.
    Map<Integer, Pair<Double, Double>> coeffStderrPairsForIndex = ctx
      .select(Tables.LINEARMODELTERM.TERMINDEX, Tables.LINEARMODELTERM.COEFFICIENT, Tables.LINEARMODELTERM.STDERR)
      .from(Tables.LINEARMODELTERM)
      .where(Tables.LINEARMODELTERM.MODEL.eq(modelId).and(Tables.LINEARMODELTERM.STDERR.isNotNull()))
      .orderBy(Tables.LINEARMODELTERM.TERMINDEX.asc())
      .fetch()
      .stream()
      .collect(Collectors.toMap(Record3::value1, r -> new Pair<Double, Double>(r.value2(), r.value3())));

    // Compute the degrees of freedom and create the t-distribution.
    int df = numRows - coeffStderrPairsForIndex.size() - ((coeffStderrPairsForIndex.containsKey(0)) ? 1 : 0);
    TDistribution dist = new TDistributionImpl(df);

    return coeffStderrPairsForIndex
      .entrySet()
      .stream()
      .map(pair -> {
        int featureIndex = pair.getKey();
        double coeff = pair.getValue().getFirst();
        double stderr = pair.getValue().getSecond();
        double t = 0;
        try {
          t = dist.inverseCumulativeProbability(1 - significanceLevel);
        } catch (MathException mEx) {
          mEx.printStackTrace();;
        }
        return new ConfidenceInterval(
          featureIndex,
          coeff - t * stderr,
          coeff + t * stderr
        );
      })
      .sorted((o1, o2) -> Integer.valueOf(o1.featureIndex).compareTo(o2.featureIndex))
      .collect(Collectors.toList());
  }

  /**
   * Rank the given models according to a ranking metric.
   * @param modelIds - The IDs of the models to rank.
   * @param metric - The metric to use for ranking.
   * @param ctx - The database context.
   * @return The IDs of the models. The models are ordered by their ranking.
   */
  public static List<Integer> rankModels(List<Integer> modelIds, ModelRankMetric metric, DSLContext ctx) {
    TableField<LinearmodelRecord, Double> rankField = Tables.LINEARMODEL.RMSE;
    switch (metric) {
      case R2: rankField = Tables.LINEARMODEL.R2; break;
      case EXPLAINED_VARIANCE: rankField = Tables.LINEARMODEL.EXPLAINEDVARIANCE; break;
      default:
    }

    return ctx
      .select(Tables.LINEARMODEL.MODEL)
      .from(Tables.LINEARMODEL)
      .where(Tables.LINEARMODEL.MODEL.in(modelIds).and(rankField.isNotNull()))
      .orderBy(rankField.desc())
      .stream()
      .map(Record1::value1)
      .collect(Collectors.toList());
  }

  /**
   * Compute the number of iterations until convergence for each of the given models. See
   * iterationsUntilConvergence(List<Double> objectiveHistory, double tolerance) for more information.
   * @param modelIds - The IDs of the models.
   * @param tolerance - The tolerance level for determining convergence.
   * @param ctx - The database context.
   * @return The list where the value at the i^th index is the number of iterations until convergence for the model with
   * ID modelIds[i].
   */
  public static List<Integer> iterationsUntilConvergence(List<Integer> modelIds, double tolerance, DSLContext ctx) {
    Map<Integer, List<Double>> objectiveHistoryForModel = new HashMap<>();
    ctx
      .select(
        Tables.MODELOBJECTIVEHISTORY.MODEL,
        Tables.MODELOBJECTIVEHISTORY.ITERATION,
        Tables.MODELOBJECTIVEHISTORY.OBJECTIVEVALUE
      )
      .from(Tables.MODELOBJECTIVEHISTORY)
      .where(Tables.MODELOBJECTIVEHISTORY.MODEL.in(modelIds))
      .orderBy(Tables.MODELOBJECTIVEHISTORY.ITERATION.asc())
      .fetch()
      .forEach(rec -> {
        if (!objectiveHistoryForModel.containsKey(rec.value1())) {
          objectiveHistoryForModel.put(rec.value1(), new ArrayList<>());
        }
        assert (objectiveHistoryForModel.get(rec.value1()).size() == rec.value2() - 1);
        objectiveHistoryForModel.get(rec.value1()).add(rec.value3());
      });

    Map<Integer, Integer> iterationsForModel = objectiveHistoryForModel
      .entrySet()
      .stream()
      .map(entry -> new Pair<>(entry.getKey(), iterationsUntilConvergence(entry.getValue(), tolerance)))
      .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));

    return modelIds
      .stream()
      .map(id -> iterationsForModel.getOrDefault(id, -1))
      .collect(Collectors.toList());
  }

  /**
   * Check if the given model is a standardized linear model. This throws an exception if the model is not linear and
   * standardized.
   * @param modelId - The ID of the model.
   * @param ctx - The database context.
   */
  private static void checkIsStandardizeLinearModel(int modelId, DSLContext ctx)
    throws ResourceNotFoundException, IllegalOperationException {
    if (!TransformerDao.exists(modelId, ctx)) {
      throw new ResourceNotFoundException(String.format(
        "We can't find linear model feature importances for Transformer %d because it doesn't exist",
        modelId
      ));
    }

    if (!isLinearModel(modelId, ctx)) {
      throw new IllegalOperationException(String.format(
        "We can't find linear model feature importances for Transformer %d because it's not a linear model",
        modelId
      ));
    }

    if (!isStandardized(modelId, ctx)) {
      throw new IllegalOperationException(String.format(
        "Transformer %d is a linear model, but it's not standardized, so we can't find feature importances",
        modelId
      ));
    }
  }

  /**
   * Return the names of the features of the given model, ordered from most important feature to least
   * important feature.
   * @param modelId - The ID of the model. This must be a linear, standardized model. If not, an exception will be
   *                thrown.
   * @param ctx - The database context.
   * @return The feature names such that the most important feature is first and the least important feature is lst.
   */
  public static List<String> featureImportances(int modelId, DSLContext ctx)
    throws ResourceNotFoundException, IllegalOperationException {
    // Ensure the model is a standardized linear model.
    checkIsStandardizeLinearModel(modelId, ctx);

    // Now get the features, ordered by importance.
    List<Integer> featureImportances = orderedFeatures(modelId, ctx);

    // Get the map from feature number to feature name.
    Map<Integer, String> nameForFeature = nameForFeature(modelId, ctx);

    // Convert the numbers into names.
    return featureImportances
      .stream()
      .map(i -> nameForFeature.containsKey(i) ? nameForFeature.get(i) : "unknown")
      .collect(Collectors.toList());
  }

  /**
   * Compare the importance of features between two models.
   * @param model1Id - The ID of the first model.
   * @param model2Id - The ID of the second model.
   * @param ctx - The database context.
   * @return The comparison of feature importances.
   */
  public static List<FeatureImportanceComparison> featureImportances(int model1Id, int model2Id, DSLContext ctx)
    throws ResourceNotFoundException, IllegalOperationException {
    // Ensure the models are standardized linear models.
    checkIsStandardizeLinearModel(model1Id, ctx);
    checkIsStandardizeLinearModel(model2Id, ctx);

    // Get the features, ordered by importance, for both models.
    List<Integer> m1FeatureImportances = orderedFeatures(model1Id, ctx);
    List<Integer> m2FeatureImportances = orderedFeatures(model2Id, ctx);

    // Convert the integer lists to maps from feature number to feature name.
    Map<Integer, String> m1NameForFeature = nameForFeature(model1Id, ctx);
    Map<Integer, String> m2NameForFeature = nameForFeature(model2Id, ctx);

    // Invert the maps.
    Map<String, Integer> m1RankForFeature =
      m1NameForFeature.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    Map<String, Integer> m2RankForFeature =
      m2NameForFeature.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    // Now we will create the List of FeatureImportanceComparison objects.
    ArrayList<FeatureImportanceComparison> importances = new ArrayList<>();

    // We'll track the feature names we've already considered.
    Set<String> usedFeatureNames = new HashSet<>();

    // Count the number of features in each model, we need this for the percentile rank computation.
    int m1NumFeatures = m1FeatureImportances.size();
    int m2NumFeatures = m2FeatureImportances.size();

    // Iterate through every feature in the first model.
    m1RankForFeature.entrySet().forEach(entry -> {
      // Mark the feature as processed.
      usedFeatureNames.add(entry.getKey());

      // Compute the feature's percentile rank in model 1.
      FeatureImportanceComparison imp = new FeatureImportanceComparison(entry.getKey());
      imp.setPercentileRankInModel1(entry.getValue().doubleValue() / m1NumFeatures);

      // If the feature is in model 2, compute its percentile rank or default to -1.
      if (m2RankForFeature.containsKey(entry.getKey())) {
        imp.setPercentileRankInModel2(m2RankForFeature.get(entry.getKey()).doubleValue() / m2NumFeatures);
      }

      // Add the feature to the importance list.
      importances.add(imp);
    });

    // Iterate through every feature in the second model.
    m2RankForFeature.entrySet().forEach(entry -> {
      // Consider features that are in the second model but NOT the first model.
      if (!usedFeatureNames.contains(entry.getKey())) {
        // Compute the percentile rank in the second model.
        FeatureImportanceComparison imp = new FeatureImportanceComparison(entry.getKey());
        imp.setPercentileRankInModel2(entry.getValue().doubleValue() / m2NumFeatures);

        // Add the feature to the importance list.
        importances.add(imp);
      }
    });

    return importances;
  }
}
