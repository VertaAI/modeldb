package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.server.storage.GridSearchCrossValidationEventDao;
import edu.mit.csail.db.ml.util.Pair;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.CrossvalidationeventRecord;
import jooq.sqlite.gen.tables.records.CrossvalidationfoldRecord;
import jooq.sqlite.gen.tables.records.GridsearchcrossvalidationeventRecord;
import jooq.sqlite.gen.tables.records.MetriceventRecord;
import modeldb.CrossValidationEvent;
import modeldb.CrossValidationFold;
import modeldb.GridSearchCrossValidationEvent;
import modeldb.GridSearchCrossValidationEventResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TestGridSearchCrossValidation {
  private TestBase.ProjExpRunTriple triple;

  @Before
  public void initialize() throws Exception {
    triple = TestBase.reset();
  }

  @Test
  public void testGridSearchCrossValidation() throws Exception {
    GridSearchCrossValidationEvent gscve = new GridSearchCrossValidationEvent(
      3,
      StructFactory.makeFitEvent(),
      Arrays.asList(
        new CrossValidationEvent(
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
            ),
            new CrossValidationFold(
              StructFactory.makeTransformer(),
              StructFactory.makeDataFrame(),
              StructFactory.makeDataFrame(),
              0.7
            )
          ),
          999
        ),
        new CrossValidationEvent(
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
              0.3
            ),
            new CrossValidationFold(
              StructFactory.makeTransformer(),
              StructFactory.makeDataFrame(),
              StructFactory.makeDataFrame(),
              0.4
            ),
            new CrossValidationFold(
              StructFactory.makeTransformer(),
              StructFactory.makeDataFrame(),
              StructFactory.makeDataFrame(),
              0.9
            )
          ),
          9999
        )
      ),
      triple.expRunId
    );

    GridSearchCrossValidationEventResponse resp = GridSearchCrossValidationEventDao.store(gscve, TestBase.ctx());

    // Verify table sizes.
    Assert.assertEquals(1, TestBase.tableSize(Tables.GRIDSEARCHCROSSVALIDATIONEVENT));
    Assert.assertEquals(2, TestBase.tableSize(Tables.GRIDCELLCROSSVALIDATION));
    Assert.assertEquals(2, TestBase.tableSize(Tables.CROSSVALIDATIONEVENT));
    Assert.assertEquals(6, TestBase.tableSize(Tables.CROSSVALIDATIONFOLD));
    Assert.assertEquals(7, TestBase.tableSize(Tables.FITEVENT));
    Assert.assertEquals(6, TestBase.tableSize(Tables.METRICEVENT));

    // 2 features per fold, 6 folds = 12 features. 3 features for best fit event. 12 + 3 = 15.
    Assert.assertEquals(15, TestBase.tableSize(Tables.FEATURE));

    // 1 original DataFrame, 3 training splits, 3 validation splits. 3 + 3 + 1 = 7.
    Assert.assertEquals(7, TestBase.tableSize(Tables.DATAFRAME));

    // One model per for each of the six folds and one best model.
    // One transformer for each split (3 training, 3 validation). 6 + 6 + 1 = 13.
    Assert.assertEquals(13, TestBase.tableSize(Tables.TRANSFORMER));

    // One spec for best model and one for each of the two cross validation grid cells.
    Assert.assertEquals(3, TestBase.tableSize(Tables.TRANSFORMERSPEC));

    // Verify the GridSearchCrossValidationEvent.
    GridsearchcrossvalidationeventRecord gscveRec = TestBase.ctx()
      .selectFrom(Tables.GRIDSEARCHCROSSVALIDATIONEVENT)
      .where(Tables.GRIDSEARCHCROSSVALIDATIONEVENT.ID.eq(resp.gscveId))
      .fetchOne();
    Assert.assertEquals(3, gscveRec.getNumfolds().intValue());
    Assert.assertEquals(resp.fitEventResponse.fitEventId, gscveRec.getBest().intValue());
    Assert.assertEquals(triple.expRunId, gscveRec.getExperimentrun().intValue());

    // Verify the GridCellCrossValidations.
    Assert.assertEquals(2, resp.crossValidationEventResponses.size());
    Assert.assertEquals(1,
      TestBase.ctx()
        .selectFrom(Tables.GRIDCELLCROSSVALIDATION)
        .where(
          Tables.GRIDCELLCROSSVALIDATION.GRIDSEARCH.eq(resp.gscveId)
          .and(Tables.GRIDCELLCROSSVALIDATION.CROSSVALIDATION.eq(
            resp.crossValidationEventResponses.get(0).crossValidationEventId
          ))
          .and(Tables.GRIDCELLCROSSVALIDATION.EXPERIMENTRUN.eq(triple.expRunId))
        )
        .fetch()
        .size()
    );
    Assert.assertEquals(1,
      TestBase.ctx()
        .selectFrom(Tables.GRIDCELLCROSSVALIDATION)
        .where(
          Tables.GRIDCELLCROSSVALIDATION.GRIDSEARCH.eq(resp.gscveId)
            .and(Tables.GRIDCELLCROSSVALIDATION.CROSSVALIDATION.eq(
              resp.crossValidationEventResponses.get(1).crossValidationEventId
            ))
            .and(Tables.GRIDCELLCROSSVALIDATION.EXPERIMENTRUN.eq(triple.expRunId))
        )
        .fetch()
        .size()
    );

    // Verify the CrossValidationEvents.
    CrossvalidationeventRecord cveRec1 = TestBase.ctx()
      .selectFrom(Tables.CROSSVALIDATIONEVENT)
      .where(Tables.CROSSVALIDATIONEVENT.ID.eq(resp.crossValidationEventResponses.get(0).crossValidationEventId))
      .fetchOne();
    CrossvalidationeventRecord cveRec2 = TestBase.ctx()
      .selectFrom(Tables.CROSSVALIDATIONEVENT)
      .where(Tables.CROSSVALIDATIONEVENT.ID.eq(resp.crossValidationEventResponses.get(1).crossValidationEventId))
      .fetchOne();
    Stream.of(cveRec1, cveRec2).forEach(rec -> {
      Assert.assertEquals(resp.fitEventResponse.dfId, rec.getDf().intValue());
      Assert.assertEquals(3, rec.getNumfolds().intValue());
      Assert.assertEquals(triple.expRunId, rec.getExperimentrun().intValue());
    });

    // Verify the CrossValidationFolds.
    List<CrossvalidationfoldRecord> folds1 = TestBase.ctx()
      .selectFrom(Tables.CROSSVALIDATIONFOLD)
      .where(Tables.CROSSVALIDATIONFOLD.EVENT.eq(resp.crossValidationEventResponses.get(0).crossValidationEventId))
      .fetch();
    List<CrossvalidationfoldRecord> folds2 = TestBase.ctx()
      .selectFrom(Tables.CROSSVALIDATIONFOLD)
      .where(Tables.CROSSVALIDATIONFOLD.EVENT.eq(resp.crossValidationEventResponses.get(1).crossValidationEventId))
      .fetch();
    folds1.forEach(fold -> Assert.assertEquals(triple.expRunId, fold.getExperimentrun().intValue()));
    folds2.forEach(fold -> Assert.assertEquals(triple.expRunId, fold.getExperimentrun().intValue()));

    // Verify that the validation and training IDs match across folds.
    List<Pair<Integer, Integer>> pairs1 = resp.crossValidationEventResponses.get(0).foldResponses
      .stream()
      .map(r -> new Pair<>(r.getTrainingId(), r.getValidationId()))
      .collect(Collectors.toList());
    List<Pair<Integer, Integer>> pairs2 = resp.crossValidationEventResponses.get(1).foldResponses
      .stream()
      .map(r -> new Pair<>(r.getTrainingId(), r.getValidationId()))
      .collect(Collectors.toList());
    Assert.assertEquals(pairs1.size(), pairs2.size());
    IntStream.range(0, pairs1.size()).forEach(i -> {
      Assert.assertEquals(pairs1.get(i).getFirst(), pairs2.get(i).getFirst());
      Assert.assertEquals(pairs1.get(i).getSecond(), pairs2.get(i).getSecond());
    });

    // Verify the MetricEvents.
    List<Integer> metricIds1 = folds1
      .stream()
      .map(CrossvalidationfoldRecord::getMetric)
      .collect(Collectors.toList());

    List<Integer> metricIds2 = folds2
      .stream()
      .map(CrossvalidationfoldRecord::getMetric)
      .collect(Collectors.toList());

    List<MetriceventRecord> metricEvents1 = TestBase.ctx()
      .selectFrom(Tables.METRICEVENT)
      .where(Tables.METRICEVENT.ID.in(metricIds1))
      .fetch()
      .stream()
      .sorted((a, b) -> metricIds1.indexOf(a.getId()) - metricIds1.indexOf(b.getId()))
      .collect(Collectors.toList());

    List<MetriceventRecord> metricEvents2 = TestBase.ctx()
      .selectFrom(Tables.METRICEVENT)
      .where(Tables.METRICEVENT.ID.in(metricIds2))
      .fetch()
      .stream()
      .sorted((a, b) -> metricIds2.indexOf(a.getId()) - metricIds2.indexOf(b.getId()))
      .collect(Collectors.toList());

    // Ensure that the MetricEvents are correct.
    Assert.assertEquals(metricEvents1.size(), metricEvents2.size());
    float[] metricVals1 = new float[]{ 0.5f, 0.8f, 0.7f };
    float[] metricVals2 = new float[]{ 0.3f, 0.4f, 0.9f };
    IntStream.range(0, metricEvents1.size()).forEach(i -> {
      Assert.assertEquals(pairs1.get(i).getSecond(), metricEvents1.get(i).getDf());
      Assert.assertEquals(pairs1.get(i).getSecond(), metricEvents2.get(i).getDf());
      Assert.assertEquals(metricVals1[i], metricEvents1.get(i).getMetricvalue(), 0.01);
      Assert.assertEquals(metricVals2[i], metricEvents2.get(i).getMetricvalue(), 0.01);
      Assert.assertEquals("precision", metricEvents1.get(i).getMetrictype());
      Assert.assertEquals("precision", metricEvents2.get(i).getMetrictype());
      Assert.assertEquals(triple.expRunId, metricEvents1.get(i).getExperimentrun().intValue());
      Assert.assertEquals(triple.expRunId, metricEvents2.get(i).getExperimentrun().intValue());
    });
  }
}
