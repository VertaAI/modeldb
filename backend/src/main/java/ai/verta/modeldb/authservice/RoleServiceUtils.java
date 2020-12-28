package ai.verta.modeldb.authservice;

import ai.verta.common.CollaboratorTypeEnum.CollaboratorType;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.TernaryEnum;
import ai.verta.common.WorkspaceTypeEnum.WorkspaceType;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.collaborator.CollaboratorBase;
import ai.verta.modeldb.collaborator.CollaboratorOrg;
import ai.verta.modeldb.collaborator.CollaboratorTeam;
import ai.verta.modeldb.collaborator.CollaboratorUser;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.*;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ServiceEnum.Service;
import com.google.protobuf.GeneratedMessageV3;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RoleServiceUtils implements RoleService {
  private static final Logger LOGGER = LogManager.getLogger(RoleServiceUtils.class);
  private AuthService authService;

  public RoleServiceUtils(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public boolean IsImplemented() {
    return true;
  }

  @Override
  public String buildRoleBindingName(
      String roleName, String resourceId, String userId, String resourceTypeName) {
    return buildRoleBindingName(
        roleName, resourceId, new CollaboratorUser(authService, userId), resourceTypeName);
  }

  @Override
  public String buildRoleBindingName(
      String roleName, String resourceId, CollaboratorBase collaborator, String resourceTypeName) {
    return roleName
        + "_"
        + resourceTypeName
        + "_"
        + resourceId
        + "_"
        + collaborator.getNameForBinding();
  }

  @Override
  public void createRoleBinding(
      Role role,
      CollaboratorBase collaborator,
      String resourceId,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    String roleBindingName =
        buildRoleBindingName(
            role.getName(), resourceId, collaborator, modelDBServiceResourceTypes.name());

    RoleBinding newRoleBinding =
        RoleBinding.newBuilder()
            .setName(roleBindingName)
            .setScope(role.getScope())
            .setRoleId(role.getId())
            .addEntities(collaborator.getEntities())
            .addResources(
                Resources.newBuilder()
                    .setService(Service.MODELDB_SERVICE)
                    .setResourceType(
                        ResourceType.newBuilder()
                            .setModeldbServiceResourceType(modelDBServiceResourceTypes))
                    .addResourceIds(resourceId)
                    .build())
            .build();
    setRoleBindingOnAuthService(true, newRoleBinding);
  }

  @Override
  public void createPublicRoleBinding(
      String resourceId, ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    String roleBindingName = buildPublicRoleBindingName(resourceId, modelDBServiceResourceTypes);

    RoleBinding newRoleBinding =
        RoleBinding.newBuilder()
            .setName(roleBindingName)
            .setPublic(true)
            .addResources(
                Resources.newBuilder()
                    .setService(Service.MODELDB_SERVICE)
                    .setResourceType(
                        ResourceType.newBuilder()
                            .setModeldbServiceResourceType(modelDBServiceResourceTypes))
                    .addResourceIds(resourceId)
                    .build())
            .build();
    setRoleBindingOnAuthService(true, newRoleBinding);
  }

  @Override
  public String buildPublicRoleBindingName(
      String resourceId, ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    return buildRoleBindingName(
        "PUBLIC_ROLE", resourceId, "PUBLIC", modelDBServiceResourceTypes.name());
  }

  @Override
  public void isSelfAllowed(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      String resourceId) {
    isSelfAllowed(true, modelDBServiceResourceTypes, modelDBServiceActions, resourceId);
  }

  private void isSelfAllowed(
      boolean retry,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      String resourceId) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);
      Resources.Builder resourceBuilder =
          Resources.newBuilder()
              .setService(ServiceEnum.Service.MODELDB_SERVICE)
              .setResourceType(
                  ResourceType.newBuilder()
                      .setModeldbServiceResourceType(modelDBServiceResourceTypes));
      if (resourceId != null) {
        resourceBuilder.addResourceIds(resourceId);
      }
      IsSelfAllowed isSelfAllowedRequest =
          IsSelfAllowed.newBuilder()
              .addResources(resourceBuilder.build())
              .addActions(
                  Action.newBuilder()
                      .setService(ServiceEnum.Service.MODELDB_SERVICE)
                      .setModeldbServiceAction(modelDBServiceActions)
                      .build())
              .build();

      Metadata requestHeaders = AuthInterceptor.METADATA_INFO.get();
      IsSelfAllowed.Response isSelfAllowedResponse =
          authServiceChannel
              .getAuthzServiceBlockingStub(requestHeaders)
              .isSelfAllowed(isSelfAllowedRequest);
      LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, isSelfAllowedResponse);

      if (!isSelfAllowedResponse.getAllowed()) {
        Status status =
            Status.newBuilder()
                .setCode(Code.PERMISSION_DENIED_VALUE)
                .setMessage("Access Denied")
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    } catch (StatusRuntimeException ex) {
      ModelDBUtils.retryOrThrowException(
          ex,
          retry,
          (ModelDBUtils.RetryCallInterface<Void>)
              retry1 -> {
                isSelfAllowed(
                    retry1, modelDBServiceResourceTypes, modelDBServiceActions, resourceId);
                return null;
              });
    }
  }

  @Override
  public Map<String, Actions> getSelfAllowedActionsBatch(
      List<String> resourceIds, ModelDBServiceResourceTypes type) {
    return getSelfAllowedActionsBatch(true, resourceIds, type);
  }

  private Map<String, Actions> getSelfAllowedActionsBatch(
      boolean retry, List<String> resourceIds, ModelDBServiceResourceTypes type) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);
      GetSelfAllowedActionsBatch getSelfAllowedActionsBatch =
          GetSelfAllowedActionsBatch.newBuilder()
              .setResources(
                  Resources.newBuilder()
                      .setService(ServiceEnum.Service.MODELDB_SERVICE)
                      .addAllResourceIds(resourceIds)
                      .setResourceType(
                          ResourceType.newBuilder().setModeldbServiceResourceType(type))
                      .build())
              .build();

      Metadata requestHeaders = AuthInterceptor.METADATA_INFO.get();
      GetSelfAllowedActionsBatch.Response getSelfAllowedActionsBatchResponse =
          authServiceChannel
              .getAuthzServiceBlockingStub(requestHeaders)
              .getSelfAllowedActionsBatch(getSelfAllowedActionsBatch);
      LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(
          ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getSelfAllowedActionsBatchResponse);
      return getSelfAllowedActionsBatchResponse.getActionsMap();

    } catch (StatusRuntimeException ex) {
      return (Map<String, Actions>)
          ModelDBUtils.retryOrThrowException(
              ex,
              retry,
              (ModelDBUtils.RetryCallInterface<Map<String, Actions>>)
                  (retry1) -> getSelfAllowedActionsBatch(retry1, resourceIds, type));
    }
  }

  @Override
  public Role getRoleByName(String roleName, RoleScope roleScope) {
    return getRoleByName(true, roleName, roleScope);
  }

  private Role getRoleByName(boolean retry, String roleName, RoleScope roleScope) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);
      GetRoleByName.Builder getRoleByNameRequest = GetRoleByName.newBuilder().setName(roleName);
      if (roleScope != null) {
        getRoleByNameRequest.setScope(roleScope);
      }
      GetRoleByName.Response getRoleByNameResponse =
          authServiceChannel
              .getRoleServiceBlockingStub()
              .getRoleByName(getRoleByNameRequest.build());
      LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getRoleByNameResponse);

      return getRoleByNameResponse.getRole();
    } catch (StatusRuntimeException ex) {
      return (Role)
          ModelDBUtils.retryOrThrowException(
              ex,
              retry,
              (ModelDBUtils.RetryCallInterface<Role>)
                  (retry1) -> getRoleByName(retry1, roleName, roleScope));
    }
  }

  @Override
  public boolean deleteRoleBinding(String roleBindingId) {
    return deleteRoleBinding(true, roleBindingId);
  }

  private boolean deleteRoleBinding(boolean retry, String roleBindingId) {
    DeleteRoleBinding deleteRoleBindingRequest =
        DeleteRoleBinding.newBuilder().setId(roleBindingId).build();
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);
      DeleteRoleBinding.Response deleteRoleBindingResponse =
          authServiceChannel
              .getRoleServiceBlockingStub()
              .deleteRoleBinding(deleteRoleBindingRequest);
      LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, deleteRoleBindingResponse);

      return deleteRoleBindingResponse.getStatus();
    } catch (StatusRuntimeException ex) {
      return (Boolean)
          ModelDBUtils.retryOrThrowException(
              ex,
              retry,
              (ModelDBUtils.RetryCallInterface<Boolean>)
                  (retry1) -> deleteRoleBinding(retry1, roleBindingId));
    }
  }

  @Override
  public boolean deleteRoleBindings(List<String> roleBindingNames) {
    return deleteRoleBindings(true, roleBindingNames);
  }

  private boolean deleteRoleBindings(boolean retry, List<String> roleBindingNames) {
    DeleteRoleBindings deleteRoleBindingRequest =
        DeleteRoleBindings.newBuilder().addAllRoleBindingNames(roleBindingNames).build();
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);

      // TODO: try using futur stub than blocking stub
      DeleteRoleBindings.Response deleteRoleBindingResponse =
          authServiceChannel
              .getRoleServiceBlockingStub()
              .deleteRoleBindings(deleteRoleBindingRequest);
      LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, deleteRoleBindingResponse);

      return deleteRoleBindingResponse.getStatus();
    } catch (StatusRuntimeException ex) {
      return (Boolean)
          ModelDBUtils.retryOrThrowException(
              ex,
              retry,
              (ModelDBUtils.RetryCallInterface<Boolean>)
                  (retry1) -> deleteRoleBindings(retry1, roleBindingNames));
    }
  }

  private void setRoleBindingOnAuthService(boolean retry, RoleBinding roleBinding) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);
      SetRoleBinding.Response setRoleBindingResponse =
          authServiceChannel
              .getRoleServiceBlockingStub()
              .setRoleBinding(SetRoleBinding.newBuilder().setRoleBinding(roleBinding).build());
      LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, setRoleBindingResponse);
    } catch (StatusRuntimeException ex) {
      ModelDBUtils.retryOrThrowException(
          ex,
          retry,
          (ModelDBUtils.RetryCallInterface<Void>)
              (retry1) -> {
                setRoleBindingOnAuthService(retry1, roleBinding);
                return null;
              });
    }
  }

  @Override
  public List<GetCollaboratorResponseItem> getResourceCollaborators(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      String resourceId,
      String resourceOwnerId,
      Metadata requestHeaders) {
    return getResourceCollaborators(
        true, modelDBServiceResourceTypes, resourceId, resourceOwnerId, requestHeaders);
  }

  private List<GetCollaboratorResponseItem> getResourceCollaborators(
      boolean retry,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      String resourceId,
      String resourceOwnerId,
      Metadata requestHeaders) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.debug("getting Resource collaborator with authChannel {}", authServiceChannel);
      return getCollaborators(
          authServiceChannel,
          resourceOwnerId,
          resourceId,
          modelDBServiceResourceTypes,
          requestHeaders);
    } catch (StatusRuntimeException ex) {
      return (List<GetCollaboratorResponseItem>)
          ModelDBUtils.retryOrThrowException(
              ex,
              retry,
              (ModelDBUtils.RetryCallInterface<List<GetCollaboratorResponseItem>>)
                  (retry1) ->
                      getResourceCollaborators(
                          retry1,
                          modelDBServiceResourceTypes,
                          resourceId,
                          resourceOwnerId,
                          requestHeaders));
    }
  }

  private Set<CollaboratorBase> getCollaborators(
      AuthServiceChannel authServiceChannel,
      String resourceOwnerId,
      String resourceId,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      Metadata requestHeaders) {
    GetAllowedEntities getAllowedEntitiesRequest =
        GetAllowedEntities.newBuilder()
            .addActions(
                Action.newBuilder()
                    .setModeldbServiceAction(modelDBServiceActions)
                    .setService(ServiceEnum.Service.MODELDB_SERVICE)
                    .build())
            .addResources(
                Resources.newBuilder()
                    .addResourceIds(resourceId)
                    .setService(ServiceEnum.Service.MODELDB_SERVICE)
                    .setResourceType(
                        ResourceType.newBuilder()
                            .setModeldbServiceResourceType(modelDBServiceResourceTypes))
                    .build())
            .build();
    LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);
    GetAllowedEntities.Response getAllowedEntitiesResponse =
        authServiceChannel
            .getAuthzServiceBlockingStub(requestHeaders)
            .getAllowedEntities(getAllowedEntitiesRequest);
    LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
    LOGGER.trace(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getAllowedEntitiesResponse);

    Set<CollaboratorBase> collaborators = new HashSet<>();
    if (getAllowedEntitiesResponse.getEntitiesCount() != 0) {
      for (Entities entities : getAllowedEntitiesResponse.getEntitiesList()) {
        entities.getUserIdsList().stream()
            .filter(id -> !id.equals(resourceOwnerId))
            .forEach(id -> collaborators.add(new CollaboratorUser(authService, id)));
        entities
            .getTeamIdsList()
            .forEach(
                teamId -> {
                  try {
                    CollaboratorTeam collaboratorTeam = new CollaboratorTeam(teamId);
                    collaborators.add(collaboratorTeam);
                  } catch (StatusRuntimeException ex) {
                    if (ex.getStatus().getCode().value() == Code.PERMISSION_DENIED_VALUE) {
                      LOGGER.info(
                          "Current user is not a member of the team : "
                              + teamId
                              + ", "
                              + ex.getMessage(),
                          ex);
                    }
                  }
                });
        entities
            .getOrgIdsList()
            .forEach(
                orgId -> {
                  try {
                    CollaboratorOrg collaboratorOrg = new CollaboratorOrg(orgId);
                    collaborators.add(collaboratorOrg);
                  } catch (StatusRuntimeException ex) {
                    if (ex.getStatus().getCode().value() == Code.PERMISSION_DENIED_VALUE) {
                      LOGGER.info(
                          "Current user is not a member of the organization : "
                              + orgId
                              + ", "
                              + ex.getMessage(),
                          ex);
                    }
                  }
                });
      }
    }

    return collaborators;
  }

  private List<GetCollaboratorResponseItem> getCollaborators(
      AuthServiceChannel authServiceChannel,
      String resourceOwnerId,
      String resourceId,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      Metadata requestHeaders) {
    List<GetCollaboratorResponseItem> getCollaboratorResponseList = new ArrayList<>();

    try {
      // Run a task specified by a Supplier object asynchronously
      CompletableFuture<Set<CollaboratorBase>> deployCollaboratorsFuture =
          CompletableFuture.supplyAsync(
              () ->
                  getCollaborators(
                      authServiceChannel,
                      resourceOwnerId,
                      resourceId,
                      modelDBServiceResourceTypes,
                      ModelDBServiceActions.DEPLOY,
                      requestHeaders));

      CompletableFuture<Set<CollaboratorBase>> readOnlyCollaboratorsFuture =
          CompletableFuture.supplyAsync(
              () ->
                  getCollaborators(
                      authServiceChannel,
                      resourceOwnerId,
                      resourceId,
                      modelDBServiceResourceTypes,
                      ModelDBServiceActions.READ,
                      requestHeaders));

      CompletableFuture<Set<CollaboratorBase>> readWriteCollaboratorsFuture =
          CompletableFuture.supplyAsync(
              () ->
                  getCollaborators(
                      authServiceChannel,
                      resourceOwnerId,
                      resourceId,
                      modelDBServiceResourceTypes,
                      ModelDBServiceActions.UPDATE,
                      requestHeaders));

      CompletableFuture<Void> collaboratorCombineFuture =
          CompletableFuture.allOf(
              deployCollaboratorsFuture, readOnlyCollaboratorsFuture, readWriteCollaboratorsFuture);

      // Wait for all task complete
      collaboratorCombineFuture.get();

      Set<CollaboratorBase> deployCollaborators = deployCollaboratorsFuture.get();
      Set<CollaboratorBase> readOnlyCollaborators = readOnlyCollaboratorsFuture.get();
      Set<CollaboratorBase> readWriteCollaborators = readWriteCollaboratorsFuture.get();

      readOnlyCollaborators.removeAll(readWriteCollaborators);
      if (readOnlyCollaborators.size() > 0) {
        LOGGER.debug("ReadOnly Collaborators count: " + readOnlyCollaborators.size());
        for (CollaboratorBase collaborator : readOnlyCollaborators) {
          GetCollaboratorResponseItem.Builder getCollaboratorResponseBuilder =
              GetCollaboratorResponseItem.newBuilder()
                  .setAuthzEntityType(collaborator.getAuthzEntityType())
                  .setVertaId(collaborator.getId());
          CollaboratorPermissions.Builder collPermBuilder = CollaboratorPermissions.newBuilder();
          collPermBuilder.setCollaboratorType(CollaboratorType.READ_ONLY);
          if (deployCollaborators.contains(collaborator)) {
            collPermBuilder.setCanDeploy(TernaryEnum.Ternary.TRUE);
          } else {
            collPermBuilder.setCanDeploy(TernaryEnum.Ternary.FALSE);
          }
          getCollaboratorResponseBuilder.setPermission(collPermBuilder.build());

          getCollaboratorResponseList.add(getCollaboratorResponseBuilder.build());
        }
      }

      if (readWriteCollaborators.size() > 0) {
        LOGGER.debug("ReadWrite Collaborators count: " + readWriteCollaborators.size());
        for (CollaboratorBase collaborator : readWriteCollaborators) {
          GetCollaboratorResponseItem.Builder getCollaboratorResponseBuilder =
              GetCollaboratorResponseItem.newBuilder()
                  .setAuthzEntityType(collaborator.getAuthzEntityType())
                  .setVertaId(collaborator.getId());
          CollaboratorPermissions.Builder collPermBuilder = CollaboratorPermissions.newBuilder();
          collPermBuilder.setCollaboratorType(CollaboratorType.READ_WRITE);
          if (deployCollaborators.contains(collaborator)) {
            collPermBuilder.setCanDeploy(TernaryEnum.Ternary.TRUE);
          } else {
            collPermBuilder.setCanDeploy(TernaryEnum.Ternary.FALSE);
          }
          getCollaboratorResponseBuilder.setPermission(collPermBuilder.build());
          getCollaboratorResponseList.add(getCollaboratorResponseBuilder.build());
        }
      }
    } catch (InterruptedException | ExecutionException ex) {
      Status status =
          Status.newBuilder().setCode(Code.INTERNAL_VALUE).setMessage(ex.getMessage()).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
    LOGGER.debug("Total Collaborators count: " + getCollaboratorResponseList.size());
    return getCollaboratorResponseList;
  }

  /**
   * Checks permissions of the user wrt the Entity
   *
   * @param resourceId --> value of key like project.id, dataset.id etc.
   * @param modelDBServiceActions --> ModelDBServiceActions.UPDATE, ModelDBServiceActions.DELETE,
   *     ModelDBServiceActions.CREATE
   */
  @Override
  public void validateEntityUserWithUserInfo(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      String resourceId,
      ModelDBServiceActions modelDBServiceActions) {
    // Check User Role
    isSelfAllowed(modelDBServiceResourceTypes, modelDBServiceActions, resourceId);
  }

  @Override
  public String buildReadOnlyRoleBindingName(
      String resourceId,
      CollaboratorBase collaborator,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.PROJECT)) {
      return buildRoleBindingName(
          ModelDBConstants.ROLE_PROJECT_READ_ONLY,
          resourceId,
          collaborator,
          ModelDBServiceResourceTypes.PROJECT.name());
    } else if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.DATASET)) {
      return buildRoleBindingName(
          ModelDBConstants.ROLE_DATASET_READ_ONLY,
          resourceId,
          collaborator,
          ModelDBServiceResourceTypes.DATASET.name());
    } else if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.REPOSITORY)) {
      return buildRoleBindingName(
          ModelDBConstants.ROLE_REPOSITORY_READ_ONLY,
          resourceId,
          collaborator,
          ModelDBServiceResourceTypes.REPOSITORY.name());
    } else {
      return ModelDBConstants.EMPTY_STRING;
    }
  }

  @Override
  public RoleBinding getRoleBindingByName(String roleBindingName) {
    return getRoleBindingByName(true, roleBindingName);
  }

  private RoleBinding getRoleBindingByName(boolean retry, String roleBindingName) {
    GetRoleBindingByName getRoleBindingByNameRequest =
        GetRoleBindingByName.newBuilder().setName(roleBindingName).build();
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);
      GetRoleBindingByName.Response getRoleBindingByNameResponse =
          authServiceChannel
              .getRoleServiceBlockingStub()
              .getRoleBindingByName(getRoleBindingByNameRequest);
      LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(
          ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getRoleBindingByNameResponse);

      return getRoleBindingByNameResponse.getRoleBinding();
    } catch (StatusRuntimeException ex) {
      LOGGER.info(roleBindingName + " : " + ex.getMessage());
      if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
        return (RoleBinding)
            ModelDBUtils.retryOrThrowException(
                ex,
                retry,
                (ModelDBUtils.RetryCallInterface<RoleBinding>)
                    (retry1) -> getRoleBindingByName(retry1, roleBindingName));
      } else if (ex.getStatus().getCode().value() == Code.NOT_FOUND_VALUE) {
        return RoleBinding.newBuilder().build();
      }
      throw ex;
    }
  }

  @Override
  public List<String> getSelfAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions) {
    return getSelfAllowedResources(true, modelDBServiceResourceTypes, modelDBServiceActions);
  }

  private List<String> getSelfAllowedResources(
      boolean retry,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions) {
    Action action =
        Action.newBuilder()
            .setService(ServiceEnum.Service.MODELDB_SERVICE)
            .setModeldbServiceAction(modelDBServiceActions)
            .build();
    GetSelfAllowedResources getAllowedResourcesRequest =
        GetSelfAllowedResources.newBuilder()
            .addActions(action)
            .setResourceType(
                ResourceType.newBuilder()
                    .setModeldbServiceResourceType(modelDBServiceResourceTypes))
            .setService(Service.MODELDB_SERVICE)
            .build();
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);
      Metadata requestHeaders = AuthInterceptor.METADATA_INFO.get();
      GetSelfAllowedResources.Response getAllowedResourcesResponse =
          authServiceChannel
              .getAuthzServiceBlockingStub(requestHeaders)
              .getSelfAllowedResources(getAllowedResourcesRequest);
      LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(
          ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getAllowedResourcesResponse);

      if (getAllowedResourcesResponse.getResourcesList().size() > 0) {
        List<String> resourcesIds = new ArrayList<>();
        for (Resources resources : getAllowedResourcesResponse.getResourcesList()) {
          resourcesIds.addAll(resources.getResourceIdsList());
        }
        return resourcesIds;
      } else {
        return Collections.emptyList();
      }
    } catch (StatusRuntimeException ex) {
      return (List<String>)
          ModelDBUtils.retryOrThrowException(
              ex,
              retry,
              (ModelDBUtils.RetryCallInterface<List<String>>)
                  (retry1) ->
                      getSelfAllowedResources(
                          retry1, modelDBServiceResourceTypes, modelDBServiceActions));
    }
  }

  @Override
  public List<String> getSelfDirectlyAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions) {
    return getSelfDirectlyAllowedResources(
        true, modelDBServiceResourceTypes, modelDBServiceActions);
  }

  private List<String> getSelfDirectlyAllowedResources(
      boolean retry,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions) {
    Action action =
        Action.newBuilder()
            .setService(ServiceEnum.Service.MODELDB_SERVICE)
            .setModeldbServiceAction(modelDBServiceActions)
            .build();
    GetSelfAllowedResources getAllowedResourcesRequest =
        GetSelfAllowedResources.newBuilder()
            .addActions(action)
            .setResourceType(
                ResourceType.newBuilder()
                    .setModeldbServiceResourceType(modelDBServiceResourceTypes))
            .setService(Service.MODELDB_SERVICE)
            .build();
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);
      Metadata requestHeaders = AuthInterceptor.METADATA_INFO.get();
      GetSelfAllowedResources.Response getAllowedResourcesResponse =
          authServiceChannel
              .getAuthzServiceBlockingStub(requestHeaders)
              .getSelfDirectlyAllowedResources(getAllowedResourcesRequest);
      LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(
          ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getAllowedResourcesResponse);

      if (getAllowedResourcesResponse.getResourcesList().size() > 0) {
        List<String> getSelfDirectlyAllowedResourceIds = new ArrayList<>();
        for (Resources resources : getAllowedResourcesResponse.getResourcesList()) {
          getSelfDirectlyAllowedResourceIds.addAll(resources.getResourceIdsList());
        }
        return getSelfDirectlyAllowedResourceIds;
      } else {
        return Collections.emptyList();
      }
    } catch (StatusRuntimeException ex) {
      return (List<String>)
          ModelDBUtils.retryOrThrowException(
              ex,
              retry,
              (ModelDBUtils.RetryCallInterface<List<String>>)
                  (retry1) ->
                      getSelfDirectlyAllowedResources(
                          retry1, modelDBServiceResourceTypes, modelDBServiceActions));
    }
  }

  @Override
  public List<String> getAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      CollaboratorBase collaboratorBase) {
    return getAllowedResources(
        true, modelDBServiceResourceTypes, modelDBServiceActions, collaboratorBase);
  }

  private List<String> getAllowedResources(
      boolean retry,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      CollaboratorBase collaboratorBase) {
    Action action =
        Action.newBuilder()
            .setService(ServiceEnum.Service.MODELDB_SERVICE)
            .setModeldbServiceAction(modelDBServiceActions)
            .build();
    Entities entity = collaboratorBase.getEntities();
    GetAllowedResources getAllowedResourcesRequest =
        GetAllowedResources.newBuilder()
            .addActions(action)
            .addEntities(entity)
            .setResourceType(
                ResourceType.newBuilder()
                    .setModeldbServiceResourceType(modelDBServiceResourceTypes))
            .setService(Service.MODELDB_SERVICE)
            .build();
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);
      Metadata requestHeaders = AuthInterceptor.METADATA_INFO.get();
      GetAllowedResources.Response getAllowedResourcesResponse =
          authServiceChannel
              .getAuthzServiceBlockingStub(requestHeaders)
              .getAllowedResources(getAllowedResourcesRequest);
      LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(
          ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getAllowedResourcesResponse);

      if (getAllowedResourcesResponse.getResourcesList().size() > 0) {
        List<String> resourcesIds = new ArrayList<>();
        for (Resources resources : getAllowedResourcesResponse.getResourcesList()) {
          resourcesIds.addAll(resources.getResourceIdsList());
        }
        return resourcesIds;
      } else {
        return Collections.emptyList();
      }
    } catch (StatusRuntimeException ex) {
      return (List<String>)
          ModelDBUtils.retryOrThrowException(
              ex,
              retry,
              (ModelDBUtils.RetryCallInterface<List<String>>)
                  (retry1) ->
                      getAllowedResources(
                          retry1,
                          modelDBServiceResourceTypes,
                          modelDBServiceActions,
                          collaboratorBase));
    }
  }

  @Override
  public GeneratedMessageV3 getTeamById(String teamId) {
    return getTeamById(true, teamId);
  }

  public GeneratedMessageV3 getTeamById(boolean retry, String teamId) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      GetTeamById getTeamById = GetTeamById.newBuilder().setTeamId(teamId).build();
      GetTeamById.Response getTeamByIdResponse =
          authServiceChannel.getTeamServiceBlockingStub().getTeamById(getTeamById);
      return getTeamByIdResponse.getTeam();
    } catch (StatusRuntimeException ex) {
      return (GeneratedMessageV3)
          ModelDBUtils.retryOrThrowException(
              ex,
              retry,
              (ModelDBUtils.RetryCallInterface<GeneratedMessageV3>)
                  (retry1) -> getTeamById(teamId));
    }
  }

  @Override
  public GeneratedMessageV3 getTeamByName(String orgId, String teamName) {
    return getTeamByName(true, orgId, teamName);
  }

  private GeneratedMessageV3 getTeamByName(boolean retry, String orgId, String teamName) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      GetTeamByName getTeamByName =
          GetTeamByName.newBuilder().setTeamName(teamName).setOrgId(orgId).build();
      GetTeamByName.Response getTeamByNameResponse =
          authServiceChannel.getTeamServiceBlockingStub().getTeamByName(getTeamByName);
      return getTeamByNameResponse.getTeam();
    } catch (StatusRuntimeException ex) {
      return (GeneratedMessageV3)
          ModelDBUtils.retryOrThrowException(
              ex,
              retry,
              (ModelDBUtils.RetryCallInterface<GeneratedMessageV3>)
                  (retry1) -> getTeamByName(retry1, orgId, teamName));
    }
  }

  @Override
  public GeneratedMessageV3 getOrgById(String orgId) {
    return getOrgById(true, orgId);
  }

  private GeneratedMessageV3 getOrgById(boolean retry, String orgId) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      GetOrganizationById getOrgById = GetOrganizationById.newBuilder().setOrgId(orgId).build();
      GetOrganizationById.Response getOrgByIdResponse =
          authServiceChannel.getOrganizationServiceBlockingStub().getOrganizationById(getOrgById);
      return getOrgByIdResponse.getOrganization();
    } catch (StatusRuntimeException ex) {
      return (GeneratedMessageV3)
          ModelDBUtils.retryOrThrowException(
              ex,
              retry,
              (ModelDBUtils.RetryCallInterface<GeneratedMessageV3>)
                  (retry1) -> getOrgById(retry1, orgId));
    }
  }

  @Override
  public GeneratedMessageV3 getOrgByName(String name) {
    return getOrgByName(true, name);
  }

  private GeneratedMessageV3 getOrgByName(boolean retry, String name) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      GetOrganizationByName getOrgByName =
          GetOrganizationByName.newBuilder().setOrgName(name).build();
      GetOrganizationByName.Response getOrgByNameResponse =
          authServiceChannel
              .getOrganizationServiceBlockingStub()
              .getOrganizationByName(getOrgByName);
      return getOrgByNameResponse.getOrganization();
    } catch (StatusRuntimeException ex) {
      return (GeneratedMessageV3)
          ModelDBUtils.retryOrThrowException(
              ex,
              retry,
              (ModelDBUtils.RetryCallInterface<GeneratedMessageV3>)
                  (retry1) -> getOrgByName(retry1, name));
    }
  }

  private List<String> getReadOnlyAccessibleResourceIds(
      boolean isHostUser,
      CollaboratorBase userInfo,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {

    Set<String> resourceIdsSet = new HashSet<>();
    if (userInfo != null && userInfo.getVertaId() != null) {
      List<String> accessibleResourceIds;
      if (isHostUser) {
        accessibleResourceIds =
            getAllowedResources(modelDBServiceResourceTypes, ModelDBServiceActions.READ, userInfo);
      } else {
        accessibleResourceIds =
            getSelfAllowedResources(modelDBServiceResourceTypes, ModelDBServiceActions.READ);
      }
      resourceIdsSet.addAll(accessibleResourceIds);
      LOGGER.debug(
          "Accessible " + modelDBServiceResourceTypes + " Ids size is {}",
          accessibleResourceIds.size());
    }

    return new ArrayList<>(resourceIdsSet);
  }

  @Override
  public List<String> getAccessibleResourceIds(
      CollaboratorBase hostUserInfo,
      CollaboratorBase currentLoginUserInfo,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      List<String> requestedResourceIds) {
    List<String> accessibleResourceIds;
    if (hostUserInfo != null) {
      accessibleResourceIds =
          getReadOnlyAccessibleResourceIds(true, hostUserInfo, modelDBServiceResourceTypes);
    } else {
      accessibleResourceIds =
          getReadOnlyAccessibleResourceIds(
              false, currentLoginUserInfo, modelDBServiceResourceTypes);
    }

    if (requestedResourceIds != null && !requestedResourceIds.isEmpty()) {
      accessibleResourceIds.retainAll(requestedResourceIds);
    }
    return accessibleResourceIds;
  }

  @Override
  public List<String> getAccessibleResourceIdsByActions(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      List<String> requestedIdList) {
    if (requestedIdList.size() == 1) {
      isSelfAllowed(modelDBServiceResourceTypes, modelDBServiceActions, requestedIdList.get(0));
      return requestedIdList;
    } else {
      List<String> allowedResourceIdList =
          getSelfAllowedResources(modelDBServiceResourceTypes, modelDBServiceActions);
      // Validate if current user has access to the entity or not
      allowedResourceIdList.retainAll(requestedIdList);
      return allowedResourceIdList;
    }
  }

  private Optional<Workspace> getWorkspaceByLegacyId(
      final String legacyWorkspaceId, final WorkspaceType workspaceType) {
    if (legacyWorkspaceId == null || legacyWorkspaceId.isEmpty()) {
      return Optional.empty();
    }
    try (final AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info("Fetching workspace " + legacyWorkspaceId);
      final Workspace workspace =
          authServiceChannel
              .getWorkspaceServiceBlockingStub()
              .getWorkspaceByLegacyId(
                  GetWorkspaceByLegacyId.newBuilder()
                      .setId(legacyWorkspaceId)
                      .setWorkspaceType(workspaceType)
                      .build());
      LOGGER.info("Got workspace " + workspace);
      return Optional.of(workspace);
    } catch (StatusRuntimeException ex) {
      ModelDBUtils.retryOrThrowException(
          ex, false, (ModelDBUtils.RetryCallInterface<Void>) (retry1) -> null);
    }
    return Optional.empty();
  }

  @Override
  public WorkspaceDTO getWorkspaceDTOByWorkspaceName(
      UserInfo currentLoginUserInfo, String workspaceName) {
    WorkspaceDTO workspaceDTO = new WorkspaceDTO();
    workspaceDTO.setWorkspaceName(workspaceName);

    /*from the name for workspace, get the workspace id and type.
    if no workspace is present assume user's personal workspace*/
    if (workspaceName == null
        || workspaceName.isEmpty()
        || workspaceName.equalsIgnoreCase(
            authService.getUsernameFromUserInfo(currentLoginUserInfo))) {
      String vertaId = authService.getVertaIdFromUserInfo(currentLoginUserInfo);
      workspaceDTO.setWorkspaceId(vertaId);
      workspaceDTO.setWorkspaceType(WorkspaceType.USER);
      workspaceDTO.setWorkspaceName(authService.getUsernameFromUserInfo(currentLoginUserInfo));
      Optional<Workspace> workspace = getWorkspaceByLegacyId(vertaId, WorkspaceType.USER);
      if (workspace.isPresent()) {
        workspaceDTO.setWorkspaceServiceId(workspace.get().getId());
      }
    } else {
      try {
        final String legacyWorkspaceId = new CollaboratorOrg(getOrgByName(workspaceName)).getId();
        workspaceDTO.setWorkspaceId(legacyWorkspaceId);
        workspaceDTO.setWorkspaceType(WorkspaceType.ORGANIZATION);
        workspaceDTO.setWorkspaceName(workspaceName);
        Optional<Workspace> workspace =
            getWorkspaceByLegacyId(legacyWorkspaceId, WorkspaceType.ORGANIZATION);
        if (workspace.isPresent()) {
          workspaceDTO.setWorkspaceServiceId(workspace.get().getId());
        }
      } catch (StatusRuntimeException e) {
        CollaboratorUser collaboratorUser =
            new CollaboratorUser(
                authService,
                authService.getUserInfo(workspaceName, ModelDBConstants.UserIdentifier.USER_NAME));
        workspaceDTO.setWorkspaceId(collaboratorUser.getId());
        workspaceDTO.setWorkspaceType(WorkspaceType.USER);
        workspaceDTO.setWorkspaceName(workspaceName);
        Optional<Workspace> workspace =
            getWorkspaceByLegacyId(collaboratorUser.getId(), WorkspaceType.USER);
        if (workspace.isPresent()) {
          workspaceDTO.setWorkspaceServiceId(workspace.get().getId());
        }
      }
    }
    return workspaceDTO;
  }

  @Override
  public Workspace getWorkspaceByWorkspaceName(
      UserInfo currentLoginUserInfo, String workspaceName) {
    /*from the name for workspace, get the workspace id and type.
    if no workspace is present assume user's personal workspace*/
    if (workspaceName == null || workspaceName.isEmpty()) {
      return authService.workspaceIdByName(
          true, authService.getUsernameFromUserInfo(currentLoginUserInfo));
    } else {
      return authService.workspaceIdByName(true, workspaceName);
    }
  }

  /**
   * Given the workspace id and type, returns WorkspaceDTO which has the id, name and type for the
   * workspace.
   */
  @Override
  public WorkspaceDTO getWorkspaceDTOByWorkspaceId(
      UserInfo currentLoginUserInfo, String workspaceId, Integer workspaceType) {
    WorkspaceDTO workspaceDTO = new WorkspaceDTO();
    workspaceDTO.setWorkspaceId(workspaceId);

    switch (workspaceType) {
      case WorkspaceType.ORGANIZATION_VALUE:
        Organization organization = (Organization) getOrgById(workspaceId);
        workspaceDTO.setWorkspaceType(WorkspaceType.ORGANIZATION);
        workspaceDTO.setWorkspaceName(organization.getName());
        return workspaceDTO;
      case WorkspaceType.USER_VALUE:
        workspaceDTO.setWorkspaceType(WorkspaceType.USER);
        if (currentLoginUserInfo == null) {
          currentLoginUserInfo = authService.getCurrentLoginUserInfo();
        }
        if (workspaceId.equalsIgnoreCase(
            authService.getVertaIdFromUserInfo(currentLoginUserInfo))) {
          workspaceDTO.setWorkspaceName(authService.getUsernameFromUserInfo(currentLoginUserInfo));
        } else {
          UserInfo userInfo =
              authService.getUserInfo(workspaceId, ModelDBConstants.UserIdentifier.VERTA_ID);
          workspaceDTO.setWorkspaceName(authService.getUsernameFromUserInfo(userInfo));
        }
        return workspaceDTO;
      default:
        return null;
    }
  }

  @Override
  public List<Organization> listMyOrganizations() {
    return listMyOrganizations(true);
  }

  private List<Organization> listMyOrganizations(boolean retry) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      ListMyOrganizations listMyOrganizations = ListMyOrganizations.newBuilder().build();
      ListMyOrganizations.Response listMyOrganizationsResponse =
          authServiceChannel
              .getOrganizationServiceBlockingStub()
              .listMyOrganizations(listMyOrganizations);
      return listMyOrganizationsResponse.getOrganizationsList();
    } catch (StatusRuntimeException ex) {
      return (List<Organization>)
          ModelDBUtils.retryOrThrowException(
              ex,
              retry,
              (ModelDBUtils.RetryCallInterface<List<Organization>>) this::listMyOrganizations);
    }
  }

  private boolean createWorkspacePermissions(
      Optional<Long> workspaceId,
      Optional<String> workspaceName,
      String resourceId,
      String resourceName,
      Optional<Long> ownerId,
      ModelDBServiceResourceTypes resourceType,
      CollaboratorPermissions permissions,
      ResourceVisibility resourceVisibility) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info("Calling CollaboratorService to create resources");
      ResourceType modeldbServiceResourceType =
          ResourceType.newBuilder().setModeldbServiceResourceType(resourceType).build();
      SetResource.Builder setResourcesBuilder =
          SetResource.newBuilder()
              .setService(Service.MODELDB_SERVICE)
              .setResourceType(modeldbServiceResourceType)
              .setResourceId(resourceId)
              .setResourceName(resourceName)
              .setVisibility(resourceVisibility);

      if (resourceVisibility.equals(ResourceVisibility.ORG_CUSTOM)) {
        setResourcesBuilder.setCollaboratorType(permissions.getCollaboratorType());
        setResourcesBuilder.setCanDeploy(permissions.getCanDeploy());
      }

      if (ownerId.isPresent()) {
        setResourcesBuilder.setOwnerId(ownerId.get());
      }
      if (workspaceId.isPresent()) {
        setResourcesBuilder.setWorkspaceId(workspaceId.get());
      } else if (workspaceName.isPresent()) {
        setResourcesBuilder = setResourcesBuilder.setWorkspaceName(workspaceName.get());
      } else {
        throw new IllegalArgumentException(
            "workspaceId and workspaceName are both empty.  One must be provided.");
      }
      SetResource.Response setResourcesResponse =
          authServiceChannel
              .getCollaboratorServiceBlockingStub()
              .setResource(setResourcesBuilder.build());

      LOGGER.info("SetResources message sent.  Response: " + setResourcesResponse);
      return true;
    } catch (StatusRuntimeException ex) {
      LOGGER.error(ex);
      ModelDBUtils.retryOrThrowException(ex, false, retry -> null);
    }
    return false;
  }

  @Override
  public GetResourcesResponseItem getEntityResource(
      String entityId, ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    List<GetResourcesResponseItem> responseItems =
        getResourceItems(
            null, new HashSet<>(Collections.singletonList(entityId)), modelDBServiceResourceTypes);
    if (responseItems.size() > 1) {
      LOGGER.warn(
          "Role service returned {}"
              + " resource response items fetching {} resource, but only expected 1. ID: {}",
          responseItems.size(),
          modelDBServiceResourceTypes.name(),
          entityId);
    }
    final Optional<GetResourcesResponseItem> responseItem =
        responseItems.stream()
            .filter(
                item ->
                    item.getResourceType().getModeldbServiceResourceType()
                            == modelDBServiceResourceTypes
                        && item.getService() == ServiceEnum.Service.MODELDB_SERVICE)
            .findFirst();
    if (responseItem.isPresent()) {
      return responseItem.get();
    }
    throw new IllegalArgumentException(
        "Failed to locate "
            + modelDBServiceResourceTypes.name()
            + " resources in UAC for "
            + modelDBServiceResourceTypes.name()
            + " ID "
            + entityId);
  }

  @Override
  public boolean entityResourceExists(
      Workspace workspace,
      String entityName,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    return getResourcesResponseItems(
                workspace, entityName, Collections.emptySet(), modelDBServiceResourceTypes)
            .size()
        > 0;
  }

  @Override
  public List<GetResourcesResponseItem> getResourceItems(
      Workspace workspace,
      Set<String> resourceIds,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    return getResourcesResponseItems(workspace, null, resourceIds, modelDBServiceResourceTypes);
  }

  private List<GetResourcesResponseItem> getResourcesResponseItems(
      Workspace workspace,
      String entityName,
      Set<String> resourceIds,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      ResourceType resourceType =
          ResourceType.newBuilder()
              .setModeldbServiceResourceType(modelDBServiceResourceTypes)
              .build();
      Resources.Builder resources =
          Resources.newBuilder()
              .setResourceType(resourceType)
              .setService(ServiceEnum.Service.MODELDB_SERVICE);

      if (resourceIds != null && !resourceIds.isEmpty()) {
        resources.addAllResourceIds(resourceIds);
      }

      if (entityName != null) {
        resources.setResourceName(entityName);
      }

      GetResources.Builder builder = GetResources.newBuilder().setResources(resources.build());
      if (workspace != null) {
        builder.setWorkspaceId(workspace.getId());
      }
      final GetResources.Response response =
          authServiceChannel.getCollaboratorServiceBlockingStub().getResources(builder.build());
      return response.getItemList();
    } catch (StatusRuntimeException ex) {
      LOGGER.error(ex);
      throw ex;
    }
  }

  @Override
  public boolean deleteResources(Resources resources) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info("Calling CollaboratorService to delete resources");
      DeleteResources deleteResources =
          DeleteResources.newBuilder().setResources(resources).build();
      DeleteResources.Response response =
          authServiceChannel.getCollaboratorServiceBlockingStub().deleteResources(deleteResources);
      LOGGER.info("DeleteResources message sent.  Response: " + response);
      return true;
    } catch (StatusRuntimeException ex) {
      LOGGER.error(ex);
      ModelDBUtils.retryOrThrowException(ex, false, retry -> null);
    }
    return false;
  }

  @Override
  public boolean deleteEntityResources(
      List<String> entityIds, ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    if (entityIds.isEmpty()) {
      return true;
    }
    ResourceType modeldbServiceResourceType =
        ResourceType.newBuilder()
            .setModeldbServiceResourceType(modelDBServiceResourceTypes)
            .build();
    Resources resources =
        Resources.newBuilder()
            .setResourceType(modeldbServiceResourceType)
            .setService(Service.MODELDB_SERVICE)
            .addAllResourceIds(entityIds)
            .build();
    return deleteResources(resources);
  }

  @Override
  public boolean createWorkspacePermissions(
      String workspaceName,
      String resourceId,
      String resourceName,
      Optional<Long> ownerId,
      ModelDBServiceResourceTypes resourceType,
      CollaboratorPermissions permissions,
      ResourceVisibility visibility) {
    return createWorkspacePermissions(
        Optional.empty(),
        Optional.of(workspaceName),
        resourceId,
        resourceName,
        ownerId,
        resourceType,
        permissions,
        visibility);
  }

  @Override
  public boolean createWorkspacePermissions(
      Long workspaceId,
      Optional<WorkspaceType> workspaceType,
      String resourceId,
      String resourceName,
      Optional<Long> ownerId,
      ModelDBServiceResourceTypes resourceType,
      CollaboratorPermissions permissions,
      ResourceVisibility visibility) {
    return createWorkspacePermissions(
        Optional.of(workspaceId),
        Optional.empty(),
        resourceId,
        resourceName,
        ownerId,
        resourceType,
        permissions,
        visibility);
  }

  @Override
  public void createWorkspacePermissions(
      String workspaceId,
      WorkspaceType workspaceType,
      String resourceId,
      String roleAdminName,
      ModelDBServiceResourceTypes resourceType,
      boolean orgScopedPublic,
      String globalSharing) {
    if (workspaceId != null && !workspaceId.isEmpty()) {
      final CollaboratorUser collaboratorUser;
      switch (workspaceType) {
        case ORGANIZATION:
          if (orgScopedPublic) {
            String globalSharingRoleName =
                new StringBuilder()
                    .append("O_")
                    .append(workspaceId)
                    .append(globalSharing)
                    .toString();
            Role globalSharingRole =
                getRoleByName(
                    globalSharingRoleName, RoleScope.newBuilder().setOrgId(workspaceId).build());
            createRoleBinding(
                globalSharingRole, new CollaboratorOrg(workspaceId), resourceId, resourceType);
          }
          Organization org = (Organization) getOrgById(workspaceId);
          collaboratorUser = new CollaboratorUser(authService, org.getOwnerId());
          break;
        case USER:
          collaboratorUser = new CollaboratorUser(authService, workspaceId);
          break;
        default:
          return;
      }
      Role admin = getRoleByName(roleAdminName, null);
      createRoleBinding(admin, collaboratorUser, resourceId, resourceType);
    }
  }

  @Override
  public List<String> getWorkspaceRoleBindings(
      String workspaceId,
      WorkspaceType workspaceType,
      String resourceId,
      String roleName,
      ModelDBServiceResourceTypes resourceTypes,
      boolean orgScopedPublic,
      String globalSharing) {
    List<String> workspaceRoleBindingList = new ArrayList<>();
    if (workspaceId != null && !workspaceId.isEmpty()) {
      try {
        CollaboratorUser collaboratorUser;
        switch (workspaceType) {
          case ORGANIZATION:
            if (orgScopedPublic) {
              String globalSharingRoleName =
                  new StringBuilder()
                      .append("O_")
                      .append(workspaceId)
                      .append(globalSharing)
                      .toString();

              String globalSharingRoleBindingName =
                  buildRoleBindingName(
                      globalSharingRoleName,
                      resourceId,
                      new CollaboratorOrg(workspaceId),
                      resourceTypes.name());
              if (globalSharingRoleBindingName != null) {
                workspaceRoleBindingList.add(globalSharingRoleBindingName);
              }
            }
            Organization org = (Organization) getOrgById(workspaceId);
            collaboratorUser = new CollaboratorUser(authService, org.getOwnerId());
            break;
          case USER:
            collaboratorUser = new CollaboratorUser(authService, workspaceId);
            break;
          default:
            return null;
        }
        String roleBindingName =
            buildRoleBindingName(roleName, resourceId, collaboratorUser, resourceTypes.name());
        if (roleBindingName != null) {
          workspaceRoleBindingList.add(roleBindingName);
        }
      } catch (Exception e) {
        if (!e.getMessage().contains("Details: Doesn't exist")) {
          throw e;
        }
        LOGGER.info("Workspace ({}) not found on UAC", workspaceId);
      }
    }
    return workspaceRoleBindingList;
  }

  @Override
  public boolean deleteAllResources(
      List<String> resourceIds, ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    return deleteAllResources(true, resourceIds, modelDBServiceResourceTypes);
  }

  private boolean deleteAllResources(
      boolean retry,
      List<String> resourceIds,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    RemoveResources removeAllCollaboratorsRequest =
        RemoveResources.newBuilder()
            .addAllResourceIds(resourceIds)
            .setResourceType(
                ResourceType.newBuilder()
                    .setModeldbServiceResourceType(modelDBServiceResourceTypes)
                    .build())
            .build();
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);

      RemoveResources.Response removeAllCollaboratorResponse =
          authServiceChannel
              .getRoleServiceBlockingStub()
              .removeResources(removeAllCollaboratorsRequest);
      LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(
          ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, removeAllCollaboratorResponse);

      return removeAllCollaboratorResponse.getStatus();
    } catch (StatusRuntimeException ex) {
      return (Boolean)
          ModelDBUtils.retryOrThrowException(
              ex,
              retry,
              (ModelDBUtils.RetryCallInterface<Boolean>)
                  (retry1) -> deleteAllResources(retry1, resourceIds, modelDBServiceResourceTypes));
    }
  }

  public boolean checkConnectionsBasedOnPrivileges(
      ModelDBResourceEnum.ModelDBServiceResourceTypes serviceResourceTypes,
      ModelDBActionEnum.ModelDBServiceActions serviceActions,
      String resourceId) {
    try {
      isSelfAllowed(serviceResourceTypes, serviceActions, resourceId);
      return true;
    } catch (Exception ex) {
      LOGGER.debug(ex.getMessage());
      return false;
    }
  }
}
