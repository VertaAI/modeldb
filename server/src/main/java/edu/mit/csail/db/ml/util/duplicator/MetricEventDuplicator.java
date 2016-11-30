package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.MetriceventRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep6;
import org.jooq.Query;

public class MetricEventDuplicator extends Duplicator<MetriceventRecord> {
  InsertValuesStep6<MetriceventRecord, Integer, Integer, Integer, String, Float, Integer> query;

  public MetricEventDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.METRICEVENT).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.METRICEVENT,
      Tables.METRICEVENT.ID,
      Tables.METRICEVENT.TRANSFORMER,
      Tables.METRICEVENT.DF,
      Tables.METRICEVENT.METRICTYPE,
      Tables.METRICEVENT.METRICVALUE,
      Tables.METRICEVENT.EXPERIMENTRUN
    );
  }

  @Override
  public void updateQuery(MetriceventRecord rec, int iteration) {
    query = query.values(
      maxId,
      TransformerDuplicator.getInstance(ctx).id(rec.getTransformer(), iteration),
      DataFrameDuplicator.getInstance(ctx).id(rec.getDf(), iteration),
      rec.getMetrictype(),
      rec.getMetricvalue(),
      ExperimentRunDuplicator.getInstance(ctx).id(rec.getExperimentrun(), iteration)
    );
  }

  private static MetricEventDuplicator instance = null;
  public static MetricEventDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new MetricEventDuplicator(ctx);
    }
    return instance;
  }
}
