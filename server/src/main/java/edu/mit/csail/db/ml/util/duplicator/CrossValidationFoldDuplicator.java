
package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.CrossvalidationfoldRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep4;
import org.jooq.Query;

public class CrossValidationFoldDuplicator extends Duplicator<CrossvalidationfoldRecord> {
  InsertValuesStep4<CrossvalidationfoldRecord, Integer, Integer, Integer, Integer> query;

  public CrossValidationFoldDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.CROSSVALIDATIONFOLD).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.CROSSVALIDATIONFOLD,
      Tables.CROSSVALIDATIONFOLD.ID,
      Tables.CROSSVALIDATIONFOLD.METRIC,
      Tables.CROSSVALIDATIONFOLD.EVENT,
      Tables.CROSSVALIDATIONFOLD.EXPERIMENTRUN
    );
  }

  @Override
  public void updateQuery(CrossvalidationfoldRecord rec, int iteration) {
    query = query.values(
      maxId,
      MetricEventDuplicator.getInstance(ctx).id(rec.getMetric(), iteration),
      CrossValidationEventDuplicator.getInstance(ctx).id(rec.getEvent(), iteration),
      ExperimentRunDuplicator.getInstance(ctx).id(rec.getExperimentrun(), iteration)
    );
  }

  private static CrossValidationFoldDuplicator instance = null;
  public static CrossValidationFoldDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new CrossValidationFoldDuplicator(ctx);
    }
    return instance;
  }
}

