package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.server.storage.PipelineEventDao;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.PipelinestageRecord;
import modeldb.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestPipelineEvent {
  private TestBase.ProjExpRunTriple triple;

  @Before
  public void initialize() throws Exception {
    triple = TestBase.reset();
  }

  @Test
  public void testStorePipelineStage() throws Exception {
    // Create a FitEvent corresponding to the Pipeline fit.
    int dfId1 = TestBase.createDataFrame(triple.expRunId, 1);
    int tId1 = TestBase.createTransformer(triple.expRunId, "t1", "");
    int sId1 = TestBase.createTransformerSpec(triple.expRunId, "s1");
    int fitEventId = TestBase.createFitEvent(triple.expRunId, dfId1, sId1, tId1);

    // Create a TransformEvent corresponding to a stage.
    int dfId2 = TestBase.createDataFrame(triple.expRunId, 2);
    int dfId3 = TestBase.createDataFrame(triple.expRunId, 3);
    int tId2 = TestBase.createTransformer(triple.expRunId, "t2", "");
    int transformEventId = TestBase.createTransformEvent(triple.expRunId, tId2, dfId2, dfId3, "", "");

    // Find the event record associated with the TransformEvent above.
    int eventId = TestBase.ctx()
      .select(Tables.EVENT.ID)
      .from(Tables.EVENT)
      .where(Tables.EVENT.EVENTTYPE.eq("transform").and(Tables.EVENT.EVENTID.eq(transformEventId)))
      .fetchOne()
      .value1();

    // Store the PipelineStage.
    PipelineEventDao.storePipelineStage(fitEventId, eventId, 1, false, triple.expRunId, TestBase.ctx());

    // Ensure the PipelineStage table has the proper values.
    Assert.assertEquals(1, TestBase.tableSize(Tables.PIPELINESTAGE));
    PipelinestageRecord rec = TestBase.ctx()
      .selectFrom(Tables.PIPELINESTAGE)
      .fetchOne();
    Assert.assertEquals(fitEventId, rec.getPipelinefitevent().intValue());
    Assert.assertEquals(eventId, rec.getTransformorfitevent().intValue());
    Assert.assertEquals(1, rec.getStagenumber().intValue());
    Assert.assertEquals(triple.expRunId, rec.getExperimentrun().intValue());
  }

  /**
   * Check that there is only one Event record of type "pipeline fit" and that
   * its eventId matches the given ID of the Pipeline's FitEvent.
   */
  private void assertCorrectPipelineFit(int expectedPipelineFitEventId) throws Exception {
    Assert.assertEquals(1, TestBase.ctx()
      .selectFrom(Tables.EVENT)
      .where(Tables.EVENT.EVENTTYPE.eq("pipeline fit"))
      .fetch()
      .size()
    );
    Assert.assertEquals(
      expectedPipelineFitEventId,
      TestBase.ctx()
        .select(Tables.EVENT.EVENTID)
        .from(Tables.EVENT)
        .where(Tables.EVENT.EVENTTYPE.eq("pipeline fit"))
        .fetchOne()
        .value1()
        .intValue()
    );
  }

  private void assertCorrectStage(int pipelineFitEventId,
                                  int stageEventId,
                                  int stageNumber,
                                  boolean isFit) throws Exception {
    PipelinestageRecord rec = TestBase.ctx()
      .selectFrom(Tables.PIPELINESTAGE)
      .where(
        Tables.PIPELINESTAGE.PIPELINEFITEVENT.eq(pipelineFitEventId)
        .and(Tables.PIPELINESTAGE.STAGENUMBER.eq(stageNumber))
        .and(Tables.PIPELINESTAGE.ISFIT.eq(isFit ? 1 : 0))
        .and(Tables.PIPELINESTAGE.TRANSFORMORFITEVENT.eq(stageEventId))
      )
      .fetchOne();
    Assert.assertEquals(pipelineFitEventId, rec.getPipelinefitevent().intValue());
    Assert.assertEquals(stageEventId, rec.getTransformorfitevent().intValue());
    Assert.assertEquals(stageNumber, rec.getStagenumber().intValue());
    Assert.assertEquals(triple.expRunId, rec.getExperimentrun().intValue());
  }

  @Test
  public void testStorePipelineOneTransformStage() throws Exception {
    // Create the PipelineEvent.
    FitEvent pipelineFit = StructFactory.makeFitEvent();
    TransformEvent te = StructFactory.makeTransformEvent();
    PipelineEvent pe = new PipelineEvent(
      pipelineFit,
      Collections.singletonList(new PipelineTransformStage(1, te)),
      Collections.emptyList(),
      triple.expRunId
    );

    // Store the PipelineEvent.
    PipelineEventResponse resp = PipelineEventDao.store(pe, TestBase.ctx());

    // Check table sizes.
    Assert.assertEquals(1, TestBase.tableSize(Tables.PIPELINESTAGE));
    Assert.assertEquals(1, TestBase.tableSize(Tables.TRANSFORMEVENT));
    Assert.assertEquals(1, TestBase.tableSize(Tables.FITEVENT));

    // Ensure "pipeline fit" event points to correct FitEvent.
    assertCorrectPipelineFit(resp.pipelineFitResponse.fitEventId);

    // Verify the TransformStage.
    assertCorrectStage(
      resp.pipelineFitResponse.fitEventId,
      resp.transformStagesResponses.get(0).eventId,
      1,
      false
    );
  }

  @Test
  public void testStorePipelineOneFitStage() throws Exception {
    // Create the PipelineEvent.
    FitEvent pipelineFit = StructFactory.makeFitEvent();
    FitEvent fe = StructFactory.makeFitEvent();
    PipelineEvent pe = new PipelineEvent(
      pipelineFit,
      Collections.emptyList(),
      Collections.singletonList(new PipelineFitStage(1, fe)),
      triple.expRunId
    );

    // Store the PipelineEvent.
    PipelineEventResponse resp = PipelineEventDao.store(pe, TestBase.ctx());

    // Check table sizes.
    Assert.assertEquals(1, TestBase.tableSize(Tables.PIPELINESTAGE));
    Assert.assertEquals(0, TestBase.tableSize(Tables.TRANSFORMEVENT));
    Assert.assertEquals(2, TestBase.tableSize(Tables.FITEVENT));

    // Ensure "pipeline fit" event points to correct FitEvent.
    assertCorrectPipelineFit(resp.pipelineFitResponse.fitEventId);

    // Verify the FitStage.
    assertCorrectStage(
      resp.pipelineFitResponse.fitEventId,
      resp.fitStagesResponses.get(0).eventId,
      1,
      true
    );
  }

  @Test
  public void testStorePipelineFitTransformFit() throws Exception {
    // Create the PipelineEvent.
    // The pipeline will consist of (Estimator 1) -> (Transformer 1) -> (Estimator 2).
    // Thus, the PipelineEvent will have:
    // (Stage 1, Fit of Estimator 1), (Stage 1, Transform of model produced by Estimator 1),
    // (Stage 2, Transform of Transformer 1)
    // (Stage 3, Fit of Estimator 2), (Stage 3, Transform of model produced by Estimator 2)
    FitEvent pipelineFit = StructFactory.makeFitEvent();
    FitEvent fe1 = StructFactory.makeFitEvent();
    FitEvent fe2 = StructFactory.makeFitEvent();
    TransformEvent te1 = StructFactory.makeTransformEvent();
    TransformEvent te2 = StructFactory.makeTransformEvent();
    TransformEvent te3 = StructFactory.makeTransformEvent();
    PipelineEvent pe = new PipelineEvent(
      pipelineFit,
      Arrays.asList(
        new PipelineTransformStage(1, te1),
        new PipelineTransformStage(2, te2),
        new PipelineTransformStage(3, te3)
      ),
      Arrays.asList(
        new PipelineFitStage(1, fe1),
        new PipelineFitStage(3, fe2)
      ),
      triple.expRunId
    );

    // Store the PipelineEvent.
    PipelineEventResponse resp = PipelineEventDao.store(pe, TestBase.ctx());

    // Check table sizes.
    Assert.assertEquals(5, TestBase.tableSize(Tables.PIPELINESTAGE));
    Assert.assertEquals(3, TestBase.tableSize(Tables.TRANSFORMEVENT));
    Assert.assertEquals(3, TestBase.tableSize(Tables.FITEVENT));

    // Ensure "pipeline fit" event points to correct FitEvent.
    assertCorrectPipelineFit(resp.pipelineFitResponse.fitEventId);

    // Verify the stages.
    assertCorrectStage(resp.pipelineFitResponse.fitEventId, resp.transformStagesResponses.get(0).eventId, 1, false);
    assertCorrectStage(resp.pipelineFitResponse.fitEventId, resp.transformStagesResponses.get(1).eventId, 2, false);
    assertCorrectStage(resp.pipelineFitResponse.fitEventId, resp.transformStagesResponses.get(2).eventId, 3, false);
    assertCorrectStage(resp.pipelineFitResponse.fitEventId, resp.fitStagesResponses.get(0).eventId, 1, true);
    assertCorrectStage(resp.pipelineFitResponse.fitEventId, resp.fitStagesResponses.get(1).eventId, 3, true);
  }

  @Test
  public void testStorePipelineTransformEvent() throws Exception {
    TransformEvent te1 = StructFactory.makeTransformEvent();
    TransformEvent te2 = StructFactory.makeTransformEvent();
    TransformEvent te3 = StructFactory.makeTransformEvent();

    List<TransformEventResponse> resp =
      PipelineEventDao.storePipelineTransformEvent(Arrays.asList(te1, te2, te3), TestBase.ctx());

    // Check table sizes.
    Assert.assertEquals(3, TestBase.tableSize(Tables.TRANSFORMEVENT));
    Assert.assertEquals(4, TestBase.tableSize(Tables.DATAFRAME));
    Assert.assertEquals(3, TestBase.tableSize(Tables.TRANSFORMER));

    // Verify the dependencies between DataFrames.
    Assert.assertEquals(3, resp.size());
    Assert.assertEquals(resp.get(0).newDataFrameId, resp.get(1).oldDataFrameId);
    Assert.assertEquals(resp.get(1).newDataFrameId, resp.get(2).oldDataFrameId);
  }

  @Test
  public void testStorePipelineTranformFitTransform() throws Exception {
    // Create the PipelineEvent.
    // The pipeline will consist of (Transformer 1) -> (Estimator 1) -> (Transformer 2).
    // Thus, the PipelineEvent will have:
    // (Stage 1, Transform of Transformer 1)
    // (Stage 2, Fit of Estimator 1), (Stage 2, Transform of model produced by Estimator 2)
    // (Stage 3, Transform of Transformer 2)
    FitEvent pipelineFit = StructFactory.makeFitEvent();
    FitEvent fe1 = StructFactory.makeFitEvent();
    TransformEvent te1 = StructFactory.makeTransformEvent();
    TransformEvent te2 = StructFactory.makeTransformEvent();
    TransformEvent te3 = StructFactory.makeTransformEvent();
    PipelineEvent pe = new PipelineEvent(
      pipelineFit,
      Arrays.asList(
        new PipelineTransformStage(1, te1),
        new PipelineTransformStage(2, te2),
        new PipelineTransformStage(3, te3)
      ),
      Collections.singletonList(new PipelineFitStage(2, fe1)),
      triple.expRunId
    );

    // Store the PipelineEvent.
    PipelineEventResponse resp = PipelineEventDao.store(pe, TestBase.ctx());

    // Check table sizes.
    Assert.assertEquals(4, TestBase.tableSize(Tables.PIPELINESTAGE));
    Assert.assertEquals(3, TestBase.tableSize(Tables.TRANSFORMEVENT));
    Assert.assertEquals(2, TestBase.tableSize(Tables.FITEVENT));

    // Ensure "pipeline fit" event points to correct FitEvent.
    assertCorrectPipelineFit(resp.pipelineFitResponse.fitEventId);

    // Verify the stages.
    assertCorrectStage(resp.pipelineFitResponse.fitEventId, resp.transformStagesResponses.get(0).eventId, 1, false);
    assertCorrectStage(resp.pipelineFitResponse.fitEventId, resp.transformStagesResponses.get(1).eventId, 2, false);
    assertCorrectStage(resp.pipelineFitResponse.fitEventId, resp.transformStagesResponses.get(2).eventId, 3, false);
    assertCorrectStage(resp.pipelineFitResponse.fitEventId, resp.fitStagesResponses.get(0).eventId, 2, true);
  }
}
