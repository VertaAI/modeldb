package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.server.storage.TransformEventDao;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.EventRecord;
import jooq.sqlite.gen.tables.records.TransformeventRecord;
import modeldb.TransformEvent;
import modeldb.TransformEventResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TestTransformEvent {
  TestBase.ProjExpRunTriple triple;

  @Before
  public void initialize() throws Exception {
    triple = TestBase.reset();
  }

  @Test
  public void testStore() throws Exception {
    TransformEvent te = StructFactory.makeTransformEvent();
    TransformEventResponse resp = TransformEventDao.store(te, TestBase.ctx());

    // Verify that we've stored entries in each table.
    Assert.assertEquals(1, TestBase.tableSize(Tables.TRANSFORMEVENT));
    Assert.assertEquals(2, TestBase.tableSize(Tables.DATAFRAME));
    Assert.assertEquals(1, TestBase.tableSize(Tables.TRANSFORMER));
    Assert.assertEquals(1, TestBase.tableSize(Tables.EVENT));

    // Ensure the TransformEvent is valid.
    TransformeventRecord rec = TestBase.ctx().selectFrom(Tables.TRANSFORMEVENT).fetchOne();
    Assert.assertEquals(rec.getOlddf().intValue(), resp.oldDataFrameId);
    Assert.assertEquals(rec.getNewdf().intValue(), resp.newDataFrameId);
    Assert.assertEquals(rec.getTransformer().intValue(), resp.transformerId);
    Assert.assertTrue(rec.getInputcolumns().contains("inCol1"));
    Assert.assertTrue(rec.getOutputcolumns().contains("outCol1"));

    // Ensure the that the Event record is correct.
    EventRecord evRec = TestBase.ctx().selectFrom(Tables.EVENT).fetchOne();
    Assert.assertEquals(evRec.getId().intValue(), resp.eventId);
    Assert.assertEquals("transform", evRec.getEventtype());
  }

  @Test
  public void testRead() throws Exception {
    int df1 = TestBase.createDataFrame(triple.expRunId, 101);
    int df2 = TestBase.createDataFrame(triple.expRunId, 102);
    int m1 = TestBase.createTransformer(triple.expRunId, "t1", "");

    int df3 = TestBase.createDataFrame(triple.expRunId, 103);
    int df4 = TestBase.createDataFrame(triple.expRunId, 104);
    int m2 = TestBase.createTransformer(triple.expRunId, "t2", "");

    int te1 = TestBase.createTransformEvent(triple.expRunId, m1, df1, df2, "incol1,incol2", "outcol1");
    int te2 = TestBase.createTransformEvent(triple.expRunId, m2, df3, df4, "incol4", "outcol2,outcol3");

    List<TransformEvent> tes = TransformEventDao.read(Arrays.asList(te1, te2), TestBase.ctx());

    Assert.assertEquals(2, tes.size());
    Assert.assertEquals(triple.expRunId, tes.get(0).experimentRunId);
    Assert.assertEquals(triple.expRunId, tes.get(1).experimentRunId);

    // Verify models.
    Assert.assertEquals(m1, tes.get(0).transformer.id);
    Assert.assertEquals(m2, tes.get(1).transformer.id);
    Assert.assertEquals(2, tes.get(0).inputColumns.size());
    Assert.assertEquals(1, tes.get(1).inputColumns.size());
    Assert.assertEquals(1, tes.get(0).outputColumns.size());
    Assert.assertEquals(2, tes.get(1).outputColumns.size());
    Assert.assertEquals("t1", tes.get(0).transformer.transformerType);
    Assert.assertEquals("t2", tes.get(1).transformer.transformerType);

    // Verify input DataFrame.
    Assert.assertEquals(df1, tes.get(0).oldDataFrame.id);
    Assert.assertEquals(df3, tes.get(1).oldDataFrame.id);
    Assert.assertEquals(0, tes.get(0).oldDataFrame.schema.size());
    Assert.assertEquals(0, tes.get(1).oldDataFrame.schema.size());
    Assert.assertEquals(101, tes.get(0).oldDataFrame.numRows);
    Assert.assertEquals(103, tes.get(1).oldDataFrame.numRows);

    // Verify the output DataFrame.
    Assert.assertEquals(df2, tes.get(0).newDataFrame.id);
    Assert.assertEquals(df4, tes.get(1).newDataFrame.id);
    Assert.assertEquals(0, tes.get(0).newDataFrame.schema.size());
    Assert.assertEquals(0, tes.get(1).newDataFrame.schema.size());
    Assert.assertEquals(102, tes.get(0).newDataFrame.numRows);
    Assert.assertEquals(104, tes.get(1).newDataFrame.numRows);
  }
}
