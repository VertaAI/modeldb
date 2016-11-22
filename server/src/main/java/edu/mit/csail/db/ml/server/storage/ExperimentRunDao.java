package edu.mit.csail.db.ml.server.storage;

import edu.mit.csail.db.ml.util.Pair;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.ExperimentrunRecord;
import modeldb.*;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Record2;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ExperimentRunDao {
    // TODO: should we do a check here if experiment run exists?
  public static ExperimentRunEventResponse store(ExperimentRunEvent erun, DSLContext ctx) {
    ExperimentRun er = erun.experimentRun;
    ExperimentrunRecord erRec = ctx.newRecord(Tables.EXPERIMENTRUN);
    erRec.setId(er.id < 0 ? null : er.id);
    erRec.setExperiment(er.experimentId);
    erRec.setDescription(er.description);
    erRec.setCreated(new Timestamp((new Date()).getTime()));
    erRec.store();
    return new ExperimentRunEventResponse(erRec.getId());
  }

  public static int getProjectId(int eRunId, DSLContext ctx) {
    return getExperimentAndProjectIds(eRunId, ctx).getValue();
  }

  public static Pair<Integer, Integer> getExperimentAndProjectIds(int eRunId, DSLContext ctx) {
    Record2<Integer, Integer> rec = ctx.select(Tables.EXPERIMENT_RUN_VIEW.EXPERIMENTID, Tables.EXPERIMENT_RUN_VIEW.PROJECTID)
      .from(Tables.EXPERIMENT_RUN_VIEW)
      .where(Tables.EXPERIMENT_RUN_VIEW.EXPERIMENTRUNID.eq(eRunId))
      .fetchOne();
    if (rec == null) {
      return new Pair<>(-1, -1);
    }
    return new Pair<>(rec.value1(), rec.value2());
  }

  public static void validateExperimentRunId(int id, DSLContext ctx) throws InvalidExperimentRunException {
    if (ctx.selectFrom(Tables.EXPERIMENTRUN).where(Tables.EXPERIMENTRUN.ID.eq(id)).fetchOne() == null) {
      throw new InvalidExperimentRunException(String.format(
        "Can't find experiment run ID %d",
        id
      ));
    }
  }

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
    return new ExperimentRun(rec.getId(), rec.getExperiment(), rec.getDescription());
  }

  public static ProjectExperimentsAndRuns readExperimentsAndRunsInProject(int projId, DSLContext ctx) {
    // Get all the (experiment run ID, experiment ID) pairs in the project.
    List<Pair<Integer, Integer>> runExpPairs = ctx
      .select(Tables.EXPERIMENT_RUN_VIEW.EXPERIMENTRUNID, Tables.EXPERIMENT_RUN_VIEW.EXPERIMENTID)
      .from(Tables.EXPERIMENT_RUN_VIEW)
      .where(Tables.EXPERIMENT_RUN_VIEW.PROJECTID.eq(projId))
      .fetch()
      .map(r -> new Pair<>(r.value1(), r.value2()));

    // Fetch all the experiments in the project.
    List<Integer> experimentIds = runExpPairs.stream().map(Pair::getValue).collect(Collectors.toList());
    int defaultExp = experimentIds.stream().mapToInt(s -> s.intValue()).min().orElse(0);
    List<Experiment> experiments = ctx
      .selectFrom(Tables.EXPERIMENT)
      .where(Tables.EXPERIMENT.ID.in(experimentIds))
      .orderBy(Tables.EXPERIMENT.ID.asc())
      .fetch()
      .map(rec -> new Experiment(rec.getId(), projId, rec.getName(), rec.getDescription(), rec.getId() == defaultExp));

    // Fetch all the experiment runs in the project.
    List<Integer> experimentRunIds = runExpPairs.stream().map(Pair::getKey).collect(Collectors.toList());
    List<ExperimentRun> experimentRuns = ctx
      .selectFrom(Tables.EXPERIMENTRUN)
      .where(Tables.EXPERIMENTRUN.ID.in(experimentRunIds))
      .orderBy(Tables.EXPERIMENTRUN.ID.asc())
      .fetch()
      .map(rec -> new ExperimentRun(rec.getId(), rec.getExperiment(), rec.getDescription()));

    return new ProjectExperimentsAndRuns(projId, experiments, experimentRuns);
  }

  public static List<ExperimentRun> readExperimentRunsInExperiment(int experimentId, DSLContext ctx) {
    List<Integer> experimentRunIds = ctx
      .select(Tables.EXPERIMENT_RUN_VIEW.EXPERIMENTRUNID)
      .from(Tables.EXPERIMENT_RUN_VIEW)
      .where(Tables.EXPERIMENT_RUN_VIEW.EXPERIMENTID.eq(experimentId))
      .fetch()
      .map(Record1::value1);

    return ctx
      .selectFrom(Tables.EXPERIMENTRUN)
      .where(Tables.EXPERIMENTRUN.EXPERIMENT.in(experimentRunIds))
      .fetch()
      .map(r -> new ExperimentRun(r.getId(), r.getExperiment(), r.getDescription()));
  }

  public static ExperimentRunDetailsResponse readExperimentRunDetails(int experimentRunId, DSLContext ctx)
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
      .from(Tables.TRANSFORMER.join(Tables.FITEVENT).on(Tables.TRANSFORMER.ID.eq(Tables.FITEVENT.TRANSFORMER)))
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
      responses.add(TransformerDao.readInfo(modelId, ctx));
    }

    return new ExperimentRunDetailsResponse(proj, exp, expRun, responses);
  }
}
