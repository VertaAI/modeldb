package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.server.algorithm.DataFrameAncestryComputer;
import modeldb.ModelAncestryResponse;
import modeldb.ProblemType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestAncestryComputer {
  TestBase.ProjExpRunTriple triple;

  @Before
  public void initialize() throws Exception {
    triple = TestBase.reset();
  }

  @Test
  public void testModelAncestry() throws Exception {
    int df1 = TestBase.createDataFrame(triple.expRunId, 101);
    int df2 = TestBase.createDataFrame(triple.expRunId, 102);
    int df3 = TestBase.createDataFrame(triple.expRunId, 103);

    int t1 = TestBase.createTransformer(triple.expRunId, "t1", "");
    int t2 = TestBase.createTransformer(triple.expRunId, "t2", "");
    int t3 = TestBase.createTransformer(triple.expRunId, "t3", "");

    int ts = TestBase.createTransformerSpec(triple.expRunId, "s1");

    TestBase.createTransformEvent(triple.expRunId, t1, df1, df2, "incol1", "outcol1,outcol2");
    TestBase.createTransformEvent(triple.expRunId, t2, df2, df3, "incol2,incol3", "outcol3");

    TestBase.createFitEvent(triple.expRunId, df3, ts, t3);

    ModelAncestryResponse resp = DataFrameAncestryComputer.computeModelAncestry(t3, TestBase.ctx());

    Assert.assertEquals(t3, resp.modelId);
    Assert.assertEquals(df3, resp.fitEvent.df.id);
    Assert.assertEquals(103, resp.fitEvent.df.numRows);
    Assert.assertEquals(ts, resp.fitEvent.spec.id);
    Assert.assertEquals("s1", resp.fitEvent.spec.transformerType);
    Assert.assertEquals(t3, resp.fitEvent.model.id);
    Assert.assertEquals("t3", resp.fitEvent.model.transformerType);
    Assert.assertEquals("predCol2", resp.fitEvent.predictionColumns.get(1));
    Assert.assertEquals("labCol2", resp.fitEvent.labelColumns.get(1));
    Assert.assertEquals(triple.expRunId, resp.fitEvent.experimentRunId);
    Assert.assertEquals(ProblemType.REGRESSION, resp.fitEvent.problemType);
    Assert.assertEquals(2, resp.transformEvents.size());
    Assert.assertEquals(df1, resp.transformEvents.get(0).oldDataFrame.id);
    Assert.assertEquals(df2, resp.transformEvents.get(0).newDataFrame.id);
    Assert.assertEquals("incol1", resp.transformEvents.get(0).inputColumns.get(0));
    Assert.assertEquals("outcol2", resp.transformEvents.get(0).outputColumns.get(1));
    Assert.assertEquals(df2, resp.transformEvents.get(1).oldDataFrame.id);
    Assert.assertEquals(df3, resp.transformEvents.get(1).newDataFrame.id);
    Assert.assertEquals("incol3", resp.transformEvents.get(1).inputColumns.get(1));
    Assert.assertEquals("outcol3", resp.transformEvents.get(1).outputColumns.get(0));
  }
}
