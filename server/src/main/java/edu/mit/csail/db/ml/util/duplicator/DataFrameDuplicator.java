package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.DataframeRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep5;
import org.jooq.Query;

public class DataFrameDuplicator extends Duplicator<DataframeRecord> {
  InsertValuesStep5<DataframeRecord, Integer, String, Integer, Integer, String> query;

  public DataFrameDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.DATAFRAME).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.DATAFRAME,
      Tables.DATAFRAME.ID,
      Tables.DATAFRAME.TAG,
      Tables.DATAFRAME.NUMROWS,
      Tables.DATAFRAME.EXPERIMENTRUN,
      Tables.DATAFRAME.FILEPATH
    );
  }

  @Override
  public void updateQuery(DataframeRecord rec, int iteration) {
    query = query.values(
      maxId,
      rec.getTag(),
      rec.getNumrows(),
      ExperimentRunDuplicator.getInstance(ctx).id(rec.getExperimentrun(), iteration),
      rec.getFilepath()
    );
  }

  private static DataFrameDuplicator instance = null;
  public static DataFrameDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new DataFrameDuplicator(ctx);
    }
    return instance;
  }
}
