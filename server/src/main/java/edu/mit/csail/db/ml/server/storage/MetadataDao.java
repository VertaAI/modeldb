package edu.mit.csail.db.ml.server.storage;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;
import com.mongodb.DB;
import modeldb.*;

public class MetadataDao {
    public static final String COLLECTION_NAME = "model_metadata";
    public static final String MODELID_KEY = "MODELDB_model_id";

    public static void store(FitEventResponse fer, FitEvent fe, DB metdataDb) {
        if (!fe.isSetMetadata()) {
            return;
        }
        DBCollection collection = metdataDb.getCollection(MetadataDao.COLLECTION_NAME);
        DBObject metadataObject = (DBObject) JSON.parse(fe.metadata);
        metadataObject.put(MODELID_KEY, fer.modelId);
        collection.insert(metadataObject);
    }

    public static String get(int modelId, DB metdataDb) {
        DBCollection collection = metdataDb.getCollection(MetadataDao.COLLECTION_NAME);
        DBObject res = collection.findOne(new BasicDBObject().append(
            MODELID_KEY, modelId));
        if (res == null) {
            return "";
        } else {
            return JSON.serialize(res);
        } 
    }
}