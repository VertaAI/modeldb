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
  public static final String COLLECTION_NAME = "model_metadata";
  public static final String MODELID_KEY = "MODELDB_model_id";

  public MongoMetadataDb(String host, int port, String dbName) {
    mongoClient = new MongoClient(host, port);
    metadataDb = mongoClient.getDB(dbName);
  }

  /**
   * Initialize connections to the underlying database
   */
  public MetadataDb getDb() {
    return this;
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
}