package edu.mit.csail.db.ml.server.storage;

import modeldb.*;
import edu.mit.csail.db.ml.server.storage.metadata.MetadataDb;

public class MetadataDao {

    public static void store(FitEventResponse fer, FitEvent fe, 
        MetadataDb metadataDb) {
        if (fe.isSetMetadata()) {
            metadataDb.put(Integer.toString(fer.modelId), fe.metadata);
        }
    }

    public static String get(int modelId, MetadataDb metadataDb) {
        return metadataDb.get(Integer.toString(modelId));
    }
}