package ai.verta.modeldb.project;

import ai.verta.common.Artifact;
import ai.verta.common.KeyValue;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.AddProjectAttributes;
import ai.verta.modeldb.AddProjectTag;
import ai.verta.modeldb.AddProjectTags;
import ai.verta.modeldb.App;
import ai.verta.modeldb.CreateProject;
import ai.verta.modeldb.DAOSet;
import ai.verta.modeldb.DeepCopyProject;
import ai.verta.modeldb.DeleteProject;
import ai.verta.modeldb.DeleteProjectArtifact;
import ai.verta.modeldb.DeleteProjectAttributes;
import ai.verta.modeldb.DeleteProjectTag;
import ai.verta.modeldb.DeleteProjectTags;
import ai.verta.modeldb.DeleteProjects;
import ai.verta.modeldb.Empty;
import ai.verta.modeldb.FindProjects;
import ai.verta.modeldb.GetArtifacts;
import ai.verta.modeldb.GetAttributes;
import ai.verta.modeldb.GetProjectById;
import ai.verta.modeldb.GetProjectByName;
import ai.verta.modeldb.GetProjectCodeVersion;
import ai.verta.modeldb.GetProjectDatasetCount;
import ai.verta.modeldb.GetProjectReadme;
import ai.verta.modeldb.GetProjectShortName;
import ai.verta.modeldb.GetProjects;
import ai.verta.modeldb.GetSummary;
import ai.verta.modeldb.GetTags;
import ai.verta.modeldb.GetUrlForArtifact;
import ai.verta.modeldb.LogAttributes;
import ai.verta.modeldb.LogProjectArtifacts;
import ai.verta.modeldb.LogProjectCodeVersion;
import ai.verta.modeldb.ProjectServiceGrpc.ProjectServiceImplBase;
import ai.verta.modeldb.SetProjectReadme;
import ai.verta.modeldb.SetProjectShortName;
import ai.verta.modeldb.UpdateProjectAttributes;
import ai.verta.modeldb.UpdateProjectDescription;
import ai.verta.modeldb.VerifyConnectionResponse;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.event.FutureEventDAO;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.InternalFuture;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FutureProjectServiceImpl extends ProjectServiceImplBase {
  private final Executor executor;
  private final FutureProjectDAO futureProjectDAO;
  private final FutureEventDAO futureEventDAO;

  private static final String DELETE_PROJECT_EVENT_TYPE =
      "delete.resource.project.delete_project_succeeded";
  private static final String UPDATE_PROJECT_EVENT_TYPE =
      "update.resource.project.update_project_succeeded";

  public FutureProjectServiceImpl(DAOSet daoSet, Executor executor) {
    this.executor = executor;
    this.futureProjectDAO = daoSet.futureProjectDAO;
    this.futureEventDAO = daoSet.futureEventDAO;
  }

  private InternalFuture<Void> addEvent(
      String entityId,
      long workspaceId,
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
    return futureEventDAO.addLocalEventWithAsync(
        ModelDBServiceResourceTypes.PROJECT.name(), eventType, workspaceId, eventMetadata);
  }

  @Override
  public void createProject(
      CreateProject request, StreamObserver<CreateProject.Response> responseObserver) {
    // super.createProject(request, responseObserver);
    try {
      final var futureResponse =
          futureProjectDAO
              .createProject(request)
              .thenCompose(
                  createdProject ->
                      addEvent(
                              createdProject.getId(),
                              createdProject.getWorkspaceServiceId(),
                              "add.resource.project.add_project_succeeded",
                              Optional.empty(),
                              Collections.emptyMap(),
                              "project logged successfully")
                          .thenApply(eventLoggedStatus -> createdProject, executor),
                  executor)
              .thenApply(
                  createdProject ->
                      CreateProject.Response.newBuilder().setProject(createdProject).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void updateProjectDescription(
      UpdateProjectDescription request,
      StreamObserver<UpdateProjectDescription.Response> responseObserver) {
    try {
      final var futureResponse =
          futureProjectDAO
              .updateProjectDescription(request)
              .thenCompose(
                  updatedProject ->
                      addEvent(
                              updatedProject.getId(),
                              updatedProject.getWorkspaceServiceId(),
                              UPDATE_PROJECT_EVENT_TYPE,
                              Optional.of("description"),
                              Collections.emptyMap(),
                              "project description updated successfully")
                          .thenApply(eventLoggedStatus -> updatedProject, executor),
                  executor)
              .thenApply(
                  updatedProject ->
                      UpdateProjectDescription.Response.newBuilder()
                          .setProject(updatedProject)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addProjectAttributes(
      AddProjectAttributes request,
      StreamObserver<AddProjectAttributes.Response> responseObserver) {
    try {
      final var futureResponse =
          futureProjectDAO
              .logAttributes(
                  LogAttributes.newBuilder()
                      .setId(request.getId())
                      .addAllAttributes(request.getAttributesList())
                      .build())
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenCompose(
                  updatedProject ->
                      addEvent(
                              updatedProject.getId(),
                              updatedProject.getWorkspaceServiceId(),
                              UPDATE_PROJECT_EVENT_TYPE,
                              Optional.of("attributes"),
                              Collections.singletonMap(
                                  "attribute_keys",
                                  new Gson()
                                      .toJsonTree(
                                          request.getAttributesList().stream()
                                              .map(KeyValue::getKey)
                                              .collect(Collectors.toSet()),
                                          new TypeToken<ArrayList<String>>() {}.getType())),
                              "project attributes added successfully")
                          .thenApply(eventLoggedStatus -> updatedProject, executor),
                  executor)
              .thenApply(
                  updatedProject ->
                      AddProjectAttributes.Response.newBuilder().setProject(updatedProject).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void updateProjectAttributes(
      UpdateProjectAttributes request,
      StreamObserver<UpdateProjectAttributes.Response> responseObserver) {
    try {
      final var futureResponse =
          futureProjectDAO
              .updateProjectAttributes(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenCompose(
                  updatedProject ->
                      addEvent(
                              updatedProject.getId(),
                              updatedProject.getWorkspaceServiceId(),
                              UPDATE_PROJECT_EVENT_TYPE,
                              Optional.of("attributes"),
                              Collections.singletonMap(
                                  "attribute_keys",
                                  new Gson()
                                      .toJsonTree(
                                          Stream.of(request.getAttribute())
                                              .map(KeyValue::getKey)
                                              .collect(Collectors.toSet()),
                                          new TypeToken<ArrayList<String>>() {}.getType())),
                              "project attributes updated successfully")
                          .thenApply(eventLoggedStatus -> updatedProject, executor),
                  executor)
              .thenApply(
                  updatedProject ->
                      UpdateProjectAttributes.Response.newBuilder()
                          .setProject(updatedProject)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectAttributes(
      GetAttributes request, StreamObserver<GetAttributes.Response> responseObserver) {
    try {
      final var futureResponse =
          futureProjectDAO
              .getAttributes(request)
              .thenApply(
                  attributes ->
                      GetAttributes.Response.newBuilder().addAllAttributes(attributes).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteProjectAttributes(
      DeleteProjectAttributes request,
      StreamObserver<DeleteProjectAttributes.Response> responseObserver) {
    try {
      final var futureResponse =
          futureProjectDAO
              .deleteAttributes(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenCompose(
                  updatedProject -> {
                    // Add succeeded event in local DB
                    Map<String, Object> extraFieldValue = new HashMap<>();
                    if (request.getDeleteAll()) {
                      extraFieldValue.put("attributes_deleted_all", true);
                    } else {
                      extraFieldValue.put(
                          "attribute_keys",
                          new Gson()
                              .toJsonTree(
                                  request.getAttributeKeysList(),
                                  new TypeToken<ArrayList<String>>() {}.getType()));
                    }
                    return addEvent(
                            updatedProject.getId(),
                            updatedProject.getWorkspaceServiceId(),
                            UPDATE_PROJECT_EVENT_TYPE,
                            Optional.of("attributes"),
                            extraFieldValue,
                            "project attributes deleted successfully")
                        .thenApply(eventLoggedStatus -> updatedProject, executor);
                  },
                  executor)
              .thenApply(
                  updatedProject ->
                      DeleteProjectAttributes.Response.newBuilder()
                          .setProject(updatedProject)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addProjectTags(
      AddProjectTags request, StreamObserver<AddProjectTags.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .addTags(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenCompose(
                  updatedProject ->
                      addEvent(
                              updatedProject.getId(),
                              updatedProject.getWorkspaceServiceId(),
                              UPDATE_PROJECT_EVENT_TYPE,
                              Optional.of("tags"),
                              Collections.singletonMap(
                                  "tags",
                                  new Gson()
                                      .toJsonTree(
                                          request.getTagsList(),
                                          new TypeToken<ArrayList<String>>() {}.getType())),
                              "project tags updated successfully")
                          .thenApply(eventLoggedStatus -> updatedProject, executor),
                  executor)
              .thenApply(
                  updatedProject ->
                      AddProjectTags.Response.newBuilder().setProject(updatedProject).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectTags(GetTags request, StreamObserver<GetTags.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .getTags(request)
              .thenApply(tags -> GetTags.Response.newBuilder().addAllTags(tags).build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteProjectTags(
      DeleteProjectTags request, StreamObserver<DeleteProjectTags.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .deleteTags(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenCompose(
                  updatedProject -> {
                    // Add succeeded event in local DB
                    Map<String, Object> extraFieldValue = new HashMap<>();
                    if (request.getDeleteAll()) {
                      extraFieldValue.put("tags_deleted_all", true);
                    } else {
                      extraFieldValue.put(
                          "tags",
                          new Gson()
                              .toJsonTree(
                                  request.getTagsList(),
                                  new TypeToken<ArrayList<String>>() {}.getType()));
                    }
                    return addEvent(
                            updatedProject.getId(),
                            updatedProject.getWorkspaceServiceId(),
                            UPDATE_PROJECT_EVENT_TYPE,
                            Optional.of("tags"),
                            extraFieldValue,
                            "project tags deleted successfully")
                        .thenApply(eventLoggedStatus -> updatedProject, executor);
                  },
                  executor)
              .thenApply(
                  updatedProject ->
                      DeleteProjectTags.Response.newBuilder().setProject(updatedProject).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addProjectTag(
      AddProjectTag request, StreamObserver<AddProjectTag.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .addTags(
                  AddProjectTags.newBuilder()
                      .setId(request.getId())
                      .addTags(request.getTag())
                      .build())
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenCompose(
                  updatedProject ->
                      addEvent(
                              updatedProject.getId(),
                              updatedProject.getWorkspaceServiceId(),
                              UPDATE_PROJECT_EVENT_TYPE,
                              Optional.of("tags"),
                              Collections.singletonMap(
                                  "tags",
                                  new Gson()
                                      .toJsonTree(
                                          Collections.singletonList(request.getTag()),
                                          new TypeToken<ArrayList<String>>() {}.getType())),
                              "project tag added successfully")
                          .thenApply(eventLoggedStatus -> updatedProject, executor),
                  executor)
              .thenApply(
                  updatedProject ->
                      AddProjectTag.Response.newBuilder().setProject(updatedProject).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteProjectTag(
      DeleteProjectTag request, StreamObserver<DeleteProjectTag.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .deleteTags(
                  DeleteProjectTags.newBuilder()
                      .setId(request.getId())
                      .addTags(request.getTag())
                      .setDeleteAll(false)
                      .build())
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenCompose(
                  updatedProject ->
                      addEvent(
                              updatedProject.getId(),
                              updatedProject.getWorkspaceServiceId(),
                              UPDATE_PROJECT_EVENT_TYPE,
                              Optional.of("tags"),
                              Collections.singletonMap(
                                  "tags",
                                  new Gson()
                                      .toJsonTree(
                                          Collections.singletonList(request.getTag()),
                                          new TypeToken<ArrayList<String>>() {}.getType())),
                              "project tag deleted successfully")
                          .thenApply(eventLoggedStatus -> updatedProject, executor),
                  executor)
              .thenApply(
                  updatedProject ->
                      DeleteProjectTag.Response.newBuilder().setProject(updatedProject).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteProject(
      DeleteProject request, StreamObserver<DeleteProject.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .deleteProjects(Collections.singletonList(request.getId()))
              .thenCompose(
                  allowedProjectResources -> {
                    List<InternalFuture<Void>> eventFuture = new ArrayList<>();
                    // Add succeeded event in local DB
                    for (var projectResource : allowedProjectResources) {
                      eventFuture.add(
                          addEvent(
                              projectResource.getResourceId(),
                              projectResource.getWorkspaceId(),
                              DELETE_PROJECT_EVENT_TYPE,
                              Optional.empty(),
                              Collections.emptyMap(),
                              "project deleted successfully"));
                    }
                    return InternalFuture.sequence(eventFuture, executor);
                  },
                  executor)
              .thenApply(
                  project -> DeleteProject.Response.newBuilder().setStatus(true).build(), executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjects(
      GetProjects request, StreamObserver<GetProjects.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .findProjects(
                  FindProjects.newBuilder()
                      .setWorkspaceName(request.getWorkspaceName())
                      .setAscending(request.getAscending())
                      .setSortKey(request.getSortKey())
                      .setPageNumber(request.getPageNumber())
                      .setPageLimit(request.getPageLimit())
                      .build())
              .thenApply(
                  findProjectResponse ->
                      GetProjects.Response.newBuilder()
                          .addAllProjects(findProjectResponse.getProjectsList())
                          .setTotalRecords(findProjectResponse.getTotalRecords())
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectById(
      GetProjectById request, StreamObserver<GetProjectById.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .getProjectById(request.getId())
              .thenApply(
                  project -> GetProjectById.Response.newBuilder().setProject(project).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectByName(
      GetProjectByName request, StreamObserver<GetProjectByName.Response> responseObserver) {
    try {
      final var futureResponse = futureProjectDAO.getProjectByName(request);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void verifyConnection(
      Empty request, StreamObserver<VerifyConnectionResponse> responseObserver) {
    super.verifyConnection(request, responseObserver);
  }

  @Override
  public void deepCopyProject(
      DeepCopyProject request, StreamObserver<DeepCopyProject.Response> responseObserver) {
    super.deepCopyProject(request, responseObserver);
  }

  @Override
  public void getSummary(GetSummary request, StreamObserver<GetSummary.Response> responseObserver) {
    try {
      final var response = futureProjectDAO.getSummary(request);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void setProjectReadme(
      SetProjectReadme request, StreamObserver<SetProjectReadme.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .setProjectReadme(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenCompose(
                  updatedProject ->
                      // Add succeeded event in local DB
                      addEvent(
                              updatedProject.getId(),
                              updatedProject.getWorkspaceServiceId(),
                              UPDATE_PROJECT_EVENT_TYPE,
                              Optional.of("readme_text"),
                              Collections.singletonMap(
                                  "readme_text", updatedProject.getReadmeText()),
                              "project readme_text updated successfully")
                          .thenApply(unused -> updatedProject, executor),
                  executor)
              .thenApply(
                  updatedProject ->
                      SetProjectReadme.Response.newBuilder().setProject(updatedProject).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectReadme(
      GetProjectReadme request, StreamObserver<GetProjectReadme.Response> responseObserver) {
    try {
      final var response = futureProjectDAO.getProjectReadme(request);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void setProjectShortName(
      SetProjectShortName request, StreamObserver<SetProjectShortName.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .setProjectShortName(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenCompose(
                  updatedProject ->
                      // Add succeeded event in local DB
                      addEvent(
                              updatedProject.getId(),
                              updatedProject.getWorkspaceServiceId(),
                              UPDATE_PROJECT_EVENT_TYPE,
                              Optional.of("short_name"),
                              Collections.singletonMap("short_name", updatedProject.getShortName()),
                              "project short_name updated successfully")
                          .thenApply(unused -> updatedProject, executor),
                  executor)
              .thenApply(
                  updatedProject ->
                      SetProjectShortName.Response.newBuilder().setProject(updatedProject).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectShortName(
      GetProjectShortName request, StreamObserver<GetProjectShortName.Response> responseObserver) {
    try {
      final var response = futureProjectDAO.getProjectShortName(request);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logProjectCodeVersion(
      LogProjectCodeVersion request,
      StreamObserver<LogProjectCodeVersion.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .logProjectCodeVersion(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenCompose(
                  updatedProject ->
                      // Add succeeded event in local DB
                      addEvent(
                              updatedProject.getId(),
                              updatedProject.getWorkspaceServiceId(),
                              UPDATE_PROJECT_EVENT_TYPE,
                              Optional.of("code_version"),
                              Collections.emptyMap(),
                              "code_version logged successfully")
                          .thenApply(unused -> updatedProject, executor),
                  executor)
              .thenApply(
                  updatedProject ->
                      LogProjectCodeVersion.Response.newBuilder()
                          .setProject(updatedProject)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectCodeVersion(
      GetProjectCodeVersion request,
      StreamObserver<GetProjectCodeVersion.Response> responseObserver) {
    try {
      final var response = futureProjectDAO.getProjectCodeVersion(request);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void findProjects(
      FindProjects request, StreamObserver<FindProjects.Response> responseObserver) {
    try {
      final var futureResponse = futureProjectDAO.findProjects(request);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getUrlForArtifact(
      GetUrlForArtifact request, StreamObserver<GetUrlForArtifact.Response> responseObserver) {
    try {
      final var futureResponse = futureProjectDAO.getUrlForArtifact(request);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logArtifacts(
      LogProjectArtifacts request, StreamObserver<LogProjectArtifacts.Response> responseObserver) {
    super.logArtifacts(request, responseObserver);
    try {
      final var futureResponse =
          futureProjectDAO
              .logArtifacts(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenCompose(
                  updatedProject ->
                      // Add succeeded event in local DB
                      addEvent(
                              updatedProject.getId(),
                              updatedProject.getWorkspaceServiceId(),
                              UPDATE_PROJECT_EVENT_TYPE,
                              Optional.of("artifacts"),
                              Collections.singletonMap(
                                  "artifact_keys",
                                  new Gson()
                                      .toJsonTree(
                                          request.getArtifactsList().stream()
                                              .map(Artifact::getKey)
                                              .collect(Collectors.toSet()),
                                          new TypeToken<ArrayList<String>>() {}.getType())),
                              "project artifacts added successfully")
                          .thenApply(unused -> updatedProject, executor),
                  executor)
              .thenApply(
                  updatedProject ->
                      LogProjectArtifacts.Response.newBuilder().setProject(updatedProject).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getArtifacts(
      GetArtifacts request, StreamObserver<GetArtifacts.Response> responseObserver) {
    try {
      final var futureResponse =
          futureProjectDAO
              .getArtifacts(request)
              .thenApply(
                  artifacts ->
                      GetArtifacts.Response.newBuilder().addAllArtifacts(artifacts).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteArtifact(
      DeleteProjectArtifact request,
      StreamObserver<DeleteProjectArtifact.Response> responseObserver) {
    try {
      final var futureResponse =
          futureProjectDAO
              .deleteArtifacts(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()), executor)
              .thenCompose(
                  updatedProject ->
                      // Add succeeded event in local DB
                      addEvent(
                              updatedProject.getId(),
                              updatedProject.getWorkspaceServiceId(),
                              UPDATE_PROJECT_EVENT_TYPE,
                              Optional.of("artifacts"),
                              Collections.singletonMap(
                                  "artifact_keys",
                                  new Gson()
                                      .toJsonTree(
                                          Collections.singletonList(request.getKey()),
                                          new TypeToken<ArrayList<String>>() {}.getType())),
                              "project artifact deleted successfully")
                          .thenApply(unused -> updatedProject, executor),
                  executor)
              .thenApply(
                  updatedProject ->
                      DeleteProjectArtifact.Response.newBuilder()
                          .setProject(updatedProject)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, futureResponse, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteProjects(
      DeleteProjects request, StreamObserver<DeleteProjects.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .deleteProjects(request.getIdsList())
              .thenCompose(
                  allowedProjectResources -> {
                    List<InternalFuture<Void>> eventFuture = new ArrayList<>();
                    // Add succeeded event in local DB
                    for (var projectResource : allowedProjectResources) {
                      eventFuture.add(
                          addEvent(
                              projectResource.getResourceId(),
                              projectResource.getWorkspaceId(),
                              DELETE_PROJECT_EVENT_TYPE,
                              Optional.empty(),
                              Collections.emptyMap(),
                              "project deleted successfully"));
                    }
                    return InternalFuture.sequence(eventFuture, executor);
                  },
                  executor)
              .thenApply(
                  project -> DeleteProjects.Response.newBuilder().setStatus(true).build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectDatasetCount(
      GetProjectDatasetCount request,
      StreamObserver<GetProjectDatasetCount.Response> responseObserver) {
    try {
      final var response =
          futureProjectDAO
              .getProjectDatasetCount(request.getProjectId())
              .thenApply(
                  datasetCount ->
                      GetProjectDatasetCount.Response.newBuilder()
                          .setDatasetCount(datasetCount)
                          .build(),
                  executor);
      FutureGrpc.ServerResponse(responseObserver, response, executor);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }
}
