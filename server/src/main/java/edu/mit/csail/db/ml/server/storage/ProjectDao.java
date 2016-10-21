package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.ProjectRecord;
import jooq.sqlite.gen.tables.records.ExperimentRecord;
import modeldb.Project;
import modeldb.ProjectEvent;
import modeldb.ProjectEventResponse;
import org.jooq.DSLContext;
import org.jooq.Record1;

import java.sql.Timestamp;
import java.util.Date;

public class ProjectDao {
  public static ProjectEventResponse store(ProjectEvent pr, DSLContext ctx) {
    Project p = pr.project;
    ProjectRecord pRec = ctx.selectFrom(Tables.PROJECT).where(
      Tables.PROJECT.NAME.eq(p.name).and(
        Tables.PROJECT.AUTHOR.eq(p.author))
    ).fetchOne();
    if (pRec != null) {
      return new ProjectEventResponse(pRec.getId());
    }

    pRec = ctx.newRecord(Tables.PROJECT);
    pRec.setId(p.id < 0 ? null : p.id);
    pRec.setName(p.name);
    pRec.setAuthor(p.author);
    pRec.setDescription(p.description);
    pRec.setCreated(new Timestamp((new Date()).getTime()));
    pRec.store();

    // create a default experiment
    ExperimentRecord eRec = ctx.newRecord(Tables.EXPERIMENT);
    eRec.setProject(pRec.getId());
    eRec.setName("Default_Experiment");
    eRec.setDescription("Default Experiment");
    eRec.setCreated(new Timestamp((new Date()).getTime()));
    eRec.store();

    return new ProjectEventResponse(pRec.getId());
  }

  // TODO: add call to return the default experiment for a project
  public static int getDefaultExperiment(int projId, DSLContext ctx) {
    Record1<Integer> rec = ctx.select(Tables.EXPERIMENT.ID.min())
    .from(Tables.EXPERIMENT)
    .where(Tables.EXPERIMENT.PROJECT.eq(projId))
    .fetchOne();
    if (rec != null) {
        return rec.value1();
    } else {
        return -1;
    }
  }
}
