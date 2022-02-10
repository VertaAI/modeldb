package ai.verta.modeldb.experiment;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.AddAttributes;
import ai.verta.modeldb.AddExperimentAttributes;
import ai.verta.modeldb.AddExperimentTag;
import ai.verta.modeldb.AddExperimentTags;
import ai.verta.modeldb.App;
import ai.verta.modeldb.CreateExperiment;
import ai.verta.modeldb.DAOSet;
import ai.verta.modeldb.DeleteExperiment;
import ai.verta.modeldb.DeleteExperimentArtifact;
import ai.verta.modeldb.DeleteExperimentAttributes;
import ai.verta.modeldb.DeleteExperimentAttributes.Response;
import ai.verta.modeldb.DeleteExperimentTag;
import ai.verta.modeldb.DeleteExperimentTags;
import ai.verta.modeldb.DeleteExperiments;
import ai.verta.modeldb.FindExperiments;
import ai.verta.modeldb.GetArtifacts;
import ai.verta.modeldb.GetAttributes;
import ai.verta.modeldb.GetExperimentById;
import ai.verta.modeldb.GetExperimentByName;
import ai.verta.modeldb.GetExperimentCodeVersion;
import ai.verta.modeldb.GetExperimentsInProject;
import ai.verta.modeldb.GetTags;
import ai.verta.modeldb.GetUrlForArtifact;
import ai.verta.modeldb.LogExperimentArtifacts;
import ai.verta.modeldb.LogExperimentCodeVersion;
import ai.verta.modeldb.ServiceSet;
import ai.verta.modeldb.UpdateExperimentDescription;
import ai.verta.modeldb.UpdateExperimentName;
import ai.verta.modeldb.UpdateExperimentNameOrDescription;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.event.FutureEventDAO;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.project.FutureProjectDAO;
import ai.verta.uac.GetResourcesResponseItem;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.grpc.stub.StreamObserver;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FutureExperimentServiceImpl extends ExperimentServiceImpl {

  private static final Logger LOGGER = LogManager.getLogger(FutureExperimentServiceImpl.class);
  private static final String UPDATE_EVENT_TYPE =
      "update.resource.experiment.update_experiment_succeeded";
  private static final String DELETE_EXPERIMENT_EVENT_TYPE =
      "delete.resource.experiment.delete_experiment_succeeded";
  private final Executor executor;
  private final FutureProjectDAO futureProjectDAO;
  private final FutureExperimentDAO futureExperimentDAO;
  private final ArtifactStoreDAO artifactStoreDAO;
  private final FutureEventDAO futureEventDAO;

  public FutureExperimentServiceImpl(ServiceSet serviceSet, DAOSet daoSet, Executor executor) {
    super(serviceSet, daoSet);
    this.executor = executor;
    this.futureProjectDAO = daoSet.futureProjectDAO;
    this.futureExperimentDAO = daoSet.futureExperimentDAO;
    this.artifactStoreDAO = daoSet.artifactStoreDAO;
    this.futureEventDAO = daoSet.futureEventDAO;
  }

  private InternalFuture<Void> addEvent(
      String entityId,
      String projectId,
      String eventType,
      Optional<String> updatedField,
      Map<String, Object> extraFieldsMap,
      String eventMessage) {

    if (!App.getInstance().mdbConfig.isEvent_system_enabled()) {
      return InternalFuture.completedInternalFuture(null);
    }

    // Add succeeded event in local DB
    JsonObject eventMetadata = new JsonObject();
    eventMetadata.addProperty("entity_id", entityId);
    eventMetadata.addProperty("project_id", projectId);
    if (updatedField.isPresent() && !updatedField.get().isEmpty()) {
      eventMetadata.addProperty("updated_field", updatedField.get());
    }
    if (extraFieldsMap != null && !extraFieldsMap.isEmpty()) {
      JsonObject updatedFieldValue = new JsonObject();
      extraFieldsMap.forEach(
          (key, value) -> {
            if (value instanceof JsonElement) {
              updatedFieldValue.add(key, (JsonElement) value);
            } else {
              updatedFieldValue.addProperty(key, String.valueOf(value));
            }
          });
      eventMetadata.add("updated_field_value", updatedFieldValue);
    }
    eventMetadata.addProperty("message", eventMessage);

    GetResourcesResponseItem projectResource =
        mdbRoleService.getEntityResource(
            Optional.of(projectId),
            Optional.empty(),
            ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT);

    return futureEventDAO.addLocalEventWithAsync(
        ModelDBServiceResourceTypes.EXPERIMENT.name(),
        eventType,
        projectResource.getWorkspaceId(),
        eventMetadata);
  }

  @Override
  public void createExperiment(
      CreateExperiment request, StreamObserver<CreateExperiment.Response> responseObserver) {
    try {
      final var futureResponse =
          futureExperimentDAO
              .createExperiment(request)
              .thenCompose(
                  createdExperiment ->
                      addEvent(
                              createdExperiment.getId(),
                              createdExperiment.getProjectId(),
                              "add.resource.experiment.add_experiment_succeeded",
                              Optional.empty(),
                              Collections.emptyMap(),
                              "experiment logged successfully")
                          .thenApply(eventLoggedStatus -> createdExperiment, executor),
                  executor)
              .thenApply(
                  createdExperiment ->
                      CreateExperiment.Response.newBuilder()
                          .setExperiment(createdExperiment)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getExperimentsInProject(
      GetExperimentsInProject request,
      StreamObserver<GetExperimentsInProject.Response> responseObserver) {
    super.getExperimentsInProject(request, responseObserver);
  }

  @Override
  public void getExperimentById(
      GetExperimentById request, StreamObserver<GetExperimentById.Response> responseObserver) {
    super.getExperimentById(request, responseObserver);
  }

  @Override
  public void getExperimentByName(
      GetExperimentByName request, StreamObserver<GetExperimentByName.Response> responseObserver) {
    super.getExperimentByName(request, responseObserver);
  }

  @Override
  public void updateExperimentNameOrDescription(
      UpdateExperimentNameOrDescription request,
      StreamObserver<UpdateExperimentNameOrDescription.Response> responseObserver) {
    super.updateExperimentNameOrDescription(request, responseObserver);
  }

  @Override
  public void updateExperimentName(
      UpdateExperimentName request,
      StreamObserver<UpdateExperimentName.Response> responseObserver) {
    super.updateExperimentName(request, responseObserver);
  }

  @Override
  public void updateExperimentDescription(
      UpdateExperimentDescription request,
      StreamObserver<UpdateExperimentDescription.Response> responseObserver) {
    super.updateExperimentDescription(request, responseObserver);
  }

  @Override
  public void addExperimentTags(
      AddExperimentTags request, StreamObserver<AddExperimentTags.Response> responseObserver) {
    super.addExperimentTags(request, responseObserver);
  }

  @Override
  public void addExperimentTag(
      AddExperimentTag request, StreamObserver<AddExperimentTag.Response> responseObserver) {
    super.addExperimentTag(request, responseObserver);
  }

  @Override
  public void getExperimentTags(
      GetTags request, StreamObserver<GetTags.Response> responseObserver) {
    super.getExperimentTags(request, responseObserver);
  }

  @Override
  public void deleteExperimentTags(
      DeleteExperimentTags request,
      StreamObserver<DeleteExperimentTags.Response> responseObserver) {
    super.deleteExperimentTags(request, responseObserver);
  }

  @Override
  public void deleteExperimentTag(
      DeleteExperimentTag request, StreamObserver<DeleteExperimentTag.Response> responseObserver) {
    super.deleteExperimentTag(request, responseObserver);
  }

  @Override
  public void addAttribute(
      AddAttributes request, StreamObserver<AddAttributes.Response> responseObserver) {
    super.addAttribute(request, responseObserver);
  }

  @Override
  public void addExperimentAttributes(
      AddExperimentAttributes request,
      StreamObserver<AddExperimentAttributes.Response> responseObserver) {
    super.addExperimentAttributes(request, responseObserver);
  }

  @Override
  public void getExperimentAttributes(
      GetAttributes request, StreamObserver<GetAttributes.Response> responseObserver) {
    super.getExperimentAttributes(request, responseObserver);
  }

  @Override
  public void deleteExperimentAttributes(
      DeleteExperimentAttributes request, StreamObserver<Response> responseObserver) {
    super.deleteExperimentAttributes(request, responseObserver);
  }

  @Override
  public void deleteExperiment(
      DeleteExperiment request, StreamObserver<DeleteExperiment.Response> responseObserver) {
    super.deleteExperiment(request, responseObserver);
  }

  @Override
  public void logExperimentCodeVersion(
      LogExperimentCodeVersion request,
      StreamObserver<LogExperimentCodeVersion.Response> responseObserver) {
    super.logExperimentCodeVersion(request, responseObserver);
  }

  @Override
  public void getExperimentCodeVersion(
      GetExperimentCodeVersion request,
      StreamObserver<GetExperimentCodeVersion.Response> responseObserver) {
    super.getExperimentCodeVersion(request, responseObserver);
  }

  @Override
  public void findExperiments(
      FindExperiments request, StreamObserver<FindExperiments.Response> responseObserver) {
    try {
      final var futureResponse = futureExperimentDAO.findExperiments(request);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getUrlForArtifact(
      GetUrlForArtifact request, StreamObserver<GetUrlForArtifact.Response> responseObserver) {
    super.getUrlForArtifact(request, responseObserver);
  }

  @Override
  public void logArtifacts(
      LogExperimentArtifacts request,
      StreamObserver<LogExperimentArtifacts.Response> responseObserver) {
    super.logArtifacts(request, responseObserver);
  }

  @Override
  public void getArtifacts(
      GetArtifacts request, StreamObserver<GetArtifacts.Response> responseObserver) {
    super.getArtifacts(request, responseObserver);
  }

  @Override
  public void deleteArtifact(
      DeleteExperimentArtifact request,
      StreamObserver<DeleteExperimentArtifact.Response> responseObserver) {
    super.deleteArtifact(request, responseObserver);
  }

  @Override
  public void deleteExperiments(
      DeleteExperiments request, StreamObserver<DeleteExperiments.Response> responseObserver) {
    super.deleteExperiments(request, responseObserver);
  }
}
