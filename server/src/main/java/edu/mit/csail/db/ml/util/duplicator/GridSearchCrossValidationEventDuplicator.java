
package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.GridsearchcrossvalidationeventRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep4;
import org.jooq.Query;

public class GridSearchCrossValidationEventDuplicator extends Duplicator<GridsearchcrossvalidationeventRecord> {
  InsertValuesStep4<GridsearchcrossvalidationeventRecord, Integer, Integer, Integer, Integer> query;

  public GridSearchCrossValidationEventDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.GRIDSEARCHCROSSVALIDATIONEVENT).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.GRIDSEARCHCROSSVALIDATIONEVENT,
      Tables.GRIDSEARCHCROSSVALIDATIONEVENT.ID,
      Tables.GRIDSEARCHCROSSVALIDATIONEVENT.NUMFOLDS,
      Tables.GRIDSEARCHCROSSVALIDATIONEVENT.BEST,
      Tables.GRIDSEARCHCROSSVALIDATIONEVENT.EXPERIMENTRUN
    );
  }

  @Override
  public void updateQuery(GridsearchcrossvalidationeventRecord rec, int iteration) {
    query = query.values(
      maxId,
      rec.getNumfolds(),
      FitEventDuplicator.getInstance(ctx).id(rec.getBest(), iteration),
      ExperimentRunDuplicator.getInstance(ctx).id(rec.getExperimentrun(), iteration)
    );
  }

  private static GridSearchCrossValidationEventDuplicator instance = null;
  public static GridSearchCrossValidationEventDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new GridSearchCrossValidationEventDuplicator(ctx);
    }
    return instance;
  }
}

