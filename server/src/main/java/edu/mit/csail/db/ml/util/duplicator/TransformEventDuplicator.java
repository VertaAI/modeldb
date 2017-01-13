package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.TransformeventRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep7;
import org.jooq.Query;

public class TransformEventDuplicator extends Duplicator<TransformeventRecord> {
  InsertValuesStep7<TransformeventRecord, Integer, Integer, Integer, Integer, String, String, Integer> query;

  public TransformEventDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.TRANSFORMEVENT).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.TRANSFORMEVENT,
      Tables.TRANSFORMEVENT.ID,
      Tables.TRANSFORMEVENT.OLDDF,
      Tables.TRANSFORMEVENT.NEWDF,
      Tables.TRANSFORMEVENT.TRANSFORMER,
      Tables.TRANSFORMEVENT.INPUTCOLUMNS,
      Tables.TRANSFORMEVENT.OUTPUTCOLUMNS,
      Tables.TRANSFORMEVENT.EXPERIMENTRUN
    );
  }

  @Override
  protected void updateQuery(TransformeventRecord rec, int iteration) {
    query = query.values(
      maxId,
      DataFrameDuplicator.getInstance(ctx).id(rec.getOlddf(), iteration),
      DataFrameDuplicator.getInstance(ctx).id(rec.getNewdf(), iteration),
      TransformerDuplicator.getInstance(ctx).id(rec.getTransformer(), iteration),
      rec.getInputcolumns(),
      rec.getOutputcolumns(),
      ExperimentRunDuplicator.getInstance(ctx).id(rec.getExperimentrun(), iteration)
    );
  }

  private static TransformEventDuplicator instance = null;
  public static TransformEventDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new TransformEventDuplicator(ctx);
    }
    return instance;
  }
}
