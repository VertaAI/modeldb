package ai.verta.modeldb.experimentRun.subtypes;

import static ai.verta.modeldb.entities.config.ConfigBlobEntity.HYPERPARAMETER;

import ai.verta.modeldb.Location;
import ai.verta.modeldb.common.ModelDBConstants;
import ai.verta.modeldb.VersioningEntry;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.Blob;
import ai.verta.modeldb.versioning.BlobDAO;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.CommitDAO;
import ai.verta.modeldb.versioning.RepositoryDAO;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Code;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.jdbi.v3.core.statement.PreparedBatch;

public class VersionInputHandler {
  private static Logger LOGGER = LogManager.getLogger(VersionInputHandler.class);

  private final Executor executor;
  private final FutureJdbi jdbi;
  private final String entity_type;
  private final String entityIdReferenceColumn;
  private final RepositoryDAO repositoryDAO;
  private final CommitDAO commitDAO;
  private final BlobDAO blobDAO;
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();

  public VersionInputHandler(
      Executor executor,
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
  public InternalFuture<Void> validateAndInsertVersionedInputs(
      String runId, VersioningEntry versioningEntry) {
    if (versioningEntry == null) {
      return InternalFuture.failedStage(
          new InvalidArgumentException("VersionedInput not found in request"));
    } else {
      InternalFuture<Map<String, Map.Entry<BlobExpanded, String>>> versionedInputFutureTask =
          validateVersioningEntity(versioningEntry);
      return versionedInputFutureTask.thenCompose(
          locationBlobWithHashMap ->
              // Insert version input for run in versioning_modeldb_entity_mapping mapping table
              insertVersioningInput(versioningEntry, locationBlobWithHashMap, runId)
                  .thenCompose(
                      unused ->
                          // Insert config blob and version input mapping in
                          // versioning_modeldb_entity_mapping_config_blob mapping table
                          insertVersioningInputMappingConfigBlob(
                              versioningEntry, locationBlobWithHashMap, runId),
                      executor)
                  .thenCompose(
                      unused ->
                          // Insert hyperparameter element and run mapping in
                          // hyperparameter_element_mapping table
                          insertHyperparameterElementMapping(
                              versioningEntry, locationBlobWithHashMap, runId),
                      executor),
          executor);
    }
  }

  /**
   * If KeyLocationMap exists in request then it will take locationBlobWithHashMap fetched by
   * previous function based on requested repository, commit, and key - locations and insert it in
   * run and config_blob mapping table called `versioning_modeldb_entity_mapping_config_blob`
   */
  private InternalFuture<Void> insertVersioningInputMappingConfigBlob(
      VersioningEntry versioningEntry,
      Map<String, Map.Entry<BlobExpanded, String>> locationBlobWithHashMap,
      String entityId) {
    if (!versioningEntry.getKeyLocationMapMap().isEmpty()) {
      return jdbi.useHandle(
          handle -> {
            List<Map<String, Object>> argsMaps = new ArrayList<>();
            for (Map.Entry<String, Location> locationEntry :
                versioningEntry.getKeyLocationMapMap().entrySet()) {
              // Prepare location key from list of locations in Versioning entry
              String locationKey = String.join("#", locationEntry.getValue().getLocationList());
              Map.Entry<BlobExpanded, String> blobExpandedWithHashMap =
                  locationBlobWithHashMap.get(locationKey);

              // Get blob from blob map for location key build from Versioning entry locations
              Blob blob = blobExpandedWithHashMap.getKey().getBlob();

              // If blob type have the config then we will add mapping entry in
              // versioning_modeldb_entity_mapping_config_blob
              if (blob.getContentCase().equals(Blob.ContentCase.CONFIG)) {
                var configBlobQuery =
                    "SELECT cb.blob_hash, cb.config_seq_number, cb.hyperparameter_type, hecbs.name, hecbs.int_value, hecbs.float_value, hecbs.string_value "
                        + " FROM config_blob cb "
                        + " JOIN hyperparameter_element_config_blob hecbs ON cb.hyperparameter_element_config_blob_hash = hecbs.blob_hash"
                        + " WHERE cb.blob_hash = :blobHash";
                handle
                    .createQuery(configBlobQuery)
                    .bind("blobHash", blobExpandedWithHashMap.getValue())
                    .map(
                        (rs, ctx) -> {
                          Map<String, Object> argsMap = new HashMap<>();
                          argsMap.put("repository_id", versioningEntry.getRepositoryId());
                          argsMap.put("commit", versioningEntry.getCommit());
                          argsMap.put("versioning_key", locationEntry.getKey());
                          argsMap.put("entityId", entityId);
                          argsMap.put("entity_type", entity_type);
                          argsMap.put("blob_hash", rs.getString("blob_hash"));
                          argsMap.put("config_seq_number", rs.getString("config_seq_number"));
                          argsMaps.add(argsMap);
                          return rs;
                        })
                    .list();
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
              PreparedBatch preparedBatch = handle.prepareBatch(blobMappingQueryStr);
              argsMaps.forEach(preparedBatch::add);
              int[] insertedCount = preparedBatch.execute();
              LOGGER.trace(
                  "Inserted versioning_modeldb_entity_mapping_config_blob count: "
                      + insertedCount.length);
            }
          });
    } else {
      return InternalFuture.runAsync(() -> {}, executor);
    }
  }

  /**
   * If found blob with type config then we will check hyperparameter_type in config blob. it has
   * hyperparameter then we will keep run and hyperparameter element mapping in separate mapping
   * table.
   */
  private InternalFuture<Void> insertHyperparameterElementMapping(
      VersioningEntry versioningEntry,
      Map<String, Map.Entry<BlobExpanded, String>> locationBlobWithHashMap,
      String entityId) {
    if (!versioningEntry.getKeyLocationMapMap().isEmpty()) {
      return jdbi.useHandle(
          handle -> {
            List<Map<String, Object>> argsMaps = new ArrayList<>();
            for (Map.Entry<String, Location> locationEntry :
                versioningEntry.getKeyLocationMapMap().entrySet()) {
              // Prepare location key from list of locations in Versioning entry
              String locationKey = String.join("#", locationEntry.getValue().getLocationList());
              Map.Entry<BlobExpanded, String> blobExpandedWithHashMap =
                  locationBlobWithHashMap.get(locationKey);

              // Get blob from blob map for location key build from Versioning entry locations
              Blob blob = blobExpandedWithHashMap.getKey().getBlob();

              // If blob type have the config and it has the HYPERPARAMETER then we will add mapping
              // entry in hyperparameter_element_mapping
              if (blob.getContentCase().equals(Blob.ContentCase.CONFIG)) {
                var configBlobQuery =
                    "SELECT hecbs.name, hecbs.int_value, hecbs.float_value, hecbs.string_value "
                        + " FROM config_blob cb "
                        + " JOIN hyperparameter_element_config_blob hecbs ON cb.hyperparameter_element_config_blob_hash = hecbs.blob_hash"
                        + " WHERE cb.blob_hash = :blobHash AND cb.hyperparameter_type = :hyperparameter_type";
                handle
                    .createQuery(configBlobQuery)
                    .bind("blobHash", blobExpandedWithHashMap.getValue())
                    .bind("hyperparameter_type", HYPERPARAMETER)
                    .map(
                        (rs, ctx) -> {
                          Map<String, Object> argsMap = new HashMap<>();
                          argsMap.put("name", rs.getString("name"));
                          argsMap.put(
                              "int_value",
                              rs.getInt("int_value") != 0 ? rs.getInt("int_value") : null);
                          argsMap.put("float_value", rs.getFloat("float_value"));
                          argsMap.put("string_value", rs.getString("string_value"));
                          argsMap.put("entity_type", entity_type);
                          argsMap.put("entity_id", entityId);
                          argsMaps.add(argsMap);
                          return rs;
                        })
                    .list();
              }
            }
            if (!argsMaps.isEmpty()) {
              var hemeStr =
                  "INSERT INTO hyperparameter_element_mapping (name, int_value, float_value, string_value, entity_type, "
                      + entityIdReferenceColumn
                      + " ) VALUES (:name, :int_value, :float_value, :string_value, :entity_type, :entity_id )";
              PreparedBatch preparedBatch = handle.prepareBatch(hemeStr);
              argsMaps.forEach(preparedBatch::add);
              int[] insertedCount = preparedBatch.execute();
              LOGGER.trace(
                  "Inserted hyperparameter_element_mapping count: " + insertedCount.length);
            }
          });
    }
    return InternalFuture.runAsync(() -> {}, executor);
  }

  private InternalFuture<Void> insertVersioningInput(
      VersioningEntry versioningEntry,
      Map<String, Map.Entry<BlobExpanded, String>> locationBlobWithHashMap,
      String entityId) {
    var existingEntityFuture =
        getVersionedInputs(Collections.singleton(entityId))
            .thenApply(
                existingVersioningEntryMap -> existingVersioningEntryMap.get(entityId), executor);
    return existingEntityFuture.thenCompose(
        existingVersioningEntry -> {
          if (existingVersioningEntry != null) {
            if (existingVersioningEntry.getRepositoryId() != versioningEntry.getRepositoryId()
                || !existingVersioningEntry.getCommit().equals(versioningEntry.getCommit())) {
              return InternalFuture.failedStage(
                  new AlreadyExistsException(
                      ModelDBConstants.DIFFERENT_REPOSITORY_OR_COMMIT_MESSAGE));
            }
          }

          return jdbi.useHandle(
              handle -> {
                var queryStr =
                    "INSERT INTO versioning_modeldb_entity_mapping "
                        + " (repository_id, commit, versioning_key, versioning_location, entity_type, versioning_blob_type, blob_hash, "
                        + entityIdReferenceColumn
                        + ") "
                        + " VALUES (:repository_id, :commit, :versioning_key, :versioning_location, :entity_type, :versioning_blob_type, :blob_hash, :entityId)";

                if (versioningEntry.getKeyLocationMapMap().isEmpty()) {
                  Map<String, Object> keysAndParameterMap = new HashMap<>();
                  keysAndParameterMap.put("repository_id", versioningEntry.getRepositoryId());
                  keysAndParameterMap.put("commit", versioningEntry.getCommit());
                  keysAndParameterMap.put("entityId", entityId);
                  keysAndParameterMap.put("entity_type", entity_type);
                  keysAndParameterMap.put("versioning_key", ModelDBConstants.EMPTY_STRING);
                  keysAndParameterMap.put("versioning_location", null);
                  keysAndParameterMap.put("versioning_blob_type", null);
                  keysAndParameterMap.put("blob_hash", null);

                  LOGGER.trace("insert experiment run query string: " + queryStr);
                  var query = handle.createUpdate(queryStr);

                  // Inserting fields arguments based on the keys and value of map
                  for (Map.Entry<String, Object> objectEntry : keysAndParameterMap.entrySet()) {
                    query.bind(objectEntry.getKey(), objectEntry.getValue());
                  }
                  query.execute();
                } else {
                  List<Map<String, Object>> argsMaps = new ArrayList<>();
                  for (Map.Entry<String, Location> locationEntry :
                      versioningEntry.getKeyLocationMapMap().entrySet()) {
                    if (existingVersioningEntry == null
                        || !existingVersioningEntry.containsKeyLocationMap(
                            locationEntry.getKey())) {
                      String locationKey =
                          String.join("#", locationEntry.getValue().getLocationList());
                      Map.Entry<BlobExpanded, String> blobExpandedWithHashMap =
                          locationBlobWithHashMap.get(locationKey);

                      Blob blob = blobExpandedWithHashMap.getKey().getBlob();

                      Map<String, Object> keysAndParameterMap = new HashMap<>();
                      keysAndParameterMap.put("repository_id", versioningEntry.getRepositoryId());
                      keysAndParameterMap.put("commit", versioningEntry.getCommit());
                      keysAndParameterMap.put("entityId", entityId);
                      keysAndParameterMap.put("entity_type", entity_type);
                      keysAndParameterMap.put("versioning_key", locationEntry.getKey());
                      keysAndParameterMap.put(
                          "versioning_location",
                          ModelDBUtils.getStringFromProtoObject(locationEntry.getValue()));
                      keysAndParameterMap.put(
                          "versioning_blob_type", blob.getContentCase().getNumber());
                      keysAndParameterMap.put("blob_hash", blobExpandedWithHashMap.getValue());

                      argsMaps.add(keysAndParameterMap);
                    }
                  }

                  if (!argsMaps.isEmpty()) {
                    PreparedBatch preparedBatch = handle.prepareBatch(queryStr);
                    argsMaps.forEach(preparedBatch::add);
                    int[] insertedCount = preparedBatch.execute();
                    LOGGER.trace(
                        "Inserted versioning_modeldb_entity_mapping count: "
                            + insertedCount.length);
                  }
                }
              });
        },
        executor);
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
  private InternalFuture<Map<String, Map.Entry<BlobExpanded, String>>> validateVersioningEntity(
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
          try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
            // Fetch version input mapped repository
            RepositoryIdentification repositoryIdentification =
                RepositoryIdentification.newBuilder()
                    .setRepoId(versioningEntry.getRepositoryId())
                    .build();
            // Fetch version input mapped commit
            CommitEntity commitEntity =
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
                String locationKey =
                    String.join("#", locationBlobKeyMap.getValue().getLocationList());
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
            handle ->
                handle
                    .createQuery(
                        "select vm.experiment_run_id, vm.repository_id, vm.commit, vm.versioning_key, vm.versioning_location  "
                            + " from versioning_modeldb_entity_mapping as vm "
                            + " where vm.experiment_run_id in (<run_ids>) "
                            + " and vm.entity_type = :entityType")
                    .bindList("run_ids", runIds)
                    .bind("entityType", entity_type)
                    .map(
                        (rs, ctx) -> {
                          try {
                            // Preparing VersionedInput (VersioningEntry) from the mapping table
                            // based on run ids but
                            // here we have store multiple locations key for single version key see
                            // line 299 loop so
                            // we are creating SimpleEntry based on each location key.
                            VersioningEntry.Builder versioningEntryBuilder =
                                VersioningEntry.newBuilder()
                                    .setRepositoryId(rs.getLong("repository_id"))
                                    .setCommit(rs.getString("commit"));

                            if (rs.getString("versioning_key") != null
                                && !rs.getString("versioning_key").isEmpty()) {
                              Location.Builder locationBuilder = Location.newBuilder();
                              CommonUtils.getProtoObjectFromString(
                                  rs.getString("versioning_location"), locationBuilder);
                              versioningEntryBuilder.putKeyLocationMap(
                                  rs.getString("versioning_key"), locationBuilder.build());
                            }

                            return new AbstractMap.SimpleEntry<>(
                                rs.getString("experiment_run_id"), versioningEntryBuilder.build());
                          } catch (InvalidProtocolBufferException e) {
                            LOGGER.error(
                                "Error generating builder for {}",
                                rs.getString("versioning_location"));
                            throw new ModelDBException(e);
                          }
                        })
                    .list())
        .thenCompose(
            simpleEntries -> {
              // Key= experiment_run_id, Value = VersionedInput
              Map<String, VersioningEntry> entryMap = new LinkedHashMap<>();

              // We have multiple location keys for single versionedInput in `simpleEntries` map so
              // now we are adding all location map in to single VersionedInput for runs.
              for (Map.Entry<String, VersioningEntry> entry : simpleEntries) {
                if (entryMap.containsKey(entry.getKey())) {
                  VersioningEntry versioningEntry = entryMap.get(entry.getKey());
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
