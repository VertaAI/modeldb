package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.server.storage.FitEventDao;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.EventRecord;
import jooq.sqlite.gen.tables.records.FeatureRecord;
import jooq.sqlite.gen.tables.records.FiteventRecord;
import modeldb.FitEvent;
import modeldb.FitEventResponse;
import modeldb.ResourceNotFoundException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.mongodb.DBObject;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestFitEvent {
  private TestBase.ProjExpRunTriple triple;

  @Before
  public void initialize() throws Exception {
    triple = TestBase.reset();
  }

  public void testStore(boolean isPipeline) throws Exception {
    FitEvent fe = StructFactory.makeFitEvent();
    FitEventResponse resp = FitEventDao.store(fe, TestBase.ctx(), isPipeline);

    // Verify that we've stored entries in each table.
    Assert.assertEquals(1, TestBase.tableSize(Tables.FITEVENT));
    Assert.assertEquals(1, TestBase.tableSize(Tables.DATAFRAME));
    Assert.assertEquals(1, TestBase.tableSize(Tables.TRANSFORMER));
    Assert.assertEquals(1, TestBase.tableSize(Tables.TRANSFORMERSPEC));
    Assert.assertEquals(1, TestBase.tableSize(Tables.EVENT));

    // Ensure the FitEvent record is valid.
    FiteventRecord rec = TestBase.ctx().selectFrom(Tables.FITEVENT).fetchOne();
    Assert.assertEquals(rec.getId().intValue(), resp.fitEventId);
    Assert.assertEquals(rec.getDf().intValue(), resp.dfId);
    Assert.assertEquals(rec.getTransformerspec().intValue(), resp.specId);
    Assert.assertEquals(rec.getTransformer().intValue(), resp.modelId);
    Assert.assertTrue(rec.getLabelcolumns().contains("labCol1"));
    Assert.assertTrue(rec.getPredictioncolumns().contains("predCol2"));
    Assert.assertNotEquals(rec.getProblemtype(), "undefined");

    // Ensure that the EventRecord is correct.
    EventRecord evRec = TestBase.ctx().selectFrom(Tables.EVENT).fetchOne();
    Assert.assertEquals(evRec.getId().intValue(), resp.eventId);
    Assert.assertEquals(isPipeline ? "pipeline fit" : "fit", evRec.getEventtype());

    // Verify that the features have been stored.
    Assert.assertEquals(3, TestBase.tableSize(Tables.FEATURE));
    List<String> features = TestBase.ctx()
      .selectFrom(Tables.FEATURE)
      .fetch()
      .map(FeatureRecord::getName)
      .stream()
      .collect(Collectors.toList());
    Assert.assertTrue(features.contains("featCol1"));
    Assert.assertTrue(features.contains("featCol2"));
  }

  @Test
  public void testStorePipeline() throws Exception {
    testStore(true);
  }

  @Test
  public void testNonPipeline() throws Exception {
    testStore(false);
  }

  @Test
  public void testNumRowsForModel() throws Exception {
    int dfId = TestBase.createDataFrame(triple.expRunId, 99);
    int modelId = TestBase.createTransformer(triple.expRunId, "linreg", "");
    int specId = TestBase.createTransformerSpec(triple.expRunId, "linreg");

    TestBase.createFitEvent(triple.expRunId, dfId, specId, modelId);

    Assert.assertEquals(99, FitEventDao.getNumRowsForModel(modelId, TestBase.ctx()));
  }

  @Test
  public void testNumRowsForModelNonExistant() throws Exception {
    try {
      FitEventDao.getNumRowsForModel(100, TestBase.ctx());
      Assert.fail();
    } catch (ResourceNotFoundException ex) {} catch (Exception ex) {
      Assert.fail();
    }
  }

  @Test
  public void testNumRowsForModels() throws Exception {
    int dfId = TestBase.createDataFrame(triple.expRunId, 99);
    int modelId = TestBase.createTransformer(triple.expRunId, "linreg", "");
    int specId = TestBase.createTransformerSpec(triple.expRunId, "linreg");
    int dfId2 = TestBase.createDataFrame(triple.expRunId, 102);
    int modelId2 = TestBase.createTransformer(triple.expRunId, "linreg", "");
    int specId2 = TestBase.createTransformerSpec(triple.expRunId, "linreg");

    TestBase.createFitEvent(triple.expRunId, dfId, specId, modelId);
    TestBase.createFitEvent(triple.expRunId, dfId2, specId2, modelId2);

    List<Integer> numRows = FitEventDao.getNumRowsForModels(Arrays.asList(modelId, modelId2), TestBase.ctx());

    Assert.assertEquals(99, numRows.get(0).intValue());
    Assert.assertEquals(102, numRows.get(1).intValue());
  }

  @Test
  public void testParentDf() throws Exception {
    int dfId = TestBase.createDataFrame(triple.expRunId, 99);
    int modelId = TestBase.createTransformer(triple.expRunId, "linreg", "");
    int specId = TestBase.createTransformerSpec(triple.expRunId, "linreg");

    TestBase.createFitEvent(triple.expRunId, dfId, specId, modelId);

    Assert.assertEquals(dfId, FitEventDao.getParentDfId(modelId, TestBase.ctx()));
  }
}
