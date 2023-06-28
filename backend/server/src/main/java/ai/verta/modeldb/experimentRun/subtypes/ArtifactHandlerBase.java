package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.Artifact;
import ai.verta.modeldb.common.config.ArtifactStoreConfig;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.Future;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.Handle;
import ai.verta.modeldb.common.subtypes.CommonArtifactHandler;
import com.google.rpc.Code;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jdbi.v3.core.statement.Query;

public abstract class ArtifactHandlerBase extends CommonArtifactHandler<String> {

  private static final String FIELD_TYPE_QUERY_PARAM = "field_type";
  private static final String ENTITY_NAME_QUERY_PARAM = "entity_name";
  protected final String fieldType;
  protected String entityIdReferenceColumn;

  public ArtifactHandlerBase(
      FutureJdbi jdbi,
      String fieldType,
      String entityName,
      ArtifactStoreConfig artifactStoreConfig) {
    super(jdbi, artifactStoreConfig, entityName);
    this.fieldType = fieldType;

    switch (entityName) {
      case "ProjectEntity":
        this.entityIdReferenceColumn = "project_id";
        break;
      case "ExperimentEntity":
        this.entityIdReferenceColumn = "experiment_id";
        break;
      case "ExperimentRunEntity":
        this.entityIdReferenceColumn = "experiment_run_id";
        break;
      default:
        throw new InternalErrorException("Invalid entity name: " + entityName);
    }
  }

  @Override
  protected Query buildGetArtifactsQuery(
      Set<String> entityIds, Optional<String> maybeKey, Handle handle) {
    var queryStr =
        String.format(
            "select ar_key as k, ar_path as p, artifact_type as at, path_only as po, linked_artifact_id as lai, filename_extension as fe, serialization as ser, artifact_subtype as ast, upload_completed as uc, %s as entity_id from %s where entity_name=:entity_name and field_type=:field_type and %s IN (<entity_ids>)",
            entityIdReferenceColumn, getTableName(), entityIdReferenceColumn);

    if (maybeKey.isPresent()) {
      queryStr = queryStr + " AND ar_key=:ar_key ";
    }

    try (var query = handle.createQuery(queryStr)) {
      query
          .bindList("entity_ids", entityIds)
          .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
          .bind(ENTITY_NAME_QUERY_PARAM, entityName);
      maybeKey.ifPresent(s -> query.bind("ar_key", s));
      return query;
    }
  }

  protected Future<Optional<Long>> getArtifactId(String entityId, String key) {
    return Future.runAsync(
            () -> {
              if (key.isEmpty()) {
                throw new ModelDBException("Key must be provided", Code.INVALID_ARGUMENT);
              }
            })
        .thenSupply(
            () ->
                jdbi.call(
                    handle -> {
                      var queryStr =
                          String.format(
                              "select id from %s where entity_name=:entity_name and field_type=:field_type and %s =:entity_id and ar_key=:ar_key",
                              getTableName(), entityIdReferenceColumn);
                      try (var query = handle.createQuery(queryStr)) {
                        return query
                            .bind(ENTITY_ID_QUERY_PARAM, entityId)
                            .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
                            .bind(ENTITY_NAME_QUERY_PARAM, entityName)
                            .bind("ar_key", key)
                            .mapTo(Long.class)
                            .findOne();
                      }
                    }));
  }

  @Override
  public Future<Void> deleteArtifacts(String entityId, Optional<List<String>> maybeKeys) {
    return jdbi.run(handle -> deleteArtifactsWithHandle(entityId, maybeKeys, handle));
  }

  @Override
  public void deleteArtifactsWithHandle(
      String entityId, Optional<List<String>> maybeKeys, Handle handle) {
    var sql =
        String.format(
            "delete from %s where entity_name=:entity_name and field_type=:field_type and %s =:entity_id",
            getTableName(), entityIdReferenceColumn);

    if (maybeKeys.isPresent() && !maybeKeys.get().isEmpty()) {
      sql += " and ar_key in (<keys>)";
    }

    try (var query = handle.createUpdate(sql)) {
      query
          .bind(ENTITY_ID_QUERY_PARAM, entityId)
          .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
          .bind(ENTITY_NAME_QUERY_PARAM, entityName);
      maybeKeys.ifPresent(maybeKeyList -> query.bindList("keys", maybeKeyList));
      query.execute();
    }
  }

  @Override
  protected void validateAndThrowErrorAlreadyExistsArtifacts(
      String entityId, List<Artifact> artifacts, Handle handle) {
    for (final var artifact : artifacts) {
      try (var query =
          handle.createQuery(
              String.format(
                  "select id from %s where entity_name=:entity_name and field_type=:field_type and ar_key=:key and %s =:entity_id",
                  getTableName(), entityIdReferenceColumn))) {
        query
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
  }

  public List<Artifact> logArtifacts(
      Handle handle, String entityId, List<Artifact> artifacts, boolean overwrite) {
    return super.logArtifacts(handle, entityId, artifacts, overwrite, false);
  }

  @Override
  protected void insertArtifactInDB(
      String entityId,
      Handle handle,
      Artifact artifact,
      boolean uploadCompleted,
      String storeTypePath) {
    var updateQueryStr =
        "insert into "
            + getTableName()
            + " (entity_name, field_type, ar_key, ar_path, artifact_type, path_only, linked_artifact_id, filename_extension, store_type_path, serialization, artifact_subtype, upload_completed, "
            + entityIdReferenceColumn
            + ") "
            + "values (:entity_name, :field_type, :key, :path, :type,:path_only,:linked_artifact_id,:filename_extension,:store_type_path, :serialization, :artifact_subtype, :upload_completed, :entity_id)";
    try (var updateQuery = handle.createUpdate(updateQueryStr)) {
      updateQuery
          .bind("key", artifact.getKey())
          .bind("path", artifact.getPath())
          .bind("type", artifact.getArtifactTypeValue())
          .bind("path_only", artifact.getPathOnly())
          .bind("linked_artifact_id", artifact.getLinkedArtifactId())
          .bind("filename_extension", artifact.getFilenameExtension())
          .bind("upload_completed", uploadCompleted)
          .bind("store_type_path", storeTypePath)
          .bind("serialization", artifact.getSerialization())
          .bind("artifact_subtype", artifact.getArtifactSubtype())
          .bind(ENTITY_ID_QUERY_PARAM, entityId)
          .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
          .bind(ENTITY_NAME_QUERY_PARAM, entityName)
          .execute();
    }
  }

  @Override
  protected AbstractMap.SimpleEntry<String, Artifact> getSimpleEntryFromResultSet(ResultSet rs)
      throws SQLException {
    return new AbstractMap.SimpleEntry<>(
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
            .build());
  }

  @Override
  protected boolean isExists(String entityId, String key, Handle handle) {
    var queryStr =
        String.format(
            "select id from %s where entity_name=:entity_name and field_type=:field_type and ar_key=:key and %s =:entity_id",
            getTableName(), entityIdReferenceColumn);
    try (var query = handle.createQuery(queryStr)) {
      return query
              .bind("key", key)
              .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
              .bind(ENTITY_NAME_QUERY_PARAM, entityName)
              .bind(ENTITY_ID_QUERY_PARAM, entityId)
              .mapTo(Long.class)
              .one()
          > 0;
    }
  }

  @Override
  protected void updateArtifactWithHandle(
      String entityId,
      Handle handle,
      Artifact artifact,
      boolean uploadCompleted,
      String storeTypePath) {
    Map<String, Object> valueMap = new HashMap<>();
    valueMap.put("ar_key", artifact.getKey());
    valueMap.put("ar_path", artifact.getPath());
    valueMap.put("artifact_type", artifact.getArtifactTypeValue());
    valueMap.put("path_only", artifact.getPathOnly());
    valueMap.put("linked_artifact_id", artifact.getLinkedArtifactId());
    valueMap.put("filename_extension", artifact.getFilenameExtension());
    valueMap.put("store_type_path", storeTypePath);
    valueMap.put("upload_completed", uploadCompleted);
    valueMap.put("serialization", artifact.getSerialization());
    valueMap.put("artifact_subtype", artifact.getArtifactSubtype());

    StringBuilder queryStrBuilder =
        new StringBuilder(String.format("UPDATE %s SET ", getTableName()));

    AtomicInteger count = new AtomicInteger();
    valueMap.forEach(
        (key, value) -> {
          queryStrBuilder.append(key).append(" = :").append(key);
          if (count.get() < valueMap.size() - 1) {
            queryStrBuilder.append(", ");
          }
          count.getAndIncrement();
        });

    queryStrBuilder.append(
        String.format(" WHERE ar_key = :ar_key and %s =:entity_id  ", entityIdReferenceColumn));

    try (var query = handle.createUpdate(queryStrBuilder.toString())) {
      // Inserting fields arguments based on the keys and value of map
      for (Map.Entry<String, Object> objectEntry : valueMap.entrySet()) {
        query.bind(objectEntry.getKey(), objectEntry.getValue());
      }

      query.bind("entity_id", entityId).execute();
    }
  }
}
