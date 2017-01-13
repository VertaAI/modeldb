package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.TransformerRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep5;
import org.jooq.Query;

import java.util.Set;

public class TransformerDuplicator extends Duplicator<TransformerRecord> {
  InsertValuesStep5<TransformerRecord, Integer, String, String, Integer, String> query;

  public TransformerDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.TRANSFORMER).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.TRANSFORMER,
      Tables.TRANSFORMER.ID,
      Tables.TRANSFORMER.TRANSFORMERTYPE,
      Tables.TRANSFORMER.TAG,
      Tables.TRANSFORMER.EXPERIMENTRUN,
      Tables.TRANSFORMER.FILEPATH
    );
  }

  @Override
  public void updateQuery(TransformerRecord rec, int iteration) {
    query = query.values(
      maxId,
      rec.getTransformertype(),
      rec.getTag(),
      ExperimentRunDuplicator.getInstance(ctx).id(rec.getExperimentrun(), iteration),
      rec.getFilepath()
    );
  }

  private static TransformerDuplicator instance = null;
  public static TransformerDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new TransformerDuplicator(ctx);
    }
    return instance;
  }
}
