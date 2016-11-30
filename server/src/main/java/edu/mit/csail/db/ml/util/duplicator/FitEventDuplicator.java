package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.FiteventRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep8;
import org.jooq.Query;

public class FitEventDuplicator extends Duplicator<FiteventRecord> {
  InsertValuesStep8<FiteventRecord, Integer, Integer, Integer, Integer, String, String, Integer, String> query;

  public FitEventDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.FITEVENT).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.FITEVENT,
      Tables.FITEVENT.ID,
      Tables.FITEVENT.TRANSFORMERSPEC,
      Tables.FITEVENT.TRANSFORMER,
      Tables.FITEVENT.DF,
      Tables.FITEVENT.PREDICTIONCOLUMNS,
      Tables.FITEVENT.LABELCOLUMNS,
      Tables.FITEVENT.EXPERIMENTRUN,
      Tables.FITEVENT.PROBLEMTYPE
    );
  }

  @Override
  public void updateQuery(FiteventRecord rec, int iteration) {
    query = query.values(
      maxId,
      TransformerSpecDuplicator.getInstance(ctx).id(rec.getTransformerspec(), iteration),
      TransformerDuplicator.getInstance(ctx).id(rec.getTransformer(), iteration),
      DataFrameDuplicator.getInstance(ctx).id(rec.getDf(), iteration),
      rec.getPredictioncolumns(),
      rec.getLabelcolumns(),
      ExperimentRunDuplicator.getInstance(ctx).id(rec.getExperimentrun(), iteration),
      rec.getProblemtype()
    );
  }

  private static FitEventDuplicator instance = null;
  public static FitEventDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new FitEventDuplicator(ctx);
    }
    return instance;
  }
}
