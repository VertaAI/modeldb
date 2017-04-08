package edu.mit.csail.db.ml.server.storage.metadata;

import java.util.List;
import java.util.Map;

public interface MetadataDb {

  /**
   * Open connection to the underlying database
   */
  public void open();

  /**
   * Close connections to the underlying database and clear state if any
   */
  public void close();

  /**
   * Remove all data from the database. Used for testing
   */
  public void reset();

  /**
   * Write data to the database, provide a generalized key/value interface
   */
  public void put(String key, String value);

  /**
   * Read data from the database using a key
   */
  public String get(String key);

  /**
   * Get the IDs of all the models that match the specified key-value pairs.
   * @param  keyValuePairs The map containing key-value pairs to match
   * @return The model IDs that matched
   */
  public List<Integer> getModelIds(Map<String, String> keyValuePairs);

  /**
   * Write a key-value pair for the model with ID modelId to the database.
   * @param  modelId - The ID of the model
   * @param  key - The key to update
   * @param  value - The sclar value for the field
   * @return A boolean indicating if the key was updated or not
   */
  public boolean updateScalarField(int modelId, String key, String value);

  /**
   * Create a vector value for the key with name vectorName based on the provided configuration.
   * @param  modelId - The ID of the model
   * @param  vectorName - The name of the vector key to create
   * @param  vectorConfig - The provided configuration for the vector being created
   * @return A boolean indicating if the vector was created of not
   */
  public boolean createVector(int modelId, String vectorName, Map<String, String> vectorConfig);

  /**
   * Add a new value to the vector field with the given name in the model with the given ID.
   * @param  modelId - The ID of the model
   * @param  vectorName - The name of the vector key to add to
   * @param  value - The value to be added
   * @return A boolean indicating if the value was added or not
   */
  public boolean addToVectorField(int modelId, String vectorName, String value);

  /**
   * Update the value at the given index of the vector field with the given name
   * in the model with the given ID.
   * @param  modelId - The ID of the model
   * @param  vectorName - The name of the vector to update
   * @param  index - The index in the vector to update
   * @param  value - The new value
   * @return A boolean indicating if the value at the index of the field was updated or not
   */
  public boolean updateVectorField(int modelId, String vectorName, int index, String value);
}
