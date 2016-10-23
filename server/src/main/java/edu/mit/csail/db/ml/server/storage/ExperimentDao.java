package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.ExperimentRecord;
import modeldb.Experiment;
import modeldb.ExperimentEvent;
import modeldb.ExperimentEventResponse;
import org.jooq.DSLContext;

import java.sql.Timestamp;
import java.util.Date;

public class ExperimentDao {
  public static ExperimentEventResponse store(ExperimentEvent expt, DSLContext ctx) {
    Experiment e = expt.experiment;
    if (e.isDefault) {
      int defaultExperiment = ProjectDao.getDefaultExperiment(e.projectId, ctx);
      System.out.println("defaultExpt:" + defaultExperiment);
      return new ExperimentEventResponse(defaultExperiment);
    }

    ExperimentRecord eRec = ctx.selectFrom(Tables.EXPERIMENT).where(
      Tables.EXPERIMENT.PROJECT.eq(e.projectId).and(
        Tables.EXPERIMENT.NAME.eq(e.name))
    ).fetchOne();
    if (eRec != null) {
      return new ExperimentEventResponse(eRec.getId());
    }

    eRec = ctx.newRecord(Tables.EXPERIMENT);
    eRec.setId(e.id < 0 ? null : e.id);
    eRec.setName(e.name);
    eRec.setDescription(e.description);
    eRec.setProject(e.projectId);
    eRec.setCreated(new Timestamp((new Date()).getTime()));
    eRec.store();
    return new ExperimentEventResponse(eRec.getId());
  }
}
