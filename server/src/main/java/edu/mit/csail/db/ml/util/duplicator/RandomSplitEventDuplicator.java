package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.RandomspliteventRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep4;
import org.jooq.Query;

public class RandomSplitEventDuplicator extends Duplicator<RandomspliteventRecord> {
  InsertValuesStep4<RandomspliteventRecord, Integer, Integer, Long, Integer> query;

  public RandomSplitEventDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.RANDOMSPLITEVENT).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.RANDOMSPLITEVENT,
      Tables.RANDOMSPLITEVENT.ID,
      Tables.RANDOMSPLITEVENT.INPUTDATAFRAMEID,
      Tables.RANDOMSPLITEVENT.RANDOMSEED,
      Tables.RANDOMSPLITEVENT.EXPERIMENTRUN
    );
  }

  @Override
  public void updateQuery(RandomspliteventRecord rec, int iteration) {
    query = query.values(
      maxId,
      DataFrameDuplicator.getInstance(ctx).id(rec.getInputdataframeid(), iteration),
      rec.getRandomseed(),
      ExperimentRunDuplicator.getInstance(ctx).id(rec.getExperimentrun(), iteration)
    );
  }

  private static RandomSplitEventDuplicator instance = null;
  public static RandomSplitEventDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new RandomSplitEventDuplicator(ctx);
    }
    return instance;
  }
}
