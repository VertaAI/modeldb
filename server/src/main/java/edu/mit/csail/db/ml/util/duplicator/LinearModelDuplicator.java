
package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.LinearmodelRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep5;
import org.jooq.Query;

public class LinearModelDuplicator extends Duplicator<LinearmodelRecord> {
  InsertValuesStep5<LinearmodelRecord, Integer, Integer, Double, Double, Double> query;

  public LinearModelDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.LINEARMODEL).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.LINEARMODEL,
      Tables.LINEARMODEL.ID,
      Tables.LINEARMODEL.MODEL,
      Tables.LINEARMODEL.RMSE,
      Tables.LINEARMODEL.EXPLAINEDVARIANCE,
      Tables.LINEARMODEL.R2
    );
  }

  @Override
  public void updateQuery(LinearmodelRecord rec, int iteration) {
    query = query.values(
      maxId,
      TransformerDuplicator.getInstance(ctx).id(rec.getModel(), iteration),
      rec.getRmse(),
      rec.getExplainedvariance(),
      rec.getR2()
    );
  }

  private static LinearModelDuplicator instance = null;
  public static LinearModelDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new LinearModelDuplicator(ctx);
    }
    return instance;
  }
}