package edu.mit.csail.db.ml.server.storage;

import edu.mit.csail.db.ml.util.Pair;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.ExperimentrunRecord;
import modeldb.*;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Record2;
import edu.mit.csail.db.ml.server.storage.metadata.MetadataDb;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class contains logic for reading and storing experiment runs.
 */
public class ExperimentRunDao {
  /**
   * Store an experiment run in the database.
   * @param erun - The experiment run.
   * @param ctx - The database context.
   * @return A response containing information about the stored experiment run.
   */
  public static ExperimentRunEventResponse store(ExperimentRunEvent erun, DSLContext ctx) {
    // ExperimentRun description is not necessarily unique and therefore we 
    // don't look for it
    // ExperimentRun ID is unique however and so we use that unique id.
    ExperimentRun er = erun.experimentRun;
    ExperimentrunRecord erRec = ctx.newRecord(Tables.EXPERIMENTRUN);
    erRec.setId(er.id < 0 ? null : er.id);
    erRec.setExperiment(er.experimentId);
    erRec.setDescription(er.description);
    erRec.setCreated(new Timestamp((new Date()).getTime()));
    if (er.isSetSha()) {
      erRec.setSha(er.getSha());
    }
    erRec.store();
    return new ExperimentRunEventResponse(erRec.getId());
  }

  /**
   * @param eRunId - The ID of an experiment run.
   * @param ctx - The database context.
   * @return The ID of the project that contains the experiment run with ID eRunId.
   */
  public static int getProjectId(int eRunId, DSLContext ctx) {
    return getExperimentAndProjectIds(eRunId, ctx).getSecond();
  }

  /**
   * Converts a row of the ExperimentRun table into a modeldb.ExperimentRun object.
   * @param erRec - The row of the table.
   * @return The modeldb.ExperimentRun object.
   */
  public static ExperimentRun recordToThrift(ExperimentrunRecord erRec) {
    ExperimentRun er = new ExperimentRun(
      erRec.getId(),
      erRec.getExperiment(),
      erRec.getDescription()
    );
    er.setSha(erRec.getSha());
    er.setCreated(erRec.getCreated().toString());
    return er;
  }

  /**
   * Get the experiment and project that contain the given experiment run.
   * @param eRunId - The ID of an experiment run.
   * @param ctx - The database context.
   * @return A (experimentId, projectId) pair indicating the IDs of the project and experiment that contain
   * the experiment run with ID eRunId. If the ExperimentRun with ID eRunId does not exist, then the pair (-1, -1) is
   * returned.
   */
  public static Pair<Integer, Integer> getExperimentAndProjectIds(int eRunId, DSLContext ctx) {
    // Read the experiment and project.
    Record2<Integer, Integer> rec = ctx
      .select(
        Tables.EXPERIMENT_RUN_VIEW.EXPERIMENTID,
        Tables.EXPERIMENT_RUN_VIEW.PROJECTID
      )
      .from(Tables.EXPERIMENT_RUN_VIEW)
      .where(Tables.EXPERIMENT_RUN_VIEW.EXPERIMENTRUNID.eq(eRunId))
      .fetchOne();
    if (rec == null) {
      // TODO: Should we throw a ResourceNotFoundException here instead?
      return new Pair<>(-1, -1);
    }
    return new Pair<>(rec.value1(), rec.value2());
  }

  /**
   * Verifies that there exists an experiment run in the ExperimentRun table with the given ID.
   * @param id - The ID of an experiment run.
   * @param ctx - The database context.
   * @throws InvalidExperimentRunException - Thrown if there is no ExperimentRun with the given ID.
   * No exception is thrown otherwise.
   */
  public static void validateExperimentRunId(int id, DSLContext ctx) throws InvalidExperimentRunException {
    if (ctx.selectFrom(Tables.EXPERIMENTRUN).where(Tables.EXPERIMENTRUN.ID.eq(id)).fetchOne() == null) {
      throw new InvalidExperimentRunException(String.format("Can't find experiment run ID %d", id));
    }
  }

  /**
   * Read the ExperimentRun with the given ID.
   * @param experimentRunId - The ID of an experiment run.
   * @param ctx - The database context.
   * @return The ExperimentRun with ID experimentRunId.
   * @throws ResourceNotFoundException - Thrown if there is no ExperimentRun with ID experimentRunId.
   */
  public static ExperimentRun read(int experimentRunId, DSLContext ctx) throws ResourceNotFoundException {
    ExperimentrunRecord rec = ctx
      .selectFrom(Tables.EXPERIMENTRUN)
      .where(Tables.EXPERIMENTRUN.ID.eq(experimentRunId))
      .fetchOne();
    if (rec == null) {
      throw new ResourceNotFoundException(String.format(
        "Can't find ExperimentRun with ID %d",
        experimentRunId
      ));
    }
    return recordToThrift(rec);
  }

  /**
   * Read all the experiments and experiment runs in a given project.
   * @param projId - The ID of a project.
   * @param ctx - The database context.
   * @return The experiment runs (with their corresponding experiments) in the project with ID projId.
   */
  public static ProjectExperimentsAndRuns readExperimentsAndRunsInProject(int projId, DSLContext ctx) {
    // Get all the (experiment run ID, experiment ID) pairs in the project.
    List<Pair<Integer, Integer>> runExpPairs = ctx
      .select(
        Tables.EXPERIMENT_RUN_VIEW.EXPERIMENTRUNID, 
        Tables.EXPERIMENT_RUN_VIEW.EXPERIMENTID
      )
      .from(Tables.EXPERIMENT_RUN_VIEW)
      .where(Tables.EXPERIMENT_RUN_VIEW.PROJECTID.eq(projId))
      .fetch()
      .map(r -> new Pair<>(r.value1(), r.value2()));

    // Fetch all the experiments in the project.
    List<Integer> experimentIds = runExpPairs.stream()
      .map(Pair::getSecond).collect(Collectors.toList());
    int defaultExp = experimentIds.stream()
      .mapToInt(s -> s.intValue()).min().orElse(0);
    List<Experiment> experiments = ctx
      .selectFrom(Tables.EXPERIMENT)
      .where(Tables.EXPERIMENT.ID.in(experimentIds))
      .orderBy(Tables.EXPERIMENT.ID.asc())
      .fetch()
      .map(rec -> new Experiment(rec.getId(), projId, rec.getName(), 
        rec.getDescription(), rec.getId() == defaultExp));

    // Fetch all the experiment runs in the project.
    List<Integer> experimentRunIds = runExpPairs.stream().map(Pair::getFirst)
      .collect(Collectors.toList());
    List<ExperimentRun> experimentRuns = ctx
      .selectFrom(Tables.EXPERIMENTRUN)
      .where(Tables.EXPERIMENTRUN.ID.in(experimentRunIds))
      .orderBy(Tables.EXPERIMENTRUN.ID.asc())
      .fetch()
      .map(rec -> recordToThrift(rec));

    return new ProjectExperimentsAndRuns(projId, experiments, experimentRuns);
  }

  /**
   * Get all the experiment runs in a given experiment.
   * @param experimentId - The ID of an experiment.
   * @param ctx - The database context.
   * @return The experiment runs inside the Experiment with ID experimentId.
   */
  public static List<ExperimentRun> readExperimentRunsInExperiment(int experimentId, DSLContext ctx) {
    // Get the IDs of all the experiment runs in the given experiment.
    List<Integer> experimentRunIds = ctx
      .select(Tables.EXPERIMENT_RUN_VIEW.EXPERIMENTRUNID)
      .from(Tables.EXPERIMENT_RUN_VIEW)
      .where(Tables.EXPERIMENT_RUN_VIEW.EXPERIMENTID.eq(experimentId))
      .fetch()
      .map(Record1::value1);

    // Turn the experiment run IDs into ExperimentRun objects.
    return ctx
      .selectFrom(Tables.EXPERIMENTRUN)
      .where(Tables.EXPERIMENTRUN.EXPERIMENT.in(experimentRunIds))
      .fetch()
      .map(r -> recordToThrift(r));
  }

  /**
   * Read details about a given experiment run.
   * @param experimentRunId - The ID of an experiment run.
   * @param ctx - The database context.
   * @return The details (e.g. Transformers) in the ExperimentRun with ID experimentRunId.
   * @throws ResourceNotFoundException - Thrown if there is no ExperimentRun with ID experimentRunId.
   */
  public static ExperimentRunDetailsResponse readExperimentRunDetails(
    int experimentRunId, 
    DSLContext ctx,
    MetadataDb metadataDb)
    throws ResourceNotFoundException {
    // Read the experiment run, experiment, and project.
    ExperimentRun expRun = read(experimentRunId, ctx);
    Experiment exp = ExperimentDao.read(expRun.getExperimentId(), ctx);
    Project proj = ProjectDao.read(exp.getProjectId(), ctx);

    // Find the IDs of the models in this ExperimentRun. Remember that a model is a Transformer that was created with
    // a FitEvent. We focus on the models because we don't want to clutter the response with every single StringIndexer,
    // Scaler, and other uninteresting Transformer.
    List<Integer> modelIds = ctx
      .select(Tables.TRANSFORMER.ID)
      .from(Tables.TRANSFORMER.join(Tables.FITEVENT)
        .on(Tables.TRANSFORMER.ID.eq(Tables.FITEVENT.TRANSFORMER)))
      .where(Tables.TRANSFORMER.EXPERIMENTRUN.eq(experimentRunId))
      .fetch()
      .map(Record1::value1);

    // Now get the ModelResponse object for each model ID.
    // The number of queries issued varies linearly with the number of models in
    // the experiment run. However, if this proves to be a performance bottleneck, we can
    // redesign it so that the number of queries is constant.
    // Also, the reason I'm using a for-loop instead of using a map() is because the readInfo
    // method can throw an exception, which isn't allowed in a map().
    List<ModelResponse> responses = new ArrayList<>();
    for (int modelId : modelIds) {
      responses.add(TransformerDao.readInfo(modelId, ctx, metadataDb));
    }

    return new ExperimentRunDetailsResponse(proj, exp, expRun, responses);
  }
}
