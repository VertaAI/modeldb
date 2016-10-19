package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.tables.records.DataframeRecord;
import jooq.sqlite.gen.tables.records.EventRecord;
import jooq.sqlite.gen.tables.records.TransformerRecord;
import jooq.sqlite.gen.tables.records.TransformeventRecord;
import modeldb.TransformEvent;
import modeldb.TransformEventResponse;
import org.jooq.DSLContext;

import java.util.stream.Collectors;

import static jooq.sqlite.gen.Tables.TRANSFORMEVENT;

public class TransformEventDao {
  public static TransformEventResponse store(TransformEvent te, DSLContext ctx,boolean generateFilepath) {
    // Store the DataFrames and Transformer.

    DataframeRecord oldDf = DataFrameDao.store(te.oldDataFrame, te.projectId, te.experimentRunId, ctx);
    DataframeRecord newDf = DataFrameDao.store(te.newDataFrame, te.projectId, te.experimentRunId, ctx);
    TransformerRecord t = TransformerDao.store(te.transformer, te.projectId, te.experimentRunId, ctx, generateFilepath);

    // Store the TransformEvent.
    TransformeventRecord teRec = ctx.newRecord(TRANSFORMEVENT);
    teRec.setId(null);
    teRec.setProject(te.projectId);
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
    EventRecord ev = EventDao.store(teRec.getId(), "transform", te.projectId, te.experimentRunId, ctx);

    // Return the TransformEventResponse.
    return new TransformEventResponse(oldDf.getId(), newDf.getId(), t.getId(), ev.getId(), t.getFilepath());
  }

  public static TransformEventResponse store(TransformEvent te, DSLContext ctx) {
    return store(te, ctx, false);
  }
}
