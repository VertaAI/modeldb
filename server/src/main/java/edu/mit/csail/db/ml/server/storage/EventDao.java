package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.EventRecord;
import modeldb.ResourceNotFoundException;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record1;

import java.util.Arrays;

import static jooq.sqlite.gen.Tables.EVENT;

public class EventDao {
  public static EventRecord store(int eventId, String eventType, int experimentId, DSLContext ctx) {
    EventRecord ev = ctx.newRecord(EVENT);
    ev.setId(null);
    ev.setExperimentrun(experimentId);
    ev.setEventtype(eventType);
    ev.setEventid(eventId);
    ev.store();
    return ev;
  }

  /**
   * Get the Event ID associated with the specific (e.g. TransformEvent, FitEvent) given event.
   * @param specificEventId - The ID of the specific kind of event.
   * @param eventType - The type of the event. If you provide "fit" as an argument, this will search for both "fit" and
   *                  "pipelineFit".
   */
  public static int getEventId(int specificEventId, String eventType, DSLContext ctx) throws ResourceNotFoundException {
    Condition whereCond = eventType.equals("fit") ?
      Tables.EVENT.EVENTTYPE.in(Arrays.asList("fit", "pipeline fit")) :
      Tables.EVENT.EVENTTYPE.eq(eventType);
    whereCond = whereCond.and(Tables.EVENT.EVENTID.eq(specificEventId));
    Record1<Integer> rec = ctx.select(Tables.EVENT.ID).from(Tables.EVENT).where(whereCond).fetchOne();

    if (rec == null) {
      throw new ResourceNotFoundException(
        String.format("Could not find Event for event type %s and ID %s", eventType, specificEventId)
      );
    }

    return rec.value1();
  }
}
