package ai.verta.modeldb.databaseServices;

import ai.verta.uac.UserInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import java.util.List;
import java.util.Map;

public interface DocumentService {

  /*
   * Check availability of given collection name in database, if does not exist then create collection
   * with given collection. This method called from each entity service Impl constructor.
   *
   * @param String collection
   */
  void checkCollectionAvailability(String collection);

  /*
   * @see ai.verta.modeldb.repository.PersistenceNoSQLService#insertOne(java.lang.String,
   * com.google.protobuf.MessageOrBuilder)
   */
  void insertOne(MessageOrBuilder object) throws InvalidProtocolBufferException;

  /**
   * Insert multiple Objects.
   *
   * @param object
   * @throws InvalidProtocolBufferException
   */
  void insertMany(List<?> object) throws InvalidProtocolBufferException;

  /*
   * @see ai.verta.modeldb.repository.PersistenceNoSQLService#find(java.lang.String)
   */
  List<?> find();

  /*
   * @see ai.verta.modeldb.repository.PersistenceNoSQLService#findListWithPagination(java.lang.Object,
   * java.lang.Object, java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String)
   */
  List<?> findListWithPagination(
      Object queryObj,
      Object projectionObj,
      Integer pageNumber,
      Integer pageLimit,
      Boolean order,
      String sortKey);

  /*
   * @see ai.verta.modeldb.repository.PersistenceNoSQLService#findByKey(java.lang.String,
   * java.lang.String, java.lang.String)
   */
  Object findByKey(String key, String value);

  /*
   * @see ai.verta.modeldb.repository.PersistenceNoSQLService#findByKey(java.lang.String,
   * java.lang.String, java.lang.String)
   */
  List<?> findListByKey(String key, String value);

  /**
   * @param queryObj --> queryObj is used to build the where clause
   * @param projectionObj --> projectionObj is used to build selected fields e.g. get only IDs or
   *     get ID and Name
   * @param sortObj --> sortObj is used to build the sort criteria based on a key
   * @param recordLimit --> recordLimit slices the top number of records
   * @return List<?> --> return list of object based on given parameters
   */
  public List<?> findListByObject(
      Object queryObj, Object projectionObj, Object sortObj, Integer recordLimit);

  /*
   * @see ai.verta.modeldb.repository.PersistenceNoSQLService#findByObject(java.lang.Object)
   */
  Object findByObject(Object queryObj);

  /*
   * @see ai.verta.modeldb.repository.PersistenceNoSQLService#deleteOne(java.lang.String,
   * java.lang.String, java.lang.String)
   */
  Boolean deleteOne(String collectionName, String key, String value);

  /*
   * @see ai.verta.modeldb.repository.PersistenceNoSQLService#deleteMany(java.lang.String,
   * java.lang.String, java.lang.String)
   */
  Boolean deleteMany(String collectionName, String key, String value);

  /*
   * @see ai.verta.modeldb.repository.PersistenceNoSQLService#deleteMany(java.lang.String,
   * java.lang.Object)
   */
  Boolean deleteManyByQuery(String collectionName, Object queryObj);

  /*
   * @see ai.verta.modeldb.repository.PersistenceNoSQLService#update(java.lang.String,
   * org.bson.Document, org.bson.Document)
   */
  long update(String collectionName, Object queryObj, Object updateObj);

  void insertOne(Object object);

  /*
   * @see ai.verta.modeldb.repository.PersistenceNoSQLService#validateEntityUser(java.lang.String,
   * java.lang.String, ai.verta.modeldb.UserInfo)
   */
  Boolean isValidEntityOwner(String entityFieldKey, String entityFieldValue, UserInfo userInfo);

  /**
   * Get list of entity according to the specified aggregation pipeline in @param.
   *
   * @param List<Object> queryObj --> List of Aggregates documents
   * @return List<Object> --> Return list of entity base on aggregate query
   */
  List<?> findListByAggregateObject(List<?> queryObj);

  /**
   * @param collectionName
   * @param queryObj
   * @return count of entity
   */
  Long getCount(String collectionName, Object queryObj);

  /**
   * Check availability of given index names over collection name in database, if it does not exist
   * then an index with the provided index details is constructed. This method called from each
   * entity service Impl constructor.
   *
   * @param collection
   * @param indexMap
   */
  void checkIndexAvailability(String collection, Map<String, Object> indexMap);

  /*
   * @see ai.verta.modeldb.repository.PersistenceNoSQLService#replaceOne(java.lang.String,
   * org.bson.Document, org.bson.Document)
   */
  long replaceOne(Object queryObj, Object updateObj);
}
