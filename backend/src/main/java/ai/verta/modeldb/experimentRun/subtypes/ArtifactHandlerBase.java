package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.Artifact;
import ai.verta.modeldb.App;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.config.MDBArtifactStoreConfig;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.config.TrialConfig;
import ai.verta.modeldb.exceptions.AlreadyExistsException;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.utils.TrialUtils;
import java.util.AbstractMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class ArtifactHandlerBase {
  private static final String FIELD_TYPE_QUERY_PARAM = "field_type";
  private static final String ENTITY_NAME_QUERY_PARAM = "entity_name";
  private static final String ENTITY_ID_QUERY_PARAM = "entity_id";
  protected final Executor executor;
  protected final FutureJdbi jdbi;
  protected final String fieldType;
  protected final String entityName;
  protected final String entityIdReferenceColumn;
  private final MDBArtifactStoreConfig artifactStoreConfig;
  private final TrialConfig trialConfig;

  protected String getTableName() {
    return "artifact";
  }

  public ArtifactHandlerBase(
      Executor executor, FutureJdbi jdbi, String fieldType, String entityName) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.fieldType = fieldType;
    this.entityName = entityName;
    MDBConfig mdbConfig = App.getInstance().mdbConfig;
    this.artifactStoreConfig = mdbConfig.artifactStoreConfig;
    this.trialConfig = mdbConfig.trial;

    switch (entityName) {
      case "ProjectEntity":
        this.entityIdReferenceColumn = "project_id";
        break;
      case "ExperimentRunEntity":
        this.entityIdReferenceColumn = "experiment_run_id";
        break;
      default:
        throw new InternalErrorException("Invalid entity name: " + entityName);
    }
  }

  protected InternalFuture<Optional<Long>> getArtifactId(String entityId, String key) {
    return InternalFuture.runAsync(
            () -> {
              if (key.isEmpty()) {
                throw new InvalidArgumentException("Key must be provided");
              }
            },
            executor)
        .thenCompose(
            unused ->
                jdbi.withHandle(
                    handle ->
                        handle
                            .createQuery(
                                String.format(
                                    "select id from %s where entity_name=:entity_name and field_type=:field_type and %s =:entity_id and ar_key=:ar_key",
                                    getTableName(), entityIdReferenceColumn))
                            .bind(ENTITY_ID_QUERY_PARAM, entityId)
                            .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
                            .bind(ENTITY_NAME_QUERY_PARAM, entityName)
                            .bind("ar_key", key)
                            .mapTo(Long.class)
                            .findOne()),
            executor);
  }

  public InternalFuture<List<Artifact>> getArtifacts(String entityId, Optional<String> maybeKey) {
    var currentFuture =
        InternalFuture.runAsync(
            () -> {
              if (entityId == null || entityId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.ENTITY_ID_IS_EMPTY_ERROR);
              }
            },
            executor);
    return currentFuture.thenCompose(
        unused ->
            jdbi.withHandle(
                handle -> {
                  var queryStr =
                      String.format(
                          "select ar_key as k, ar_path as p, artifact_type as at, path_only as po, linked_artifact_id as lai, filename_extension as fe, serialization as ser, artifact_subtype as ast, upload_completed as uc from %s where entity_name=:entity_name and field_type=:field_type and %s =:entity_id",
                          getTableName(), entityIdReferenceColumn);

                  if (maybeKey.isPresent()) {
                    queryStr = queryStr + " AND ar_key=:ar_key ";
                  }

                  var query =
                      handle
                          .createQuery(queryStr)
                          .bind(ENTITY_ID_QUERY_PARAM, entityId)
                          .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
                          .bind(ENTITY_NAME_QUERY_PARAM, entityName);
                  maybeKey.ifPresent(s -> query.bind("ar_key", s));
                  return query
                      .map(
                          (rs, ctx) ->
                              Artifact.newBuilder()
                                  .setKey(rs.getString("k"))
                                  .setPath(rs.getString("p"))
                                  .setArtifactTypeValue(rs.getInt("at"))
                                  .setPathOnly(rs.getBoolean("po"))
                                  .setLinkedArtifactId(rs.getString("lai"))
                                  .setFilenameExtension(rs.getString("fe"))
                                  .setSerialization(rs.getString("ser"))
                                  .setArtifactSubtype(rs.getString("ast"))
                                  .setUploadCompleted(rs.getBoolean("uc"))
                                  .build())
                      .list();
                }),
        executor);
  }

  public InternalFuture<MapSubtypes<Artifact>> getArtifactsMap(Set<String> entityIds) {
    return jdbi.withHandle(
            handle -> {
              var queryStr =
                  String.format(
                      "select ar_key as k, ar_path as p, artifact_type as at, path_only as po, linked_artifact_id as lai, filename_extension as fe, serialization as ser, artifact_subtype as ast, upload_completed as uc, %s as entity_id from %s where entity_name=:entity_name and field_type=:field_type and %s in (<entity_ids>)",
                      entityIdReferenceColumn, getTableName(), entityIdReferenceColumn);

              var query =
                  handle
                      .createQuery(queryStr)
                      .bindList("entity_ids", entityIds)
                      .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
                      .bind(ENTITY_NAME_QUERY_PARAM, entityName);

              return query
                  .map(
                      (rs, ctx) ->
                          new AbstractMap.SimpleEntry<>(
                              rs.getString(ENTITY_ID_QUERY_PARAM),
                              Artifact.newBuilder()
                                  .setKey(rs.getString("k"))
                                  .setPath(rs.getString("p"))
                                  .setArtifactTypeValue(rs.getInt("at"))
                                  .setPathOnly(rs.getBoolean("po"))
                                  .setLinkedArtifactId(rs.getString("lai"))
                                  .setFilenameExtension(rs.getString("fe"))
                                  .setSerialization(rs.getString("ser"))
                                  .setArtifactSubtype(rs.getString("ast"))
                                  .setUploadCompleted(rs.getBoolean("uc"))
                                  .build()))
                  .list();
            })
        .thenApply(MapSubtypes::from, executor);
  }

  public InternalFuture<Void> logArtifacts(
      String entityId, List<Artifact> artifacts, boolean overwrite) {
    // Validate input
    return InternalFuture.runAsync(
            () -> {
              if (entityId == null || entityId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.ENTITY_ID_IS_EMPTY_ERROR);
              }
              for (final var artifact : artifacts) {
                String errorMessage = null;
                if (artifact.getKey().isEmpty()
                    && (artifact.getPathOnly() && artifact.getPath().isEmpty())) {
                  errorMessage = "Artifact key and Artifact path not found in request";
                } else if (artifact.getKey().isEmpty()) {
                  errorMessage = "Artifact key not found in request";
                } else if (artifact.getPathOnly() && artifact.getPath().isEmpty()) {
                  errorMessage = "Artifact path not found in request";
                }

                if (errorMessage != null) {
                  throw new InvalidArgumentException(errorMessage);
                }
              }
            },
            executor)
        .thenCompose(
            unused ->
                // Check for conflicts
                jdbi.useHandle(
                    handle -> {
                      if (overwrite) {
                        handle
                            .createUpdate(
                                String.format(
                                    "delete from %s where entity_name=:entity_name and field_type=:field_type and ar_key in (<keys>) and %s =:entity_id",
                                    getTableName(), entityIdReferenceColumn))
                            .bindList(
                                "keys",
                                artifacts.stream()
                                    .map(Artifact::getKey)
                                    .collect(Collectors.toList()))
                            .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
                            .bind(ENTITY_NAME_QUERY_PARAM, entityName)
                            .bind(ENTITY_ID_QUERY_PARAM, entityId)
                            .execute();
                      } else {
                        for (final var artifact : artifacts) {
                          handle
                              .createQuery(
                                  String.format(
                                      "select id from %s where entity_name=:entity_name and field_type=:field_type and ar_key=:key and %s =:entity_id",
                                      getTableName(), entityIdReferenceColumn))
                              .bind("key", artifact.getKey())
                              .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
                              .bind(ENTITY_NAME_QUERY_PARAM, entityName)
                              .bind(ENTITY_ID_QUERY_PARAM, entityId)
                              .mapTo(Long.class)
                              .findOne()
                              .ifPresent(
                                  present -> {
                                    throw new AlreadyExistsException(
                                        "Key '" + artifact.getKey() + "' already exists");
                                  });
                        }
                      }
                    }),
            executor)
        .thenAccept(
            unused -> {
              if (entityName.equals("ExperimentRunEntity") && fieldType.equals("artifacts")) {
                jdbi.withHandle(
                        handle ->
                            handle
                                .createQuery(
                                    String.format(
                                        "select count(id) from %s where entity_name=:entity_name and field_type=:field_type and %s =:entity_id ",
                                        getTableName(), entityIdReferenceColumn))
                                .bind(ENTITY_ID_QUERY_PARAM, entityId)
                                .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
                                .bind(ENTITY_NAME_QUERY_PARAM, entityName)
                                .mapTo(Long.class)
                                .one())
                    .thenAccept(
                        count ->
                            TrialUtils.validateMaxArtifactsForTrial(
                                trialConfig, artifacts.size(), count.intValue()),
                        executor);
              }
            },
            executor)
        .thenCompose(
            unused ->
                // Log
                jdbi.useHandle(
                    handle -> {
                      for (final var artifact : artifacts) {
                        var storeTypePath =
                            !artifact.getPathOnly()
                                ? artifactStoreConfig.storeTypePathPrefix() + artifact.getPath()
                                : "";
                        handle
                            .createUpdate(
                                "insert into "
                                    + getTableName()
                                    + " (entity_name, field_type, ar_key, ar_path, artifact_type, path_only, linked_artifact_id, filename_extension, store_type_path, serialization, artifact_subtype, upload_completed, "
                                    + entityIdReferenceColumn
                                    + ") "
                                    + "values (:entity_name, :field_type, :key, :path, :type,:path_only,:linked_artifact_id,:filename_extension,:store_type_path, :serialization, :artifact_subtype, :upload_completed, :entity_id)")
                            .bind("key", artifact.getKey())
                            .bind("path", artifact.getPath())
                            .bind("type", artifact.getArtifactTypeValue())
                            .bind("path_only", artifact.getPathOnly())
                            .bind("linked_artifact_id", artifact.getLinkedArtifactId())
                            .bind("filename_extension", artifact.getFilenameExtension())
                            .bind(
                                "upload_completed",
                                !artifactStoreConfig
                                    .getArtifactStoreType()
                                    .equals(ModelDBConstants.S3))
                            .bind("store_type_path", storeTypePath)
                            .bind("serialization", artifact.getSerialization())
                            .bind("artifact_subtype", artifact.getArtifactSubtype())
                            .bind(ENTITY_ID_QUERY_PARAM, entityId)
                            .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
                            .bind(ENTITY_NAME_QUERY_PARAM, entityName)
                            .execute();
                      }
                    }),
            executor);
  }

  public InternalFuture<Void> deleteArtifacts(String entityId, Optional<List<String>> maybeKeys) {
    var currentFuture =
        InternalFuture.runAsync(
            () -> {
              if (entityId == null || entityId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.ENTITY_ID_IS_EMPTY_ERROR);
              }
            },
            executor);
    return currentFuture.thenCompose(
        unused ->
            jdbi.useHandle(
                handle -> {
                  var sql =
                      String.format(
                          "delete from %s where entity_name=:entity_name and field_type=:field_type and %s =:entity_id",
                          getTableName(), entityIdReferenceColumn);

                  if (maybeKeys.isPresent() && !maybeKeys.get().isEmpty()) {
                    sql += " and ar_key in (<keys>)";
                  }

                  var query =
                      handle
                          .createUpdate(sql)
                          .bind(ENTITY_ID_QUERY_PARAM, entityId)
                          .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
                          .bind(ENTITY_NAME_QUERY_PARAM, entityName);

                  if (maybeKeys.isPresent() && !maybeKeys.get().isEmpty()) {
                    query = query.bindList("keys", maybeKeys.get());
                  }

                  query.execute();
                }),
        executor);
  }
}
