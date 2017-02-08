package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.*;
import modeldb.*;
import org.jooq.DSLContext;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class contains logic for storing grid search cross validation events.
 */
public class GridSearchCrossValidationEventDao {
  /**
   * Store a grid search cross validation event in the database.
   * @param gscve - The grid search cross validation event.
   * @param ctx - The database context.
   * @return The response indicating that the event has been stored.
   */
  public static GridSearchCrossValidationEventResponse store(GridSearchCrossValidationEvent gscve, DSLContext ctx) {
    // Ensure that the FitEvent of the overall best model has the same experiment run ID as the overall event.
    gscve.bestFit.setExperimentRunId(gscve.experimentRunId);

    // Store the FitEvent for the best model.
    FitEventResponse fer = FitEventDao.store(gscve.bestFit.setProblemType(gscve.problemType), ctx);

    // Ensure that each cross validation event in the grid search cross validation event has the same experiment
    // run ID.
    gscve.crossValidations.forEach(cv -> cv.setExperimentRunId(gscve.experimentRunId));

    // Store an entry in the GridSearchCrossValidationEvent table.
    GridsearchcrossvalidationeventRecord gscveRec = ctx.newRecord(Tables.GRIDSEARCHCROSSVALIDATIONEVENT);
    gscveRec.setId(null);
    gscveRec.setNumfolds(gscve.numFolds);
    gscveRec.setBest(fer.fitEventId);
    gscveRec.setExperimentrun(gscve.experimentRunId);
    gscveRec.store();

    // Store an entry in the Event table.
    EventRecord ev = EventDao.store(gscveRec.getId(), "cross validation grid search", gscve.experimentRunId, ctx);

    // Collect the responses to storing each cross validation fold in this list.
    List<CrossValidationEventResponse> cveResponses = Collections.emptyList();

    // The user may have turned off storage of intermediate cross validation events. So, we only execute the
    // code inside this if-block if there are indeed intermediates to store.
    if (!gscve.crossValidations.isEmpty()) {

      // First, ensure that the input DataFrame for each cross validation event is the same.
      gscve.crossValidations.forEach(cve -> cve.setDf(cve.df.setId(fer.dfId)));

      // Create a dummy Transformer to reflect the TransformEvent that creates the training and validation DataFrames
      // from the original DataFrame.
      Transformer cveTransformer = new Transformer(-1, "CrossValidationSplitterTransformer", "");

      // Store a TransformEvent to indicate the creation of each validation DataFrame.
      List<TransformEventResponse> validationDfs = gscve
        .crossValidations
        .get(0)
        .folds
        .stream()
        .map(fold -> TransformEventDao.store(
          new TransformEvent(
            gscve.bestFit.df.setId(fer.dfId),
            fold.validationDf,
            cveTransformer,
            Collections.emptyList(),
            Collections.emptyList(),
            gscve.experimentRunId
          ),
          ctx
        ))
        .collect(Collectors.toList());

      // Store a TransformEvent to indicate the creation of each training DataFrame.
      List<TransformEventResponse> trainingDfs = gscve
        .crossValidations
        .get(0)
        .folds
        .stream()
        .map(fold -> TransformEventDao.store(
          new TransformEvent(
            gscve.bestFit.df.setId(fer.dfId),
            fold.trainingDf,
            cveTransformer,
            Collections.emptyList(),
            Collections.emptyList(),
            gscve.experimentRunId
          ),
          ctx
        ))
        .collect(Collectors.toList());

      // Store each CrossValidationEvent.
      cveResponses = gscve
        .crossValidations
        .stream()
        .map(cve -> {
          IntStream
            .range(0, cve.folds.size())
            .forEach(ind ->
              cve.folds.get(ind)
                .setValidationDf(cve.folds.get(ind).validationDf.setId(validationDfs.get(ind).getNewDataFrameId()))
                .setTrainingDf(cve.folds.get(ind).trainingDf.setId(trainingDfs.get(ind).getNewDataFrameId()))
            );
          return CrossValidationEventDao.store(cve.setProblemType(gscve.problemType), ctx);
        })
        .collect(Collectors.toList());

      // Store entries in the GridCellCrossValidation (one for each hyperparameter configuration).
      cveResponses
        .forEach(cver -> {
          GridcellcrossvalidationRecord rec = ctx.newRecord(Tables.GRIDCELLCROSSVALIDATION);
          rec.setId(null);
          rec.setGridsearch(gscveRec.getId());
          rec.setCrossvalidation(cver.getCrossValidationEventId());
          rec.setExperimentrun(gscve.experimentRunId);
          rec.store();
          rec.getId();
        });
    }

    // Return the response.
    return new GridSearchCrossValidationEventResponse(gscveRec.getId(), ev.getId(), fer, cveResponses);
  }
}
