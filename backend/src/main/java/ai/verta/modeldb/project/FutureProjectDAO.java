package ai.verta.modeldb.project;

import ai.verta.common.KeyValue;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.AddExperimentRunTags;
import ai.verta.modeldb.DeleteExperimentRunTags;
import ai.verta.modeldb.DeleteProjectAttributes;
import ai.verta.modeldb.GetAttributes;
import ai.verta.modeldb.GetTags;
import ai.verta.modeldb.LogAttributes;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.UpdateProjectAttributes;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.exceptions.PermissionDeniedException;
import ai.verta.modeldb.experimentRun.subtypes.AttributeHandler;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FutureProjectDAO {
  private static Logger LOGGER = LogManager.getLogger(FutureProjectDAO.class);

  private final Executor executor;
  private final FutureJdbi jdbi;
  private final UAC uac;

  private final AttributeHandler attributeHandler;
  private final TagsHandler tagsHandler;

  public FutureProjectDAO(Executor executor, FutureJdbi jdbi, UAC uac) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.uac = uac;

    String entityName = "ProjectEntity";
    attributeHandler = new AttributeHandler(executor, jdbi, entityName);
    tagsHandler = new TagsHandler(executor, jdbi, entityName);
  }

  public InternalFuture<Void> deleteAttributes(DeleteProjectAttributes request) {
    final var projectId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    final Optional<List<String>> maybeKeys =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getAttributeKeysList());

    return checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> attributeHandler.deleteKeyValues(projectId, maybeKeys), executor)
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now), executor);
  }

  public InternalFuture<List<KeyValue>> getAttributes(GetAttributes request) {
    final var projectId = request.getId();

    return checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(unused -> attributeHandler.getKeyValues(projectId), executor);
  }

  public InternalFuture<Void> logAttributes(LogAttributes request) {
    final var projectId = request.getId();
    final var attributes = request.getAttributesList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> attributeHandler.logKeyValues(projectId, attributes), executor)
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now), executor);
  }

  public InternalFuture<Void> updateProjectAttributes(UpdateProjectAttributes request) {
    final var projectId = request.getId();
    final var attribute = request.getAttribute();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> attributeHandler.updateKeyValue(projectId, attribute), executor)
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now), executor);
  }

  public InternalFuture<Void> addTags(AddExperimentRunTags request) {
    final var projectId = request.getId();
    final var tags = request.getTagsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    return checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> tagsHandler.addTags(projectId, tags), executor)
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now), executor);
  }

  public InternalFuture<Void> deleteTags(DeleteExperimentRunTags request) {
    final var projectId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    final Optional<List<String>> maybeTags =
        request.getDeleteAll() ? Optional.empty() : Optional.of(request.getTagsList());

    return checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.UPDATE)
        .thenCompose(unused -> tagsHandler.deleteTags(projectId, maybeTags), executor)
        .thenCompose(unused -> updateModifiedTimestamp(projectId, now), executor);
  }

  public InternalFuture<List<String>> getTags(GetTags request) {
    final var projectId = request.getId();

    return checkProjectPermission(projectId, ModelDBActionEnum.ModelDBServiceActions.READ)
        .thenCompose(unused -> tagsHandler.getTags(projectId), executor);
  }

  private InternalFuture<Void> updateModifiedTimestamp(String projectId, Long now) {
    return jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    "update project set date_updated=greatest(date_updated, :now) where id=:project_id")
                .bind("project_id", projectId)
                .bind("now", now)
                .execute());
  }

  private InternalFuture<Void> checkProjectPermission(
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

  public InternalFuture<Project> getPreAccessProjectById(String projectId) {
    // TODO: Fix this later and return actual project details
    return InternalFuture.completedInternalFuture(Project.newBuilder().build());
  }
}
