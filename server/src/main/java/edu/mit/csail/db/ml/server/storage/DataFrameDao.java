package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.DataframeRecord;
import jooq.sqlite.gen.tables.records.DataframecolumnRecord;
import modeldb.DataFrame;
import modeldb.DataFrameColumn;
import modeldb.ResourceNotFoundException;
import org.jooq.DSLContext;

import java.util.List;

import static jooq.sqlite.gen.Tables.DATAFRAME;
import static jooq.sqlite.gen.Tables.DATAFRAMECOLUMN;

public class DataFrameDao {
  public static DataframeRecord store(DataFrame df, int experimentId, DSLContext ctx) {
    DataframeRecord rec = ctx.selectFrom(Tables.DATAFRAME).where(Tables.DATAFRAME.ID.eq(df.id)).fetchOne();
    if (rec != null) {
      return rec;
    }

    final DataframeRecord dfRec = ctx.newRecord(DATAFRAME);
    dfRec.setId(null);
    dfRec.setNumrows(df.numRows);
    dfRec.setExperimentrun(experimentId);
    dfRec.setTag(df.tag);
    dfRec.store();

    df.getSchema().forEach(col -> {
      DataframecolumnRecord colRec = ctx.newRecord(DATAFRAMECOLUMN);
      colRec.setId(null);
      colRec.setDfid(dfRec.getId());
      colRec.setName(col.name);
      colRec.setType(col.type);
      colRec.store();
      colRec.getId();
    });

    return dfRec;
  }

  public static List<DataFrameColumn> readSchema(int dfId, DSLContext ctx) {
    return ctx
      .select(Tables.DATAFRAMECOLUMN.NAME, Tables.DATAFRAMECOLUMN.TYPE)
      .from(Tables.DATAFRAMECOLUMN)
      .where(Tables.DATAFRAMECOLUMN.DFID.eq(dfId))
      .fetch()
      .map(r -> new DataFrameColumn(r.value1(), r.value2()));
  }

  public static DataFrame read(int dfId, DSLContext ctx) throws ResourceNotFoundException {
    DataframeRecord rec = ctx.selectFrom(Tables.DATAFRAME).where(Tables.DATAFRAME.ID.eq(dfId)).fetchOne();
    if (rec == null) {
      throw new ResourceNotFoundException(String.format(
        "Could not readHyperparameters DataFrame %d, it doesn't exist",
        dfId
      ));
    }
    return new DataFrame(rec.getId(), readSchema(dfId, ctx), rec.getNumrows(), rec.getTag());
  }

  public static boolean exists(int id, DSLContext ctx) {
    return ctx.selectFrom(Tables.DATAFRAME).where(Tables.DATAFRAME.ID.eq(id)).fetchOne() != null;
  }
}
