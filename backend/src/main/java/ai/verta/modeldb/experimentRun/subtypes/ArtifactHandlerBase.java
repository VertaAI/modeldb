package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.Artifact;
import ai.verta.modeldb.App;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class ArtifactHandlerBase {
  protected final Executor executor;
  protected final FutureJdbi jdbi;
  protected final String fieldType;
  protected final String entityName;
  protected final String entityIdReferenceColumn;

  protected String getTableName() {
    return "artifact";
  }

  public ArtifactHandlerBase(
      Executor executor, FutureJdbi jdbi, String fieldType, String entityName) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.fieldType = fieldType;
    this.entityName = entityName;

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
                                "select id from "
                                    + getTableName()
                                    + " where entity_name=:entity_name and field_type=:field_type and "
                                    + entityIdReferenceColumn
                                    + "=:entity_id and ar_key=:ar_key")
                            .bind("entity_id", entityId)
                            .bind("field_type", fieldType)
                            .bind("entity_name", entityName)
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
                throw new InvalidArgumentException("Entity id is empty");
              }
            },
            executor);
    return currentFuture.thenCompose(
        unused ->
            jdbi.withHandle(
                handle -> {
                  var queryStr =
                      "select ar_key as k, ar_path as p, artifact_type as at, path_only as po, linked_artifact_id as lai, filename_extension as fe from "
                          + getTableName()
                          + " where entity_name=:entity_name and field_type=:field_type and "
                          + entityIdReferenceColumn
                          + "=:entity_id";

                  if (maybeKey.isPresent()) {
                    queryStr = queryStr + " AND ar_key=:ar_key ";
                  }

                  var query =
                      handle
                          .createQuery(queryStr)
                          .bind("entity_id", entityId)
                          .bind("field_type", fieldType)
                          .bind("entity_name", entityName);
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
                                  .build())
                      .list();
                }),
        executor);
  }

  public InternalFuture<MapSubtypes<Artifact>> getArtifactsMap(Set<String> entityIds) {
    return jdbi.withHandle(
            handle -> {
              var queryStr =
                  "select ar_key as k, ar_path as p, artifact_type as at, path_only as po, linked_artifact_id as lai, filename_extension as fe, "
                      + entityIdReferenceColumn
                      + " as entity_id from "
                      + getTableName()
                      + " where entity_name=:entity_name and field_type=:field_type and "
                      + entityIdReferenceColumn
                      + " in (<entity_ids>)";

              var query =
                  handle
                      .createQuery(queryStr)
                      .bindList("entity_ids", entityIds)
                      .bind("field_type", fieldType)
                      .bind("entity_name", entityName);

              return query
                  .map(
                      (rs, ctx) ->
                          new AbstractMap.SimpleEntry<>(
                              rs.getString("entity_id"),
                              Artifact.newBuilder()
                                  .setKey(rs.getString("k"))
                                  .setPath(rs.getString("p"))
                                  .setArtifactTypeValue(rs.getInt("at"))
                                  .setPathOnly(rs.getBoolean("po"))
                                  .setLinkedArtifactId(rs.getString("lai"))
                                  .setFilenameExtension(rs.getString("fe"))
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
                throw new InvalidArgumentException("Entity id is empty");
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
                                "delete from "
                                    + getTableName()
                                    + " where entity_name=:entity_name and field_type=:field_type and ar_key in (<keys>) and "
                                    + entityIdReferenceColumn
                                    + "=:entity_id")
                            .bindList(
                                "keys",
                                artifacts.stream()
                                    .map(Artifact::getKey)
                                    .collect(Collectors.toList()))
                            .bind("field_type", fieldType)
                            .bind("entity_name", entityName)
                            .bind("entity_id", entityId)
                            .execute();
                      } else {
                        for (final var artifact : artifacts) {
                          handle
                              .createQuery(
                                  "select id from "
                                      + getTableName()
                                      + " where entity_name=:entity_name and field_type=:field_type and ar_key=:key and "
                                      + entityIdReferenceColumn
                                      + "=:entity_id")
                              .bind("key", artifact.getKey())
                              .bind("field_type", fieldType)
                              .bind("entity_name", entityName)
                              .bind("entity_id", entityId)
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
        .thenCompose(
            unused ->
                // Log
                jdbi.useHandle(
                    handle -> {
                      for (final var artifact : artifacts) {
                        var storeTypePath =
                            !artifact.getPathOnly()
                                ? App.getInstance().config.artifactStoreConfig.storeTypePathPrefix()
                                    + artifact.getPath()
                                : "";
                        handle
                            .createUpdate(
                                "insert into "
                                    + getTableName()
                                    + " (entity_name, field_type, ar_key, ar_path, artifact_type, path_only, linked_artifact_id, filename_extension, store_type_path,"
                                    + entityIdReferenceColumn
                                    + ") "
                                    + "values (:entity_name, :field_type, :key, :path, :type,:path_only,:linked_artifact_id,:filename_extension,:store_type_path, :entity_id)")
                            .bind("key", artifact.getKey())
                            .bind("path", artifact.getPath())
                            .bind("type", artifact.getArtifactTypeValue())
                            .bind("path_only", artifact.getPathOnly())
                            .bind("linked_artifact_id", artifact.getLinkedArtifactId())
                            .bind("filename_extension", artifact.getFilenameExtension())
                            .bind("store_type_path", storeTypePath)
                            .bind("entity_id", entityId)
                            .bind("field_type", fieldType)
                            .bind("entity_name", entityName)
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
                throw new InvalidArgumentException("Entity id is empty");
              }
            },
            executor);
    return currentFuture.thenCompose(
        unused ->
            jdbi.useHandle(
                handle -> {
                  var sql =
                      "delete from "
                          + getTableName()
                          + " where entity_name=:entity_name and field_type=:field_type and "
                          + entityIdReferenceColumn
                          + "=:entity_id";

                  if (maybeKeys.isPresent()) {
                    sql += " and ar_key in (<keys>)";
                  }

                  var query =
                      handle
                          .createUpdate(sql)
                          .bind("entity_id", entityId)
                          .bind("field_type", fieldType)
                          .bind("entity_name", entityName);

                  if (maybeKeys.isPresent()) {
                    query = query.bindList("keys", maybeKeys.get());
                  }

                  query.execute();
                }),
        executor);
  }
}
