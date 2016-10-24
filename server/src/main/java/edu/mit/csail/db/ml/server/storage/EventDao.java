package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.tables.records.EventRecord;
import org.jooq.DSLContext;

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
}
