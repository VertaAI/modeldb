package ai.verta.modeldb.common.subtypes;

import ai.verta.common.Artifact;
import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.config.ArtifactStoreConfig;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import com.google.rpc.Code;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Query;

public abstract class CommonArtifactHandler<T> {
  protected static final String ENTITY_ID_QUERY_PARAM = "entity_id";
  protected final Executor executor;
  protected final FutureJdbi jdbi;
  private final ArtifactStoreConfig artifactStoreConfig;

  protected String getTableName() {
    return "artifact";
  }

  public CommonArtifactHandler(
      Executor executor, FutureJdbi jdbi, ArtifactStoreConfig artifactStoreConfig) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.artifactStoreConfig = artifactStoreConfig;
  }

  public InternalFuture<List<Artifact>> getArtifacts(T entityId, Optional<String> maybeKey) {
    var currentFuture = InternalFuture.runAsync(() -> validateField(entityId), executor);
    return currentFuture.thenCompose(
        unused ->
            jdbi.withHandle(
                handle -> {
                  Query query =
                      buildGetArtifactsQuery(Collections.singleton(entityId), maybeKey, handle);
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

  private void validateField(T entityId) {
    if (entityId == null
        || ((entityId instanceof String) && ((String) entityId).isEmpty())
        || ((entityId instanceof Number) && ((Long) entityId == 0))) {
      throw new ModelDBException(CommonMessages.ENTITY_ID_IS_EMPTY_ERROR, Code.INVALID_ARGUMENT);
    }
  }

  protected abstract Query buildGetArtifactsQuery(
      Set<T> entityIds, Optional<String> maybeKey, Handle handle);

  public InternalFuture<MapSubtypes<T, Artifact>> getArtifactsMap(Set<T> entityIds) {
    return jdbi.withHandle(
            handle -> {
              Query query = buildGetArtifactsQuery(entityIds, Optional.empty(), handle);
              return query.map((rs, ctx) -> getSimpleEntryFromResultSet(rs)).list();
            })
        .thenApply(MapSubtypes::from, executor);
  }

  protected abstract AbstractMap.SimpleEntry<T, Artifact> getSimpleEntryFromResultSet(ResultSet rs)
      throws SQLException;

  public void logArtifacts(Handle handle, T entityId, List<Artifact> artifacts, boolean overwrite) {
    // Validate input
    validateField(entityId);

    for (final var artifact : artifacts) {
      String errorMessage = null;
      if (artifact.getKey().isEmpty() && (artifact.getPathOnly() && artifact.getPath().isEmpty())) {
        errorMessage = "Artifact key and Artifact path not found in request";
      } else if (artifact.getKey().isEmpty()) {
        errorMessage = "Artifact key not found in request";
      } else if (artifact.getPathOnly() && artifact.getPath().isEmpty()) {
        errorMessage = "Artifact path not found in request";
      }

      if (errorMessage != null) {
        throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
      }
    }

    if (overwrite) {
      deleteArtifactsWithHandle(
          entityId,
          Optional.of(artifacts.stream().map(Artifact::getKey).collect(Collectors.toList())),
          handle);
    } else {
      validateAndThrowErrorAlreadyExistsArtifacts(entityId, artifacts, handle);
    }

    for (final var artifact : artifacts) {
      var uploadCompleted = !artifactStoreConfig.getArtifactStoreType().equals(CommonConstants.S3);
      if (artifact.getUploadCompleted()) {
        uploadCompleted = true;
      }
      var storeTypePath =
          !artifact.getPathOnly()
              ? artifactStoreConfig.storeTypePathPrefix() + artifact.getPath()
              : "";
      insertArtifactInDB(entityId, handle, artifact, uploadCompleted, storeTypePath);
    }
  }

  protected abstract void insertArtifactInDB(
      T entityId, Handle handle, Artifact artifact, boolean uploadCompleted, String storeTypePath);

  protected abstract void validateAndThrowErrorAlreadyExistsArtifacts(
      T entityId, List<Artifact> artifacts, Handle handle);

  protected abstract void deleteArtifactsWithHandle(
      T entityId, Optional<List<String>> maybeKeys, Handle handle);

  public abstract InternalFuture<Void> deleteArtifacts(
      T entityId, Optional<List<String>> maybeKeys);
}
