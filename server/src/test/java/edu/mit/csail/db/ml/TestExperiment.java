package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.server.storage.ExperimentDao;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.ExperimentRecord;
import modeldb.Experiment;
import modeldb.ExperimentEvent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestExperiment {

  @Before
  public void initialize() throws Exception {
    TestBase.clearTables();
  }

  @Test
  public void testStoreDefaultWithEmpty() throws Exception {
    // Create the default experiment.
    int projId = TestBase.createProject().projId;
    int expId = ExperimentDao.store(
      new ExperimentEvent(
        new Experiment(-1, projId, "testname", "testdesc", true)
      ),
      TestBase.ctx()
    ).getExperimentId();

    Assert.assertEquals(-1, expId);
  }

  @Test
  public void testStoreDefaultWithoutEmpty() throws Exception {
    // Create an experiment.
    TestBase.ProjExpRunTriple triple = TestBase.createExperiment();

    // Get the default experiment.
    int expId = ExperimentDao.store(
      new ExperimentEvent(
        new Experiment(-1, triple.projId, "testname", "testdesc", true)
      ),
      TestBase.ctx()
    ).getExperimentId();

    // Verify that we didn't actually store another experiment and that we received
    // the default experiment ID as a response..
    Assert.assertEquals(1, TestBase.tableSize(Tables.EXPERIMENT));
    Assert.assertEquals(triple.expId, expId);
  }

  @Test
  public void testStore() throws Exception {
    // Create a project.
    int projId = TestBase.createProject().projId;

    // Store an experiment.
    int expId = ExperimentDao.store(
      new ExperimentEvent(
        new Experiment(-1, projId, "testname", "testdesc", false)
      ),
      TestBase.ctx()
    ).getExperimentId();

    // Verify that the proper result was stored.
    Assert.assertEquals(1, TestBase.tableSize(Tables.EXPERIMENT));
    ExperimentRecord rec = TestBase.ctx().selectFrom(Tables.EXPERIMENT).fetchOne();
    Assert.assertEquals(projId, rec.getProject().intValue());
    Assert.assertEquals("testname", rec.getName());
    Assert.assertEquals("testdesc", rec.getDescription());
  }

  @Test
  public void testRead() throws Exception {
    TestBase.ProjExpRunTriple triple = TestBase.createExperiment();

    Assert.assertEquals(1, TestBase.tableSize(Tables.EXPERIMENT));
    Experiment exp = ExperimentDao.read(triple.expId, TestBase.ctx());
    Assert.assertEquals(triple.projId, exp.getProjectId());
    Assert.assertEquals("Test experiment", exp.getName());
    Assert.assertEquals("Test experiment description", exp.getDescription());
    Assert.assertTrue(exp.isDefault);

    triple = TestBase.createExperiment(triple.projId);

    Assert.assertEquals(2, TestBase.tableSize(Tables.EXPERIMENT));
    exp = ExperimentDao.read(triple.expId, TestBase.ctx());
    Assert.assertEquals(triple.projId, exp.getProjectId());
    Assert.assertEquals("Test experiment", exp.getName());
    Assert.assertEquals("Test experiment description", exp.getDescription());
    Assert.assertFalse(exp.isDefault);
  }
}
