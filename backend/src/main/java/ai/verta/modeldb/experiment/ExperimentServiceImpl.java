package ai.verta.modeldb.experiment;

import ai.verta.common.Artifact;
import ai.verta.common.ArtifactTypeEnum.ArtifactType;
import ai.verta.common.KeyValue;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.*;
import ai.verta.modeldb.DeleteExperimentAttributes.Response;
import ai.verta.modeldb.ExperimentServiceGrpc.ExperimentServiceImplBase;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.event.FutureEventDAO;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.exceptions.PermissionDeniedException;
import ai.verta.modeldb.metadata.MetadataServiceImpl;
import ai.verta.modeldb.project.ProjectDAO;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.UserInfo;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import io.grpc.stub.StreamObserver;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExperimentServiceImpl extends ExperimentServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(ExperimentServiceImpl.class);
  private static final String UPDATE_EVENT_TYPE =
      "update.resource.experiment.update_experiment_succeeded";
  private final AuthService authService;
  private final MDBRoleService mdbRoleService;
  private final ExperimentDAO experimentDAO;
  private final ProjectDAO projectDAO;
  private final ArtifactStoreDAO artifactStoreDAO;
  private final FutureEventDAO futureEventDAO;

  public ExperimentServiceImpl(ServiceSet serviceSet, DAOSet daoSet) {
    this.authService = serviceSet.authService;
    this.mdbRoleService = serviceSet.mdbRoleService;
    this.experimentDAO = daoSet.experimentDAO;
    this.projectDAO = daoSet.projectDAO;
    this.artifactStoreDAO = daoSet.artifactStoreDAO;
    this.futureEventDAO = daoSet.futureEventDAO;
  }

  private void addEvent(
      String entityId,
      String projectId,
      long workspaceId,
      String eventType,
      Optional<String> updatedField,
      Map<String, Object> extraFieldsMap,
      String eventMessage) {
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
    futureEventDAO.addLocalEventWithBlocking(
        ModelDBServiceResourceTypes.EXPERIMENT.name(), eventType, workspaceId, eventMetadata);
  }

  /**
   * Convert CreateExperiment request to Experiment object. This method generate the experiment Id
   * using UUID and put it in Experiment object.
   *
   * @param request : CreateExperiment request
   * @param userInfo : current login UserInfo
   * @return Experiment : experimentDeleteExperiments
   */
  private Experiment getExperimentFromRequest(CreateExperiment request, UserInfo userInfo) {

    String errorMessage = null;
    if (request.getProjectId().isEmpty()) {
      errorMessage = "Project ID not found in CreateExperiment request";
    } else if (request.getName().isEmpty()) {
      request = request.toBuilder().setName(MetadataServiceImpl.createRandomName()).build();
    }

    if (errorMessage != null) {
      throw new InvalidArgumentException(errorMessage);
    }

    /*
     * Create Experiment entity from given CreateExperiment request. generate UUID and put as id in
     * Experiment for uniqueness.
     */
    var experimentBuilder =
        Experiment.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setProjectId(request.getProjectId())
            .setName(ModelDBUtils.checkEntityNameLength(request.getName()))
            .setDescription(request.getDescription())
            .addAllAttributes(request.getAttributesList())
            .addAllTags(ModelDBUtils.checkEntityTagsLength(request.getTagsList()))
            .addAllArtifacts(request.getArtifactsList())
            .setVersionNumber(1L);

    if (request.getDateCreated() != 0L) {
      experimentBuilder
          .setDateCreated(request.getDateCreated())
          .setDateUpdated(request.getDateCreated());
    } else {
      experimentBuilder
          .setDateCreated(Calendar.getInstance().getTimeInMillis())
          .setDateUpdated(Calendar.getInstance().getTimeInMillis());
    }
    if (userInfo != null) {
      experimentBuilder.setOwner(authService.getVertaIdFromUserInfo(userInfo));
    }

    return experimentBuilder.build();
  }

  /**
   * Convert CreateExperiment request to Experiment entity and insert in database.
   *
   * @param request : CreateExperiment request
   * @param responseObserver : CreateExperiment.Response responseObserver
   */
  @Override
  public void createExperiment(
      CreateExperiment request, StreamObserver<CreateExperiment.Response> responseObserver) {
    try {

      // Get the user info from the Context
      var userInfo = authService.getCurrentLoginUserInfo();

      var experiment = getExperimentFromRequest(request, userInfo);

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT,
          experiment.getProjectId(),
          ModelDBServiceActions.UPDATE);

      experiment = experimentDAO.insertExperiment(experiment, userInfo);
      var response = CreateExperiment.Response.newBuilder().setExperiment(experiment).build();

      // Add succeeded event in local DB
      addEvent(
          experiment.getId(),
          experiment.getProjectId(),
          authService.getWorkspaceIdFromUserInfo(userInfo),
          "add.resource.experiment.add_experiment_succeeded",
          Optional.empty(),
          Collections.emptyMap(),
          "experiment added successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, CreateExperiment.Response.getDefaultInstance());
    }
  }

  @Override
  public void getExperimentsInProject(
      GetExperimentsInProject request,
      StreamObserver<GetExperimentsInProject.Response> responseObserver) {
    try {

      if (request.getProjectId().isEmpty()) {
        var errorMessage = "Project ID not found in GetExperimentsInProject request";
        throw new InvalidArgumentException(errorMessage);
      }

      if (!projectDAO.projectExistsInDB(request.getProjectId())) {
        var errorMessage = "Project ID not found.";
        throw new NotFoundException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getProjectId(), ModelDBServiceActions.READ);

      var experimentPaginationDTO =
          experimentDAO.getExperimentsInProject(
              projectDAO,
              request.getProjectId(),
              request.getPageNumber(),
              request.getPageLimit(),
              request.getAscending(),
              request.getSortKey());
      List<Experiment> experiments = experimentPaginationDTO.getExperiments();
      var response =
          GetExperimentsInProject.Response.newBuilder()
              .addAllExperiments(experiments)
              .setTotalRecords(experimentPaginationDTO.getTotalRecords())
              .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetExperimentsInProject.Response.getDefaultInstance());
    }
  }

  @Override
  public void getExperimentById(
      GetExperimentById request, StreamObserver<GetExperimentById.Response> responseObserver) {
    try {

      if (request.getId().isEmpty()) {
        var errorMessage = "Experiment ID not found in GetExperimentById request";
        throw new InvalidArgumentException(errorMessage);
      }

      var experiment = experimentDAO.getExperiment(request.getId());
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT,
          experiment.getProjectId(),
          ModelDBServiceActions.READ);

      var response = GetExperimentById.Response.newBuilder().setExperiment(experiment).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetExperimentById.Response.getDefaultInstance());
    }
  }

  @Override
  public void getExperimentByName(
      GetExperimentByName request, StreamObserver<GetExperimentByName.Response> responseObserver) {
    try {

      String errorMessage = null;
      if (request.getProjectId().isEmpty() || request.getName().isEmpty()) {
        errorMessage = "Experiment name and Project ID is not found in GetExperimentByName request";
      } else if (request.getProjectId().isEmpty()) {
        errorMessage = "Project ID not found in GetExperimentByName request";
      } else if (request.getName().isEmpty()) {
        errorMessage = "Experiment name not found in GetExperimentByName request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getProjectId(), ModelDBServiceActions.READ);

      List<KeyValue> keyValue = new ArrayList<>();
      var projectIdValue = Value.newBuilder().setStringValue(request.getProjectId()).build();
      var nameValue = Value.newBuilder().setStringValue(request.getName()).build();
      keyValue.add(
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.PROJECT_ID)
              .setValue(projectIdValue)
              .build());
      keyValue.add(KeyValue.newBuilder().setKey(ModelDBConstants.NAME).setValue(nameValue).build());

      List<Experiment> experiments = experimentDAO.getExperiments(keyValue);
      if (experiments == null || experiments.isEmpty()) {
        errorMessage =
            "Experiment with name " + nameValue + " not found in project " + projectIdValue;
        throw new ModelDBException(errorMessage, Code.NOT_FOUND);
      }
      if (experiments.size() != 1) {
        errorMessage =
            "Multiple experiments with name " + nameValue + " found in project " + projectIdValue;
        throw new ModelDBException(errorMessage, Code.INTERNAL);
      }
      var response =
          GetExperimentByName.Response.newBuilder().setExperiment(experiments.get(0)).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetExperimentByName.Response.getDefaultInstance());
    }
  }

  /**
   * Update Experiment name Or Description in Experiment Entity. Create Experiment object with
   * updated data from UpdateExperimentNameOrDescription request and update in database.
   *
   * @param request : UpdateExperimentNameOrDescription request
   * @param responseObserver : UpdateExperimentNameOrDescription.Response responseObserver
   */
  @Override
  public void updateExperimentNameOrDescription(
      UpdateExperimentNameOrDescription request,
      StreamObserver<UpdateExperimentNameOrDescription.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        var errorMessage = "Experiment ID not found in UpdateExperimentNameOrDescription request";
        throw new InvalidArgumentException(errorMessage);
      } else if (request.getName().isEmpty()) {
        request = request.toBuilder().setName(MetadataServiceImpl.createRandomName()).build();
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment updatedExperiment = null;
      Map<String, String> updatedFieldsMap = new HashMap<>();
      if (!request.getName().isEmpty()) {
        updatedExperiment =
            experimentDAO.updateExperimentName(
                request.getId(), ModelDBUtils.checkEntityNameLength(request.getName()));
        updatedFieldsMap.put("name", updatedExperiment.getName());
      }
      // FIXME: this code never allows us to set the description as an empty string
      if (!request.getDescription().isEmpty()) {
        updatedExperiment =
            experimentDAO.updateExperimentDescription(request.getId(), request.getDescription());
      }

      var response =
          UpdateExperimentNameOrDescription.Response.newBuilder()
              .setExperiment(updatedExperiment)
              .build();

      // Add succeeded event in local DB
      for (Map.Entry<String, String> entry : updatedFieldsMap.entrySet()) {
        addEvent(
            updatedExperiment.getId(),
            updatedExperiment.getProjectId(),
            authService.getWorkspaceIdFromUserInfo(authService.getCurrentLoginUserInfo()),
            UPDATE_EVENT_TYPE,
            Optional.of(entry.getKey()),
            Collections.singletonMap(entry.getKey(), entry.getValue()),
            String.format("experiment %s updated successfully", entry.getKey()));
      }

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, UpdateExperimentNameOrDescription.Response.getDefaultInstance());
    }
  }

  /**
   * Update Experiment name in Experiment Entity. Create Experiment object with updated data from
   * UpdateExperimentName request and update in database.
   *
   * @param request : UpdateExperimentName request
   * @param responseObserver : UpdateExperimentName.Response responseObserver
   */
  @Override
  public void updateExperimentName(
      UpdateExperimentName request,
      StreamObserver<UpdateExperimentName.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        throw new InvalidArgumentException(
            "Experiment ID not found in UpdateExperimentName request");
      } else if (request.getName().isEmpty()) {
        request = request.toBuilder().setName(MetadataServiceImpl.createRandomName()).build();
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      var updatedExperiment =
          experimentDAO.updateExperimentName(
              request.getId(), ModelDBUtils.checkEntityNameLength(request.getName()));

      var response =
          UpdateExperimentName.Response.newBuilder().setExperiment(updatedExperiment).build();

      // Add succeeded event in local DB
      addEvent(
          updatedExperiment.getId(),
          updatedExperiment.getProjectId(),
          authService.getWorkspaceIdFromUserInfo(authService.getCurrentLoginUserInfo()),
          UPDATE_EVENT_TYPE,
          Optional.of("name"),
          Collections.singletonMap("name", updatedExperiment.getName()),
          "experiment name updated successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, UpdateExperimentName.Response.getDefaultInstance());
    }
  }

  /**
   * Update Experiment Description in Experiment Entity. Create Experiment object with updated data
   * from UpdateExperimentDescription request and update in database.
   *
   * @param request : UpdateExperimentDescription request
   * @param responseObserver : UpdateExperimentDescription.Response responseObserver
   */
  @Override
  public void updateExperimentDescription(
      UpdateExperimentDescription request,
      StreamObserver<UpdateExperimentDescription.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        throw new InvalidArgumentException(
            "Experiment ID not found in UpdateExperimentDescription request");
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      var updatedExperiment =
          experimentDAO.updateExperimentDescription(request.getId(), request.getDescription());
      var response =
          UpdateExperimentDescription.Response.newBuilder()
              .setExperiment(updatedExperiment)
              .build();

      // Add succeeded event in local DB
      addEvent(
          updatedExperiment.getId(),
          updatedExperiment.getProjectId(),
          authService.getWorkspaceIdFromUserInfo(authService.getCurrentLoginUserInfo()),
          UPDATE_EVENT_TYPE,
          Optional.of("description"),
          Collections.emptyMap(),
          "experiment description updated successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, UpdateExperimentDescription.Response.getDefaultInstance());
    }
  }

  @Override
  public void addExperimentTags(
      AddExperimentTags request, StreamObserver<AddExperimentTags.Response> responseObserver) {

    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getTagsList().isEmpty()) {
        errorMessage = "Experiment ID and Experiment tags not found in AddExperimentTags request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in AddExperimentTags request";
      } else if (request.getTagsList().isEmpty()) {
        errorMessage = "Experiment tags not found in AddExperimentTags request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      var updatedExperiment =
          experimentDAO.addExperimentTags(
              request.getId(), ModelDBUtils.checkEntityTagsLength(request.getTagsList()));
      var response =
          AddExperimentTags.Response.newBuilder().setExperiment(updatedExperiment).build();

      // Add succeeded event in local DB
      addEvent(
          updatedExperiment.getId(),
          updatedExperiment.getProjectId(),
          authService.getWorkspaceIdFromUserInfo(authService.getCurrentLoginUserInfo()),
          UPDATE_EVENT_TYPE,
          Optional.of("tags"),
          Collections.singletonMap(
              "tags",
              new Gson()
                  .toJsonTree(
                      request.getTagsList(), new TypeToken<ArrayList<String>>() {}.getType())),
          "experiment tags added successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, AddExperimentTags.Response.getDefaultInstance());
    }
  }

  @Override
  public void addExperimentTag(
      AddExperimentTag request, StreamObserver<AddExperimentTag.Response> responseObserver) {

    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getTag().isEmpty()) {
        errorMessage = "Experiment ID and Experiment Tag not found in AddExperimentTag request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in AddExperimentTag request";
      } else if (request.getTag().isEmpty()) {
        errorMessage = "Experiment Tag not found in AddExperimentTag request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      var updatedExperiment =
          experimentDAO.addExperimentTags(
              request.getId(),
              ModelDBUtils.checkEntityTagsLength(Collections.singletonList(request.getTag())));
      var response =
          AddExperimentTag.Response.newBuilder().setExperiment(updatedExperiment).build();

      // Add succeeded event in local DB
      addEvent(
          updatedExperiment.getId(),
          updatedExperiment.getProjectId(),
          authService.getWorkspaceIdFromUserInfo(authService.getCurrentLoginUserInfo()),
          UPDATE_EVENT_TYPE,
          Optional.of("tags"),
          Collections.singletonMap(
              "tags",
              new Gson()
                  .toJsonTree(
                      Collections.singletonList(request.getTag()),
                      new TypeToken<ArrayList<String>>() {}.getType())),
          "experiment tag added successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, AddExperimentTag.Response.getDefaultInstance());
    }
  }

  @Override
  public void getExperimentTags(
      GetTags request, StreamObserver<GetTags.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        var errorMessage = "Experiment ID not found in GetTags request";
        throw new InvalidArgumentException(errorMessage);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<String> experimentTags = experimentDAO.getExperimentTags(request.getId());
      var response = GetTags.Response.newBuilder().addAllTags(experimentTags).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetTags.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteExperimentTags(
      DeleteExperimentTags request,
      StreamObserver<DeleteExperimentTags.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getTagsList().isEmpty() && !request.getDeleteAll()) {
        errorMessage =
            "Experiment ID and Experiment tags not found in DeleteExperimentTags request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in DeleteExperimentTags request";
      } else if (request.getTagsList().isEmpty() && !request.getDeleteAll()) {
        errorMessage = "Experiment tags not found in DeleteExperimentTags request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      var updatedExperiment =
          experimentDAO.deleteExperimentTags(
              request.getId(), request.getTagsList(), request.getDeleteAll());
      var response =
          DeleteExperimentTags.Response.newBuilder().setExperiment(updatedExperiment).build();
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
          updatedExperiment.getId(),
          updatedExperiment.getProjectId(),
          authService.getWorkspaceIdFromUserInfo(authService.getCurrentLoginUserInfo()),
          UPDATE_EVENT_TYPE,
          Optional.of("tags"),
          extraField,
          "experiment tags deleted successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteExperimentTags.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteExperimentTag(
      DeleteExperimentTag request, StreamObserver<DeleteExperimentTag.Response> responseObserver) {

    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getTag().isEmpty()) {
        errorMessage = "Experiment ID and Experiment tag not found in DeleteExperimentTag request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in DeleteExperimentTag request";
      } else if (request.getTag().isEmpty()) {
        errorMessage = "Experiment tag not found in DeleteExperimentTag request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      var updatedExperiment =
          experimentDAO.deleteExperimentTags(
              request.getId(), Collections.singletonList(request.getTag()), false);
      var response =
          DeleteExperimentTag.Response.newBuilder().setExperiment(updatedExperiment).build();

      // Add succeeded event in local DB
      addEvent(
          updatedExperiment.getId(),
          updatedExperiment.getProjectId(),
          authService.getWorkspaceIdFromUserInfo(authService.getCurrentLoginUserInfo()),
          UPDATE_EVENT_TYPE,
          Optional.of("tags"),
          Collections.singletonMap(
              "tags",
              new Gson()
                  .toJsonTree(
                      Collections.singletonList(request.getTag()),
                      new TypeToken<ArrayList<String>>() {}.getType())),
          "experiment tag deleted successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteExperimentTag.Response.getDefaultInstance());
    }
  }

  @Override
  public void addAttribute(
      AddAttributes request, StreamObserver<AddAttributes.Response> responseObserver) {
    try {

      if (request.getId().isEmpty()) {
        var errorMessage = "Experiment ID not found in AddAttributes request";
        throw new InvalidArgumentException(errorMessage);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      if (projectIdFromExperimentMap.size() == 0) {
        throw new PermissionDeniedException(
            ModelDBMessages.ACCESS_IS_DENIED_EXPERIMENT_NOT_FOUND_FOR_GIVEN_ID + request.getId());
      }
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      experimentDAO.addExperimentAttributes(
          request.getId(), Collections.singletonList(request.getAttribute()));
      var response = AddAttributes.Response.newBuilder().setStatus(true).build();

      // Add succeeded event in local DB
      addEvent(
          request.getId(),
          projectId,
          authService.getWorkspaceIdFromUserInfo(authService.getCurrentLoginUserInfo()),
          UPDATE_EVENT_TYPE,
          Optional.of("attributes"),
          Collections.singletonMap(
              "attribute_keys",
              new Gson()
                  .toJsonTree(
                      Stream.of(request.getAttribute())
                          .map(KeyValue::getKey)
                          .collect(Collectors.toSet()),
                      new TypeToken<ArrayList<String>>() {}.getType())),
          "experiment attribute added successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, AddAttributes.Response.getDefaultInstance());
    }
  }

  @Override
  public void addExperimentAttributes(
      AddExperimentAttributes request,
      StreamObserver<AddExperimentAttributes.Response> responseObserver) {
    try {

      String errorMessage = null;
      if (request.getId().isEmpty() && request.getAttributesList().isEmpty()) {
        errorMessage =
            "Experiment ID and Experiment Attributes not found in AddExperimentAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in AddExperimentAttributes request";
      } else if (request.getAttributesList().isEmpty()) {
        errorMessage = "Experiment Attributes not found in AddExperimentAttributes request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      var experiment =
          experimentDAO.addExperimentAttributes(request.getId(), request.getAttributesList());
      var response =
          AddExperimentAttributes.Response.newBuilder().setExperiment(experiment).build();

      // Add succeeded event in local DB
      addEvent(
          request.getId(),
          projectId,
          authService.getWorkspaceIdFromUserInfo(authService.getCurrentLoginUserInfo()),
          UPDATE_EVENT_TYPE,
          Optional.of("attributes"),
          Collections.singletonMap(
              "attribute_keys",
              new Gson()
                  .toJsonTree(
                      request.getAttributesList().stream()
                          .map(KeyValue::getKey)
                          .collect(Collectors.toSet()),
                      new TypeToken<ArrayList<String>>() {}.getType())),
          "experiment attributes added successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, AddExperimentAttributes.Response.getDefaultInstance());
    }
  }

  @Override
  public void getExperimentAttributes(
      GetAttributes request, StreamObserver<GetAttributes.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty()
          && request.getAttributeKeysList().isEmpty()
          && !request.getGetAll()) {
        errorMessage =
            "Experiment ID and Experiment Attribute keys not found in GetAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in GetAttributes request";
      } else if (request.getAttributeKeysList().isEmpty() && !request.getGetAll()) {
        errorMessage = "Experiment Attribute keys not found in GetAttributes request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      if (projectIdFromExperimentMap.size() == 0) {
        throw new PermissionDeniedException(
            ModelDBMessages.ACCESS_IS_DENIED_EXPERIMENT_NOT_FOUND_FOR_GIVEN_ID + request.getId());
      }
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<KeyValue> attributes =
          experimentDAO.getExperimentAttributes(
              request.getId(), request.getAttributeKeysList(), request.getGetAll());
      var response = GetAttributes.Response.newBuilder().addAllAttributes(attributes).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetAttributes.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteExperimentAttributes(
      DeleteExperimentAttributes request, StreamObserver<Response> responseObserver) {
    try {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty()
          && request.getAttributeKeysList().isEmpty()
          && !request.getDeleteAll()) {
        errorMessage =
            "Experiment ID and Experiment Attribute keys not found in DeleteExperimentAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in DeleteExperimentAttributes request";
      } else if (request.getAttributeKeysList().isEmpty() && !request.getDeleteAll()) {
        errorMessage = "Experiment Attribute keys not found in DeleteExperimentAttributes request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      var updatedExperiment =
          experimentDAO.deleteExperimentAttributes(
              request.getId(), request.getAttributeKeysList(), request.getDeleteAll());
      var response =
          DeleteExperimentAttributes.Response.newBuilder().setExperiment(updatedExperiment).build();

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
          updatedExperiment.getId(),
          updatedExperiment.getProjectId(),
          authService.getWorkspaceIdFromUserInfo(authService.getCurrentLoginUserInfo()),
          UPDATE_EVENT_TYPE,
          Optional.of("attributes"),
          extraField,
          "experiment attributes deleted successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteExperimentAttributes.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteExperiment(
      DeleteExperiment request, StreamObserver<DeleteExperiment.Response> responseObserver) {
    try {

      if (request.getId().isEmpty()) {
        var errorMessage = "Experiment ID not found in DeleteExperiment request";
        throw new InvalidArgumentException(errorMessage);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      if (projectIdFromExperimentMap.size() == 0) {
        throw new PermissionDeniedException(
            ModelDBMessages.ACCESS_IS_DENIED_EXPERIMENT_NOT_FOUND_FOR_GIVEN_ID + request.getId());
      }
      String projectId = projectIdFromExperimentMap.get(request.getId());

      List<String> deletedIds =
          experimentDAO.deleteExperiments(Collections.singletonList(request.getId()));
      var response =
          DeleteExperiment.Response.newBuilder().setStatus(!deletedIds.isEmpty()).build();

      // Add succeeded event in local DB
      addEvent(
          request.getId(),
          projectId,
          authService.getWorkspaceIdFromUserInfo(authService.getCurrentLoginUserInfo()),
          "delete.resource.experiment.delete_experiment_succeeded",
          Optional.empty(),
          Collections.emptyMap(),
          "experiment deleted successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, DeleteExperiment.Response.getDefaultInstance());
    }
  }

  @Override
  public void logExperimentCodeVersion(
      LogExperimentCodeVersion request,
      StreamObserver<LogExperimentCodeVersion.Response> responseObserver) {
    try {
      /*Parameter validation*/
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getCodeVersion() == null) {
        errorMessage =
            "Experiment ID and Code version not found in LogExperimentCodeVersion request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in LogExperimentCodeVersion request";
      } else if (request.getCodeVersion() == null) {
        errorMessage = "CodeVersion not found in LogExperimentCodeVersion request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      /*User validation*/
      var existingExperiment = experimentDAO.getExperiment(request.getId());
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT,
          existingExperiment.getProjectId(),
          ModelDBServiceActions.UPDATE);

      /*UpdateCode version*/
      Experiment updatedExperiment;

      if (!existingExperiment.getCodeVersionSnapshot().hasCodeArchive()
          && !existingExperiment.getCodeVersionSnapshot().hasGitSnapshot()) {
        updatedExperiment =
            experimentDAO.logExperimentCodeVersion(request.getId(), request.getCodeVersion());
      } else {
        errorMessage = "Code version already logged for experiment " + existingExperiment.getId();
        throw new AlreadyExistsException(errorMessage);
      }
      var response =
          LogExperimentCodeVersion.Response.newBuilder().setExperiment(updatedExperiment).build();

      // Add succeeded event in local DB
      addEvent(
          updatedExperiment.getId(),
          updatedExperiment.getProjectId(),
          authService.getWorkspaceIdFromUserInfo(authService.getCurrentLoginUserInfo()),
          UPDATE_EVENT_TYPE,
          Optional.of("code_version"),
          Collections.emptyMap(),
          "experiment code_version updated successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, LogExperimentCodeVersion.Response.getDefaultInstance());
    }
  }

  @Override
  public void getExperimentCodeVersion(
      GetExperimentCodeVersion request,
      StreamObserver<GetExperimentCodeVersion.Response> responseObserver) {
    try {
      /*Parameter validation*/
      if (request.getId().isEmpty()) {
        var errorMessage = "Experiment ID not found in GetExperimentCodeVersion request";
        throw new InvalidArgumentException(errorMessage);
      }

      /*User validation*/
      var existingExperiment = experimentDAO.getExperiment(request.getId());
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT,
          existingExperiment.getProjectId(),
          ModelDBServiceActions.READ);

      /*Get code version*/
      var codeVersion = existingExperiment.getCodeVersionSnapshot();

      var response =
          GetExperimentCodeVersion.Response.newBuilder().setCodeVersion(codeVersion).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetExperimentCodeVersion.Response.getDefaultInstance());
    }
  }

  @Override
  public void findExperiments(
      FindExperiments request, StreamObserver<FindExperiments.Response> responseObserver) {
    try {

      if (!request.getProjectId().isEmpty()) {
        // Validate if current user has access to the entity or not
        mdbRoleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.PROJECT,
            request.getProjectId(),
            ModelDBServiceActions.READ);
      }

      var userInfo = authService.getCurrentLoginUserInfo();
      var experimentPaginationDTO = experimentDAO.findExperiments(projectDAO, userInfo, request);
      List<Experiment> experiments = experimentPaginationDTO.getExperiments();
      var response =
          FindExperiments.Response.newBuilder()
              .addAllExperiments(experiments)
              .setTotalRecords(experimentPaginationDTO.getTotalRecords())
              .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, FindExperiments.Response.getDefaultInstance());
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
        errorMessage = "Experiment ID and Key and Method not found in GetUrlForArtifact request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in GetUrlForArtifact request";
      } else if (request.getKey().isEmpty()) {
        errorMessage = "Artifact Key not found in GetUrlForArtifact request";
      } else if (request.getMethod().isEmpty()) {
        errorMessage = "Method is not found in GetUrlForArtifact request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      if (projectIdFromExperimentMap.size() == 0) {
        throw new NotFoundException(
            "Experiment '" + request.getId() + "' is not associated with any project");
      }

      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      String s3Key = null;

      /*Process code*/
      if (request.getArtifactType() == ArtifactType.CODE) {
        // just creating the error string
        errorMessage = "Code versioning artifact not found at experiment and project level";
        s3Key = getUrlForCode(request);
      } else {
        errorMessage = "Experiment level artifacts only supported for code";
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
    var expr = experimentDAO.getExperiment(request.getId());
    if (expr.getCodeVersionSnapshot() != null
        && expr.getCodeVersionSnapshot().getCodeArchive() != null) {
      s3Key = expr.getCodeVersionSnapshot().getCodeArchive().getPath();
    } else {
      var proj = projectDAO.getProjectByID(expr.getProjectId());
      if (proj.getCodeVersionSnapshot() != null
          && proj.getCodeVersionSnapshot().getCodeArchive() != null) {
        s3Key = proj.getCodeVersionSnapshot().getCodeArchive().getPath();
      }
    }
    return s3Key;
  }

  @Override
  public void logArtifacts(
      LogExperimentArtifacts request,
      StreamObserver<LogExperimentArtifacts.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getArtifactsList().isEmpty()) {
        errorMessage = "Experiment ID and Artifacts not found in LogArtifacts request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in LogArtifacts request";
      } else if (request.getArtifactsList().isEmpty()) {
        errorMessage = "Artifacts not found in LogArtifacts request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      if (projectIdFromExperimentMap.size() == 0) {
        throw new PermissionDeniedException(
            ModelDBMessages.ACCESS_IS_DENIED_EXPERIMENT_NOT_FOUND_FOR_GIVEN_ID + request.getId());
      }

      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      List<Artifact> artifactList =
          ModelDBUtils.getArtifactsWithUpdatedPath(request.getId(), request.getArtifactsList());

      var updatedExperiment = experimentDAO.logArtifacts(request.getId(), artifactList);
      var response =
          LogExperimentArtifacts.Response.newBuilder().setExperiment(updatedExperiment).build();

      // Add succeeded event in local DB
      addEvent(
          request.getId(),
          projectId,
          authService.getWorkspaceIdFromUserInfo(authService.getCurrentLoginUserInfo()),
          UPDATE_EVENT_TYPE,
          Optional.of("artifacts"),
          Collections.singletonMap(
              "artifact_keys",
              new Gson()
                  .toJsonTree(
                      request.getArtifactsList().stream()
                          .map(Artifact::getKey)
                          .collect(Collectors.toSet()),
                      new TypeToken<ArrayList<String>>() {}.getType())),
          "experiment artifacts added successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, LogExperimentArtifacts.Response.getDefaultInstance());
    }
  }

  @Override
  public void getArtifacts(
      GetArtifacts request, StreamObserver<GetArtifacts.Response> responseObserver) {
    try {

      if (request.getId().isEmpty()) {
        var errorMessage = "Experiment ID not found in GetArtifacts request";
        throw new InvalidArgumentException(errorMessage);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      if (projectIdFromExperimentMap.size() == 0) {
        throw new PermissionDeniedException(
            ModelDBMessages.ACCESS_IS_DENIED_EXPERIMENT_NOT_FOUND_FOR_GIVEN_ID + request.getId());
      }
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<Artifact> artifactList = experimentDAO.getExperimentArtifacts(request.getId());
      var response = GetArtifacts.Response.newBuilder().addAllArtifacts(artifactList).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetArtifacts.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteArtifact(
      DeleteExperimentArtifact request,
      StreamObserver<DeleteExperimentArtifact.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getKey().isEmpty()) {
        errorMessage = "Experiment ID and Artifact key not found in DeleteArtifact request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Experiment ID not found in DeleteArtifact request";
      } else if (request.getKey().isEmpty()) {
        errorMessage = "Artifact key not found in DeleteArtifact request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      mdbRoleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      var updatedExperiment = experimentDAO.deleteArtifacts(request.getId(), request.getKey());
      var response =
          DeleteExperimentArtifact.Response.newBuilder().setExperiment(updatedExperiment).build();

      // Add succeeded event in local DB
      addEvent(
          updatedExperiment.getId(),
          updatedExperiment.getProjectId(),
          authService.getWorkspaceIdFromUserInfo(authService.getCurrentLoginUserInfo()),
          UPDATE_EVENT_TYPE,
          Optional.of("artifacts"),
          Collections.singletonMap(
              "artifact_keys",
              new Gson()
                  .toJsonTree(
                      Collections.singletonList(request.getKey()),
                      new TypeToken<ArrayList<String>>() {}.getType())),
          "experiment artifact deleted successfully");

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteExperimentArtifact.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteExperiments(
      DeleteExperiments request, StreamObserver<DeleteExperiments.Response> responseObserver) {
    try {
      if (request.getIdsList().isEmpty()) {
        throw new InvalidArgumentException("Experiment IDs not found in DeleteExperiments request");
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(request.getIdsList());
      List<GetResourcesResponseItem> entityResources =
          mdbRoleService.getResourceItems(
              null,
              new HashSet<>(projectIdFromExperimentMap.values()),
              ModelDBServiceResourceTypes.PROJECT,
              false);
      List<String> deletedIds = experimentDAO.deleteExperiments(request.getIdsList());
      var response =
          DeleteExperiments.Response.newBuilder().setStatus(!deletedIds.isEmpty()).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteExperiments.Response.getDefaultInstance());
    }
  }
}
