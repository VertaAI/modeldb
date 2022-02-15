package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.Artifact;
import ai.verta.modeldb.common.config.ArtifactStoreConfig;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.subtypes.CommonArtifactHandler;
import com.google.rpc.Code;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Query;

public abstract class ArtifactHandlerBase extends CommonArtifactHandler<String> {

  private static final String FIELD_TYPE_QUERY_PARAM = "field_type";
  private static final String ENTITY_NAME_QUERY_PARAM = "entity_name";
  protected final String fieldType;
  protected String entityIdReferenceColumn;

  public ArtifactHandlerBase(
      Executor executor,
      FutureJdbi jdbi,
      String fieldType,
      String entityName,
      ArtifactStoreConfig artifactStoreConfig) {
    super(executor, jdbi, artifactStoreConfig, entityName);
    this.fieldType = fieldType;

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

    var query =
        handle
            .createQuery(queryStr)
            .bindList("entity_ids", entityIds)
            .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
            .bind(ENTITY_NAME_QUERY_PARAM, entityName);
    maybeKey.ifPresent(s -> query.bind("ar_key", s));
    return query;
  }

  protected InternalFuture<Optional<Long>> getArtifactId(String entityId, String key) {
    return InternalFuture.runAsync(
            () -> {
              if (key.isEmpty()) {
                throw new ModelDBException("Key must be provided", Code.INVALID_ARGUMENT);
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

  @Override
  public InternalFuture<Void> deleteArtifacts(String entityId, Optional<List<String>> maybeKeys) {
    return jdbi.useHandle(handle -> deleteArtifactsWithHandle(entityId, maybeKeys, handle));
  }

  @Override
  protected void deleteArtifactsWithHandle(
      String entityId, Optional<List<String>> maybeKeys, Handle handle) {
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
  }

  @Override
  protected void validateAndThrowErrorAlreadyExistsArtifacts(
      String entityId, List<Artifact> artifacts, Handle handle) {
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
                throw new AlreadyExistsException("Key '" + artifact.getKey() + "' already exists");
              });
    }
  }

  @Override
  protected void insertArtifactInDB(
      String entityId,
      Handle handle,
      Artifact artifact,
      boolean uploadCompleted,
      String storeTypePath) {
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
        .bind("upload_completed", uploadCompleted)
        .bind("store_type_path", storeTypePath)
        .bind("serialization", artifact.getSerialization())
        .bind("artifact_subtype", artifact.getArtifactSubtype())
        .bind(ENTITY_ID_QUERY_PARAM, entityId)
        .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
        .bind(ENTITY_NAME_QUERY_PARAM, entityName)
        .execute();
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
}
