package ai.verta.modeldb.dataset;

import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.OperatorEnum;
import ai.verta.common.ValueTypeEnum;
import ai.verta.modeldb.Dataset;
import ai.verta.modeldb.*;
import ai.verta.modeldb.DatasetServiceGrpc.DatasetServiceImplBase;
import ai.verta.modeldb.audit_log.AuditLogLocalDAO;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.dto.DatasetPaginationDTO;
import ai.verta.modeldb.dto.ExperimentPaginationDTO;
import ai.verta.modeldb.dto.ExperimentRunPaginationDTO;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.entities.audit_log.AuditLogLocalEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEnums;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.exceptions.ModelDBException;
import ai.verta.modeldb.exceptions.NotFoundException;
import ai.verta.modeldb.experiment.ExperimentDAO;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.metadata.MetadataServiceImpl;
import ai.verta.modeldb.project.ProjectDAO;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.*;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.ServiceEnum;
import ai.verta.uac.UserInfo;
import com.google.gson.Gson;
import com.google.protobuf.ListValue;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static io.grpc.Status.Code.INVALID_ARGUMENT;

public class DatasetServiceImpl extends DatasetServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(DatasetServiceImpl.class);
  private final RepositoryDAO repositoryDAO;
  private final CommitDAO commitDAO;
  private final MetadataDAO metadataDAO;
  private AuthService authService;
  private RoleService roleService;
  private ProjectDAO projectDAO;
  private ExperimentDAO experimentDAO;
  private ExperimentRunDAO experimentRunDAO;
  private final AuditLogLocalDAO auditLogLocalDAO;
  private static final String SERVICE_NAME =
      String.format("%s.%s", ModelDBConstants.SERVICE_NAME, ModelDBConstants.DATASET);

  public DatasetServiceImpl(
      AuthService authService,
      RoleService roleService,
      ProjectDAO projectDAO,
      ExperimentDAO experimentDAO,
      ExperimentRunDAO experimentRunDAO,
      RepositoryDAO repositoryDAO,
      CommitDAO commitDAO,
      MetadataDAO metadataDAO,
      AuditLogLocalDAO auditLogLocalDAO) {
    this.authService = authService;
    this.roleService = roleService;
    this.projectDAO = projectDAO;
    this.experimentDAO = experimentDAO;
    this.experimentRunDAO = experimentRunDAO;
    this.repositoryDAO = repositoryDAO;
    this.commitDAO = commitDAO;
    this.metadataDAO = metadataDAO;
    this.auditLogLocalDAO = auditLogLocalDAO;
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
                        ModelDBConstants.DATASET,
                        ServiceEnum.Service.MODELDB_SERVICE.name(),
                        metadataBlob))
            .collect(Collectors.toList());
    if (!auditLogLocalEntities.isEmpty()) {
      auditLogLocalDAO.saveAuditLogs(auditLogLocalEntities);
    }
  }

  /**
   * Create a Dataset from the input parameters and logs it. Required input parameter : name
   * Generate a random UUID for id set Name,Description,Attributes, Tags and Visibility from the
   * request. set current user as an owner
   */
  @Override
  public void createDataset(
      CreateDataset request, StreamObserver<CreateDataset.Response> responseObserver) {
    try {
      if (request.getName().isEmpty()) {
        request = request.toBuilder().setName(MetadataServiceImpl.createRandomName()).build();
      }

      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.REPOSITORY, null, ModelDBServiceActions.CREATE);

      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();

      Dataset dataset = getDatasetFromRequest(request, userInfo);
      ModelDBUtils.checkPersonalWorkspace(
          userInfo, dataset.getWorkspaceType(), dataset.getWorkspaceId(), "repository");

      Repository repository =
          repositoryDAO.createRepository(commitDAO, metadataDAO, dataset, true, userInfo);
      Dataset createdDataset =
          dataset
              .toBuilder()
              .setId(String.valueOf(repository.getId()))
              .setTimeCreated(repository.getDateCreated())
              .setTimeUpdated(repository.getDateUpdated())
              .build();

      saveAuditLogs(
          userInfo, ModelDBConstants.CREATE, Collections.singletonList(createdDataset.getId()), "");
      responseObserver.onNext(
          CreateDataset.Response.newBuilder().setDataset(createdDataset).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, CreateDataset.Response.getDefaultInstance());
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
            .setName(ModelDBUtils.checkEntityNameLength(request.getName()))
            .setDescription(request.getDescription())
            .addAllAttributes(request.getAttributesList())
            .addAllTags(ModelDBUtils.checkEntityTagsLength(request.getTagsList()))
            .setDatasetVisibility(request.getDatasetVisibility())
            .setVisibility(request.getVisibility())
            .setDatasetType(request.getDatasetType())
            .setCustomPermission(request.getCustomPermission());

    if (request.getTimeCreated() != 0L) {
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
        datasetBuilder.setWorkspaceServiceId(workspaceDTO.getWorkspaceServiceId());
        datasetBuilder.setWorkspaceType(workspaceDTO.getWorkspaceType());
      }
    }
    return datasetBuilder.build();
  }

  /** Fetch all (owned and shared) dataset for the current user */
  @Override
  public void getAllDatasets(
      GetAllDatasets request, StreamObserver<GetAllDatasets.Response> responseObserver) {
    try {
      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();

      FindDatasets.Builder findDatasets =
          FindDatasets.newBuilder()
              .setPageNumber(request.getPageNumber())
              .setPageLimit(request.getPageLimit())
              .setAscending(request.getAscending())
              .setSortKey(request.getSortKey())
              .setWorkspaceName(request.getWorkspaceName());

      if (findDatasets.getSortKey().equals(ModelDBConstants.TIME_UPDATED)) {
        findDatasets.setSortKey(ModelDBConstants.DATE_UPDATED);
      }
      if (findDatasets.getSortKey().equals(ModelDBConstants.TIME_CREATED)) {
        findDatasets.setSortKey(ModelDBConstants.DATE_CREATED);
      }

      DatasetPaginationDTO datasetPaginationDTO =
          repositoryDAO.findDatasets(
              metadataDAO, findDatasets.build(), userInfo, ResourceVisibility.PRIVATE);

      LOGGER.debug(
          ModelDBMessages.ACCESSIBLE_DATASET_IN_SERVICE, datasetPaginationDTO.getDatasets().size());
      responseObserver.onNext(
          GetAllDatasets.Response.newBuilder()
              .addAllDatasets(datasetPaginationDTO.getDatasets())
              .setTotalRecords(datasetPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, GetAllDatasets.Response.getDefaultInstance());
    }
  }

  /** Deletes dataset corresponding to the id. Required input parameter : id */
  @Override
  public void deleteDataset(
      DeleteDataset request, StreamObserver<DeleteDataset.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        throw new InvalidArgumentException(ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST);
      }

      deleteRepositoriesByDatasetIds(Collections.singletonList(request.getId()));
      saveAuditLogs(null, ModelDBConstants.DELETE, Collections.singletonList(request.getId()), "");
      responseObserver.onNext(DeleteDataset.Response.newBuilder().setStatus(true).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, DeleteDataset.Response.getDefaultInstance());
    }
  }

  /**
   * Fetches dataset corresponding to the id. <br>
   * Required input parameter : id
   */
  @Override
  public void getDatasetById(
      GetDatasetById request, StreamObserver<GetDatasetById.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        throw new InvalidArgumentException(ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.REPOSITORY, request.getId(), ModelDBServiceActions.READ);

      responseObserver.onNext(repositoryDAO.getDatasetById(metadataDAO, request.getId()));
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, GetDatasetById.Response.getDefaultInstance());
    }
  }

  @Override
  public void findDatasets(
      FindDatasets request, StreamObserver<FindDatasets.Response> responseObserver) {
    try {
      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      DatasetPaginationDTO datasetPaginationDTO =
          repositoryDAO.findDatasets(metadataDAO, request, userInfo, ResourceVisibility.PRIVATE);
      responseObserver.onNext(
          FindDatasets.Response.newBuilder()
              .addAllDatasets(datasetPaginationDTO.getDatasets())
              .setTotalRecords(datasetPaginationDTO.getTotalRecords())
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, FindDatasets.Response.getDefaultInstance());
    }
  }

  @Override
  public void getDatasetByName(
      GetDatasetByName request, StreamObserver<GetDatasetByName.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getName().isEmpty()) {
        throw new InvalidArgumentException(ModelDBMessages.DATASET_NAME_NOT_FOUND_IN_REQUEST);
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
          repositoryDAO.findDatasets(
              metadataDAO, findDatasets.build(), userInfo, ResourceVisibility.PRIVATE);

      if (datasetPaginationDTO.getTotalRecords() == 0) {
        throw new NotFoundException("Dataset not found");
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

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, GetDatasetByName.Response.getDefaultInstance());
    }
  }

  @Override
  public void updateDatasetName(
      UpdateDatasetName request, StreamObserver<UpdateDatasetName.Response> responseObserver) {
    try {
      // Request Parameter Validation
      String errorMessage = null;
      if (request.getId().isEmpty() && request.getName().isEmpty()) {
        errorMessage = "Dataset ID and Dataset name not found in UpdateDatasetName request";
      } else if (request.getId().isEmpty()) {
        errorMessage = ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST;
      } else if (request.getName().isEmpty()) {
        request = request.toBuilder().setName(MetadataServiceImpl.createRandomName()).build();
      }

      if (errorMessage != null) {
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.REPOSITORY, request.getId(), ModelDBServiceActions.UPDATE);

      GetDatasetById.Response getDatasetResponse =
          repositoryDAO.getDatasetById(metadataDAO, request.getId());
      Dataset updatedDataset =
          getDatasetResponse.getDataset().toBuilder().setName(request.getName()).build();
      repositoryDAO.createRepository(commitDAO, metadataDAO, updatedDataset, false, null);
      getDatasetResponse = repositoryDAO.getDatasetById(metadataDAO, request.getId());
      updatedDataset = getDatasetResponse.getDataset();

      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(updatedDataset.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE, "update", "name", updatedDataset.getName()));

      responseObserver.onNext(
          UpdateDatasetName.Response.newBuilder().setDataset(updatedDataset).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, UpdateDatasetName.Response.getDefaultInstance());
    }
  }

  @Override
  public void updateDatasetDescription(
      UpdateDatasetDescription request,
      StreamObserver<UpdateDatasetDescription.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getId().isEmpty()) {
        throw new ModelDBException(
            ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST, INVALID_ARGUMENT);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.REPOSITORY, request.getId(), ModelDBServiceActions.UPDATE);

      GetDatasetById.Response getDatasetResponse =
          repositoryDAO.getDatasetById(metadataDAO, request.getId());
      Dataset updatedDataset =
          getDatasetResponse
              .getDataset()
              .toBuilder()
              .setDescription(request.getDescription())
              .build();
      repositoryDAO.createRepository(commitDAO, metadataDAO, updatedDataset, false, null);
      getDatasetResponse = repositoryDAO.getDatasetById(metadataDAO, request.getId());
      updatedDataset = getDatasetResponse.getDataset();
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(updatedDataset.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "update",
              "description",
              updatedDataset.getDescription()));
      responseObserver.onNext(
          UpdateDatasetDescription.Response.newBuilder().setDataset(updatedDataset).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, UpdateDatasetDescription.Response.getDefaultInstance());
    }
  }

  @Override
  public void addDatasetTags(
      AddDatasetTags request, StreamObserver<AddDatasetTags.Response> responseObserver) {
    try {
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
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.REPOSITORY, request.getId(), ModelDBServiceActions.UPDATE);

      AddDatasetTags.Response updatedDataset =
          repositoryDAO.addDatasetTags(
              metadataDAO,
              request.getId(),
              ModelDBUtils.checkEntityTagsLength(request.getTagsList()));
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(request.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "add",
              "tags",
              new Gson().toJsonTree(request.getTagsList())));
      responseObserver.onNext(updatedDataset);
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, AddDatasetTags.Response.getDefaultInstance());
    }
  }

  @Override
  public void getDatasetTags(GetTags request, StreamObserver<GetTags.Response> responseObserver) {
    try {
      // Request Parameter Validation
      LOGGER.info("getDatasetTags not supported");
      throw new ModelDBException("Not supported", Code.UNIMPLEMENTED);

    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, GetTags.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteDatasetTags(
      DeleteDatasetTags request, StreamObserver<DeleteDatasetTags.Response> responseObserver) {
    try {
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
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.REPOSITORY, request.getId(), ModelDBServiceActions.UPDATE);

      Dataset updatedDataset =
          repositoryDAO.deleteDatasetTags(
              metadataDAO, request.getId(), request.getTagsList(), request.getDeleteAll());
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(updatedDataset.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "delete",
              "tags",
              request.getDeleteAll() ? "deleteAll" : new Gson().toJsonTree(request.getTagsList())));
      responseObserver.onNext(
          DeleteDatasetTags.Response.newBuilder().setDataset(updatedDataset).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, DeleteDatasetTags.Response.getDefaultInstance());
    }
  }

  @Override
  public void addDatasetAttributes(
      AddDatasetAttributes request,
      StreamObserver<AddDatasetAttributes.Response> responseObserver) {
    try {
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
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.REPOSITORY, request.getId(), ModelDBServiceActions.UPDATE);

      GetDatasetById.Response getDatasetResponse =
          repositoryDAO.getDatasetById(metadataDAO, request.getId());
      Dataset updatedDataset =
          getDatasetResponse
              .getDataset()
              .toBuilder()
              .addAllAttributes(request.getAttributesList())
              .build();
      repositoryDAO.createRepository(commitDAO, metadataDAO, updatedDataset, false, null);
      getDatasetResponse = repositoryDAO.getDatasetById(metadataDAO, request.getId());
      updatedDataset = getDatasetResponse.getDataset();
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
          AddDatasetAttributes.Response.newBuilder().setDataset(updatedDataset).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, AddDatasetAttributes.Response.getDefaultInstance());
    }
  }

  @Override
  public void updateDatasetAttributes(
      UpdateDatasetAttributes request,
      StreamObserver<UpdateDatasetAttributes.Response> responseObserver) {
    try {
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
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.REPOSITORY, request.getId(), ModelDBServiceActions.UPDATE);

      GetDatasetById.Response getDatasetResponse =
          repositoryDAO.getDatasetById(metadataDAO, request.getId());
      Dataset updatedDataset =
          getDatasetResponse.getDataset().toBuilder().addAttributes(request.getAttribute()).build();
      repositoryDAO.createRepository(commitDAO, metadataDAO, updatedDataset, false, null);
      getDatasetResponse = repositoryDAO.getDatasetById(metadataDAO, request.getId());
      updatedDataset = getDatasetResponse.getDataset();
      saveAuditLogs(
          null,
          ModelDBConstants.UPDATE,
          Collections.singletonList(updatedDataset.getId()),
          String.format(
              ModelDBConstants.METADATA_JSON_TEMPLATE,
              "update",
              "attributes",
              ModelDBUtils.getStringFromProtoObject(request.getAttribute())));
      responseObserver.onNext(
          UpdateDatasetAttributes.Response.newBuilder().setDataset(updatedDataset).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, UpdateDatasetAttributes.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteDatasetAttributes(
      DeleteDatasetAttributes request,
      StreamObserver<DeleteDatasetAttributes.Response> responseObserver) {
    try {
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
        throw new InvalidArgumentException(errorMessage);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.REPOSITORY, request.getId(), ModelDBServiceActions.UPDATE);

      repositoryDAO.deleteRepositoryAttributes(
          Long.parseLong(request.getId()),
          request.getAttributeKeysList(),
          request.getDeleteAll(),
          false,
          RepositoryEnums.RepositoryTypeEnum.DATASET);
      GetDatasetById.Response getDatasetResponse =
          repositoryDAO.getDatasetById(metadataDAO, request.getId());
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
          DeleteDatasetAttributes.Response.newBuilder()
              .setDataset(getDatasetResponse.getDataset())
              .build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, DeleteDatasetAttributes.Response.getDefaultInstance());
    }
  }

  @Override
  public void deleteDatasets(
      DeleteDatasets request, StreamObserver<DeleteDatasets.Response> responseObserver) {
    try {
      // Request Parameter Validation
      if (request.getIdsList().isEmpty()) {
        throw new InvalidArgumentException(ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST);
      }

      deleteRepositoriesByDatasetIds(request.getIdsList());
      saveAuditLogs(null, ModelDBConstants.DELETE, request.getIdsList(), "");
      responseObserver.onNext(DeleteDatasets.Response.newBuilder().setStatus(true).build());
      responseObserver.onCompleted();

    } catch (Exception e) {
      ModelDBUtils.observeError(responseObserver, e, DeleteDatasets.Response.getDefaultInstance());
    }
  }

  private void deleteRepositoriesByDatasetIds(List<String> datasetIds) throws ModelDBException {
    for (String datasetId : datasetIds) {
      repositoryDAO.deleteRepository(
          DeleteRepositoryRequest.newBuilder()
              .setRepositoryId(
                  RepositoryIdentification.newBuilder().setRepoId(Long.parseLong(datasetId)))
              .build(),
          commitDAO,
          experimentRunDAO,
          false,
          RepositoryEnums.RepositoryTypeEnum.DATASET);
    }
  }

  @Override
  public void getLastExperimentByDatasetId(
      LastExperimentByDatasetId request,
      StreamObserver<LastExperimentByDatasetId.Response> responseObserver) {
    try {
      if (request.getDatasetId().isEmpty()) {
        throw new InvalidArgumentException(ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.REPOSITORY,
          request.getDatasetId(),
          ModelDBServiceActions.READ);
      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();

      RepositoryIdentification repositoryIdentification =
          RepositoryIdentification.newBuilder()
              .setRepoId(Long.parseLong(request.getDatasetId()))
              .build();
      ListCommitsRequest.Builder listCommitsRequest =
          ListCommitsRequest.newBuilder().setRepositoryId(repositoryIdentification);
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
              false);
      List<String> datasetVersionIds = new ArrayList<>();
      ListValue.Builder listValueBuilder = ListValue.newBuilder();
      List<Commit> commitList = listCommitsResponse.getCommitsList();
      if (!commitList.isEmpty()) {
        for (Commit commit : commitList) {
          datasetVersionIds.add(commit.getCommitSha());
          listValueBuilder.addValues(
              Value.newBuilder().setStringValue(commit.getCommitSha()).build());
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
            experimentRunDAO.findExperimentRuns(projectDAO, userInfo, findExperimentRuns);
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
              experimentDAO.findExperiments(projectDAO, userInfo, findExperiments);
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

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, LastExperimentByDatasetId.Response.getDefaultInstance());
    }
  }

  @Override
  public void getExperimentRunByDataset(
      GetExperimentRunByDataset request,
      StreamObserver<GetExperimentRunByDataset.Response> responseObserver) {
    try {
      if (request.getDatasetId().isEmpty()) {
        throw new InvalidArgumentException(ModelDBMessages.DATASET_ID_NOT_FOUND_IN_REQUEST);
      }

      // Validate if current user has access to the entity or not
      roleService.validateEntityUserWithUserInfo(
          ModelDBServiceResourceTypes.REPOSITORY,
          request.getDatasetId(),
          ModelDBServiceActions.READ);

      // Get the user info from the Context
      UserInfo userInfo = authService.getCurrentLoginUserInfo();
      RepositoryIdentification repositoryIdentification =
          RepositoryIdentification.newBuilder()
              .setRepoId(Long.parseLong(request.getDatasetId()))
              .build();
      ListCommitsRequest.Builder listCommitsRequest =
          ListCommitsRequest.newBuilder().setRepositoryId(repositoryIdentification);
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
              false);
      List<String> datasetVersionIds = new ArrayList<>();
      ListValue.Builder listValueBuilder = ListValue.newBuilder();
      List<Commit> commitList = listCommitsResponse.getCommitsList();
      if (!commitList.isEmpty()) {
        for (Commit commit : commitList) {
          datasetVersionIds.add(commit.getCommitSha());
          listValueBuilder.addValues(
              Value.newBuilder().setStringValue(commit.getCommitSha()).build());
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
            experimentRunDAO.findExperimentRuns(projectDAO, userInfo, findExperimentRuns);
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

    } catch (Exception e) {
      ModelDBUtils.observeError(
          responseObserver, e, GetExperimentRunByDataset.Response.getDefaultInstance());
    }
  }
}
