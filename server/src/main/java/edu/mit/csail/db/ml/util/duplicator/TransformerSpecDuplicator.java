package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.TransformerspecRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep4;
import org.jooq.Query;

public class TransformerSpecDuplicator extends Duplicator<TransformerspecRecord> {
  InsertValuesStep4<TransformerspecRecord, Integer, String, String, Integer> query;
//  id INTEGER PRIMARY KEY AUTOINCREMENT,
//    -- The kind of Transformer that this spec describes (e.g. linear regression)
//  transformerType TEXT NOT NULL,
//  -- User assigned content about this spec
//  tag TEXT,
//  -- The experiment run in which this spec is contained
//  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL

  public TransformerSpecDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.TRANSFORMERSPEC).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  protected void resetQuery() {
    query = ctx.insertInto(
      Tables.TRANSFORMERSPEC,
      Tables.TRANSFORMERSPEC.ID,
      Tables.TRANSFORMERSPEC.TRANSFORMERTYPE,
      Tables.TRANSFORMERSPEC.TAG,
      Tables.TRANSFORMERSPEC.EXPERIMENTRUN
    );
  }

  @Override
  protected void updateQuery(TransformerspecRecord rec, int iteration) {
    query = query.values(
      maxId,
      rec.getTransformertype(),
      rec.getTag(),
      ExperimentRunDuplicator.getInstance(ctx).id(rec.getExperimentrun(), iteration)
    );
  }

  private static TransformerSpecDuplicator instance = null;
  public static TransformerSpecDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new TransformerSpecDuplicator(ctx);
    }
    return instance;
  }
}
