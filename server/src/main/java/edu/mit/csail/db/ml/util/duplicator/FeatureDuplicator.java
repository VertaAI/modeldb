package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.FeatureRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep4;
import org.jooq.InsertValuesStep5;
import org.jooq.Query;

public class FeatureDuplicator extends Duplicator<FeatureRecord> {
//  id INTEGER PRIMARY KEY AUTOINCREMENT,
//  name TEXT NOT NULL,
//  featureIndex INTEGER NOT NULL,
//  importance DOUBLE NOT NULL,
//  transformer INTEGER REFERENCES TRANSFORMER

  InsertValuesStep5<FeatureRecord, Integer, String, Integer, Double, Integer> query;

  public FeatureDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.FEATURE).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.FEATURE,
      Tables.FEATURE.ID,
      Tables.FEATURE.NAME,
      Tables.FEATURE.FEATUREINDEX,
      Tables.FEATURE.IMPORTANCE,
      Tables.FEATURE.TRANSFORMER
    );
  }

  @Override
  public void updateQuery(FeatureRecord rec, int iteration) {
    query = query.values(
      maxId,
      rec.getName(),
      rec.getFeatureindex(),
      rec.getImportance(),
      TransformerDuplicator.getInstance(ctx).id(rec.getTransformer(), iteration)
    );
  }

  private static FeatureDuplicator instance = null;
  public static FeatureDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new FeatureDuplicator(ctx);
    }
    return instance;
  }
}

