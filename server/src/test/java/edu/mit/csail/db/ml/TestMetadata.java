package edu.mit.csail.db.ml;

import edu.mit.csail.db.ml.server.storage.FitEventDao;
import edu.mit.csail.db.ml.server.storage.MetadataDao;
import jooq.sqlite.gen.Tables;
import modeldb.FitEvent;
import modeldb.FitEventResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.mongodb.util.JSON;
import com.mongodb.DBObject;
import edu.mit.csail.db.ml.server.storage.metadata.MongoMetadataDb;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestMetadata {
  private TestBase.ProjExpRunTriple triple;

  @Before
  public void initialize() throws Exception {
    triple = TestBase.reset();
  }

  @Test
  public void testStoreGet() throws Exception {
    FitEvent fe = StructFactory.makeFitEvent();
    String serializedMetadata = "{'key1':'value1', 'key2':30}";
    fe.setMetadata(serializedMetadata);
    FitEventResponse resp = FitEventDao.store(fe, TestBase.ctx(), false);
    MetadataDao.store(resp, fe, TestBase.getMetadataDb());

    String actualDbContents = MetadataDao.get(resp.modelId, 
      TestBase.getMetadataDb());
    DBObject parsedDbContents = (DBObject) JSON.parse(actualDbContents);
    Assert.assertEquals(parsedDbContents.get("key1"), "value1");
    Assert.assertEquals(30, parsedDbContents.get("key2"));
    Assert.assertEquals(resp.modelId,
      parsedDbContents.get(MongoMetadataDb.MODELID_KEY));
  }

  @Test
  public void testGetModelIdsSingle() throws Exception {
    FitEvent fe = StructFactory.makeFitEvent();
    String serializedMetadata = "{'key1':'value1', 'key2':30}";
    fe.setMetadata(serializedMetadata);
    FitEventResponse resp = FitEventDao.store(fe, TestBase.ctx(), false);
    MetadataDao.store(resp, fe, TestBase.getMetadataDb());

    Map<String, String> kvToMatch = new HashMap<>();
    List<Integer> noMatch = Collections.emptyList();
    List<Integer> oneMatch = Arrays.asList(resp.modelId);

    Assert.assertEquals(oneMatch, MetadataDao.getModelIds(kvToMatch, TestBase.getMetadataDb()));

    kvToMatch.put("key1", "value1");
    Assert.assertEquals(oneMatch, MetadataDao.getModelIds(kvToMatch, TestBase.getMetadataDb()));

    kvToMatch.put("key2", "wrong value");
    Assert.assertEquals(noMatch, MetadataDao.getModelIds(kvToMatch, TestBase.getMetadataDb()));
  }

  @Test
  public void testGetModelIdsMultiple() throws Exception {
    FitEvent fe = StructFactory.makeFitEvent();
    String serializedMetadata = "{'key1':'value1', 'key2':'30'}";
    fe.setMetadata(serializedMetadata);
    FitEventResponse resp = FitEventDao.store(fe, TestBase.ctx(), false);
    MetadataDao.store(resp, fe, TestBase.getMetadataDb());

    FitEvent fe2 = StructFactory.makeFitEvent();
    String serializedMetadata2 = "{'key1':'value2', 'key2':'30'}";
    fe2.setMetadata(serializedMetadata2);
    FitEventResponse resp2 = FitEventDao.store(fe2, TestBase.ctx(), false);
    MetadataDao.store(resp2, fe2, TestBase.getMetadataDb());

    Map<String, String> kvToMatch = new HashMap<>();
    List<Integer> noMatch = Collections.emptyList();
    List<Integer> oneMatch = Arrays.asList(resp2.modelId);
    List<Integer> allMatch = Arrays.asList(resp.modelId, resp2.modelId);

    // Test when there are no kv pairs
    Assert.assertEquals(allMatch, MetadataDao.getModelIds(kvToMatch, TestBase.getMetadataDb()));
    // Test with 1 kv pair that matches all
    kvToMatch.put("key2", "30");
    Assert.assertEquals(allMatch, MetadataDao.getModelIds(kvToMatch, TestBase.getMetadataDb()));
    // Test with kv pair that matches 1
    kvToMatch.put("key1", "value2");
    Assert.assertEquals(oneMatch, MetadataDao.getModelIds(kvToMatch, TestBase.getMetadataDb()));
    // Test with 2 kv pairs where 1 doesn't match
    kvToMatch.put("author", "wrong value");
    Assert.assertEquals(noMatch, MetadataDao.getModelIds(kvToMatch, TestBase.getMetadataDb()));
  }

  @Test
  public void testUpdateFieldSimpleExisting() throws Exception {
    FitEvent fe = StructFactory.makeFitEvent();
    String serializedMetadata = "{'key1':'value1', 'key2':'30'}";
    fe.setMetadata(serializedMetadata);
    FitEventResponse resp = FitEventDao.store(fe, TestBase.ctx(), false);
    MetadataDao.store(resp, fe, TestBase.getMetadataDb());

    Assert.assertTrue(MetadataDao.updateScalarField(resp.modelId, "key1", 
      "new value", TestBase.getMetadataDb()));
    String actualDbContents = MetadataDao.get(resp.modelId, 
      TestBase.getMetadataDb());
    DBObject parsedDbContents = (DBObject) JSON.parse(actualDbContents);
    Assert.assertEquals("new value", parsedDbContents.get("key1"));
    Assert.assertEquals("30", parsedDbContents.get("key2"));
    Assert.assertEquals(resp.modelId,
      parsedDbContents.get(MongoMetadataDb.MODELID_KEY));
  }

  @Test
  public void testUpdateFieldSimpleNew() throws Exception {
    FitEvent fe = StructFactory.makeFitEvent();
    String serializedMetadata = "{'key1':'value1'}";
    fe.setMetadata(serializedMetadata);
    FitEventResponse resp = FitEventDao.store(fe, TestBase.ctx(), false);
    MetadataDao.store(resp, fe, TestBase.getMetadataDb());

    Assert.assertTrue(MetadataDao.updateScalarField(resp.modelId, "key2", 
      "value2", TestBase.getMetadataDb()));
    String actualDbContents = MetadataDao.get(resp.modelId, 
      TestBase.getMetadataDb());
    DBObject parsedDbContents = (DBObject) JSON.parse(actualDbContents);
    Assert.assertEquals("value1", parsedDbContents.get("key1"));
    Assert.assertEquals("value2", parsedDbContents.get("key2"));
    Assert.assertEquals(resp.modelId,
      parsedDbContents.get(MongoMetadataDb.MODELID_KEY));
  }

  @Test
  public void testUpdateFieldNested() throws Exception {
    FitEvent fe = StructFactory.makeFitEvent();
    String serializedMetadata = "{'key1': {'nested_key_1': 'nested_value_1', 'nested_key_2': 'nested_value_2'}, 'key2':'30'}";
    fe.setMetadata(serializedMetadata);
    FitEventResponse resp = FitEventDao.store(fe, TestBase.ctx(), false);
    MetadataDao.store(resp, fe, TestBase.getMetadataDb());

    Assert.assertTrue(MetadataDao.updateScalarField(resp.modelId, "key1.nested_key_1", 
      "new value", TestBase.getMetadataDb()));
    String actualDbContents = MetadataDao.get(resp.modelId, 
      TestBase.getMetadataDb());
    DBObject parsedDbContents = (DBObject) JSON.parse(actualDbContents);
    Object key1Contents = parsedDbContents.get("key1");
    Assert.assertTrue(key1Contents instanceof DBObject);
    Assert.assertEquals("new value", ((DBObject) key1Contents).get("nested_key_1"));
    Assert.assertEquals("nested_value_2", ((DBObject) key1Contents).get("nested_key_2"));
    Assert.assertEquals("30", parsedDbContents.get("key2"));
    Assert.assertEquals(resp.modelId,
      parsedDbContents.get(MongoMetadataDb.MODELID_KEY));
  }

  @Test
  public void testUpdateFieldVector() throws Exception {
    FitEvent fe = StructFactory.makeFitEvent();
    String serializedMetadata = "{'key1': ['value1', 'value2'], 'key2':'30'}";
    fe.setMetadata(serializedMetadata);
    FitEventResponse resp = FitEventDao.store(fe, TestBase.ctx(), false);
    MetadataDao.store(resp, fe, TestBase.getMetadataDb());

    Assert.assertTrue(MetadataDao.updateScalarField(resp.modelId, "key1.0", 
      "new value",
      TestBase.getMetadataDb()));
    String actualDbContents = MetadataDao.get(resp.modelId, 
      TestBase.getMetadataDb());
    DBObject parsedDbContents = (DBObject) JSON.parse(actualDbContents);
    Object key1Contents = parsedDbContents.get("key1");
    Assert.assertTrue(key1Contents instanceof List);
    Assert.assertEquals("new value", ((List) key1Contents).get(0));
    Assert.assertEquals("value2", ((List) key1Contents).get(1));
    Assert.assertEquals("30", parsedDbContents.get("key2"));
    Assert.assertEquals(resp.modelId,
      parsedDbContents.get(MongoMetadataDb.MODELID_KEY));
  }

  @Test
  public void testUpdateFieldVectorAppend() throws Exception {
    FitEvent fe = StructFactory.makeFitEvent();
    String serializedMetadata = "{'key1': ['value1', 'value2'], 'key2':'30'}";
    fe.setMetadata(serializedMetadata);
    FitEventResponse resp = FitEventDao.store(fe, TestBase.ctx(), false);
    MetadataDao.store(resp, fe, TestBase.getMetadataDb());

    Assert.assertTrue(MetadataDao.updateScalarField(resp.modelId, "key1.2", 
      "new value", TestBase.getMetadataDb()));
    String actualDbContents = MetadataDao.get(resp.modelId, 
      TestBase.getMetadataDb());
    DBObject parsedDbContents = (DBObject) JSON.parse(actualDbContents);
    Object key1Contents = parsedDbContents.get("key1");
    Assert.assertTrue(key1Contents instanceof List);
    Assert.assertEquals("value1", ((List) key1Contents).get(0));
    Assert.assertEquals("value2", ((List) key1Contents).get(1));
    Assert.assertEquals("new value", ((List) key1Contents).get(2));
    Assert.assertEquals("30", parsedDbContents.get("key2"));
    Assert.assertEquals(resp.modelId,
      parsedDbContents.get(MongoMetadataDb.MODELID_KEY));
  }

  @Test
  public void testUpdateFieldScalarNestedInVector() throws Exception {
    FitEvent fe = StructFactory.makeFitEvent();
    String serializedMetadata = "{'key1': [{'nested_key_1': 'value index 0'}, {'nested_key_1': 'value index 1'}], 'key2':'30'}";
    fe.setMetadata(serializedMetadata);
    FitEventResponse resp = FitEventDao.store(fe, TestBase.ctx(), false);
    MetadataDao.store(resp, fe, TestBase.getMetadataDb());

    Assert.assertTrue(MetadataDao.updateScalarField(resp.modelId, "key1.1.nested_key_1", 
      "new value", TestBase.getMetadataDb()));
    String actualDbContents = MetadataDao.get(resp.modelId, 
      TestBase.getMetadataDb());
    DBObject parsedDbContents = (DBObject) JSON.parse(actualDbContents);
    Object key1Contents = parsedDbContents.get("key1");
    Assert.assertTrue(key1Contents instanceof List);
    Assert.assertTrue(((List) key1Contents).get(0) instanceof DBObject);
    Assert.assertEquals("value index 0", ((DBObject) ((List) key1Contents).get(0)).get("nested_key_1"));
    Assert.assertTrue(((List) key1Contents).get(1) instanceof DBObject);
    Assert.assertEquals("new value", ((DBObject) ((List) key1Contents).get(1)).get("nested_key_1"));
    Assert.assertEquals("30", parsedDbContents.get("key2"));
    Assert.assertEquals(resp.modelId,
      parsedDbContents.get(MongoMetadataDb.MODELID_KEY));
  }

  @Test
  public void testCreateVectorNotExisting() throws Exception {
    FitEvent fe = StructFactory.makeFitEvent();
    String serializedMetadata = "{'key1':'value1'}";
    fe.setMetadata(serializedMetadata);
    FitEventResponse resp = FitEventDao.store(fe, TestBase.ctx(), false);
    MetadataDao.store(resp, fe, TestBase.getMetadataDb());

    Assert.assertTrue(MetadataDao.createVector(resp.modelId, "key2", 
      Collections.emptyMap(), TestBase.getMetadataDb()));
    String actualDbContents = MetadataDao.get(resp.modelId, 
      TestBase.getMetadataDb());
    DBObject parsedDbContents = (DBObject) JSON.parse(actualDbContents);
    Assert.assertEquals("value1", parsedDbContents.get("key1"));
    Assert.assertTrue(parsedDbContents.get("key2") instanceof List);
    Assert.assertEquals(resp.modelId,
      parsedDbContents.get(MongoMetadataDb.MODELID_KEY));
  }

  @Test
  public void testCreateVectorExisting() throws Exception {
    FitEvent fe = StructFactory.makeFitEvent();
    String serializedMetadata = "{'key1':'value1'}";
    fe.setMetadata(serializedMetadata);
    FitEventResponse resp = FitEventDao.store(fe, TestBase.ctx(), false);
    MetadataDao.store(resp, fe, TestBase.getMetadataDb());

    Assert.assertFalse(MetadataDao.createVector(resp.modelId, "key1", 
      Collections.emptyMap(), TestBase.getMetadataDb()));
    String actualDbContents = MetadataDao.get(resp.modelId, 
      TestBase.getMetadataDb());
    DBObject parsedDbContents = (DBObject) JSON.parse(actualDbContents);
    Assert.assertEquals("value1", parsedDbContents.get("key1"));
    Assert.assertEquals(resp.modelId,
      parsedDbContents.get(MongoMetadataDb.MODELID_KEY));
  }

  @Test
  public void testAddToVectorFieldNotVectorFail() throws Exception {
    FitEvent fe = StructFactory.makeFitEvent();
    String serializedMetadata = "{'key1':'value1'}";
    fe.setMetadata(serializedMetadata);
    FitEventResponse resp = FitEventDao.store(fe, TestBase.ctx(), false);
    MetadataDao.store(resp, fe, TestBase.getMetadataDb());

    Assert.assertFalse(MetadataDao.addToVectorField(resp.modelId, "key1", 
      "value", TestBase.getMetadataDb()));
    String actualDbContents = MetadataDao.get(resp.modelId, 
      TestBase.getMetadataDb());
    DBObject parsedDbContents = (DBObject) JSON.parse(actualDbContents);
    Assert.assertEquals("value1", parsedDbContents.get("key1"));
    Assert.assertEquals(resp.modelId,
      parsedDbContents.get(MongoMetadataDb.MODELID_KEY));
  }

  @Test
  public void testAddToVectorFieldSuccess() throws Exception {
    FitEvent fe = StructFactory.makeFitEvent();
    String serializedMetadata = "{'key1': ['elem1']}";
    fe.setMetadata(serializedMetadata);
    FitEventResponse resp = FitEventDao.store(fe, TestBase.ctx(), false);
    MetadataDao.store(resp, fe, TestBase.getMetadataDb());

    Assert.assertTrue(MetadataDao.addToVectorField(resp.modelId, "key1", 
      "elem2", TestBase.getMetadataDb()));
    String actualDbContents = MetadataDao.get(resp.modelId, 
      TestBase.getMetadataDb());
    DBObject parsedDbContents = (DBObject) JSON.parse(actualDbContents);
    Object key1Contents = parsedDbContents.get("key1");
    Assert.assertTrue(key1Contents instanceof List);
    Assert.assertEquals("elem1", ((List) key1Contents).get(0));
    Assert.assertEquals("elem2", ((List) key1Contents).get(1));
    Assert.assertEquals(resp.modelId,
      parsedDbContents.get(MongoMetadataDb.MODELID_KEY));
  }
}
