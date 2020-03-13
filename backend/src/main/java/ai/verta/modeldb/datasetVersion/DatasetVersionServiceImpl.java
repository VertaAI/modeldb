package ai.verta.modeldb.datasetVersion;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.AddDatasetVersionAttributes;
import ai.verta.modeldb.AddDatasetVersionTags;
import ai.verta.modeldb.CreateDataset;
import ai.verta.modeldb.CreateDatasetVersion;
import ai.verta.modeldb.CreateDatasetVersion.Response;
import ai.verta.modeldb.Dataset;
import ai.verta.modeldb.DatasetVersion;
import ai.verta.modeldb.DatasetVersionServiceGrpc.DatasetVersionServiceImplBase;
import ai.verta.modeldb.DeleteDatasetVersion;
import ai.verta.modeldb.DeleteDatasetVersionAttributes;
import ai.verta.modeldb.DeleteDatasetVersionTags;
import ai.verta.modeldb.DeleteDatasetVersions;
import ai.verta.modeldb.FindDatasetVersions;
import ai.verta.modeldb.GetAllDatasetVersionsByDatasetId;
import ai.verta.modeldb.GetAttributes;
import ai.verta.modeldb.GetDatasetVersionById;
import ai.verta.modeldb.GetLatestDatasetVersionByDatasetId;
import ai.verta.modeldb.GetTags;
import ai.verta.modeldb.ModelDBAuthInterceptor;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.PathDatasetVersionInfo;
import ai.verta.modeldb.QueryDatasetVersionInfo;
import ai.verta.modeldb.RawDatasetVersionInfo;
import ai.verta.modeldb.SetDatasetVersionVisibilty;
import ai.verta.modeldb.UpdateDatasetVersionAttributes;
import ai.verta.modeldb.UpdateDatasetVersionDescription;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.dataset.DatasetDAO;
import ai.verta.modeldb.dto.DatasetVersionDTO;
import ai.verta.modeldb.monitoring.ErrorCountResource;
import ai.verta.modeldb.monitoring.QPSCountResource;
import ai.verta.modeldb.monitoring.RequestLatencyResource;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ModelResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.uac.UserInfo;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatasetVersionServiceImpl extends DatasetVersionServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(DatasetVersionServiceImpl.class);
  private AuthService authService;
  private RoleService roleService;
  private DatasetDAO datasetDAO;
  private DatasetVersionDAO datasetVersionDAO;

  public DatasetVersionServiceImpl(
      AuthService authService,
      RoleService roleService,
      DatasetDAO datasetDAO,
      DatasetVersionDAO datasetVersionDAO) {
    this.authService = authService;
    this.roleService = roleService;
    this.datasetDAO = datasetDAO;
    this.datasetVersionDAO = datasetVersionDAO;
  }

  /**
   * For getting datasetVersions that user has access to (either as owner or a collaborator), fetch
   * all datasetVersions of the requested datasetVersionIds then iterate that list and check if
   * datasetVersion is accessible or not. The list of accessible datasetVersionIDs is built and
   * returned by this method.
   *
   * @param requestedDatasetVersionIds : datasetVersion Ids
   * @return List<String> : list of accessible DatasetVersion Id
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  public List<String> getAccessibleDatasetVersionIDs(
      List<String> requestedDatasetVersionIds, ModelDBServiceActions modelDBServiceActions)
      throws InvalidProtocolBufferException {
    List<DatasetVersion> datasetVersionList =
        datasetVersionDAO.getDatasetVersionsByBatchIds(requestedDatasetVersionIds);
    if (datasetVersionList.size() == 0) {
      Status statusMessage =
          Status.newBuilder()
              .setCode(Code.PERMISSION_DENIED_VALUE)
              .setMessage(
                  ModelDBMessages.ACCESS_IS_DENIDE_DATASET_VERSION_ENTITIES_MSG
                      + requestedDatasetVersionIds)
              .build();
      throw StatusProto.toStatusRuntimeException(statusMessage);
    }
    Map<String, String> datasetIdDatasetVersionIdMap = new HashMap<>();
    for (DatasetVersion datasetVersion : datasetVersionList) {
      datasetIdDatasetVersionIdMap.put(datasetVersion.getId(), datasetVersion.getDatasetId());
    }
    Set<String> datasetIdSet = new HashSet<>(datasetIdDatasetVersionIdMap.values());

    List<String> accessibleDatasetVersionIds = new ArrayList<>();
    List<String> allowedDatasetIds;
    // Validate if current user has access to the entity or not
    if (datasetIdSet.size() == 1) {
      roleService.isSelfAllowed(
          ModelDBServiceResourceTypes.DATASET,
          modelDBServiceActions,
          new ArrayList<>(datasetIdSet).get(0));
      accessibleDatasetVersionIds.addAll(requestedDatasetVersionIds);
    } else {
      allowedDatasetIds =
          roleService.getSelfAllowedResources(
              ModelDBServiceResourceTypes.DATASET, modelDBServiceActions);
      // Validate if current user has access to the entity or not
      allowedDatasetIds.retainAll(requestedDatasetVersionIds);
      accessibleDatasetVersionIds.addAll(allowedDatasetIds);
    }
    return accessibleDatasetVersionIds;
  }

  /**
   * Create a DatasetVersion from the input parameters and logs it. Required input parameter :
   * datasetId which is the id of the parent dataset. Generate a random UUID for id set datasetID,
   * parentDatasetVersionID, Description, Attributes, Tags and Visibility from the request. set
   * current user as an owner
   *
   * <p>Based on the DatasetType initialize either {@link QueryDatasetVersionInfo}, {@link
   * RawDatasetVersionInfo} or {@link PathDatasetVersionInfo}.
   */
  @Override
  public void createDatasetVersion(
      CreateDatasetVersion request, StreamObserver<Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      /*Parameter validation*/
      if (request.getDatasetId().isEmpty()) {
        logAndThrowError(
            ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST,
            Code.INVALID_ARGUMENT_VALUE,
            Any.pack(CreateDatasetVersion.Response.getDefaultInstance()));
      }

      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET,
          request.getDatasetId(),
          ModelDBServiceActions.UPDATE);

      Dataset dataset = datasetDAO.getDatasetById(request.getDatasetId());
      if (dataset.getDatasetType() != request.getDatasetType()) {
        logAndThrowError(
            ModelDBMessages.DATASET_VERSION_TYPE_NOT_MATCH_WITH_DATSET_TYPE,
            Code.INVALID_ARGUMENT_VALUE,
            Any.pack(CreateDatasetVersion.Response.getDefaultInstance()));
      }

      /*Get the user info from the Context*/
      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      DatasetVersion datasetVersion =
          datasetVersionDAO.createDatasetVersion(request, dataset, userInfo);
      responseObserver.onNext(
          CreateDatasetVersion.Response.newBuilder().setDatasetVersion(datasetVersion).build());
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

  /** Fetch all the datasetVersions pointed to by dataset ID */
  @Override
  public void getAllDatasetVersionsByDatasetId(
      GetAllDatasetVersionsByDatasetId request,
      StreamObserver<GetAllDatasetVersionsByDatasetId.Response> responseObserver) {
    QPSCountResource.inc();
    LOGGER.trace(ModelDBMessages.GET_DATASET_VERSION_MSG);
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      /*Parameter validation*/

      if (request.getDatasetId().isEmpty()) {
        logAndThrowError(
            ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST,
            Code.INVALID_ARGUMENT_VALUE,
            Any.pack(GetAllDatasetVersionsByDatasetId.Response.getDefaultInstance()));
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET, request.getDatasetId(), ModelDBServiceActions.READ);

      /*Get the user info from the Context*/
      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      /*Get Data*/
      DatasetVersionDTO datasetVersionDTO =
          datasetVersionDAO.getDatasetVersions(
              request.getDatasetId(),
              request.getPageNumber(),
              request.getPageLimit(),
              request.getAscending(),
              request.getSortKey(),
              userInfo);
      /*Build response*/
      responseObserver.onNext(
          GetAllDatasetVersionsByDatasetId.Response.newBuilder()
              .addAllDatasetVersions(datasetVersionDTO.getDatasetVersions())
              .setTotalRecords(datasetVersionDTO.getTotalRecords())
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
              .addDetails(Any.pack(GetAllDatasetVersionsByDatasetId.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  /**
   * Deletes datasetVersion corresponding to the id. This only deltes it from the database. Required
   * input parameter : id
   */
  @Override
  public void deleteDatasetVersion(
      DeleteDatasetVersion request,
      StreamObserver<DeleteDatasetVersion.Response> responseObserver) {
    QPSCountResource.inc();
    LOGGER.trace(ModelDBMessages.GET_DATASET_VERSION_MSG);
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      /*Parameter validation*/
      if (request.getId().isEmpty()) {
        logAndThrowError(
            ModelDBMessages.DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST,
            Code.INVALID_ARGUMENT_VALUE,
            Any.pack(DeleteDatasetVersion.Response.getDefaultInstance()));
      }

      boolean deleteStatus = deleteDatasetVersions(Collections.singletonList(request.getId()));

      responseObserver.onNext(
          DeleteDatasetVersion.Response.newBuilder().setStatus(deleteStatus).build());
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
              .addDetails(Any.pack(DeleteDatasetVersion.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  /** get the datasetVersion with the most recent time logged */
  @Override
  public void getLatestDatasetVersionByDatasetId(
      GetLatestDatasetVersionByDatasetId request,
      StreamObserver<GetLatestDatasetVersionByDatasetId.Response> responseObserver) {
    QPSCountResource.inc();
    LOGGER.trace("Getting Latest dataset version.");
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      /*Parameter validation*/
      if (request.getDatasetId().isEmpty()) {
        logAndThrowError(
            ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST,
            Code.INVALID_ARGUMENT_VALUE,
            Any.pack(GetLatestDatasetVersionByDatasetId.Response.getDefaultInstance()));
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET, request.getDatasetId(), ModelDBServiceActions.READ);

      String sortKey =
          request.getSortKey().isEmpty() ? ModelDBConstants.TIME_LOGGED : request.getSortKey();

      /*Get the user info from the Context*/
      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      /*Get Data*/
      DatasetVersionDTO datasetVersionDTO =
          datasetVersionDAO.getDatasetVersions(
              request.getDatasetId(), 1, 1, request.getAscending(), sortKey, userInfo);
      if (datasetVersionDTO.getDatasetVersions().size() != 1) {
        logAndThrowError(
            ModelDBConstants.INTERNAL_ERROR,
            Code.INTERNAL_VALUE,
            Any.pack(GetLatestDatasetVersionByDatasetId.Response.getDefaultInstance()));
      }

      /*Build response*/
      responseObserver.onNext(
          GetLatestDatasetVersionByDatasetId.Response.newBuilder()
              .setDatasetVersion(datasetVersionDTO.getDatasetVersions().get(0))
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
              .setCode(Code.INTERNAL_VALUE)
              .setMessage(ModelDBConstants.INTERNAL_ERROR)
              .addDetails(
                  Any.pack(GetLatestDatasetVersionByDatasetId.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  /**
   * internal utility function to reduce lines of code
   *
   * @param errorMessage
   * @param errorCode
   * @param defaultResponse
   */
  // TODO: check if this could be bumped up to utils so it can be used across files
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

  /**
   * Fecth datasetVersion correponding to the id. <br>
   * Required input parameter : id
   */
  @Override
  public void getDatasetVersionById(
      GetDatasetVersionById request,
      StreamObserver<GetDatasetVersionById.Response> responseObserver) {
    QPSCountResource.inc();
    LOGGER.trace(ModelDBMessages.GET_DATASET_VERSION_MSG);
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      /*Parameter validation*/
      if (request.getId().isEmpty()) {
        logAndThrowError(
            ModelDBMessages.DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST,
            Code.INVALID_ARGUMENT_VALUE,
            Any.pack(GetDatasetVersionById.Response.getDefaultInstance()));
      }

      DatasetVersion datasetVersion = datasetVersionDAO.getDatasetVersion(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET,
          datasetVersion.getDatasetId(),
          ModelDBServiceActions.READ);

      /*Build response*/
      responseObserver.onNext(
          GetDatasetVersionById.Response.newBuilder().setDatasetVersion(datasetVersion).build());
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
              .addDetails(Any.pack(GetDatasetVersionById.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void findDatasetVersions(
      FindDatasetVersions request, StreamObserver<FindDatasetVersions.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();

      if (!request.getDatasetId().isEmpty()) {
        // Validate if current user has access to the entity or not
        roleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.DATASET,
            request.getDatasetId(),
            ModelDBServiceActions.READ);
      }

      if (!request.getDatasetVersionIdsList().isEmpty()) {
        List<String> accessibleExperimentIds =
            getAccessibleDatasetVersionIDs(
                request.getDatasetVersionIdsList(), ModelDBServiceActions.READ);
        request =
            request
                .toBuilder()
                .clearDatasetVersionIds()
                .addAllDatasetVersionIds(accessibleExperimentIds)
                .build();
      }

      DatasetVersionDTO datasetVersionPaginationDTO =
          datasetVersionDAO.findDatasetVersions(request, userInfo);
      responseObserver.onNext(
          FindDatasetVersions.Response.newBuilder()
              .addAllDatasetVersions(datasetVersionPaginationDTO.getDatasetVersions())
              .setTotalRecords(datasetVersionPaginationDTO.getTotalRecords())
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
              .addDetails(Any.pack(FindDatasetVersions.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void updateDatasetVersionDescription(
      UpdateDatasetVersionDescription request,
      StreamObserver<UpdateDatasetVersionDescription.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getDescription().isEmpty()) {
        errorMessage =
            "DatasetVersion ID and DatasetVersion description not found in UpdateDatasetVersionDescription request";
      } else if (request.getId().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST;
      } else if (request.getDescription().isEmpty()) {
        errorMessage =
            "DatasetVersion description not found in UpdateDatasetVersionDescription request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(UpdateDatasetVersionDescription.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      DatasetVersion datasetVersion = datasetVersionDAO.getDatasetVersion(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET,
          datasetVersion.getDatasetId(),
          ModelDBServiceActions.UPDATE);

      datasetVersion =
          datasetVersionDAO.updateDatasetVersionDescription(
              request.getId(), request.getDescription());

      responseObserver.onNext(
          UpdateDatasetVersionDescription.Response.newBuilder()
              .setDatasetVersion(datasetVersion)
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
              .addDetails(Any.pack(UpdateDatasetVersionDescription.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void addDatasetVersionTags(
      AddDatasetVersionTags request,
      StreamObserver<AddDatasetVersionTags.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getTagsList().isEmpty()) {
        errorMessage =
            "DatasetVersion ID and DatasetVersion tags not found in AddDatasetVersionTags request";
      } else if (request.getId().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST;
      } else if (request.getTagsList().isEmpty()) {
        errorMessage = "DatasetVersion tags not found in AddDatasetVersionTags request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(AddDatasetVersionTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      DatasetVersion datasetVersion = datasetVersionDAO.getDatasetVersion(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET,
          datasetVersion.getDatasetId(),
          ModelDBServiceActions.READ);

      DatasetVersion updatedDatasetVersion =
          datasetVersionDAO.addDatasetVersionTags(
              request.getId(), ModelDBUtils.checkEntityTagsLength(request.getTagsList()));

      responseObserver.onNext(
          AddDatasetVersionTags.Response.newBuilder()
              .setDatasetVersion(updatedDatasetVersion)
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
              .addDetails(Any.pack(AddDatasetVersionTags.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getDatasetVersionTags(
      GetTags request, StreamObserver<GetTags.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        LOGGER.warn(ModelDBMessages.DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(ModelDBMessages.DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST)
                .addDetails(Any.pack(GetTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      DatasetVersion datasetVersion = datasetVersionDAO.getDatasetVersion(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET,
          datasetVersion.getDatasetId(),
          ModelDBServiceActions.READ);

      List<String> tags = datasetVersionDAO.getDatasetVersionTags(request.getId());
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
  public void deleteDatasetVersionTags(
      DeleteDatasetVersionTags request,
      StreamObserver<DeleteDatasetVersionTags.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getTagsList().isEmpty() && !request.getDeleteAll()) {
        errorMessage =
            "DatasetVersion ID and DatasetVersion tags not found in DeleteDatasetVersionTags request";
      } else if (request.getId().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST;
      } else if (request.getTagsList().isEmpty() && !request.getDeleteAll()) {
        errorMessage = "DatasetVersion tags not found in DeleteDatasetVersionTags request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteDatasetVersionTags.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      DatasetVersion datasetVersion = datasetVersionDAO.getDatasetVersion(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET,
          datasetVersion.getDatasetId(),
          ModelDBServiceActions.UPDATE);

      DatasetVersion updatedDatasetVersion =
          datasetVersionDAO.deleteDatasetVersionTags(
              request.getId(), request.getTagsList(), request.getDeleteAll());

      responseObserver.onNext(
          DeleteDatasetVersionTags.Response.newBuilder()
              .setDatasetVersion(updatedDatasetVersion)
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
              .addDetails(Any.pack(DeleteDatasetVersionTags.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void addDatasetVersionAttributes(
      AddDatasetVersionAttributes request,
      StreamObserver<AddDatasetVersionAttributes.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getAttributesList().isEmpty()) {
        errorMessage =
            "DatasetVersion ID and Attribute list not found in AddDatasetVersionAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST;
      } else if (request.getAttributesList().isEmpty()) {
        errorMessage = "Attribute list not found in AddDatasetVersionAttributes request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(AddDatasetVersionAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      DatasetVersion datasetVersion = datasetVersionDAO.getDatasetVersion(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET,
          datasetVersion.getDatasetId(),
          ModelDBServiceActions.READ);

      DatasetVersion updatedDatasetVersion =
          datasetVersionDAO.addDatasetVersionAttributes(
              request.getId(), request.getAttributesList());
      responseObserver.onNext(
          AddDatasetVersionAttributes.Response.newBuilder()
              .setDatasetVersion(updatedDatasetVersion)
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
              .addDetails(Any.pack(AddDatasetVersionAttributes.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void updateDatasetVersionAttributes(
      UpdateDatasetVersionAttributes request,
      StreamObserver<UpdateDatasetVersionAttributes.Response> responseObserver) {
    QPSCountResource.inc();

    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getAttribute().getKey().isEmpty()) {
        errorMessage =
            "DatasetVersion ID and attribute key not found in UpdateDatasetVersionAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST;
      } else if (request.getAttribute().getKey().isEmpty()) {
        errorMessage = "Attribute key not found in UpdateDatasetVersionAttributes request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(UpdateDatasetVersionAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      DatasetVersion datasetVersion = datasetVersionDAO.getDatasetVersion(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET,
          datasetVersion.getDatasetId(),
          ModelDBServiceActions.UPDATE);

      DatasetVersion updatedDatasetVersion =
          datasetVersionDAO.updateDatasetVersionAttributes(request.getId(), request.getAttribute());
      responseObserver.onNext(
          UpdateDatasetVersionAttributes.Response.newBuilder()
              .setDatasetVersion(updatedDatasetVersion)
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
              .addDetails(Any.pack(UpdateDatasetVersionAttributes.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getDatasetVersionAttributes(
      GetAttributes request, StreamObserver<GetAttributes.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty()
          && request.getAttributeKeysList().isEmpty()
          && !request.getGetAll()) {
        errorMessage =
            "DatasetVersion ID and DatasetVersion attribute keys not found in GetAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST;
      } else if (request.getAttributeKeysList().isEmpty() && !request.getGetAll()) {
        errorMessage = "DatasetVersion attribute keys not found in GetAttributes request";
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

      DatasetVersion datasetVersion = datasetVersionDAO.getDatasetVersion(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET,
          datasetVersion.getDatasetId(),
          ModelDBServiceActions.READ);

      List<KeyValue> attributes =
          datasetVersionDAO.getDatasetVersionAttributes(
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
  public void deleteDatasetVersionAttributes(
      DeleteDatasetVersionAttributes request,
      StreamObserver<DeleteDatasetVersionAttributes.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty()
          && request.getAttributeKeysList().isEmpty()
          && !request.getDeleteAll()) {
        errorMessage =
            "DatasetVersion ID and DatasetVersion attribute keys not found in DeleteDatasetVersionAttributes request";
      } else if (request.getId().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST;
      } else if (request.getAttributeKeysList().isEmpty() && !request.getDeleteAll()) {
        errorMessage =
            "DatasetVersion attribute keys not found in DeleteDatasetVersionAttributes request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(DeleteDatasetVersionAttributes.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      DatasetVersion datasetVersion = datasetVersionDAO.getDatasetVersion(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET,
          datasetVersion.getDatasetId(),
          ModelDBServiceActions.UPDATE);

      DatasetVersion updatedDatasetVersion =
          datasetVersionDAO.deleteDatasetVersionAttributes(
              request.getId(), request.getAttributeKeysList(), request.getDeleteAll());
      responseObserver.onNext(
          DeleteDatasetVersionAttributes.Response.newBuilder()
              .setDatasetVersion(updatedDatasetVersion)
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
              .addDetails(Any.pack(DeleteDatasetVersionAttributes.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void setDatasetVersionVisibility(
      SetDatasetVersionVisibilty request,
      StreamObserver<SetDatasetVersionVisibilty.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        LOGGER.warn(ModelDBMessages.DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(ModelDBMessages.DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST)
                .addDetails(Any.pack(SetDatasetVersionVisibilty.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      DatasetVersion datasetVersion = datasetVersionDAO.getDatasetVersion(request.getId());
      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.DATASET,
          datasetVersion.getDatasetId(),
          ModelDBServiceActions.READ);

      datasetVersion =
          datasetVersionDAO.setDatasetVersionVisibility(
              request.getId(), request.getDatasetVersionVisibility());
      responseObserver.onNext(
          SetDatasetVersionVisibilty.Response.newBuilder()
              .setDatasetVersion(datasetVersion)
              .build());
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
              .addDetails(Any.pack(SetDatasetVersionVisibilty.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void deleteDatasetVersions(
      DeleteDatasetVersions request,
      StreamObserver<DeleteDatasetVersions.Response> responseObserver) {
    QPSCountResource.inc();
    LOGGER.trace("Deleting dataset version.");
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      /*Parameter validation*/
      if (request.getIdsList().isEmpty()) {
        logAndThrowError(
            ModelDBMessages.DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST,
            Code.INVALID_ARGUMENT_VALUE,
            Any.pack(DeleteDatasetVersion.Response.getDefaultInstance()));
      }

      boolean deleteStatus = deleteDatasetVersions(request.getIdsList());

      responseObserver.onNext(
          DeleteDatasetVersions.Response.newBuilder().setStatus(deleteStatus).build());
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
              .addDetails(Any.pack(DeleteDatasetVersions.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  private boolean deleteDatasetVersions(List<String> datasetVersionIds)
      throws InvalidProtocolBufferException {
    List<String> accessibleDatasetVersionIds =
        getAccessibleDatasetVersionIDs(datasetVersionIds, ModelDBServiceActions.UPDATE);
    if (accessibleDatasetVersionIds.isEmpty()) {
      Status statusMessage =
          Status.newBuilder()
              .setCode(Code.PERMISSION_DENIED_VALUE)
              .setMessage(
                  ModelDBMessages.ACCESS_IS_DENIDE_DATASET_VERSION_ENTITIES_MSG + datasetVersionIds)
              .build();
      throw StatusProto.toStatusRuntimeException(statusMessage);
    }
    return datasetVersionDAO.deleteDatasetVersions(datasetVersionIds, true);
  }
}
