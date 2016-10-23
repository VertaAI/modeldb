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
    // TODO: rewrite as a join
    // find experiment from the experiment run
    Record1<Integer> rec = ctx.select(Tables.EXPERIMENTRUN.EXPERIMENT)
        .from(Tables.EXPERIMENTRUN)
        .where(Tables.EXPERIMENTRUN.ID.eq(eRunId))
        .fetchOne();

    if (rec == null) {
      return -1;
    }
    int exptId = rec.value1();


    // find project from experiment
    rec = ctx.select(Tables.EXPERIMENT.PROJECT)
        .from(Tables.EXPERIMENT)
        .where(Tables.EXPERIMENT.ID.eq(exptId))
        .fetchOne();
    if (rec == null) {
      return -1;
    }
    int projId = rec.value1();

    return projId;
  }
}
