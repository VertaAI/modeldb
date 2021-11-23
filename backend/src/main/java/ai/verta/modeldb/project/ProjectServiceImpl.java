package ai.verta.modeldb.project;

import ai.verta.common.Artifact;
import ai.verta.common.ArtifactTypeEnum.ArtifactType;
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
import ai.verta.modeldb.ExperimentRun;
import ai.verta.modeldb.FindProjects;
import ai.verta.modeldb.GetArtifacts;
import ai.verta.modeldb.GetAttributes;
import ai.verta.modeldb.GetProjectById;
import ai.verta.modeldb.GetProjectByName;
import ai.verta.modeldb.GetProjectCodeVersion;
import ai.verta.modeldb.GetProjectReadme;
import ai.verta.modeldb.GetProjectShortName;
import ai.verta.modeldb.GetProjects;
import ai.verta.modeldb.GetSummary;
import ai.verta.modeldb.GetTags;
import ai.verta.modeldb.GetUrlForArtifact;
import ai.verta.modeldb.LastModifiedExperimentRunSummary;
import ai.verta.modeldb.LogProjectArtifacts;
import ai.verta.modeldb.LogProjectCodeVersion;
import ai.verta.modeldb.LogProjectCodeVersion.Response;
import ai.verta.modeldb.MetricsSummary;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.ProjectServiceGrpc.ProjectServiceImplBase;
import ai.verta.modeldb.ServiceSet;
import ai.verta.modeldb.SetProjectReadme;
import ai.verta.modeldb.SetProjectShortName;
import ai.verta.modeldb.UpdateProjectAttributes;
import ai.verta.modeldb.UpdateProjectDescription;
import ai.verta.modeldb.VerifyConnectionResponse;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.event.FutureEventDAO;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ResourceVisibility;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProjectServiceImpl extends ProjectServiceImplBase {

  public static final Logger LOGGER = LogManager.getLogger(ProjectServiceImpl.class);
  protected static final String DELETE_PROJECT_EVENT_TYPE =
      "delete.resource.project.delete_project_succeeded";
  protected static final String UPDATE_PROJECT_EVENT_TYPE =
      "update.resource.project.update_project_succeeded";
  private final AuthService authService;
  private final MDBRoleService mdbRoleService;
  private final ProjectDAO projectDAO;
  private final ExperimentRunDAO experimentRunDAO;
  private final ArtifactStoreDAO artifactStoreDAO;
  private final FutureEventDAO futureEventDAO;

  public ProjectServiceImpl(ServiceSet serviceSet, DAOSet daoSet) {
    this.authService = serviceSet.authService;
    this.mdbRoleService = serviceSet.mdbRoleService;
    this.projectDAO = daoSet.projectDAO;
    this.experimentRunDAO = daoSet.experimentRunDAO;
    this.artifactStoreDAO = daoSet.artifactStoreDAO;
    this.futureEventDAO = daoSet.futureEventDAO;
  }

  private void addEvent(
      String entityId,
      Optional<Long> workspaceId,
      String eventType,
      Optional<String> updatedField,
      Map<String, Object> extraFieldsMap,
      String eventMessage) {

    if (!App.getInstance().mdbConfig.isEvent_system_enabled()) {
      return;
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

    if (workspaceId.isEmpty()) {
      GetResourcesResponseItem projectResource =
          mdbRoleService.getEntityResource(
              Optional.of(entityId), Optional.empty(), ModelDBServiceResourceTypes.PROJECT);
      workspaceId = Optional.of(projectResource.getWorkspaceId());
    }

    futureEventDAO.addLocalEventWithBlocking(
        ModelDBServiceResourceTypes.PROJECT.name(), eventType, workspaceId.get(), eventMetadata);
  }

  /**
   * Convert CreateProject request to Project entity and insert in database.
   *
   * @param CreateProject request, CreateProject.Response response
   * @return void
   */
  @Override
  public void createProject(
      CreateProject request, StreamObserver<CreateProject.Response> responseObserver) {
    try {
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, null, ModelDBServiceActions.CREATE);

      // Get the user info from the Context
      var userInfo = authService.getCurrentLoginUserInfo();
      var project = projectDAO.insertProject(request, userInfo);

      var response = CreateProject.Response.newBuilder().setProject(project).build();

      // Add succeeded event in local DB
      addEvent(
          project.getId(),
          Optional.of(project.getWorkspaceServiceId()),
          "add.resource.project.add_project_succeeded",
          Optional.empty(),
          Collections.emptyMap(),
          "project logged successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, CreateProject.Response.getDefaultInstance());
    }
  }

  /**
   * Update project Description in Project Entity. Create project object with updated data from
   * UpdateProjectDescription request and update in database.
   *
   * @param UpdateProjectDescription request, UpdateProjectDescription.Response response
   * @return void
   */
  @Override
  public void updateProjectDescription(
      UpdateProjectDescription request,
      StreamObserver<UpdateProjectDescription.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        var errorMessage = "Project ID is not found in UpdateProjectDescription request";
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.UPDATE);

      var updatedProject =
          projectDAO.updateProjectDescription(request.getId(), request.getDescription());
      var response =
          UpdateProjectDescription.Response.newBuilder().setProject(updatedProject).build();

      // Add succeeded event in local DB
      addEvent(
          updatedProject.getId(),
          Optional.of(updatedProject.getWorkspaceServiceId()),
          UPDATE_PROJECT_EVENT_TYPE,
          Optional.of("description"),
          Collections.emptyMap(),
          "project description updated successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, UpdateProjectDescription.Response.getDefaultInstance());
    }
  }

  @Override
  public void addProjectAttributes(
      AddProjectAttributes request,
      StreamObserver<AddProjectAttributes.Response> responseObserver) {
    try {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getAttributesList().isEmpty()) {
        errorMessage = "Project ID and Attribute list not found in AddProjectAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Project ID not found in AddProjectAttributes request";
      } else if (request.getAttributesList().isEmpty()) {
        errorMessage = "Attribute list not found in AddProjectAttributes request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.UPDATE);

      var updatedProject =
          projectDAO.addProjectAttributes(request.getId(), request.getAttributesList());
      var response = AddProjectAttributes.Response.newBuilder().setProject(updatedProject).build();
      addEvent(
          updatedProject.getId(),
          Optional.of(updatedProject.getWorkspaceServiceId()),
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
          "project attributes added successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, AddProjectAttributes.Response.getDefaultInstance());
    }
  }

  /**
   * Updates the project Attributes field from the Project Entity.
   *
   * @param UpdateProjectAttributes request, UpdateProjectAttributes.Response response
   * @return void
   */
  @Override
  public void updateProjectAttributes(
      UpdateProjectAttributes request,
      StreamObserver<UpdateProjectAttributes.Response> responseObserver) {
    try {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getAttribute().getKey().isEmpty()) {
        errorMessage = "Project ID and attribute key not found in UpdateProjectAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Project ID not found in UpdateProjectAttributes request";
      } else if (request.getAttribute().getKey().isEmpty()) {
        errorMessage = "Attribute key not found in UpdateProjectAttributes request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.UPDATE);

      var updatedProject =
          projectDAO.updateProjectAttributes(request.getId(), request.getAttribute());
      var response =
          UpdateProjectAttributes.Response.newBuilder().setProject(updatedProject).build();

      // Add succeeded event in local DB
      addEvent(
          updatedProject.getId(),
          Optional.of(updatedProject.getWorkspaceServiceId()),
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
          "project attributes updated successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, UpdateProjectAttributes.Response.getDefaultInstance());
    }
  }

  /**
   * This method provide List<KeyValue> attributes of given projectId in GetProjectAttributes
   * request.
   *
   * @param GetProjectAttributes request, GetProjectAttributes.Response response
   * @return void
   */
  @Override
  public void getProjectAttributes(
      GetAttributes request, StreamObserver<GetAttributes.Response> responseObserver) {
    try {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty()
          && request.getAttributeKeysList().isEmpty()
          && !request.getGetAll()) {
        errorMessage = "Project ID and Project attribute keys not found in GetAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Project ID not found in GetAttributes request";
      } else if (request.getAttributeKeysList().isEmpty() && !request.getGetAll()) {
        errorMessage = "Project attribute keys not found in GetAttributes request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.READ);

      List<KeyValue> attributes =
          projectDAO.getProjectAttributes(
              request.getId(), request.getAttributeKeysList(), request.getGetAll());

      var response = GetAttributes.Response.newBuilder().addAllAttributes(attributes).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetAttributes.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteProjectAttributes(
      DeleteProjectAttributes request,
      StreamObserver<DeleteProjectAttributes.Response> responseObserver) {
    try {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty()
          && request.getAttributeKeysList().isEmpty()
          && !request.getDeleteAll()) {
        errorMessage =
            "Project ID and Project attribute keys not found in DeleteProjectAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Project ID not found in DeleteProjectAttributes request";
      } else if (request.getAttributeKeysList().isEmpty() && !request.getDeleteAll()) {
        errorMessage = "Project attribute keys not found in DeleteProjectAttributes request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.DELETE);

      var updatedProject =
          projectDAO.deleteProjectAttributes(
              request.getId(), request.getAttributeKeysList(), request.getDeleteAll());
      var response =
          DeleteProjectAttributes.Response.newBuilder().setProject(updatedProject).build();

      // Add succeeded event in local DB
      Map<String, Object> extraField = new HashMap<>();
      if (request.getDeleteAll()) {
        extraField.put("attributes_delete_all", true);
      } else {
        extraField.put(
            "attribute_keys",
            new Gson()
                .toJsonTree(
                    request.getAttributeKeysList(),
                    new TypeToken<ArrayList<String>>() {}.getType()));
      }
      addEvent(
          updatedProject.getId(),
          Optional.of(updatedProject.getWorkspaceServiceId()),
          UPDATE_PROJECT_EVENT_TYPE,
          Optional.of("attributes"),
          extraField,
          "project attributes deleted successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteProjectAttributes.Response.getDefaultInstance());
    }
  }

  /**
   * Add the Tags in project Tags field.
   *
   * @param AddProjectTags request, AddProjectTags.Response response
   * @return void
   */
  @Override
  public void addProjectTags(
      AddProjectTags request, StreamObserver<AddProjectTags.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        var errorMessage = "Project ID not found in AddProjectTags request";
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.UPDATE);

      var updatedProject =
          projectDAO.addProjectTags(
              request.getId(), ModelDBUtils.checkEntityTagsLength(request.getTagsList()));
      var response = AddProjectTags.Response.newBuilder().setProject(updatedProject).build();

      // Add succeeded event in local DB
      addEvent(
          updatedProject.getId(),
          Optional.of(updatedProject.getWorkspaceServiceId()),
          UPDATE_PROJECT_EVENT_TYPE,
          Optional.of("tags"),
          Collections.singletonMap(
              "tags",
              new Gson()
                  .toJsonTree(
                      request.getTagsList(), new TypeToken<ArrayList<String>>() {}.getType())),
          "project tags added successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, AddProjectTags.Response.getDefaultInstance());
    }
  }

  @Override
  public void getProjectTags(GetTags request, StreamObserver<GetTags.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        var errorMessage = "Project ID not found in GetTags request";
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.READ);

      List<String> tags = projectDAO.getProjectTags(request.getId());
      var response = GetTags.Response.newBuilder().addAllTags(tags).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetTags.Response.getDefaultInstance());
    }
  }

  /**
   * Delete the project Tags field from the Project Entity.
   *
   * @param DeleteProjectTags request, DeleteProjectTags.Response response
   * @return void
   */
  @Override
  public void deleteProjectTags(
      DeleteProjectTags request, StreamObserver<DeleteProjectTags.Response> responseObserver) {
    try {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getTagsList().isEmpty() && !request.getDeleteAll()) {
        errorMessage = "Project ID and Project tags not found in DeleteProjectTags request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Project ID not found in DeleteProjectTags request";
      } else if (request.getTagsList().isEmpty() && !request.getDeleteAll()) {
        errorMessage = "Project tags not found in DeleteProjectTags request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.UPDATE);

      var updatedProject =
          projectDAO.deleteProjectTags(
              request.getId(), request.getTagsList(), request.getDeleteAll());
      var response = DeleteProjectTags.Response.newBuilder().setProject(updatedProject).build();

      // Add succeeded event in local DB
      Map<String, Object> extraField = new HashMap<>();
      if (request.getDeleteAll()) {
        extraField.put("tags_delete_all", true);
      } else {
        extraField.put(
            "tags",
            new Gson()
                .toJsonTree(
                    request.getTagsList(), new TypeToken<ArrayList<String>>() {}.getType()));
      }
      addEvent(
          updatedProject.getId(),
          Optional.of(updatedProject.getWorkspaceServiceId()),
          UPDATE_PROJECT_EVENT_TYPE,
          Optional.of("tags"),
          extraField,
          "project tags deleted successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteProjectTags.Response.getDefaultInstance());
    }
  }

  @Override
  public void addProjectTag(
      AddProjectTag request, StreamObserver<AddProjectTag.Response> responseObserver) {
    try {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getTag().isEmpty()) {
        errorMessage = "Project ID and Project tag not found in AddProjectTag request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Project ID not found in AddProjectTag request";
      } else if (request.getTag().isEmpty()) {
        errorMessage = "Project tag not found in AddProjectTag request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.UPDATE);

      var updatedProject =
          projectDAO.addProjectTags(
              request.getId(),
              ModelDBUtils.checkEntityTagsLength(Collections.singletonList(request.getTag())));
      var response = AddProjectTag.Response.newBuilder().setProject(updatedProject).build();

      // Add succeeded event in local DB
      addEvent(
          updatedProject.getId(),
          Optional.of(updatedProject.getWorkspaceServiceId()),
          UPDATE_PROJECT_EVENT_TYPE,
          Optional.of("tags"),
          Collections.singletonMap(
              "tags",
              new Gson()
                  .toJsonTree(
                      Collections.singletonList(request.getTag()),
                      new TypeToken<ArrayList<String>>() {}.getType())),
          "project tag added successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, AddProjectTag.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteProjectTag(
      DeleteProjectTag request, StreamObserver<DeleteProjectTag.Response> responseObserver) {
    try {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getTag().isEmpty()) {
        errorMessage = "Project ID and Project tag not found in DeleteProjectTag request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Project ID not found in DeleteProjectTag request";
      } else if (request.getTag().isEmpty()) {
        errorMessage = "Project tag not found in DeleteProjectTag request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.UPDATE);

      var updatedProject =
          projectDAO.deleteProjectTags(request.getId(), Arrays.asList(request.getTag()), false);
      var response = DeleteProjectTag.Response.newBuilder().setProject(updatedProject).build();

      // Add succeeded event in local DB
      addEvent(
          updatedProject.getId(),
          Optional.of(updatedProject.getWorkspaceServiceId()),
          UPDATE_PROJECT_EVENT_TYPE,
          Optional.of("tags"),
          Collections.singletonMap(
              "tags",
              new Gson()
                  .toJsonTree(
                      Collections.singletonList(request.getTag()),
                      new TypeToken<ArrayList<String>>() {}.getType())),
          "project tag deleted successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, DeleteProjectTag.Response.getDefaultInstance());
    }
  }

  /**
   * Get ProjectId from DeleteProject request and delete it from database.
   *
   * @param DeleteProject request, DeleteProject.Response response
   * @return void
   */
  @Override
  public void deleteProject(
      DeleteProject request, StreamObserver<DeleteProject.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        var errorMessage = "Project ID not found in DeleteProject request";
        throw new InvalidArgumentException(errorMessage);
      }

      List<String> deletedProjectIds =
          projectDAO.deleteProjects(Collections.singletonList(request.getId()));
      var response =
          DeleteProject.Response.newBuilder().setStatus(!deletedProjectIds.isEmpty()).build();

      // Add succeeded event in local DB
      addEvent(
          request.getId(),
          Optional.empty(),
          DELETE_PROJECT_EVENT_TYPE,
          Optional.empty(),
          Collections.emptyMap(),
          "project deleted successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, DeleteProject.Response.getDefaultInstance());
    }
  }

  /**
   * Gets all the projects belonging to the user and returns as response. If user auth is not
   * enabled, it returns all the projects from the database.
   *
   * @param GetProjects request, GetProjects.Response response
   * @return void
   */
  @Override
  public void getProjects(
      GetProjects request, StreamObserver<GetProjects.Response> responseObserver) {
    try {
      LOGGER.debug("getting project");
      var userInfo = authService.getCurrentLoginUserInfo();

      FindProjects.Builder findProjects =
          FindProjects.newBuilder()
              .setPageNumber(request.getPageNumber())
              .setPageLimit(request.getPageLimit())
              .setAscending(request.getAscending())
              .setSortKey(request.getSortKey())
              .setWorkspaceName(request.getWorkspaceName());

      var projectPaginationDTO =
          projectDAO.findProjects(findProjects.build(), null, userInfo, ResourceVisibility.PRIVATE);

      List<Project> projects = projectPaginationDTO.getProjects();
      var response =
          GetProjects.Response.newBuilder()
              .addAllProjects(projects)
              .setTotalRecords(projectPaginationDTO.getTotalRecords())
              .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetProjects.Response.getDefaultInstance());
    }
  }

  @Override
  public void getProjectById(
      GetProjectById request, StreamObserver<GetProjectById.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        var errorMessage = "Project ID not found in GetProjectById request";
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.READ);

      var project = projectDAO.getProjectByID(request.getId());
      var response = GetProjectById.Response.newBuilder().setProject(project).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetProjectById.Response.getDefaultInstance());
    }
  }

  @Override
  public void getProjectByName(
      GetProjectByName request, StreamObserver<GetProjectByName.Response> responseObserver) {
    try {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getName().isEmpty()) {
        errorMessage = "Project name is not found in GetProjectByName request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      // Get the user info from the Context
      var userInfo = authService.getCurrentLoginUserInfo();
      String workspaceName =
          request.getWorkspaceName().isEmpty()
              ? authService.getUsernameFromUserInfo(userInfo)
              : request.getWorkspaceName();
      List<GetResourcesResponseItem> responseItem =
          mdbRoleService.getEntityResourcesByName(
              Optional.of(request.getName()),
              Optional.empty(),
              ModelDBServiceResourceTypes.PROJECT);

      FindProjects.Builder findProjects =
          FindProjects.newBuilder()
              .addAllProjectIds(
                  responseItem.stream()
                      .map(GetResourcesResponseItem::getResourceId)
                      .collect(Collectors.toList()))
              .setWorkspaceName(workspaceName);

      var projectPaginationDTO =
          projectDAO.findProjects(findProjects.build(), null, userInfo, ResourceVisibility.PRIVATE);

      if (projectPaginationDTO.getTotalRecords() == 0) {
        throw new NotFoundException("Project not found");
      }

      Project selfOwnerProject = null;
      List<Project> sharedProjects = new ArrayList<>();
      Set<String> projectIds = new HashSet<>();

      for (Project project : projectPaginationDTO.getProjects()) {
        if (userInfo == null
            || project.getOwner().equals(authService.getVertaIdFromUserInfo(userInfo))) {
          selfOwnerProject = project;
        } else {
          sharedProjects.add(project);
        }
        projectIds.add(project.getId());
      }

      var responseBuilder = GetProjectByName.Response.newBuilder();
      if (selfOwnerProject != null) {
        responseBuilder.setProjectByUser(selfOwnerProject);
      }
      responseBuilder.addAllSharedProjects(sharedProjects);

      var response = responseBuilder.build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetProjectByName.Response.getDefaultInstance());
    }
  }

  @Override
  public void verifyConnection(
      Empty request, StreamObserver<VerifyConnectionResponse> responseObserver) {
    responseObserver.onNext(VerifyConnectionResponse.newBuilder().setStatus(true).build());
    responseObserver.onCompleted();
  }

  @Override
  public void deepCopyProject(
      DeepCopyProject request, StreamObserver<DeepCopyProject.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId() == null) {
        var errorMessage = "Project ID not found in DeepCopyProject request";
        throw new InvalidArgumentException(errorMessage);
      }

      // Get the user info from the Context
      var userInfo = authService.getCurrentLoginUserInfo();

      var project = projectDAO.deepCopyProjectForUser(request.getId(), userInfo);
      var response = DeepCopyProject.Response.newBuilder().setProject(project).build();

      // Add succeeded event in local DB
      addEvent(
          project.getId(),
          Optional.of(project.getWorkspaceServiceId()),
          "clone.resource.project.clone_project_succeeded",
          Optional.empty(),
          Collections.emptyMap(),
          "project clone successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, DeepCopyProject.Response.getDefaultInstance());
    }
  }

  private Map<String, Double[]> getMinMaxMetricsValueMap(
      Map<String, Double[]> minMaxMetricsValueMap, KeyValue keyValue) {
    Double value = keyValue.getValue().getNumberValue();
    Double[] minMaxValueArray = minMaxMetricsValueMap.get(keyValue.getKey());
    if (minMaxValueArray == null) {
      minMaxValueArray = new Double[2]; // Index 0 = minValue, Index 1 = maxValue
    }
    if (minMaxValueArray[0] == null || minMaxValueArray[0] > value) {
      minMaxValueArray[0] = value;
    }
    if (minMaxValueArray[1] == null || minMaxValueArray[1] < value) {
      minMaxValueArray[1] = value;
    }
    minMaxMetricsValueMap.put(keyValue.getKey(), minMaxValueArray);
    return minMaxMetricsValueMap;
  }

  @Override
  public void getSummary(GetSummary request, StreamObserver<GetSummary.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getEntityId().isEmpty()) {
        var errorMessage = "Project ID not found in GetSummary request";
        throw new InvalidArgumentException(errorMessage);
      }

      LOGGER.debug("Getting user info");
      var userInfo = authService.getCurrentLoginUserInfo();

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getEntityId(), ModelDBServiceActions.READ);

      List<Project> projects =
          projectDAO.getProjects(ModelDBConstants.ID, request.getEntityId(), userInfo);
      if (projects.isEmpty()) {
        var errorMessage = "Project not found for given EntityId";
        throw new NotFoundException(errorMessage);
      } else if (projects.size() != 1) {
        var errorMessage = "Multiple projects found for given EntityId";
        throw new InternalErrorException(errorMessage);
      }
      var project = projects.get(0);

      Long experimentCount =
          projectDAO.getExperimentCount(Collections.singletonList(project.getId()));
      Long experimentRunCount =
          projectDAO.getExperimentRunCount(Collections.singletonList(project.getId()));

      List<ExperimentRun> experimentRuns =
          experimentRunDAO.getExperimentRuns(
              ModelDBConstants.PROJECT_ID, request.getEntityId(), userInfo);

      LastModifiedExperimentRunSummary lastModifiedExperimentRunSummary = null;
      List<MetricsSummary> minMaxMetricsValueList = new ArrayList<>();
      if (!experimentRuns.isEmpty()) {
        ExperimentRun lastModifiedExperimentRun = null;
        Map<String, Double[]> minMaxMetricsValueMap = new HashMap<>(); // In double[], Index 0 =
        // minValue, Index 1 =
        // maxValue
        Set<String> keySet = new HashSet<>();

        for (ExperimentRun experimentRun : experimentRuns) {
          if (lastModifiedExperimentRun == null
              || lastModifiedExperimentRun.getDateUpdated() < experimentRun.getDateUpdated()) {
            lastModifiedExperimentRun = experimentRun;
          }

          for (KeyValue keyValue : experimentRun.getMetricsList()) {
            keySet.add(keyValue.getKey());
            minMaxMetricsValueMap = getMinMaxMetricsValueMap(minMaxMetricsValueMap, keyValue);
          }
        }

        lastModifiedExperimentRunSummary =
            LastModifiedExperimentRunSummary.newBuilder()
                .setLastUpdatedTime(lastModifiedExperimentRun.getDateUpdated())
                .setName(lastModifiedExperimentRun.getName())
                .build();

        for (String key : keySet) {
          Double[] minMaxValueArray = minMaxMetricsValueMap.get(key); // Index 0 = minValue, Index 1
          // = maxValue
          var minMaxMetricsSummary =
              MetricsSummary.newBuilder()
                  .setKey(key)
                  .setMinValue(minMaxValueArray[0]) // Index 0 =
                  // minValue
                  .setMaxValue(minMaxValueArray[1]) // Index 1 = maxValue
                  .build();
          minMaxMetricsValueList.add(minMaxMetricsSummary);
        }
      }

      var responseBuilder =
          GetSummary.Response.newBuilder()
              .setName(project.getName())
              .setLastUpdatedTime(project.getDateUpdated())
              .setTotalExperiment(experimentCount)
              .setTotalExperimentRuns(experimentRunCount)
              .addAllMetrics(minMaxMetricsValueList);

      if (lastModifiedExperimentRunSummary != null) {
        responseBuilder.setLastModifiedExperimentRunSummary(lastModifiedExperimentRunSummary);
      }

      var response = responseBuilder.build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetSummary.Response.getDefaultInstance());
    }
  }

  @Override
  public void setProjectReadme(
      SetProjectReadme request, StreamObserver<SetProjectReadme.Response> responseObserver) {
    try {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getReadmeText() == null) {
        errorMessage = "Project ID and Project Readme text not found in SetProjectReadme request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Project ID not found in SetProjectReadme request";
      } else if (request.getReadmeText() == null) {
        errorMessage = "Project Readme text not found in SetProjectReadme request";
      }

      if (errorMessage != null) {
        LOGGER.info(errorMessage);
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.UPDATE);

      var updatedProject = projectDAO.updateProjectReadme(request.getId(), request.getReadmeText());
      var response = SetProjectReadme.Response.newBuilder().setProject(updatedProject).build();

      // Add succeeded event in local DB
      JsonObject eventMetadata = new JsonObject();
      eventMetadata.addProperty("entity_id", updatedProject.getId());
      eventMetadata.addProperty("updated_field", "read_me_text");
      eventMetadata.addProperty("updated_field_value", request.getReadmeText());
      eventMetadata.addProperty("message", "project read_me_text updated successfully");
      futureEventDAO.addLocalEventWithBlocking(
          ModelDBServiceResourceTypes.PROJECT.name(),
          UPDATE_PROJECT_EVENT_TYPE,
          updatedProject.getWorkspaceServiceId(),
          eventMetadata);

      addEvent(
          updatedProject.getId(),
          Optional.of(updatedProject.getWorkspaceServiceId()),
          UPDATE_PROJECT_EVENT_TYPE,
          Optional.of("read_me_text"),
          Collections.emptyMap(),
          "project read_me_text updated successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, SetProjectReadme.Response.getDefaultInstance());
    }
  }

  @Override
  public void getProjectReadme(
      GetProjectReadme request, StreamObserver<GetProjectReadme.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        var errorMessage = "Project ID not found in GetProjectReadme request";
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.READ);

      var project = projectDAO.getProjectByID(request.getId());
      var response =
          GetProjectReadme.Response.newBuilder().setReadmeText(project.getReadmeText()).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetProjectReadme.Response.getDefaultInstance());
    }
  }

  @Override
  public void setProjectShortName(
      SetProjectShortName request, StreamObserver<SetProjectShortName.Response> responseObserver) {
    try {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getShortName().isEmpty()) {
        errorMessage = "Project ID and Project shortName not found in SetProjectShortName request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Project ID not found in SetProjectShortName request";
      } else if (request.getShortName().isEmpty()) {
        errorMessage = "Project shortName not found in SetProjectShortName request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      LOGGER.debug("Getting user info");
      var userInfo = authService.getCurrentLoginUserInfo();

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.UPDATE);

      String projectShortName = ModelDBUtils.convertToProjectShortName(request.getShortName());
      if (!projectShortName.equals(request.getShortName())) {
        errorMessage = "Project short name is not valid";
        throw new InternalErrorException(errorMessage);
      }

      var project =
          projectDAO.setProjectShortName(request.getId(), request.getShortName(), userInfo);
      var response = SetProjectShortName.Response.newBuilder().setProject(project).build();

      // Add succeeded event in local DB
      addEvent(
          project.getId(),
          Optional.of(project.getWorkspaceServiceId()),
          UPDATE_PROJECT_EVENT_TYPE,
          Optional.of("short_name"),
          Collections.singletonMap("short_name", project.getShortName()),
          "project short_name updated successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, SetProjectShortName.Response.getDefaultInstance());
    }
  }

  @Override
  public void getProjectShortName(
      GetProjectShortName request, StreamObserver<GetProjectShortName.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        var errorMessage = "Project ID not found in GetProjectShortName request";
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.READ);

      var project = projectDAO.getProjectByID(request.getId());
      var response =
          GetProjectShortName.Response.newBuilder().setShortName(project.getShortName()).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetProjectShortName.Response.getDefaultInstance());
    }
  }

  @Override
  public void logProjectCodeVersion(
      LogProjectCodeVersion request, StreamObserver<Response> responseObserver) {
    try {
      /*Parameter validation*/
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getCodeVersion() == null) {
        errorMessage = "Project ID and Code version not found in LogProjectCodeVersion request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Project ID not found in LogProjectCodeVersion request";
      } else if (request.getCodeVersion() == null) {
        errorMessage = "CodeVersion not found in LogProjectCodeVersion request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.UPDATE);

      var existingProject = projectDAO.getProjectByID(request.getId());
      Project updatedProject;
      /*Update Code version*/
      if (!existingProject.getCodeVersionSnapshot().hasCodeArchive()
          && !existingProject.getCodeVersionSnapshot().hasGitSnapshot()) {
        updatedProject =
            projectDAO.logProjectCodeVersion(request.getId(), request.getCodeVersion());
      } else {
        errorMessage = "Code version already logged for project " + existingProject.getId();
        throw new AlreadyExistsException(errorMessage);
      }
      /*Build response*/
      var responseBuilder = LogProjectCodeVersion.Response.newBuilder().setProject(updatedProject);

      // Add succeeded event in local DB
      addEvent(
          updatedProject.getId(),
          Optional.of(updatedProject.getWorkspaceServiceId()),
          UPDATE_PROJECT_EVENT_TYPE,
          Optional.of("code_version"),
          Collections.emptyMap(),
          "code_version logged successfully");

      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, LogProjectCodeVersion.Response.getDefaultInstance());
    }
  }

  @Override
  public void getProjectCodeVersion(
      GetProjectCodeVersion request,
      StreamObserver<GetProjectCodeVersion.Response> responseObserver) {
    try {
      /*Parameter validation*/
      if (request.getId().isEmpty()) {
        var errorMessage = "Project ID not found in GetProjectCodeVersion request";
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.UPDATE);

      /*Get code version*/
      var existingProject = projectDAO.getProjectByID(request.getId());
      var codeVersion = existingProject.getCodeVersionSnapshot();

      var response =
          GetProjectCodeVersion.Response.newBuilder().setCodeVersion(codeVersion).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetProjectCodeVersion.Response.getDefaultInstance());
    }
  }

  @Override
  public void findProjects(
      FindProjects request, StreamObserver<FindProjects.Response> responseObserver) {
    try {
      /*User validation*/
      // Get the user info from the Context
      var userInfo = authService.getCurrentLoginUserInfo();

      var projectPaginationDTO =
          projectDAO.findProjects(request, null, userInfo, ResourceVisibility.PRIVATE);

      List<Project> projects = projectPaginationDTO.getProjects();
      var response =
          FindProjects.Response.newBuilder()
              .addAllProjects(projects)
              .setTotalRecords(projectPaginationDTO.getTotalRecords())
              .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, FindProjects.Response.getDefaultInstance());
    }
  }

  @Override
  public void getUrlForArtifact(
      GetUrlForArtifact request, StreamObserver<GetUrlForArtifact.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty()
          && request.getKey().isEmpty()
          && request.getMethod().isEmpty()) {
        errorMessage = "Project ID and Key and Method not found in GetUrlForArtifact request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Project ID not found in GetUrlForArtifact request";
      } else if (request.getKey().isEmpty()) {
        errorMessage = "Artifact Key not found in GetUrlForArtifact request";
      } else if (request.getMethod().isEmpty()) {
        errorMessage = "Method is not found in GetUrlForArtifact request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.READ);

      String s3Key = null;

      /*Process code*/
      if (request.getArtifactType() == ArtifactType.CODE) {
        // just creating the error string
        errorMessage = "Code versioning artifact not found at project level";
        s3Key = getUrlForCode(request);
      } else {
        errorMessage = "Project level artifacts only supported for code";
        throw new InvalidArgumentException(errorMessage);
      }

      if (s3Key == null) {
        throw new NotFoundException(errorMessage);
      }
      var response = artifactStoreDAO.getUrlForArtifact(s3Key, request.getMethod());
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetUrlForArtifact.Response.getDefaultInstance());
    }
  }

  private String getUrlForCode(GetUrlForArtifact request) {
    String s3Key = null;
    var proj = projectDAO.getProjectByID(request.getId());
    if (proj.getCodeVersionSnapshot() != null
        && proj.getCodeVersionSnapshot().getCodeArchive() != null) {
      s3Key = proj.getCodeVersionSnapshot().getCodeArchive().getPath();
    }
    return s3Key;
  }

  @Override
  public void logArtifacts(
      LogProjectArtifacts request, StreamObserver<LogProjectArtifacts.Response> responseObserver) {
    try {
      if (request.getId().isEmpty() && request.getArtifactsList().isEmpty()) {
        throw new InvalidArgumentException(
            "Project ID and Artifacts not found in LogArtifacts request");
      } else if (request.getId().isEmpty()) {
        throw new InvalidArgumentException("Project ID not found in LogArtifacts request");
      } else if (request.getArtifactsList().isEmpty()) {
        throw new InvalidArgumentException("Artifacts not found in LogArtifacts request");
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.UPDATE);

      List<Artifact> artifactList =
          ModelDBUtils.getArtifactsWithUpdatedPath(request.getId(), request.getArtifactsList());
      var updatedProject = projectDAO.logArtifacts(request.getId(), artifactList);
      var responseBuilder = LogProjectArtifacts.Response.newBuilder().setProject(updatedProject);

      // Add succeeded event in local DB
      addEvent(
          updatedProject.getId(),
          Optional.of(updatedProject.getWorkspaceServiceId()),
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
          "project artifacts added successfully");

      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, LogProjectArtifacts.Response.getDefaultInstance());
    }
  }

  @Override
  public void getArtifacts(
      GetArtifacts request, StreamObserver<GetArtifacts.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        throw new InvalidArgumentException("Project ID not found in GetArtifacts request");
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.READ);

      List<Artifact> artifactList = projectDAO.getProjectArtifacts(request.getId());
      var response = GetArtifacts.Response.newBuilder().addAllArtifacts(artifactList).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetArtifacts.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteArtifact(
      DeleteProjectArtifact request,
      StreamObserver<DeleteProjectArtifact.Response> responseObserver) {
    try {
      if (request.getId().isEmpty() && request.getKey().isEmpty()) {
        throw new InvalidArgumentException(
            "Project ID and Artifact key not found in DeleteArtifact request");
      } else if (request.getId().isEmpty()) {
        throw new InvalidArgumentException("Project ID not found in DeleteArtifact request");
      } else if (request.getKey().isEmpty()) {
        throw new InvalidArgumentException("Artifact key not found in DeleteArtifact request");
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getId(), ModelDBServiceActions.UPDATE);

      var updatedProject = projectDAO.deleteArtifacts(request.getId(), request.getKey());
      var response = DeleteProjectArtifact.Response.newBuilder().setProject(updatedProject).build();

      // Add succeeded event in local DB
      addEvent(
          updatedProject.getId(),
          Optional.of(updatedProject.getWorkspaceServiceId()),
          UPDATE_PROJECT_EVENT_TYPE,
          Optional.of("artifacts"),
          Collections.singletonMap(
              "artifact_keys",
              new Gson()
                  .toJsonTree(
                      Collections.singletonList(request.getKey()),
                      new TypeToken<ArrayList<String>>() {}.getType())),
          "project artifact deleted successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteProjectArtifact.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteProjects(
      DeleteProjects request, StreamObserver<DeleteProjects.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getIdsList().isEmpty()) {
        throw new InvalidArgumentException("Project IDs not found in DeleteProjects request");
      }

      List<String> deletedProjectIds = projectDAO.deleteProjects(request.getIdsList());
      var response =
          DeleteProjects.Response.newBuilder().setStatus(!deletedProjectIds.isEmpty()).build();

      // Add succeeded event in local DB
      for (String projectId : deletedProjectIds) {
        addEvent(
            projectId,
            Optional.empty(),
            DELETE_PROJECT_EVENT_TYPE,
            Optional.empty(),
            Collections.emptyMap(),
            "project deleted successfully");
      }
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, DeleteProjects.Response.getDefaultInstance());
    }
  }
}
