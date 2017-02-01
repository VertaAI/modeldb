package edu.mit.csail.db.ml.server.algorithm.similarity.comparators;

import edu.mit.csail.db.ml.server.algorithm.similarity.ModelComparator;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.LinearmodelRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.TableField;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This abstract class makes it possible to find similar linear models. Similarity is measured by comparing the
 * absolute value difference between a given field (from getComparisonField) of the linear models. Subclasses should
 * implement the getComparisonField method.
 */
public abstract class LinearModelComparator implements ModelComparator {
  /**
   * Subclasses should implement this.
   * @return The field of the LinearModel table that will be used for comparison.
   */
  public abstract TableField<LinearmodelRecord, Double> getComparisonField();

  /**
   * @param modelId - We seek models that are similar to the model with the given ID.
   * @param similarModels - If this is an empty list, then we will read the database and
   *                      find similar models there. Otherwise, the given list of model IDs
   *                      will be re-ordered such that the most similar model is first and
   *                      the least similar model will be last.
   * @param limit - The maximum number of model IDs to return.
   * @param ctx - The context for interacting with the database.
   * @return Similar models are the ones whose getComparisonField() value is closest to the corresponding value
   * of the model with ID modelID.
   */
  @Override
  public List<Integer> similarModels(int modelId, List<Integer> similarModels, int limit, DSLContext ctx) {
    // Fetch the compareVal for the model.
    Record1<Double> rec = ctx
      .select(getComparisonField())
      .from(Tables.LINEARMODEL)
      .where(Tables.LINEARMODEL.MODEL.eq(modelId).and(getComparisonField().isNotNull()))
      .fetchOne();
    if (rec == null) {
      return similarModels;
    }
    double compareVal = rec.value1();

    // Apply the correct WHERE condition.
    Condition whereCondition = getComparisonField().isNotNull();
    if (!similarModels.isEmpty()) {
      whereCondition = whereCondition.and(Tables.LINEARMODEL.MODEL.in(similarModels));
    }

    // Filter down to models whose compareVal values are close to the given model.
    return merge(
      ctx
        .select(Tables.LINEARMODEL.MODEL)
        .from(Tables.LINEARMODEL)
        .where(whereCondition)
        .orderBy(getComparisonField().sub(compareVal).abs().asc())
        .limit(limit)
        .stream()
        .distinct()
        .map(Record1::value1)
        .collect(Collectors.toList()),
      similarModels
    );
  }
}
