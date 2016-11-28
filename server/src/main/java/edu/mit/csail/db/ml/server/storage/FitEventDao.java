package edu.mit.csail.db.ml.server.storage;

import edu.mit.csail.db.ml.util.Pair;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.*;
import modeldb.*;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record1;

import java.util.*;
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
    feRec.setLabelcolumns(fe.labelColumns.stream().collect(Collectors.joining(",")));
    feRec.store();

    // Store Event.
    EventRecord ev = EventDao.store(feRec.getId(), isPipeline ? "pipeline fit" : "fit", fe.experimentRunId, ctx);

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


  public static int getNumRowsForModel(int modelId, DSLContext ctx)
    throws ResourceNotFoundException {
    int numRows = getNumRowsForModels(Collections.singletonList(modelId), ctx).get(0);
    if (numRows < 0) {
      throw new ResourceNotFoundException(String.format(
        "Could not find number of rows used to train Transformer %d because the Transformer doesn't exist",
        modelId
      ));
    }
    return numRows;
  }

  public static FitEvent read(int fitEventId, DSLContext ctx) throws ResourceNotFoundException {
    return read(Collections.singletonList(fitEventId), ctx).get(0);
  }

  public static int getFitEventIdForModelId(int modelId, DSLContext ctx) throws ResourceNotFoundException {
    Record1<Integer> rec =
      ctx.select(Tables.FITEVENT.ID).from(Tables.FITEVENT).where(Tables.FITEVENT.TRANSFORMER.eq(modelId)).fetchOne();

    if (rec == null) {
      throw new ResourceNotFoundException(String.format("Could not find FitEvent for model %d", modelId));
    }

    return rec.value1();
  }

  /**
   * Read the FitEvents associated with the given IDs.
   * @param fitEventIds - The TransformEvent IDs to look up.
   * @return A list of TransformEvents, transformEvents, where transformEvents.get(i) is the TransformEvent associated
   *  with transformEventIds.get(i). The schema field of each TransformEvent will be emtpy. This is done for performance
   *  reasons (reduces storage space and avoids extra query).
   * @throws ResourceNotFoundException - Thrown if any of the IDs do not have an associated TransformEvent.
   */
  public static List<FitEvent> read(List<Integer> fitEventIds, DSLContext ctx)
    throws ResourceNotFoundException {
    Map<Integer, FitEvent> fitEventForId = new HashMap<>();

    ctx.select(
      Tables.DATAFRAME.ID,
      Tables.DATAFRAME.TAG,
      Tables.DATAFRAME.NUMROWS,
      Tables.DATAFRAME.FILEPATH,
      Tables.TRANSFORMERSPEC.ID,
      Tables.TRANSFORMERSPEC.TRANSFORMERTYPE,
      Tables.TRANSFORMERSPEC.TAG,
      Tables.FITEVENT.ID,
      Tables.FITEVENT.PREDICTIONCOLUMNS,
      Tables.FITEVENT.LABELCOLUMNS,
      Tables.FITEVENT.PROBLEMTYPE,
      Tables.FITEVENT.EXPERIMENTRUN,
      Tables.TRANSFORMER.ID,
      Tables.TRANSFORMER.TRANSFORMERTYPE,
      Tables.TRANSFORMER.TAG,
      Tables.TRANSFORMER.FILEPATH
    )
      .from(
        Tables.FITEVENT
          .join(Tables.TRANSFORMER).on(Tables.FITEVENT.TRANSFORMER.eq(Tables.TRANSFORMER.ID))
          .join(Tables.DATAFRAME).on(Tables.FITEVENT.DF.eq(Tables.DATAFRAME.ID))
          .join(Tables.TRANSFORMERSPEC).on(Tables.FITEVENT.TRANSFORMERSPEC.eq(Tables.TRANSFORMERSPEC.ID)))
      .where(Tables.FITEVENT.ID.in(fitEventIds))
      .fetch()
      .forEach(rec -> {
        DataFrame df = new DataFrame(
          rec.get(Tables.DATAFRAME.ID),
          Collections.emptyList(),
          rec.get(Tables.DATAFRAME.NUMROWS),
          rec.get(Tables.DATAFRAME.TAG)
        );
        df.setFilepath(rec.get(Tables.DATAFRAME.FILEPATH));

        TransformerSpec spec = new TransformerSpec(
          rec.get(Tables.TRANSFORMERSPEC.ID),
          rec.get(Tables.TRANSFORMERSPEC.TRANSFORMERTYPE),
          Collections.emptyList(),
          rec.get(Tables.TRANSFORMERSPEC.TAG)
        );

        Transformer transformer = new Transformer(
          rec.get(Tables.TRANSFORMER.ID),
          rec.get(Tables.TRANSFORMER.TRANSFORMERTYPE),
          rec.get(Tables.TRANSFORMER.TAG)
        );
        transformer.setFilepath(rec.get(Tables.TRANSFORMER.FILEPATH));

        int expRunId = rec.get(Tables.FITEVENT.EXPERIMENTRUN);
        List<String> predictionCols = Arrays.asList(rec.get(Tables.FITEVENT.PREDICTIONCOLUMNS).split(","));
        List<String> labelCols = Arrays.asList(rec.get(Tables.FITEVENT.LABELCOLUMNS).split(","));

        FitEvent fitEvent = new FitEvent(
          df,
          spec,
          transformer,
          Collections.emptyList(),
          predictionCols,
          labelCols,
          expRunId
        );
        fitEvent.setProblemType(ProblemTypeConverter.fromString(rec.get(Tables.FITEVENT.PROBLEMTYPE)));

        int fitEventId = rec.get(Tables.FITEVENT.ID);
        fitEventForId.put(fitEventId, fitEvent);
      });

    List<FitEvent> fitEvents = new ArrayList<>();
    IntStream.range(0, fitEventIds.size()).forEach(i -> {
      fitEvents.add(fitEventForId.getOrDefault(fitEventIds.get(i), null));
    });

    if (fitEvents.contains(null)) {
      throw new ResourceNotFoundException("Could not find FitEvent with ID %s" + fitEvents.indexOf(null));
    }
    return fitEvents;
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
