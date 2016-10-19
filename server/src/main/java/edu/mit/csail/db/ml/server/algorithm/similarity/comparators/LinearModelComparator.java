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

public abstract class LinearModelComparator implements ModelComparator {
  public abstract TableField<LinearmodelRecord, Double> getComparisonField();

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
