package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.server.storage.TransformerSpecDao;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.HyperparameterRecord;
import jooq.sqlite.gen.tables.records.TransformerspecRecord;
import modeldb.HyperParameter;
import modeldb.TransformerSpec;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestTransformerSpec {
  private int expRunId;

  @Before
  public void initialize() throws Exception {
    expRunId = TestBase.reset().expRunId;
  }

  public void checkHyperparameters(List<HyperparameterRecord> hps, int specId) {
    Assert.assertEquals(2, hps.size());
    HyperparameterRecord first = hps.get(0);
    HyperparameterRecord second = hps.get(1);
    if (second.getParamname().equals("hp1")) {
      HyperparameterRecord tmp = first;
      first = second;
      second = tmp;
    }

    Assert.assertEquals("hp1", first.getParamname());
    Assert.assertEquals("hp2", second.getParamname());
    Assert.assertEquals(specId, first.getSpec().intValue());
    Assert.assertEquals(specId, second.getSpec().intValue());
    Assert.assertEquals("val1", first.getParamvalue());
    Assert.assertEquals("val2", second.getParamvalue());
    Assert.assertEquals("string", first.getParamtype());
    Assert.assertEquals("int", second.getParamtype());
    Assert.assertEquals(1, first.getParamminvalue().doubleValue(), 0.1);
    Assert.assertEquals(2, first.getParammaxvalue().doubleValue(), 0.1);
    Assert.assertEquals(3, second.getParamminvalue().doubleValue(), 0.1);
    Assert.assertEquals(4, second.getParammaxvalue().doubleValue(), 0.1);
    Assert.assertEquals(expRunId, first.getExperimentrun().intValue());
    Assert.assertEquals(expRunId, first.getExperimentrun().intValue());
  }

  @Test
  public void testStore() throws Exception {
    // Store the spec.
    int specId = TransformerSpecDao.store(
      new TransformerSpec(
        -1,
        "ttype",
        Arrays.asList(
          new HyperParameter("hp1", "val1", "string", 1, 2),
          new HyperParameter("hp2", "val2", "int", 3, 4)
        ),
        "tag"
      ),
      expRunId,
      TestBase.ctx()
    ).getId();

    // Verify the spec.
    Assert.assertEquals(1, TestBase.tableSize(Tables.TRANSFORMERSPEC));
    TransformerspecRecord rec = TestBase.ctx().selectFrom(Tables.TRANSFORMERSPEC).fetchOne();
    Assert.assertEquals(specId, rec.getId().intValue());
    Assert.assertEquals("ttype", rec.getTransformertype());
    Assert.assertEquals("tag", rec.getTag());
    Assert.assertEquals(expRunId, rec.getExperimentrun().intValue());

    // Verify the hyperparameters.
    List<HyperparameterRecord> hps = TestBase
      .ctx()
      .selectFrom(Tables.HYPERPARAMETER)
      .fetch();
    checkHyperparameters(hps, specId);
  }

  @Test
  public void testRead() throws Exception {
    int sid = TestBase.createTransformerSpec(expRunId, "ttype");

    TransformerSpec spec = TransformerSpecDao.read(sid, TestBase.ctx());

    Assert.assertEquals(sid, spec.getId());
    Assert.assertEquals("ttype", spec.getTransformerType());
    Assert.assertEquals(2, spec.hyperparameters.size());
    // Checking the hyperparameters should involve more code, but this will do for now.
    HyperParameter first = spec.hyperparameters.get(0);
    HyperParameter second = spec.hyperparameters.get(1);
    if (first.getName().equals("hp2")) {
      HyperParameter tmp = first;
      first = second;
      second = tmp;
    }
    Assert.assertEquals("hp1", first.getName());
    Assert.assertEquals("hp2", second.getName());
    Assert.assertEquals("val1", first.getValue());
    Assert.assertEquals("val2", second.getValue());
    Assert.assertEquals("string", first.getType());
    Assert.assertEquals("int", second.getType());
    Assert.assertEquals(1, first.getMin(), 0.1);
    Assert.assertEquals(2, first.getMax(), 0.1);
    Assert.assertEquals(3, second.getMin(), 0.1);
    Assert.assertEquals(4, second.getMax(), 0.1);
  }
}
