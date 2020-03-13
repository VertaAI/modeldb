package ai.verta.modeldb.collaborator;

import ai.verta.common.CollaboratorTypeEnum;
import ai.verta.common.TernaryEnum.Ternary;
import ai.verta.modeldb.ModelDBAuthInterceptor;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBConstants.UserIdentifier;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.dataset.DatasetDAO;
import ai.verta.modeldb.monitoring.ErrorCountResource;
import ai.verta.modeldb.monitoring.QPSCountResource;
import ai.verta.modeldb.monitoring.RequestLatencyResource;
import ai.verta.modeldb.project.ProjectDAO;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.Actions;
import ai.verta.uac.AddCollaboratorRequest;
import ai.verta.uac.AddCollaboratorRequest.Response;
import ai.verta.uac.AddCollaboratorRequest.Response.Builder;
import ai.verta.uac.CollaboratorServiceGrpc.CollaboratorServiceImplBase;
import ai.verta.uac.EntitiesEnum;
import ai.verta.uac.GetCollaborator;
import ai.verta.uac.GetCollaboratorResponse;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ModelResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.uac.RemoveCollaborator;
import ai.verta.uac.Role;
import ai.verta.uac.RoleBinding;
import ai.verta.uac.UserInfo;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CollaboratorServiceImpl extends CollaboratorServiceImplBase {

  private static final Logger LOGGER = LogManager.getLogger(CollaboratorServiceImpl.class);
  private AuthService authService;
  private RoleService roleService;
  private ProjectDAO projectDAO;
  private DatasetDAO datasetDAO;

  public CollaboratorServiceImpl(
      AuthService authService,
      RoleService roleService,
      ProjectDAO projectDAO,
      DatasetDAO datasetDAO) {
    this.authService = authService;
    this.roleService = roleService;
    this.projectDAO = projectDAO;
    this.datasetDAO = datasetDAO;
  }

  private CollaboratorBase getShareWithCollaborator(
      String shareWithIdOrEmail, EntitiesEnum.EntitiesTypes entitiesTypes) {
    switch (entitiesTypes) {
      case TEAM:
        return new CollaboratorTeam(shareWithIdOrEmail, roleService);
      case ORGANIZATION:
        return new CollaboratorOrg(shareWithIdOrEmail, roleService);
      case USER:
      case UNKNOWN:
        // Get the user info by verta OR emailId form the AuthService
        GeneratedMessageV3 shareWith = getShareWithUser(shareWithIdOrEmail);
        LOGGER.trace("Shared with userInfo : {}", shareWith);
        return new CollaboratorUser(authService, shareWith);
      default:
        String errorMessage = "Unknown type";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INTERNAL_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(AddCollaboratorRequest.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
    }
  }

  private UserInfo getShareWithUser(String sharedWithUser) {
    boolean isValidEmail = ModelDBUtils.isValidEmail(sharedWithUser);
    if (isValidEmail) {
      LOGGER.debug("Found shared user with emailId");
    }
    UserIdentifier userIdentifier =
        isValidEmail ? UserIdentifier.EMAIL_ID : UserIdentifier.VERTA_ID;
    // Get the user info by verta OR emailId form the AuthService
    UserInfo shareWithUserInfo = authService.getUserInfo(sharedWithUser, userIdentifier);
    LOGGER.trace("Shared with userInfo : {}", shareWithUserInfo);
    return shareWithUserInfo;
  }

  private void validateRequestParameters(AddCollaboratorRequest request, String entityType) {
    String errorMessage = null;
    if (request.getShareWith().isEmpty() && request.getEntityIdsList().isEmpty()) {
      errorMessage =
          "Shared User and " + entityType + " IDs not found in AddCollaboratorRequest request";
    } else if (request.getShareWith().isEmpty()) {
      errorMessage = "Shared User not found in AddCollaboratorRequest request";
    } else if (request.getEntityIdsList().isEmpty()) {
      errorMessage = entityType + " IDs not found in AddCollaboratorRequest request";
    }

    if (errorMessage != null) {
      LOGGER.warn(errorMessage);
      Status status =
          Status.newBuilder()
              .setCode(Code.INVALID_ARGUMENT_VALUE)
              .setMessage(errorMessage)
              .addDetails(Any.pack(AddCollaboratorRequest.Response.getDefaultInstance()))
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  private Map<String, String> getEntityOwnerMap(
      List<String> entityIds, ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    Map<String, String> entityOwnersMap = new HashMap<>();
    if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.PROJECT)) {
      entityOwnersMap = projectDAO.getOwnersByProjectIds(entityIds);
    } else if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.DATASET)) {
      entityOwnersMap = datasetDAO.getOwnersByDatasetIds(entityIds);
    }
    return entityOwnersMap;
  }

  private String getRoleNameBasedOnRequest(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      CollaboratorTypeEnum.CollaboratorType collaboratorType) {
    if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.PROJECT)
        && collaboratorType.equals(CollaboratorTypeEnum.CollaboratorType.READ_WRITE)) {
      return ModelDBConstants.ROLE_PROJECT_READ_WRITE;
    } else if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.PROJECT)
        && collaboratorType.equals(CollaboratorTypeEnum.CollaboratorType.READ_ONLY)) {
      return ModelDBConstants.ROLE_PROJECT_READ_ONLY;
    } else if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.DATASET)
        && collaboratorType.equals(CollaboratorTypeEnum.CollaboratorType.READ_WRITE)) {
      return ModelDBConstants.ROLE_DATASET_READ_WRITE;
    } else if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.DATASET)
        && collaboratorType.equals(CollaboratorTypeEnum.CollaboratorType.READ_ONLY)) {
      return ModelDBConstants.ROLE_DATASET_READ_ONLY;
    } else {
      String errorMessage = "collaborator type and resource type are not found as expected";
      LOGGER.warn(errorMessage);
      Status status =
          Status.newBuilder()
              .setCode(Code.INVALID_ARGUMENT_VALUE)
              .setMessage(errorMessage)
              .addDetails(Any.pack(AddCollaboratorRequest.Response.getDefaultInstance()))
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  private boolean deleteRoleBinding(
      String roleName,
      String resourceId,
      CollaboratorBase collaborator,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    String readOnlyRoleBindingName =
        roleService.buildRoleBindingName(
            roleName, resourceId, collaborator, modelDBServiceResourceTypes.name());
    RoleBinding readOnlyRoleBinding = roleService.getRoleBindingByName(readOnlyRoleBindingName);
    if (readOnlyRoleBinding != null && !readOnlyRoleBinding.getId().isEmpty()) {
      return roleService.deleteRoleBinding(readOnlyRoleBinding.getId());
    }
    return false;
  }

  private AddCollaboratorRequest.Response addORUpdateCollaborators(
      AddCollaboratorRequest request, ModelDBServiceResourceTypes modelDBServiceResourceTypes) {

    // Check User accessibleIds
    List<String> accessibleResourceIds = new ArrayList<>();
    if (request.getEntityIdsList().size() == 1) {
      roleService.isSelfAllowed(
          modelDBServiceResourceTypes, ModelDBServiceActions.UPDATE, request.getEntityIds(0));
      accessibleResourceIds.addAll(request.getEntityIdsList());
    } else {
      List<String> allowedResourceIds =
          roleService.getSelfAllowedResources(
              modelDBServiceResourceTypes, ModelDBServiceActions.UPDATE);
      // Validate if current user has access to the entity or not
      allowedResourceIds.retainAll(request.getEntityIdsList());
      accessibleResourceIds.addAll(allowedResourceIds);
    }

    if (accessibleResourceIds.isEmpty()) {
      Status status =
          Status.newBuilder()
              .setCode(Code.PERMISSION_DENIED_VALUE)
              .setMessage("Access Denied for given resource ids : " + request.getEntityIdsList())
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }

    // list of projects and their owners
    Map<String, String> resourceOwnersMap =
        getEntityOwnerMap(accessibleResourceIds, modelDBServiceResourceTypes);

    CollaboratorBase shareWithCollaborator =
        getShareWithCollaborator(request.getShareWith(), request.getAuthzEntityType());
    if (!shareWithCollaborator.isUser()
        || !resourceOwnersMap.containsValue(shareWithCollaborator.getVertaId())) {
      String roleName =
          getRoleNameBasedOnRequest(modelDBServiceResourceTypes, request.getCollaboratorType());
      assert roleName != null;
      Role role = roleService.getRoleByName(roleName, null);

      Role projectDeployRole = null;
      if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.PROJECT)) {
        if (request.getCanDeploy().equals(Ternary.TRUE)) {
          projectDeployRole = roleService.getRoleByName(ModelDBConstants.ROLE_PROJECT_DEPLOY, null);
        }
      }

      for (String resourceId : accessibleResourceIds) {
        processBindingsForResource(
            resourceId,
            role,
            shareWithCollaborator,
            modelDBServiceResourceTypes,
            request.getCanDeploy(),
            projectDeployRole);
      }
      Map<String, Actions> actions =
          roleService.getSelfAllowedActionsBatch(
              accessibleResourceIds, modelDBServiceResourceTypes);
      Builder response =
          Response.newBuilder()
              .setStatus(true)
              .addAllSelfAllowedActions(
                  ModelDBUtils.getActionsList(request.getEntityIdsList(), actions));
      shareWithCollaborator.addToResponse(response);
      return response.build();
    }

    Status status =
        Status.newBuilder()
            .setCode(Code.ALREADY_EXISTS_VALUE)
            .setMessage("Collaborator already owns a : " + request.getEntityIdsList())
            .build();
    throw StatusProto.toStatusRuntimeException(status);
  }

  private void processBindingsForResource(
      String resourceId,
      Role role,
      CollaboratorBase shareWithCollaborator,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      Ternary canDeploy,
      Role projectDeployRole) {

    throwExceptionIfCollaboratorAdmin(
        resourceId, shareWithCollaborator, modelDBServiceResourceTypes);
    AutoCloseable idLock = null;
    try {
      try {
        idLock = acquireWriteLock(resourceId);
      } catch (ExecutionException e) {
        LOGGER.warn("Error getting lock on respource {}", e);
      }
      deleteAllCollaboratorRoles(shareWithCollaborator, resourceId, modelDBServiceResourceTypes);
      roleService.createRoleBinding(
          role, shareWithCollaborator, resourceId, modelDBServiceResourceTypes);

      if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.PROJECT)
          && Ternary.TRUE.equals(canDeploy)
          && projectDeployRole != null) {
        roleService.createRoleBinding(
            projectDeployRole, shareWithCollaborator, resourceId, modelDBServiceResourceTypes);
      }
    } finally {
      if (idLock != null) {
        try {
          idLock.close();
        } catch (Exception e) {
          LOGGER.error("Exception while releasing the lock {}", e);
        }
      }
    }
  }

  private void throwExceptionIfCollaboratorAdmin(
      String resourceId,
      CollaboratorBase shareWithCollaborator,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    if (shareWithCollaborator.isUser()) {
      String adminRoleBindingName =
          roleService.buildAdminRoleBindingName(
              resourceId, shareWithCollaborator, modelDBServiceResourceTypes);
      RoleBinding adminRoleBinding = roleService.getRoleBindingByName(adminRoleBindingName);
      if (adminRoleBinding != null && !adminRoleBinding.getId().isEmpty()) {
        LOGGER.warn(
            "Modifying admin collaborator not permitted. Entity {}, admin {}",
            resourceId,
            shareWithCollaborator.getVertaId());
        Status status =
            Status.newBuilder()
                .setCode(Code.PERMISSION_DENIED_VALUE)
                .setMessage(ModelDBMessages.MODIFICATION_OF_ORG_ADMIN_COLLABORATOR_DENIED)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    }
  }

  private void deleteAllCollaboratorRoles(
      CollaboratorBase shareWithCollaborator,
      String resourceId,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    for (String roleName : getAllCollaboratorRoleNames(modelDBServiceResourceTypes)) {
      deleteRoleBinding(roleName, resourceId, shareWithCollaborator, modelDBServiceResourceTypes);
    }
  }

  private String[] getAllCollaboratorRoleNames(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    if (modelDBServiceResourceTypes == ModelDBServiceResourceTypes.PROJECT) {
      return new String[] {
        ModelDBConstants.ROLE_PROJECT_DEPLOY,
        ModelDBConstants.ROLE_PROJECT_READ_WRITE,
        ModelDBConstants.ROLE_PROJECT_READ_ONLY
      };
    }
    return new String[] {
      ModelDBConstants.ROLE_DATASET_READ_WRITE, ModelDBConstants.ROLE_DATASET_READ_ONLY
    };
  }

  @Override
  public void addOrUpdateProjectCollaborator(
      AddCollaboratorRequest request,
      StreamObserver<AddCollaboratorRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      // validate request parameters
      validateRequestParameters(request, ModelDBConstants.PROJECT_COLLABORATORS);

      responseObserver.onNext(
          addORUpdateCollaborators(request, ModelDBServiceResourceTypes.PROJECT));
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
              .addDetails(Any.pack(AddCollaboratorRequest.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  private RemoveCollaborator.Response removeCollaborator(
      RemoveCollaborator request, ModelDBServiceResourceTypes modelDBServiceResourceTypes) {

    boolean deletedStatus = false;
    CollaboratorBase shareWithCollaborator =
        getShareWithCollaborator(request.getShareWith(), request.getAuthzEntityType());

    throwExceptionIfCollaboratorAdmin(
        request.getEntityId(), shareWithCollaborator, modelDBServiceResourceTypes);

    String readOnlyRoleBindingName =
        roleService.buildReadOnlyRoleBindingName(
            request.getEntityId(), shareWithCollaborator, modelDBServiceResourceTypes);
    RoleBinding readOnlyRoleBinding = roleService.getRoleBindingByName(readOnlyRoleBindingName);
    if (readOnlyRoleBinding != null && !readOnlyRoleBinding.getId().isEmpty()) {
      roleService.deleteRoleBinding(readOnlyRoleBinding.getId());
      deletedStatus = true;
    }

    String readWriteRoleBindingName =
        roleService.buildReadWriteRoleBindingName(
            request.getEntityId(), shareWithCollaborator, modelDBServiceResourceTypes);
    RoleBinding readWriteRoleBinding = roleService.getRoleBindingByName(readWriteRoleBindingName);
    if (readWriteRoleBinding != null && !readWriteRoleBinding.getId().isEmpty()) {
      roleService.deleteRoleBinding(readWriteRoleBinding.getId());
      deletedStatus = true;
    }

    if (modelDBServiceResourceTypes == ModelDBServiceResourceTypes.PROJECT) {
      String projectDeployRoleBindingName =
          roleService.buildProjectDeployRoleBindingName(
              request.getEntityId(), shareWithCollaborator, modelDBServiceResourceTypes);
      RoleBinding projectDeployRoleBinding =
          roleService.getRoleBindingByName(projectDeployRoleBindingName);
      if (projectDeployRoleBinding != null && !projectDeployRoleBinding.getId().isEmpty()) {
        roleService.deleteRoleBinding(projectDeployRoleBinding.getId());
        deletedStatus = true;
      }
    }

    RemoveCollaborator.Response.Builder response =
        RemoveCollaborator.Response.newBuilder().setStatus(deletedStatus);

    if (deletedStatus) {
      final List<String> resourceIds = Collections.singletonList(request.getEntityId());
      Map<String, Actions> actions =
          roleService.getSelfAllowedActionsBatch(resourceIds, modelDBServiceResourceTypes);
      response.addAllSelfAllowedActions(ModelDBUtils.getActionsList(resourceIds, actions));
    }
    return response.build();
  }

  @Override
  public void removeProjectCollaborator(
      RemoveCollaborator request, StreamObserver<RemoveCollaborator.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getShareWith().isEmpty() && request.getEntityId().isEmpty()) {
        errorMessage = "Shared User and Project ID not found in RemoveProjectCollaborator request";
      } else if (request.getShareWith().isEmpty()) {
        errorMessage = "Shared User not found in RemoveProjectCollaborator request";
      } else if (request.getEntityId().isEmpty()) {
        errorMessage = "Project ID not found in RemoveProjectCollaborator request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(RemoveCollaborator.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      responseObserver.onNext(removeCollaborator(request, ModelDBServiceResourceTypes.PROJECT));
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
              .addDetails(Any.pack(RemoveCollaborator.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getProjectCollaborators(
      GetCollaborator request, StreamObserver<GetCollaborator.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getEntityId().isEmpty()) {
        String errorMessage = "Project ID not found in GetCollaborator request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetCollaborator.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      // Check User Role
      roleService.isSelfAllowed(
          ModelDBServiceResourceTypes.PROJECT, ModelDBServiceActions.READ, request.getEntityId());

      // list of projects and their owners
      Map<String, String> projectOwnersMap =
          getEntityOwnerMap(
              Collections.singletonList(request.getEntityId()),
              ModelDBServiceResourceTypes.PROJECT);
      Metadata requestHeaders = ModelDBAuthInterceptor.METADATA_INFO.get();
      List<GetCollaboratorResponse> responseData =
          roleService.getResourceCollaborators(
              ModelDBServiceResourceTypes.PROJECT,
              request.getEntityId(),
              projectOwnersMap.get(request.getEntityId()),
              requestHeaders);
      responseObserver.onNext(
          GetCollaborator.Response.newBuilder().addAllSharedUsers(responseData).build());
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
              .addDetails(Any.pack(GetCollaborator.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void addOrUpdateDatasetCollaborator(
      AddCollaboratorRequest request,
      StreamObserver<AddCollaboratorRequest.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {

      // validate request parameters
      validateRequestParameters(request, ModelDBConstants.DATASET_COLLABORATORS);

      responseObserver.onNext(
          addORUpdateCollaborators(request, ModelDBServiceResourceTypes.DATASET));
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
              .addDetails(Any.pack(AddCollaboratorRequest.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void getDatasetCollaborators(
      GetCollaborator request, StreamObserver<GetCollaborator.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      if (request.getEntityId().isEmpty()) {
        String errorMessage = "Dataset ID not found in GetCollaborator request";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(GetCollaborator.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      // Check User Role
      roleService.isSelfAllowed(
          ModelDBServiceResourceTypes.DATASET, ModelDBServiceActions.READ, request.getEntityId());

      // list of datasets and their owners
      Map<String, String> datasetOwnersMap =
          getEntityOwnerMap(
              Collections.singletonList(request.getEntityId()),
              ModelDBServiceResourceTypes.DATASET);
      Metadata requestHeaders = ModelDBAuthInterceptor.METADATA_INFO.get();
      List<GetCollaboratorResponse> responseData =
          roleService.getResourceCollaborators(
              ModelDBServiceResourceTypes.DATASET,
              request.getEntityId(),
              datasetOwnersMap.get(request.getEntityId()),
              requestHeaders);
      responseObserver.onNext(
          GetCollaborator.Response.newBuilder().addAllSharedUsers(responseData).build());
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
              .addDetails(Any.pack(GetCollaborator.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  @Override
  public void removeDatasetCollaborator(
      RemoveCollaborator request, StreamObserver<RemoveCollaborator.Response> responseObserver) {
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBAuthInterceptor.METHOD_NAME.get())) {
      String errorMessage = null;
      if (request.getShareWith().isEmpty() && request.getEntityId().isEmpty()) {
        errorMessage = "Shared User and Dataset ID not found in RemoveDatasetCollaborator request";
      } else if (request.getShareWith().isEmpty()) {
        errorMessage = "Shared User not found in RemoveDatasetCollaborator request";
      } else if (request.getEntityId().isEmpty()) {
        errorMessage = "Dataset ID not found in RemoveDatasetCollaborator request";
      }

      if (errorMessage != null) {
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .addDetails(Any.pack(RemoveCollaborator.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      responseObserver.onNext(removeCollaborator(request, ModelDBServiceResourceTypes.DATASET));
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
              .addDetails(Any.pack(RemoveCollaborator.Response.getDefaultInstance()))
              .build();
      StatusRuntimeException statusRuntimeException = StatusProto.toStatusRuntimeException(status);
      ErrorCountResource.inc(statusRuntimeException);
      responseObserver.onError(statusRuntimeException);
    }
  }

  private static final long CACHE_SIZE = 1000;
  private static final int DURATION = 10;

  private LoadingCache<String, ReadWriteLock> locks =
      CacheBuilder.newBuilder()
          .maximumSize(CACHE_SIZE)
          .expireAfterWrite(DURATION, TimeUnit.MINUTES)
          .build(
              new CacheLoader<String, ReadWriteLock>() {
                public ReadWriteLock load(String lockKey) {
                  return new ReentrantReadWriteLock() {};
                }
              });

  private AutoCloseable acquireWriteLock(String lockKey) throws ExecutionException {
    ReadWriteLock lock = locks.get(lockKey);
    Lock writeLock = lock.writeLock();
    writeLock.lock();
    return writeLock::unlock;
  }
}
