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

import java.nio.file.Paths;

// Note that this just tests the Transformer specific methods.
// We will test the model (i.e. Transformers with FitEvent and Features) in TestModel.java.
public class TestTransformer {
  private int expRunId;

  @Before
  public void initialize() throws Exception {
    expRunId = TestBase.reset().expRunId;
  }


  @Test
  public void testStore() throws Exception {
    // Store the Transformer.
    Transformer t = new Transformer(-1, "ttype", "tag");

    int tid = TransformerDao.store(
      t,
      expRunId,
      TestBase.ctx()
    ).getId();

    // Verify that stored Transformer is correct.
    Assert.assertEquals(1, TestBase.tableSize(Tables.TRANSFORMER));
    TransformerRecord rec = TestBase.ctx().selectFrom(Tables.TRANSFORMER).fetchOne();
    Assert.assertEquals(tid, rec.getId().intValue());
    Assert.assertEquals("tag", rec.getTag());
    Assert.assertEquals("ttype", rec.getTransformertype());
    Assert.assertEquals(expRunId, rec.getExperimentrun().intValue());

  }

  @Test
  public void testGenerateFilePath() throws Exception {
    Assert.assertTrue(TransformerDao.generateFilepath().startsWith(ModelDbConfig.getInstance().fsPrefix));
  }

  @Test
  public void testGetFilePathNewTransformer() throws Exception {
    Assert.assertEquals(0, TestBase.tableSize(Tables.TRANSFORMER));
    String filepath = TransformerDao.getFilePath(StructFactory.makeTransformer(), expRunId, "myfile", TestBase.ctx());
    Assert.assertTrue(filepath.endsWith("myfile"));
    Assert.assertEquals(1, TestBase.tableSize(Tables.TRANSFORMER));
  }

  @Test
  public void testGetFilepathExistingTransformer() throws Exception {
    int tid = TestBase.createTransformer(expRunId, "ttype", null);
    Assert.assertEquals(1, TestBase.tableSize(Tables.TRANSFORMER));
    String filepath = TransformerDao.getFilePath(new Transformer(tid, null, null), expRunId, "", TestBase.ctx());
    Assert.assertTrue(filepath.length() > 0);
    Assert.assertEquals(1, TestBase.tableSize(Tables.TRANSFORMER));
  }

  @Test
  public void testGetFilepathForTransformerWithFilepath() throws Exception {
    int tid = TestBase.createTransformer(expRunId, "ttype", "test");
    Assert.assertEquals(1, TestBase.tableSize(Tables.TRANSFORMER));
    String filepath = TransformerDao.getFilePath(new Transformer(tid, null, null), expRunId, "ignore", TestBase.ctx());
    Assert.assertEquals("test", filepath);
    Assert.assertEquals(1, TestBase.tableSize(Tables.TRANSFORMER));
  }

  @Test
  public void testGetFilepathNonExistentTransformer() throws Exception {
    try {
      TransformerDao.getFilePath(new Transformer(1, null, null), expRunId, "", TestBase.ctx());
      Assert.fail();
    } catch (ResourceNotFoundException ex) {} catch (Exception ex) {
      ex.printStackTrace();
      Assert.fail();
    }
  }

  @Test
  public void testGetFilepathDuplicate() throws Exception {
    TestBase.createTransformer(expRunId, "ttype", Paths.get(TestBase.getConfig().fsPrefix, "myfile").toString());
    String filepath = TransformerDao.getFilePath(StructFactory.makeTransformer(), expRunId, "myfile", TestBase.ctx());
    Assert.assertFalse(filepath.endsWith("myfile"));
    Assert.assertTrue(filepath.contains("myfile"));
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
