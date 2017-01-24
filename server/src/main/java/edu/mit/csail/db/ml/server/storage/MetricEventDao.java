package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.DataframeRecord;
import jooq.sqlite.gen.tables.records.EventRecord;
import jooq.sqlite.gen.tables.records.MetriceventRecord;
import jooq.sqlite.gen.tables.records.TransformerRecord;
import modeldb.MetricEvent;
import modeldb.MetricEventResponse;
import org.jooq.DSLContext;

/**
 * This class contains logic for storing and reading metric events.
 */
public class MetricEventDao {
  /**
   * Store a MetricEvent in the database.
   * @param me - The metric event.
   * @param ctx - The database context.
   * @return A response indicating that the MetricEvent has been stored.
   */
  public static MetricEventResponse store(MetricEvent me, DSLContext ctx) {
    // Store the DataFrame being evaluated.
    DataframeRecord df = DataFrameDao.store(me.df, me.experimentRunId, ctx);

    // Store the Transformer being evaluated.
    TransformerRecord t = TransformerDao.store(me.model, me.experimentRunId, ctx);

    // Store an entry in the MetricEvent table.
    MetriceventRecord meRec = ctx.newRecord(Tables.METRICEVENT);
    meRec.setId(null);
    meRec.setTransformer(t.getId());
    meRec.setDf(df.getId());
    meRec.setMetrictype(me.metricType);
    meRec.setMetricvalue(Double.valueOf(me.metricValue).floatValue());
    meRec.setExperimentrun(me.experimentRunId);
    meRec.store();

    // Store an entry in the Event table.
    EventRecord ev = EventDao.store(meRec.getId(), "metric", me.experimentRunId, ctx);

    return new MetricEventResponse(t.getId(), df.getId(), ev.getId(), meRec.getId());
  }
}
