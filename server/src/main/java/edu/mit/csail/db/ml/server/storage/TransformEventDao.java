package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.Dataframe;
import jooq.sqlite.gen.tables.records.DataframeRecord;
import jooq.sqlite.gen.tables.records.EventRecord;
import jooq.sqlite.gen.tables.records.TransformerRecord;
import jooq.sqlite.gen.tables.records.TransformeventRecord;
import modeldb.*;
import org.jooq.DSLContext;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static jooq.sqlite.gen.Tables.DATAFRAME;
import static jooq.sqlite.gen.Tables.TRANSFORMEVENT;

public class TransformEventDao {
  public static TransformEventResponse store(TransformEvent te, DSLContext ctx) {
    // Store the DataFrames and Transformer.

    DataframeRecord oldDf = DataFrameDao.store(te.oldDataFrame, te.experimentRunId, ctx);
    DataframeRecord newDf = DataFrameDao.store(te.newDataFrame, te.experimentRunId, ctx);
    TransformerRecord t = TransformerDao.store(te.transformer, te.experimentRunId, ctx);

    // Store the TransformEvent.
    TransformeventRecord teRec = ctx.newRecord(TRANSFORMEVENT);
    teRec.setId(null);
    teRec.setExperimentrun(te.experimentRunId);
    teRec.setNewdf(newDf.getId());
    teRec.setOlddf(oldDf.getId());
    teRec.setTransformer(t.getId());
    // Remove duplicate columns and remove input columns from output columns.
    te.setInputColumns(
      te.inputColumns
        .stream()
        .distinct()
        .sorted()
        .collect(Collectors.toList())
    );
    te.setOutputColumns(
      te.outputColumns
        .stream()
        .distinct()
        .filter(col -> !te.inputColumns.contains(col))
        .sorted()
        .collect(Collectors.toList())
    );


    teRec.setInputcolumns(te.inputColumns.stream().collect(Collectors.joining(",")));
    teRec.setOutputcolumns(te.outputColumns.stream().collect(Collectors.joining(",")));
    teRec.store();

    // Store the Event.
    EventRecord ev = EventDao.store(teRec.getId(), "transform", te.experimentRunId, ctx);

    // Return the TransformEventResponse.
    return new TransformEventResponse(oldDf.getId(), newDf.getId(), t.getId(), ev.getId(), t.getFilepath());
  }

  /**
   * Read the TransformEvents associated with the given IDs.
   * @param transformEventIds - The TransformEvent IDs to look up.
   * @return A list of TransformEvents, transformEvents, where transformEvents.get(i) is the TransformEvent associated
   *  with transformEventIds.get(i). The schema field of each TransformEvent will be emtpy. This is done for performance
   *  reasons (reduces storage space and avoids extra query).
   * @throws ResourceNotFoundException - Thrown if any of the IDs do not have an associated TransformEvent.
   */
  public static List<TransformEvent> read(List<Integer> transformEventIds, DSLContext ctx)
    throws ResourceNotFoundException {
    Map<Integer, TransformEvent> transformEventForId = new HashMap<>();


    String OLD_DF_TABLE = "olddf";
    String NEW_DF_TABLE = "newdf";

    Dataframe oldDfTable = Tables.DATAFRAME.as(OLD_DF_TABLE);
    Dataframe newDfTable = Tables.DATAFRAME.as(NEW_DF_TABLE);

    ctx.select(
      oldDfTable.ID,
      oldDfTable.TAG,
      oldDfTable.NUMROWS,
      oldDfTable.FILEPATH,
      newDfTable.ID,
      newDfTable.TAG,
      newDfTable.NUMROWS,
      newDfTable.FILEPATH,
      Tables.TRANSFORMEVENT.ID,
      Tables.TRANSFORMEVENT.INPUTCOLUMNS,
      Tables.TRANSFORMEVENT.OUTPUTCOLUMNS,
      Tables.TRANSFORMEVENT.EXPERIMENTRUN,
      Tables.TRANSFORMER.ID,
      Tables.TRANSFORMER.TRANSFORMERTYPE,
      Tables.TRANSFORMER.TAG,
      Tables.TRANSFORMER.FILEPATH
    )
      .from(
        Tables.TRANSFORMEVENT
          .join(Tables.TRANSFORMER).on(Tables.TRANSFORMEVENT.TRANSFORMER.eq(Tables.TRANSFORMER.ID))
          .join(Tables.DATAFRAME.as(OLD_DF_TABLE)).on(Tables.TRANSFORMEVENT.OLDDF.eq(oldDfTable.ID))
          .join(Tables.DATAFRAME.as(NEW_DF_TABLE)).on(Tables.TRANSFORMEVENT.NEWDF.eq(newDfTable.ID)))
      .where(Tables.TRANSFORMEVENT.ID.in(transformEventIds))
      .fetch()
      .forEach(rec -> {
        DataFrame oldDf = new DataFrame(
          rec.get(oldDfTable.ID),
          Collections.emptyList(),
          rec.get(oldDfTable.NUMROWS),
          rec.get(oldDfTable.TAG)
        );
        oldDf.setFilepath(rec.get(oldDfTable.FILEPATH));

        DataFrame newDf = new DataFrame(
          rec.get(newDfTable.ID),
          Collections.emptyList(),
          rec.get(newDfTable.NUMROWS),
          rec.get(newDfTable.TAG)
        );
        newDf.setFilepath(rec.get(newDfTable.FILEPATH));

        Transformer transformer = new Transformer(
          rec.get(Tables.TRANSFORMER.ID),
          rec.get(Tables.TRANSFORMER.TRANSFORMERTYPE),
          rec.get(Tables.TRANSFORMER.TAG)
        );
        transformer.setFilepath(rec.get(Tables.TRANSFORMER.FILEPATH));

        int expRunId = rec.get(Tables.TRANSFORMEVENT.EXPERIMENTRUN);
        List<String> inputCols = Arrays.asList(rec.get(Tables.TRANSFORMEVENT.INPUTCOLUMNS).split(","));
        List<String> outputCols = Arrays.asList(rec.get(Tables.TRANSFORMEVENT.OUTPUTCOLUMNS).split(","));
        TransformEvent transformEvent = new TransformEvent(oldDf, newDf, transformer, inputCols, outputCols, expRunId);

        int transformEventId = rec.get(Tables.TRANSFORMEVENT.ID);
        transformEventForId.put(transformEventId, transformEvent);
      });

    List<TransformEvent> transformEvents = new ArrayList<>();
    IntStream.range(0, transformEventIds.size()).forEach(i -> {
      transformEvents.add(transformEventForId.getOrDefault(transformEventIds.get(i), null));
    });

    if (transformEvents.contains(null)) {
      throw new ResourceNotFoundException("Could not find TransformEvent with ID %s" + transformEvents.indexOf(null));
    }
    return transformEvents;
  }
}
