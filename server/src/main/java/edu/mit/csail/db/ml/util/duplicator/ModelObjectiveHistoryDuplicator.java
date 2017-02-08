
package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.ModelobjectivehistoryRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep4;
import org.jooq.Query;

public class ModelObjectiveHistoryDuplicator extends Duplicator<ModelobjectivehistoryRecord> {
  InsertValuesStep4<ModelobjectivehistoryRecord, Integer, Integer, Integer, Double> query;

  public ModelObjectiveHistoryDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.MODELOBJECTIVEHISTORY).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.MODELOBJECTIVEHISTORY,
      Tables.MODELOBJECTIVEHISTORY.ID,
      Tables.MODELOBJECTIVEHISTORY.MODEL,
      Tables.MODELOBJECTIVEHISTORY.ITERATION,
      Tables.MODELOBJECTIVEHISTORY.OBJECTIVEVALUE
    );
  }

  @Override
  public void updateQuery(ModelobjectivehistoryRecord rec, int iteration) {
    query = query.values(
      maxId,
      TransformerDuplicator.getInstance(ctx).id(rec.getModel(), iteration),
      rec.getIteration(),
      rec.getObjectivevalue()
    );
  }

  private static ModelObjectiveHistoryDuplicator instance = null;
  public static ModelObjectiveHistoryDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new ModelObjectiveHistoryDuplicator(ctx);
    }
    return instance;
  }
}

