
package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.DataframesplitRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep5;
import org.jooq.Query;

public class DataFrameSplitDuplicator extends Duplicator<DataframesplitRecord> {
//  id INTEGER PRIMARY KEY AUTOINCREMENT,
//  splitEventId INTEGER REFERENCES RandomSplitEvent NOT NULL,
//  weight FLOAT NOT NULL,
//  dataFrameId INTEGER REFERENCES DataFrame NOT NULL,
//  experimentRun INTEGER REFERENCES ExperimentRun NOT NULL
  InsertValuesStep5<DataframesplitRecord, Integer, Integer, Float, Integer, Integer> query;

  public DataFrameSplitDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.DATAFRAMESPLIT).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.DATAFRAMESPLIT,
      Tables.DATAFRAMESPLIT.ID,
      Tables.DATAFRAMESPLIT.SPLITEVENTID,
      Tables.DATAFRAMESPLIT.WEIGHT,
      Tables.DATAFRAMESPLIT.DATAFRAMEID,
      Tables.DATAFRAMESPLIT.EXPERIMENTRUN
    );
  }

  @Override
  public void updateQuery(DataframesplitRecord rec, int iteration) {
    query = query.values(
      maxId,
      RandomSplitEventDuplicator.getInstance(ctx).id(rec.getSpliteventid(), iteration),
      rec.getWeight(),
      DataFrameDuplicator.getInstance(ctx).id(rec.getDataframeid(), iteration),
      ExperimentRunDuplicator.getInstance(ctx).id(rec.getExperimentrun(), iteration)
    );
  }

  private static DataFrameSplitDuplicator instance = null;
  public static DataFrameSplitDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new DataFrameSplitDuplicator(ctx);
    }
    return instance;
  }
}

