package ai.verta.modeldb.datasetVersion;

import ai.verta.common.KeyValue;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.AddDatasetVersionAttributes;
import ai.verta.modeldb.AddDatasetVersionTags;
import ai.verta.modeldb.CreateDatasetVersion;
import ai.verta.modeldb.CreateDatasetVersion.Response;
import ai.verta.modeldb.DatasetVersion;
import ai.verta.modeldb.DatasetVersionServiceGrpc.DatasetVersionServiceImplBase;
import ai.verta.modeldb.DeleteDatasetVersion;
import ai.verta.modeldb.DeleteDatasetVersionAttributes;
import ai.verta.modeldb.DeleteDatasetVersionTags;
import ai.verta.modeldb.DeleteDatasetVersions;
import ai.verta.modeldb.FindDatasetVersions;
import ai.verta.modeldb.GetAllDatasetVersionsByDatasetId;
import ai.verta.modeldb.GetAttributes;
import ai.verta.modeldb.GetLatestDatasetVersionByDatasetId;
import ai.verta.modeldb.ModelDBAuthInterceptor;
import ai.verta.modeldb.ModelDBException;
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
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.monitoring.QPSCountResource;
import ai.verta.modeldb.monitoring.RequestLatencyResource;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.BlobDAO;
import ai.verta.modeldb.versioning.Commit;
import ai.verta.modeldb.versioning.CommitDAO;
import ai.verta.modeldb.versioning.CreateCommitRequest;
import ai.verta.modeldb.versioning.DeleteCommitRequest;
import ai.verta.modeldb.versioning.ListCommitsRequest;
import ai.verta.modeldb.versioning.Pagination;
import ai.verta.modeldb.versioning.RepositoryDAO;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.UserInfo;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatasetVersionServiceImpl extends DatasetVersionServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(DatasetVersionServiceImpl.class);
  private AuthService authService;
  private RoleService roleService;
  private DatasetDAO datasetDAO;
  private DatasetVersionDAO datasetVersionDAO;
  private final RepositoryDAO repositoryDAO;
  private final CommitDAO commitDAO;
  private final BlobDAO blobDAO;
  private final MetadataDAO metadataDAO;

  public DatasetVersionServiceImpl(
      AuthService authService,
      RoleService roleService,
      DatasetDAO datasetDAO,
      DatasetVersionDAO datasetVersionDAO,
      RepositoryDAO repositoryDAO,
      CommitDAO commitDAO,
      BlobDAO blobDAO,
      MetadataDAO metadataDAO) {
    this.authService = authService;
    this.roleService = roleService;
    this.datasetDAO = datasetDAO;
    this.datasetVersionDAO = datasetVersionDAO;
    this.repositoryDAO = repositoryDAO;
    this.commitDAO = commitDAO;
    this.blobDAO = blobDAO;
    this.metadataDAO = metadataDAO;
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
      } else if (request.getParentId().isEmpty()) {
        throw new ModelDBException(
            "Parent datasetVersion id not found in the CreateDatasetVersion",
            io.grpc.Status.Code.INVALID_ARGUMENT);
      }

      /*Get the user info from the Context*/
      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      DatasetVersion datasetVersion =
          datasetVersionDAO.getDatasetVersionFromRequest(authService, request, userInfo);
      RepositoryIdentification repositoryIdentification =
          RepositoryIdentification.newBuilder()
              .setRepoId(Long.parseLong(request.getDatasetId()))
              .build();
      RepositoryEntity repositoryEntity =
          repositoryDAO.getRepositoryById(repositoryIdentification, true);
      CreateCommitRequest.Response createCommitResponse =
          commitDAO.setCommitFromDatasetVersion(
              datasetVersion, blobDAO, metadataDAO, repositoryEntity);
      datasetVersion =
          blobDAO.convertToDatasetVersion(
              metadataDAO, repositoryEntity, createCommitResponse.getCommit().getCommitSha());
      responseObserver.onNext(
          CreateDatasetVersion.Response.newBuilder().setDatasetVersion(datasetVersion).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, CreateDatasetVersion.Response.getDefaultInstance());
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
      DatasetVersionDTO datasetVersionDTO =
          getDatasetVersionDTOByDatasetId(
              request.getDatasetId(), request.getPageNumber(), request.getPageLimit());

      /*Build response*/
      responseObserver.onNext(
          GetAllDatasetVersionsByDatasetId.Response.newBuilder()
              .addAllDatasetVersions(datasetVersionDTO.getDatasetVersions())
              .setTotalRecords(datasetVersionDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, GetAllDatasetVersionsByDatasetId.Response.getDefaultInstance());
    }
  }

  private DatasetVersionDTO getDatasetVersionDTOByDatasetId(
      String datasetId, int pageNumber, int pageLimit)
      throws InvalidProtocolBufferException, ModelDBException {

    /*Get Data*/
    RepositoryIdentification repositoryIdentification =
        RepositoryIdentification.newBuilder().setRepoId(Long.parseLong(datasetId)).build();
    ListCommitsRequest.Builder listCommitsRequest =
        ListCommitsRequest.newBuilder().setRepositoryId(repositoryIdentification);
    if (pageLimit > 0 && pageNumber > 0) {
      Pagination pagination =
          Pagination.newBuilder().setPageLimit(pageLimit).setPageNumber(pageNumber).build();
      listCommitsRequest.setPagination(pagination);
    }
    ListCommitsRequest.Response listCommitsResponse =
        commitDAO.listCommits(
            listCommitsRequest.build(),
            (session -> repositoryDAO.getRepositoryById(session, repositoryIdentification)));

    List<DatasetVersion> datasetVersions = new ArrayList<>();
    long totalRecords = listCommitsResponse.getTotalRecords();
    totalRecords = totalRecords > 0 ? totalRecords - 1 : totalRecords;

    RepositoryEntity repositoryEntity =
        repositoryDAO.getRepositoryById(repositoryIdentification, false);
    for (Commit commit : listCommitsResponse.getCommitsList()) {
      if (commit.getParentShasList().isEmpty()) {
        continue;
      }
      datasetVersions.add(
          blobDAO.convertToDatasetVersion(metadataDAO, repositoryEntity, commit.getCommitSha()));
    }

    DatasetVersionDTO datasetVersionDTO = new DatasetVersionDTO();
    datasetVersionDTO.setDatasetVersions(datasetVersions);
    datasetVersionDTO.setTotalRecords(totalRecords);
    return datasetVersionDTO;
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
      } else if (request.getDatasetId().isEmpty()) {
        logAndThrowError(
            ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST,
            Code.INVALID_ARGUMENT_VALUE,
            Any.pack(DeleteDatasetVersion.Response.getDefaultInstance()));
      }

      DeleteCommitRequest deleteCommitRequest =
          DeleteCommitRequest.newBuilder()
              .setCommitSha(request.getId())
              .setRepositoryId(
                  RepositoryIdentification.newBuilder()
                      .setRepoId(Long.parseLong(request.getDatasetId()))
                      .build())
              .build();
      commitDAO.deleteCommit(deleteCommitRequest, repositoryDAO);

      responseObserver.onNext(DeleteDatasetVersion.Response.newBuilder().setStatus(true).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, DeleteDatasetVersion.Response.getDefaultInstance());
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

      // TODO: Implement sort key in future
      /*String sortKey =
      request.getSortKey().isEmpty() ? ModelDBConstants.TIME_LOGGED : request.getSortKey();*/

      DatasetVersionDTO datasetVersionDTO =
          getDatasetVersionDTOByDatasetId(request.getDatasetId(), 1, 1);

      if (datasetVersionDTO.getDatasetVersions().size() != 1) {
        logAndThrowError(
            "No datasetVersion found for dataset '" + request.getDatasetId(),
            Code.NOT_FOUND_VALUE,
            Any.pack(GetLatestDatasetVersionByDatasetId.Response.getDefaultInstance()));
      }

      /*Build response*/
      responseObserver.onNext(
          GetLatestDatasetVersionByDatasetId.Response.newBuilder()
              .setDatasetVersion(datasetVersionDTO.getDatasetVersions().get(0))
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, GetLatestDatasetVersionByDatasetId.Response.getDefaultInstance());
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

      DatasetVersionDTO datasetVersionPaginationDTO =
          datasetVersionDAO.findDatasetVersions(datasetDAO, request, userInfo);
      responseObserver.onNext(
          FindDatasetVersions.Response.newBuilder()
              .addAllDatasetVersions(datasetVersionPaginationDTO.getDatasetVersions())
              .setTotalRecords(datasetVersionPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, FindDatasetVersions.Response.getDefaultInstance());
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

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, UpdateDatasetVersionDescription.Response.getDefaultInstance());
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
      if (request.getId().isEmpty()
          && request.getTagsList().isEmpty()
          && request.getDatasetId().isEmpty()) {
        errorMessage =
            "DatasetVersion ID and DatasetVersion tags and DatasetId not found in AddDatasetVersionTags request";
      } else if (request.getId().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST;
      } else if (request.getTagsList().isEmpty()) {
        errorMessage = "DatasetVersion tags not found in AddDatasetVersionTags request";
      } else if (request.getDatasetId().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST;
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

      RepositoryIdentification repositoryIdentification =
          RepositoryIdentification.newBuilder()
              .setRepoId(Long.parseLong(request.getDatasetId()))
              .build();
      RepositoryEntity repositoryEntity =
          repositoryDAO.getRepositoryById(repositoryIdentification, true);
      commitDAO.addDeleteDatasetVersionTags(
          metadataDAO, true, repositoryEntity, request.getId(), request.getTagsList(), false);

      DatasetVersion datasetVersion =
          blobDAO.convertToDatasetVersion(metadataDAO, repositoryEntity, request.getId());

      responseObserver.onNext(
          AddDatasetVersionTags.Response.newBuilder().setDatasetVersion(datasetVersion).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, AddDatasetVersionTags.Response.getDefaultInstance());
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
      if (request.getId().isEmpty()
          && request.getTagsList().isEmpty()
          && !request.getDeleteAll()
          && request.getDatasetId().isEmpty()) {
        errorMessage =
            "DatasetVersion ID and DatasetVersion tags and DatasetId not found in DeleteDatasetVersionTags request";
      } else if (request.getId().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST;
      } else if (request.getTagsList().isEmpty() && !request.getDeleteAll()) {
        errorMessage = "DatasetVersion tags not found in DeleteDatasetVersionTags request";
      } else if (request.getDatasetId().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST;
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

      RepositoryIdentification repositoryIdentification =
          RepositoryIdentification.newBuilder()
              .setRepoId(Long.parseLong(request.getDatasetId()))
              .build();
      RepositoryEntity repositoryEntity =
          repositoryDAO.getRepositoryById(repositoryIdentification, true);
      commitDAO.addDeleteDatasetVersionTags(
          metadataDAO,
          false,
          repositoryEntity,
          request.getId(),
          request.getTagsList(),
          request.getDeleteAll());
      DatasetVersion datasetVersion =
          blobDAO.convertToDatasetVersion(metadataDAO, repositoryEntity, request.getId());

      responseObserver.onNext(
          DeleteDatasetVersionTags.Response.newBuilder().setDatasetVersion(datasetVersion).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, DeleteDatasetVersionTags.Response.getDefaultInstance());
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

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, AddDatasetVersionAttributes.Response.getDefaultInstance());
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

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, UpdateDatasetVersionAttributes.Response.getDefaultInstance());
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

    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, GetAttributes.Response.getDefaultInstance());
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

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, DeleteDatasetVersionAttributes.Response.getDefaultInstance());
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
    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, SetDatasetVersionVisibilty.Response.getDefaultInstance());
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
            Any.pack(DeleteDatasetVersions.Response.getDefaultInstance()));
      } else if (request.getDatasetId().isEmpty()) {
        logAndThrowError(
            ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST,
            Code.INVALID_ARGUMENT_VALUE,
            Any.pack(DeleteDatasetVersions.Response.getDefaultInstance()));
      }

      boolean deleteStatus =
          commitDAO.deleteCommits(
              RepositoryIdentification.newBuilder()
                  .setRepoId(Long.parseLong(request.getDatasetId()))
                  .build(),
              request.getIdsList(),
              repositoryDAO);

      responseObserver.onNext(
          DeleteDatasetVersions.Response.newBuilder().setStatus(deleteStatus).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, DeleteDatasetVersions.Response.getDefaultInstance());
    }
  }
}
