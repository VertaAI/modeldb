package ai.verta.modeldb.experimentrun.subtypes;

import static ai.verta.modeldb.entities.config.ConfigBlobEntity.HYPERPARAMETER;

import ai.verta.modeldb.App;
import ai.verta.modeldb.Location;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.VersioningEntry;
import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.Handle;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.versioning.Blob;
import ai.verta.modeldb.versioning.BlobDAO;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.CommitDAO;
import ai.verta.modeldb.versioning.RepositoryDAO;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import com.google.rpc.Code;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VersionInputHandler {
  private static final String REPOSITORY_ID_QUERY_PARAM = "repository_id";
  private static final String COMMIT_QUERY_PARAM = "commit";
  private static final String VERSIONING_KEY_QUERY_PARAM = "versioning_key";
  private static final String ENTITY_ID_QUERY_PARAM = "entityId";
  private static final String ENTITY_TYPE_QUERY_PARAM = "entity_type";
  private static final String BLOB_HASH_QUERY_PARAM = "blob_hash";
  private static final String INT_VALUE_QUERY_SELECTED_PARAM = "int_value";
  private static final String VERSIONING_LOCATION_QUERY_PARAM = "versioning_location";
  private static Logger LOGGER = LogManager.getLogger(VersionInputHandler.class);

  private final FutureExecutor executor;
  private final FutureJdbi jdbi;
  private final String entity_type;
  private final String entityIdReferenceColumn;
  private final RepositoryDAO repositoryDAO;
  private final CommitDAO commitDAO;
  private final BlobDAO blobDAO;
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();

  public VersionInputHandler(
      FutureExecutor executor,
      FutureJdbi jdbi,
      String entityName,
      RepositoryDAO repositoryDAO,
      CommitDAO commitDAO,
      BlobDAO blobDAO) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.entity_type = entityName;

    this.repositoryDAO = repositoryDAO;
    this.commitDAO = commitDAO;
    this.blobDAO = blobDAO;

    switch (entityName) {
      case "ExperimentRunEntity":
        this.entityIdReferenceColumn = "experiment_run_id";
        break;
      default:
        throw new InternalErrorException("Invalid entity name: " + entityName);
    }
  }

  /**
   * - This function validate requested version_input if exists - it will validate repository and
   * commit exists or not and also if request has KeyLocationMap then it will fetch commit blobs and
   * check those requested key and location blobs are exists or not and if those blobs are exists
   * then we will prepare map for further mapping entry with run in mapping table called
   * `versioning_modeldb_entity_mapping_config_blob`. - Also if we found the blob with CONFIG type
   * then again we are keep hyperparameter element blob mapping for those run in separate mapping
   * table called `hyperparameter_element_mapping`
   */
  public void validateAndInsertVersionedInputs(
      Handle handle,
      String runId,
      VersioningEntry versioningEntry,
      Map<String, Map.Entry<BlobExpanded, String>> locationBlobWithHashMap) {
    if (versioningEntry == null) {
      throw new InvalidArgumentException("VersionedInput not found in request");
    } else {
      // Insert version input for run in versioning_modeldb_entity_mapping mapping table
      insertVersioningInput(handle, versioningEntry, locationBlobWithHashMap, runId);

      // Insert config blob and version input mapping in
      // versioning_modeldb_entity_mapping_config_blob mapping table
      insertVersioningInputMappingConfigBlob(
          handle, versioningEntry, locationBlobWithHashMap, runId);

      // Insert hyperparameter element and run mapping in
      // hyperparameter_element_mapping table
      insertHyperparameterElementMapping(handle, versioningEntry, locationBlobWithHashMap, runId);
    }
  }

  /**
   * If KeyLocationMap exists in request then it will take locationBlobWithHashMap fetched by
   * previous function based on requested repository, commit, and key - locations and insert it in
   * run and config_blob mapping table called `versioning_modeldb_entity_mapping_config_blob`
   */
  private void insertVersioningInputMappingConfigBlob(
      Handle handle,
      VersioningEntry versioningEntry,
      Map<String, Map.Entry<BlobExpanded, String>> locationBlobWithHashMap,
      String entityId) {
    if (!versioningEntry.getKeyLocationMapMap().isEmpty()) {
      List<Map<String, Object>> argsMaps = new ArrayList<>();
      for (Map.Entry<String, Location> locationEntry :
          versioningEntry.getKeyLocationMapMap().entrySet()) {
        // Prepare location key from list of locations in Versioning entry
        var locationKey = String.join("#", locationEntry.getValue().getLocationList());
        Map.Entry<BlobExpanded, String> blobExpandedWithHashMap =
            locationBlobWithHashMap.get(locationKey);

        // Get blob from blob map for location key build from Versioning entry locations
        var blob = blobExpandedWithHashMap.getKey().getBlob();

        // If blob type have the config then we will add mapping entry in
        // versioning_modeldb_entity_mapping_config_blob
        if (blob.getContentCase().equals(Blob.ContentCase.CONFIG)) {
          var configBlobQuery =
              "SELECT cb.blob_hash, cb.config_seq_number, cb.hyperparameter_type, hecbs.name, hecbs.int_value, hecbs.float_value, hecbs.string_value "
                  + " FROM config_blob cb "
                  + " JOIN hyperparameter_element_config_blob hecbs ON cb.hyperparameter_element_config_blob_hash = hecbs.blob_hash"
                  + " WHERE cb.blob_hash = :blobHash";
          try (var findQuery = handle.createQuery(configBlobQuery)) {
            findQuery
                .bind("blobHash", blobExpandedWithHashMap.getValue())
                .map(
                    (rs, ctx) -> {
                      Map<String, Object> argsMap = new HashMap<>();
                      argsMap.put(REPOSITORY_ID_QUERY_PARAM, versioningEntry.getRepositoryId());
                      argsMap.put(COMMIT_QUERY_PARAM, versioningEntry.getCommit());
                      argsMap.put(VERSIONING_KEY_QUERY_PARAM, locationEntry.getKey());
                      argsMap.put(ENTITY_ID_QUERY_PARAM, entityId);
                      argsMap.put(ENTITY_TYPE_QUERY_PARAM, entity_type);
                      argsMap.put(BLOB_HASH_QUERY_PARAM, rs.getString(BLOB_HASH_QUERY_PARAM));
                      argsMap.put("config_seq_number", rs.getString("config_seq_number"));
                      argsMaps.add(argsMap);
                      return rs;
                    })
                .list();
          }
        }
      }
      if (!argsMaps.isEmpty()) {
        var blobMappingQueryStr =
            " INSERT INTO versioning_modeldb_entity_mapping_config_blob "
                + " (versioning_modeldb_entity_mapping_repository_id, "
                + " versioning_modeldb_entity_mapping_commit, "
                + " versioning_modeldb_entity_mapping_versioning_key, "
                + " versioning_modeldb_entity_mapping_"
                + entityIdReferenceColumn
                + ", "
                + " versioning_modeldb_entity_mapping_entity_type, "
                + " config_blob_entity_blob_hash, "
                + " config_blob_entity_config_seq_number) "
                + " VALUES (:repository_id, :commit, :versioning_key, :entityId, :entity_type, :blob_hash, :config_seq_number)";
        argsMaps.forEach(
            a -> {
              try (var updateQuery = handle.createUpdate(blobMappingQueryStr)) {
                updateQuery.bindMap(a).execute();
              }
            });
      }
    }
  }

  /**
   * If found blob with type config then we will check hyperparameter_type in config blob. it has
   * hyperparameter then we will keep run and hyperparameter element mapping in separate mapping
   * table.
   */
  private void insertHyperparameterElementMapping(
      Handle handle,
      VersioningEntry versioningEntry,
      Map<String, Map.Entry<BlobExpanded, String>> locationBlobWithHashMap,
      String entityId) {
    if (!versioningEntry.getKeyLocationMapMap().isEmpty()) {
      List<Map<String, Object>> argsMaps = new ArrayList<>();
      for (Map.Entry<String, Location> locationEntry :
          versioningEntry.getKeyLocationMapMap().entrySet()) {
        // Prepare location key from list of locations in Versioning entry
        var locationKey = String.join("#", locationEntry.getValue().getLocationList());
        Map.Entry<BlobExpanded, String> blobExpandedWithHashMap =
            locationBlobWithHashMap.get(locationKey);

        // Get blob from blob map for location key build from Versioning entry locations
        var blob = blobExpandedWithHashMap.getKey().getBlob();

        // If blob type have the config and it has the HYPERPARAMETER then we will add mapping
        // entry in hyperparameter_element_mapping
        if (blob.getContentCase().equals(Blob.ContentCase.CONFIG)) {
          var configBlobQuery =
              "SELECT hecbs.name, hecbs.int_value, hecbs.float_value, hecbs.string_value "
                  + " FROM config_blob cb "
                  + " JOIN hyperparameter_element_config_blob hecbs ON cb.hyperparameter_element_config_blob_hash = hecbs.blob_hash"
                  + " WHERE cb.blob_hash = :blobHash AND cb.hyperparameter_type = :hyperparameter_type";
          try (var findquery = handle.createQuery(configBlobQuery)) {
            findquery
                .bind("blobHash", blobExpandedWithHashMap.getValue())
                .bind("hyperparameter_type", HYPERPARAMETER)
                .map(
                    (rs, ctx) -> {
                      Map<String, Object> argsMap = new HashMap<>();
                      argsMap.put("name", rs.getString("name"));
                      argsMap.put(
                          INT_VALUE_QUERY_SELECTED_PARAM,
                          rs.getInt(INT_VALUE_QUERY_SELECTED_PARAM) != 0
                              ? rs.getInt(INT_VALUE_QUERY_SELECTED_PARAM)
                              : null);
                      argsMap.put("float_value", rs.getFloat("float_value"));
                      argsMap.put("string_value", rs.getString("string_value"));
                      argsMap.put(ENTITY_TYPE_QUERY_PARAM, entity_type);
                      argsMap.put("entity_id", entityId);
                      argsMaps.add(argsMap);
                      return rs;
                    })
                .list();
          }
        }
      }
      if (!argsMaps.isEmpty()) {
        var hemeStr =
            "INSERT INTO hyperparameter_element_mapping (name, int_value, float_value, string_value, entity_type, "
                + entityIdReferenceColumn
                + " ) VALUES (:name, :int_value, :float_value, :string_value, :entity_type, :entity_id )";
        argsMaps.forEach(
            a -> {
              try (var updateQuery = handle.createUpdate(hemeStr)) {
                updateQuery.bindMap(a).execute();
              }
            });
      }
    }
  }

  private void insertVersioningInput(
      Handle handle,
      VersioningEntry versioningEntry,
      Map<String, Map.Entry<BlobExpanded, String>> locationBlobWithHashMap,
      String entityId) {
    VersioningEntry existingVersioningEntry;
    try {
      existingVersioningEntry =
          getVersionedInputs(Collections.singleton(entityId))
              .thenApply(
                  existingVersioningEntryMap -> existingVersioningEntryMap.get(entityId), executor)
              .blockAndGet();
    } catch (Exception e) {
      throw new ModelDBException(e);
    }

    if (existingVersioningEntry != null) {
      if (existingVersioningEntry.getRepositoryId() != versioningEntry.getRepositoryId()
          || !existingVersioningEntry.getCommit().equals(versioningEntry.getCommit())) {
        throw new AlreadyExistsException(ModelDBConstants.DIFFERENT_REPOSITORY_OR_COMMIT_MESSAGE);
      }
    }

    var queryStr =
        String.format(
            "INSERT INTO versioning_modeldb_entity_mapping "
                + " (repository_id, %s, versioning_key, versioning_location, entity_type, versioning_blob_type, blob_hash, %s) "
                + " VALUES (:repository_id, :commit, :versioning_key, :versioning_location, :entity_type, :versioning_blob_type, :blob_hash, :entityId)",
            App.getInstance().mdbConfig.getDatabase().getRdbConfiguration().isMssql()
                ? "\"commit\""
                : "commit",
            entityIdReferenceColumn);

    if (versioningEntry.getKeyLocationMapMap().isEmpty()) {
      Map<String, Object> keysAndParameterMap = new HashMap<>();
      keysAndParameterMap.put(REPOSITORY_ID_QUERY_PARAM, versioningEntry.getRepositoryId());
      keysAndParameterMap.put(COMMIT_QUERY_PARAM, versioningEntry.getCommit());
      keysAndParameterMap.put(ENTITY_ID_QUERY_PARAM, entityId);
      keysAndParameterMap.put(ENTITY_TYPE_QUERY_PARAM, entity_type);
      keysAndParameterMap.put(VERSIONING_KEY_QUERY_PARAM, CommonConstants.EMPTY_STRING);
      keysAndParameterMap.put(VERSIONING_LOCATION_QUERY_PARAM, null);
      keysAndParameterMap.put("versioning_blob_type", null);
      keysAndParameterMap.put(BLOB_HASH_QUERY_PARAM, null);

      LOGGER.trace("insert experiment run query string: " + queryStr);
      try (var query = handle.createUpdate(queryStr)) {
        // Inserting fields arguments based on the keys and value of map
        for (Map.Entry<String, Object> objectEntry : keysAndParameterMap.entrySet()) {
          query.bind(objectEntry.getKey(), objectEntry.getValue());
        }
        query.execute();
      }
    } else {
      List<Map<String, Object>> argsMaps = new ArrayList<>();
      for (Map.Entry<String, Location> locationEntry :
          versioningEntry.getKeyLocationMapMap().entrySet()) {
        if (existingVersioningEntry == null
            || !existingVersioningEntry.containsKeyLocationMap(locationEntry.getKey())) {
          var locationKey = String.join("#", locationEntry.getValue().getLocationList());
          Map.Entry<BlobExpanded, String> blobExpandedWithHashMap =
              locationBlobWithHashMap.get(locationKey);

          var blob = blobExpandedWithHashMap.getKey().getBlob();

          Map<String, Object> keysAndParameterMap = new HashMap<>();
          keysAndParameterMap.put(REPOSITORY_ID_QUERY_PARAM, versioningEntry.getRepositoryId());
          keysAndParameterMap.put(COMMIT_QUERY_PARAM, versioningEntry.getCommit());
          keysAndParameterMap.put(ENTITY_ID_QUERY_PARAM, entityId);
          keysAndParameterMap.put(ENTITY_TYPE_QUERY_PARAM, entity_type);
          keysAndParameterMap.put(VERSIONING_KEY_QUERY_PARAM, locationEntry.getKey());
          keysAndParameterMap.put(
              VERSIONING_LOCATION_QUERY_PARAM,
              CommonUtils.getStringFromProtoObject(locationEntry.getValue()));
          keysAndParameterMap.put("versioning_blob_type", blob.getContentCase().getNumber());
          keysAndParameterMap.put(BLOB_HASH_QUERY_PARAM, blobExpandedWithHashMap.getValue());

          argsMaps.add(keysAndParameterMap);
        }
      }

      if (!argsMaps.isEmpty()) {
        argsMaps.forEach(
            a -> {
              try (var updateQuery = handle.createUpdate(queryStr)) {
                updateQuery.bindMap(a).execute();
              }
            });
      }
    }
  }

  /**
   * This function validate requested parameters for version_input it will validate repository and
   * commit exists or not and also if request has KeyLocationMap then it will fetch commit blobs and
   * check those requested key and location blobs are exists or not and if those blobs are exists
   * then we will prepare map for further mapping entry with run in mapping table called
   * `versioning_modeldb_entity_mapping_config_blob` and return that map
   *
   * @param versioningEntry : versioningEntry
   * @return returns a map from location to an Entry of BlobExpanded and sha
   * @throws ModelDBException ModelDBException
   */
  public InternalFuture<Map<String, Map.Entry<BlobExpanded, String>>> validateVersioningEntity(
      VersioningEntry versioningEntry) {

    final var futureTask =
        InternalFuture.runAsync(
            () -> {
              String errorMessage = null;
              if (versioningEntry.getRepositoryId() == 0L) {
                errorMessage = "Repository Id not found in VersioningEntry";
              } else if (versioningEntry.getCommit().isEmpty()) {
                errorMessage = "Commit hash not found in VersioningEntry";
              }

              if (errorMessage != null) {
                throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
              }
            },
            executor);

    return futureTask.thenCompose(
        unused -> {
          Map<String, Map.Entry<BlobExpanded, String>> requestedLocationBlobWithHashMap =
              new HashMap<>();
          try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
            // Fetch version input mapped repository
            var repositoryIdentification =
                RepositoryIdentification.newBuilder()
                    .setRepoId(versioningEntry.getRepositoryId())
                    .build();
            // Fetch version input mapped commit
            var commitEntity =
                commitDAO.getCommitEntity(
                    session,
                    versioningEntry.getCommit(),
                    (session1) ->
                        repositoryDAO.getRepositoryById(session, repositoryIdentification));

            // If key and location mapping exists in the  versioned input request then we will fetch
            // those mapping based location key and validate
            if (!versioningEntry.getKeyLocationMapMap().isEmpty()) {
              Map<String, Map.Entry<BlobExpanded, String>> locationBlobWithHashMap =
                  blobDAO.getCommitBlobMapWithHash(
                      session,
                      commitEntity.getRootSha(),
                      new ArrayList<>(),
                      Collections.emptyList());
              for (Map.Entry<String, Location> locationBlobKeyMap :
                  versioningEntry.getKeyLocationMapMap().entrySet()) {
                var locationKey = String.join("#", locationBlobKeyMap.getValue().getLocationList());
                // If requested locations and commit blob locations are not matched then we will
                // throw error for logging invalid versioned input
                if (!locationBlobWithHashMap.containsKey(locationKey)) {
                  return InternalFuture.failedStage(
                      new ModelDBException(
                          "Blob Location '"
                              + locationBlobKeyMap.getValue().getLocationList()
                              + "' for key '"
                              + locationBlobKeyMap.getKey()
                              + "' not found in commit blobs",
                          Code.INVALID_ARGUMENT));
                }
                requestedLocationBlobWithHashMap.put(
                    locationKey, locationBlobWithHashMap.get(locationKey));
              }
            }
          } catch (Exception ex) {
            return InternalFuture.failedStage(ex);
          }
          return InternalFuture.completedInternalFuture(requestedLocationBlobWithHashMap);
        },
        executor);
  }

  // We have a mapping table for the runs and VersionedInput called
  // versioning_modeldb_entity_mapping so while getting runs we will fetch versioned inout from
  // there.
  public InternalFuture<Map<String, VersioningEntry>> getVersionedInputs(Set<String> runIds) {
    return jdbi.withHandle(
            handle -> {
              try (var findQuery =
                  handle.createQuery(
                      String.format(
                          "select vm.experiment_run_id, vm.repository_id, vm.%s, vm.versioning_key, vm.versioning_location  "
                              + " from versioning_modeldb_entity_mapping as vm "
                              + " where vm.experiment_run_id in (<run_ids>) "
                              + " and vm.entity_type = :entityType",
                          App.getInstance().mdbConfig.getDatabase().getRdbConfiguration().isMssql()
                              ? "\"commit\""
                              : "commit"))) {
                return findQuery
                    .bindList("run_ids", runIds)
                    .bind("entityType", entity_type)
                    .map(
                        (rs, ctx) -> {
                          // Preparing VersionedInput (VersioningEntry) from the mapping table
                          // based on run ids but
                          // here we have store multiple locations key for single version key see
                          // line 299 loop so
                          // we are creating SimpleEntry based on each location key.
                          var versioningEntryBuilder =
                              VersioningEntry.newBuilder()
                                  .setRepositoryId(rs.getLong(REPOSITORY_ID_QUERY_PARAM))
                                  .setCommit(rs.getString(COMMIT_QUERY_PARAM));

                          if (rs.getString(VERSIONING_KEY_QUERY_PARAM) != null
                              && !rs.getString(VERSIONING_KEY_QUERY_PARAM).isEmpty()) {
                            var locationBuilder = Location.newBuilder();
                            CommonUtils.getProtoObjectFromString(
                                rs.getString(VERSIONING_LOCATION_QUERY_PARAM), locationBuilder);
                            versioningEntryBuilder.putKeyLocationMap(
                                rs.getString(VERSIONING_KEY_QUERY_PARAM), locationBuilder.build());
                          }

                          return new AbstractMap.SimpleEntry<>(
                              rs.getString("experiment_run_id"), versioningEntryBuilder.build());
                        })
                    .list();
              }
            })
        .thenCompose(
            simpleEntries -> {
              // Key= experiment_run_id, Value = VersionedInput
              Map<String, VersioningEntry> entryMap = new LinkedHashMap<>();

              // We have multiple location keys for single versionedInput in `simpleEntries` map so
              // now we are adding all location map in to single VersionedInput for runs.
              for (Map.Entry<String, VersioningEntry> entry : simpleEntries) {
                if (entryMap.containsKey(entry.getKey())) {
                  var versioningEntry = entryMap.get(entry.getKey());
                  versioningEntry =
                      versioningEntry
                          .toBuilder()
                          .putAllKeyLocationMap(entry.getValue().getKeyLocationMapMap())
                          .build();
                  entryMap.put(entry.getKey(), versioningEntry);
                } else {
                  entryMap.put(entry.getKey(), entry.getValue());
                }
              }
              return InternalFuture.completedInternalFuture(entryMap);
            },
            executor);
  }
}
