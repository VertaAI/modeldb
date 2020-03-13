package ai.verta.modeldb.dataset;

import ai.verta.common.KeyValue;
import ai.verta.common.ValueTypeEnum;
import ai.verta.modeldb.AddDatasetAttributes;
import ai.verta.modeldb.AddDatasetTags;
import ai.verta.modeldb.App;
import ai.verta.modeldb.CreateDataset;
import ai.verta.modeldb.Dataset;
import ai.verta.modeldb.DatasetServiceGrpc.DatasetServiceImplBase;
import ai.verta.modeldb.DatasetVersion;
import ai.verta.modeldb.DatasetVisibilityEnum.DatasetVisibility;
import ai.verta.modeldb.DeleteDataset;
import ai.verta.modeldb.DeleteDatasetAttributes;
import ai.verta.modeldb.DeleteDatasetTags;
import ai.verta.modeldb.DeleteDatasets;
import ai.verta.modeldb.Experiment;
import ai.verta.modeldb.ExperimentRun;
import ai.verta.modeldb.FindDatasetVersions;
import ai.verta.modeldb.FindDatasets;
import ai.verta.modeldb.FindExperimentRuns;
import ai.verta.modeldb.FindExperiments;
import ai.verta.modeldb.GetAllDatasets;
import ai.verta.modeldb.GetAttributes;
import ai.verta.modeldb.GetDatasetById;
import ai.verta.modeldb.GetDatasetByName;
import ai.verta.modeldb.GetExperimentRunByDataset;
import ai.verta.modeldb.GetTags;
import ai.verta.modeldb.KeyValueQuery;
import ai.verta.modeldb.LastExperimentByDatasetId;
import ai.verta.modeldb.ModelDBAuthInterceptor;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.OperatorEnum;
import ai.verta.modeldb.SetDatasetVisibilty;
import ai.verta.modeldb.SetDatasetWorkspace;
import ai.verta.modeldb.SetDatasetWorkspace.Response;
import ai.verta.modeldb.UpdateDatasetAttributes;
import ai.verta.modeldb.UpdateDatasetDescription;
import ai.verta.modeldb.UpdateDatasetName;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAO;
import ai.verta.modeldb.dto.DatasetPaginationDTO;
import ai.verta.modeldb.dto.DatasetVersionDTO;
import ai.verta.modeldb.dto.ExperimentPaginationDTO;
import ai.verta.modeldb.dto.ExperimentRunPaginationDTO;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.experiment.ExperimentDAO;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.monitoring.ErrorCountResource;
import ai.verta.modeldb.monitoring.QPSCountResource;
import ai.verta.modeldb.monitoring.RequestLatencyResource;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ModelResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.uac.UserInfo;
import com.google.protobuf.Any;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatasetServiceImpl extends DatasetServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(DatasetServiceImpl.class);
  private AuthService authService;
  private RoleService roleService;
  private DatasetDAO datasetDAO;
  private DatasetVersionDAO datasetVersionDAO;
  private ExperimentDAO experimentDAO;
  private ExperimentRunDAO experimentRunDAO;

  public DatasetServiceImpl(
      AuthService authService,
      RoleService roleService,
      DatasetDAO datasetDAO,
      DatasetVersionDAO datasetVersionDAO,
      ExperimentDAO experimentDAO,
      ExperimentRunDAO experimentRunDAO) {
    this.authService = authService;
    this.roleService = roleService;
    this.datasetDAO = datasetDAO;
    this.datasetVersionDAO = datasetVersionDAO;
    this.experimentDAO = experimentDAO;
    this.experimentRunDAO = experimentRunDAO;
  }

  /**
   * Create a Dataset from the input parameters and logs it. Required input parameter : name
   * Generate a random UUID for id set Name,Description,Attributes, Tags and Visibility from the
   * request. set current user as an owner
   */
  @Override
  public void createDataset(
      CreateDataset request, StreamObserver<CreateDataset.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getName().isEmpty()) {
        LOGGER.warn(ModelDBMessages.DATASET_NAME_NOT_FOUND_IN_REQUEST);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(ModelDBMessages.DATASET_NAME_NOT_FOUND_IN_REQUEST)
                .addDetails(Any.pack(CreateDataset.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET, null, ModelDBServiceActions.CREATE);

      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();

      Dataset dataset = getDatasetFromRequest(request, userInfo);
      ModelDBUtils.checkPersonalWorkspace(
          userInfo, dataset.getWorkspaceType(), dataset.getWorkspaceId(), "dataset");
      Dataset createdDataset = datasetDAO.createDataset(dataset, userInfo);

      responseObserver.onNext(
          CreateDataset.Response.newBuilder().setDataset(createdDataset).build());
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
              .addDetails(Any.pack(CreateDataset.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  private Dataset getDatasetFromRequest(CreateDataset request, UserInfo userInfo) {
    /*
     * Generate a random UUID for id
     * set Name,Description,Attributes, Tags and Visibility from the request
     * set times to current time
     */
    Dataset.Builder datasetBuilder =
        Dataset.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setName(ModelDBUtils.checkEntityNameLength(request.getName()))
            .setDescription(request.getDescription())
            .addAllAttributes(request.getAttributesList())
            .addAllTags(ModelDBUtils.checkEntityTagsLength(request.getTagsList()))
            .setDatasetVisibility(request.getDatasetVisibility())
            .setDatasetType(request.getDatasetType());

    if (App.getInstance().getStoreClientCreationTimestamp() && request.getTimeCreated() != 0L) {
      datasetBuilder
          .setTimeCreated(request.getTimeCreated())
          .setTimeUpdated(request.getTimeCreated());
    } else {
      datasetBuilder
          .setTimeCreated(Calendar.getInstance().getTimeInMillis())
          .setTimeUpdated(Calendar.getInstance().getTimeInMillis());
    }

    /*
     * Set current user as owner.
     */

    if (userInfo != null) {
      String vertaId = authService.getVertaIdFromUserInfo(userInfo);
      datasetBuilder.setOwner(vertaId);
      String workspaceName = request.getWorkspaceName();
      WorkspaceDTO workspaceDTO =
          roleService.getWorkspaceDTOByWorkspaceName(userInfo, workspaceName);
      if (workspaceDTO.getWorkspaceId() != null) {
        datasetBuilder.setWorkspaceId(workspaceDTO.getWorkspaceId());
        datasetBuilder.setWorkspaceType(workspaceDTO.getWorkspaceType());
      }
    }
    return datasetBuilder.build();
  }

  /** Fetch all (owned and shared) dataset for the current user */
  @Override
  public void getAllDatasets(
      GetAllDatasets request, StreamObserver<GetAllDatasets.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      LOGGER.trace("getting dataset");
      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();

      FindDatasets.Builder findDatasets =
          FindDatasets.newBuilder()
              .setPageNumber(request.getPageNumber())
              .setPageLimit(request.getPageLimit())
              .setAscending(request.getAscending())
              .setSortKey(request.getSortKey())
              .setWorkspaceName(request.getWorkspaceName());

      DatasetPaginationDTO datasetPaginationDTO =
          datasetDAO.findDatasets(findDatasets.build(), userInfo, DatasetVisibility.PRIVATE);

      LOGGER.debug(
          ModelDBMessages.ACCESSIBLE_DATASET_IN_SERVICE, datasetPaginationDTO.getDatasets().size());
      responseObserver.onNext(
          GetAllDatasets.Response.newBuilder()
              .addAllDatasets(datasetPaginationDTO.getDatasets())
              .setTotalRecords(datasetPaginationDTO.getTotalRecords())
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
              .addDetails(Any.pack(GetAllDatasets.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  /** Deletes dataset corresponding to the id. Required input parameter : id */
  @Override
  public void deleteDataset(
      DeleteDataset request, StreamObserver<DeleteDataset.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        LOGGER.warn(ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST)
                .addDetails(Any.pack(DeleteDataset.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      boolean deleteStatus = datasetDAO.deleteDatasets(Collections.singletonList(request.getId()));
      responseObserver.onNext(DeleteDataset.Response.newBuilder().setStatus(deleteStatus).build());
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
              .addDetails(Any.pack(DeleteDataset.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  /**
   * Fetches dataset corresponding to the id. <br>
   * Required input parameter : id
   */
  @Override
  public void getDatasetById(
      GetDatasetById request, StreamObserver<GetDatasetById.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        LOGGER.warn(ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST)
                .addDetails(Any.pack(GetDatasetById.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET, request.getId(), ModelDBServiceActions.READ);

      Dataset dataset = datasetDAO.getDatasetById(request.getId());
      responseObserver.onNext(GetDatasetById.Response.newBuilder().setDataset(dataset).build());
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
              .addDetails(Any.pack(GetDatasetById.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void findDatasets(
      FindDatasets request, StreamObserver<FindDatasets.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      DatasetPaginationDTO datasetPaginationDTO =
          datasetDAO.findDatasets(request, userInfo, DatasetVisibility.PRIVATE);
      responseObserver.onNext(
          FindDatasets.Response.newBuilder()
              .addAllDatasets(datasetPaginationDTO.getDatasets())
              .setTotalRecords(datasetPaginationDTO.getTotalRecords())
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
              .addDetails(Any.pack(FindDatasets.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getDatasetByName(
      GetDatasetByName request, StreamObserver<GetDatasetByName.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      if (request.getName().isEmpty()) {
        LOGGER.warn(ModelDBMessages.DATASET_NAME_NOT_FOUND_IN_REQUEST);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(ModelDBMessages.DATASET_NAME_NOT_FOUND_IN_REQUEST)
                .addDetails(Any.pack(GetDatasetByName.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();

      FindDatasets.Builder findDatasets =
          FindDatasets.newBuilder()
              .addPredicates(
                  KeyValueQuery.newBuilder()
                      .setKey(ModelDBConstants.NAME)
                      .setValue(Value.newBuilder().setStringValue(request.getName()).build())
                      .setOperator(OperatorEnum.Operator.EQ)
                      .setValueType(ValueTypeEnum.ValueType.STRING)
                      .build())
              .setWorkspaceName(
                  request.getWorkspaceName().isEmpty()
                      ? authService.getUsernameFromUserInfo(userInfo)
                      : request.getWorkspaceName());

      DatasetPaginationDTO datasetPaginationDTO =
          datasetDAO.findDatasets(findDatasets.build(), userInfo, DatasetVisibility.PRIVATE);

      if (datasetPaginationDTO.getTotalRecords() == 0) {
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage("Dataset not found")
                .addDetails(Any.pack(GetDatasetByName.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      Dataset selfOwnerdataset = null;
      List<Dataset> sharedDatasets = new ArrayList<>();

      for (Dataset dataset : datasetPaginationDTO.getDatasets()) {
        if (userInfo == null
            || dataset.getOwner().equals(authService.getVertaIdFromUserInfo(userInfo))) {
          selfOwnerdataset = dataset;
        } else {
          sharedDatasets.add(dataset);
        }
      }

      GetDatasetByName.Response.Builder responseBuilder = GetDatasetByName.Response.newBuilder();
      if (selfOwnerdataset != null) {
        responseBuilder.setDatasetByUser(selfOwnerdataset);
      }
      responseBuilder.addAllSharedDatasets(sharedDatasets);

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
              .addDetails(Any.pack(GetDatasetByName.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void updateDatasetName(
      UpdateDatasetName request, StreamObserver<UpdateDatasetName.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getName().isEmpty()) {
        errorMessage = "Dataset ID and Dataset name not found in UpdateDatasetName request";
      } else if (request.getId().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST;
      } else if (request.getName().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_NAME_NOT_FOUND_IN_REQUEST;
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(UpdateDatasetName.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET, request.getId(), ModelDBServiceActions.UPDATE);

      Dataset dataset =
          datasetDAO.updateDatasetName(
              request.getId(), ModelDBUtils.checkEntityNameLength(request.getName()));

      responseObserver.onNext(UpdateDatasetName.Response.newBuilder().setDataset(dataset).build());
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
              .addDetails(Any.pack(UpdateDatasetName.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void updateDatasetDescription(
      UpdateDatasetDescription request,
      StreamObserver<UpdateDatasetDescription.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getDescription().isEmpty()) {
        errorMessage =
            "Dataset ID and Dataset description not found in UpdateDatasetDescription request";
      } else if (request.getId().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST;
      } else if (request.getDescription().isEmpty()) {
        errorMessage = "Dataset description not found in UpdateDatasetDescription request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(UpdateDatasetDescription.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET, request.getId(), ModelDBServiceActions.UPDATE);

      Dataset dataset =
          datasetDAO.updateDatasetDescription(request.getId(), request.getDescription());

      responseObserver.onNext(
          UpdateDatasetDescription.Response.newBuilder().setDataset(dataset).build());
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
              .addDetails(Any.pack(UpdateDatasetDescription.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void addDatasetTags(
      AddDatasetTags request, StreamObserver<AddDatasetTags.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getTagsList().isEmpty()) {
        errorMessage = "Dataset ID and Dataset tags not found in AddDatasetTags request";
      } else if (request.getId().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST;
      } else if (request.getTagsList().isEmpty()) {
        errorMessage = "Dataset tags not found in AddDatasetTags request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(AddDatasetTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET, request.getId(), ModelDBServiceActions.UPDATE);

      Dataset updatedDataset =
          datasetDAO.addDatasetTags(
              request.getId(), ModelDBUtils.checkEntityTagsLength(request.getTagsList()));

      responseObserver.onNext(
          AddDatasetTags.Response.newBuilder().setDataset(updatedDataset).build());
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
              .addDetails(Any.pack(AddDatasetTags.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getDatasetTags(GetTags request, StreamObserver<GetTags.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        LOGGER.warn(ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST)
                .addDetails(Any.pack(GetTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET, request.getId(), ModelDBServiceActions.READ);

      List<String> tags = datasetDAO.getDatasetTags(request.getId());
      responseObserver.onNext(GetTags.Response.newBuilder().addAllTags(tags).build());
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
  public void deleteDatasetTags(
      DeleteDatasetTags request, StreamObserver<DeleteDatasetTags.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getTagsList().isEmpty() && !request.getDeleteAll()) {
        errorMessage = "Dataset ID and Dataset tags not found in DeleteDatasetTags request";
      } else if (request.getId().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST;
      } else if (request.getTagsList().isEmpty() && !request.getDeleteAll()) {
        errorMessage = "Dataset tags not found in DeleteDatasetTags request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteDatasetTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET, request.getId(), ModelDBServiceActions.UPDATE);

      Dataset updatedDataset =
          datasetDAO.deleteDatasetTags(
              request.getId(), request.getTagsList(), request.getDeleteAll());

      responseObserver.onNext(
          DeleteDatasetTags.Response.newBuilder().setDataset(updatedDataset).build());
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
              .addDetails(Any.pack(DeleteDatasetTags.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void addDatasetAttributes(
      AddDatasetAttributes request,
      StreamObserver<AddDatasetAttributes.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getAttributesList().isEmpty()) {
        errorMessage = "Dataset ID and Attribute list not found in AddDatasetAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST;
      } else if (request.getAttributesList().isEmpty()) {
        errorMessage = "Attribute list not found in AddDatasetAttributes request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(AddDatasetAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET, request.getId(), ModelDBServiceActions.UPDATE);

      Dataset updatedDataset =
          datasetDAO.addDatasetAttributes(request.getId(), request.getAttributesList());
      responseObserver.onNext(
          AddDatasetAttributes.Response.newBuilder().setDataset(updatedDataset).build());
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
              .addDetails(Any.pack(AddDatasetAttributes.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void updateDatasetAttributes(
      UpdateDatasetAttributes request,
      StreamObserver<UpdateDatasetAttributes.Response> responseObserver) {
    QPSCountResource.inc();

    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getAttribute().getKey().isEmpty()) {
        errorMessage = "Dataset ID and attribute key not found in UpdateDatasetAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST;
      } else if (request.getAttribute().getKey().isEmpty()) {
        errorMessage = "Attribute key not found in UpdateDatasetAttributes request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(UpdateDatasetAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET, request.getId(), ModelDBServiceActions.UPDATE);

      Dataset updatedDataset =
          datasetDAO.updateDatasetAttributes(request.getId(), request.getAttribute());
      responseObserver.onNext(
          UpdateDatasetAttributes.Response.newBuilder().setDataset(updatedDataset).build());
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
              .addDetails(Any.pack(UpdateDatasetAttributes.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getDatasetAttributes(
      GetAttributes request, StreamObserver<GetAttributes.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty()
          && request.getAttributeKeysList().isEmpty()
          && !request.getGetAll()) {
        errorMessage = "Dataset ID and Dataset attribute keys not found in GetAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST;
      } else if (request.getAttributeKeysList().isEmpty() && !request.getGetAll()) {
        errorMessage = "Dataset attribute keys not found in GetAttributes request";
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

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET, request.getId(), ModelDBServiceActions.READ);

      List<KeyValue> attributes =
          datasetDAO.getDatasetAttributes(
              request.getId(), request.getAttributeKeysList(), request.getGetAll());
      responseObserver.onNext(
          GetAttributes.Response.newBuilder().addAllAttributes(attributes).build());
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

  @Override
  public void deleteDatasetAttributes(
      DeleteDatasetAttributes request,
      StreamObserver<DeleteDatasetAttributes.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty()
          && request.getAttributeKeysList().isEmpty()
          && !request.getDeleteAll()) {
        errorMessage =
            "Dataset ID and Dataset attribute keys not found in DeleteDatasetAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST;
      } else if (request.getAttributeKeysList().isEmpty() && !request.getDeleteAll()) {
        errorMessage = "Dataset attribute keys not found in DeleteDatasetAttributes request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteDatasetAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET, request.getId(), ModelDBServiceActions.UPDATE);

      Dataset updatedDataset =
          datasetDAO.deleteDatasetAttributes(
              request.getId(), request.getAttributeKeysList(), request.getDeleteAll());
      responseObserver.onNext(
          DeleteDatasetAttributes.Response.newBuilder().setDataset(updatedDataset).build());
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
              .addDetails(Any.pack(DeleteDatasetAttributes.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void setDatasetVisibility(
      SetDatasetVisibilty request, StreamObserver<SetDatasetVisibilty.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        LOGGER.warn(ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST)
                .addDetails(Any.pack(SetDatasetVisibilty.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET, request.getId(), ModelDBServiceActions.UPDATE);

      Dataset dataset =
          datasetDAO.setDatasetVisibility(request.getId(), request.getDatasetVisibility());

      responseObserver.onNext(
          SetDatasetVisibilty.Response.newBuilder().setDataset(dataset).build());
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage());
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage());
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(SetDatasetVisibilty.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void deleteDatasets(
      DeleteDatasets request, StreamObserver<DeleteDatasets.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      if (request.getIdsList().isEmpty()) {
        LOGGER.warn(ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST)
                .addDetails(Any.pack(DeleteDatasets.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      boolean deleteStatus = datasetDAO.deleteDatasets(request.getIdsList());
      responseObserver.onNext(DeleteDatasets.Response.newBuilder().setStatus(deleteStatus).build());
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
              .addDetails(Any.pack(DeleteDatasets.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getLastExperimentByDatasetId(
      LastExperimentByDatasetId request,
      StreamObserver<LastExperimentByDatasetId.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getDatasetId().isEmpty()) {
        ModelDBUtils.logAndThrowError(
            ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST,
            Code.INVALID_ARGUMENT_VALUE,
            Any.pack(LastExperimentByDatasetId.Response.getDefaultInstance()));
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET, request.getDatasetId(), ModelDBServiceActions.READ);

      FindDatasetVersions findDatasetVersions =
          FindDatasetVersions.newBuilder()
              .setDatasetId(request.getDatasetId())
              .setIdsOnly(true)
              .build();
      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      DatasetVersionDTO datasetVersionDTO =
          datasetVersionDAO.findDatasetVersions(findDatasetVersions, userInfo);
      List<String> datasetVersionIds = new ArrayList<>();
      ListValue.Builder listValueBuilder = ListValue.newBuilder();
      if (datasetVersionDTO != null
          && datasetVersionDTO.getDatasetVersions() != null
          && !datasetVersionDTO.getDatasetVersions().isEmpty()) {
        for (DatasetVersion datasetVersion : datasetVersionDTO.getDatasetVersions()) {
          datasetVersionIds.add(datasetVersion.getId());
          listValueBuilder.addValues(
              Value.newBuilder().setStringValue(datasetVersion.getId()).build());
        }
      }

      Experiment lastUpdatedExperiment = null;
      if (!datasetVersionIds.isEmpty()) {
        KeyValueQuery keyValueQuery =
            KeyValueQuery.newBuilder()
                .setKey(ModelDBConstants.DATASETS + "." + ModelDBConstants.LINKED_ARTIFACT_ID)
                .setValue(Value.newBuilder().setListValue(listValueBuilder.build()).build())
                .setOperator(OperatorEnum.Operator.IN)
                .build();
        FindExperimentRuns findExperimentRuns =
            FindExperimentRuns.newBuilder().addPredicates(keyValueQuery).build();
        ExperimentRunPaginationDTO experimentRunPaginationDTO =
            experimentRunDAO.findExperimentRuns(findExperimentRuns);
        if (experimentRunPaginationDTO != null
            && experimentRunPaginationDTO.getExperimentRuns() != null
            && !experimentRunPaginationDTO.getExperimentRuns().isEmpty()) {
          List<ExperimentRun> experimentRuns = experimentRunPaginationDTO.getExperimentRuns();
          List<String> experimentIds = new ArrayList<>();
          for (ExperimentRun experimentRun : experimentRuns) {
            experimentIds.add(experimentRun.getExperimentId());
          }
          FindExperiments findExperiments =
              FindExperiments.newBuilder()
                  .addAllExperimentIds(experimentIds)
                  .setPageLimit(1)
                  .setPageNumber(1)
                  .setSortKey(ModelDBConstants.DATE_UPDATED)
                  .setAscending(false)
                  .build();
          ExperimentPaginationDTO experimentPaginationDTO =
              experimentDAO.findExperiments(findExperiments);
          if (experimentPaginationDTO.getExperiments() != null
              && !experimentPaginationDTO.getExperiments().isEmpty()) {
            lastUpdatedExperiment = experimentPaginationDTO.getExperiments().get(0);
          }
        }
      }

      if (lastUpdatedExperiment != null) {
        responseObserver.onNext(
            LastExperimentByDatasetId.Response.newBuilder()
                .setExperiment(lastUpdatedExperiment)
                .build());
      } else {
        responseObserver.onNext(LastExperimentByDatasetId.Response.newBuilder().build());
      }
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      responseObserver.onError(e);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
      ErrorCountResource.inc(e);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL.getNumber())
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(Any.pack(LastExperimentByDatasetId.Response.getDefaultInstance()))
              .build();
      responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
  }

  @Override
  public void getExperimentRunByDataset(
      GetExperimentRunByDataset request,
      StreamObserver<GetExperimentRunByDataset.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      if (request.getDatasetId().isEmpty()) {
        ModelDBUtils.logAndThrowError(
            ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST,
            Code.INVALID_ARGUMENT_VALUE,
            Any.pack(GetExperimentRunByDataset.Response.getDefaultInstance()));
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET, request.getDatasetId(), ModelDBServiceActions.READ);

      FindDatasetVersions findDatasetVersions =
          FindDatasetVersions.newBuilder()
              .setDatasetId(request.getDatasetId())
              .setIdsOnly(true)
              .build();
      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      DatasetVersionDTO datasetVersionDTO =
          datasetVersionDAO.findDatasetVersions(findDatasetVersions, userInfo);
      List<String> datasetVersionIds = new ArrayList<>();
      ListValue.Builder listValueBuilder = ListValue.newBuilder();
      if (datasetVersionDTO != null
          && datasetVersionDTO.getDatasetVersions() != null
          && !datasetVersionDTO.getDatasetVersions().isEmpty()) {
        for (DatasetVersion datasetVersion : datasetVersionDTO.getDatasetVersions()) {
          datasetVersionIds.add(datasetVersion.getId());
          listValueBuilder.addValues(
              Value.newBuilder().setStringValue(datasetVersion.getId()).build());
        }
      }

      List<ExperimentRun> experimentRuns = new ArrayList<>();
      if (!datasetVersionIds.isEmpty()) {
        KeyValueQuery keyValueQuery =
            KeyValueQuery.newBuilder()
                .setKey(ModelDBConstants.DATASETS + "." + ModelDBConstants.LINKED_ARTIFACT_ID)
                .setValue(Value.newBuilder().setListValue(listValueBuilder.build()).build())
                .setOperator(OperatorEnum.Operator.IN)
                .build();
        FindExperimentRuns findExperimentRuns =
            FindExperimentRuns.newBuilder().addPredicates(keyValueQuery).build();
        ExperimentRunPaginationDTO experimentRunPaginationDTO =
            experimentRunDAO.findExperimentRuns(findExperimentRuns);
        if (experimentRunPaginationDTO != null
            && experimentRunPaginationDTO.getExperimentRuns() != null
            && !experimentRunPaginationDTO.getExperimentRuns().isEmpty()) {
          experimentRuns.addAll(experimentRunPaginationDTO.getExperimentRuns());
        }
      }

      responseObserver.onNext(
          GetExperimentRunByDataset.Response.newBuilder()
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
              .addDetails(Any.pack(GetExperimentRunByDataset.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void setDatasetWorkspace(
      SetDatasetWorkspace request, StreamObserver<Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getWorkspaceName().isEmpty()) {
        errorMessage = "Dataset ID and Workspace not found in SetDatasetWorkspace request";
      } else if (request.getId().isEmpty()) {
        errorMessage = "Dataset ID not found in SetDatasetWorkspace request";
      } else if (request.getWorkspaceName().isEmpty()) {
        errorMessage = "Workspace not found in SetDatasetWorkspace request";
      }
      if (errorMessage != null) {
        ModelDBUtils.logAndThrowError(
            errorMessage,
            Code.INVALID_ARGUMENT_VALUE,
            Any.pack(SetDatasetWorkspace.Response.getDefaultInstance()));
      }
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET, request.getId(), ModelDBServiceActions.UPDATE);

      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      WorkspaceDTO workspaceDTO = null;
      if (userInfo != null) {
        workspaceDTO =
            roleService.getWorkspaceDTOByWorkspaceName(userInfo, request.getWorkspaceName());
      }
      Dataset dataset = datasetDAO.setDatasetWorkspace(request.getId(), workspaceDTO);
      responseObserver.onNext(
          SetDatasetWorkspace.Response.newBuilder().setDataset(dataset).build());
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
              .addDetails(Any.pack(SetDatasetWorkspace.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }
}
