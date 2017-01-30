package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.server.storage.CrossValidationEventDao;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.CrossvalidationeventRecord;
import jooq.sqlite.gen.tables.records.MetriceventRecord;
import modeldb.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class TestCrossValidation {
  private TestBase.ProjExpRunTriple triple;

  @Before
  public void initialize() throws Exception {
    triple = TestBase.reset();
  }

  @Test
  public void testStoreCrossValidation() throws Exception {
    CrossValidationEvent cve = new CrossValidationEvent(
      StructFactory.makeDataFrame(),
      StructFactory.makeTransformerSpec(),
      1L,
      "precision",
      Arrays.asList("lab1", "lab2"),
      Arrays.asList("pred1", "pred2"),
      Arrays.asList("ft1", "ft2"),
      Arrays.asList(
        new CrossValidationFold(
          StructFactory.makeTransformer(),
          StructFactory.makeDataFrame(),
          StructFactory.makeDataFrame(),
          0.5
        ),
        new CrossValidationFold(
          StructFactory.makeTransformer(),
          StructFactory.makeDataFrame(),
          StructFactory.makeDataFrame(),
          0.8
        )
      ),
      triple.expRunId
    );
    cve.setProblemType(ProblemType.REGRESSION);

    CrossValidationEventResponse resp = CrossValidationEventDao.store(cve, TestBase.ctx());

    // Verify table sizes.
    Assert.assertEquals(4, TestBase.tableSize(Tables.FEATURE));
    Assert.assertEquals(2, TestBase.tableSize(Tables.METRICEVENT));
    Assert.assertEquals(2, TestBase.tableSize(Tables.FITEVENT));
    Assert.assertEquals(2, TestBase.tableSize(Tables.CROSSVALIDATIONFOLD));
    Assert.assertEquals(1, TestBase.tableSize(Tables.CROSSVALIDATIONEVENT));
    Assert.assertEquals(1,
      TestBase.ctx()
        .selectFrom(Tables.EVENT)
        .where(Tables.EVENT.EVENTTYPE.eq("cross validation"))
        .fetch()
        .size()
    );
    Assert.assertEquals(5, TestBase.tableSize(Tables.DATAFRAME));
    Assert.assertEquals(2, TestBase.tableSize(Tables.TRANSFORMER));
    Assert.assertEquals(1 ,TestBase.tableSize(Tables.TRANSFORMERSPEC));

    // Verify the IDs of the CrossValidationEventResponse.
    CrossvalidationeventRecord cvRec = TestBase.ctx().selectFrom(Tables.CROSSVALIDATIONEVENT).fetchOne();
    Assert.assertEquals(resp.dfId, cvRec.getDf().intValue());
    Assert.assertEquals(resp.specId, cvRec.getSpec().intValue());
    Assert.assertEquals(resp.crossValidationEventId, cvRec.getId().intValue());
    Assert.assertEquals(2, resp.foldResponses.size());

    // Verify the Event ID.
    Assert.assertEquals(resp.eventId,
      TestBase.ctx()
        .select(Tables.EVENT.ID)
        .from(Tables.EVENT)
        .where(Tables.EVENT.EVENTTYPE.eq("cross validation"))
        .fetchOne()
        .value1()
        .intValue()
    );

    // Ensure we have the proper FitEvents.
    Assert.assertEquals(1,
      TestBase.ctx()
        .selectFrom(Tables.FITEVENT)
        .where(
          Tables.FITEVENT.DF.eq(resp.foldResponses.get(0).getTrainingId()),
          Tables.FITEVENT.TRANSFORMER.eq(resp.foldResponses.get(0).getModelId()),
          Tables.FITEVENT.TRANSFORMERSPEC.eq(resp.specId)
        )
        .fetch()
        .size()
    );
    Assert.assertEquals(1,
      TestBase.ctx()
        .selectFrom(Tables.FITEVENT)
        .where(
          Tables.FITEVENT.DF.eq(resp.foldResponses.get(1).getTrainingId()),
          Tables.FITEVENT.TRANSFORMER.eq(resp.foldResponses.get(1).getModelId()),
          Tables.FITEVENT.TRANSFORMERSPEC.eq(resp.specId)
        )
        .fetch()
        .size()
    );

    // Ensure we have the proper metric events.
    Assert.assertEquals(2,
      TestBase.ctx()
        .selectFrom(Tables.METRICEVENT)
        .where(Tables.METRICEVENT.METRICTYPE.eq("precision"))
        .fetch()
        .size()
    );
    MetriceventRecord firstMetricEvent = TestBase.ctx()
      .selectFrom(Tables.METRICEVENT)
      .where(Tables.METRICEVENT.METRICVALUE.eq(0.5f))
      .fetchOne();
    MetriceventRecord secondMetricEvent = TestBase.ctx()
      .selectFrom(Tables.METRICEVENT)
      .where(Tables.METRICEVENT.METRICVALUE.eq(0.8f))
      .fetchOne();
    Assert.assertEquals(resp.foldResponses.get(0).modelId, firstMetricEvent.getTransformer().intValue());
    Assert.assertEquals(resp.foldResponses.get(0).validationId, firstMetricEvent.getDf().intValue());
    Assert.assertEquals(resp.foldResponses.get(1).modelId, secondMetricEvent.getTransformer().intValue());
    Assert.assertEquals(resp.foldResponses.get(1).validationId, secondMetricEvent.getDf().intValue());

    // Ensure we have stored the CrossValidationFolds.
    Assert.assertEquals(
      1,
      TestBase.ctx()
        .selectFrom(Tables.CROSSVALIDATIONFOLD)
        .where(
          Tables.CROSSVALIDATIONFOLD.EXPERIMENTRUN.eq(triple.expRunId)
          .and(Tables.CROSSVALIDATIONFOLD.METRIC.eq(firstMetricEvent.getId()))
          .and(Tables.CROSSVALIDATIONFOLD.EVENT.eq(resp.crossValidationEventId))
        )
        .fetch()
        .size()
    );
    Assert.assertEquals(
      1,
      TestBase.ctx()
        .selectFrom(Tables.CROSSVALIDATIONFOLD)
        .where(
          Tables.CROSSVALIDATIONFOLD.EXPERIMENTRUN.eq(triple.expRunId)
            .and(Tables.CROSSVALIDATIONFOLD.METRIC.eq(secondMetricEvent.getId()))
            .and(Tables.CROSSVALIDATIONFOLD.EVENT.eq(resp.crossValidationEventId))
        )
        .fetch()
        .size()
    );
  }
}
