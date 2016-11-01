package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.server.algorithm.Feature;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class TestFeatures {
  private TestBase.ProjExpRunTriple triple;

  @Before
  public void initialize() throws Exception {
    triple = TestBase.reset();
  }

  @Test
  public void testOriginalFeatures() throws Exception {
    int t1 = TestBase.createTransformer(triple.expRunId, "t1", "");
    int t2 = TestBase.createTransformer(triple.expRunId, "t2", "");
    int t3 = TestBase.createTransformer(triple.expRunId, "t3", "");
    int d1 = TestBase.createDataFrame(triple.expRunId, 1);
    int d2 = TestBase.createDataFrame(triple.expRunId, 2);
    int d3 = TestBase.createDataFrame(triple.expRunId, 3);
    int s1 = TestBase.createTransformerSpec(triple.expRunId, "s1");

    TestBase.createTransformEvent(triple.expRunId, t1, d1, d2, "incol1,incol2", "outcol1,outcol2");
    TestBase.createTransformEvent(triple.expRunId, t2, d2, d3, "outcol1", "outcol3,outcol4");
    TestBase.createFitEvent(triple.expRunId, d3, s1, t3);

    List<String> features = Feature.originalFeatures(t3, TestBase.ctx());

    Assert.assertEquals(2, features.size());
    Assert.assertTrue(features.contains("incol1"));
    Assert.assertTrue(features.contains("incol2"));
  }
}
