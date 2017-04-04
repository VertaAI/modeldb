package edu.mit.csail.db.ml.server.storage.metadata;

import java.util.List;
import java.util.ArrayList;
import com.mongodb.MongoClient;
import com.mongodb.DB;
import com.mongodb.MongoException;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;

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
  public void put(String key, String value) {
    DBCollection collection = metadataDb.getCollection(COLLECTION_NAME);
    DBObject metadataObject = (DBObject) JSON.parse(value);
    metadataObject.put(MODELID_KEY, Integer.parseInt(key));
    collection.insert(metadataObject);
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

  public boolean updateScalarField(int modelId, String key, String value) {
    DBCollection collection = metadataDb.getCollection(COLLECTION_NAME);
    BasicDBObject updatedField = new BasicDBObject();
    updatedField.append("$set", new BasicDBObject().append(key, value));
    BasicDBObject modelQuery = new BasicDBObject().append(MODELID_KEY, modelId);
    WriteResult res = collection.update(modelQuery, updatedField);
    if (res.wasAcknowledged()) {
      return res.isUpdateOfExisting();
    }
    return false;
  }

  public boolean createVector(int modelId, String vectorName, Map<String, String> vectorConfig) {
    // TODO: use vectorConfig
    DBCollection collection = metadataDb.getCollection(COLLECTION_NAME);
    BasicDBObject modelQuery = new BasicDBObject().append(MODELID_KEY, modelId);
    DBObject modelDocument = collection.findOne(modelQuery);
    if (!modelDocument.containsField(vectorName)) {
      BasicDBObject updatedField = new BasicDBObject();
      updatedField.append("$set", new BasicDBObject().append(vectorName, new ArrayList<DBObject>()));
      collection.update(modelQuery, updatedField);
      return false;
    }
    return true;
  }

  public boolean addToVectorField(int modelId, String vectorName, String value) {
    DBCollection collection = metadataDb.getCollection(COLLECTION_NAME);
    BasicDBObject updatedField = new BasicDBObject();
    updatedField.append("$push", new BasicDBObject().append(vectorName, value));
    BasicDBObject modelQuery = new BasicDBObject().append(MODELID_KEY, modelId);
    WriteResult res = collection.update(modelQuery, updatedField);
    if (res.wasAcknowledged()) {
      return res.isUpdateOfExisting();
    }
    return false;
  }

  public boolean updateVectorField(int modelId, String vectorName, int index, String value) {
    DBCollection collection = metadataDb.getCollection(COLLECTION_NAME);
    BasicDBObject updatedField = new BasicDBObject();
    updatedField.append("$set", new BasicDBObject().append(
        vectorName.concat(".").concat(index).concat(".content"), value));
    BasicDBObject modelQuery = new BasicDBObject().append(MODELID_KEY, modelId);
    WriteResult res = collection.update(modelQuery, updatedField);
    if (res.wasAcknowledged()) {
      return res.isUpdateOfExisting();
    }
    return false;
  }
}
