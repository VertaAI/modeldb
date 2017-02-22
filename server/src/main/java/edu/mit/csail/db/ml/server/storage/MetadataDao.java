package edu.mit.csail.db.ml.server.storage;

public class MetadataDao {
    public static final COLLECTION_NAME = "model_metadata";
    // TODO: what should be the return type?
    public static void store(String metadata, DB metdataDb) {
        DBCollection collection = metdataDb.getCollection(self.COLLECTION_NAME);
        JSON metadataDict = json.parse(metadata);
        collection.insert(metadataDict)
    }

    public static String get() {
        return "";
    }
}