package ai.verta.modeldb.common.subtypes;

import ai.verta.common.Artifact;
import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.config.ArtifactStoreConfig;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import com.google.rpc.Code;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Query;

public abstract class CommonArtifactHandler {
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

  public InternalFuture<List<Artifact>> getArtifacts(String entityId, Optional<String> maybeKey) {
    var currentFuture =
        InternalFuture.runAsync(
            () -> {
              if (entityId == null || entityId.isEmpty()) {
                throw new ModelDBException(
                    CommonMessages.ENTITY_ID_IS_EMPTY_ERROR, Code.INVALID_ARGUMENT);
              }
            },
            executor);
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

  protected abstract Query buildGetArtifactsQuery(
      Set<String> entityIds, Optional<String> maybeKey, Handle handle);

  public InternalFuture<MapSubtypes<Artifact>> getArtifactsMap(Set<String> entityIds) {
    return jdbi.withHandle(
            handle -> {
              Query query = buildGetArtifactsQuery(entityIds, Optional.empty(), handle);
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
                throw new ModelDBException(
                    CommonMessages.ENTITY_ID_IS_EMPTY_ERROR, Code.INVALID_ARGUMENT);
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
                  throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
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
                        deleteArtifactsWithHandle(
                            entityId,
                            Optional.of(
                                artifacts.stream()
                                    .map(Artifact::getKey)
                                    .collect(Collectors.toList())),
                            handle);
                      } else {
                        validateAndThrowErrorAlreadyExistsArtifacts(entityId, artifacts, handle);
                      }
                    }),
            executor)
        .thenAccept(unused -> validateArtifactsForTrial(entityId, artifacts), executor)
        .thenCompose(
            unused ->
                // Log
                jdbi.useHandle(
                    handle -> {
                      for (final var artifact : artifacts) {
                        var uploadCompleted =
                            !artifactStoreConfig.getArtifactStoreType().equals(CommonConstants.S3);
                        if (artifact.getUploadCompleted()) {
                          uploadCompleted = true;
                        }
                        var storeTypePath =
                            !artifact.getPathOnly()
                                ? artifactStoreConfig.storeTypePathPrefix() + artifact.getPath()
                                : "";
                        insertArtifactInDB(
                            entityId, handle, artifact, uploadCompleted, storeTypePath);
                      }
                    }),
            executor);
  }

  protected abstract void insertArtifactInDB(
      String entityId,
      Handle handle,
      Artifact artifact,
      boolean uploadCompleted,
      String storeTypePath);

  protected abstract InternalFuture<Void> validateArtifactsForTrial(
      String entityId, List<Artifact> artifacts);

  protected abstract void validateAndThrowErrorAlreadyExistsArtifacts(
      String entityId, List<Artifact> artifacts, Handle handle);

  protected abstract void deleteArtifactsWithHandle(
      String entityId, Optional<List<String>> maybeKeys, Handle handle);

  public abstract InternalFuture<Void> deleteArtifacts(
      String entityId, Optional<List<String>> maybeKeys);
}
