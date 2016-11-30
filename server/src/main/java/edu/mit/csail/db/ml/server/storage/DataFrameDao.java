package edu.mit.csail.db.ml.server.storage;

import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.DataframeRecord;
import jooq.sqlite.gen.tables.records.DataframecolumnRecord;
import jooq.sqlite.gen.tables.records.MetadatakvRecord;
import jooq.sqlite.gen.tables.records.DataframemetadataRecord;
import modeldb.DataFrame;
import modeldb.DataFrameColumn;
import modeldb.MetadataKV;
import modeldb.ResourceNotFoundException;
import org.jooq.DSLContext;

import java.util.List;

import static jooq.sqlite.gen.Tables.DATAFRAME;
import static jooq.sqlite.gen.Tables.DATAFRAMECOLUMN;
import static jooq.sqlite.gen.Tables.METADATAKV;
import static jooq.sqlite.gen.Tables.DATAFRAMEMETADATA;

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
    if (df.isSetFilepath()) {
      dfRec.setFilepath(df.getFilepath());
    }
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

    // store the metadata
    if (df.isSetMetadata()) {
      df.getMetadata().forEach(metadata -> {
        MetadatakvRecord mRec = ctx.newRecord(METADATAKV);
        mRec.setId(null);
        mRec.setKey(metadata.key);
        mRec.setValue(metadata.value);
        mRec.setValuetype(metadata.valueType);
        mRec.store();

        DataframemetadataRecord dfmRec = ctx.newRecord(DATAFRAMEMETADATA);
        dfmRec.setId(null);
        dfmRec.setDfid(dfRec.getId());
        dfmRec.setMetadatakvid(mRec.getId());
        dfmRec.store();
      });
    }

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

  public static List<MetadataKV> readMetadata(int dfId, DSLContext ctx) {
    return ctx
      .select(Tables.METADATAKV.KEY, Tables.METADATAKV.VALUE, 
        Tables.METADATAKV.VALUETYPE)
      .from(Tables.METADATAKV.join(Tables.DATAFRAMEMETADATA)
        .on(Tables.METADATAKV.ID.eq(Tables.DATAFRAMEMETADATA.METADATAKVID)))
      .where(Tables.DATAFRAMEMETADATA.DFID.eq(dfId))
      .fetch()
      .map(kv -> new MetadataKV(kv.value1(), kv.value2(), kv.value3()));
  }

  public static DataFrame read(int dfId, DSLContext ctx) throws ResourceNotFoundException {
    DataframeRecord rec = ctx.selectFrom(Tables.DATAFRAME).where(Tables.DATAFRAME.ID.eq(dfId)).fetchOne();
    if (rec == null) {
      throw new ResourceNotFoundException(String.format(
        "Could not read DataFrame %d, it doesn't exist",
        dfId
      ));
    }
    DataFrame df = new DataFrame(rec.getId(), readSchema(dfId, ctx), rec.getNumrows(), rec.getTag());
    df.setFilepath(rec.getFilepath());
    // read the metadata
    df.setMetadata(readMetadata(dfId, ctx));
    return df;
  }

  public static boolean exists(int id, DSLContext ctx) {
    return ctx.selectFrom(Tables.DATAFRAME).where(Tables.DATAFRAME.ID.eq(id)).fetchOne() != null;
  }
}
