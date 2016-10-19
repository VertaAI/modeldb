package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.ExperimentrunRecord;
import modeldb.ExperimentRun;
import modeldb.ExperimentRunEvent;
import modeldb.ExperimentRunEventResponse;
import org.jooq.DSLContext;

import java.sql.Timestamp;
import java.util.Date;

public class ExperimentRunDao {
  public static ExperimentRunEventResponse store(ExperimentRunEvent erun, DSLContext ctx) {
    ExperimentRun er = erun.experimentrun;
    ExperimentrunRecord erRec = ctx.newRecord(Tables.EXPERIMENTRUN);
    erRec.setId(er.id < 0 ? null : er.id);
    erRec.setDescription(er.description);
    erRec.setProject(er.projectId);
    erRec.setCreated(new Timestamp((new Date()).getTime()));
    erRec.store();
    return new ExperimentRunEventResponse(erRec.getId());
  }
}
