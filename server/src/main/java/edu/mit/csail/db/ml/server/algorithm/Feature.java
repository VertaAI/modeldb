package edu.mit.csail.db.ml.server.algorithm;

import edu.mit.csail.db.ml.server.storage.FitEventDao;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.TransformeventRecord;
import modeldb.CompareFeaturesResponse;
import modeldb.ResourceNotFoundException;
import org.jooq.DSLContext;
import org.jooq.Record1;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class exposes methods to operate on the feature-sets of models.
 */
public class Feature {
  /**
   * Find all models that use any of the given features.
   * @param featureNames - A set of feature names.
   * @param ctx - The database context.
   * @return Lists of IDs of models that have at least one feature (in Feature table) that is present in featureNames.
   */
  public static List<Integer> modelsWithFeatures(List<String> featureNames, DSLContext ctx) {
    // De-duplicate by making a set.
    Set<String> featureSet = new HashSet<>(featureNames);

    // Find the models by finding all the rows of Feature in
    // the feature set and return the models that contain every single feature in the feature set.
    return ctx
      .select(Tables.FEATURE.TRANSFORMER)
      .from(Tables.FEATURE)
      .where(Tables.FEATURE.NAME.in(featureSet))
      .groupBy(Tables.FEATURE.TRANSFORMER)
      .having(Tables.FEATURE.NAME.count().eq(featureSet.size()))
      .stream()
      .map(Record1::value1)
      .collect(Collectors.toList());
  }

  /**
   * Find the original set of features that produced the feature-set used by the model with the given ID.
   * @param modelId - The ID of the model whose original features we seek.
   * @param ctx - The database context.
   * @return The list of original feature names. For example, suppose we begin with a DataFrame that has a column "age"
   * and do a TransformEvent to produce a DataFrame with column "ageInDays". Then, suppose we train a model on the
   * "ageInDays" column. Then, the original feature-set of the model is simply "age".
   */
  public static List<String> originalFeatures(int modelId, DSLContext ctx) throws ResourceNotFoundException {
    // This reads everything from the TransformEvent table. If this proves to be a performance bottleneck. We can
    // use one of the following optimizations:
    // 1. Only query TransformEvents in the same project as the modelId.
    // 2. Make TransformEvents keep track of the entire ancestry of DataFrames, rather than just the parent DataFrame.

    // Create a mapping from DataFrame ID to the TransformEvent that produced it.
    Map<Integer, TransformeventRecord> transformRecordForDf = ctx
      .selectFrom(Tables.TRANSFORMEVENT)
      .fetch()
      .stream()
      .collect(Collectors.toMap(TransformeventRecord::getNewdf, r -> r));

    // Find the DataFrame that produced the given model.
    int df = FitEventDao.getParentDfId(modelId, ctx);

    // Walk up the ancestry chain of TransformEvents. For each TransformEvent, remove the output
    // columns from the feature set and add in the input columns.
    Set<String> features = new HashSet<>();
    TransformeventRecord te;
    while (transformRecordForDf.containsKey(df)) {
      te = transformRecordForDf.get(df);
      Stream.of(te.getOutputcolumns().split(",")).forEach(features::remove);
      Stream.of(te.getInputcolumns().split(",")).forEach(features::add);
      df = te.getOlddf();
    }

    // Filter out empty strings and return.
    return features.stream().filter(f -> !f.equals("")).collect(Collectors.toList());
  }

  /**
   * Compare the feature-sets of two models.
   * @param modelId1 - The ID of the first model.
   * @param modelId2 - The ID of the second model.
   * @param ctx - The database context.
   * @return A comparison (i.e. common features, features only in model 1, features only in model 2) of the feature-sets
   * of the two given models.
   */
  public static CompareFeaturesResponse compareFeatures(int modelId1, int modelId2, DSLContext ctx)
    throws ResourceNotFoundException {
    String ERROR_FORMAT = "Could not find features for Transformer %d - the Transformer may not exist or it may " +
      "not have any features.";
    // Fetch the features associated with the models.
    Set<String> features1 = ctx
      .select(Tables.FEATURE.NAME)
      .from(Tables.FEATURE)
      .where(Tables.FEATURE.TRANSFORMER.eq(modelId1))
      .orderBy(Tables.FEATURE.FEATUREINDEX.asc())
      .fetch()
      .map(Record1::value1)
      .stream()
      .collect(Collectors.toSet());
    if (features1.isEmpty()) {
      throw new ResourceNotFoundException(String.format(ERROR_FORMAT, modelId1));
    }

    Set<String> features2 = ctx
      .select(Tables.FEATURE.NAME)
      .from(Tables.FEATURE)
      .where(Tables.FEATURE.TRANSFORMER.eq(modelId2))
      .orderBy(Tables.FEATURE.FEATUREINDEX.asc())
      .fetch()
      .map(Record1::value1)
      .stream()
      .collect(Collectors.toSet());

    if (features2.isEmpty()) {
      throw new ResourceNotFoundException(String.format(ERROR_FORMAT, modelId2));
    }

    // Compute the common features.
    Set<String> commonFeatures = new HashSet<>(features1);
    commonFeatures.retainAll(features2);

    features1.removeAll(commonFeatures);
    features2.removeAll(commonFeatures);

    return new CompareFeaturesResponse(
      new ArrayList<>(features1),
      new ArrayList<>(features2),
      new ArrayList<>(commonFeatures)
    );
  }
}
