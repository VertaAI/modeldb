package ai.verta.modeldb.experimentRun.subtypes;

import static ai.verta.modeldb.entities.config.ConfigBlobEntity.HYPERPARAMETER;

import ai.verta.modeldb.DAOSet;
import ai.verta.modeldb.ExperimentRun;
import ai.verta.modeldb.Location;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.VersioningEntry;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.Blob;
import ai.verta.modeldb.versioning.BlobDAO;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.CommitDAO;
import ai.verta.modeldb.versioning.RepositoryDAO;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

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

  public VersionInputHandler(Executor executor, FutureJdbi jdbi, String entityName, DAOSet daoSet) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.entity_type = entityName;

    this.repositoryDAO = daoSet.repositoryDAO;
    this.commitDAO = daoSet.commitDAO;
    this.blobDAO = daoSet.blobDAO;

    switch (entityName) {
      case "ExperimentRunEntity":
        this.entityIdReferenceColumn = "experiment_run_id";
        break;
      default:
        throw new InternalErrorException("Invalid entity name: " + entityName);
    }
  }

  public InternalFuture<Void> validateAndInsertVersionedInputs(ExperimentRun experimentRun) {
    if (experimentRun.hasVersionedInputs()) {
      InternalFuture<Map<String, Map.Entry<BlobExpanded, String>>> versionedInputFutureTask =
          validateVersioningEntity(experimentRun.getVersionedInputs());
      return versionedInputFutureTask.thenCompose(
          locationBlobWithHashMap ->
              insetVersioningInput(
                  experimentRun.getVersionedInputs(),
                  locationBlobWithHashMap,
                  experimentRun.getId()),
          executor);
    }
    return InternalFuture.runAsync(() -> {}, executor);
  }

  private InternalFuture<Void> insetVersioningInput(
      VersioningEntry versioningEntry,
      Map<String, Map.Entry<BlobExpanded, String>> locationBlobWithHashMap,
      String entityId) {
    return jdbi.useHandle(
        handle -> {
          var queryStr =
              "INSERT INTO versioning_modeldb_entity_mapping "
                  + " (repository_id, commit, versioning_key, versioning_location, entity_type, versioning_blob_type, blob_hash, "
                  + entityIdReferenceColumn
                  + ") "
                  + " VALUES (:repository_id, :commit, :versioning_key, :versioning_location, :entity_type, :versioning_blob_type, :blob_hash, :entityId)";
          if (versioningEntry.getKeyLocationMapMap().isEmpty()) {
            handle
                .createUpdate(queryStr)
                .bind("repository_id", versioningEntry.getRepositoryId())
                .bind("commit", versioningEntry.getCommit())
                .bind("versioning_key", ModelDBConstants.EMPTY_STRING)
                .bind("versioning_location", ModelDBConstants.EMPTY_STRING)
                .bind("versioning_blob_type", ModelDBConstants.EMPTY_STRING)
                .bind("blob_hash", ModelDBConstants.EMPTY_STRING)
                .bind("entity_type", entity_type)
                .bind("entityId", entityId)
                .execute();
          } else {
            for (Map.Entry<String, Location> locationEntry :
                versioningEntry.getKeyLocationMapMap().entrySet()) {
              String locationKey = String.join("#", locationEntry.getValue().getLocationList());
              Map.Entry<BlobExpanded, String> blobExpandedWithHashMap =
                  locationBlobWithHashMap.get(locationKey);

              Blob blob = blobExpandedWithHashMap.getKey().getBlob();

              handle
                  .createUpdate(queryStr)
                  .bind("repository_id", versioningEntry.getRepositoryId())
                  .bind("commit", versioningEntry.getCommit())
                  .bind("versioning_key", locationEntry.getKey())
                  .bind(
                      "versioning_location",
                      ModelDBUtils.getStringFromProtoObject(locationEntry.getValue()))
                  .bind("versioning_blob_type", blob.getContentCase().getNumber())
                  .bind("blob_hash", blobExpandedWithHashMap.getValue())
                  .bind("entity_type", entity_type)
                  .bind("entityId", entityId)
                  .execute();

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
                          String blobMappingQueryStr =
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
                          handle
                              .createUpdate(blobMappingQueryStr)
                              .bind("repository_id", versioningEntry.getRepositoryId())
                              .bind("commit", versioningEntry.getCommit())
                              .bind("versioning_key", locationEntry.getKey())
                              .bind("entityId", entityId)
                              .bind("entity_type", entity_type)
                              .bind("blob_hash", rs.getString("blob_hash"))
                              .bind("config_seq_number", rs.getString("config_seq_number"))
                              .execute();

                          // Insert prepare hyperparameter elemMappings
                          if (rs.getInt("hyperparameter_type") == HYPERPARAMETER) {
                            String hemeStr =
                                "INSERT INTO hyperparameter_element_mapping (name, int_value, float_value, string_value, entity_type, "
                                    + entityIdReferenceColumn
                                    + " ) VALUES (:name, :int_value, :float_value, :string_value, :entity_type, :entity_id )";
                            handle
                                .createUpdate(hemeStr)
                                .bind("name", rs.getString("name"))
                                .bind(
                                    "int_value",
                                    rs.getInt("int_value") != 0 ? rs.getInt("int_value") : null)
                                .bind("float_value", rs.getFloat("float_value"))
                                .bind("string_value", rs.getString("string_value"))
                                .bind("entity_type", entity_type)
                                .bind("entity_id", entityId)
                                .execute();
                          }
                          return rs;
                        })
                    .list();
              }
            }
          }
        });
  }

  /**
   * @param versioningEntry : versioningEntry
   * @return returns a map from location to an Entry of BlobExpanded and sha
   * @throws ModelDBException ModelDBException
   */
  private InternalFuture<Map<String, Map.Entry<BlobExpanded, String>>> validateVersioningEntity(
      VersioningEntry versioningEntry) {
    String errorMessage = null;
    if (versioningEntry.getRepositoryId() == 0L) {
      errorMessage = "Repository Id not found in VersioningEntry";
    } else if (versioningEntry.getCommit().isEmpty()) {
      errorMessage = "Commit hash not found in VersioningEntry";
    }

    if (errorMessage != null) {
      throw new ModelDBException(errorMessage, io.grpc.Status.Code.INVALID_ARGUMENT);
    }
    Map<String, Map.Entry<BlobExpanded, String>> requestedLocationBlobWithHashMap = new HashMap<>();
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryIdentification repositoryIdentification =
          RepositoryIdentification.newBuilder()
              .setRepoId(versioningEntry.getRepositoryId())
              .build();
      CommitEntity commitEntity =
          commitDAO.getCommitEntity(
              session,
              versioningEntry.getCommit(),
              (session1) -> repositoryDAO.getRepositoryById(session, repositoryIdentification));
      if (!versioningEntry.getKeyLocationMapMap().isEmpty()) {
        Map<String, Map.Entry<BlobExpanded, String>> locationBlobWithHashMap =
            blobDAO.getCommitBlobMapWithHash(
                session, commitEntity.getRootSha(), new ArrayList<>(), Collections.emptyList());
        for (Map.Entry<String, Location> locationBlobKeyMap :
            versioningEntry.getKeyLocationMapMap().entrySet()) {
          String locationKey = String.join("#", locationBlobKeyMap.getValue().getLocationList());
          if (!locationBlobWithHashMap.containsKey(locationKey)) {
            throw new ModelDBException(
                "Blob Location '"
                    + locationBlobKeyMap.getValue().getLocationList()
                    + "' for key '"
                    + locationBlobKeyMap.getKey()
                    + "' not found in commit blobs",
                io.grpc.Status.Code.INVALID_ARGUMENT);
          }
          requestedLocationBlobWithHashMap.put(
              locationKey, locationBlobWithHashMap.get(locationKey));
        }
      }
    } catch (ModelDBException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ModelDBException(ex);
    }
    return InternalFuture.completedInternalFuture(requestedLocationBlobWithHashMap);
  }
}
