
package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.GridcellcrossvalidationRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep4;
import org.jooq.Query;

public class GridCellCrossValidationDuplicator extends Duplicator<GridcellcrossvalidationRecord> {
  InsertValuesStep4<GridcellcrossvalidationRecord, Integer, Integer, Integer, Integer> query;

  public GridCellCrossValidationDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.GRIDCELLCROSSVALIDATION).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.GRIDCELLCROSSVALIDATION,
      Tables.GRIDCELLCROSSVALIDATION.ID,
      Tables.GRIDCELLCROSSVALIDATION.GRIDSEARCH,
      Tables.GRIDCELLCROSSVALIDATION.CROSSVALIDATION,
      Tables.GRIDCELLCROSSVALIDATION.EXPERIMENTRUN
    );
  }

  @Override
  public void updateQuery(GridcellcrossvalidationRecord rec, int iteration) {
    query = query.values(
      maxId,
      GridSearchCrossValidationEventDuplicator.getInstance(ctx).id(rec.getGridsearch(), iteration),
      CrossValidationEventDuplicator.getInstance(ctx).id(rec.getCrossvalidation(), iteration),
      ExperimentRunDuplicator.getInstance(ctx).id(rec.getExperimentrun(), iteration)
    );
  }

  private static GridCellCrossValidationDuplicator instance = null;
  public static GridCellCrossValidationDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new GridCellCrossValidationDuplicator(ctx);
    }
    return instance;
  }
}

