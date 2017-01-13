package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.server.storage.DataFrameDao;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.DataframeRecord;
import jooq.sqlite.gen.tables.records.DataframecolumnRecord;
import jooq.sqlite.gen.tables.records.MetadatakvRecord;
import modeldb.DataFrame;
import modeldb.DataFrameColumn;
import modeldb.MetadataKV;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TestDataFrame {
  private int expRunId;

  @Before
  public void initialize() throws Exception {
    expRunId = TestBase.reset().expRunId;
  }

  private void checkSchema(List<DataFrameColumn> schema) {
    DataFrameColumn firstCol = schema.get(0);
    DataFrameColumn secondCol = schema.get(1);
    if (secondCol.getName().equals("first")) {
      DataFrameColumn tmp = firstCol;
      firstCol = secondCol;
      secondCol = tmp;
    }

    Assert.assertEquals("first", firstCol.getName());
    Assert.assertEquals("string", firstCol.getType());
    Assert.assertEquals("second", secondCol.getName());
    Assert.assertEquals("int", secondCol.getType());
  }

  public void testStoreDataFrameHelper(boolean hasPath) throws Exception {
    DataFrame df = new DataFrame(
      -1,
      Arrays.asList(new DataFrameColumn("first", "string"), new DataFrameColumn("second", "int")),
      10,
      "tag"
    );

    if (hasPath) {
      df.setFilepath("path/to/df");
    }

    // Store the DataFrame.
    int dfId = DataFrameDao.store(
      df,
      expRunId,
      TestBase.ctx()
    ).getId();

    // Verify that the DataFrame record is correct.
    Assert.assertEquals(1, TestBase.tableSize(Tables.DATAFRAME));
    DataframeRecord rec = TestBase.ctx().selectFrom(Tables.DATAFRAME).fetchOne();
    Assert.assertEquals(dfId, rec.getId().intValue());
    Assert.assertEquals("tag", rec.getTag());
    Assert.assertEquals(10, rec.getNumrows().intValue());
    Assert.assertEquals(expRunId, rec.getExperimentrun().intValue());

    if (hasPath) {
      Assert.assertEquals(df.getFilepath(), rec.getFilepath());
    }

    // Verify that the schema is correct.
    Assert.assertEquals(2, TestBase.tableSize(Tables.DATAFRAMECOLUMN));
    List<DataframecolumnRecord> recs = TestBase.ctx()
      .selectFrom(Tables.DATAFRAMECOLUMN)
      .orderBy(Tables.DATAFRAMECOLUMN.NAME.asc())
      .fetch();
    Assert.assertEquals(2, recs.size());
    Assert.assertEquals(dfId, recs.get(0).getDfid().intValue());
    Assert.assertEquals(dfId, recs.get(1).getDfid().intValue());
    Assert.assertArrayEquals(new String[] {"first", "second"}, recs.stream().map(r -> r.getName()).toArray());
    Assert.assertArrayEquals(new String[] {"string", "int"}, recs.stream().map(r -> r.getType()).toArray());
  }

  @Test
  public void testStoreDataFrameWithPath() throws Exception {
    testStoreDataFrameHelper(true /* hasPath */);
  }

  @Test
  public void testStoreDataFrameHelperWithoutPath() throws Exception {
    testStoreDataFrameHelper(false /* hasPath */);
  }

  public DataFrame createDataFrameWithMetadata() {
    DataFrame df = new DataFrame(
      -1,
      Arrays.asList(new DataFrameColumn("first", "string"), new DataFrameColumn("second", "int")),
      10,
      "tag"
    );

    // store the metadata
    MetadataKV kv1 = new MetadataKV("num_cols", "10", "integer");
    MetadataKV kv2 = new MetadataKV("distribution", "gaussian", "string");

    df.setMetadata(Arrays.asList(kv1, kv2));
    return df;
  }

  // TODO: There is a lot of repetition in this test. It should be simplified
  @Test
  public void testStoreDataFrameWithMetadata() throws Exception {
    DataFrame df = createDataFrameWithMetadata();
    // Store the DataFrame.
    int dfId = DataFrameDao.store(
      df,
      expRunId,
      TestBase.ctx()
    ).getId();

    // Verify that the schema is correct.
    Assert.assertEquals(2, TestBase.tableSize(Tables.DATAFRAMECOLUMN));
    List<DataframecolumnRecord> recs = TestBase.ctx()
      .selectFrom(Tables.DATAFRAMECOLUMN)
      .orderBy(Tables.DATAFRAMECOLUMN.NAME.asc())
      .fetch();
    Assert.assertEquals(2, recs.size());
    Assert.assertEquals(dfId, recs.get(0).getDfid().intValue());
    Assert.assertEquals(dfId, recs.get(1).getDfid().intValue());
    Assert.assertArrayEquals(new String[] {"first", "second"}, 
      recs.stream().map(r -> r.getName()).toArray());
    Assert.assertArrayEquals(new String[] {"string", "int"}, 
      recs.stream().map(r -> r.getType()).toArray());

    // verify that metadata is correct
    Assert.assertEquals(2, TestBase.tableSize(Tables.DATAFRAMEMETADATA));
    List<MetadataKV> mrecs = TestBase.ctx()
      .select(Tables.METADATAKV.KEY, Tables.METADATAKV.VALUE, 
        Tables.METADATAKV.VALUETYPE)
      .from(Tables.METADATAKV.join(Tables.DATAFRAMEMETADATA)
        .on(Tables.METADATAKV.ID.eq(Tables.DATAFRAMEMETADATA.METADATAKVID)))
      .where(Tables.DATAFRAMEMETADATA.DFID.eq(dfId))
      .orderBy(Tables.METADATAKV.KEY.asc())
      .fetch()
      .map(rec -> new MetadataKV(rec.value1(), rec.value2(), rec.value3()));

      Assert.assertEquals(2, mrecs.size());
      Assert.assertArrayEquals(new String[] {"distribution", "num_cols"}, 
        mrecs.stream().map(r -> r.getKey()).toArray());
      Assert.assertArrayEquals(new String[] {"gaussian", "10"}, 
        mrecs.stream().map(r -> r.getValue()).toArray());
      Assert.assertArrayEquals(new String[] {"string", "integer"}, 
        mrecs.stream().map(r -> r.getValueType()).toArray());
  }

  public boolean isEqualMetadataKV(MetadataKV kv1, MetadataKV kv2) {
    return kv1.getKey().equals(kv2.getKey()) && kv1.getValue().equals(
      kv2.getValue()) && kv1.getValueType().equals(kv2.getValueType());
  }

  public void checkMetadata(List<MetadataKV> metadata1, 
    List<MetadataKV> metadata2) {
    Assert.assertEquals(metadata1.size(), metadata2.size());
    for (int i = 0; i < metadata1.size(); i++) {
      Assert.assertTrue(isEqualMetadataKV(metadata1.get(i), metadata2.get(i)));
    }
  }

  @Test
  public void testReadMetadata() throws Exception {
    DataFrame df = createDataFrameWithMetadata();
    int dfId = DataFrameDao.store(
      df,
      expRunId,
      TestBase.ctx()
    ).getId();
    List<MetadataKV> metadata = DataFrameDao.readMetadata(dfId, TestBase.ctx());
    checkMetadata(metadata, df.metadata);
  }

  @Test
  public void testReadSchema() throws Exception {
    int dfId = TestBase.createDataFrame(expRunId, 1);
    List<DataFrameColumn> schema = DataFrameDao.readSchema(dfId, TestBase.ctx());
    Assert.assertEquals(2, schema.size());
    checkSchema(schema);
  }

  @Test
  public void testExists() throws Exception {
    Assert.assertFalse(DataFrameDao.exists(1, TestBase.ctx()));
    int dfId = TestBase.createDataFrame(expRunId, 1);
    Assert.assertTrue(DataFrameDao.exists(dfId, TestBase.ctx()));
    Assert.assertFalse(DataFrameDao.exists(dfId + 1, TestBase.ctx()));
  }

  @Test
  public void testRead() throws Exception {
    int dfId = TestBase.createDataFrame(expRunId, 1);
    DataFrame df = DataFrameDao.read(dfId, TestBase.ctx());
    checkSchema(df.schema);
    Assert.assertEquals(1, df.numRows);
    Assert.assertEquals("tag", df.tag);
  }

  @Test
  public void testReadWithMetadata() throws Exception {
    DataFrame df = createDataFrameWithMetadata();
    int dfId = DataFrameDao.store(
      df,
      expRunId,
      TestBase.ctx()
    ).getId();
    DataFrame dfRead = DataFrameDao.read(dfId, TestBase.ctx());
    checkSchema(df.schema);
    checkMetadata(df.metadata, dfRead.metadata);
    Assert.assertEquals(10, df.numRows);
    Assert.assertEquals("tag", df.tag);
  }
}
