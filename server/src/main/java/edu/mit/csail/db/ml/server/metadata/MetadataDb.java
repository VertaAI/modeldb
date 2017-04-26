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
   * @return  A boolean indicating if the put to database was successful or not
   */
  public boolean put(String key, String value);

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
   * @param  key - The key to update, which follows MongoDB's dot notation
   * @param  value - The value for the field, 
   *  where datetime values follow the format given 
   *  <a href="http://joda-time.sourceforge.net/apidocs/org/joda/time/format/ISODateTimeFormat.html#dateTimeParser()">here</a>
   * @param  valueType - The type of the value (string, int, double, long, datetime, or bool)
   * @return A boolean indicating if the key was updated or not
   * @throws ParseException
   */
  public boolean createOrUpdateScalarField(int modelId, String key, String value, String valueType);

  /**
   * Create a vector value for the key with name vectorName based on the provided configuration.
   * @param  modelId - The ID of the model
   * @param  vectorName - The name of the vector key to create
   * @param  vectorConfig - The provided configuration for the vector being created
   * @return A boolean indicating if the vector was created of not
   */
  public boolean createVectorField(int modelId, String vectorName, Map<String, String> vectorConfig);

  /**
   * Update the value of a vector field at the specifie valueIndex for the model with ID modelId to the database.
   * @param  modelId - The ID of the model
   * @param  key - The field to update, which follows MongoDB's dot notation
   * @param  valueIndex - The index of the value to update (0-indexed)
   * @param  value - The value for the field,
   *  where datetime values follow the format given 
   *  <a href="http://joda-time.sourceforge.net/apidocs/org/joda/time/format/ISODateTimeFormat.html#dateTimeParser()">here</a>
   * @param  valueType - The type of the value (string, int, double, long, datetime, or bool)
   * @return A boolean indicating if the key was updated or not
   * @throws ParseException
   */
  public boolean updateVectorField(int modelId, String key, int valueIndex, String value, String valueType);

  /**
   * Add a new value to the vector field with the given name in the model with the given ID.
   * @param  modelId - The ID of the model
   * @param  vectorName - The name of the vector key to add to
   * @param  value - The value to be added,
   *  where datetime values follow the format given 
   *  <a href="http://joda-time.sourceforge.net/apidocs/org/joda/time/format/ISODateTimeFormat.html#dateTimeParser()">here</a>
   * @param  valueType = The type of the value (string, int, double, long, datetime, or bool)
   * @return A boolean indicating if the value was added or not
   */
  public boolean appendToVectorField(int modelId, String vectorName, String value, String valueType);
}
