package ai.verta.modeldb.experiment;

import ai.verta.common.Artifact;
import ai.verta.common.ArtifactTypeEnum.ArtifactType;
import ai.verta.common.KeyValue;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.*;
import ai.verta.modeldb.DeleteExperimentAttributes.Response;
import ai.verta.modeldb.ExperimentServiceGrpc.ExperimentServiceImplBase;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.audit_log.AuditLogLocalDAO;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.entities.audit_log.AuditLogLocalEntity;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.dto.ExperimentPaginationDTO;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.exceptions.PermissionDeniedException;
import ai.verta.modeldb.metadata.MetadataServiceImpl;
import ai.verta.modeldb.monitoring.MonitoringInterceptor;
import ai.verta.modeldb.project.ProjectDAO;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ServiceEnum.Service;
import ai.verta.uac.UserInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class ExperimentServiceImpl extends ExperimentServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(ExperimentServiceImpl.class);
  private final AuthService authService;
  private final RoleService roleService;
  private final ExperimentDAO experimentDAO;
  private final ProjectDAO projectDAO;
  private final ArtifactStoreDAO artifactStoreDAO;
  private final AuditLogLocalDAO auditLogLocalDAO;
  private static final String SERVICE_NAME =
      String.format("%s.%s", ModelDBConstants.SERVICE_NAME, ModelDBConstants.EXPERIMENT);

  public ExperimentServiceImpl(ServiceSet serviceSet, DAOSet daoSet) {
    this.authService = serviceSet.authService;
    this.roleService = serviceSet.roleService;
    this.experimentDAO = daoSet.experimentDAO;
    this.projectDAO = daoSet.projectDAO;
    this.artifactStoreDAO = daoSet.artifactStoreDAO;
    this.auditLogLocalDAO = daoSet.auditLogLocalDAO;
  }

  private void saveAuditLog(
      Optional<UserInfo> userInfo,
      ModelDBServiceActions action,
      Map<String, Long> resourceIdWorkspaceIdMap,
      String request,
      String response,
      Long workspaceId) {
    auditLogLocalDAO.saveAuditLog(
        new AuditLogLocalEntity(
            SERVICE_NAME,
            authService.getVertaIdFromUserInfo(
                userInfo.orElseGet(authService::getCurrentLoginUserInfo)),
            action,
            resourceIdWorkspaceIdMap,
            ModelDBServiceResourceTypes.EXPERIMENT,
            Service.MODELDB_SERVICE,
            MonitoringInterceptor.METHOD_NAME.get(),
            request,
            response,
            workspaceId));
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
    Experiment.Builder experimentBuilder =
        Experiment.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setProjectId(request.getProjectId())
            .setName(ModelDBUtils.checkEntityNameLength(request.getName()))
            .setDescription(request.getDescription())
            .addAllAttributes(request.getAttributesList())
            .addAllTags(ModelDBUtils.checkEntityTagsLength(request.getTagsList()))
            .addAllArtifacts(request.getArtifactsList());

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
  public synchronized void createExperiment(
      CreateExperiment request, StreamObserver<CreateExperiment.Response> responseObserver) {
    try {

      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();

      Experiment experiment = getExperimentFromRequest(request, userInfo);

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT,
          experiment.getProjectId(),
          ModelDBServiceActions.UPDATE);

      experiment = experimentDAO.insertExperiment(experiment, userInfo);
      CreateExperiment.Response response =
          CreateExperiment.Response.newBuilder().setExperiment(experiment).build();

      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(
              experiment.getProjectId(), ModelDBServiceResourceTypes.PROJECT);
      saveAuditLog(
          Optional.of(userInfo),
          ModelDBServiceActions.CREATE,
          Collections.singletonMap(experiment.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, CreateExperiment.Response.getDefaultInstance());
    }
  }

  @Override
  public synchronized void getExperimentsInProject(
      GetExperimentsInProject request,
      StreamObserver<GetExperimentsInProject.Response> responseObserver) {
    try {

      if (request.getProjectId().isEmpty()) {
        String errorMessage = "Project ID not found in GetExperimentsInProject request";
        throw new InvalidArgumentException(errorMessage);
      }

      if (!projectDAO.projectExistsInDB(request.getProjectId())) {
        String errorMessage = "Project ID not found.";
        throw new NotFoundException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getProjectId(), ModelDBServiceActions.READ);

      ExperimentPaginationDTO experimentPaginationDTO =
          experimentDAO.getExperimentsInProject(
              projectDAO,
              request.getProjectId(),
              request.getPageNumber(),
              request.getPageLimit(),
              request.getAscending(),
              request.getSortKey());
      List<Experiment> experiments = experimentPaginationDTO.getExperiments();
      GetExperimentsInProject.Response response =
          GetExperimentsInProject.Response.newBuilder()
              .addAllExperiments(experiments)
              .setTotalRecords(experimentPaginationDTO.getTotalRecords())
              .build();
      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(
              request.getProjectId(), ModelDBServiceResourceTypes.PROJECT);
      Map<String, Long> auditResourceMap = new HashMap<>();
      experiments.forEach(
          experiment -> auditResourceMap.put(experiment.getId(), entityResource.getWorkspaceId()));
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.READ,
          auditResourceMap,
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetExperimentsInProject.Response.getDefaultInstance());
    }
  }

  @Override
  public synchronized void getExperimentById(
      GetExperimentById request, StreamObserver<GetExperimentById.Response> responseObserver) {
    try {

      if (request.getId().isEmpty()) {
        String errorMessage = "Experiment ID not found in GetExperimentById request";
        throw new InvalidArgumentException(errorMessage);
      }

      Experiment experiment = experimentDAO.getExperiment(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT,
          experiment.getProjectId(),
          ModelDBServiceActions.READ);

      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(
              experiment.getProjectId(), ModelDBServiceResourceTypes.PROJECT);
      GetExperimentById.Response response =
          GetExperimentById.Response.newBuilder().setExperiment(experiment).build();
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.READ,
          Collections.singletonMap(experiment.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetExperimentById.Response.getDefaultInstance());
    }
  }

  @Override
  public synchronized void getExperimentByName(
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
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getProjectId(), ModelDBServiceActions.READ);

      List<KeyValue> keyValue = new ArrayList<>();
      Value projectIdValue = Value.newBuilder().setStringValue(request.getProjectId()).build();
      Value nameValue = Value.newBuilder().setStringValue(request.getName()).build();
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
      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(
              request.getProjectId(), ModelDBServiceResourceTypes.PROJECT);
      GetExperimentByName.Response response =
          GetExperimentByName.Response.newBuilder().setExperiment(experiments.get(0)).build();
      Map<String, Long> auditResourceMap = new HashMap<>();
      experiments.forEach(
          experiment -> auditResourceMap.put(experiment.getId(), entityResource.getWorkspaceId()));
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.READ,
          auditResourceMap,
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
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
  public synchronized void updateExperimentNameOrDescription(
      UpdateExperimentNameOrDescription request,
      StreamObserver<UpdateExperimentNameOrDescription.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        String errorMessage =
            "Experiment ID not found in UpdateExperimentNameOrDescription request";
        throw new InvalidArgumentException(errorMessage);
      } else if (request.getName().isEmpty()) {
        request = request.toBuilder().setName(MetadataServiceImpl.createRandomName()).build();
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment updatedExperiment = null;
      if (!request.getName().isEmpty()) {
        updatedExperiment =
            experimentDAO.updateExperimentName(
                request.getId(), ModelDBUtils.checkEntityNameLength(request.getName()));
      }
      // FIXME: this code never allows us to set the description as an empty string
      if (!request.getDescription().isEmpty()) {
        updatedExperiment =
            experimentDAO.updateExperimentDescription(request.getId(), request.getDescription());
      }

      UpdateExperimentNameOrDescription.Response response =
          UpdateExperimentNameOrDescription.Response.newBuilder()
              .setExperiment(updatedExperiment)
              .build();

      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(projectId, ModelDBServiceResourceTypes.PROJECT);
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.UPDATE,
          Collections.singletonMap(request.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
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
  public synchronized void updateExperimentName(
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
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment updatedExperiment =
          experimentDAO.updateExperimentName(
              request.getId(), ModelDBUtils.checkEntityNameLength(request.getName()));

      UpdateExperimentName.Response response =
          UpdateExperimentName.Response.newBuilder().setExperiment(updatedExperiment).build();

      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(projectId, ModelDBServiceResourceTypes.PROJECT);
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.UPDATE,
          Collections.singletonMap(updatedExperiment.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
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
  public synchronized void updateExperimentDescription(
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
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment updatedExperiment =
          experimentDAO.updateExperimentDescription(request.getId(), request.getDescription());
      UpdateExperimentDescription.Response response =
          UpdateExperimentDescription.Response.newBuilder()
              .setExperiment(updatedExperiment)
              .build();

      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(projectId, ModelDBServiceResourceTypes.PROJECT);
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.UPDATE,
          Collections.singletonMap(updatedExperiment.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, UpdateExperimentDescription.Response.getDefaultInstance());
    }
  }

  @Override
  public synchronized void addExperimentTags(
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
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment updatedExperiment =
          experimentDAO.addExperimentTags(
              request.getId(), ModelDBUtils.checkEntityTagsLength(request.getTagsList()));
      AddExperimentTags.Response response =
          AddExperimentTags.Response.newBuilder().setExperiment(updatedExperiment).build();

      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(projectId, ModelDBServiceResourceTypes.PROJECT);
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.UPDATE,
          Collections.singletonMap(updatedExperiment.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, AddExperimentTags.Response.getDefaultInstance());
    }
  }

  @Override
  public synchronized void addExperimentTag(
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
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment updatedExperiment =
          experimentDAO.addExperimentTags(
              request.getId(),
              ModelDBUtils.checkEntityTagsLength(Collections.singletonList(request.getTag())));
      AddExperimentTag.Response response =
          AddExperimentTag.Response.newBuilder().setExperiment(updatedExperiment).build();

      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(projectId, ModelDBServiceResourceTypes.PROJECT);
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.UPDATE,
          Collections.singletonMap(updatedExperiment.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, AddExperimentTag.Response.getDefaultInstance());
    }
  }

  @Override
  public synchronized void getExperimentTags(
      GetTags request, StreamObserver<GetTags.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        String errorMessage = "Experiment ID not found in GetTags request";
        throw new InvalidArgumentException(errorMessage);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<String> experimentTags = experimentDAO.getExperimentTags(request.getId());
      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(projectId, ModelDBServiceResourceTypes.PROJECT);
      GetTags.Response response = GetTags.Response.newBuilder().addAllTags(experimentTags).build();
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.READ,
          Collections.singletonMap(request.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetTags.Response.getDefaultInstance());
    }
  }

  @Override
  public synchronized void deleteExperimentTags(
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
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment updatedExperiment =
          experimentDAO.deleteExperimentTags(
              request.getId(), request.getTagsList(), request.getDeleteAll());
      DeleteExperimentTags.Response response =
          DeleteExperimentTags.Response.newBuilder().setExperiment(updatedExperiment).build();

      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(projectId, ModelDBServiceResourceTypes.PROJECT);
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.UPDATE,
          Collections.singletonMap(updatedExperiment.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteExperimentTags.Response.getDefaultInstance());
    }
  }

  @Override
  public synchronized void deleteExperimentTag(
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
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment updatedExperiment =
          experimentDAO.deleteExperimentTags(
              request.getId(), Collections.singletonList(request.getTag()), false);
      DeleteExperimentTag.Response response =
          DeleteExperimentTag.Response.newBuilder().setExperiment(updatedExperiment).build();

      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(projectId, ModelDBServiceResourceTypes.PROJECT);
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.UPDATE,
          Collections.singletonMap(updatedExperiment.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteExperimentTag.Response.getDefaultInstance());
    }
  }

  @Override
  public synchronized void addAttribute(
      AddAttributes request, StreamObserver<AddAttributes.Response> responseObserver) {
    try {

      if (request.getId().isEmpty()) {
        String errorMessage = "Experiment ID not found in AddAttributes request";
        throw new InvalidArgumentException(errorMessage);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      if (projectIdFromExperimentMap.size() == 0) {
        throw new PermissionDeniedException(
            "Access is denied. Experiment not found for given id : " + request.getId());
      }
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      experimentDAO.addExperimentAttributes(
          request.getId(), Collections.singletonList(request.getAttribute()));
      AddAttributes.Response response = AddAttributes.Response.newBuilder().setStatus(true).build();

      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(projectId, ModelDBServiceResourceTypes.PROJECT);
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.UPDATE,
          Collections.singletonMap(request.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, AddAttributes.Response.getDefaultInstance());
    }
  }

  @Override
  public synchronized void addExperimentAttributes(
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
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment experiment =
          experimentDAO.addExperimentAttributes(request.getId(), request.getAttributesList());
      AddExperimentAttributes.Response response =
          AddExperimentAttributes.Response.newBuilder().setExperiment(experiment).build();

      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(projectId, ModelDBServiceResourceTypes.PROJECT);
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.UPDATE,
          Collections.singletonMap(experiment.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, AddExperimentAttributes.Response.getDefaultInstance());
    }
  }

  @Override
  public synchronized void getExperimentAttributes(
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
            "Access is denied. Experiment not found for given id : " + request.getId());
      }
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<KeyValue> attributes =
          experimentDAO.getExperimentAttributes(
              request.getId(), request.getAttributeKeysList(), request.getGetAll());
      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(projectId, ModelDBServiceResourceTypes.PROJECT);
      GetAttributes.Response response =
          GetAttributes.Response.newBuilder().addAllAttributes(attributes).build();
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.READ,
          Collections.singletonMap(request.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetAttributes.Response.getDefaultInstance());
    }
  }

  @Override
  public synchronized void deleteExperimentAttributes(
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
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment updatedExperiment =
          experimentDAO.deleteExperimentAttributes(
              request.getId(), request.getAttributeKeysList(), request.getDeleteAll());
      DeleteExperimentAttributes.Response response =
          DeleteExperimentAttributes.Response.newBuilder().setExperiment(updatedExperiment).build();

      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(projectId, ModelDBServiceResourceTypes.PROJECT);
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.UPDATE,
          Collections.singletonMap(updatedExperiment.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteExperimentAttributes.Response.getDefaultInstance());
    }
  }

  @Override
  public synchronized void deleteExperiment(
      DeleteExperiment request, StreamObserver<DeleteExperiment.Response> responseObserver) {
    try {

      if (request.getId().isEmpty()) {
        String errorMessage = "Experiment ID not found in DeleteExperiment request";
        throw new InvalidArgumentException(errorMessage);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      if (projectIdFromExperimentMap.size() == 0) {
        throw new PermissionDeniedException(
            "Access is denied. Experiment not found for given id : " + request.getId());
      }
      String projectId = projectIdFromExperimentMap.get(request.getId());

      List<String> deletedIds =
          experimentDAO.deleteExperiments(Collections.singletonList(request.getId()));
      DeleteExperiment.Response response =
          DeleteExperiment.Response.newBuilder().setStatus(!deletedIds.isEmpty()).build();

      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(projectId, ModelDBServiceResourceTypes.PROJECT);
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.DELETE,
          Collections.singletonMap(request.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, DeleteExperiment.Response.getDefaultInstance());
    }
  }

  @Override
  public synchronized void logExperimentCodeVersion(
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
      Experiment existingExperiment = experimentDAO.getExperiment(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
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
      LogExperimentCodeVersion.Response response =
          LogExperimentCodeVersion.Response.newBuilder().setExperiment(updatedExperiment).build();

      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(
              existingExperiment.getProjectId(), ModelDBServiceResourceTypes.PROJECT);
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.UPDATE,
          Collections.singletonMap(request.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, LogExperimentCodeVersion.Response.getDefaultInstance());
    }
  }

  @Override
  public synchronized void getExperimentCodeVersion(
      GetExperimentCodeVersion request,
      StreamObserver<GetExperimentCodeVersion.Response> responseObserver) {
    try {
      /*Parameter validation*/
      if (request.getId().isEmpty()) {
        String errorMessage = "Experiment ID not found in GetExperimentCodeVersion request";
        throw new InvalidArgumentException(errorMessage);
      }

      /*User validation*/
      Experiment existingExperiment = experimentDAO.getExperiment(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT,
          existingExperiment.getProjectId(),
          ModelDBServiceActions.READ);

      /*Get code version*/
      CodeVersion codeVersion = existingExperiment.getCodeVersionSnapshot();

      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(
              existingExperiment.getProjectId(), ModelDBServiceResourceTypes.PROJECT);
      GetExperimentCodeVersion.Response response =
          GetExperimentCodeVersion.Response.newBuilder().setCodeVersion(codeVersion).build();
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.READ,
          Collections.singletonMap(existingExperiment.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetExperimentCodeVersion.Response.getDefaultInstance());
    }
  }

  @Override
  public synchronized void findExperiments(
      FindExperiments request, StreamObserver<FindExperiments.Response> responseObserver) {
    try {

      if (!request.getProjectId().isEmpty()) {
        // Validate if current user has access to the entity or not
        roleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.PROJECT,
            request.getProjectId(),
            ModelDBServiceActions.READ);
      }

      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      ExperimentPaginationDTO experimentPaginationDTO =
          experimentDAO.findExperiments(projectDAO, userInfo, request);
      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(
              request.getProjectId(), ModelDBServiceResourceTypes.PROJECT);
      List<Experiment> experiments = experimentPaginationDTO.getExperiments();
      FindExperiments.Response response =
          FindExperiments.Response.newBuilder()
              .addAllExperiments(experiments)
              .setTotalRecords(experimentPaginationDTO.getTotalRecords())
              .build();

      Map<String, Long> auditResourceMap = new HashMap<>();
      experiments.forEach(
          experiment -> auditResourceMap.put(experiment.getId(), entityResource.getWorkspaceId()));
      saveAuditLog(
          Optional.of(userInfo),
          ModelDBServiceActions.READ,
          auditResourceMap,
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, FindExperiments.Response.getDefaultInstance());
    }
  }

  @Override
  public synchronized void getUrlForArtifact(
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
      roleService.validateEntityUserWithUserInfo(
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
      GetUrlForArtifact.Response response =
          artifactStoreDAO.getUrlForArtifact(s3Key, request.getMethod());
      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(projectId, ModelDBServiceResourceTypes.PROJECT);
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.READ,
          Collections.singletonMap(request.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetUrlForArtifact.Response.getDefaultInstance());
    }
  }

  private String getUrlForCode(GetUrlForArtifact request)
      throws InvalidProtocolBufferException, ExecutionException, InterruptedException {
    String s3Key = null;
    Experiment expr = experimentDAO.getExperiment(request.getId());
    if (expr.getCodeVersionSnapshot() != null
        && expr.getCodeVersionSnapshot().getCodeArchive() != null) {
      s3Key = expr.getCodeVersionSnapshot().getCodeArchive().getPath();
    } else {
      Project proj = projectDAO.getProjectByID(expr.getProjectId());
      if (proj.getCodeVersionSnapshot() != null
          && proj.getCodeVersionSnapshot().getCodeArchive() != null) {
        s3Key = proj.getCodeVersionSnapshot().getCodeArchive().getPath();
      }
    }
    return s3Key;
  }

  @Override
  public synchronized void logArtifacts(
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
            "Access is denied. Experiment not found for given id : " + request.getId());
      }

      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      List<Artifact> artifactList =
          ModelDBUtils.getArtifactsWithUpdatedPath(request.getId(), request.getArtifactsList());

      Experiment updatedExperiment = experimentDAO.logArtifacts(request.getId(), artifactList);
      LogExperimentArtifacts.Response response =
          LogExperimentArtifacts.Response.newBuilder().setExperiment(updatedExperiment).build();

      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(projectId, ModelDBServiceResourceTypes.PROJECT);
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.UPDATE,
          Collections.singletonMap(request.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, LogExperimentArtifacts.Response.getDefaultInstance());
    }
  }

  @Override
  public synchronized void getArtifacts(
      GetArtifacts request, StreamObserver<GetArtifacts.Response> responseObserver) {
    try {

      if (request.getId().isEmpty()) {
        String errorMessage = "Experiment ID not found in GetArtifacts request";
        throw new InvalidArgumentException(errorMessage);
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(Collections.singletonList(request.getId()));
      if (projectIdFromExperimentMap.size() == 0) {
        throw new PermissionDeniedException(
            "Access is denied. Experiment not found for given id : " + request.getId());
      }
      String projectId = projectIdFromExperimentMap.get(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<Artifact> artifactList = experimentDAO.getExperimentArtifacts(request.getId());
      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(
              projectIdFromExperimentMap.get(request.getId()), ModelDBServiceResourceTypes.PROJECT);
      GetArtifacts.Response response =
          GetArtifacts.Response.newBuilder().addAllArtifacts(artifactList).build();
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.READ,
          Collections.singletonMap(request.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetArtifacts.Response.getDefaultInstance());
    }
  }

  @Override
  public synchronized void deleteArtifact(
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
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Experiment updatedExperiment =
          experimentDAO.deleteArtifacts(request.getId(), request.getKey());
      DeleteExperimentArtifact.Response response =
          DeleteExperimentArtifact.Response.newBuilder().setExperiment(updatedExperiment).build();

      GetResourcesResponseItem entityResource =
          roleService.getEntityResource(projectId, ModelDBServiceResourceTypes.PROJECT);
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.UPDATE,
          Collections.singletonMap(request.getId(), entityResource.getWorkspaceId()),
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResource.getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteExperimentArtifact.Response.getDefaultInstance());
    }
  }

  @Override
  public synchronized void deleteExperiments(
      DeleteExperiments request, StreamObserver<DeleteExperiments.Response> responseObserver) {
    try {
      if (request.getIdsList().isEmpty()) {
        throw new InvalidArgumentException("Experiment IDs not found in DeleteExperiments request");
      }

      Map<String, String> projectIdFromExperimentMap =
          experimentDAO.getProjectIdsByExperimentIds(request.getIdsList());
      List<GetResourcesResponseItem> entityResources =
          roleService.getResourceItems(
              null,
              new HashSet<>(projectIdFromExperimentMap.values()),
              ModelDBServiceResourceTypes.PROJECT);
      List<String> deletedIds = experimentDAO.deleteExperiments(request.getIdsList());
      DeleteExperiments.Response response =
          DeleteExperiments.Response.newBuilder().setStatus(!deletedIds.isEmpty()).build();

      Map<String, Long> projectWorkspaceMap = new HashMap<>();
      entityResources.forEach(
          responseItem ->
              projectWorkspaceMap.put(responseItem.getResourceId(), responseItem.getWorkspaceId()));
      Map<String, Long> auditResourceMap = new HashMap<>();
      deletedIds.forEach(
          experimentId -> {
            String projectId = projectIdFromExperimentMap.get(experimentId);
            auditResourceMap.put(experimentId, projectWorkspaceMap.get(projectId));
          });
      saveAuditLog(
          Optional.empty(),
          ModelDBServiceActions.DELETE,
          auditResourceMap,
          ModelDBUtils.getStringFromProtoObject(request),
          ModelDBUtils.getStringFromProtoObject(response),
          entityResources.get(0).getWorkspaceId());
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteExperiments.Response.getDefaultInstance());
    }
  }
}
