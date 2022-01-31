package ai.verta.modeldb.project;

import ai.verta.common.KeyValue;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.AddProjectTags;
import ai.verta.modeldb.DeleteProjectAttributes;
import ai.verta.modeldb.DeleteProjectTags;
import ai.verta.modeldb.Empty;
import ai.verta.modeldb.GetAttributes;
import ai.verta.modeldb.GetTags;
import ai.verta.modeldb.GetUrlForArtifact;
import ai.verta.modeldb.LogAttributes;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.UpdateProjectAttributes;
import ai.verta.modeldb.VerifyConnectionResponse;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAO;
import ai.verta.modeldb.exceptions.PermissionDeniedException;
import ai.verta.modeldb.experimentRun.subtypes.ArtifactHandler;
import ai.verta.modeldb.experimentRun.subtypes.AttributeHandler;
import ai.verta.modeldb.experimentRun.subtypes.CodeVersionHandler;
import ai.verta.modeldb.experimentRun.subtypes.DatasetHandler;
import ai.verta.modeldb.experimentRun.subtypes.TagsHandler;
import ai.verta.uac.Action;
import ai.verta.uac.IsSelfAllowed;
import ai.verta.uac.ModelDBActionEnum;
import ai.verta.uac.ResourceType;
import ai.verta.uac.Resources;
import ai.verta.uac.ServiceEnum;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

public class FutureProjectDAO {

  private final Executor executor;
  private final FutureJdbi jdbi;
  private final UAC uac;
  private final boolean isMssql;

  private final AttributeHandler attributeHandler;
  private final TagsHandler tagsHandler;
  private final ArtifactHandler artifactHandler;

  public FutureProjectDAO(
      Executor executor,
      FutureJdbi jdbi,
      UAC uac,
      ArtifactStoreDAO artifactStoreDAO,
      DatasetVersionDAO datasetVersionDAO,
      MDBConfig mdbConfig) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.uac = uac;
    this.isMssql = mdbConfig.getDatabase().getRdbConfiguration().isMssql();

    var entityName = "ProjectEntity";
    attributeHandler = new AttributeHandler(executor, jdbi, entityName);
    tagsHandler = new TagsHandler(executor, jdbi, entityName);
    CodeVersionHandler codeVersionHandler = new CodeVersionHandler(executor, jdbi, "project");
    DatasetHandler datasetHandler = new DatasetHandler(executor, jdbi, entityName, mdbConfig);
    artifactHandler =
        new ArtifactHandler(
            executor,
            jdbi,
            entityName,
            codeVersionHandler,
            datasetHandler,
            artifactStoreDAO,
            datasetVersionDAO,
            mdbConfig);
  }

  public InternalFuture<Void> deleteAttributes(DeleteProjectAttributes request) {
    final var projectId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    var validateArgumentFuture =
        InternalFuture.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              }
            },
            executor);

    final Optional<List<String>> maybeKeys =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getAttributeKeysList());

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(unused -> attributeHandler.deleteKeyValues(projectId, maybeKeys), executor)
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now), executor)
        .thenCompose(unused -> updateVersionNumber(projectId), executor);
  }

  public InternalFuture<List<KeyValue>> getAttributes(GetAttributes request) {
    final var projectId = request.getId();
    final var keys = request.getAttributeKeysList();
    final var getAll = request.getGetAll();

    var validateArgumentFuture =
        InternalFuture.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              } else if (keys.isEmpty() && !getAll) {
                throw new InvalidArgumentException("Attribute keys not present");
              }
            },
            executor);

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.READ),
            executor)
        .thenCompose(unused -> attributeHandler.getKeyValues(projectId, keys, getAll), executor);
  }

  public InternalFuture<Void> logAttributes(LogAttributes request) {
    final var projectId = request.getId();
    final var attributes = request.getAttributesList();
    final var now = Calendar.getInstance().getTimeInMillis();

    var validateArgumentFuture =
        InternalFuture.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              } else if (attributes.isEmpty()) {
                throw new InvalidArgumentException("Attributes not present");
              }
            },
            executor);

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(unused -> attributeHandler.logKeyValues(projectId, attributes), executor)
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now), executor)
        .thenCompose(unused -> updateVersionNumber(projectId), executor);
  }

  public InternalFuture<Void> updateProjectAttributes(UpdateProjectAttributes request) {
    final var projectId = request.getId();
    final var attribute = request.getAttribute();
    final var now = Calendar.getInstance().getTimeInMillis();

    var validateArgumentFuture =
        InternalFuture.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              } else if (attribute.getKey().isEmpty()) {
                throw new InvalidArgumentException("Attribute not present");
              }
            },
            executor);

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(unused -> attributeHandler.updateKeyValue(projectId, attribute), executor)
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now), executor)
        .thenCompose(unused -> updateVersionNumber(projectId), executor);
  }

  public InternalFuture<Void> addTags(AddProjectTags request) {
    final var projectId = request.getId();
    final var tags = request.getTagsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    var validateArgumentFuture =
        InternalFuture.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              } else if (tags.isEmpty()) {
                throw new InvalidArgumentException("Tags not present");
              }
            },
            executor);

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(unused -> tagsHandler.addTags(projectId, tags), executor)
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now), executor)
        .thenCompose(unused -> updateVersionNumber(projectId), executor);
  }

  public InternalFuture<Void> deleteTags(DeleteProjectTags request) {
    final var projectId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    var validateArgumentFuture =
        InternalFuture.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              }
            },
            executor);

    final Optional<List<String>> maybeTags =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getTagsList());

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE),
            executor)
        .thenCompose(unused -> tagsHandler.deleteTags(projectId, maybeTags), executor)
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now), executor)
        .thenCompose(unused -> updateVersionNumber(projectId), executor);
  }

  public InternalFuture<List<String>> getTags(GetTags request) {
    final var projectId = request.getId();
    var validateArgumentFuture =
        InternalFuture.runAsync(
            () -> {
              if (projectId.isEmpty()) {
                throw new InvalidArgumentException(ModelDBMessages.PROJECT_ID_NOT_PRESENT_ERROR);
              }
            },
            executor);

    return validateArgumentFuture
        .thenCompose(
            unused ->
                checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.READ),
            executor)
        .thenCompose(unused -> tagsHandler.getTags(projectId), executor);
  }

  private InternalFuture<Void> updateModifiedTimestamp(String projectId, Long now) {
    String greatestValueStr;
    if (isMssql) {
      greatestValueStr =
          "(SELECT MAX(value) FROM (VALUES (date_updated),(:now)) AS maxvalues(value))";
    } else {
      greatestValueStr = "greatest(date_updated, :now)";
    }

    return jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    String.format(
                        "update project set date_updated=%s where id=:project_id",
                        greatestValueStr))
                .bind("project_id", projectId)
                .bind("now", now)
                .execute());
  }

  private InternalFuture<Void> updateVersionNumber(String projectId) {
    return jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    "update project set version_number=(version_number + 1) where id=:project_id")
                .bind("project_id", projectId)
                .execute());
  }

  public InternalFuture<Void> checkProjectPermission(
      String projId, ModelDBActionEnum.ModelDBServiceActions action) {
    return FutureGrpc.ClientRequest(
            uac.getAuthzService()
                .isSelfAllowed(
                    IsSelfAllowed.newBuilder()
                        .addActions(
                            Action.newBuilder()
                                .setModeldbServiceAction(action)
                                .setService(ServiceEnum.Service.MODELDB_SERVICE))
                        .addResources(
                            Resources.newBuilder()
                                .setService(ServiceEnum.Service.MODELDB_SERVICE)
                                .setResourceType(
                                    ResourceType.newBuilder()
                                        .setModeldbServiceResourceType(
                                            ModelDBResourceEnum.ModelDBServiceResourceTypes
                                                .PROJECT))
                                .addResourceIds(projId))
                        .build()),
            executor)
        .thenAccept(
            response -> {
              if (!response.getAllowed()) {
                throw new PermissionDeniedException("Permission denied");
              }
            },
            executor);
  }

  public InternalFuture<Long> getProjectDatasetCount(String projectId) {
    return checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(
            unused ->
                jdbi.withHandle(
                    handle -> {
                      var queryStr =
                          "SELECT COUNT(distinct ar.linked_artifact_id) from artifact ar inner join experiment_run er ON er.id = ar.experiment_run_id "
                              + " WHERE er.project_id = :projectId AND ar.experiment_run_id is not null AND ar.linked_artifact_id <> '' ";
                      return handle
                          .createQuery(queryStr)
                          .bind("projectId", projectId)
                          .mapTo(Long.class)
                          .one();
                    }),
            executor);
  }

  public InternalFuture<GetUrlForArtifact.Response> getUrlForArtifact(GetUrlForArtifact request) {
    final var projectId = request.getId();

    InternalFuture<Void> permissionCheck;
    if (request.getMethod().equalsIgnoreCase("get")) {
      permissionCheck =
          checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.READ);
    } else {
      permissionCheck =
          checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE);
    }

    return permissionCheck.thenCompose(
        unused -> artifactHandler.getUrlForArtifact(request), executor);
  }

  public InternalFuture<VerifyConnectionResponse> verifyConnection(Empty request) {
    return InternalFuture.completedInternalFuture(
        VerifyConnectionResponse.newBuilder().setStatus(true).build());
  }
}
