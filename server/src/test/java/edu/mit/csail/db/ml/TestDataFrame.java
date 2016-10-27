package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.server.storage.DataFrameDao;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.DataframeRecord;
import jooq.sqlite.gen.tables.records.DataframecolumnRecord;
import modeldb.DataFrame;
import modeldb.DataFrameColumn;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TestDataFrame {
  private int expRunId;

  @Before
  public void initialize() throws Exception {
    expRunId = TestBase.reset();
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

  @Test
  public void testStoreDataFrame() throws Exception {
    // Store the DataFrame.
    int dfId = DataFrameDao.store(
      new DataFrame(
        -1,
        Arrays.asList(new DataFrameColumn("first", "string"), new DataFrameColumn("second", "int")),
        10,
        "tag"
      ),
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
}
