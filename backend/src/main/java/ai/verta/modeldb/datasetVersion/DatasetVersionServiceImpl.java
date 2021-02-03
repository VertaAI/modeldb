package ai.verta.modeldb.datasetVersion;

import ai.verta.common.KeyValue;
import ai.verta.common.Pagination;
import ai.verta.modeldb.*;
import ai.verta.modeldb.CreateDatasetVersion.Response;
import ai.verta.modeldb.DatasetVersionServiceGrpc.DatasetVersionServiceImplBase;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.audit_log.AuditLogLocalDAO;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.dto.CommitPaginationDTO;
import ai.verta.modeldb.dto.DatasetVersionDTO;
import ai.verta.modeldb.entities.audit_log.AuditLogLocalEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEnums;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.*;
import ai.verta.uac.ServiceEnum;
import ai.verta.uac.UserInfo;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Code;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatasetVersionServiceImpl extends DatasetVersionServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(DatasetVersionServiceImpl.class);
  private final AuthService authService;
  private final RepositoryDAO repositoryDAO;
  private final CommitDAO commitDAO;
  private final BlobDAO blobDAO;
  private final MetadataDAO metadataDAO;
  private final ArtifactStoreDAO artifactStoreDAO;
  private final AuditLogLocalDAO auditLogLocalDAO;
  private static final String SERVICE_NAME =
      String.format("%s.%s", ModelDBConstants.SERVICE_NAME, ModelDBConstants.DATASET_VERSION);

  public DatasetVersionServiceImpl(ServiceSet serviceSet, DAOSet daoSet) {
    this.authService = serviceSet.authService;
    this.repositoryDAO = daoSet.repositoryDAO;
    this.commitDAO = daoSet.commitDAO;
    this.blobDAO = daoSet.blobDAO;
    this.metadataDAO = daoSet.metadataDAO;
    this.artifactStoreDAO = daoSet.artifactStoreDAO;
    this.auditLogLocalDAO = daoSet.auditLogLocalDAO;
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
                        ModelDBConstants.DATASET_VERSION,
                        ServiceEnum.Service.MODELDB_SERVICE.name(),
                        metadataBlob))
            .collect(Collectors.toList());
    if (!auditLogLocalEntities.isEmpty()) {
      auditLogLocalDAO.saveAuditLogs(auditLogLocalEntities);
    }
  }

  private DatasetVersion getDatasetVersionFromRequest(
      AuthService authService, CreateDatasetVersion request, UserInfo userInfo)
      throws ModelDBException {
    DatasetVersion.Builder datasetVersionBuilder =
        DatasetVersion.newBuilder()
            .setVersion(request.getVersion())
            .setDatasetId(request.getDatasetId())
            .setDescription(request.getDescription())
            .addAllTags(request.getTagsList())
            .addAllAttributes(request.getAttributesList());

    if (request.getTimeCreated() != 0L) {
      datasetVersionBuilder.setTimeLogged(request.getTimeCreated());
      datasetVersionBuilder.setTimeUpdated(request.getTimeCreated());
    } else {
      datasetVersionBuilder.setTimeLogged(Calendar.getInstance().getTimeInMillis());
      datasetVersionBuilder.setTimeUpdated(Calendar.getInstance().getTimeInMillis());
    }

    if (!request.getParentId().isEmpty()) {
      datasetVersionBuilder.setParentId(request.getParentId());
    }

    if (userInfo != null) {
      datasetVersionBuilder.setOwner(authService.getVertaIdFromUserInfo(userInfo));
    }

    if (!request.hasPathDatasetVersionInfo() && !request.hasDatasetBlob()) {
      LOGGER.info("Request {}", request);
      throw new ModelDBException("Not supported", io.grpc.Status.Code.UNIMPLEMENTED);
    }
    datasetVersionBuilder.setPathDatasetVersionInfo(request.getPathDatasetVersionInfo());

    if (request.hasDatasetBlob()) {
      datasetVersionBuilder.setDatasetBlob(request.getDatasetBlob());
    }

    return datasetVersionBuilder.build();
  }

  /**
   * Create a DatasetVersion from the input parameters and logs it. Required input parameter :
   * datasetId which is the id of the parent dataset. Generate a random UUID for id set datasetID,
   * parentDatasetVersionID, Description, Attributes, Tags and Visibility from the request. set
   * current user as an owner
   *
   * <p>Based on the DatasetType initialize {@link PathDatasetVersionInfo}.
   */
  @Override
  public void createDatasetVersion(
      CreateDatasetVersion request, StreamObserver<Response> responseObserver) {
    try {
      /*Parameter validation*/
      if (request.getDatasetId().isEmpty()) {
        throw new InvalidArgumentException(ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST);
      }

      /*Get the user info from the Context*/
      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      DatasetVersion datasetVersion = getDatasetVersionFromRequest(authService, request, userInfo);

      RepositoryIdentification repositoryIdentification =
          RepositoryIdentification.newBuilder()
              .setRepoId(Long.parseLong(request.getDatasetId()))
              .build();

      RepositoryEntity repositoryEntity =
          repositoryDAO.getProtectedRepositoryById(repositoryIdentification, true);
      CreateCommitRequest.Response createCommitResponse =
          commitDAO.setCommitFromDatasetVersion(
              datasetVersion, repositoryDAO, blobDAO, metadataDAO, repositoryEntity);
      datasetVersion =
          blobDAO.convertToDatasetVersion(
              repositoryDAO,
              metadataDAO,
              repositoryEntity,
              createCommitResponse.getCommit().getCommitSha(),
              true);
      saveAuditLogs(
          userInfo, ModelDBConstants.CREATE, Collections.singletonList(datasetVersion.getId()), "");
      responseObserver.onNext(
          CreateDatasetVersion.Response.newBuilder().setDatasetVersion(datasetVersion).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, CreateDatasetVersion.Response.getDefaultInstance());
    }
  }

  /** Fetch all the datasetVersions pointed to by dataset ID */
  @Override
  public void getAllDatasetVersionsByDatasetId(
      GetAllDatasetVersionsByDatasetId request,
      StreamObserver<GetAllDatasetVersionsByDatasetId.Response> responseObserver) {
    try {
      /*Parameter validation*/
      if (request.getDatasetId().isEmpty()) {
        throw new InvalidArgumentException(ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST);
      }
      DatasetVersionDTO datasetVersionDTO =
          getDatasetVersionDTOByDatasetId(
              request.getDatasetId(),
              request.getPageNumber(),
              request.getPageLimit(),
              request.getAscending());

      /*Build response*/
      responseObserver.onNext(
          GetAllDatasetVersionsByDatasetId.Response.newBuilder()
              .addAllDatasetVersions(datasetVersionDTO.getDatasetVersions())
              .setTotalRecords(datasetVersionDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetAllDatasetVersionsByDatasetId.Response.getDefaultInstance());
    }
  }

  private DatasetVersionDTO getDatasetVersionDTOByDatasetId(
      String datasetId, int pageNumber, int pageLimit, boolean ascending)
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
            (session ->
                repositoryDAO.getRepositoryById(
                    session,
                    repositoryIdentification,
                    false,
                    false,
                    RepositoryEnums.RepositoryTypeEnum.DATASET)),
            ascending);

    long totalRecords = listCommitsResponse.getTotalRecords();
    totalRecords = totalRecords > 0 ? totalRecords - 1 : totalRecords;

    RepositoryEntity repositoryEntity =
        repositoryDAO.getProtectedRepositoryById(repositoryIdentification, false);
    List<DatasetVersion> datasetVersions =
        new ArrayList<>(
            convertRepoDatasetVersions(repositoryEntity, listCommitsResponse.getCommitsList()));

    DatasetVersionDTO datasetVersionDTO = new DatasetVersionDTO();
    datasetVersionDTO.setDatasetVersions(datasetVersions);
    datasetVersionDTO.setTotalRecords(totalRecords);
    return datasetVersionDTO;
  }

  private List<DatasetVersion> convertRepoDatasetVersions(
      RepositoryEntity repositoryEntity, List<Commit> commitList) throws ModelDBException {
    List<DatasetVersion> datasetVersions = new ArrayList<>();
    for (Commit commit : commitList) {
      if (commit.getParentShasList().isEmpty()) {
        continue;
      }
      datasetVersions.add(
          blobDAO.convertToDatasetVersion(
              repositoryDAO, metadataDAO, repositoryEntity, commit.getCommitSha(), false));
    }
    return datasetVersions;
  }

  /**
   * Deletes datasetVersion corresponding to the id. This only deltes it from the database. Required
   * input parameter : id
   */
  @Override
  public void deleteDatasetVersion(
      DeleteDatasetVersion request,
      StreamObserver<DeleteDatasetVersion.Response> responseObserver) {
    try {
      /*Parameter validation*/
      if (request.getId().isEmpty()) {
        throw new InvalidArgumentException(ModelDBMessages.DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST);
      }

      commitDAO.deleteDatasetVersions(
          request.getDatasetId().isEmpty()
              ? null
              : RepositoryIdentification.newBuilder()
                  .setRepoId(Long.parseLong(request.getDatasetId()))
                  .build(),
          Collections.singletonList(request.getId()),
          repositoryDAO);
      saveAuditLogs(null, ModelDBConstants.DELETE, Collections.singletonList(request.getId()), "");
      responseObserver.onNext(DeleteDatasetVersion.Response.getDefaultInstance());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteDatasetVersion.Response.getDefaultInstance());
    }
  }

  /** get the datasetVersion with the most recent time logged */
  @Override
  public void getLatestDatasetVersionByDatasetId(
      GetLatestDatasetVersionByDatasetId request,
      StreamObserver<GetLatestDatasetVersionByDatasetId.Response> responseObserver) {
    try {
      /*Parameter validation*/
      if (request.getDatasetId().isEmpty()) {
        throw new InvalidArgumentException(ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST);
      }

      UserInfo currentLoginUserInfo = authService.getCurrentLoginUserInfo();

      FindRepositoriesBlobs.Builder findRepositoriesBlobs =
          FindRepositoriesBlobs.newBuilder()
              .addRepoIds(Integer.parseInt(request.getDatasetId()))
              .setPageLimit(1)
              .setPageNumber(1);
      String sortKey =
          request.getSortKey().isEmpty() ? ModelDBConstants.DATE_CREATED : request.getSortKey();
      CommitPaginationDTO commitPaginationDTO =
          commitDAO.findCommits(
              findRepositoriesBlobs.build(),
              currentLoginUserInfo,
              false,
              false,
              true,
              sortKey,
              request.getAscending());
      if (commitPaginationDTO.getCommitEntities().size() != 1) {
        throw new NotFoundException(
            "No datasetVersion found for dataset '" + request.getDatasetId() + "'");
      }

      RepositoryEntity repositoryEntity = null;
      if (!request.getDatasetId().isEmpty()) {
        RepositoryIdentification repositoryIdentification =
            RepositoryIdentification.newBuilder()
                .setRepoId(Long.parseLong(request.getDatasetId()))
                .build();
        repositoryEntity =
            repositoryDAO.getProtectedRepositoryById(repositoryIdentification, false);
      }
      List<DatasetVersion> datasetVersions =
          new ArrayList<>(
              convertRepoDatasetVersions(repositoryEntity, commitPaginationDTO.getCommits()));

      if (datasetVersions.size() != 1) {
        throw new NotFoundException(
            "No datasetVersion found for dataset '" + request.getDatasetId() + "'");
      }
      /*Build response*/
      responseObserver.onNext(
          GetLatestDatasetVersionByDatasetId.Response.newBuilder()
              .setDatasetVersion(datasetVersions.get(0))
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetLatestDatasetVersionByDatasetId.Response.getDefaultInstance());
    }
  }

  // FIXME: moving to versioning based datset versions we only support KeyValue Query on Attribute
  // to be String
  @Override
  public void findDatasetVersions(
      FindDatasetVersions request, StreamObserver<FindDatasetVersions.Response> responseObserver) {
    try {
      UserInfo currentLoginUserInfo = authService.getCurrentLoginUserInfo();
      FindRepositoriesBlobs.Builder findRepositoriesBlobs =
          FindRepositoriesBlobs.newBuilder()
              .addAllCommits(request.getDatasetVersionIdsList())
              .addAllPredicates(request.getPredicatesList())
              .setPageLimit(request.getPageLimit())
              .setPageNumber(request.getPageNumber())
              .setWorkspaceName(request.getWorkspaceName());
      if (!request.getDatasetId().isEmpty()) {
        findRepositoriesBlobs.addRepoIds(Long.parseLong(request.getDatasetId()));
      }
      CommitPaginationDTO commitPaginationDTO =
          commitDAO.findCommits(
              findRepositoriesBlobs.build(),
              currentLoginUserInfo,
              false,
              false,
              true,
              request.getSortKey(),
              request.getAscending());

      RepositoryEntity repositoryEntity = null;
      if (!request.getDatasetId().isEmpty()) {
        RepositoryIdentification repositoryIdentification =
            RepositoryIdentification.newBuilder()
                .setRepoId(Long.parseLong(request.getDatasetId()))
                .build();
        repositoryEntity =
            repositoryDAO.getProtectedRepositoryById(repositoryIdentification, false);
      }
      List<DatasetVersion> datasetVersions =
          new ArrayList<>(
              convertRepoDatasetVersions(repositoryEntity, commitPaginationDTO.getCommits()));
      responseObserver.onNext(
          FindDatasetVersions.Response.newBuilder()
              .addAllDatasetVersions(datasetVersions)
              .setTotalRecords(commitPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, FindDatasetVersions.Response.getDefaultInstance());
    }
  }

  @Override
  public void updateDatasetVersionDescription(
      UpdateDatasetVersionDescription request,
      StreamObserver<UpdateDatasetVersionDescription.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        throw new ModelDBException(
            ModelDBMessages.DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST, Code.INVALID_ARGUMENT);
      }

      DatasetVersion datasetVersion =
          commitDAO.updateDatasetVersionDescription(
              repositoryDAO,
              blobDAO,
              metadataDAO,
              request.getDatasetId(),
              request.getId(),
              request.getDescription());
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(datasetVersion.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "update",
              "description",
              request.getDescription()));
      responseObserver.onNext(
          UpdateDatasetVersionDescription.Response.newBuilder()
              .setDatasetVersion(datasetVersion)
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, UpdateDatasetVersionDescription.Response.getDefaultInstance());
    }
  }

  @Override
  public void addDatasetVersionTags(
      AddDatasetVersionTags request,
      StreamObserver<AddDatasetVersionTags.Response> responseObserver) {
    try {
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
        throw new InvalidArgumentException(errorMessage);
      }

      DatasetVersion datasetVersion =
          commitDAO.addDeleteDatasetVersionTags(
              repositoryDAO,
              blobDAO,
              metadataDAO,
              true,
              request.getDatasetId(),
              request.getId(),
              request.getTagsList(),
              false);
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(datasetVersion.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "add",
              "tags",
              new Gson().toJsonTree(request.getTagsList())));
      responseObserver.onNext(
          AddDatasetVersionTags.Response.newBuilder().setDatasetVersion(datasetVersion).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, AddDatasetVersionTags.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteDatasetVersionTags(
      DeleteDatasetVersionTags request,
      StreamObserver<DeleteDatasetVersionTags.Response> responseObserver) {
    try {
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
        throw new InvalidArgumentException(errorMessage);
      }

      DatasetVersion datasetVersion =
          commitDAO.addDeleteDatasetVersionTags(
              repositoryDAO,
              blobDAO,
              metadataDAO,
              false,
              request.getDatasetId(),
              request.getId(),
              request.getTagsList(),
              request.getDeleteAll());
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(datasetVersion.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "delete",
              "tags",
              request.getDeleteAll() ? "deleteAll" : new Gson().toJsonTree(request.getTagsList())));
      responseObserver.onNext(
          DeleteDatasetVersionTags.Response.newBuilder().setDatasetVersion(datasetVersion).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteDatasetVersionTags.Response.getDefaultInstance());
    }
  }

  @Override
  public void addDatasetVersionAttributes(
      AddDatasetVersionAttributes request,
      StreamObserver<AddDatasetVersionAttributes.Response> responseObserver) {
    try {
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
        throw new InvalidArgumentException(errorMessage);
      }

      DatasetVersion updatedDatasetVersion =
          blobDAO.addUpdateDatasetVersionAttributes(
              repositoryDAO,
              commitDAO,
              metadataDAO,
              request.getDatasetId().isEmpty() ? null : Long.parseLong(request.getDatasetId()),
              request.getId(),
              request.getAttributesList(),
              true);
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "add",
              "attributes",
              new Gson().toJsonTree(request.getAttributesList())));
      responseObserver.onNext(
          AddDatasetVersionAttributes.Response.newBuilder()
              .setDatasetVersion(updatedDatasetVersion)
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, AddDatasetVersionAttributes.Response.getDefaultInstance());
    }
  }

  @Override
  public void updateDatasetVersionAttributes(
      UpdateDatasetVersionAttributes request,
      StreamObserver<UpdateDatasetVersionAttributes.Response> responseObserver) {
    try {
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
        throw new InvalidArgumentException(errorMessage);
      }

      DatasetVersion updatedDatasetVersion =
          blobDAO.addUpdateDatasetVersionAttributes(
              repositoryDAO,
              commitDAO,
              metadataDAO,
              request.getDatasetId().isEmpty() ? null : Long.parseLong(request.getDatasetId()),
              request.getId(),
              Collections.singletonList(request.getAttribute()),
              false);
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "add",
              "attributes",
              ModelDBUtils.getStringFromProtoObject(request.getAttribute())));
      responseObserver.onNext(
          UpdateDatasetVersionAttributes.Response.newBuilder()
              .setDatasetVersion(updatedDatasetVersion)
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, UpdateDatasetVersionAttributes.Response.getDefaultInstance());
    }
  }

  @Override
  public void getDatasetVersionAttributes(
      GetDatasetVersionAttributes request,
      StreamObserver<GetDatasetVersionAttributes.Response> responseObserver) {
    try {
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
        throw new InvalidArgumentException(errorMessage);
      }

      List<KeyValue> attributes =
          blobDAO.getDatasetVersionAttributes(
              repositoryDAO,
              commitDAO,
              request.getDatasetId().isEmpty() ? null : Long.parseLong(request.getDatasetId()),
              request.getId(),
              Collections.singletonList(ModelDBConstants.DEFAULT_VERSIONING_BLOB_LOCATION),
              request.getAttributeKeysList());

      responseObserver.onNext(
          GetDatasetVersionAttributes.Response.newBuilder().addAllAttributes(attributes).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetDatasetVersionAttributes.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteDatasetVersionAttributes(
      DeleteDatasetVersionAttributes request,
      StreamObserver<DeleteDatasetVersionAttributes.Response> responseObserver) {
    try {
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
        throw new InvalidArgumentException(errorMessage);
      }

      DatasetVersion updatedDatasetVersion =
          blobDAO.deleteDatasetVersionAttributes(
              repositoryDAO,
              commitDAO,
              metadataDAO,
              request.getDatasetId().isEmpty() ? null : Long.parseLong(request.getDatasetId()),
              request.getId(),
              request.getAttributeKeysList(),
              Collections.singletonList(ModelDBConstants.DEFAULT_VERSIONING_BLOB_LOCATION),
              request.getDeleteAll());
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "delete",
              "attributes",
              request.getDeleteAll()
                  ? "deleteAll"
                  : new Gson().toJsonTree(request.getAttributeKeysList())));
      responseObserver.onNext(
          DeleteDatasetVersionAttributes.Response.newBuilder()
              .setDatasetVersion(updatedDatasetVersion)
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteDatasetVersionAttributes.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteDatasetVersions(
      DeleteDatasetVersions request,
      StreamObserver<DeleteDatasetVersions.Response> responseObserver) {
    try {
      /*Parameter validation*/
      if (request.getIdsList().isEmpty()) {
        throw new InvalidArgumentException(ModelDBMessages.DATASET_VERSION_ID_NOT_FOUND_IN_REQUEST);
      }

      commitDAO.deleteDatasetVersions(
          request.getDatasetId().isEmpty()
              ? null
              : RepositoryIdentification.newBuilder()
                  .setRepoId(Long.parseLong(request.getDatasetId()))
                  .build(),
          request.getIdsList(),
          repositoryDAO);
      saveAuditLogs(null, ModelDBConstants.DELETE, request.getIdsList(), "");
      responseObserver.onNext(DeleteDatasetVersions.Response.getDefaultInstance());
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, DeleteDatasetVersions.Response.getDefaultInstance());
    }
  }

  @Override
  public void getUrlForDatasetBlobVersioned(
      GetUrlForDatasetBlobVersioned request,
      StreamObserver<GetUrlForDatasetBlobVersioned.Response> responseObserver) {
    try {
      // Validate request parameters
      validateGetUrlForVersionedDatasetBlobRequest(request);

      GetUrlForDatasetBlobVersioned.Response response =
          blobDAO.getUrlForVersionedDatasetBlob(
              artifactStoreDAO,
              repositoryDAO,
              request.getDatasetId(),
              (session, repository) ->
                  commitDAO.getCommitEntity(session, request.getDatasetVersionId(), repository),
              request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetUrlForDatasetBlobVersioned.Response.getDefaultInstance());
    }
  }

  private void validateGetUrlForVersionedDatasetBlobRequest(GetUrlForDatasetBlobVersioned request)
      throws ModelDBException {
    String errorMessage = null;
    if (request.getMethod().isEmpty() && request.getPathDatasetComponentBlobPath().isEmpty()) {
      errorMessage =
          "Method type AND DatasetBlob path not found in GetUrlForDatasetBlobVersioned request";
    } else if (request.getPathDatasetComponentBlobPath().isEmpty()) {
      errorMessage = "DatasetBlob path not found in GetUrlForDatasetBlobVersioned request";
    } else if (request.getMethod().isEmpty()) {
      errorMessage = "Method is not found in GetUrlForDatasetBlobVersioned request";
    }
    if (errorMessage != null) {
      LOGGER.info(errorMessage);
      throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
    }
  }

  @Override
  public void commitVersionedDatasetBlobArtifactPart(
      CommitVersionedDatasetBlobArtifactPart request,
      StreamObserver<CommitVersionedDatasetBlobArtifactPart.Response> responseObserver) {
    try {
      String errorMessage = null;
      if (!request.hasArtifactPart()) {
        errorMessage = "Artifact Part not found in CommitVersionedDatasetBlobArtifactPart request";
      }

      if (errorMessage != null) {
        LOGGER.info(errorMessage);
        throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
      }

      blobDAO.commitVersionedDatasetBlobArtifactPart(
          repositoryDAO,
          request.getDatasetId(),
          (session, repository) ->
              commitDAO.getCommitEntity(session, request.getDatasetVersionId(), repository),
          request);
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getDatasetVersionId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "commit",
              "versionedDatasetBlobArtifactPart",
              ModelDBUtils.getStringFromProtoObject(request.getArtifactPart())));
      responseObserver.onNext(CommitVersionedDatasetBlobArtifactPart.Response.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver,
          e,
          CommitVersionedDatasetBlobArtifactPart.Response.getDefaultInstance());
    }
  }

  @Override
  public void getCommittedVersionedDatasetBlobArtifactParts(
      GetCommittedVersionedDatasetBlobArtifactParts request,
      StreamObserver<GetCommittedVersionedDatasetBlobArtifactParts.Response> responseObserver) {
    try {
      GetCommittedVersionedDatasetBlobArtifactParts.Response response =
          blobDAO.getCommittedVersionedDatasetBlobArtifactParts(
              repositoryDAO,
              request.getDatasetId(),
              (session, repository) ->
                  commitDAO.getCommitEntity(session, request.getDatasetVersionId(), repository),
              request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver,
          e,
          GetCommittedVersionedDatasetBlobArtifactParts.Response.getDefaultInstance());
    }
  }

  @Override
  public void commitMultipartVersionedDatasetBlobArtifact(
      CommitMultipartVersionedDatasetBlobArtifact request,
      StreamObserver<CommitMultipartVersionedDatasetBlobArtifact.Response> responseObserver) {
    try {
      if (request.getPathDatasetComponentBlobPath().isEmpty()) {
        String errorMessage =
            "Path not found in CommitMultipartVersionedDatasetBlobArtifact request";
        throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
      }

      blobDAO.commitMultipartVersionedDatasetBlobArtifact(
          repositoryDAO,
          request.getDatasetId(),
          (session, repository) ->
              commitDAO.getCommitEntity(session, request.getDatasetVersionId(), repository),
          request,
          artifactStoreDAO::commitMultipart);
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getDatasetVersionId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "commit",
              "multipartVersionedDatasetBlobArtifact-location",
              request.getPathDatasetComponentBlobPath()));
      responseObserver.onNext(
          CommitMultipartVersionedDatasetBlobArtifact.Response.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver,
          e,
          CommitMultipartVersionedDatasetBlobArtifact.Response.getDefaultInstance());
    }
  }

  @Override
  public void getDatasetVersionById(
      GetDatasetVersionById request,
      StreamObserver<GetDatasetVersionById.Response> responseObserver) {
    try {
      if (request.getId().isEmpty()) {
        String errorMessage = "DatasetVersion id not found in GetDatasetVersionById request";
        throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
      }

      responseObserver.onNext(
          GetDatasetVersionById.Response.newBuilder()
              .setDatasetVersion(
                  commitDAO.getDatasetVersionById(
                      repositoryDAO, blobDAO, metadataDAO, request.getId()))
              .build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetDatasetVersionById.Response.getDefaultInstance());
    }
  }
}
