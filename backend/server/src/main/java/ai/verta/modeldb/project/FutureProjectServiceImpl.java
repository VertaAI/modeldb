package ai.verta.modeldb.project;

import ai.verta.common.Artifact;
import ai.verta.common.KeyValue;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.*;
import ai.verta.modeldb.ProjectServiceGrpc.ProjectServiceImplBase;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.event.FutureEventDAO;
import ai.verta.modeldb.common.futures.Future;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureGrpc;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FutureProjectServiceImpl extends ProjectServiceImplBase {
  private final FutureExecutor executor;
  private final FutureProjectDAO futureProjectDAO;
  private final FutureEventDAO futureEventDAO;

  private static final String DELETE_PROJECT_EVENT_TYPE =
      "delete.resource.project.delete_project_succeeded";
  private static final String UPDATE_PROJECT_EVENT_TYPE =
      "update.resource.project.update_project_succeeded";

  public FutureProjectServiceImpl(DAOSet daoSet, FutureExecutor executor) {
    this.executor = executor;
    this.futureProjectDAO = daoSet.getFutureProjectDAO();
    this.futureEventDAO = daoSet.getFutureEventDAO();
  }

  private Future<Void> addEvent(
      String entityId,
      long workspaceId,
      String eventType,
      Optional<String> updatedField,
      Map<String, Object> extraFieldsMap,
      String eventMessage) {

    if (!App.getInstance().mdbConfig.isEvent_system_enabled()) {
      return Future.of(null);
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
    return futureEventDAO
        .addLocalEventWithAsync(
            ModelDBServiceResourceTypes.PROJECT.name(), eventType, workspaceId, eventMetadata)
        .toFuture();
  }

  @Override
  public void createProject(
      CreateProject request, StreamObserver<CreateProject.Response> responseObserver) {
    try {
      Future<Project> projectFuture =
          futureProjectDAO
              .createProject(request)
              .thenCompose(
                  createdProject -> {
                    return addEvent(
                            createdProject.getId(),
                            createdProject.getWorkspaceServiceId(),
                            "add.resource.project.add_project_succeeded",
                            Optional.empty(),
                            Collections.emptyMap(),
                            "project logged successfully")
                        .thenCompose(eventLoggedStatus -> Future.of(createdProject));
                  });
      final var futureResponse =
          projectFuture.<CreateProject.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Project, ? extends CreateProject.Response>)
                              createdProject ->
                                  CreateProject.Response.newBuilder()
                                      .setProject(createdProject)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void updateProjectDescription(
      UpdateProjectDescription request,
      StreamObserver<UpdateProjectDescription.Response> responseObserver) {
    try {
      Future<Project> projectFuture =
          futureProjectDAO
              .updateProjectDescription(request)
              .thenCompose(
                  updatedProject -> {
                    return addEvent(
                            updatedProject.getId(),
                            updatedProject.getWorkspaceServiceId(),
                            UPDATE_PROJECT_EVENT_TYPE,
                            Optional.of("description"),
                            Collections.emptyMap(),
                            "project description updated successfully")
                        .thenCompose(eventLoggedStatus -> Future.of(updatedProject));
                  });
      final var futureResponse =
          projectFuture.<UpdateProjectDescription.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Project, ? extends UpdateProjectDescription.Response>)
                              updatedProject ->
                                  UpdateProjectDescription.Response.newBuilder()
                                      .setProject(updatedProject)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addProjectAttributes(
      AddProjectAttributes request,
      StreamObserver<AddProjectAttributes.Response> responseObserver) {
    try {
      Future<Project> projectFuture =
          futureProjectDAO
              .logAttributes(
                  LogAttributes.newBuilder()
                      .setId(request.getId())
                      .addAllAttributes(request.getAttributesList())
                      .build())
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()))
              .thenCompose(
                  updatedProject -> {
                    return addEvent(
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
                        .thenCompose(eventLoggedStatus -> Future.of(updatedProject));
                  });
      final var futureResponse =
          projectFuture.<AddProjectAttributes.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Project, ? extends AddProjectAttributes.Response>)
                              updatedProject ->
                                  AddProjectAttributes.Response.newBuilder()
                                      .setProject(updatedProject)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void updateProjectAttributes(
      UpdateProjectAttributes request,
      StreamObserver<UpdateProjectAttributes.Response> responseObserver) {
    try {
      Future<Project> projectFuture =
          futureProjectDAO
              .updateProjectAttributes(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()))
              .thenCompose(
                  updatedProject -> {
                    return addEvent(
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
                        .thenCompose(eventLoggedStatus -> Future.of(updatedProject));
                  });
      final var futureResponse =
          projectFuture.<UpdateProjectAttributes.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Project, ? extends UpdateProjectAttributes.Response>)
                              updatedProject ->
                                  UpdateProjectAttributes.Response.newBuilder()
                                      .setProject(updatedProject)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectAttributes(
      GetAttributes request, StreamObserver<GetAttributes.Response> responseObserver) {
    try {
      Future<List<KeyValue>> listFuture = futureProjectDAO.getAttributes(request);
      final var futureResponse =
          listFuture.<GetAttributes.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super List<KeyValue>, ? extends GetAttributes.Response>)
                              attributes ->
                                  GetAttributes.Response.newBuilder()
                                      .addAllAttributes(attributes)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteProjectAttributes(
      DeleteProjectAttributes request,
      StreamObserver<DeleteProjectAttributes.Response> responseObserver) {
    try {
      Future<Project> projectFuture =
          futureProjectDAO
              .deleteAttributes(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()))
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
                        .thenCompose(eventLoggedStatus -> Future.of(updatedProject));
                  });
      // Add succeeded event in local DB
      final var futureResponse =
          projectFuture.<DeleteProjectAttributes.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Project, ? extends DeleteProjectAttributes.Response>)
                              updatedProject ->
                                  DeleteProjectAttributes.Response.newBuilder()
                                      .setProject(updatedProject)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addProjectTags(
      AddProjectTags request, StreamObserver<AddProjectTags.Response> responseObserver) {
    try {
      Future<Project> projectFuture =
          futureProjectDAO
              .addTags(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()))
              .thenCompose(
                  updatedProject -> {
                    return addEvent(
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
                        .thenCompose(eventLoggedStatus -> Future.of(updatedProject));
                  });
      final var response =
          projectFuture.<AddProjectTags.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Project, ? extends AddProjectTags.Response>)
                              updatedProject ->
                                  AddProjectTags.Response.newBuilder()
                                      .setProject(updatedProject)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
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
              .thenCompose(
                  tags -> Future.of(GetTags.Response.newBuilder().addAllTags(tags).build()));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteProjectTags(
      DeleteProjectTags request, StreamObserver<DeleteProjectTags.Response> responseObserver) {
    try {
      Future<Project> projectFuture =
          futureProjectDAO
              .deleteTags(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()))
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
                        .thenCompose(eventLoggedStatus -> Future.of(updatedProject));
                  });
      // Add succeeded event in local DB
      final var response =
          projectFuture.<DeleteProjectTags.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Project, ? extends DeleteProjectTags.Response>)
                              updatedProject ->
                                  DeleteProjectTags.Response.newBuilder()
                                      .setProject(updatedProject)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void addProjectTag(
      AddProjectTag request, StreamObserver<AddProjectTag.Response> responseObserver) {
    try {
      Future<Project> projectFuture =
          futureProjectDAO
              .addTags(
                  AddProjectTags.newBuilder()
                      .setId(request.getId())
                      .addTags(request.getTag())
                      .build())
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()))
              .thenCompose(
                  updatedProject -> {
                    return addEvent(
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
                        .thenCompose(eventLoggedStatus -> Future.of(updatedProject));
                  });
      final var response =
          projectFuture.<AddProjectTag.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Project, ? extends AddProjectTag.Response>)
                              updatedProject ->
                                  AddProjectTag.Response.newBuilder()
                                      .setProject(updatedProject)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteProjectTag(
      DeleteProjectTag request, StreamObserver<DeleteProjectTag.Response> responseObserver) {
    try {
      Future<Project> projectFuture =
          futureProjectDAO
              .deleteTags(
                  DeleteProjectTags.newBuilder()
                      .setId(request.getId())
                      .addTags(request.getTag())
                      .setDeleteAll(false)
                      .build())
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()))
              .thenCompose(
                  updatedProject -> {
                    return addEvent(
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
                        .thenCompose(eventLoggedStatus -> Future.of(updatedProject));
                  });
      final var response =
          projectFuture.<DeleteProjectTag.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Project, ? extends DeleteProjectTag.Response>)
                              updatedProject ->
                                  DeleteProjectTag.Response.newBuilder()
                                      .setProject(updatedProject)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
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
                    List<Future<Void>> eventFuture = new ArrayList<>();
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
                    return Future.sequence(eventFuture);
                  })
              .thenCompose(
                  project ->
                      Future.of(DeleteProject.Response.newBuilder().setStatus(true).build()));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjects(
      GetProjects request, StreamObserver<GetProjects.Response> responseObserver) {
    try {
      Future<FindProjects.Response> responseFuture =
          futureProjectDAO.findProjects(
              FindProjects.newBuilder()
                  .setWorkspaceName(request.getWorkspaceName())
                  .setAscending(request.getAscending())
                  .setSortKey(request.getSortKey())
                  .setPageNumber(request.getPageNumber())
                  .setPageLimit(request.getPageLimit())
                  .build());
      final var response =
          responseFuture.<GetProjects.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super FindProjects.Response, ? extends GetProjects.Response>)
                              findProjectResponse ->
                                  GetProjects.Response.newBuilder()
                                      .addAllProjects(findProjectResponse.getProjectsList())
                                      .setTotalRecords(findProjectResponse.getTotalRecords())
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectById(
      GetProjectById request, StreamObserver<GetProjectById.Response> responseObserver) {
    try {
      Future<Project> projectFuture = futureProjectDAO.getProjectById(request.getId());
      final var response =
          projectFuture.<GetProjectById.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Project, ? extends GetProjectById.Response>)
                              project ->
                                  GetProjectById.Response.newBuilder().setProject(project).build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectByName(
      GetProjectByName request, StreamObserver<GetProjectByName.Response> responseObserver) {
    try {
      final var futureResponse = futureProjectDAO.getProjectByName(request);
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void verifyConnection(
      Empty request, StreamObserver<VerifyConnectionResponse> responseObserver) {
    try {
      final var response = futureProjectDAO.verifyConnection(request);
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
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
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void setProjectReadme(
      SetProjectReadme request, StreamObserver<SetProjectReadme.Response> responseObserver) {
    try {
      Future<Project> projectFuture =
          futureProjectDAO
              .setProjectReadme(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()))
              .thenCompose(
                  updatedProject ->
                  // Add succeeded event in local DB
                  {
                    return addEvent(
                            updatedProject.getId(),
                            updatedProject.getWorkspaceServiceId(),
                            UPDATE_PROJECT_EVENT_TYPE,
                            Optional.of("readme_text"),
                            Collections.singletonMap("readme_text", updatedProject.getReadmeText()),
                            "project readme_text updated successfully")
                        .thenCompose(unused -> Future.of(updatedProject));
                  });
      // Add succeeded event in local DB
      final var response =
          projectFuture.<SetProjectReadme.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Project, ? extends SetProjectReadme.Response>)
                              updatedProject ->
                                  SetProjectReadme.Response.newBuilder()
                                      .setProject(updatedProject)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectReadme(
      GetProjectReadme request, StreamObserver<GetProjectReadme.Response> responseObserver) {
    try {
      final var response = futureProjectDAO.getProjectReadme(request);
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void setProjectShortName(
      SetProjectShortName request, StreamObserver<SetProjectShortName.Response> responseObserver) {
    try {
      Future<Project> projectFuture =
          futureProjectDAO
              .setProjectShortName(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()))
              .thenCompose(
                  updatedProject ->
                  // Add succeeded event in local DB
                  {
                    return addEvent(
                            updatedProject.getId(),
                            updatedProject.getWorkspaceServiceId(),
                            UPDATE_PROJECT_EVENT_TYPE,
                            Optional.of("short_name"),
                            Collections.singletonMap("short_name", updatedProject.getShortName()),
                            "project short_name updated successfully")
                        .thenCompose(unused -> Future.of(updatedProject));
                  });
      // Add succeeded event in local DB
      final var response =
          projectFuture.<SetProjectShortName.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Project, ? extends SetProjectShortName.Response>)
                              updatedProject ->
                                  SetProjectShortName.Response.newBuilder()
                                      .setProject(updatedProject)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectShortName(
      GetProjectShortName request, StreamObserver<GetProjectShortName.Response> responseObserver) {
    try {
      final var response = futureProjectDAO.getProjectShortName(request);
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logProjectCodeVersion(
      LogProjectCodeVersion request,
      StreamObserver<LogProjectCodeVersion.Response> responseObserver) {
    try {
      Future<Project> projectFuture =
          futureProjectDAO
              .logProjectCodeVersion(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()))
              .thenCompose(
                  updatedProject ->
                  // Add succeeded event in local DB
                  {
                    return addEvent(
                            updatedProject.getId(),
                            updatedProject.getWorkspaceServiceId(),
                            UPDATE_PROJECT_EVENT_TYPE,
                            Optional.of("code_version"),
                            Collections.emptyMap(),
                            "code_version logged successfully")
                        .thenCompose(unused -> Future.of(updatedProject));
                  });
      // Add succeeded event in local DB
      final var response =
          projectFuture.<LogProjectCodeVersion.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Project, ? extends LogProjectCodeVersion.Response>)
                              updatedProject ->
                                  LogProjectCodeVersion.Response.newBuilder()
                                      .setProject(updatedProject)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
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
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void findProjects(
      FindProjects request, StreamObserver<FindProjects.Response> responseObserver) {
    try {
      final var futureResponse = futureProjectDAO.findProjects(request);
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getUrlForArtifact(
      GetUrlForArtifact request, StreamObserver<GetUrlForArtifact.Response> responseObserver) {
    try {
      final var futureResponse = futureProjectDAO.getUrlForArtifact(request);
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void logArtifacts(
      LogProjectArtifacts request, StreamObserver<LogProjectArtifacts.Response> responseObserver) {
    try {
      Future<Project> projectFuture =
          futureProjectDAO
              .logArtifacts(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()))
              .thenCompose(
                  updatedProject ->
                  // Add succeeded event in local DB
                  {
                    return addEvent(
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
                        .thenCompose(unused -> Future.of(updatedProject));
                  });
      // Add succeeded event in local DB
      final var futureResponse =
          projectFuture.<LogProjectArtifacts.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Project, ? extends LogProjectArtifacts.Response>)
                              updatedProject ->
                                  LogProjectArtifacts.Response.newBuilder()
                                      .setProject(updatedProject)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getArtifacts(
      GetArtifacts request, StreamObserver<GetArtifacts.Response> responseObserver) {
    try {
      Future<List<Artifact>> listFuture = futureProjectDAO.getArtifacts(request);
      final var futureResponse =
          listFuture.<GetArtifacts.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super List<Artifact>, ? extends GetArtifacts.Response>)
                              artifacts ->
                                  GetArtifacts.Response.newBuilder()
                                      .addAllArtifacts(artifacts)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteArtifact(
      DeleteProjectArtifact request,
      StreamObserver<DeleteProjectArtifact.Response> responseObserver) {
    try {
      Future<ai.verta.modeldb.Project> projectFuture =
          futureProjectDAO
              .deleteArtifacts(request)
              .thenCompose(unused -> futureProjectDAO.getProjectById(request.getId()))
              .thenCompose(
                  updatedProject ->
                  // Add succeeded event in local DB
                  {
                    return addEvent(
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
                        .thenCompose(unused -> Future.of(updatedProject));
                  });
      // Add succeeded event in local DB
      final var futureResponse =
          projectFuture.<DeleteProjectArtifact.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Project, ? extends DeleteProjectArtifact.Response>)
                              updatedProject ->
                                  DeleteProjectArtifact.Response.newBuilder()
                                      .setProject(updatedProject)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, futureResponse);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void deleteProjects(
      DeleteProjects request, StreamObserver<DeleteProjects.Response> responseObserver) {
    try {
      Future<List<Void>> listFuture =
          futureProjectDAO
              .deleteProjects(request.getIdsList())
              .thenCompose(
                  allowedProjectResources -> {
                    List<Future<Void>> eventFuture = new ArrayList<>();
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
                    return Future.sequence(eventFuture);
                  });
      // Add succeeded event in local DB
      final var response =
          listFuture.<DeleteProjects.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super List<Void>, ? extends DeleteProjects.Response>)
                              project ->
                                  DeleteProjects.Response.newBuilder().setStatus(true).build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }

  @Override
  public void getProjectDatasetCount(
      GetProjectDatasetCount request,
      StreamObserver<GetProjectDatasetCount.Response> responseObserver) {
    try {
      Future<Long> longFuture = futureProjectDAO.getProjectDatasetCount(request.getProjectId());
      final var response =
          longFuture.<GetProjectDatasetCount.Response>thenCompose(
              t ->
                  Future.of(
                      ((Function<? super Long, ? extends GetProjectDatasetCount.Response>)
                              datasetCount ->
                                  GetProjectDatasetCount.Response.newBuilder()
                                      .setDatasetCount(datasetCount)
                                      .build())
                          .apply(t)));
      FutureGrpc.serverResponse(responseObserver, response);
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e);
    }
  }
}
