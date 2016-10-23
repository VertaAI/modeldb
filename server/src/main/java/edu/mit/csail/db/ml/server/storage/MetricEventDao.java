package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.DataframeRecord;
import jooq.sqlite.gen.tables.records.EventRecord;
import jooq.sqlite.gen.tables.records.MetriceventRecord;
import jooq.sqlite.gen.tables.records.TransformerRecord;
import modeldb.MetricEvent;
import modeldb.MetricEventResponse;
import org.jooq.DSLContext;

public class MetricEventDao {
  public static MetricEventResponse store(MetricEvent me, DSLContext ctx) {
    DataframeRecord df = DataFrameDao.store(me.df, me.experimentRunId, ctx);
    TransformerRecord t = TransformerDao.store(me.model, me.experimentRunId, ctx);


    MetriceventRecord meRec = ctx.newRecord(Tables.METRICEVENT);
    meRec.setId(null);
    meRec.setTransformer(t.getId());
    meRec.setDf(df.getId());
    meRec.setMetrictype(me.metricType);
    meRec.setMetricvalue(Double.valueOf(me.metricValue).floatValue());
    meRec.setExperimentrun(me.experimentRunId);
    meRec.store();

    EventRecord ev = EventDao.store(meRec.getId(), "metric", me.experimentRunId, ctx);

    return new MetricEventResponse(t.getId(), df.getId(), ev.getId(), meRec.getId());
  }
}
