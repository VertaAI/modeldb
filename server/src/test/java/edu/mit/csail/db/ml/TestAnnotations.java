package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.server.storage.AnnotationDao;
import jooq.sqlite.gen.Tables;
import jooq.sqlite.gen.tables.records.AnnotationRecord;
import jooq.sqlite.gen.tables.records.AnnotationfragmentRecord;
import modeldb.*;
import org.jooq.Record1;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class TestAnnotations {
  private int expRunId;

  private AnnotationFragment makeMessageFragment(String message) {
    return new AnnotationFragment("message", null, null, null, message);
  }

  private AnnotationFragment makeDataFrameFragment(int numRows) {
    return new AnnotationFragment("dataframe", new DataFrame(-1, Collections.emptyList(), numRows, ""), null, null, "");
  }

  private AnnotationFragment makeTransformerFragment(String transformerType) {
    Transformer t = new Transformer(-1, transformerType, "");
    return new AnnotationFragment("transformer", null, null, t, "");
  }

  private AnnotationFragment makeTransformerSpecFragment(String transformerType) {
    TransformerSpec s = new TransformerSpec(-1, transformerType, Collections.emptyList(), "");
    return new AnnotationFragment("spec", null, s, null, "");
  }

  private AnnotationEvent makeEvent(AnnotationFragment... fragments) {
    return new AnnotationEvent(Arrays.asList(fragments), expRunId);
  }

  private int createAnnotation(
    String msg,
    int msgIndex,
    int trId,
    int trIndex,
    int sId,
    int specIndex,
    int dfId,
    int dfIndex
  ) throws Exception {
    AnnotationRecord ar = TestBase.ctx().newRecord(Tables.ANNOTATION);
    ar.setId(null);
    ar.setPosted(TestBase.now());
    ar.setExperimentrun(expRunId);
    ar.store();

    AnnotationfragmentRecord msgRec = TestBase.ctx().newRecord(Tables.ANNOTATIONFRAGMENT);
    msgRec.setId(null);
    msgRec.setAnnotation(ar.getId());
    msgRec.setFragmentindex(msgIndex);
    msgRec.setType("message");
    msgRec.setExperimentrun(expRunId);
    msgRec.setMessage(msg);
    msgRec.store();
    msgRec.getId();

    AnnotationfragmentRecord dfRec = TestBase.ctx().newRecord(Tables.ANNOTATIONFRAGMENT);
    dfRec.setId(null);
    dfRec.setAnnotation(ar.getId());
    dfRec.setFragmentindex(dfIndex);
    dfRec.setType("dataframe");
    dfRec.setExperimentrun(expRunId);
    dfRec.setDataframe(dfId);
    dfRec.store();
    dfRec.getId();

    AnnotationfragmentRecord tRec = TestBase.ctx().newRecord(Tables.ANNOTATIONFRAGMENT);
    tRec.setId(null);
    tRec.setAnnotation(ar.getId());
    tRec.setFragmentindex(trIndex);
    tRec.setType("transformer");
    tRec.setExperimentrun(expRunId);
    tRec.setTransformer(trId);
    tRec.store();
    tRec.getId();

    AnnotationfragmentRecord sRec = TestBase.ctx().newRecord(Tables.ANNOTATIONFRAGMENT);
    sRec.setId(null);
    sRec.setAnnotation(ar.getId());
    sRec.setFragmentindex(specIndex);
    sRec.setType("spec");
    sRec.setExperimentrun(expRunId);
    sRec.setSpec(sId);
    sRec.store();
    sRec.getId();

    return ar.getId();
  }

  @Before
  public void initialize() throws Exception {
    expRunId = TestBase.reset().expRunId;
  }

  @Test
  public void testStoreMessageFragments() throws Exception {
    // Store the message fragments.
    AnnotationEventResponse resp = AnnotationDao
      .store(makeEvent(makeMessageFragment("first"), makeMessageFragment("second")), TestBase.ctx());

    // Verify that we have stored them.
    Assert.assertEquals(1, TestBase.tableSize(Tables.ANNOTATION));
    Assert.assertEquals(2, TestBase.tableSize(Tables.ANNOTATIONFRAGMENT));

    // Get the fragments.
    List<String> messages = TestBase
      .ctx()
      .select(Tables.ANNOTATIONFRAGMENT.MESSAGE)
      .from(Tables.ANNOTATIONFRAGMENT)
      .where(Tables.ANNOTATIONFRAGMENT.ANNOTATION.eq(resp.annotationId))
      .orderBy(Tables.ANNOTATIONFRAGMENT.FRAGMENTINDEX.asc())
      .fetch()
      .map(Record1::value1);
    Assert.assertEquals(2, messages.size());
    Assert.assertEquals("first", messages.get(0));
    Assert.assertEquals("second", messages.get(1));
  }

  @Test
  public void storedMixedFragments() throws Exception {
    // Store annotation with mixed fragments.
    AnnotationEventResponse resp = AnnotationDao.store(
      makeEvent(
        makeMessageFragment("first"),
        makeDataFrameFragment(2),
        makeTransformerFragment("third"),
        makeTransformerSpecFragment("fourth")
      ),
      TestBase.ctx()
    );

    // Verify that it has been stored.
    Assert.assertEquals(1, TestBase.tableSize(Tables.ANNOTATION));
    Assert.assertEquals(4, TestBase.tableSize(Tables.ANNOTATIONFRAGMENT));

    // Get the fragments.
    List<AnnotationfragmentRecord> fragments = TestBase
      .ctx()
      .selectFrom(Tables.ANNOTATIONFRAGMENT)
      .where(Tables.ANNOTATIONFRAGMENT.ANNOTATION.eq(resp.annotationId))
      .orderBy(Tables.ANNOTATIONFRAGMENT.FRAGMENTINDEX.asc())
      .fetch()
      .map(r -> r);
    Assert.assertEquals(4, fragments.size());
    Assert.assertArrayEquals(new String[] {"message", "dataframe", "transformer", "spec"}, new String[] {
      fragments.get(0).getType(),
      fragments.get(1).getType(),
      fragments.get(2).getType(),
      fragments.get(3).getType()
    });

    fragments.forEach(frag -> Assert.assertEquals(resp.annotationId, frag.getAnnotation().intValue()));

    // Assert that there's one transformer, dataframe, and spec.
    Stream.of(Tables.DATAFRAME, Tables.TRANSFORMER, Tables.TRANSFORMERSPEC).forEach(tab ->{
      try {
        Assert.assertEquals(1, TestBase.tableSize(tab));
      } catch (Exception ex) {
        ex.printStackTrace();
        Assert.fail();
      }
    });

    // Check that the transformer, dataframe, and spec are right.
    Assert.assertEquals(
      2,
      TestBase.ctx().select(Tables.DATAFRAME.NUMROWS).from(Tables.DATAFRAME).fetchOne().value1().intValue()
    );
    Assert.assertEquals(
      "third",
      TestBase.ctx().select(Tables.TRANSFORMER.TRANSFORMERTYPE).from(Tables.TRANSFORMER).fetchOne().value1()
    );
    Assert.assertEquals(
      "fourth",
      TestBase.ctx().select(Tables.TRANSFORMERSPEC.TRANSFORMERTYPE).from(Tables.TRANSFORMERSPEC).fetchOne().value1()
    );
  }

  @Test
  public void testToString() {
    Assert.assertEquals("DataFrame(2)", AnnotationDao.fragmentToString(
      new AnnotationfragmentRecord(-1, -1, -1, "dataframe", -1, 2, -1, "", 1)
    ));
    Assert.assertEquals("hello", AnnotationDao.fragmentToString(
      new AnnotationfragmentRecord(-1, -1, -1, "message", -1, 2, -1, "hello", 1)
    ));
    Assert.assertEquals("TransformerSpec(2)", AnnotationDao.fragmentToString(
      new AnnotationfragmentRecord(-1, -1, -1, "spec", -1, -1, 2, "hello", 1)
    ));
    Assert.assertEquals("Transformer(2)", AnnotationDao.fragmentToString(
      new AnnotationfragmentRecord(-1, -1, -1, "transformer", 2, -1, -1, "hello", 1)
    ));
  }

  @Test
  public void testReadStrings() throws Exception {
    int dfId = TestBase.createDataFrame(expRunId, 1);
    int trId = TestBase.createTransformer(expRunId, "tr", "path");
    int sId = TestBase.createTransformerSpec(expRunId, "s");
    int aeId1 = createAnnotation("hello", 0, trId, 1, sId, 2, dfId, 3);
    int aeId2 = createAnnotation("hi", 2, trId, 3, sId, 0, dfId, 1);

    List<String> results = AnnotationDao.readStrings(Arrays.asList(aeId1, aeId2), TestBase.ctx());
    Assert.assertEquals(2, results.size());
    Assert.assertEquals(String.format(
      "hello Transformer(%d) TransformerSpec(%d) DataFrame(%d)",
      trId, sId, dfId
    ), results.get(0));
    Assert.assertEquals(String.format(
      "TransformerSpec(%d) DataFrame(%d) hi Transformer(%d)",
      sId, dfId, trId
    ), results.get(1));
  }

  @Test
  public void testReadString() throws Exception {
    int dfId = TestBase.createDataFrame(expRunId, 1);
    int trId = TestBase.createTransformer(expRunId, "tr", "path");
    int sId = TestBase.createTransformerSpec(expRunId, "s");
    int aeId1 = createAnnotation("hello", 0, trId, 1, sId, 2, dfId, 3);

    Assert.assertEquals(String.format(
      "hello Transformer(%d) TransformerSpec(%d) DataFrame(%d)",
      trId, sId, dfId
    ), AnnotationDao.readString(aeId1, TestBase.ctx()));
  }
}
