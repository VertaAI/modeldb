package edu.mit.csail.db.ml.server.storage;

import javafx.util.Pair;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.ProjectRecord;
import jooq.sqlite.gen.tables.records.ExperimentRecord;
import modeldb.*;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Record3;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    eRec.setId(null);
    eRec.setProject(pRec.getId());
    eRec.setName("Default_Experiment");
    eRec.setDescription("Default Experiment");
    eRec.setCreated(new Timestamp((new Date()).getTime()));
    eRec.store();

    return new ProjectEventResponse(pRec.getId());
  }

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

  public static Project read(int projId, DSLContext ctx) throws ResourceNotFoundException {
    ProjectRecord rec = ctx
      .selectFrom(Tables.PROJECT)
      .where(Tables.PROJECT.ID.eq(projId))
      .fetchOne();

    if (rec == null) {
      throw new ResourceNotFoundException(String.format(
        "Can't find Project with ID %d",
        projId
      ));
    }

    return new Project(rec.getId(), rec.getName(), rec.getAuthor(), rec.getDescription());
  }

  public static List<ProjectOverviewResponse> getProjectOverviews(DSLContext ctx) {
    Map<Integer, Project> projectForId = ctx
      .selectFrom(Tables.PROJECT)
      .fetch()
      .map(rec -> new Project(rec.getId(), rec.getName(), rec.getAuthor(), rec.getDescription()))
      .stream()
      .collect(Collectors.toMap(Project::getId, p -> p));

    Map<Integer, Integer> numExperimentsForProjId = ctx
      .select(Tables.EXPERIMENT.PROJECT, Tables.EXPERIMENT.ID.count())
      .from(Tables.EXPERIMENT)
      .groupBy(Tables.EXPERIMENT.PROJECT)
      .fetch()
      .stream()
      .collect(Collectors.toMap(Record2::value1, Record2::value2));


    Map<Integer, Integer> numExperimentRunsForProjId  = ctx
      .select(Tables.EXPERIMENT_RUN_VIEW.PROJECTID, Tables.EXPERIMENT_RUN_VIEW.EXPERIMENTID.count())
      .from(Tables.EXPERIMENT_RUN_VIEW)
      .groupBy(Tables.EXPERIMENT_RUN_VIEW.PROJECTID)
      .fetch()
      .stream()
      .collect(Collectors.toMap(Record2::value1, Record2::value2));

    return projectForId
      .keySet()
      .stream()
      .map(id -> new ProjectOverviewResponse(
        projectForId.get(id),
        numExperimentsForProjId.get(id),
        numExperimentRunsForProjId.get(id)
      ))
      .collect(Collectors.toList());
  }
}
