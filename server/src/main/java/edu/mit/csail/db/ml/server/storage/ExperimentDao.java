package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.ExperimentRecord;
import modeldb.Experiment;
import modeldb.ExperimentEvent;
import modeldb.ExperimentEventResponse;
import modeldb.ResourceNotFoundException;
import org.jooq.DSLContext;

import java.sql.Timestamp;
import java.util.Date;

/**
 * This class contains logic for storing and reading experiments.
 */
public class ExperimentDao {
  /**
   * Store an entry in the Experiment table.
   * @param expt - The Experiment.
   * @param ctx - The database context.
   * @return A response indicating that the Experiment was stored.
   */
  public static ExperimentEventResponse store(ExperimentEvent expt, DSLContext ctx) {
    Experiment e = expt.experiment;

    // If the experiment is the default experiment, just read and return the default experiment for the
    // project.
    if (e.isDefault) {
      int defaultExperiment = ProjectDao.getDefaultExperiment(e.projectId, ctx);
      return new ExperimentEventResponse(defaultExperiment);
    }

    // Check if there's already an experiment with this project ID and experiment name. If so, return it.
    ExperimentRecord eRec = ctx.selectFrom(Tables.EXPERIMENT).where(
      Tables.EXPERIMENT.PROJECT.eq(e.projectId).and(
        Tables.EXPERIMENT.NAME.eq(e.name))
    ).fetchOne();
    if (eRec != null) {
      return new ExperimentEventResponse(eRec.getId());
    }

    // Store an entry in the Experiment table and return a response.
    eRec = ctx.newRecord(Tables.EXPERIMENT);
    eRec.setId(e.id < 0 ? null : e.id);
    eRec.setName(e.name);
    eRec.setDescription(e.description);
    eRec.setProject(e.projectId);
    eRec.setCreated(new Timestamp((new Date()).getTime()));
    eRec.store();
    return new ExperimentEventResponse(eRec.getId());
  }

  /**
   * Read the entry from the Experiment table with the given ID.
   * @param experimentId - The ID (i.e. primary key) that we will look up in the Experiment table.
   * @param ctx - The database context.
   * @return The Experiment whose ID is experimentId.
   * @throws ResourceNotFoundException - Thrown if there is no entry in the Experiment table that has the
   * given experimentId.
   */
  public static Experiment read(int experimentId, DSLContext ctx) throws ResourceNotFoundException {
    // Read the experiment.
    ExperimentRecord rec = ctx
      .selectFrom(Tables.EXPERIMENT)
      .where(Tables.EXPERIMENT.ID.eq(experimentId))
      .fetchOne();

    // If it can't be found, throw an exception.
    if (rec == null) {
      throw new ResourceNotFoundException(String.format(
        "Can't find Experiment with ID %d",
        experimentId
      ));
    }

    // In order to determine whether this is the default experiment, we need to look up the
    // ID of the default experiment.
    int defExpId = ProjectDao.getDefaultExperiment(rec.getProject(), ctx);

    return new Experiment(rec.getId(), rec.getProject(), rec.getName(), rec.getDescription(), rec.getId() == defExpId);
  }
}
