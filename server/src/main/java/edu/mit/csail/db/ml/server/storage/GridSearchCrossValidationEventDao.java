package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.*;
import modeldb.*;
import org.jooq.DSLContext;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GridSearchCrossValidationEventDao {
  public static GridSearchCrossValidationEventResponse store(GridSearchCrossValidationEvent gscve, DSLContext ctx) {
    gscve.bestFit.setExperimentRunId(gscve.experimentRunId);
    FitEventResponse fer = FitEventDao.store(gscve.bestFit.setProblemType(gscve.problemType), ctx);
    gscve.crossValidations.forEach(cv -> cv.setExperimentRunId(gscve.experimentRunId));

    GridsearchcrossvalidationeventRecord gscveRec = ctx.newRecord(Tables.GRIDSEARCHCROSSVALIDATIONEVENT);
    gscveRec.setId(null);
    gscveRec.setNumfolds(gscve.numFolds);
    gscveRec.setBest(fer.fitEventId);
    gscveRec.setExperimentrun(gscve.experimentRunId);
    gscveRec.store();

    EventRecord ev = EventDao.store(gscveRec.getId(), "cross validation grid search", gscve.experimentRunId, ctx);


    List<CrossValidationEventResponse> cveResponses = Collections.emptyList();

    if (!gscve.crossValidations.isEmpty()) {

      gscve.crossValidations.forEach(cve -> cve.setDf(cve.df.setId(fer.dfId)));
      Transformer cveTransformer = new Transformer(-1, "CrossValidationSplitterTransformer", "");

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

    return new GridSearchCrossValidationEventResponse(gscveRec.getId(), ev.getId(), fer, cveResponses);
  }
}
