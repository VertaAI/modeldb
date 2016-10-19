package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.DataframeRecord;
import jooq.sqlite.gen.tables.records.DataframecolumnRecord;
import modeldb.DataFrame;
import org.jooq.DSLContext;

import static jooq.sqlite.gen.Tables.DATAFRAME;
import static jooq.sqlite.gen.Tables.DATAFRAMECOLUMN;

public class DataFrameDao {
  public static DataframeRecord store(DataFrame df, int projId, int experimentId, DSLContext ctx) {
    DataframeRecord rec = ctx.selectFrom(Tables.DATAFRAME).where(Tables.DATAFRAME.ID.eq(df.id)).fetchOne();
    if (rec != null) {
      return rec;
    }

    final DataframeRecord dfRec = ctx.newRecord(DATAFRAME);
    dfRec.setId(null);
    dfRec.setNumrows(df.numRows);
    dfRec.setProject(projId);
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
}
