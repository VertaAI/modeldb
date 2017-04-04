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

    public static boolean updateScalarField(int modelId, String key, String value, MetadataDb metadataDb) {
        return metadataDb.updateScalarField(modelId, key, value);
    }

    public static boolean createVector(int modelId, String vectorName, Map<String, String> vectorConfig, MetadataDb metadataDb) {
        return metadataDb.createVector(modelId, vectorName, vectorConfig);
    }

    public static boolean addToVectorField(int modelId, String vectorName, String value, MetadataDb metadataDb) {
        return metadataDb.addToVectorField(modelId, vectorName, value);
    }

    public static boolean updateVectorField(int modelId, String vectorName, int index, String value, MetadataDb metadataDb) {
        return metadataDb.updateVectorField(modelId, vectorName, index, value);
    }
}