package ai.verta.modeldb.experimentRun;

import ai.verta.common.Artifact;
import ai.verta.common.ArtifactTypeEnum.ArtifactType;
import ai.verta.common.KeyValue;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.*;
import ai.verta.modeldb.ExperimentRunServiceGrpc.ExperimentRunServiceImplBase;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.audit_log.AuditLogLocalDAO;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAO;
import ai.verta.modeldb.dto.ExperimentRunPaginationDTO;
import ai.verta.modeldb.entities.audit_log.AuditLogLocalEntity;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.exceptions.PermissionDeniedException;
import ai.verta.modeldb.experiment.ExperimentDAO;
import ai.verta.modeldb.metadata.MetadataServiceImpl;
import ai.verta.modeldb.project.ProjectDAO;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.CommitDAO;
import ai.verta.modeldb.versioning.RepositoryDAO;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ServiceEnum.Service;
import ai.verta.uac.UserInfo;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import io.grpc.stub.StreamObserver;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExperimentRunServiceImpl extends ExperimentRunServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(ExperimentRunServiceImpl.class);
  private final AuthService authService;
  private final RoleService roleService;
  private final ExperimentRunDAO experimentRunDAO;
  private final ProjectDAO projectDAO;
  private final ExperimentDAO experimentDAO;
  private final ArtifactStoreDAO artifactStoreDAO;
  private final DatasetVersionDAO datasetVersionDAO;
  private final RepositoryDAO repositoryDAO;
  private final CommitDAO commitDAO;
  private final AuditLogLocalDAO auditLogLocalDAO;
  private static final String SERVICE_NAME =
      String.format("%s.%s", ModelDBConstants.SERVICE_NAME, ModelDBConstants.EXPERIMENT_RUN);

  public ExperimentRunServiceImpl(ServiceSet serviceSet, DAOSet daoSet) {
    this.auditLogLocalDAO = daoSet.auditLogLocalDAO;
    this.authService = serviceSet.authService;
    this.roleService = serviceSet.roleService;
    this.experimentRunDAO = daoSet.experimentRunDAO;
    this.projectDAO = daoSet.projectDAO;
    this.experimentDAO = daoSet.experimentDAO;
    this.artifactStoreDAO = daoSet.artifactStoreDAO;
    this.datasetVersionDAO = daoSet.datasetVersionDAO;
    this.commitDAO = daoSet.commitDAO;
    this.repositoryDAO = daoSet.repositoryDAO;
  }

  private void saveAuditLogs(
      UserInfo userInfo, String action, List<String> resourceIds, String metadataBlob) {
    List<AuditLogLocalEntity> auditLogLocalEntities =
        resourceIds.stream()
            .map(
                resourceId ->
                    new AuditLogLocalEntity(
                        SERVICE_NAME,
                        authService.getVertaIdFromUserInfo(
                            userInfo == null ? authService.getCurrentLoginUserInfo() : userInfo),
                        action,
                        resourceId,
                        ModelDBConstants.EXPERIMENT_RUN,
                        Service.MODELDB_SERVICE.name(),
                        metadataBlob))
            .collect(Collectors.toList());
    if (!auditLogLocalEntities.isEmpty()) {
      auditLogLocalDAO.saveAuditLogs(auditLogLocalEntities);
    }
  }

  private void validateExperimentEntity(String experimentId) throws InvalidProtocolBufferException {
    experimentDAO.getExperiment(experimentId);
  }

  /**
   * Convert CreateExperimentRun request to Experiment object. This method generate the
   * ExperimentRun Id using UUID and put it in ExperimentRun object.
   *
   * @param request : CreateExperimentRun request
   * @param userInfo : current login UserInfo
   * @return ExperimentRun : experimentRun
   */
  private ExperimentRun getExperimentRunFromRequest(
      CreateExperimentRun request, UserInfo userInfo) {

    String errorMessage = null;
    if (request.getProjectId().isEmpty() && request.getExperimentId().isEmpty()) {
      errorMessage = "Project ID and Experiment ID not found in CreateExperimentRun request";
    } else if (request.getProjectId().isEmpty()) {
      errorMessage = "Project ID not found in CreateExperimentRun request";
    } else if (request.getExperimentId().isEmpty()) {
      errorMessage = "Experiment ID not found in CreateExperimentRun request";
    } else if (request.getName().isEmpty()) {
      request = request.toBuilder().setName(MetadataServiceImpl.createRandomName()).build();
    }

    if (errorMessage != null) {
      throw new InvalidArgumentException(errorMessage);
    }

    /*
     * Create ExperimentRun entity from given CreateExperimentRun request. generate UUID and put as
     * id in ExperimentRun for uniqueness.
     */
    ExperimentRun.Builder experimentRunBuilder =
        ExperimentRun.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setProjectId(request.getProjectId())
            .setExperimentId(request.getExperimentId())
            .setName(ModelDBUtils.checkEntityNameLength(request.getName()))
            .setDescription(request.getDescription())
            .setStartTime(request.getStartTime())
            .setEndTime(request.getEndTime())
            .setCodeVersion(request.getCodeVersion())
            .setParentId(request.getParentId())
            .addAllTags(ModelDBUtils.checkEntityTagsLength(request.getTagsList()))
            .addAllAttributes(request.getAttributesList())
            .addAllHyperparameters(request.getHyperparametersList())
            .addAllArtifacts(request.getArtifactsList())
            .addAllDatasets(request.getDatasetsList())
            .addAllMetrics(request.getMetricsList())
            .addAllObservations(request.getObservationsList())
            .addAllFeatures(request.getFeaturesList());

    if (request.getDateCreated() != 0L) {
      experimentRunBuilder
          .setDateCreated(request.getDateCreated())
          .setDateUpdated(request.getDateCreated());
    } else {
      experimentRunBuilder
          .setDateCreated(Calendar.getInstance().getTimeInMillis())
          .setDateUpdated(Calendar.getInstance().getTimeInMillis());
    }

    if (request.getCodeVersionSnapshot() != null) {
      experimentRunBuilder.setCodeVersionSnapshot(request.getCodeVersionSnapshot());
    }
    if (request.getVersionedInputs() != null && request.hasVersionedInputs()) {
      experimentRunBuilder.setVersionedInputs(request.getVersionedInputs());
    }
    if (userInfo != null) {
      experimentRunBuilder.setOwner(authService.getVertaIdFromUserInfo(userInfo));
    }

    return experimentRunBuilder.build();
  }

  /**
   * Convert CreateExperimentRun request to ExperimentRun entity and insert in database.
   *
   * @param request : CreateExperimentRun request
   * @param responseObserver : CreateExperimentRun.Response response
   */
  @Override
  public void createExperimentRun(
      CreateExperimentRun request, StreamObserver<CreateExperimentRun.Response> responseObserver) {
    try {

      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();

      ExperimentRun experimentRun = getExperimentRunFromRequest(request, userInfo);

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT,
          request.getProjectId(),
          ModelDBServiceActions.UPDATE);
      validateExperimentEntity(request.getExperimentId());

      experimentRun = experimentRunDAO.insertExperimentRun(projectDAO, experimentRun, userInfo);
      saveAuditLogs(
          userInfo, ModelDBConstants.CREATE, Collections.singletonList(experimentRun.getId()), "");
      responseObserver.onNext(
          CreateExperimentRun.Response.newBuilder().setExperimentRun(experimentRun).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, CreateExperimentRun.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteExperimentRun(
      DeleteExperimentRun request, StreamObserver<DeleteExperimentRun.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in DeleteExperimentRun request";
        throw new InvalidArgumentException(errorMessage);
      }

      List<String> deletedRunIds =
          experimentRunDAO.deleteExperimentRuns(Collections.singletonList(request.getId()));
      saveAuditLogs(null, ModelDBConstants.DELETE, deletedRunIds, "");
      responseObserver.onNext(
          DeleteExperimentRun.Response.newBuilder().setStatus(!deletedRunIds.isEmpty()).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteExperimentRun.Response.getDefaultInstance());
    }
  }

  @Override
  public void getExperimentRunsInProject(
      GetExperimentRunsInProject request,
      StreamObserver<GetExperimentRunsInProject.Response> responseObserver) {
    try {

      if (request.getProjectId().isEmpty()) {
        String errorMessage = "Project ID not found in GetExperimentRunsInProject request";
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getProjectId(), ModelDBServiceActions.READ);

      ExperimentRunPaginationDTO experimentRunPaginationDTO =
          experimentRunDAO.getExperimentRunsFromEntity(
              projectDAO,
              ModelDBConstants.PROJECT_ID,
              request.getProjectId(),
              request.getPageNumber(),
              request.getPageLimit(),
              request.getAscending(),
              request.getSortKey());
      responseObserver.onNext(
          GetExperimentRunsInProject.Response.newBuilder()
              .addAllExperimentRuns(experimentRunPaginationDTO.getExperimentRuns())
              .setTotalRecords(experimentRunPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetExperimentRunsInProject.Response.getDefaultInstance());
    }
  }

  @Override
  public void getExperimentRunsInExperiment(
      GetExperimentRunsInExperiment request,
      StreamObserver<GetExperimentRunsInExperiment.Response> responseObserver) {
    try {

      if (request.getExperimentId().isEmpty()) {
        String errorMessage = "Experiment ID not found in GetExperimentRunsInExperiment request";
        throw new InvalidArgumentException(errorMessage);
      }

      Map<String, String> projectIdsMap =
          experimentDAO.getProjectIdsByExperimentIds(
              Collections.singletonList(request.getExperimentId()));
      if (projectIdsMap.size() == 0) {
        throw new PermissionDeniedException(ModelDBConstants.ACCESS_DENIED_EXPERIMENT_RUN);
      }
      String projectId = projectIdsMap.get(request.getExperimentId());

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      ExperimentRunPaginationDTO experimentRunPaginationDTO =
          experimentRunDAO.getExperimentRunsFromEntity(
              projectDAO,
              ModelDBConstants.EXPERIMENT_ID,
              request.getExperimentId(),
              request.getPageNumber(),
              request.getPageLimit(),
              request.getAscending(),
              request.getSortKey());
      responseObserver.onNext(
          GetExperimentRunsInExperiment.Response.newBuilder()
              .addAllExperimentRuns(experimentRunPaginationDTO.getExperimentRuns())
              .setTotalRecords(experimentRunPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetExperimentRunsInExperiment.Response.getDefaultInstance());
    }
  }

  @Override
  public void getExperimentRunById(
      GetExperimentRunById request,
      StreamObserver<GetExperimentRunById.Response> responseObserver) {
    try {

      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in GetExperimentRunById request";
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      FindExperimentRuns findExperimentRuns =
          FindExperimentRuns.newBuilder().addExperimentRunIds(request.getId()).build();
      ExperimentRunPaginationDTO experimentRunPaginationDTO =
          experimentRunDAO.findExperimentRuns(
              projectDAO, authService.getCurrentLoginUserInfo(), findExperimentRuns);
      LOGGER.debug(
          ModelDBMessages.EXP_RUN_RECORD_COUNT_MSG, experimentRunPaginationDTO.getTotalRecords());
      GetExperimentRunById.Response.Builder response = GetExperimentRunById.Response.newBuilder();
      if (experimentRunPaginationDTO.getExperimentRuns() != null
          && !experimentRunPaginationDTO.getExperimentRuns().isEmpty()) {
        response.setExperimentRun(experimentRunPaginationDTO.getExperimentRuns().get(0));
      }
      responseObserver.onNext(response.build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetExperimentRunById.Response.getDefaultInstance());
    }
  }

  @Override
  public void getExperimentRunByName(
      GetExperimentRunByName request,
      StreamObserver<GetExperimentRunByName.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getName().isEmpty() && request.getExperimentId().isEmpty()) {
        errorMessage =
            "ExperimentRun name and Experiment ID not found in GetExperimentRunByName request";
      } else if (request.getName().isEmpty()) {
        errorMessage = "ExperimentRun name not found in GetExperimentRunByName request";
      } else if (request.getExperimentId().isEmpty()) {
        errorMessage = "Experiment ID not found in GetExperimentRunByName request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      Map<String, String> projectIdsMap =
          experimentDAO.getProjectIdsByExperimentIds(
              Collections.singletonList(request.getExperimentId()));
      String projectId = projectIdsMap.get(request.getExperimentId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<KeyValue> experimentRunFilter = new ArrayList<>();
      Value experimentIDValue =
          Value.newBuilder().setStringValue(request.getExperimentId()).build();
      experimentRunFilter.add(
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.EXPERIMENT_ID)
              .setValue(experimentIDValue)
              .build());
      Value experimentRunNameValue = Value.newBuilder().setStringValue(request.getName()).build();
      experimentRunFilter.add(
          KeyValue.newBuilder()
              .setKey(ModelDBConstants.NAME)
              .setValue(experimentRunNameValue)
              .build());

      List<ExperimentRun> experimentRunList =
          experimentRunDAO.getExperimentRuns(experimentRunFilter);
      if (experimentRunList.isEmpty()) {
        throw new NotFoundException("ExperimentRun not found in database");
      } else if (experimentRunList.size() != 1) {
        throw new InternalErrorException("Multiple ExperimentRun found in database");
      }

      responseObserver.onNext(
          GetExperimentRunByName.Response.newBuilder()
              .setExperimentRun(experimentRunList.get(0))
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetExperimentRunByName.Response.getDefaultInstance());
    }
  }

  @Override
  public void updateExperimentRunDescription(
      UpdateExperimentRunDescription request,
      StreamObserver<UpdateExperimentRunDescription.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        String errorMessage =
            "ExperimentRun ID not found in UpdateExperimentRunDescription request";
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.updateExperimentRunDescription(
              request.getId(), request.getDescription());
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(updatedExperimentRun.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "update",
              "description",
              request.getDescription()));
      responseObserver.onNext(
          UpdateExperimentRunDescription.Response.newBuilder()
              .setExperimentRun(updatedExperimentRun)
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, UpdateExperimentRunDescription.Response.getDefaultInstance());
    }
  }

  @Override
  public void updateExperimentRunName(
      UpdateExperimentRunName request,
      StreamObserver<UpdateExperimentRunName.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in UpdateExperimentRunName request";
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      experimentRunDAO.updateExperimentRunName(
          request.getId(), ModelDBUtils.checkEntityNameLength(request.getName()));

      auditLogLocalDAO.saveAuditLogs(
          Collections.singletonList(
              new AuditLogLocalEntity(
                  SERVICE_NAME,
                  authService.getVertaIdFromUserInfo(authService.getCurrentLoginUserInfo()),
                  ModelDBConstants.UPDATE,
                  request.getId(),
                  ModelDBConstants.EXPERIMENT_RUN,
                  Service.MODELDB_SERVICE.name(),
                  String.format(
                      ModelDBConstants.METADATA_JSON_TEMPLATE,
                      "update",
                      "name",
                      request.getName()))));
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE, "update", "name", request.getName()));
      responseObserver.onNext(UpdateExperimentRunName.Response.newBuilder().build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, UpdateExperimentRunName.Response.getDefaultInstance());
    }
  }

  @Override
  public void addExperimentRunTags(
      AddExperimentRunTags request,
      StreamObserver<AddExperimentRunTags.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getTagsList().isEmpty()) {
        errorMessage =
            "ExperimentRun ID and ExperimentRun tags not found in AddExperimentRunTags request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in AddExperimentRunTags request";
      } else if (request.getTagsList().isEmpty()) {
        errorMessage = "ExperimentRun tags not found in AddExperimentRunTags request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.addExperimentRunTags(
              request.getId(), ModelDBUtils.checkEntityTagsLength(request.getTagsList()));
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(updatedExperimentRun.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "add",
              "tags",
              new Gson().toJsonTree(request.getTagsList())));
      responseObserver.onNext(
          AddExperimentRunTags.Response.newBuilder()
              .setExperimentRun(updatedExperimentRun)
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, AddExperimentRunTags.Response.getDefaultInstance());
    }
  }

  @Override
  public void addExperimentRunTag(
      AddExperimentRunTag request, StreamObserver<AddExperimentRunTag.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getTag().isEmpty()) {
        errorMessage =
            "ExperimentRun ID and ExperimentRun Tag not found in AddExperimentRunTag request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in AddExperimentRunTag request";
      } else if (request.getTag().isEmpty()) {
        errorMessage = "ExperimentRun Tag not found in AddExperimentRunTag request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.addExperimentRunTags(
              request.getId(),
              ModelDBUtils.checkEntityTagsLength(Collections.singletonList(request.getTag())));
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(updatedExperimentRun.getId()),
          String.format(ModelDBConstants.METADATA_JSON_TEMPLATE, "add", "tag", request.getTag()));
      responseObserver.onNext(
          AddExperimentRunTag.Response.newBuilder().setExperimentRun(updatedExperimentRun).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, AddExperimentRunTag.Response.getDefaultInstance());
    }
  }

  @Override
  public void getExperimentRunTags(
      GetTags request, StreamObserver<GetTags.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in GetExperimentRunTags request";
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<String> experimentRunTags = experimentRunDAO.getExperimentRunTags(request.getId());
      responseObserver.onNext(GetTags.Response.newBuilder().addAllTags(experimentRunTags).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetTags.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteExperimentRunTags(
      DeleteExperimentRunTags request,
      StreamObserver<DeleteExperimentRunTags.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getTagsList().isEmpty() && !request.getDeleteAll()) {
        errorMessage =
            "ExperimentRun ID and ExperimentRun tags not found in DeleteExperimentRunTags request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in DeleteExperimentRunTags request";
      } else if (request.getTagsList().isEmpty() && !request.getDeleteAll()) {
        errorMessage = "ExperimentRun tags not found in DeleteExperimentRunTags request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.deleteExperimentRunTags(
              request.getId(), request.getTagsList(), request.getDeleteAll());
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(updatedExperimentRun.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "delete",
              "tags",
              request.getDeleteAll() ? "deleteAll" : new Gson().toJsonTree(request.getTagsList())));
      responseObserver.onNext(
          DeleteExperimentRunTags.Response.newBuilder()
              .setExperimentRun(updatedExperimentRun)
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteExperimentRunTags.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteExperimentRunTag(
      DeleteExperimentRunTag request,
      StreamObserver<DeleteExperimentRunTag.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getTag().isEmpty()) {
        errorMessage =
            "ExperimentRun ID and ExperimentRun tag not found in DeleteExperimentRunTag request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in DeleteExperimentRunTag request";
      } else if (request.getTag().isEmpty()) {
        errorMessage = "ExperimentRun tag not found in DeleteExperimentRunTag request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.deleteExperimentRunTags(
              request.getId(), Collections.singletonList(request.getTag()), false);
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(updatedExperimentRun.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE, "delete", "tag", request.getTag()));
      responseObserver.onNext(
          DeleteExperimentRunTag.Response.newBuilder()
              .setExperimentRun(updatedExperimentRun)
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteExperimentRunTag.Response.getDefaultInstance());
    }
  }

  @Override
  public void addExperimentRunAttributes(
      AddExperimentRunAttributes request,
      StreamObserver<AddExperimentRunAttributes.Response> responseObserver) {
    try {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getAttributesList().isEmpty()) {
        errorMessage =
            "ExperimentRun ID and ExperimentRun Attributes not found in AddExperimentRunAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in AddExperimentRunAttributes request";
      } else if (request.getAttributesList().isEmpty()) {
        errorMessage = "ExperimentRun Attributes not found in AddExperimentRunAttributes request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      experimentRunDAO.addExperimentRunAttributes(request.getId(), request.getAttributesList());
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "add",
              "attributes",
              new Gson().toJsonTree(request.getAttributesList())));
      responseObserver.onNext(AddExperimentRunAttributes.Response.newBuilder().build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, AddExperimentRunAttributes.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteExperimentRunAttributes(
      DeleteExperimentRunAttributes request,
      StreamObserver<DeleteExperimentRunAttributes.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty()
          && request.getAttributeKeysList().isEmpty()
          && !request.getDeleteAll()) {
        errorMessage =
            "ExperimentRun ID and ExperimentRun attributes not found in DeleteExperimentRunAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in DeleteExperimentRunAttributes request";
      } else if (request.getAttributeKeysList().isEmpty() && !request.getDeleteAll()) {
        errorMessage =
            "ExperimentRun attributes not found in DeleteExperimentRunAttributes request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      experimentRunDAO.deleteExperimentRunAttributes(
          request.getId(), request.getAttributeKeysList(), request.getDeleteAll());
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "delete",
              "attributes",
              request.getDeleteAll() ? "deleteAll" : request.getAttributeKeysList()));
      responseObserver.onNext(DeleteExperimentRunAttributes.Response.newBuilder().build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteExperimentRunAttributes.Response.getDefaultInstance());
    }
  }

  @Override
  public void logObservation(
      LogObservation request, StreamObserver<LogObservation.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty()
          && !request.getObservation().hasArtifact()
          && !request.getObservation().hasAttribute()) {
        errorMessage = "ExperimentRun ID and Observation not found in LogObservation request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogObservation request";
      } else if (!request.getObservation().hasArtifact()
          && !request.getObservation().hasAttribute()) {
        errorMessage = "Observation not found in LogObservation request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      experimentRunDAO.logObservations(
          request.getId(), Collections.singletonList(request.getObservation()));
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "add",
              "observation",
              new Gson().toJsonTree(request.getObservation())));
      responseObserver.onNext(LogObservation.Response.newBuilder().build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, LogObservation.Response.getDefaultInstance());
    }
  }

  @Override
  public void logObservations(
      LogObservations request, StreamObserver<LogObservations.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getObservationsList().isEmpty()) {
        errorMessage = "ExperimentRun ID and Observations not found in LogObservations request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogObservations request";
      } else if (request.getObservationsList().isEmpty()) {
        errorMessage = "Observations not found in LogObservations request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      experimentRunDAO.logObservations(request.getId(), request.getObservationsList());
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "add",
              "observations",
              new Gson().toJsonTree(request.getObservationsList())));
      responseObserver.onNext(LogObservations.Response.newBuilder().build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, LogObservations.Response.getDefaultInstance());
    }
  }

  @Override
  public void getObservations(
      GetObservations request, StreamObserver<GetObservations.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getObservationKey().isEmpty()) {
        errorMessage = "ExperimentRun ID and Observation key not found in GetObservations request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in GetObservations request";
      } else if (request.getObservationKey().isEmpty()) {
        errorMessage = "Observation key not found in GetObservations request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<Observation> observations =
          experimentRunDAO.getObservationByKey(request.getId(), request.getObservationKey());
      responseObserver.onNext(
          GetObservations.Response.newBuilder().addAllObservations(observations).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetObservations.Response.getDefaultInstance());
    }
  }

  @Override
  public void logMetric(LogMetric request, StreamObserver<LogMetric.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty()
          && (request.getMetric().getKey() == null || request.getMetric().getValue() == null)) {
        errorMessage =
            "ExperimentRun ID and New KeyValue for Metric not found in LogMetric request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogMetric request";
      } else if (request.getMetric().getKey() == null || request.getMetric().getValue() == null) {
        errorMessage = "New KeyValue for Metric not found in LogMetric request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      experimentRunDAO.logMetrics(request.getId(), Collections.singletonList(request.getMetric()));
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "add",
              "metric",
              new Gson().toJsonTree(request.getMetric())));
      responseObserver.onNext(LogMetric.Response.newBuilder().build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, LogMetric.Response.getDefaultInstance());
    }
  }

  @Override
  public void logMetrics(LogMetrics request, StreamObserver<LogMetrics.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getMetricsList().isEmpty()) {
        errorMessage = "ExperimentRun ID and New Metrics not found in LogMetrics request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogMetrics request";
      } else if (request.getMetricsList().isEmpty()) {
        errorMessage = "New Metrics not found in LogMetrics request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      experimentRunDAO.logMetrics(request.getId(), request.getMetricsList());
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "add",
              "metrics",
              new Gson().toJsonTree(request.getMetricsList())));
      responseObserver.onNext(LogMetrics.Response.newBuilder().build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, LogMetrics.Response.getDefaultInstance());
    }
  }

  @Override
  public void getMetrics(GetMetrics request, StreamObserver<GetMetrics.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in GetMetrics request";
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<KeyValue> metricList = experimentRunDAO.getExperimentRunMetrics(request.getId());
      responseObserver.onNext(GetMetrics.Response.newBuilder().addAllMetrics(metricList).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetMetrics.Response.getDefaultInstance());
    }
  }

  @Override
  public void getDatasets(
      GetDatasets request, StreamObserver<GetDatasets.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in GetDatasets request";
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<Artifact> datasetList = experimentRunDAO.getExperimentRunDatasets(request.getId());
      responseObserver.onNext(
          GetDatasets.Response.newBuilder().addAllDatasets(datasetList).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetDatasets.Response.getDefaultInstance());
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
        errorMessage = "ExperimentRun ID and Key and Method not found in GetUrlForArtifact request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in GetUrlForArtifact request";
      } else if (request.getKey().isEmpty()) {
        errorMessage = "Artifact Key not found in GetUrlForArtifact request";
      } else if (request.getMethod().isEmpty()) {
        errorMessage = "Method is not found in GetUrlForArtifact request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      final String s3Key;
      final String uploadId;

      /*Process code*/
      if (request.getArtifactType() == ArtifactType.CODE) {
        // just creating the error string
        errorMessage =
            "Code versioning artifact not found at experimentRun, experiment and project level";
        s3Key = getUrlForCode(request);
        uploadId = null;
      } else if (request.getArtifactType() == ArtifactType.DATA) {
        errorMessage = "Data versioning artifact not found";
        Entry<String, String> s3KeyUploadId = getUrlForData(request);
        s3Key = s3KeyUploadId.getKey();
        uploadId = s3KeyUploadId.getValue();
      } else {
        errorMessage =
            "ExperimentRun ID "
                + request.getId()
                + " does not have the artifact "
                + request.getKey();

        Entry<String, String> s3KeyUploadId =
            experimentRunDAO.getExperimentRunArtifactS3PathAndMultipartUploadID(
                request.getId(),
                request.getKey(),
                request.getPartNumber(),
                key -> artifactStoreDAO.initializeMultipart(key));
        s3Key = s3KeyUploadId.getKey();
        uploadId = s3KeyUploadId.getValue();
      }
      if (s3Key == null) {
        throw new NotFoundException(errorMessage);
      }
      GetUrlForArtifact.Response response =
          artifactStoreDAO.getUrlForArtifactMultipart(
              s3Key, request.getMethod(), request.getPartNumber(), uploadId);
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("key", s3Key);
      jsonObject.addProperty("method", request.getMethod());
      jsonObject.addProperty("part_number", request.getPartNumber());
      jsonObject.addProperty("upload_id", uploadId);
      saveAuditLogs(
          null,
          ModelDBConstants.GET,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "get",
              "getURLForArtifact",
              new Gson().toJson(jsonObject)));
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetUrlForArtifact.Response.getDefaultInstance());
    }
  }

  private Map.Entry<String, String> getUrlForData(GetUrlForArtifact request)
      throws InvalidProtocolBufferException, ModelDBException {

    assert (request.getArtifactType().equals(ArtifactType.DATA));
    assert (!request.getId().isEmpty());
    assert (!request.getKey().isEmpty());
    ExperimentRun exprRun = experimentRunDAO.getExperimentRun(request.getId());
    List<Artifact> datasets = exprRun.getDatasetsList();
    for (Artifact dataset : datasets) {
      if (dataset.getKey().equals(request.getKey()))
        return new SimpleEntry<>(
            datasetVersionDAO.getUrlForDatasetVersion(
                dataset.getLinkedArtifactId(), request.getMethod()),
            null);
    }
    // if the loop above did not return anything that means there was no Dataset logged with the
    // particular key
    // pre the dataset-as-fcc project datasets were stored as artifacts, so check there before
    // returning
    return experimentRunDAO.getExperimentRunArtifactS3PathAndMultipartUploadID(
        request.getId(),
        request.getKey(),
        request.getPartNumber(),
        s3Key -> artifactStoreDAO.initializeMultipart(s3Key));
  }

  private String getUrlForCode(GetUrlForArtifact request)
      throws InvalidProtocolBufferException, ModelDBException {
    ExperimentRun exprRun = experimentRunDAO.getExperimentRun(request.getId());
    String s3Key = null;
    /*If code version is not logged at a lower level we check for code at the higher level
     * We use the code version logged closest to the experiment run to generate the URL.*/
    if (exprRun.getCodeVersionSnapshot() != null
        && exprRun.getCodeVersionSnapshot().getCodeArchive() != null) {
      s3Key = exprRun.getCodeVersionSnapshot().getCodeArchive().getPath();
    } else {
      Experiment expr = experimentDAO.getExperiment(exprRun.getExperimentId());
      if (expr.getCodeVersionSnapshot() != null
          && expr.getCodeVersionSnapshot().getCodeArchive() != null) {
        s3Key = expr.getCodeVersionSnapshot().getCodeArchive().getPath();
      } else {
        Project proj = projectDAO.getProjectByID(exprRun.getProjectId());
        if (proj.getCodeVersionSnapshot() != null
            && proj.getCodeVersionSnapshot().getCodeArchive() != null) {
          s3Key = proj.getCodeVersionSnapshot().getCodeArchive().getPath();
        }
      }
    }
    return s3Key;
  }

  @Override
  public void logArtifact(
      LogArtifact request, StreamObserver<LogArtifact.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty()
          && request.getArtifact().getKey().isEmpty()
          && (request.getArtifact().getPathOnly() && request.getArtifact().getPath().isEmpty())) {
        errorMessage =
            "ExperimentRun ID and Artifact key and Artifact path not found in LogArtifact request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogArtifact request";
      } else if (request.getArtifact().getKey().isEmpty()) {
        errorMessage = "Artifact key not found in LogArtifact request";
      } else if (request.getArtifact().getPathOnly() && request.getArtifact().getPath().isEmpty()) {
        errorMessage = "Artifact path not found in LogArtifact request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      List<Artifact> artifacts =
          ModelDBUtils.getArtifactsWithUpdatedPath(
              request.getId(), Collections.singletonList(request.getArtifact()));
      // get(0) because the input parameter is a single value and function always return a single
      // value.
      if (artifacts.size() != 1) {
        throw new InternalErrorException(
            "Expected artifacts count is one but found " + artifacts.size());
      }
      Artifact artifact = artifacts.get(0);

      experimentRunDAO.logArtifacts(request.getId(), Collections.singletonList(artifact));
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "add",
              "artifact",
              new Gson().toJsonTree(request.getArtifact())));
      responseObserver.onNext(LogArtifact.Response.newBuilder().build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, LogArtifact.Response.getDefaultInstance());
    }
  }

  @Override
  public void logArtifacts(
      LogArtifacts request, StreamObserver<LogArtifacts.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getArtifactsList().isEmpty()) {
        errorMessage = "ExperimentRun ID and Artifacts not found in LogArtifacts request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogArtifacts request";
      } else if (request.getArtifactsList().isEmpty()) {
        errorMessage = "Artifacts not found in LogArtifacts request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      List<Artifact> artifactList =
          ModelDBUtils.getArtifactsWithUpdatedPath(request.getId(), request.getArtifactsList());

      experimentRunDAO.logArtifacts(request.getId(), artifactList);
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "add",
              "artifacts",
              new Gson().toJsonTree(artifactList)));
      responseObserver.onNext(LogArtifacts.Response.newBuilder().build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, LogArtifacts.Response.getDefaultInstance());
    }
  }

  @Override
  public void getArtifacts(
      GetArtifacts request, StreamObserver<GetArtifacts.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in GetArtifacts request";
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<Artifact> artifactList = experimentRunDAO.getExperimentRunArtifacts(request.getId());
      responseObserver.onNext(
          GetArtifacts.Response.newBuilder().addAllArtifacts(artifactList).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetArtifacts.Response.getDefaultInstance());
    }
  }

  @Override
  public void logExperimentRunCodeVersion(
      LogExperimentRunCodeVersion request,
      StreamObserver<LogExperimentRunCodeVersion.Response> responseObserver) {
    try {
      /*Parameter validation*/
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getCodeVersion() == null) {
        errorMessage =
            "ExperimentRun ID and Code version not found in LogExperimentRunCodeVersion request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogExperimentRunCodeVersion request";
      } else if (request.getCodeVersion() == null) {
        errorMessage = "CodeVersion not found in LogExperimentRunCodeVersion request";
      }

      if (errorMessage != null) {
        LOGGER.info(errorMessage);
        throw new InvalidArgumentException(errorMessage);
      }

      /*User validation*/
      ExperimentRun existingExperimentRun = experimentRunDAO.getExperimentRun(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT,
          existingExperimentRun.getProjectId(),
          ModelDBServiceActions.UPDATE);

      /*UpdateCode version*/
      if (request.getOverwrite()) {
        experimentRunDAO.logExperimentRunCodeVersion(request.getId(), request.getCodeVersion());
      } else {
        if (!existingExperimentRun.getCodeVersionSnapshot().hasCodeArchive()
            && !existingExperimentRun.getCodeVersionSnapshot().hasGitSnapshot()) {
          experimentRunDAO.logExperimentRunCodeVersion(request.getId(), request.getCodeVersion());
        } else {
          errorMessage =
              "Code version already logged for experiment " + existingExperimentRun.getId();
          throw new AlreadyExistsException(errorMessage);
        }
      }
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "log",
              "code_version",
              new Gson().toJsonTree(request.getCodeVersion())));
      responseObserver.onNext(LogExperimentRunCodeVersion.Response.newBuilder().build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, LogExperimentRunCodeVersion.Response.getDefaultInstance());
    }
  }

  @Override
  public void getExperimentRunCodeVersion(
      GetExperimentRunCodeVersion request,
      StreamObserver<GetExperimentRunCodeVersion.Response> responseObserver) {
    try {
      /*Parameter validation*/
      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in GetCodeVersion request";
        throw new InvalidArgumentException(errorMessage);
      }

      /*User validation*/
      ExperimentRun existingExperimentRun = experimentRunDAO.getExperimentRun(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT,
          existingExperimentRun.getProjectId(),
          ModelDBServiceActions.READ);

      /*Get code version*/
      CodeVersion codeVersion = existingExperimentRun.getCodeVersionSnapshot();

      responseObserver.onNext(
          GetExperimentRunCodeVersion.Response.newBuilder().setCodeVersion(codeVersion).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetExperimentRunCodeVersion.Response.getDefaultInstance());
    }
  }

  @Override
  public void logHyperparameter(
      LogHyperparameter request, StreamObserver<LogHyperparameter.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getHyperparameter().getKey().isEmpty()) {
        errorMessage =
            "ExperimentRun ID and New Hyperparameter not found in LogHyperparameter request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogHyperparameter request";
      } else if (request.getHyperparameter().getKey().isEmpty()) {
        errorMessage = "Hyperparameter not found in LogHyperparameter request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      experimentRunDAO.logHyperparameters(
          request.getId(), Collections.singletonList(request.getHyperparameter()));
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "add",
              "hyperparameter",
              new Gson().toJsonTree(request.getHyperparameter())));
      responseObserver.onNext(LogHyperparameter.Response.newBuilder().build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, LogHyperparameter.Response.getDefaultInstance());
    }
  }

  @Override
  public void logHyperparameters(
      LogHyperparameters request, StreamObserver<LogHyperparameters.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getHyperparametersList().isEmpty()) {
        errorMessage =
            "ExperimentRun ID and New Hyperparameters not found in LogHyperparameters request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogHyperparameters request";
      } else if (request.getHyperparametersList().isEmpty()) {
        errorMessage = "Hyperparameters not found in LogHyperparameters request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      experimentRunDAO.logHyperparameters(request.getId(), request.getHyperparametersList());
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "add",
              "hyperparameters",
              new Gson().toJsonTree(request.getHyperparametersList())));
      responseObserver.onNext(LogHyperparameters.Response.newBuilder().build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, LogHyperparameters.Response.getDefaultInstance());
    }
  }

  @Override
  public void getHyperparameters(
      GetHyperparameters request, StreamObserver<GetHyperparameters.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in GetHyperparameters request";
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<KeyValue> hyperparameterList =
          experimentRunDAO.getExperimentRunHyperparameters(request.getId());
      responseObserver.onNext(
          GetHyperparameters.Response.newBuilder()
              .addAllHyperparameters(hyperparameterList)
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetHyperparameters.Response.getDefaultInstance());
    }
  }

  @Override
  public void logAttribute(
      LogAttribute request, StreamObserver<LogAttribute.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getAttribute().getKey().isEmpty()) {
        errorMessage = "ExperimentRun ID and New Attribute not found in LogAttribute request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogAttribute request";
      } else if (request.getAttribute().getKey().isEmpty()) {
        errorMessage = "Attribute not found in LogAttribute request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);
      experimentRunDAO.logAttributes(
          request.getId(), Collections.singletonList(request.getAttribute()));
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE, "add", "attribute", request.getAttribute()));
      LOGGER.info("Auditing complete, creating response.");
      responseObserver.onNext(LogAttribute.Response.newBuilder().build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, LogAttribute.Response.getDefaultInstance());
    }
  }

  @Override
  public void logAttributes(
      LogAttributes request, StreamObserver<LogAttributes.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getAttributesList().isEmpty()) {
        errorMessage = "ExperimentRun ID and New Attributes not found in LogAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogAttributes request";
      } else if (request.getAttributesList().isEmpty()) {
        errorMessage = "Attributes not found in LogAttributes request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      experimentRunDAO.logAttributes(request.getId(), request.getAttributesList());
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "add",
              "attributes",
              request.getAttributesList()));
      responseObserver.onNext(LogAttributes.Response.newBuilder().build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, LogAttributes.Response.getDefaultInstance());
    }
  }

  @Override
  public void getExperimentRunAttributes(
      GetAttributes request, StreamObserver<GetAttributes.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty()
          && request.getAttributeKeysList().isEmpty()
          && !request.getGetAll()) {
        errorMessage = "ExperimentRun ID and Attributes not found in GetAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in GetAttributes request";
      } else if (request.getAttributeKeysList().isEmpty() && !request.getGetAll()) {
        errorMessage = "Attributes not found in GetAttributes request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<KeyValue> attributeList =
          experimentRunDAO.getExperimentRunAttributes(
              request.getId(), request.getAttributeKeysList(), request.getGetAll());
      responseObserver.onNext(
          GetAttributes.Response.newBuilder().addAllAttributes(attributeList).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetAttributes.Response.getDefaultInstance());
    }
  }

  @Override
  public void findExperimentRuns(
      FindExperimentRuns request, StreamObserver<FindExperimentRuns.Response> responseObserver) {
    try {
      if (!request.getProjectId().isEmpty()) {
        // Validate if current user has access to the entity or not
        roleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.PROJECT,
            request.getProjectId(),
            ModelDBServiceActions.READ);
      } else if (!request.getExperimentId().isEmpty()) {
        Map<String, String> projectIdsMap =
            experimentDAO.getProjectIdsByExperimentIds(
                Collections.singletonList(request.getExperimentId()));
        String projectId = projectIdsMap.get(request.getExperimentId());
        // Validate if current user has access to the entity or not
        roleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);
      }

      ExperimentRunPaginationDTO experimentRunPaginationDTO =
          experimentRunDAO.findExperimentRuns(
              projectDAO, authService.getCurrentLoginUserInfo(), request);
      responseObserver.onNext(
          FindExperimentRuns.Response.newBuilder()
              .addAllExperimentRuns(experimentRunPaginationDTO.getExperimentRuns())
              .setTotalRecords(experimentRunPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, FindExperimentRuns.Response.getDefaultInstance());
    }
  }

  @Override
  public void sortExperimentRuns(
      SortExperimentRuns request, StreamObserver<SortExperimentRuns.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getExperimentRunIdsList().isEmpty() && request.getSortKey().isEmpty()) {
        errorMessage = "ExperimentRun Id's and sort key not found in SortExperimentRuns request";
      } else if (request.getExperimentRunIdsList().isEmpty()) {
        errorMessage = "ExperimentRun Id's not found in SortExperimentRuns request";
      } else if (request.getSortKey().isEmpty()) {
        errorMessage = "Sort key not found in SortExperimentRuns request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      ExperimentRunPaginationDTO experimentRunPaginationDTO =
          experimentRunDAO.sortExperimentRuns(projectDAO, request);
      responseObserver.onNext(
          SortExperimentRuns.Response.newBuilder()
              .addAllExperimentRuns(experimentRunPaginationDTO.getExperimentRuns())
              .setTotalRecords(experimentRunPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, SortExperimentRuns.Response.getDefaultInstance());
    }
  }

  @Override
  public void getTopExperimentRuns(
      TopExperimentRunsSelector request,
      StreamObserver<TopExperimentRunsSelector.Response> responseObserver) {
    try {
      if (!request.getProjectId().isEmpty()) {
        // Validate if current user has access to the entity or not
        roleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.PROJECT,
            request.getProjectId(),
            ModelDBServiceActions.READ);
      } else if (!request.getExperimentId().isEmpty()) {
        Map<String, String> projectIdsMap =
            experimentDAO.getProjectIdsByExperimentIds(
                Collections.singletonList(request.getExperimentId()));
        String projectId = projectIdsMap.get(request.getExperimentId());
        // Validate if current user has access to the entity or not
        roleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);
      }

      List<ExperimentRun> experimentRuns =
          experimentRunDAO.getTopExperimentRuns(projectDAO, request);
      responseObserver.onNext(
          TopExperimentRunsSelector.Response.newBuilder()
              .addAllExperimentRuns(experimentRuns)
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, TopExperimentRunsSelector.Response.getDefaultInstance());
    }
  }

  @Override
  public void logJobId(LogJobId request, StreamObserver<LogJobId.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getJobId().isEmpty()) {
        errorMessage = "ExperimentRun ID and Job ID not found in LogJobId request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogJobId request";
      } else if (request.getJobId().isEmpty()) {
        errorMessage = "Job ID not found in LogJobId request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      experimentRunDAO.logJobId(request.getId(), request.getJobId());
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(ModelDBConstants.METADATA_JSON_TEMPLATE, "add", "job", request.getJobId()));
      responseObserver.onNext(LogJobId.Response.newBuilder().build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, LogJobId.Response.getDefaultInstance());
    }
  }

  @Override
  public void getJobId(GetJobId request, StreamObserver<GetJobId.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in GetJobId request";
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      String jobId = experimentRunDAO.getJobId(request.getId());
      responseObserver.onNext(GetJobId.Response.newBuilder().setJobId(jobId).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, GetJobId.Response.getDefaultInstance());
    }
  }

  @Override
  public void getChildrenExperimentRuns(
      GetChildrenExperimentRuns request,
      StreamObserver<GetChildrenExperimentRuns.Response> responseObserver) {
    try {
      if (request.getExperimentRunId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in GetChildrenExperimentRuns request";
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId =
          experimentRunDAO.getProjectIdByExperimentRunId(request.getExperimentRunId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      ExperimentRunPaginationDTO experimentRunPaginationDTO =
          experimentRunDAO.getExperimentRunsFromEntity(
              projectDAO,
              ModelDBConstants.PARENT_ID,
              request.getExperimentRunId(),
              request.getPageNumber(),
              request.getPageLimit(),
              request.getAscending(),
              request.getSortKey());
      responseObserver.onNext(
          GetChildrenExperimentRuns.Response.newBuilder()
              .addAllExperimentRuns(experimentRunPaginationDTO.getExperimentRuns())
              .setTotalRecords(experimentRunPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetChildrenExperimentRuns.Response.getDefaultInstance());
    }
  }

  @Override
  public void setParentExperimentRunId(
      SetParentExperimentRunId request,
      StreamObserver<SetParentExperimentRunId.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getExperimentRunId().isEmpty() && request.getParentId().isEmpty()) {
        errorMessage =
            "ExperimentRun ID OR Parent ExperimentRun ID not found in SetParentExperimentRunId request";
      } else if (request.getExperimentRunId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in SetParentExperimentRunId request";
      } else if (request.getParentId().isEmpty()) {
        errorMessage = "Parent ExperimentRun ID not found in SetParentExperimentRunId request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String parentExperimentRunProjectId =
          experimentRunDAO.getProjectIdByExperimentRunId(request.getParentId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT,
          parentExperimentRunProjectId,
          ModelDBServiceActions.UPDATE);

      String existingChildrenExperimentRunProjectId =
          experimentRunDAO.getProjectIdByExperimentRunId(request.getExperimentRunId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT,
          existingChildrenExperimentRunProjectId,
          ModelDBServiceActions.UPDATE);

      experimentRunDAO.setParentExperimentRunId(
          request.getExperimentRunId(), request.getParentId());
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getExperimentRunId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE, "add", "parentId", request.getParentId()));
      responseObserver.onNext(SetParentExperimentRunId.Response.newBuilder().build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, SetParentExperimentRunId.Response.getDefaultInstance());
    }
  }

  @Override
  public void logDataset(LogDataset request, StreamObserver<LogDataset.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()
          && request.getDataset().getLinkedArtifactId().isEmpty()
          && !request.getDataset().getArtifactType().equals(ArtifactType.DATA)) {
        throw new InvalidArgumentException(
            "LogDataset only supported for Artifact type Data.\nExperimentRun ID and Dataset id not found in LogArtifact request.");
      } else if (request.getId().isEmpty()) {
        throw new InvalidArgumentException("ExperimentRun ID not found in LogDataset request");
      } else if (request.getDataset().getLinkedArtifactId().isEmpty()) {
        throw new InvalidArgumentException("Dataset ID not found in LogArtifact request");
      } else if (!request.getDataset().getArtifactType().equals(ArtifactType.DATA)) {
        throw new InvalidArgumentException("LogDataset only supported for Artifact type Data");
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Artifact dataset = request.getDataset();

      experimentRunDAO.logDatasets(
          request.getId(), Collections.singletonList(dataset), request.getOverwrite());
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "add",
              "dataset",
              new Gson().toJsonTree(dataset)));
      responseObserver.onNext(LogDataset.Response.newBuilder().build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, LogDataset.Response.getDefaultInstance());
    }
  }

  @Override
  public void logDatasets(
      LogDatasets request, StreamObserver<ai.verta.modeldb.LogDatasets.Response> responseObserver) {
    try {
      if (request.getId().isEmpty() && request.getDatasetsList().isEmpty()) {
        throw new InvalidArgumentException(
            "ExperimentRun ID and Datasets not found in LogDatasets request");
      } else if (request.getId().isEmpty()) {
        throw new InvalidArgumentException("ExperimentRun ID not found in LogDatasets request");
      } else if (request.getDatasetsList().isEmpty()) {
        throw new InvalidArgumentException("Datasets not found in LogDatasets request");
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      experimentRunDAO.logDatasets(
          request.getId(), request.getDatasetsList(), request.getOverwrite());
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "add",
              "datasets",
              new Gson().toJsonTree(request.getDatasetsList())));
      responseObserver.onNext(LogDatasets.Response.newBuilder().build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, LogDatasets.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteArtifact(
      DeleteArtifact request, StreamObserver<DeleteArtifact.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getKey().isEmpty()) {
        errorMessage = "ExperimentRun ID and Artifact key not found in DeleteArtifact request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in DeleteArtifact request";
      } else if (request.getKey().isEmpty()) {
        errorMessage = "Artifact key not found in DeleteArtifact request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      experimentRunDAO.deleteArtifacts(request.getId(), request.getKey());
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE, "delete", "artifacts", request.getKey()));
      responseObserver.onNext(DeleteArtifact.Response.newBuilder().build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, DeleteArtifact.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteExperimentRuns(
      DeleteExperimentRuns request,
      StreamObserver<DeleteExperimentRuns.Response> responseObserver) {
    try {
      if (request.getIdsList().isEmpty()) {
        throw new InvalidArgumentException(
            "ExperimentRun IDs not found in DeleteExperimentRuns request");
      }

      List<String> deleteExperimentRunsIds =
          experimentRunDAO.deleteExperimentRuns(request.getIdsList());
      saveAuditLogs(null, ModelDBConstants.DELETE, deleteExperimentRunsIds, "");
      responseObserver.onNext(
          DeleteExperimentRuns.Response.newBuilder()
              .setStatus(!deleteExperimentRunsIds.isEmpty())
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteExperimentRuns.Response.getDefaultInstance());
    }
  }

  @Override
  public void logVersionedInput(
      LogVersionedInput request, StreamObserver<LogVersionedInput.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getVersionedInputs() == null) {
        errorMessage =
            "ExperimentRun ID and Versioning value not found in LogVersionedInput request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogVersionedInput request";
      } else if (request.getVersionedInputs() == null || !request.hasVersionedInputs()) {
        errorMessage = "Versioning value not found in LogVersionedInput request";
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      experimentRunDAO.logVersionedInput(request);
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(ModelDBConstants.METADATA_JSON_TEMPLATE, "add", "version_input", ""));
      responseObserver.onNext(LogVersionedInput.Response.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, LogVersionedInput.Response.getDefaultInstance());
    }
  }

  @Override
  public void getVersionedInputs(
      GetVersionedInput request, StreamObserver<GetVersionedInput.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in GetVersionedInput request";
      }

      if (errorMessage != null) {
        throw new ModelDBException(errorMessage, io.grpc.Status.Code.INVALID_ARGUMENT);
      }

      GetVersionedInput.Response response = experimentRunDAO.getVersionedInputs(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetVersionedInput.Response.getDefaultInstance());
    }
  }

  @Override
  public void commitArtifactPart(
      CommitArtifactPart request, StreamObserver<CommitArtifactPart.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in CommitArtifactPart request";
      } else if (request.getKey().isEmpty()) {
        errorMessage = "Artifact key not found in CommitArtifactPart request";
      } else if (request.getArtifactPart().getPartNumber() == 0) {
        errorMessage = "Artifact part number is not specified in CommitArtifactPart request";
      }

      if (errorMessage != null) {
        throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      CommitArtifactPart.Response response = experimentRunDAO.commitArtifactPart(request);
      JsonObject partInfoJson = new JsonObject();
      partInfoJson.addProperty(
          "artifact_part", ModelDBUtils.getStringFromProtoObject(request.getArtifactPart()));
      partInfoJson.addProperty("artifact_key", request.getKey());
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "upload_artifact_part",
              "artifact_part_info",
              new Gson().toJson(partInfoJson)));
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, CommitArtifactPart.Response.getDefaultInstance());
    }
  }

  @Override
  public void getCommittedArtifactParts(
      GetCommittedArtifactParts request,
      StreamObserver<GetCommittedArtifactParts.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in GetCommittedArtifactParts request";
      } else if (request.getKey().isEmpty()) {
        errorMessage = "Artifact key not found in GetCommittedArtifactParts request";
      }

      if (errorMessage != null) {
        throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      GetCommittedArtifactParts.Response response =
          experimentRunDAO.getCommittedArtifactParts(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetCommittedArtifactParts.Response.getDefaultInstance());
    }
  }

  @Override
  public void commitMultipartArtifact(
      CommitMultipartArtifact request,
      StreamObserver<CommitMultipartArtifact.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in CommitMultipartArtifact request";
      } else if (request.getKey().isEmpty()) {
        errorMessage = "Artifact key not found in CommitMultipartArtifact request";
      }

      if (errorMessage != null) {
        throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Gson gson = new Gson();
      JsonObject multipartInfo = new JsonObject();
      CommitMultipartArtifact.Response response =
          experimentRunDAO.commitMultipartArtifact(
              request,
              (s3Key, uploadId, partETags) -> {
                multipartInfo.addProperty("artifact_key", s3Key);
                multipartInfo.addProperty("upload_id", uploadId);
                multipartInfo.add("part_etags", gson.toJsonTree(partETags));
                artifactStoreDAO.commitMultipart(s3Key, uploadId, partETags);
              });
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "commit_artifact_parts",
              "artifact_multipart_info",
              new Gson().toJson(multipartInfo)));
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, CommitMultipartArtifact.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteHyperparameters(
      DeleteHyperparameters request,
      StreamObserver<DeleteHyperparameters.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in DeleteHyperparameters request";
      } else if (request.getHyperparameterKeysList().isEmpty() && !request.getDeleteAll()) {
        errorMessage =
            "Hyperparameter keys not found and deleteAll flag has false in DeleteHyperparameters request";
      }

      if (errorMessage != null) {
        throw new ModelDBException(errorMessage, io.grpc.Status.Code.INVALID_ARGUMENT);
      }

      experimentRunDAO.deleteExperimentRunKeyValuesEntities(
          request.getId(),
          request.getHyperparameterKeysList(),
          request.getDeleteAll(),
          ModelDBConstants.HYPERPARAMETERS);
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "delete",
              "hyperparameters",
              request.getDeleteAll()
                  ? "deleteAll"
                  : new Gson().toJsonTree(request.getHyperparameterKeysList())));
      responseObserver.onNext(DeleteHyperparameters.Response.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteHyperparameters.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteMetrics(
      DeleteMetrics request, StreamObserver<DeleteMetrics.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in DeleteMetrics request";
      } else if (request.getMetricKeysList().isEmpty() && !request.getDeleteAll()) {
        errorMessage =
            "Metrics keys not found and deleteAll flag has false in DeleteMetrics request";
      }

      if (errorMessage != null) {
        throw new ModelDBException(errorMessage, io.grpc.Status.Code.INVALID_ARGUMENT);
      }

      experimentRunDAO.deleteExperimentRunKeyValuesEntities(
          request.getId(),
          request.getMetricKeysList(),
          request.getDeleteAll(),
          ModelDBConstants.METRICS);
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "delete",
              "metrics",
              request.getDeleteAll()
                  ? "deleteAll"
                  : new Gson().toJsonTree(request.getMetricKeysList())));
      responseObserver.onNext(DeleteMetrics.Response.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, DeleteMetrics.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteObservations(
      DeleteObservations request, StreamObserver<DeleteObservations.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in DeleteObservations request";
      } else if (request.getObservationKeysList().isEmpty() && !request.getDeleteAll()) {
        errorMessage =
            "Observation keys not found and deleteAll flag has false in DeleteObservations request";
      }

      if (errorMessage != null) {
        throw new ModelDBException(errorMessage, io.grpc.Status.Code.INVALID_ARGUMENT);
      }

      experimentRunDAO.deleteExperimentRunObservationsEntities(
          request.getId(), request.getObservationKeysList(), request.getDeleteAll());
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "delete",
              "observation",
              request.getDeleteAll()
                  ? "deleteAll"
                  : new Gson().toJsonTree(request.getObservationKeysList())));
      responseObserver.onNext(DeleteObservations.Response.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteObservations.Response.getDefaultInstance());
    }
  }

  @Override
  public void listCommitExperimentRuns(
      ListCommitExperimentRunsRequest request,
      StreamObserver<ListCommitExperimentRunsRequest.Response> responseObserver) {
    try {
      if (request.getCommitSha().isEmpty()) {
        throw new ModelDBException("Commit SHA should not be empty", Code.INVALID_ARGUMENT);
      }

      ListCommitExperimentRunsRequest.Response response =
          experimentRunDAO.listCommitExperimentRuns(
              projectDAO,
              request,
              (session) -> repositoryDAO.getRepositoryById(session, request.getRepositoryId()),
              (session, repository) ->
                  commitDAO.getCommitEntity(session, request.getCommitSha(), repository));
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, ListCommitExperimentRunsRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void listBlobExperimentRuns(
      ListBlobExperimentRunsRequest request,
      StreamObserver<ListBlobExperimentRunsRequest.Response> responseObserver) {
    try {
      if (request.getCommitSha().isEmpty()) {
        throw new ModelDBException("Commit SHA should not be empty", Code.INVALID_ARGUMENT);
      }

      ListBlobExperimentRunsRequest.Response response =
          experimentRunDAO.listBlobExperimentRuns(
              projectDAO,
              request,
              (session) -> repositoryDAO.getRepositoryById(session, request.getRepositoryId()),
              (session, repository) ->
                  commitDAO.getCommitEntity(session, request.getCommitSha(), repository));
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, ListBlobExperimentRunsRequest.Response.getDefaultInstance());
    }
  }

  @Override
  public void getExperimentRunsByDatasetVersionId(
      GetExperimentRunsByDatasetVersionId request,
      StreamObserver<GetExperimentRunsByDatasetVersionId.Response> responseObserver) {
    try {
      if (request.getDatasetVersionId().isEmpty()) {
        throw new ModelDBException("DatasetVersion Id should not be empty", Code.INVALID_ARGUMENT);
      }

      ExperimentRunPaginationDTO experimentRunPaginationDTO =
          experimentRunDAO.getExperimentRunsByDatasetVersionId(projectDAO, request);
      GetExperimentRunsByDatasetVersionId.Response response =
          GetExperimentRunsByDatasetVersionId.Response.newBuilder()
              .addAllExperimentRuns(experimentRunPaginationDTO.getExperimentRuns())
              .setTotalRecords(experimentRunPaginationDTO.getTotalRecords())
              .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetExperimentRunsByDatasetVersionId.Response.getDefaultInstance());
    }
  }

  @Override
  public void cloneExperimentRun(
      CloneExperimentRun request, StreamObserver<CloneExperimentRun.Response> responseObserver) {
    try {
      if (request.getSrcExperimentRunId().isEmpty()) {
        throw new ModelDBException(
            "Source ExperimentRun Id should not be empty", Code.INVALID_ARGUMENT);
      }

      ExperimentRun clonedExperimentRun =
          experimentRunDAO.cloneExperimentRun(
              projectDAO, request, authService.getCurrentLoginUserInfo());
      saveAuditLogs(
          null,
          ModelDBConstants.CREATE,
          Collections.singletonList(clonedExperimentRun.getId()),
          "");
      responseObserver.onNext(
          CloneExperimentRun.Response.newBuilder().setRun(clonedExperimentRun).build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, CloneExperimentRun.Response.getDefaultInstance());
    }
  }

  @Override
  public void logEnvironment(
      LogEnvironment request, StreamObserver<LogEnvironment.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        throw new ModelDBException("ExperimentRun Id should not be empty", Code.INVALID_ARGUMENT);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      experimentRunDAO.logEnvironment(request.getId(), request.getEnvironment());
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "log",
              "environment",
              ModelDBUtils.getStringFromProtoObject(request.getEnvironment())));
      responseObserver.onNext(LogEnvironment.Response.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(responseObserver, e, LogEnvironment.Response.getDefaultInstance());
    }
  }
}
