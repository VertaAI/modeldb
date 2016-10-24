package edu.mit.csail.db.ml.server.storage;

import javafx.util.Pair;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.*;
import modeldb.FitEvent;
import modeldb.FitEventResponse;
import modeldb.ResourceNotFoundException;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FitEventDao {
  public static FitEventResponse store(FitEvent fe, DSLContext ctx) {
    return store(fe, ctx, false);
  }

  public static FitEventResponse store(FitEvent fe, DSLContext ctx, boolean isPipeline) {
    // Store DataFrame, Transformer, and TransformerSpec.
    DataframeRecord df = DataFrameDao.store(fe.df, fe.experimentRunId, ctx);
    TransformerRecord t = TransformerDao.store(fe.model, fe.experimentRunId, ctx);
    TransformerspecRecord s = TransformerSpecDao.store(fe.spec, fe.experimentRunId, ctx);

    // Store the FitEvent.
    FiteventRecord feRec = ctx.newRecord(Tables.FITEVENT);
    feRec.setId(null);
    feRec.setExperimentrun(fe.experimentRunId);
    feRec.setDf(df.getId());
    feRec.setTransformer(t.getId());
    feRec.setTransformerspec(s.getId());
    feRec.setProblemtype(ProblemTypeConverter.toString(fe.problemType));

    // Make sure the prediction, label, and feature columns do not have duplicates.
    // Make sure the label columns and feature columns do not use a prediction column.
    fe.setPredictionColumns(
      fe.predictionColumns
        .stream()
        .distinct()
        .collect(Collectors.toList())
    );
    fe.setLabelColumns(
      fe.labelColumns
        .stream()
        .distinct()
        .filter(col -> !fe.predictionColumns.contains(col))
        .sorted()
        .collect(Collectors.toList())
    );
    fe.setFeatureColumns(
      fe.featureColumns
        .stream()
        .distinct()
        .filter(col -> !fe.predictionColumns.contains(col))
        .sorted()
        .collect(Collectors.toList())
    );

    feRec.setPredictioncolumns(fe.predictionColumns.stream().collect(Collectors.joining(",")));
    feRec.setLabelcolumn(fe.labelColumns.stream().collect(Collectors.joining(",")));
    feRec.store();

    // Store Event.
    EventRecord ev = EventDao.store(feRec.getId(), isPipeline ? "pipeline fit " : "fit", fe.experimentRunId, ctx);

    // Store features.
    IntStream.range(0, fe.featureColumns.size()).forEach(i -> {
      String name = fe.featureColumns.get(i);
      FeatureRecord featureRecord = ctx.newRecord(Tables.FEATURE);
      featureRecord.setId(null);
      featureRecord.setName(name);
      featureRecord.setFeatureindex(i);
      featureRecord.setTransformer(t.getId());
      featureRecord.setImportance(0.0); // By default we say that all features have importance 0.
      featureRecord.store();
      featureRecord.getId();
    });
    // Return the response.
    return new FitEventResponse(df.getId(), s.getId(), t.getId(), ev.getId(), feRec.getId());
  }

  public static List<Integer> getNumRowsForModels(List<Integer> modelIds, DSLContext ctx) {
    Map<Integer, Integer> indexForId =
      IntStream.range(0, modelIds.size())
      .mapToObj(i -> new Pair<>(modelIds.get(i), i))
      .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

    // Rather than making the call fail if we can't find one of the models, we instead say it has -1 rows.
    List<Integer> numRows = modelIds.stream().map(mid -> -1).collect(Collectors.toList());

    ctx
      .select(Tables.DATAFRAME.NUMROWS, Tables.FITEVENT.TRANSFORMER)
      .from(Tables.FITEVENT)
      .join(Tables.DATAFRAME)
      .on(Tables.DATAFRAME.ID.eq(Tables.FITEVENT.DF))
      .where(Tables.FITEVENT.TRANSFORMER.in(modelIds))
      .fetch()
      .into(rec -> numRows.set(indexForId.get(rec.value2()), rec.value1()));

    return numRows;
  }

  /**
   * Get the ID of the DataFrame that produced the model with the given ID.
   * @param modelId - The id of the produced model.
   * @param ctx - Jooq context.
   * @return The ID (or -1 if this model was not created by a FitEvent).
   */
  public static int getParentDfId(int modelId, DSLContext ctx) throws ResourceNotFoundException {
    FiteventRecord rec = ctx
      .selectFrom(Tables.FITEVENT)
      .where(Tables.FITEVENT.TRANSFORMER.eq(modelId))
      .fetchOne();
    if (rec == null) {
      throw new ResourceNotFoundException(String.format(
        "Could not find the DataFrame that produced Transformer %d because that Transformer doesn't exist",
        modelId
      ));
    }
    return rec.getDf();
  }
}
