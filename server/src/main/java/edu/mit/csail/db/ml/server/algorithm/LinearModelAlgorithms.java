package edu.mit.csail.db.ml.server.algorithm;

import edu.mit.csail.db.ml.server.storage.TransformerDao;
import javafx.util.Pair;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.LinearmodelRecord;
import modeldb.*;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.TDistribution;
import org.apache.commons.math.distribution.TDistributionImpl;
import org.jooq.*;

import java.util.*;
import java.util.stream.Collectors;

public class LinearModelAlgorithms {
  private final static String STANDARDIZATION = "standardization";

  private static boolean isLinearModel(int modelId, DSLContext ctx) {
    return ctx.selectFrom(Tables.LINEARMODEL).where(Tables.LINEARMODEL.MODEL.eq(modelId)).fetchOne() != null;
  }

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

  private static List<Integer> orderedFeatures(int modelId, DSLContext ctx) {
    return ctx
      .select(Tables.FEATURE.FEATUREINDEX)
      .from(Tables.FEATURE)
      .where(Tables.FEATURE.TRANSFORMER.eq(modelId))
      .orderBy(Tables.FEATURE.IMPORTANCE.desc())
      .fetch()
      .map(Record1::value1);
  }

  private static Map<Integer, String> nameForFeature(int modelId, DSLContext ctx) {
    return ctx.select(Tables.FEATURE.FEATUREINDEX, Tables.FEATURE.NAME)
      .from(Tables.FEATURE)
      .where(Tables.FEATURE.TRANSFORMER.eq(modelId))
      .orderBy(Tables.FEATURE.FEATUREINDEX)
      .fetch()
      .stream()
      .collect(Collectors.toMap(Record2::value1, Record2::value2));
  }

  private static int iterationsUntilConvergence(List<Double> objectiveHistory, double tolerance) {
    for (int i = 1; i < objectiveHistory.size(); i++) {
      if (Math.abs(objectiveHistory.get(i) - objectiveHistory.get(i - 1)) < tolerance) {
        return i + 1;
      }
    }
    return objectiveHistory.size();
  }

  public static List<ConfidenceInterval> confidenceIntervals(int modelId, double significanceLevel, DSLContext ctx)
    throws BadRequestException, IllegalOperationException, ResourceNotFoundException {
    if (!(significanceLevel > 0 && significanceLevel < 1)) {
      throw new BadRequestException(String.format(
        "Can't make linear model confidence intervals for Transformer %d because significance level %.4f " +
          "must be between 0 and 1 (exclusive both sides)",
        modelId,
        significanceLevel
      ));
    }
    if (!TransformerDao.exists(modelId, ctx)) {
      throw new ResourceNotFoundException(String.format(
        "Can't make linear model confidence intervals for Transformer %d because it doesn't exist",
        modelId
      ));
    }

    // If the model isn't linear, return an error.
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

    int df = numRows - coeffStderrPairsForIndex.size() - ((coeffStderrPairsForIndex.containsKey(0)) ? 1 : 0);
    TDistribution dist = new TDistributionImpl(df);

    return coeffStderrPairsForIndex
      .entrySet()
      .stream()
      .map(pair -> {
        int featureIndex = pair.getKey();
        double coeff = pair.getValue().getKey();
        double stderr = pair.getValue().getValue();
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
      .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

    return modelIds
      .stream()
      .map(id -> iterationsForModel.getOrDefault(id, -1))
      .collect(Collectors.toList());
  }

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
