package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.ExperimentRecord;
import jooq.sqlite.gen.tables.records.ProjectRecord;
import modeldb.*;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.impl.DSL;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class contains logic for reading and storing projects.
 */
public class ProjectDao {
  /**
   * Store a project in the database.
   * @param pr - The project.
   * @param ctx - The database context.
   * @return A response indicating that the project has been stored.
   */
  public static ProjectEventResponse store(ProjectEvent pr, DSLContext ctx) {
    // TODO: Do we really need to distinguish between ProjectEvent and Project? A ProjectEvent doesn't do anything else
    // besides just storing a Project.
    Project p = pr.project;

    // Check if there already exists a project with the given name and author.
    // If so, then just return that project.
    ProjectRecord pRec = ctx.selectFrom(Tables.PROJECT).where(
      Tables.PROJECT.NAME.eq(p.name).and(
        Tables.PROJECT.AUTHOR.eq(p.author))
    ).fetchOne();
    if (pRec != null) {
      return new ProjectEventResponse(pRec.getId());
    }

    // Otherwise, create a new project and store it in the Project table.
    pRec = ctx.newRecord(Tables.PROJECT);
    pRec.setId(p.id < 0 ? null : p.id);
    pRec.setName(p.name);
    pRec.setAuthor(p.author);
    pRec.setDescription(p.description);
    pRec.setCreated(new Timestamp((new Date()).getTime()));
    pRec.store();

    // Create a default experiment for the project.
    ExperimentRecord eRec = ctx.newRecord(Tables.EXPERIMENT);
    eRec.setId(null);
    eRec.setProject(pRec.getId());
    eRec.setName("Default_Experiment");
    eRec.setDescription("Default Experiment");
    eRec.setCreated(new Timestamp((new Date()).getTime()));
    eRec.store();

    return new ProjectEventResponse(pRec.getId());
  }

  /**
   * Every project begins with a default experiment. This method gets the ID of the default experiment for a given
   * project.
   * @param projId - The ID of a project.
   * @param ctx - The database context.
   * @return The ID of the default experiment in the project with the given ID. If there is no project with ID projId,
   * then -1 will be returned.
   */
  public static int getDefaultExperiment(int projId, DSLContext ctx) {
    Record1<Integer> rec = ctx.select(Tables.EXPERIMENT.ID.min())
    .from(Tables.EXPERIMENT)
    .where(Tables.EXPERIMENT.PROJECT.eq(projId))
    .fetchOne();
    if (rec != null && rec.value1() != null) {
        return rec.value1();
    } else {
        return -1;
    }
  }

  /**
   * Read the project with the given ID.
   * @param projId - The ID of the project.
   * @param ctx - The database context.
   * @return The project with ID projId.
   * @throws ResourceNotFoundException - Thrown if there's no project with ID projId.
   */
  public static Project read(int projId, DSLContext ctx) throws ResourceNotFoundException {
    // Query for the project with the given ID.
    ProjectRecord rec = ctx
      .selectFrom(Tables.PROJECT)
      .where(Tables.PROJECT.ID.eq(projId))
      .fetchOne();

    // Thrown an exception if there's no project with the given ID, otherwise, return the project.
    if (rec == null) {
      throw new ResourceNotFoundException(String.format(
        "Can't find Project with ID %d",
        projId
      ));
    }

    return new Project(rec.getId(), rec.getName(), rec.getAuthor(), rec.getDescription());
  }

  /**
   * Get an overview of all the projects in ModelDB.
   * @param ctx - The database context.
   * @return An overview of all the projects in ModelDB. This includes the project names, IDs, authors, descriptions.
   * It also includes the number of experiments and number of experiment runs in each project.
   */
  public static List<ProjectOverviewResponse> getProjectOverviews(DSLContext ctx) {
    // Create a map that goes from project ID to the corresponding Project object.
    Map<Integer, Project> projectForId = ctx
      .selectFrom(Tables.PROJECT)
      .fetch()
      .map(rec -> new Project(rec.getId(), rec.getName(), rec.getAuthor(), rec.getDescription()))
      .stream()
      .collect(Collectors.toMap(Project::getId, p -> p));

    // Create a map that goes from project ID to the number of experiments in the project.
    Map<Integer, Integer> numExperimentsForProjId = ctx
      .select(Tables.EXPERIMENT.PROJECT, Tables.EXPERIMENT.ID.count())
      .from(Tables.EXPERIMENT)
      .groupBy(Tables.EXPERIMENT.PROJECT)
      .fetch()
      .stream()
      .collect(Collectors.toMap(Record2::value1, Record2::value2));

    // Creates a map that goes from project ID to the number of experiment runs in the project.
    Map<Integer, Integer> numExperimentRunsForProjId  = ctx
      .select(Tables.EXPERIMENT_RUN_VIEW.PROJECTID, Tables.EXPERIMENT_RUN_VIEW.EXPERIMENTID.count())
      .from(Tables.EXPERIMENT_RUN_VIEW)
      .groupBy(Tables.EXPERIMENT_RUN_VIEW.PROJECTID)
      .fetch()
      .stream()
      .collect(Collectors.toMap(Record2::value1, Record2::value2));

    // Construct a response from the above maps and return it.
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

  /**
   * Get the IDs of all the projects that match the specified key-value pairs.
   * @param  keyValuePairs - The map containing key-value pairs to match.
   * @param  ctx - The database context.
   * @return A list of all project IDs that match the given attributes.
   */
  public static List<Integer> getProjectIds(Map<String, String> keyValuePairs, DSLContext ctx) {
    return ctx
          .select(Tables.PROJECT.ID)
          .from(Tables.PROJECT)
          .where(DSL.and(keyValuePairs.keySet()
                                      .stream()
                                      .map(
                                        key -> DSL.field(DSL.name("PROJECT", key))
                                              .eq(keyValuePairs.get(key)))
                                      .collect(Collectors.toList())))
          .fetch()
          .stream()
          .map(rec -> rec.value1())
          .collect(Collectors.toList());
  }

  /**
   * Update the given field of the project of the given ID with the given value.
   * The field must be an existing field of the project.
   * @param  projectId - The ID of the project.
   * @param  key - The key to update.
   * @param  value - The value for the key.
   * @param  ctx - The database context.
   * @return whether field was successfully updated or not
   */
  public static boolean updateProject(int projectId, String key, String value, DSLContext ctx) {
    // TODO: throw some kind of exception when key doesn't exist
    int recordsUpdated = ctx
      .update(Tables.PROJECT)
      .set(DSL.field(DSL.name("PROJECT", key)), value)
      .where(Tables.PROJECT.ID.eq(projectId))
      .execute();
    return recordsUpdated > 0;
  }
}
