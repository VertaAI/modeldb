package edu.mit.csail.db.ml.server.storage.metadata;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import com.mongodb.MongoClient;
import com.mongodb.DB;
import com.mongodb.MongoException;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;
import com.mongodb.WriteResult;
import com.mongodb.WriteConcernException;
import org.joda.time.DateTime;

public class MongoMetadataDb implements MetadataDb {

  private MongoClient mongoClient;
  private DB metadataDb;
  private String host;
  private int port;
  private String dbName;
  public static final String COLLECTION_NAME = "model_metadata";
  public static final String MODELID_KEY = "MODELDB_model_id";

  public MongoMetadataDb(String host, int port, String dbName) {
    this.host = host;
    this.port = port;
    this.dbName = dbName;
  }

  /**
   * Open connections to the underlying database
   */
  public void open() {
    mongoClient = new MongoClient(host, port);
    metadataDb = mongoClient.getDB(dbName);
  }

  /**
   * Close connections to the underlying database and clear state if any
   */
  public void close() {
    mongoClient.close();
  }

  /**
   * Remove all data from the database. Used for testing
   */
  public void reset() {
    for (String collectionName : metadataDb.getCollectionNames()) {
      if (collectionName.indexOf("system.") == -1) {
        metadataDb.getCollection(collectionName).drop();
      }
    }
  }

  /**
   * Write data to the database, provide a generalized key/value interface
   */
  public boolean put(String key, String value) {
    DBCollection collection = metadataDb.getCollection(COLLECTION_NAME);
    DBObject metadataObject = (DBObject) JSON.parse(value);
    metadataObject.put(MODELID_KEY, Integer.parseInt(key));
    WriteResult res = collection.insert(metadataObject);
    return res.wasAcknowledged();
  }

  /**
   * Read data from the database using a key
   * TODO: what if there are multiple results?
   */
  public String get(String key) {
    String result = null;
    DBCollection collection = metadataDb.getCollection(COLLECTION_NAME);
    int modelId = Integer.parseInt(key);
    DBObject res = collection.findOne(new BasicDBObject().append(
        MODELID_KEY, modelId));
    if (res == null) {
        result = "";
    } else {
        result = JSON.serialize(res);
    }
    return result;
  }

  public List<Integer> getModelIds(Map<String, String> keyValuePairs) {
    DBCollection collection = metadataDb.getCollection(COLLECTION_NAME);
    BasicDBObject modelQuery = new BasicDBObject(keyValuePairs);
    return collection.find(modelQuery)
                    .toArray()
                    .stream()
                    .map(dbObj -> (int)dbObj.get(MODELID_KEY))
                    .collect(Collectors.toList());
  }

  private BasicDBObject getCastedKeyValuePair(String key, String value, String valueType) {
    BasicDBObject keyValuePair = new BasicDBObject();
    switch(valueType) {
      case "string":
        keyValuePair.append(key, value);
        break;
      case "int":
        keyValuePair.append(key, Integer.parseInt(value));
        break;
      case "double":
        keyValuePair.append(key, Double.parseDouble(value));
        break;
      case "long":
        keyValuePair.append(key, Long.parseLong(value));
        break;
      case "datetime":
        keyValuePair.append(key, DateTime.parse(value).toDate());
        break;
      case "bool":
        keyValuePair.append(key, Boolean.parseBoolean(value));
        break;
      default:
        throw new IllegalArgumentException("Unsupported value type: " + valueType);
    }
    return keyValuePair;
  }

  public boolean createOrUpdateScalarField(int modelId, String key, String value,
   String valueType) {
    BasicDBObject keyValuePair = getCastedKeyValuePair(key, value, valueType);
    DBCollection collection = metadataDb.getCollection(COLLECTION_NAME);
    BasicDBObject updatedField = new BasicDBObject(
      "$set", keyValuePair);
    BasicDBObject modelQuery = new BasicDBObject(MODELID_KEY, modelId);
    WriteResult res = collection.update(modelQuery, updatedField);
    if (res.wasAcknowledged()) {
      return res.isUpdateOfExisting();
    }
    return false;
  }

  public boolean createVectorField(int modelId, String vectorName, Map<String, String> vectorConfig) {
    // TODO: use vectorConfig
    DBCollection collection = metadataDb.getCollection(COLLECTION_NAME);
    BasicDBObject modelQuery = new BasicDBObject(MODELID_KEY, modelId)
      .append(vectorName, new BasicDBObject("$exists", false));
    DBObject modelDocument = collection.findOne(modelQuery);
    BasicDBObject updatedField = new BasicDBObject();
    updatedField.append("$set", new BasicDBObject(vectorName, new ArrayList<DBObject>()));
    WriteResult res = collection.update(modelQuery, updatedField);
    if (res.wasAcknowledged()) {
      return res.isUpdateOfExisting();
    }
    return false;
  }

  public boolean updateVectorField(int modelId, String vectorName, int valueIndex, 
    String value, String valueType) {
    String indexDotNotation = vectorName.concat(".").concat(String.valueOf(valueIndex));
    return createOrUpdateScalarField(modelId, indexDotNotation, value, valueType);
  }

  public boolean appendToVectorField(int modelId, String vectorName, String value,
    String valueType) {
    DBCollection collection = metadataDb.getCollection(COLLECTION_NAME);
    BasicDBObject keyValuePair = getCastedKeyValuePair(vectorName, value, valueType);
    BasicDBObject updatedField = new BasicDBObject("$push", keyValuePair);
    BasicDBObject modelQuery = new BasicDBObject(MODELID_KEY, modelId);
    WriteResult res;
    try {
      res = collection.update(modelQuery, updatedField);
      if (res.wasAcknowledged()) {
        return res.isUpdateOfExisting();
      }
    } catch (MongoException e) {
      return false;
    }
    return false;
  }
}
