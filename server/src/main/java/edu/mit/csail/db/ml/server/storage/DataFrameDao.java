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

/**
 * This class contains logic for storing and reading DataFrames.
 */
public class DataFrameDao {
  /**
   * Store the given DataFrame in the database.
   * @param df - The DataFrame.
   * @param experimentRunId - The ID of the experiment run that contains the DataFrame.
   * @param ctx - The database context.
   * @return The row in the DataFrame table.
   */
  public static DataframeRecord store(DataFrame df, int experimentRunId, DSLContext ctx) {
    // Check if a DataFrame with the given ID already exists. If so, then just return it.
    DataframeRecord rec = ctx.selectFrom(Tables.DATAFRAME).where(Tables.DATAFRAME.ID.eq(df.id)).fetchOne();
    if (rec != null) {
      return rec;
    }

    // Store an entry in the DataFrame table.
    final DataframeRecord dfRec = ctx.newRecord(DATAFRAME);
    dfRec.setId(null);
    dfRec.setNumrows(df.numRows);
    dfRec.setExperimentrun(experimentRunId);
    dfRec.setTag(df.tag);
    if (df.isSetFilepath()) {
      dfRec.setFilepath(df.getFilepath());
    }
    dfRec.store();

    // Store an entry in DataFrame column for each column of the DataFrame.
    df.getSchema().forEach(col -> {
      DataframecolumnRecord colRec = ctx.newRecord(DATAFRAMECOLUMN);
      colRec.setId(null);
      colRec.setDfid(dfRec.getId());
      colRec.setName(col.name);
      colRec.setType(col.type);
      colRec.store();
      colRec.getId();
    });

    // Store entries in the MetadataKV and DataFrameMetadata tables.
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

    // Return the row that was stored in the DataFrame table.
    return dfRec;
  }

  /**
   * Read the schema for the DataFrame with the given ID.
   * @param dfId - The ID of a DataFrame. It MUST exist in the database.
   * @param ctx - The database context.
   * @return The list of columns in the DataFrame with ID dfId.
   */
  public static List<DataFrameColumn> readSchema(int dfId, DSLContext ctx) {
    return ctx
      .select(Tables.DATAFRAMECOLUMN.NAME, Tables.DATAFRAMECOLUMN.TYPE)
      .from(Tables.DATAFRAMECOLUMN)
      .where(Tables.DATAFRAMECOLUMN.DFID.eq(dfId))
      .fetch()
      .map(r -> new DataFrameColumn(r.value1(), r.value2()));
  }

  /**
   * Read the metadata associated with the DataFrame with the given ID.
   * @param dfId - The ID of the DataFrame. It MUST exist in the database.
   * @param ctx - The database context.
   * @return The list of Metadata key-value pairs for the DataFrame.
   */
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

  /**
   * Read the DataFrame with the given ID.
   * @param dfId - The ID of the DataFrame.
   * @param ctx - The database context.
   * @return The DataFrame with the given ID.
   * @throws ResourceNotFoundException - Thrown if there is no row in the DataFrame table that has a primary key of
   * dfId.
   */
  public static DataFrame read(int dfId, DSLContext ctx) throws ResourceNotFoundException {
    // Attempt to read the row with the given ID. Throw an exception if it cannot be found.
    DataframeRecord rec = ctx.selectFrom(Tables.DATAFRAME).where(Tables.DATAFRAME.ID.eq(dfId)).fetchOne();
    if (rec == null) {
      throw new ResourceNotFoundException(String.format(
        "Could not read DataFrame %d, it doesn't exist",
        dfId
      ));
    }

    // Turn the row into a modeldb.DataFrame object.
    DataFrame df = new DataFrame(rec.getId(), readSchema(dfId, ctx), rec.getNumrows(), rec.getTag());
    df.setFilepath(rec.getFilepath());

    // Read the metadata.
    df.setMetadata(readMetadata(dfId, ctx));

    return df;
  }

  /**
   * Check if the DataFrame table contains a row whose primary key is equal to id.
   * @param id - The primary key value we want to find.
   * @param ctx - The database context.
   * @return Whether there exists a row in the DataFrame table that has a primary key equal to id.
   */
  public static boolean exists(int id, DSLContext ctx) {
    return ctx.selectFrom(Tables.DATAFRAME).where(Tables.DATAFRAME.ID.eq(id)).fetchOne() != null;
  }
}
