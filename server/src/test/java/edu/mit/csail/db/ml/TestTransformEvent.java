package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.client.StructFactory;
import edu.mit.csail.db.ml.server.storage.TransformEventDao;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.EventRecord;
import jooq.sqlite.gen.tables.records.TransformeventRecord;
import modeldb.TransformEvent;
import modeldb.TransformEventResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestTransformEvent {
  TestBase.ProjExpRunTriple triple;

  @Before
  public void initialize() throws Exception {
    triple = TestBase.reset();
  }

  public void testStore(boolean generateFilePath) throws Exception {
    TransformEvent te = StructFactory.makeTransformEvent();
    TransformEventResponse resp = TransformEventDao.store(te, TestBase.ctx(), generateFilePath);

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

    // Verify that the Transformer has a filepath.
    String filepath = TestBase.ctx().selectFrom(Tables.TRANSFORMER).fetchOne().getFilepath();
    if (generateFilePath) {
      Assert.assertTrue(filepath.length() > 0);
    } else {
      Assert.assertEquals("", filepath);
    }
  }

  @Test
  public void testStoreNoFilepath() throws Exception {
    testStore(false);
  }

  @Test
  public void testStoreFilepath() throws Exception {
    testStore(true);
  }
}
