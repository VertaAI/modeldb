package ai.verta.modeldb.datasetVersion;

import ai.verta.common.KeyValue;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.Pagination;
import ai.verta.modeldb.AddDatasetVersionAttributes;
import ai.verta.modeldb.AddDatasetVersionTags;
import ai.verta.modeldb.CommitMultipartVersionedDatasetBlobArtifact;
import ai.verta.modeldb.CommitVersionedDatasetBlobArtifactPart;
import ai.verta.modeldb.CreateDatasetVersion;
import ai.verta.modeldb.CreateDatasetVersion.Response;
import ai.verta.modeldb.DAOSet;
import ai.verta.modeldb.DatasetVersion;
import ai.verta.modeldb.DatasetVersionServiceGrpc.DatasetVersionServiceImplBase;
import ai.verta.modeldb.DeleteDatasetVersion;
import ai.verta.modeldb.DeleteDatasetVersionAttributes;
import ai.verta.modeldb.DeleteDatasetVersionTags;
import ai.verta.modeldb.DeleteDatasetVersions;
import ai.verta.modeldb.FindDatasetVersions;
import ai.verta.modeldb.GetAllDatasetVersionsByDatasetId;
import ai.verta.modeldb.GetCommittedVersionedDatasetBlobArtifactParts;
import ai.verta.modeldb.GetDatasetVersionAttributes;
import ai.verta.modeldb.GetDatasetVersionById;
import ai.verta.modeldb.GetLatestDatasetVersionByDatasetId;
import ai.verta.modeldb.GetUrlForDatasetBlobVersioned;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.PathDatasetVersionInfo;
import ai.verta.modeldb.ServiceSet;
import ai.verta.modeldb.UpdateDatasetVersionAttributes;
import ai.verta.modeldb.UpdateDatasetVersionDescription;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.common.authservice.UACApisUtil;
import ai.verta.modeldb.common.event.FutureEventDAO;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.dto.DatasetVersionDTO;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEnums;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.modeldb.versioning.BlobDAO;
import ai.verta.modeldb.versioning.CommitDAO;
import ai.verta.modeldb.versioning.FindRepositoriesBlobs;
import ai.verta.modeldb.versioning.ListCommitsRequest;
import ai.verta.modeldb.versioning.RepositoryDAO;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.UserInfo;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.rpc.Code;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatasetVersionServiceImpl extends DatasetVersionServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(DatasetVersionServiceImpl.class);
  private static final String DELETE_DATASET_VERSION_EVENT_TYPE =
      "delete.resource.dataset_version.delete_dataset_version_succeeded";
  private static final String UPDATE_DATASET_VERSION_EVENT_TYPE =
      "update.resource.dataset_version.update_dataset_version_succeeded";
  private final UACApisUtil uacApisUtil;
  private final RepositoryDAO repositoryDAO;
  private final CommitDAO commitDAO;
  private final BlobDAO blobDAO;
  private final MetadataDAO metadataDAO;
  private final ArtifactStoreDAO artifactStoreDAO;
  private final MDBRoleService mdbRoleService;
  private final FutureEventDAO futureEventDAO;
  private final boolean isEventSystemEnabled;

  public DatasetVersionServiceImpl(ServiceSet serviceSet, DAOSet daoSet) {
    this.uacApisUtil = serviceSet.getUacApisUtil();
    this.mdbRoleService = serviceSet.getMdbRoleService();
    this.repositoryDAO = daoSet.getRepositoryDAO();
    this.commitDAO = daoSet.getCommitDAO();
    this.blobDAO = daoSet.getBlobDAO();
    this.metadataDAO = daoSet.getMetadataDAO();
    this.artifactStoreDAO = daoSet.getArtifactStoreDAO();
    this.futureEventDAO = daoSet.getFutureEventDAO();
    this.isEventSystemEnabled = serviceSet.getApp().mdbConfig.isEvent_system_enabled();
  }

  private void addEvent(
      String entityId,
      String datasetId,
      String eventType,
      Optional<String> updatedField,
      Map<String, Object> extraFieldsMap,
      String eventMessage) {

    if (!isEventSystemEnabled) {
      return;
    }

    // Add succeeded event in local DB
    JsonObject eventMetadata = new JsonObject();
    eventMetadata.addProperty("entity_id", entityId);
    eventMetadata.addProperty("dataset_id", datasetId);
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

    GetResourcesResponseItem datasetResource =
        mdbRoleService.getEntityResource(
            Optional.of(datasetId),
            Optional.empty(),
            ModelDBResourceEnum.ModelDBServiceResourceTypes.DATASET);

    futureEventDAO.addLocalEventWithBlocking(
        ModelDBServiceResourceTypes.DATASET_VERSION.name(),
        eventType,
        datasetResource.getWorkspaceId(),
        eventMetadata);
  }

  private DatasetVersion getDatasetVersionFromRequest(
      UACApisUtil uacApisUtil, CreateDatasetVersion request, UserInfo userInfo)
      throws ModelDBException {
    var datasetVersionBuilder =
        DatasetVersion.newBuilder()
            .setVersion(request.getVersion())
            .setDatasetId(request.getDatasetId())
            .setDescription(request.getDescription())
            .addAllTags(request.getTagsList())
            .addAllAttributes(request.getAttributesList())
            .setVersionNumber(1L);

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
      datasetVersionBuilder.setOwner(uacApisUtil.getVertaIdFromUserInfo(userInfo));
    }

    if (!request.hasPathDatasetVersionInfo() && !request.hasDatasetBlob()) {
      LOGGER.info("Request {}", request);
      throw new ModelDBException("Not supported", Code.UNIMPLEMENTED);
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
      var userInfo = uacApisUtil.getCurrentLoginUserInfo().blockAndGet();
      var datasetVersion = getDatasetVersionFromRequest(uacApisUtil, request, userInfo);

      var repositoryIdentification =
          RepositoryIdentification.newBuilder()
              .setRepoId(Long.parseLong(request.getDatasetId()))
              .build();

      var repositoryEntity =
          repositoryDAO.getProtectedRepositoryById(repositoryIdentification, true);
      var createCommitResponse =
          commitDAO.setCommitFromDatasetVersion(
              datasetVersion, repositoryDAO, blobDAO, metadataDAO, repositoryEntity);
      datasetVersion =
          blobDAO.convertToDatasetVersion(
              repositoryDAO,
              metadataDAO,
              repositoryEntity,
              createCommitResponse.getCommit().getCommitSha(),
              true);
      var response =
          CreateDatasetVersion.Response.newBuilder().setDatasetVersion(datasetVersion).build();

      // Add succeeded event in local DB
      addEvent(
          datasetVersion.getId(),
          datasetVersion.getDatasetId(),
          "add.resource.dataset_version.add_dataset_version_succeeded",
          Optional.empty(),
          Collections.emptyMap(),
          "dataset_version logged successfully");

      responseObserver.onNext(response);
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
      var datasetVersionDTO =
          getDatasetVersionDTOByDatasetId(
              request.getDatasetId(),
              request.getPageNumber(),
              request.getPageLimit(),
              request.getAscending());
      final var response =
          GetAllDatasetVersionsByDatasetId.Response.newBuilder()
              .addAllDatasetVersions(datasetVersionDTO.getDatasetVersions())
              .setTotalRecords(datasetVersionDTO.getTotalRecords())
              .build();
      /*Build response*/
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetAllDatasetVersionsByDatasetId.Response.getDefaultInstance());
    }
  }

  private DatasetVersionDTO getDatasetVersionDTOByDatasetId(
      String datasetId, int pageNumber, int pageLimit, boolean ascending) throws ModelDBException {

    /*Get Data*/
    var repositoryIdentification =
        RepositoryIdentification.newBuilder().setRepoId(Long.parseLong(datasetId)).build();
    ListCommitsRequest.Builder listCommitsRequest =
        ListCommitsRequest.newBuilder().setRepositoryId(repositoryIdentification);
    if (pageLimit > 0 && pageNumber > 0) {
      var pagination =
          Pagination.newBuilder().setPageLimit(pageLimit).setPageNumber(pageNumber).build();
      listCommitsRequest.setPagination(pagination);
    }
    var listCommitsResponse =
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

    var repositoryEntity =
        repositoryDAO.getProtectedRepositoryById(repositoryIdentification, false);
    List<DatasetVersion> datasetVersions =
        new ArrayList<>(
            RdbmsUtils.convertRepoDatasetVersions(
                repositoryDAO,
                metadataDAO,
                blobDAO,
                repositoryEntity,
                listCommitsResponse.getCommitsList()));

    var datasetVersionDTO = new DatasetVersionDTO();
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
      var response = DeleteDatasetVersion.Response.getDefaultInstance();

      // Add succeeded event in local DB
      addEvent(
          request.getId(),
          request.getDatasetId(),
          DELETE_DATASET_VERSION_EVENT_TYPE,
          Optional.empty(),
          Collections.emptyMap(),
          "dataset_version delete successfully");

      responseObserver.onNext(response);
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

      var currentLoginUserInfo = uacApisUtil.getCurrentLoginUserInfo().blockAndGet();

      FindRepositoriesBlobs.Builder findRepositoriesBlobs =
          FindRepositoriesBlobs.newBuilder()
              .addRepoIds(Integer.parseInt(request.getDatasetId()))
              .setPageLimit(1)
              .setPageNumber(1);
      String sortKey =
          request.getSortKey().isEmpty() ? ModelDBConstants.DATE_CREATED : request.getSortKey();
      var commitPaginationDTO =
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
        var repositoryIdentification =
            RepositoryIdentification.newBuilder()
                .setRepoId(Long.parseLong(request.getDatasetId()))
                .build();
        repositoryEntity =
            repositoryDAO.getProtectedRepositoryById(repositoryIdentification, false);
      }
      List<DatasetVersion> datasetVersions =
          new ArrayList<>(
              RdbmsUtils.convertRepoDatasetVersions(
                  repositoryDAO,
                  metadataDAO,
                  blobDAO,
                  repositoryEntity,
                  commitPaginationDTO.getCommits()));

      if (datasetVersions.size() != 1) {
        throw new NotFoundException(
            "No datasetVersion found for dataset '" + request.getDatasetId() + "'");
      }
      final var datasetVersion = datasetVersions.get(0);
      final var response =
          GetLatestDatasetVersionByDatasetId.Response.newBuilder()
              .setDatasetVersion(datasetVersion)
              .build();
      /*Build response*/
      responseObserver.onNext(response);
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
      var currentLoginUserInfo = uacApisUtil.getCurrentLoginUserInfo().blockAndGet();
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
      var commitPaginationDTO =
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
        var repositoryIdentification =
            RepositoryIdentification.newBuilder()
                .setRepoId(Long.parseLong(request.getDatasetId()))
                .build();
        repositoryEntity =
            repositoryDAO.getProtectedRepositoryById(repositoryIdentification, false);
      }
      List<DatasetVersion> datasetVersions =
          new ArrayList<>(
              RdbmsUtils.convertRepoDatasetVersions(
                  repositoryDAO,
                  metadataDAO,
                  blobDAO,
                  repositoryEntity,
                  commitPaginationDTO.getCommits()));
      final var response =
          FindDatasetVersions.Response.newBuilder()
              .addAllDatasetVersions(datasetVersions)
              .setTotalRecords(commitPaginationDTO.getTotalRecords())
              .build();
      responseObserver.onNext(response);
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

      var datasetVersion =
          commitDAO.updateDatasetVersionDescription(
              repositoryDAO,
              blobDAO,
              metadataDAO,
              request.getDatasetId(),
              request.getId(),
              request.getDescription());
      var response =
          UpdateDatasetVersionDescription.Response.newBuilder()
              .setDatasetVersion(datasetVersion)
              .build();

      // Add succeeded event in local DB
      addEvent(
          datasetVersion.getId(),
          datasetVersion.getDatasetId(),
          UPDATE_DATASET_VERSION_EVENT_TYPE,
          Optional.of("description"),
          Collections.emptyMap(),
          "dataset_version description updated successfully");

      responseObserver.onNext(response);
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

      var datasetVersion =
          commitDAO.addDeleteDatasetVersionTags(
              repositoryDAO,
              blobDAO,
              metadataDAO,
              true,
              request.getDatasetId(),
              request.getId(),
              request.getTagsList(),
              false);
      var response =
          AddDatasetVersionTags.Response.newBuilder().setDatasetVersion(datasetVersion).build();

      // Add succeeded event in local DB
      addEvent(
          datasetVersion.getId(),
          datasetVersion.getDatasetId(),
          UPDATE_DATASET_VERSION_EVENT_TYPE,
          Optional.of("tags"),
          Collections.singletonMap(
              "tags",
              new Gson()
                  .toJsonTree(
                      request.getTagsList(), new TypeToken<ArrayList<String>>() {}.getType())),
          "dataset_version tags added successfully");

      responseObserver.onNext(response);
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

      var datasetVersion =
          commitDAO.addDeleteDatasetVersionTags(
              repositoryDAO,
              blobDAO,
              metadataDAO,
              false,
              request.getDatasetId(),
              request.getId(),
              request.getTagsList(),
              request.getDeleteAll());
      var response =
          DeleteDatasetVersionTags.Response.newBuilder().setDatasetVersion(datasetVersion).build();

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
          datasetVersion.getId(),
          datasetVersion.getDatasetId(),
          UPDATE_DATASET_VERSION_EVENT_TYPE,
          Optional.of("tags"),
          extraField,
          "dataset_version tags deleted successfully");

      responseObserver.onNext(response);
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

      var updatedDatasetVersion =
          blobDAO.addUpdateDatasetVersionAttributes(
              repositoryDAO,
              commitDAO,
              metadataDAO,
              request.getDatasetId().isEmpty() ? null : Long.parseLong(request.getDatasetId()),
              request.getId(),
              request.getAttributesList(),
              true);
      var response =
          AddDatasetVersionAttributes.Response.newBuilder()
              .setDatasetVersion(updatedDatasetVersion)
              .build();

      // Add succeeded event in local DB
      addEvent(
          updatedDatasetVersion.getId(),
          updatedDatasetVersion.getDatasetId(),
          UPDATE_DATASET_VERSION_EVENT_TYPE,
          Optional.of("attributes"),
          Collections.singletonMap(
              "attribute_keys",
              new Gson()
                  .toJsonTree(
                      request.getAttributesList().stream()
                          .map(KeyValue::getKey)
                          .collect(Collectors.toSet()),
                      new TypeToken<ArrayList<String>>() {}.getType())),
          "dataset_version attributes added successfully");

      responseObserver.onNext(response);
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

      var updatedDatasetVersion =
          blobDAO.addUpdateDatasetVersionAttributes(
              repositoryDAO,
              commitDAO,
              metadataDAO,
              request.getDatasetId().isEmpty() ? null : Long.parseLong(request.getDatasetId()),
              request.getId(),
              Collections.singletonList(request.getAttribute()),
              false);
      var response =
          UpdateDatasetVersionAttributes.Response.newBuilder()
              .setDatasetVersion(updatedDatasetVersion)
              .build();

      // Add succeeded event in local DB
      addEvent(
          updatedDatasetVersion.getId(),
          updatedDatasetVersion.getDatasetId(),
          UPDATE_DATASET_VERSION_EVENT_TYPE,
          Optional.of("attributes"),
          Collections.singletonMap(
              "attribute_keys",
              new Gson()
                  .toJsonTree(
                      Stream.of(request.getAttribute())
                          .map(KeyValue::getKey)
                          .collect(Collectors.toSet()),
                      new TypeToken<ArrayList<String>>() {}.getType())),
          "dataset_version attribute updated successfully");

      responseObserver.onNext(response);
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

      final var response =
          GetDatasetVersionAttributes.Response.newBuilder().addAllAttributes(attributes).build();
      responseObserver.onNext(response);
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

      var updatedDatasetVersion =
          blobDAO.deleteDatasetVersionAttributes(
              repositoryDAO,
              commitDAO,
              metadataDAO,
              request.getDatasetId().isEmpty() ? null : Long.parseLong(request.getDatasetId()),
              request.getId(),
              request.getAttributeKeysList(),
              Collections.singletonList(ModelDBConstants.DEFAULT_VERSIONING_BLOB_LOCATION),
              request.getDeleteAll());
      var response =
          DeleteDatasetVersionAttributes.Response.newBuilder()
              .setDatasetVersion(updatedDatasetVersion)
              .build();

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
          updatedDatasetVersion.getId(),
          updatedDatasetVersion.getDatasetId(),
          UPDATE_DATASET_VERSION_EVENT_TYPE,
          Optional.of("attributes"),
          extraField,
          "dataset_version attributes deleted successfully");

      responseObserver.onNext(response);
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
      var response = DeleteDatasetVersions.Response.getDefaultInstance();

      // Add succeeded event in local DB
      for (String datasetVersionId : request.getIdsList()) {
        addEvent(
            datasetVersionId,
            request.getDatasetId(),
            DELETE_DATASET_VERSION_EVENT_TYPE,
            Optional.empty(),
            Collections.emptyMap(),
            "dataset_version delete successfully");
      }

      responseObserver.onNext(response);
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

      var response =
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
      var response = CommitVersionedDatasetBlobArtifactPart.Response.newBuilder().build();
      responseObserver.onNext(response);
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
      var response =
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
        var errorMessage = "Path not found in CommitMultipartVersionedDatasetBlobArtifact request";
        throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
      }

      blobDAO.commitMultipartVersionedDatasetBlobArtifact(
          repositoryDAO,
          request.getDatasetId(),
          (session, repository) ->
              commitDAO.getCommitEntity(session, request.getDatasetVersionId(), repository),
          request,
          artifactStoreDAO::commitMultipart);
      var response = CommitMultipartVersionedDatasetBlobArtifact.Response.newBuilder().build();
      responseObserver.onNext(response);
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
        var errorMessage = "DatasetVersion id not found in GetDatasetVersionById request";
        throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
      }

      final var response =
          GetDatasetVersionById.Response.newBuilder()
              .setDatasetVersion(
                  commitDAO.getDatasetVersionById(
                      repositoryDAO, blobDAO, metadataDAO, request.getId()))
              .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      CommonUtils.observeError(
          responseObserver, e, GetDatasetVersionById.Response.getDefaultInstance());
    }
  }
}
