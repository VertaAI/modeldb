package ai.verta.modeldb.common.subtypes;

import ai.verta.common.Artifact;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.config.ArtifactStoreConfig;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.Handle;
import ai.verta.modeldb.common.futures.InternalFuture;
import com.google.rpc.Code;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jdbi.v3.core.statement.Query;

public abstract class CommonArtifactHandler<T> {
  protected static final String ENTITY_ID_QUERY_PARAM = "entity_id";
  protected final FutureExecutor executor;
  protected final FutureJdbi jdbi;
  private final ArtifactStoreConfig artifactStoreConfig;
  protected final String entityName;

  protected String getTableName() {
    return "artifact";
  }

  public CommonArtifactHandler(
      FutureExecutor executor,
      FutureJdbi jdbi,
      ArtifactStoreConfig artifactStoreConfig,
      String entityName) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.artifactStoreConfig = artifactStoreConfig;
    this.entityName = entityName;
  }

  public InternalFuture<List<Artifact>> getArtifacts(T entityId, Optional<String> maybeKey) {
    var currentFuture = InternalFuture.runAsync(() -> validateField(entityId), executor);
    return currentFuture
        .thenCompose(
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
            executor)
        .thenApply(
            artifacts ->
                artifacts.stream()
                    .sorted(Comparator.comparing(Artifact::getKey))
                    .collect(Collectors.toList()),
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
        .thenApply(
            simpleEntries ->
                simpleEntries.stream()
                    .sorted(Comparator.comparing(entry -> entry.getValue().getKey()))
                    .collect(Collectors.toList()),
            executor)
        .thenApply(MapSubtypes::from, executor);
  }

  protected abstract AbstractMap.SimpleEntry<T, Artifact> getSimpleEntryFromResultSet(ResultSet rs)
      throws SQLException;

  public List<Artifact> logArtifacts(
      Handle handle,
      T entityId,
      List<Artifact> artifacts,
      boolean overwrite,
      boolean isPopulateFromOtherEntity) {
    // Validate input
    validateField(entityId);

    for (final var artifact : artifacts) {
      if (artifact.getKey().isEmpty()) {
        var errorMessage = "Artifact key not found in request";
        throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
      }
    }

    if (!overwrite) {
      validateAndThrowErrorAlreadyExistsArtifacts(entityId, artifacts, handle);
    }

    List<Artifact> pathUpdatedArtifacts = new ArrayList<>();
    for (var artifact : artifacts) {
      var uploadCompleted =
          !artifactStoreConfig.getArtifactStoreType().equals(ArtifactStoreConfig.S3_TYPE_STORE);
      if (artifact.getUploadCompleted()) {
        uploadCompleted = true;
      }

      var path = artifact.getPath();
      if (!isPopulateFromOtherEntity) {
        var validPrefix = artifactStoreConfig.getPathPrefixWithSeparator() + this.entityName;
        path = validPrefix + "/" + entityId + "/" + artifact.getKey();

        var filenameExtension = artifact.getFilenameExtension();
        if (!filenameExtension.isEmpty() && !path.endsWith("." + filenameExtension)) {
          path += "." + filenameExtension;
        }

        artifact = artifact.toBuilder().setPath(path).build();
      }
      var storeTypePath = !artifact.getPathOnly() ? path : "";

      if (overwrite && isExists(entityId, artifact.getKey(), handle)) {
        updateArtifactWithHandle(entityId, handle, artifact, uploadCompleted, storeTypePath);
      } else {
        insertArtifactInDB(entityId, handle, artifact, uploadCompleted, storeTypePath);
      }
      pathUpdatedArtifacts.add(artifact);
    }
    return pathUpdatedArtifacts.stream()
        .sorted(Comparator.comparing(Artifact::getKey))
        .collect(Collectors.toList());
  }

  protected abstract void insertArtifactInDB(
      T entityId, Handle handle, Artifact artifact, boolean uploadCompleted, String storeTypePath);

  protected abstract void validateAndThrowErrorAlreadyExistsArtifacts(
      T entityId, List<Artifact> artifacts, Handle handle);

  protected abstract boolean isExists(T entityId, String key, Handle handle);

  protected abstract void deleteArtifactsWithHandle(
      T entityId, Optional<List<String>> maybeKeys, Handle handle);

  public abstract InternalFuture<Void> deleteArtifacts(
      T entityId, Optional<List<String>> maybeKeys);

  protected abstract void updateArtifactWithHandle(
      T entityId, Handle handle, Artifact artifact, boolean uploadCompleted, String storeTypePath);
}
