package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.server.storage.ExperimentRunDao;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.ExperimentrunRecord;
import modeldb.ExperimentRun;
import modeldb.ExperimentRunEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestExperimentRun {

  @Before
  public void initialize() throws Exception {
    TestBase.clearTables();
  }

  public void testStoreHelper(boolean hasSha) throws Exception {
    // Create an experiment.
    int expId = TestBase.createExperiment().expId;
    String sha = "A1B2C3D4E5";

    ExperimentRun er = new ExperimentRun(-1, expId, "testdesc");
    if (hasSha) {
      er.setSha(sha);
    }
    int expRunId = ExperimentRunDao.store(
      new ExperimentRunEvent(er),
      TestBase.ctx()
    ).getExperimentRunId();

    // Verify that the proper result was stored.
    Assert.assertEquals(1, TestBase.tableSize(Tables.EXPERIMENTRUN));
    ExperimentrunRecord rec = TestBase.ctx().selectFrom(Tables.EXPERIMENTRUN).fetchOne();
    Assert.assertEquals(expId, rec.getExperiment().intValue());
    Assert.assertEquals("testdesc", rec.getDescription());
    if (hasSha) {
      Assert.assertEquals(sha, rec.getSha());
    }
  }

  @Test
  public void testStore() throws Exception {
    testStoreHelper(false /* hasSha */);
  }

  @Test
  public void testStoreWithSha() throws Exception {
    testStoreHelper(true /* hasSha */);
  }

  @Test
  public void testRead() throws Exception {
    TestBase.ProjExpRunTriple triple = TestBase.createExperimentRun();

    Assert.assertEquals(1, TestBase.tableSize(Tables.EXPERIMENTRUN));
    ExperimentRun er = ExperimentRunDao.read(triple.expRunId, TestBase.ctx());
    Assert.assertEquals(triple.expId, er.getExperimentId());
    Assert.assertEquals("Test experiment run", er.getDescription());
    Assert.assertNotEquals(null, er.getCreated());
    Assert.assertEquals(null, er.getSha());

    triple = TestBase.createExperimentRunWithSha();

    Assert.assertEquals(2, TestBase.tableSize(Tables.EXPERIMENTRUN));
    er = ExperimentRunDao.read(triple.expRunId, TestBase.ctx());
    Assert.assertEquals(triple.expId, er.getExperimentId());
    Assert.assertEquals("Test experiment run", er.getDescription());
    Assert.assertNotEquals(null, er.getCreated());
    Assert.assertEquals("A1B2C3D4E5", er.getSha());
  }
}
