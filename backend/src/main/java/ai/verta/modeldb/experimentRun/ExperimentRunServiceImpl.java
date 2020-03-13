package ai.verta.modeldb.experimentRun;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.AddExperimentRunAttributes;
import ai.verta.modeldb.AddExperimentRunTag;
import ai.verta.modeldb.AddExperimentRunTags;
import ai.verta.modeldb.App;
import ai.verta.modeldb.Artifact;
import ai.verta.modeldb.ArtifactTypeEnum.ArtifactType;
import ai.verta.modeldb.CodeVersion;
import ai.verta.modeldb.CreateExperimentRun;
import ai.verta.modeldb.DeleteArtifact;
import ai.verta.modeldb.DeleteExperiment;
import ai.verta.modeldb.DeleteExperimentRun;
import ai.verta.modeldb.DeleteExperimentRunAttributes;
import ai.verta.modeldb.DeleteExperimentRunTag;
import ai.verta.modeldb.DeleteExperimentRunTags;
import ai.verta.modeldb.DeleteExperimentRuns;
import ai.verta.modeldb.DeleteExperiments;
import ai.verta.modeldb.Experiment;
import ai.verta.modeldb.ExperimentRun;
import ai.verta.modeldb.ExperimentRunServiceGrpc.ExperimentRunServiceImplBase;
import ai.verta.modeldb.FindExperimentRuns;
import ai.verta.modeldb.GetArtifacts;
import ai.verta.modeldb.GetAttributes;
import ai.verta.modeldb.GetChildrenExperimentRuns;
import ai.verta.modeldb.GetDatasets;
import ai.verta.modeldb.GetExperimentRunById;
import ai.verta.modeldb.GetExperimentRunByName;
import ai.verta.modeldb.GetExperimentRunCodeVersion;
import ai.verta.modeldb.GetExperimentRunsInExperiment;
import ai.verta.modeldb.GetExperimentRunsInProject;
import ai.verta.modeldb.GetHyperparameters;
import ai.verta.modeldb.GetJobId;
import ai.verta.modeldb.GetMetrics;
import ai.verta.modeldb.GetObservations;
import ai.verta.modeldb.GetTags;
import ai.verta.modeldb.GetUrlForArtifact;
import ai.verta.modeldb.GetVersionedInput;
import ai.verta.modeldb.LogArtifact;
import ai.verta.modeldb.LogArtifacts;
import ai.verta.modeldb.LogAttribute;
import ai.verta.modeldb.LogAttributes;
import ai.verta.modeldb.LogDataset;
import ai.verta.modeldb.LogDataset.Response;
import ai.verta.modeldb.LogDatasets;
import ai.verta.modeldb.LogExperimentRunCodeVersion;
import ai.verta.modeldb.LogHyperparameter;
import ai.verta.modeldb.LogHyperparameters;
import ai.verta.modeldb.LogJobId;
import ai.verta.modeldb.LogMetric;
import ai.verta.modeldb.LogMetrics;
import ai.verta.modeldb.LogObservation;
import ai.verta.modeldb.LogObservations;
import ai.verta.modeldb.LogVersionedInput;
import ai.verta.modeldb.ModelDBAuthInterceptor;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.Observation;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.SetParentExperimentRunId;
import ai.verta.modeldb.SortExperimentRuns;
import ai.verta.modeldb.TopExperimentRunsSelector;
import ai.verta.modeldb.UpdateExperimentRunDescription;
import ai.verta.modeldb.UpdateExperimentRunName;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAO;
import ai.verta.modeldb.dto.ExperimentRunPaginationDTO;
import ai.verta.modeldb.experiment.ExperimentDAO;
import ai.verta.modeldb.monitoring.ErrorCountResource;
import ai.verta.modeldb.monitoring.QPSCountResource;
import ai.verta.modeldb.monitoring.RequestLatencyResource;
import ai.verta.modeldb.project.ProjectDAO;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ModelResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.uac.UserInfo;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExperimentRunServiceImpl extends ExperimentRunServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(ExperimentRunServiceImpl.class);
  private AuthService authService;
  private RoleService roleService;
  private ExperimentRunDAO experimentRunDAO;
  private ProjectDAO projectDAO;
  private ExperimentDAO experimentDAO;
  private ArtifactStoreDAO artifactStoreDAO;
  private DatasetVersionDAO datasetVersionDAO;

  public ExperimentRunServiceImpl(
      AuthService authService,
      RoleService roleService,
      ExperimentRunDAO experimentRunDAO,
      ProjectDAO projectDAO,
      ExperimentDAO experimentDAO,
      ArtifactStoreDAO artifactStoreDAO,
      DatasetVersionDAO datasetVersionDAO) {
    this.authService = authService;
    this.roleService = roleService;
    this.experimentRunDAO = experimentRunDAO;
    this.projectDAO = projectDAO;
    this.experimentDAO = experimentDAO;
    this.artifactStoreDAO = artifactStoreDAO;
    this.datasetVersionDAO = datasetVersionDAO;
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
    if (request.getProjectId().isEmpty()
        && request.getExperimentId().isEmpty()
        && request.getName().isEmpty()) {
      errorMessage =
          "Project ID and Experiment ID and ExperimentRun name not found in CreateExperimentRun request";
    } else if (request.getProjectId().isEmpty()) {
      errorMessage = "Project ID not found in CreateExperimentRun request";
    } else if (request.getExperimentId().isEmpty()) {
      errorMessage = "Experiment ID not found in CreateExperimentRun request";
    } else if (request.getName().isEmpty()) {
      errorMessage = "ExperimentRun name not found in CreateExperimentRun request";
    }

    if (errorMessage != null) {
      LOGGER.warn(errorMessage);
      Status status =
          Status.newBuilder()
              .setCode(Code.INVALID_ARGUMENT_VALUE)
              .setMessage(errorMessage)
              .addDetails(Any.pack(CreateExperimentRun.Response.getDefaultInstance()))
              .build();
      throw StatusProto.toStatusRuntimeException(status);
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

    if (App.getInstance().getStoreClientCreationTimestamp() && request.getDateCreated() != 0L) {
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
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();

      ExperimentRun experimentRun = getExperimentRunFromRequest(request, userInfo);

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT,
          request.getProjectId(),
          ModelDBServiceActions.UPDATE);
      validateExperimentEntity(request.getExperimentId());

      experimentRun = experimentRunDAO.insertExperimentRun(experimentRun);
      responseObserver.onNext(
          CreateExperimentRun.Response.newBuilder().setExperimentRun(experimentRun).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, CreateExperimentRun.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteExperimentRun(
      DeleteExperimentRun request, StreamObserver<DeleteExperimentRun.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in DeleteExperimentRun request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteExperiment.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      boolean deleteStatus = deleteExperimentRuns(Collections.singletonList(request.getId()));

      responseObserver.onNext(
          DeleteExperimentRun.Response.newBuilder().setStatus(deleteStatus).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(DeleteExperimentRun.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getExperimentRunsInProject(
      GetExperimentRunsInProject request,
      StreamObserver<GetExperimentRunsInProject.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getProjectId().isEmpty()) {
        String errorMessage = "Project ID not found in GetExperimentRunsInProject request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetExperimentRunsInProject.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, request.getProjectId(), ModelDBServiceActions.READ);

      ExperimentRunPaginationDTO experimentRunPaginationDTO =
          experimentRunDAO.getExperimentRunsFromEntity(
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

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetExperimentRunsInProject.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getExperimentRunsInExperiment(
      GetExperimentRunsInExperiment request,
      StreamObserver<GetExperimentRunsInExperiment.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getExperimentId().isEmpty()) {
        String errorMessage = "Experiment ID not found in GetExperimentRunsInExperiment request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetExperimentRunsInExperiment.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Map<String, String> projectIdsMap =
          experimentDAO.getProjectIdsByExperimentIds(
              Collections.singletonList(request.getExperimentId()));
      if (projectIdsMap.size() == 0) {
        ModelDBUtils.logAndThrowError(
            ModelDBConstants.ACCESS_DENIED_EXPERIMENT_RUN,
            Code.PERMISSION_DENIED_VALUE,
            Any.pack(GetExperimentRunsInExperiment.getDefaultInstance()));
      }
      String projectId = projectIdsMap.get(request.getExperimentId());

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      ExperimentRunPaginationDTO experimentRunPaginationDTO =
          experimentRunDAO.getExperimentRunsFromEntity(
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

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetExperimentRunsInExperiment.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getExperimentRunById(
      GetExperimentRunById request,
      StreamObserver<GetExperimentRunById.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in GetExperimentRunById request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetExperimentRunById.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      ExperimentRun experimentRun = experimentRunDAO.getExperimentRun(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT,
          experimentRun.getProjectId(),
          ModelDBServiceActions.READ);

      responseObserver.onNext(
          GetExperimentRunById.Response.newBuilder().setExperimentRun(experimentRun).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetExperimentRunById.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getExperimentRunByName(
      GetExperimentRunByName request,
      StreamObserver<GetExperimentRunByName.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
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
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetExperimentRunByName.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
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
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage("ExperimentRun not found in database")
                .addDetails(Any.pack(GetExperimentRunByName.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      } else if (experimentRunList.size() != 1) {
        Status status =
            Status.newBuilder()
                .setCode(Code.INTERNAL_VALUE)
                .setMessage("Multiple ExperimentRun found in database")
                .addDetails(Any.pack(GetExperimentRunByName.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      responseObserver.onNext(
          GetExperimentRunByName.Response.newBuilder()
              .setExperimentRun(experimentRunList.get(0))
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetExperimentRunByName.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void updateExperimentRunDescription(
      UpdateExperimentRunDescription request,
      StreamObserver<UpdateExperimentRunDescription.Response> responseObserver) {
    QPSCountResource.inc();

    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getId().isEmpty()) {
        String errorMessage =
            "ExperimentRun ID not found in UpdateExperimentRunDescription request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(UpdateExperimentRunDescription.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.updateExperimentRunDescription(
              request.getId(), request.getDescription());

      responseObserver.onNext(
          UpdateExperimentRunDescription.Response.newBuilder()
              .setExperimentRun(updatedExperimentRun)
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(UpdateExperimentRunDescription.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void updateExperimentRunName(
      UpdateExperimentRunName request,
      StreamObserver<UpdateExperimentRunName.Response> responseObserver) {
    QPSCountResource.inc();

    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in UpdateExperimentRunName request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(UpdateExperimentRunName.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.updateExperimentRunName(
              request.getId(), ModelDBUtils.checkEntityNameLength(request.getName()));

      responseObserver.onNext(
          UpdateExperimentRunName.Response.newBuilder()
              .setExperimentRun(updatedExperimentRun)
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(UpdateExperimentRunName.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void addExperimentRunTags(
      AddExperimentRunTags request,
      StreamObserver<AddExperimentRunTags.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
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
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(AddExperimentRunTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.addExperimentRunTags(
              request.getId(), ModelDBUtils.checkEntityTagsLength(request.getTagsList()));
      responseObserver.onNext(
          AddExperimentRunTags.Response.newBuilder()
              .setExperimentRun(updatedExperimentRun)
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(AddExperimentRunTags.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void addExperimentRunTag(
      AddExperimentRunTag request, StreamObserver<AddExperimentRunTag.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
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
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(AddExperimentRunTag.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.addExperimentRunTags(
              request.getId(),
              ModelDBUtils.checkEntityTagsLength(Collections.singletonList(request.getTag())));
      responseObserver.onNext(
          AddExperimentRunTag.Response.newBuilder().setExperimentRun(updatedExperimentRun).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(AddExperimentRunTag.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getExperimentRunTags(
      GetTags request, StreamObserver<GetTags.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in GetExperimentRunTags request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<String> experimentRunTags = experimentRunDAO.getExperimentRunTags(request.getId());
      responseObserver.onNext(GetTags.Response.newBuilder().addAllTags(experimentRunTags).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetTags.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void deleteExperimentRunTags(
      DeleteExperimentRunTags request,
      StreamObserver<DeleteExperimentRunTags.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
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
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteExperimentRunTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.deleteExperimentRunTags(
              request.getId(), request.getTagsList(), request.getDeleteAll());
      responseObserver.onNext(
          DeleteExperimentRunTags.Response.newBuilder()
              .setExperimentRun(updatedExperimentRun)
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(DeleteExperimentRunTags.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void deleteExperimentRunTag(
      DeleteExperimentRunTag request,
      StreamObserver<DeleteExperimentRunTag.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
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
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteExperimentRunTag.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.deleteExperimentRunTags(
              request.getId(), Collections.singletonList(request.getTag()), false);
      responseObserver.onNext(
          DeleteExperimentRunTag.Response.newBuilder()
              .setExperimentRun(updatedExperimentRun)
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(DeleteExperimentRunTag.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void addExperimentRunAttributes(
      AddExperimentRunAttributes request,
      StreamObserver<AddExperimentRunAttributes.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
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
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(AddExperimentRunAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.addExperimentRunAttributes(request.getId(), request.getAttributesList());
      responseObserver.onNext(
          AddExperimentRunAttributes.Response.newBuilder()
              .setExperimentRun(updatedExperimentRun)
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(AddExperimentRunAttributes.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void deleteExperimentRunAttributes(
      DeleteExperimentRunAttributes request,
      StreamObserver<DeleteExperimentRunAttributes.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
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
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteExperimentRunAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.deleteExperimentRunAttributes(
              request.getId(), request.getAttributeKeysList(), request.getDeleteAll());
      responseObserver.onNext(
          DeleteExperimentRunAttributes.Response.newBuilder()
              .setExperimentRun(updatedExperimentRun)
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(DeleteExperimentRunAttributes.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void logObservation(
      LogObservation request, StreamObserver<LogObservation.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
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
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogObservation.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.logObservations(
              request.getId(), Collections.singletonList(request.getObservation()));
      responseObserver.onNext(
          LogObservation.Response.newBuilder().setExperimentRun(updatedExperimentRun).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(LogObservation.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void logObservations(
      LogObservations request, StreamObserver<LogObservations.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getObservationsList().isEmpty()) {
        errorMessage = "ExperimentRun ID and Observations not found in LogObservations request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogObservations request";
      } else if (request.getObservationsList().isEmpty()) {
        errorMessage = "Observations not found in LogObservations request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogObservations.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.logObservations(request.getId(), request.getObservationsList());
      responseObserver.onNext(
          LogObservations.Response.newBuilder().setExperimentRun(updatedExperimentRun).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(LogObservations.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getObservations(
      GetObservations request, StreamObserver<GetObservations.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getObservationKey().isEmpty()) {
        errorMessage = "ExperimentRun ID and Observation key not found in GetObservations request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in GetObservations request";
      } else if (request.getObservationKey().isEmpty()) {
        errorMessage = "Observation key not found in GetObservations request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetObservations.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
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

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetObservations.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void logMetric(LogMetric request, StreamObserver<LogMetric.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
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
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogMetric.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.logMetrics(
              request.getId(), Collections.singletonList(request.getMetric()));
      responseObserver.onNext(
          LogMetric.Response.newBuilder().setExperimentRun(updatedExperimentRun).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(LogMetric.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void logMetrics(LogMetrics request, StreamObserver<LogMetrics.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getMetricsList().isEmpty()) {
        errorMessage = "ExperimentRun ID and New Metrics not found in LogMetrics request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogMetrics request";
      } else if (request.getMetricsList().isEmpty()) {
        errorMessage = "New Metrics not found in LogMetrics request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogMetrics.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.logMetrics(request.getId(), request.getMetricsList());
      responseObserver.onNext(
          LogMetrics.Response.newBuilder().setExperimentRun(updatedExperimentRun).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(LogMetrics.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getMetrics(GetMetrics request, StreamObserver<GetMetrics.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in GetMetrics request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetMetrics.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<KeyValue> metricList = experimentRunDAO.getExperimentRunMetrics(request.getId());
      responseObserver.onNext(GetMetrics.Response.newBuilder().addAllMetrics(metricList).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetMetrics.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getDatasets(
      GetDatasets request, StreamObserver<GetDatasets.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in GetDatasets request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetDatasets.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<Artifact> datasetList = experimentRunDAO.getExperimentRunDatasets(request.getId());
      responseObserver.onNext(
          GetDatasets.Response.newBuilder().addAllDatasets(datasetList).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetDatasets.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getUrlForArtifact(
      GetUrlForArtifact request, StreamObserver<GetUrlForArtifact.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
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
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetUrlForArtifact.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      String s3Key = null;

      /*Process code*/
      if (request.getArtifactType() == ArtifactType.CODE) {
        // just creating the error string
        errorMessage =
            "Code versioning artifact not found at experimentRun, experiment and project level";
        s3Key = getUrlForCode(request);
      } else if (request.getArtifactType() == ArtifactType.DATA) {
        errorMessage = "Data versioning artifact not found";
        s3Key = getUrlForData(request);
      } else {
        errorMessage =
            "ExperimentRun ID "
                + request.getId()
                + " does not have the artifact "
                + request.getKey();

        s3Key =
            getS3Path(
                experimentRunDAO.getExperimentRunArtifacts(request.getId()), request.getKey());
      }
      if (s3Key == null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetUrlForArtifact.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      GetUrlForArtifact.Response response =
          artifactStoreDAO.getUrlForArtifact(s3Key, request.getMethod());
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetUrlForArtifact.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  private String getUrlForData(GetUrlForArtifact request) throws InvalidProtocolBufferException {

    assert (request.getArtifactType().equals(ArtifactType.DATA));
    assert (!request.getId().isEmpty());
    assert (!request.getKey().isEmpty());
    ExperimentRun exprRun = experimentRunDAO.getExperimentRun(request.getId());
    List<Artifact> datasets = exprRun.getDatasetsList();
    for (Artifact dataset : datasets) {
      if (dataset.getKey().equals(request.getKey()))
        return datasetVersionDAO.getUrlForDatasetVersion(
            dataset.getLinkedArtifactId(), request.getMethod());
    }
    // if the loop above did not return anything that means there was no Dataset logged with the
    // particular key
    // pre the dataset-as-fcc project datasets were stored as artifacts, so check there before
    // returning
    return getS3Path(experimentRunDAO.getExperimentRunArtifacts(request.getId()), request.getKey());
  }

  private String getUrlForCode(GetUrlForArtifact request) throws InvalidProtocolBufferException {
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

  private String getS3Path(List<Artifact> experimentRunArtifacts, String artifactKey) {
    for (Artifact artifact : experimentRunArtifacts) {
      if (artifactKey.equalsIgnoreCase(artifact.getKey())) return artifact.getPath();
    }
    return null;
  }

  @Override
  public void logArtifact(
      LogArtifact request, StreamObserver<LogArtifact.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
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
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogArtifact.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
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
        errorMessage = "Expected artifacts count is one but found " + artifacts.size();
        ModelDBUtils.logAndThrowError(
            errorMessage, Code.INTERNAL_VALUE, Any.pack(LogArtifact.Response.getDefaultInstance()));
      }
      Artifact artifact = artifacts.get(0);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.logArtifacts(request.getId(), Collections.singletonList(artifact));
      LogArtifact.Response.Builder responseBuilder =
          LogArtifact.Response.newBuilder().setExperimentRun(updatedExperimentRun);
      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(LogArtifact.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void logArtifacts(
      LogArtifacts request, StreamObserver<LogArtifacts.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getArtifactsList().isEmpty()) {
        errorMessage = "ExperimentRun ID and Artifacts not found in LogArtifacts request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogArtifacts request";
      } else if (request.getArtifactsList().isEmpty()) {
        errorMessage = "Artifacts not found in LogArtifacts request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogArtifacts.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      List<Artifact> artifactList =
          ModelDBUtils.getArtifactsWithUpdatedPath(request.getId(), request.getArtifactsList());

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.logArtifacts(request.getId(), artifactList);
      LogArtifacts.Response.Builder responseBuilder =
          LogArtifacts.Response.newBuilder().setExperimentRun(updatedExperimentRun);
      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(LogArtifacts.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getArtifacts(
      GetArtifacts request, StreamObserver<GetArtifacts.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in GetArtifacts request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetArtifacts.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      List<Artifact> artifactList = experimentRunDAO.getExperimentRunArtifacts(request.getId());
      responseObserver.onNext(
          GetArtifacts.Response.newBuilder().addAllArtifacts(artifactList).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetArtifacts.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void logExperimentRunCodeVersion(
      LogExperimentRunCodeVersion request,
      StreamObserver<LogExperimentRunCodeVersion.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
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
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogExperimentRunCodeVersion.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      /*User validation*/
      ExperimentRun existingExperimentRun = experimentRunDAO.getExperimentRun(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT,
          existingExperimentRun.getProjectId(),
          ModelDBServiceActions.UPDATE);

      /*UpdateCode version*/
      ExperimentRun updatedExperimentRun;
      if (request.getOverwrite()) {
        updatedExperimentRun =
            experimentRunDAO.logExperimentRunCodeVersion(request.getId(), request.getCodeVersion());
      } else {
        if (!existingExperimentRun.getCodeVersionSnapshot().hasCodeArchive()
            && !existingExperimentRun.getCodeVersionSnapshot().hasGitSnapshot()) {
          updatedExperimentRun =
              experimentRunDAO.logExperimentRunCodeVersion(
                  request.getId(), request.getCodeVersion());
        } else {
          errorMessage =
              "Code version already logged for experiment " + existingExperimentRun.getId();
          Status status =
              Status.newBuilder()
                  .setCode(Code.ALREADY_EXISTS_VALUE)
                  .setMessage(errorMessage)
                  .addDetails(Any.pack(LogExperimentRunCodeVersion.Response.getDefaultInstance()))
                  .build();
          throw StatusProto.toStatusRuntimeException(status);
        }
      }
      /*Build response*/
      LogExperimentRunCodeVersion.Response.Builder responseBuilder =
          LogExperimentRunCodeVersion.Response.newBuilder().setExperimentRun(updatedExperimentRun);
      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(LogExperimentRunCodeVersion.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getExperimentRunCodeVersion(
      GetExperimentRunCodeVersion request,
      StreamObserver<GetExperimentRunCodeVersion.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      /*Parameter validation*/
      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in GetCodeVersion request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetExperimentRunCodeVersion.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
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

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetExperimentRunCodeVersion.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void logHyperparameter(
      LogHyperparameter request, StreamObserver<LogHyperparameter.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
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
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogHyperparameter.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.logHyperparameters(
              request.getId(), Collections.singletonList(request.getHyperparameter()));
      responseObserver.onNext(
          LogHyperparameter.Response.newBuilder().setExperimentRun(updatedExperimentRun).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(LogHyperparameter.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void logHyperparameters(
      LogHyperparameters request, StreamObserver<LogHyperparameters.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
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
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogHyperparameters.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.logHyperparameters(request.getId(), request.getHyperparametersList());
      responseObserver.onNext(
          LogHyperparameters.Response.newBuilder().setExperimentRun(updatedExperimentRun).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(LogHyperparameters.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getHyperparameters(
      GetHyperparameters request, StreamObserver<GetHyperparameters.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getId().isEmpty()) {
        String errorMessaeg = "ExperimentRun ID not found in GetHyperparameters request";
        LOGGER.warn(errorMessaeg);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessaeg)
                .addDetails(Any.pack(GetHyperparameters.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
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

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetHyperparameters.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void logAttribute(
      LogAttribute request, StreamObserver<LogAttribute.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getAttribute().getKey().isEmpty()) {
        errorMessage = "ExperimentRun ID and New Attribute not found in LogAttribute request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogAttribute request";
      } else if (request.getAttribute().getKey().isEmpty()) {
        errorMessage = "Attribute not found in LogAttribute request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogAttribute.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.logAttributes(
              request.getId(), Collections.singletonList(request.getAttribute()));
      responseObserver.onNext(
          LogAttribute.Response.newBuilder().setExperimentRun(updatedExperimentRun).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(LogAttribute.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void logAttributes(
      LogAttributes request, StreamObserver<LogAttributes.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getAttributesList().isEmpty()) {
        errorMessage = "ExperimentRun ID and New Attributes not found in LogAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogAttributes request";
      } else if (request.getAttributesList().isEmpty()) {
        errorMessage = "Attributes not found in LogAttributes request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.logAttributes(request.getId(), request.getAttributesList());
      responseObserver.onNext(
          LogAttributes.Response.newBuilder().setExperimentRun(updatedExperimentRun).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(LogAttributes.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getExperimentRunAttributes(
      GetAttributes request, StreamObserver<GetAttributes.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
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
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
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

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetAttributes.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  /**
   * For getting experimentRuns that user has access to (either as the owner or a collaborator):
   * <br>
   *
   * <ol>
   *   <li>Iterate through all experimentRuns of the requested experimentRunIds
   *   <li>Get the project Id they belong to.
   *   <li>Check if project is accessible or not.
   * </ol>
   *
   * The list of accessible experimentRunIDs is built and returned by this method.
   *
   * @param requestedExperimentRunIds : experimentRun Ids
   * @return List<String> : list of accessible ExperimentRun Id
   */
  public List<String> getAccessibleExperimentRunIDs(
      List<String> requestedExperimentRunIds, ModelDBServiceActions modelDBServiceActions) {
    List<String> accessibleExperimentRunIds = new ArrayList<>();

    Map<String, String> projectIdExperimentRunIdMap =
        experimentRunDAO.getProjectIdsFromExperimentRunIds(requestedExperimentRunIds);
    if (projectIdExperimentRunIdMap.size() == 0) {
      Status status =
          Status.newBuilder()
              .setCode(Code.PERMISSION_DENIED_VALUE)
              .setMessage(
                  "Access is denied. Experiment not found for given ids : "
                      + requestedExperimentRunIds)
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
    Set<String> projectIdSet = new HashSet<>(projectIdExperimentRunIdMap.values());

    List<String> allowedProjectIds;
    // Validate if current user has access to the entity or not
    if (projectIdSet.size() == 1) {
      roleService.isSelfAllowed(
          ModelDBServiceResourceTypes.PROJECT,
          modelDBServiceActions,
          new ArrayList<>(projectIdSet).get(0));
      accessibleExperimentRunIds.addAll(requestedExperimentRunIds);
    } else {
      allowedProjectIds =
          roleService.getSelfAllowedResources(
              ModelDBServiceResourceTypes.PROJECT, modelDBServiceActions);
      // Validate if current user has access to the entity or not
      allowedProjectIds.retainAll(requestedExperimentRunIds);
      accessibleExperimentRunIds.addAll(allowedProjectIds);
    }
    return accessibleExperimentRunIds;
  }

  @Override
  public void findExperimentRuns(
      FindExperimentRuns request, StreamObserver<FindExperimentRuns.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getProjectId().isEmpty()
          && request.getExperimentId().isEmpty()
          && request.getExperimentRunIdsList().isEmpty()) {
        String errorMessage =
            "Project ID and Experiment ID and ExperimentRun Id's not found in FindExperimentRuns request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(FindExperimentRuns.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

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

      if (!request.getExperimentRunIdsList().isEmpty()) {
        List<String> accessibleExperimentRunIds =
            getAccessibleExperimentRunIDs(
                request.getExperimentRunIdsList(), ModelDBServiceActions.READ);
        if (accessibleExperimentRunIds.isEmpty()) {
          ModelDBUtils.logAndThrowError(
              ModelDBConstants.ACCESS_DENIED_EXPERIMENT_RUN,
              Code.PERMISSION_DENIED_VALUE,
              Any.pack(FindExperimentRuns.getDefaultInstance()));
        }

        request =
            request
                .toBuilder()
                .clearExperimentRunIds()
                .addAllExperimentRunIds(accessibleExperimentRunIds)
                .build();
      }

      ExperimentRunPaginationDTO experimentRunPaginationDTO =
          experimentRunDAO.findExperimentRuns(request);
      responseObserver.onNext(
          FindExperimentRuns.Response.newBuilder()
              .addAllExperimentRuns(experimentRunPaginationDTO.getExperimentRuns())
              .setTotalRecords(experimentRunPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(FindExperimentRuns.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void sortExperimentRuns(
      SortExperimentRuns request, StreamObserver<SortExperimentRuns.Response> responseObserver) {
    QPSCountResource.inc();

    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getExperimentRunIdsList().isEmpty() && request.getSortKey().isEmpty()) {
        errorMessage = "ExperimentRun Id's and sort key not found in SortExperimentRuns request";
      } else if (request.getExperimentRunIdsList().isEmpty()) {
        errorMessage = "ExperimentRun Id's not found in SortExperimentRuns request";
      } else if (request.getSortKey().isEmpty()) {
        errorMessage = "Sort key not found in SortExperimentRuns request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(SortExperimentRuns.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<String> accessibleExperimentRunIds =
          getAccessibleExperimentRunIDs(
              request.getExperimentRunIdsList(), ModelDBServiceActions.READ);
      if (accessibleExperimentRunIds.isEmpty()) {
        ModelDBUtils.logAndThrowError(
            ModelDBConstants.ACCESS_DENIED_EXPERIMENT_RUN,
            Code.PERMISSION_DENIED_VALUE,
            Any.pack(FindExperimentRuns.getDefaultInstance()));
      }

      request =
          request
              .toBuilder()
              .clearExperimentRunIds()
              .addAllExperimentRunIds(accessibleExperimentRunIds)
              .build();

      ExperimentRunPaginationDTO experimentRunPaginationDTO =
          experimentRunDAO.sortExperimentRuns(request);
      responseObserver.onNext(
          SortExperimentRuns.Response.newBuilder()
              .addAllExperimentRuns(experimentRunPaginationDTO.getExperimentRuns())
              .setTotalRecords(experimentRunPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(SortExperimentRuns.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getTopExperimentRuns(
      TopExperimentRunsSelector request,
      StreamObserver<TopExperimentRunsSelector.Response> responseObserver) {
    QPSCountResource.inc();

    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if ((request.getProjectId().isEmpty()
              && request.getExperimentId().isEmpty()
              && request.getExperimentRunIdsList().isEmpty())
          || request.getSortKey().isEmpty()) {
        String errorMessage =
            "Project ID and Experiment ID and Experiment IDs and Sort key not found in TopExperimentRunsSelector request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(TopExperimentRunsSelector.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

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

      if (!request.getExperimentRunIdsList().isEmpty()) {
        List<String> accessibleExperimentRunIds =
            getAccessibleExperimentRunIDs(
                request.getExperimentRunIdsList(), ModelDBServiceActions.READ);
        if (accessibleExperimentRunIds.isEmpty()) {
          ModelDBUtils.logAndThrowError(
              ModelDBConstants.ACCESS_DENIED_EXPERIMENT_RUN,
              Code.PERMISSION_DENIED_VALUE,
              Any.pack(FindExperimentRuns.getDefaultInstance()));
        }

        request =
            request
                .toBuilder()
                .clearExperimentRunIds()
                .addAllExperimentRunIds(accessibleExperimentRunIds)
                .build();
      }

      List<ExperimentRun> experimentRuns = experimentRunDAO.getTopExperimentRuns(request);
      responseObserver.onNext(
          TopExperimentRunsSelector.Response.newBuilder()
              .addAllExperimentRuns(experimentRuns)
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(TopExperimentRunsSelector.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void logJobId(LogJobId request, StreamObserver<LogJobId.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getJobId().isEmpty()) {
        errorMessage = "ExperimentRun ID and Job ID not found in LogJobId request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogJobId request";
      } else if (request.getJobId().isEmpty()) {
        errorMessage = "Job ID not found in LogJobId request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(LogJobId.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.logJobId(request.getId(), request.getJobId());
      responseObserver.onNext(
          LogJobId.Response.newBuilder().setExperimentRun(updatedExperimentRun).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(LogJobId.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getJobId(GetJobId request, StreamObserver<GetJobId.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in GetJobId request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetJobId.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      String jobId = experimentRunDAO.getJobId(request.getId());
      responseObserver.onNext(GetJobId.Response.newBuilder().setJobId(jobId).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetJobId.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getChildrenExperimentRuns(
      GetChildrenExperimentRuns request,
      StreamObserver<GetChildrenExperimentRuns.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getExperimentRunId().isEmpty()) {
        String errorMessage = "ExperimentRun ID not found in GetChildrenExperimentRuns request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetChildrenExperimentRuns.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId =
          experimentRunDAO.getProjectIdByExperimentRunId(request.getExperimentRunId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.READ);

      ExperimentRunPaginationDTO experimentRunPaginationDTO =
          experimentRunDAO.getExperimentRunsFromEntity(
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

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetChildrenExperimentRuns.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void setParentExperimentRunId(
      SetParentExperimentRunId request,
      StreamObserver<SetParentExperimentRunId.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
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
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetChildrenExperimentRuns.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
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

      ExperimentRun existingChildrenExperimentRun =
          experimentRunDAO.setParentExperimentRunId(
              request.getExperimentRunId(), request.getParentId());
      responseObserver.onNext(
          SetParentExperimentRunId.Response.newBuilder()
              .setExperimentRun(existingChildrenExperimentRun)
              .build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(GetChildrenExperimentRuns.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void logDataset(LogDataset request, StreamObserver<Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId().isEmpty()
          && request.getDataset().getLinkedArtifactId().isEmpty()
          && !request.getDataset().getArtifactType().equals(ArtifactType.DATA)) {
        errorMessage =
            "LogDataset only supported for Artifact type Data.\nExperimentRun ID and Dataset id not found in LogArtifact request.";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogDataset request";
      } else if (request.getDataset().getLinkedArtifactId().isEmpty()) {
        errorMessage = "Dataset ID not found in LogArtifact request";
      } else if (!request.getDataset().getArtifactType().equals(ArtifactType.DATA)) {
        errorMessage = "LogDataset only supported for Artifact type Data";
      }

      if (errorMessage != null) {
        logAndThrowError(
            errorMessage,
            Code.INVALID_ARGUMENT_VALUE,
            Any.pack(LogDataset.Response.getDefaultInstance()));
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      Artifact dataset = request.getDataset();

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.logDatasets(
              request.getId(), Collections.singletonList(dataset), request.getOverwrite());

      LogDataset.Response.Builder responseBuilder =
          LogDataset.Response.newBuilder().setExperimentRun(updatedExperimentRun);
      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(LogDataset.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void logDatasets(
      LogDatasets request, StreamObserver<ai.verta.modeldb.LogDatasets.Response> responseObserver) {
    QPSCountResource.inc();

    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getDatasetsList().isEmpty()) {
        errorMessage = "ExperimentRun ID and Datasets not found in LogDatasets request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in LogDatasets request";
      } else if (request.getDatasetsList().isEmpty()) {
        errorMessage = "Datasets not found in LogDatasets request";
      }

      if (errorMessage != null) {
        logAndThrowError(
            errorMessage,
            Code.INVALID_ARGUMENT_VALUE,
            Any.pack(LogDatasets.Response.getDefaultInstance()));
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.logDatasets(
              request.getId(), request.getDatasetsList(), request.getOverwrite());
      LogDatasets.Response.Builder responseBuilder =
          LogDatasets.Response.newBuilder().setExperimentRun(updatedExperimentRun);
      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(LogDatasets.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  private void logAndThrowError(String errorMessage, int errorCode, Any defaultResponse) {
    LOGGER.warn(errorMessage);
    Status status =
        Status.newBuilder()
            .setCode(errorCode)
            .setMessage(errorMessage)
            .addDetails(defaultResponse)
            .build();
    throw StatusProto.toStatusRuntimeException(status);
  }

  @Override
  public void deleteArtifact(
      DeleteArtifact request, StreamObserver<DeleteArtifact.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getKey().isEmpty()) {
        errorMessage = "ExperimentRun ID and Artifact key not found in DeleteArtifact request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "ExperimentRun ID not found in DeleteArtifact request";
      } else if (request.getKey().isEmpty()) {
        errorMessage = "Artifact key not found in DeleteArtifact request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteArtifact.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      String projectId = experimentRunDAO.getProjectIdByExperimentRunId(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.PROJECT, projectId, ModelDBServiceActions.UPDATE);

      ExperimentRun updatedExperimentRun =
          experimentRunDAO.deleteArtifacts(request.getId(), request.getKey());
      responseObserver.onNext(
          DeleteArtifact.Response.newBuilder().setExperimentRun(updatedExperimentRun).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(DeleteArtifact.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void deleteExperimentRuns(
      DeleteExperimentRuns request,
      StreamObserver<DeleteExperimentRuns.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getIdsList().isEmpty()) {
        String errorMessage = "ExperimentRun IDs not found in DeleteExperimentRuns request";
        ModelDBUtils.logAndThrowError(
            errorMessage,
            Code.INVALID_ARGUMENT_VALUE,
            Any.pack(DeleteExperiments.Response.getDefaultInstance()));
      }

      boolean deleteStatus = deleteExperimentRuns(request.getIdsList());
      responseObserver.onNext(
          DeleteExperimentRuns.Response.newBuilder().setStatus(deleteStatus).build());
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(DeleteExperimentRuns.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  private boolean deleteExperimentRuns(List<String> experimentIds) {
    List<String> accessibleExperimentRunIds =
        getAccessibleExperimentRunIDs(experimentIds, ModelDBServiceActions.UPDATE);
    if (accessibleExperimentRunIds.isEmpty()) {
      Status statusMessage =
          Status.newBuilder()
              .setCode(Code.PERMISSION_DENIED_VALUE)
              .setMessage(
                  "Access is denied. User is unauthorized for given ExperimentRun entities : "
                      + accessibleExperimentRunIds)
              .build();
      throw StatusProto.toStatusRuntimeException(statusMessage);
    }

    return experimentRunDAO.deleteExperimentRuns(accessibleExperimentRunIds);
  }

  @Override
  public void logVersionedInput(
      LogVersionedInput request, StreamObserver<LogVersionedInput.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
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
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteArtifact.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      LogVersionedInput.Response response = experimentRunDAO.logVersionedInput(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, LogVersionedInput.Response.getDefaultInstance());
    }
  }

  @Override
  public void getVersionedInputs(
      GetVersionedInput request, StreamObserver<GetVersionedInput.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
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
      ModelDBUtils.observeError(
          responseObserver, e, GetVersionedInput.Response.getDefaultInstance());
    }
  }
}
