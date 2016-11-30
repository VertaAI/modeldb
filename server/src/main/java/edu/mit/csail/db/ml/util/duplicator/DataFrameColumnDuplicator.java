
package edu.mit.csail.db.ml.util.duplicator;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.DataframecolumnRecord;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep4;
import org.jooq.Query;

public class DataFrameColumnDuplicator extends Duplicator<DataframecolumnRecord> {
  InsertValuesStep4<DataframecolumnRecord, Integer, Integer, String, String> query;

  public DataFrameColumnDuplicator(DSLContext ctx) {
    init(ctx);
    ctx.selectFrom(Tables.DATAFRAMECOLUMN).forEach(rec -> updateMaps(rec.getId(), rec));
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public void resetQuery() {
    query = ctx.insertInto(
      Tables.DATAFRAMECOLUMN,
      Tables.DATAFRAMECOLUMN.ID,
      Tables.DATAFRAMECOLUMN.DFID,
      Tables.DATAFRAMECOLUMN.NAME,
      Tables.DATAFRAMECOLUMN.TYPE
    );
  }

  @Override
  public void updateQuery(DataframecolumnRecord rec, int iteration) {
    query = query.values(
      maxId,
      DataFrameDuplicator.getInstance(ctx).id(rec.getDfid(), iteration),
      rec.getName(),
      rec.getType()
    );
  }

  private static DataFrameColumnDuplicator instance = null;
  public static DataFrameColumnDuplicator getInstance(DSLContext ctx) {
    if (instance == null) {
      instance = new DataFrameColumnDuplicator(ctx);
    }
    return instance;
  }
}

