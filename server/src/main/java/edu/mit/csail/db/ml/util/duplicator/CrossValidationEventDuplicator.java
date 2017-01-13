
package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.CrossvalidationeventRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep4;
import org.jooq.InsertValuesStep7;
import org.jooq.Query;

public class CrossValidationEventDuplicator extends Duplicator<CrossvalidationeventRecord> {
  InsertValuesStep7<CrossvalidationeventRecord, Integer, Integer, Integer, Integer, Long, String, Integer> query;

  public CrossValidationEventDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.CROSSVALIDATIONEVENT).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.CROSSVALIDATIONEVENT,
      Tables.CROSSVALIDATIONEVENT.ID,
      Tables.CROSSVALIDATIONEVENT.DF,
      Tables.CROSSVALIDATIONEVENT.SPEC,
      Tables.CROSSVALIDATIONEVENT.NUMFOLDS,
      Tables.CROSSVALIDATIONEVENT.RANDOMSEED,
      Tables.CROSSVALIDATIONEVENT.EVALUATOR,
      Tables.CROSSVALIDATIONEVENT.EXPERIMENTRUN
    );
  }

  @Override
  public void updateQuery(CrossvalidationeventRecord rec, int iteration) {
    query = query.values(
      maxId,
      DataFrameDuplicator.getInstance(ctx).id(rec.getDf(), iteration),
      TransformerSpecDuplicator.getInstance(ctx).id(rec.getSpec(), iteration),
      rec.getNumfolds(),
      rec.getRandomseed(),
      rec.getEvaluator(),
      ExperimentRunDuplicator.getInstance(ctx).id(rec.getExperimentrun(), iteration)
    );
  }

  private static CrossValidationEventDuplicator instance = null;
  public static CrossValidationEventDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new CrossValidationEventDuplicator(ctx);
    }
    return instance;
  }
}

