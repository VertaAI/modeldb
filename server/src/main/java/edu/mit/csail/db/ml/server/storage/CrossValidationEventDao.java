package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.*;
import modeldb.*;
import org.jooq.DSLContext;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CrossValidationEventDao {
  public static CrossValidationEventResponse store(CrossValidationEvent cve, DSLContext ctx) {
    DataframeRecord df = DataFrameDao.store(cve.df, cve.experimentRunId, ctx);
    TransformerspecRecord spec = TransformerSpecDao.store(cve.spec, cve.experimentRunId, ctx);

    CrossvalidationeventRecord cveRec = ctx.newRecord(Tables.CROSSVALIDATIONEVENT);
    cveRec.setId(null);
    cveRec.setDf(df.getId());
    cveRec.setSpec(spec.getId());
    cveRec.setNumfolds(cve.folds.size());
    cveRec.setRandomseed(cve.seed);
    cveRec.setEvaluator(cve.evaluator);
    cveRec.setExperimentrun(cve.experimentRunId);
    cveRec.store();

    EventRecord ev = EventDao.store(cveRec.getId(), "cross validation", cve.experimentRunId, ctx);

    List<FitEventResponse> fers = cve
      .folds
      .stream()
      .map(fold -> new FitEvent(
        fold.trainingDf,
        cve.spec.setId(spec.getId()),
        fold.model,
        cve.featureColumns,
        cve.predictionColumns,
        cve.labelColumns,
        cve.experimentRunId
      ).setProblemType(cve.problemType))
      .map(fe -> FitEventDao.store(fe, ctx))
      .collect(Collectors.toList());

    List<MetricEventResponse> mers = IntStream
      .range(0, cve.folds.size())
      .mapToObj(ind -> new MetricEvent(
        cve.folds.get(ind).validationDf,
        cve.folds.get(ind).model.setId(fers.get(ind).modelId),
        cve.evaluator,
        cve.folds.get(ind).score,
        cve.labelColumns.get(0), // TODO: Are these reasonable settings?
        cve.predictionColumns.get(0),
        cve.experimentRunId
      ))
      .map(me -> MetricEventDao.store(me, ctx))
      .collect(Collectors.toList());


    List<CrossValidationFoldResponse> foldResponses = IntStream
      .range(0, cve.folds.size())
      .mapToObj(ind -> new CrossValidationFoldResponse(
        fers.get(ind).modelId,
        mers.get(ind).dfId,
        fers.get(ind).dfId
      ))
      .collect(Collectors.toList());

    mers.forEach(me -> {
      CrossvalidationfoldRecord rec = ctx.newRecord(Tables.CROSSVALIDATIONFOLD);
      rec.setId(null);
      rec.setMetric(me.getMetricEventId());
      rec.setEvent(cveRec.getId());
      rec.setExperimentrun(cve.experimentRunId);
      rec.store();
      rec.getId();
    });

    return new CrossValidationEventResponse(df.getId(), spec.getId(), ev.getId(), foldResponses, cveRec.getId());
  }
}
