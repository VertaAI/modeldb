package edu.mit.csail.db.ml.server.storage.metadata;

import java.util.List;

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
}