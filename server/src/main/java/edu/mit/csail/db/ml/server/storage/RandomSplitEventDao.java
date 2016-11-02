package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.DataframeRecord;
import jooq.sqlite.gen.tables.records.DataframesplitRecord;
import jooq.sqlite.gen.tables.records.EventRecord;
import jooq.sqlite.gen.tables.records.RandomspliteventRecord;
import modeldb.*;
import org.jooq.DSLContext;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RandomSplitEventDao {
  public static RandomSplitEventResponse store(RandomSplitEvent rse, DSLContext ctx) {
    // Store the old DataFrame.
    DataframeRecord oldDf = DataFrameDao.store(rse.oldDataFrame, rse.experimentRunId, ctx);

    // Store the RandomSplitEvent.
    RandomspliteventRecord rseRec = ctx.newRecord(Tables.RANDOMSPLITEVENT);
    rseRec.setId(null);
    rseRec.setInputdataframeid(oldDf.getId());
    rseRec.setRandomseed(rse.seed);
    rseRec.setExperimentrun(rse.experimentRunId);
    rseRec.store();

    // Store the associated Event.
    EventRecord ev = EventDao.store(rseRec.getId(), "random split", rse.experimentRunId, ctx);

    // Store a DataFrame for each split.
    List<DataframeRecord> splitDfs = rse
      .splitDataFrames
      .stream()
      .map(df -> DataFrameDao.store(df, rse.experimentRunId, ctx))
      .collect(Collectors.toList());

    // Store a DataFrameSplit for each split.
    IntStream
      .range(0, rse.splitDataFrames.size())
      .forEach(ind -> {
        DataframesplitRecord splRec = ctx.newRecord(Tables.DATAFRAMESPLIT);
        splRec.setId(null);
        splRec.setSpliteventid(rseRec.getId());
        splRec.setWeight(rse.weights.get(ind).floatValue());
        splRec.setDataframeid(splitDfs.get(ind).getId());
        splRec.setExperimentrun(rse.experimentRunId);
        splRec.store();
        splRec.getId();
      });

    // Store a TransformEvent for each split. This allows us to preserve the ancestor chain of DataFrames.

    // Each split will derive from the original DataFrame.
    DataFrame oldDataFrame = rse.oldDataFrame.setId(oldDf.getId());

    // We'll use a dummy Transformer.
    Transformer rseTransformer = new Transformer(-1, Collections.emptyList(), "RandomSplitTransformer", "");

    // Store the TransformEvent for each split.
    List<Integer> splitIds = IntStream.range(0, rse.splitDataFrames.size())
      .mapToObj(index -> {
        DataFrame splitDataFrame = rse.splitDataFrames.get(index);
        TransformEvent te = new TransformEvent(
          oldDataFrame,
          splitDataFrame,
          rseTransformer,
          Collections.emptyList(),
          Collections.emptyList(),
          rse.experimentRunId
        );
        return TransformEventDao.store(te, ctx, false).newDataFrameId;
      })
      .collect(Collectors.toList());

    return new RandomSplitEventResponse(oldDf.getId(), splitIds, ev.getId());
  }
}
