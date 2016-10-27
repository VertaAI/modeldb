package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.conf.ModelDbConfig;
import edu.mit.csail.db.ml.server.storage.TransformerDao;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.TransformerRecord;
import modeldb.InvalidFieldException;
import modeldb.ResourceNotFoundException;
import modeldb.Transformer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

// Note that this just tests the Transformer specific methods.
// We will test the model (i.e. Transformers with FitEvent and Features) in TestModel.java.
public class TestTransformer {
  private int expRunId;

  @Before
  public void initialize() throws Exception {
    expRunId = TestBase.reset();
  }


  private void testStore(boolean generateFilePath) throws Exception {
    // Store the Transformer.
    int tid = TransformerDao.store(
      new Transformer(-1, Arrays.asList(1.1, 2.2), "ttype", "tag"),
      expRunId,
      TestBase.ctx(),
      generateFilePath
    ).getId();

    // Verify that stored Transformer is correct.
    Assert.assertEquals(1, TestBase.tableSize(Tables.TRANSFORMER));
    TransformerRecord rec = TestBase.ctx().selectFrom(Tables.TRANSFORMER).fetchOne();
    Assert.assertEquals(tid, rec.getId().intValue());
    Assert.assertEquals("tag", rec.getTag());
    Assert.assertEquals("ttype", rec.getTransformertype());
    Assert.assertEquals("1.1,2.2", rec.getWeights());
    Assert.assertEquals(expRunId, rec.getExperimentrun().intValue());
    if (generateFilePath) {
      Assert.assertTrue(rec.getFilepath().length() > 0);
    } else {
      Assert.assertEquals("", rec.getFilepath());
    }
  }

  @Test
  public void testStore() throws Exception {
    testStore(false);
  }

  @Test
  public void testStoreWithFilepath() throws Exception {
    testStore(true);
  }

  @Test
  public void testGenerateFilePath() throws Exception {
    Assert.assertTrue(TransformerDao.generateFilepath().startsWith(ModelDbConfig.getInstance().fsPrefix));
  }

  @Test
  public void testExists() throws Exception {
    Assert.assertFalse(TransformerDao.exists(1, TestBase.ctx()));
    int tId = TestBase.createTransformer(expRunId, "ttype", "test");
    Assert.assertTrue(TransformerDao.exists(tId, TestBase.ctx()));
    Assert.assertFalse(TransformerDao.exists(tId + 1, TestBase.ctx()));
  }

  @Test
  public void testPathNoTransformer() throws Exception {
    try {
      TransformerDao.path(1, TestBase.ctx());
      Assert.fail();
    } catch (ResourceNotFoundException ex) {} catch (Exception ex) {
      Assert.fail();
    }
  }

  @Test
  public void testPathNoPath() throws Exception {
    int tId = TestBase.createTransformer(expRunId, "ttype", null);
    try {
      TransformerDao.path(tId, TestBase.ctx());
      Assert.fail();
    } catch (InvalidFieldException ex) {} catch (Exception ex) {
      Assert.fail();
    }
  }

  @Test
  public void testPathEmptyPath() throws Exception {
    int tId = TestBase.createTransformer(expRunId, "ttype", "");
    try {
      TransformerDao.path(tId, TestBase.ctx());
      Assert.fail();
    } catch (InvalidFieldException ex) {} catch (Exception ex) {
      Assert.fail();
    }
  }

  @Test
  public void testPath() throws Exception {
    int tId = TestBase.createTransformer(expRunId, "ttype", "mypath");
    Assert.assertEquals("mypath", TransformerDao.path(tId, TestBase.ctx()));
  }
}
