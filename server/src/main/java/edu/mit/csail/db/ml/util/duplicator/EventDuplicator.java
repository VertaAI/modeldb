
package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.EventRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep4;
import org.jooq.Query;

public class EventDuplicator extends Duplicator<EventRecord> {
  InsertValuesStep4<EventRecord, Integer, String, Integer, Integer> query;

  public EventDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.EVENT).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.EVENT,
      Tables.EVENT.ID,
      Tables.EVENT.EVENTTYPE,
      Tables.EVENT.EVENTID,
      Tables.EVENT.EXPERIMENTRUN
    );
  }

  private int updatedEventId(String eventType, int eventId, int iteration) {
    switch (eventType) {
      case "fit":
        return FitEventDuplicator.getInstance(ctx).id(eventId, iteration);
      case "pipeline fit":
        return FitEventDuplicator.getInstance(ctx).id(eventId, iteration);
      case "transform":
        return TransformEventDuplicator.getInstance(ctx).id(eventId, iteration);
      case "metric":
        return MetricEventDuplicator.getInstance(ctx).id(eventId, iteration);
      case "random split":
        return RandomSplitEventDuplicator.getInstance(ctx).id(eventId, iteration);
      case "cross validation grid search":
        return GridSearchCrossValidationEventDuplicator.getInstance(ctx).id(eventId, iteration);
      case "cross validation":
        return CrossValidationEventDuplicator.getInstance(ctx).id(eventId, iteration);
      default:
        throw new IllegalArgumentException("We don't support duplication of this event type: " + eventType);
    }
  }

  @Override
  public void updateQuery(EventRecord rec, int iteration) {
    query = query.values(
      maxId,
      rec.getEventtype(),
      updatedEventId(rec.getEventtype(), rec.getEventid(), iteration),
      ExperimentRunDuplicator.getInstance(ctx).id(rec.getExperimentrun(), iteration)
    );
  }

  private static EventDuplicator instance = null;
  public static EventDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new EventDuplicator(ctx);
    }
    return instance;
  }
}

