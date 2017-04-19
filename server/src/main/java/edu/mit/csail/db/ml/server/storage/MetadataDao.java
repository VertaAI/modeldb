package edu.mit.csail.db.ml.server.storage;

import modeldb.*;
import edu.mit.csail.db.ml.server.storage.metadata.MetadataDb;
import java.util.List;
import java.util.Map;

public class MetadataDao {

    public static boolean store(FitEventResponse fer, FitEvent fe, 
        MetadataDb metadataDb) {
        if (fe.isSetMetadata()) {
            return metadataDb.put(Integer.toString(fer.modelId), fe.metadata);
        }
        return true;
    }

    public static String get(int modelId, MetadataDb metadataDb) {
        return metadataDb.get(Integer.toString(modelId));
    }

    public static List<Integer> getModelIds(Map<String, String> keyValuePairs, MetadataDb metadataDb) {
        return metadataDb.getModelIds(keyValuePairs);
    }

    public static boolean createOrUpdateScalarField(int modelId, String key, String value, String valueType, MetadataDb metadataDb) {
        return metadataDb.createOrUpdateScalarField(modelId, key, value, valueType);
    }

    public static boolean createVectorField(int modelId, String vectorName, Map<String, String> vectorConfig, MetadataDb metadataDb) {
        return metadataDb.createVectorField(modelId, vectorName, vectorConfig);
    }

    public static boolean updateVectorField(int modelId, String key, int valueIndex, String value, String valueType, MetadataDb metadataDb) {
        return metadataDb.updateVectorField(modelId, key, valueIndex, value, valueType);
    }

    public static boolean appendToVectorField(int modelId, String vectorName, String value, String valueType, MetadataDb metadataDb) {
        return metadataDb.appendToVectorField(modelId, vectorName, value, valueType);
    }
}