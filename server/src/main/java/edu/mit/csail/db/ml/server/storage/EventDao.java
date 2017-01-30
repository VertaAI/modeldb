package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.EventRecord;
import modeldb.ResourceNotFoundException;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record1;

import java.util.Arrays;

import static jooq.sqlite.gen.Tables.EVENT;

/**
 * Contains logic for storing and reading Events (i.e. entries in the Event table). For most of the events that
 * ModelDB supports (e.g. CrossValidationEvent, FitEvent), we store entries in the Event table.
 */
public class EventDao {
  /**
   * Store an entry in the Event table.
   * @param eventId - The ID of the underlying event. For example, if we've created a FitEvent with ID = 1234, then we
   *                need to store an entry in the the Event table corresponding to the FitEvent. Therefore, we should
   *                pass eventId = 1234 to this function.
   * @param eventType - The type of the underlying event. For example, this should be "fit" or "pipelineFit" for
   *                  FitEvents, "transform" for TransformEvents, etc.
   * @param experimentRunId - The ID of the experiment run that contains the underlying event.
   * @param ctx - The database context.
   * @return The row of the Event table.
   */
  public static EventRecord store(int eventId, String eventType, int experimentRunId, DSLContext ctx) {
    EventRecord ev = ctx.newRecord(EVENT);
    ev.setId(null);
    ev.setExperimentrun(experimentRunId);
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
   * @throws ResourceNotFoundException - Thrown if there's no entry in the Event table that has the given underlying
   * event ID and event type.
   */
  public static int getEventId(int specificEventId, String eventType, DSLContext ctx) throws ResourceNotFoundException {
    // Create the WHERE condition of the SQL statement by checking whether the event type is "fit".
    Condition whereCond = eventType.equals("fit") ?
      Tables.EVENT.EVENTTYPE.in(Arrays.asList("fit", "pipeline fit")) :
      Tables.EVENT.EVENTTYPE.eq(eventType);
    whereCond = whereCond.and(Tables.EVENT.EVENTID.eq(specificEventId));

    // Run the query.
    Record1<Integer> rec = ctx.select(Tables.EVENT.ID).from(Tables.EVENT).where(whereCond).fetchOne();

    // Throw an exception if no row is found.
    if (rec == null) {
      throw new ResourceNotFoundException(
        String.format("Could not find Event for event type %s and ID %s", eventType, specificEventId)
      );
    }

    // Return the primary key (i.e. ID) of the row in the Event table.
    return rec.value1();
  }
}
