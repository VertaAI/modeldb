package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.ExperimentrunRecord;
import modeldb.ExperimentRun;
import modeldb.ExperimentRunEvent;
import modeldb.ExperimentRunEventResponse;
import org.jooq.DSLContext;
import org.jooq.Record1;

import java.sql.Timestamp;
import java.util.Date;

public class ExperimentRunDao {
    // TODO: should we do a check here if experiment run exists?
  public static ExperimentRunEventResponse store(ExperimentRunEvent erun, DSLContext ctx) {
    ExperimentRun er = erun.experimentRun;
    ExperimentrunRecord erRec = ctx.newRecord(Tables.EXPERIMENTRUN);
    erRec.setId(er.id < 0 ? null : er.id);
    erRec.setExperiment(er.experimentId);
    erRec.setCreated(new Timestamp((new Date()).getTime()));
    erRec.store();
    return new ExperimentRunEventResponse(erRec.getId());
  }

  public static int getProjectId(int eRunId, DSLContext ctx) {
    Record1<Integer> rec = ctx.select(Tables.EXPERIMENT_RUN_VIEW.PROJECTID)
      .from(Tables.EXPERIMENT_RUN_VIEW)
      .where(Tables.EXPERIMENT_RUN_VIEW.EXPERIMENTRUNID.eq(eRunId))
      .fetchOne();
    if (rec == null) {
      return -1;
    }
    return rec.value1();
  }
}
