package ai.verta.modeldb.common.authservice;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.WorkspaceTypeEnum;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.collaborator.CollaboratorBase;
import ai.verta.modeldb.common.collaborator.CollaboratorOrg;
import ai.verta.modeldb.common.collaborator.CollaboratorUser;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.exceptions.PermissionDeniedException;
import ai.verta.uac.*;
import ai.verta.uac.ServiceEnum.Service;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.StatusRuntimeException;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RoleServiceUtils implements RoleService {
  private static final Logger LOGGER = LogManager.getLogger(RoleServiceUtils.class);
  protected final UAC uac;
  protected AuthService authService;
  private final Integer timeout;

  public RoleServiceUtils(AuthService authService, Integer timeout, UAC uac) {
    this.authService = authService;
    this.timeout = timeout;
    this.uac = uac;
  }

  /**
   * @param workspaceId workspace.id
   * @param workspaceName workspace.name
   * @param resourceId project.id, repository.id etc.
   * @param resourceName project.name, repository.name etc.
   * @param ownerId: parameter added for migration where we should have to populate entity owner.For
   *     other UAC will populate the owner ID
   * @param resourceType PROJECT, REPOSITORY, DATASET
   * @param permissions CollaboratorPermissions.
   * @param resourceVisibility ResourceVisibility
   * @return {@link Boolean} true, false based on success call
   */
  @Override
  public boolean createWorkspacePermissions(
      Optional<Long> workspaceId,
      Optional<String> workspaceName,
      String resourceId,
      String resourceName,
      Optional<Long> ownerId,
      ModelDBServiceResourceTypes resourceType,
      CollaboratorPermissions permissions,
      ResourceVisibility resourceVisibility) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      LOGGER.trace("Calling CollaboratorService to create resources");
      var modeldbServiceResourceType =
          ResourceType.newBuilder().setModeldbServiceResourceType(resourceType).build();
      var setResourcesBuilder =
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
      var setResourcesResponse =
          authServiceChannel
              .getCollaboratorServiceBlockingStub()
              .setResource(setResourcesBuilder.build());

      LOGGER.trace("SetResources message sent.  Response: {}", setResourcesResponse);
      return true;
    } catch (StatusRuntimeException ex) {
      LOGGER.trace(ex);
      CommonUtils.retryOrThrowException(ex, false, retry -> null, timeout);
    }
    return false;
  }

  public boolean deleteResourcesWithServiceUser(Resources resources) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      LOGGER.trace("Calling CollaboratorService to delete resources");
      var deleteResources = DeleteResources.newBuilder().setResources(resources).build();
      var response =
          authServiceChannel
              .getCollaboratorServiceBlockingStubForServiceUser()
              .deleteResources(deleteResources);
      LOGGER.trace("DeleteResources message sent.  Response: {}", response);
      return true;
    } catch (StatusRuntimeException ex) {
      LOGGER.trace(ex);
      CommonUtils.retryOrThrowException(ex, false, retry -> null, timeout);
    }
    return false;
  }

  @Override
  public boolean deleteEntityResourcesWithServiceUser(
      List<String> entityIds, ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    var modeldbServiceResourceType =
        ResourceType.newBuilder()
            .setModeldbServiceResourceType(modelDBServiceResourceTypes)
            .build();
    var resources =
        Resources.newBuilder()
            .setResourceType(modeldbServiceResourceType)
            .setService(Service.MODELDB_SERVICE)
            .addAllResourceIds(entityIds)
            .build();
    return deleteResourcesWithServiceUser(resources);
  }

  @Override
  public GetResourcesResponseItem getEntityResource(
      Optional<String> entityId,
      Optional<String> workspaceName,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      List<GetResourcesResponseItem> responseItems =
          getGetResourcesResponseItems(
              entityId,
              Optional.empty(),
              workspaceName,
              modelDBServiceResourceTypes,
              authServiceChannel);
      if (responseItems.size() > 1) {
        var mdbServiceTypeName = modelDBServiceResourceTypes.name();
        LOGGER.warn(
            "Role service returned {}"
                + " resource response items fetching {} resource, but only expected 1. ID: {}",
            responseItems.size(),
            mdbServiceTypeName,
            entityId);
      }
      Optional<GetResourcesResponseItem> responseItem = responseItems.stream().findFirst();
      if (responseItem.isPresent()) {
        return responseItem.get();
      } else {
        StringBuilder errorMessage =
            new StringBuilder("Failed to locate ")
                .append(modelDBServiceResourceTypes.name())
                .append(" resources in UAC for ")
                .append(modelDBServiceResourceTypes.name())
                .append(" ID ")
                .append(entityId);
        throw new NotFoundException(errorMessage.toString());
      }
    } catch (StatusRuntimeException ex) {
      LOGGER.trace(ex);
      throw ex;
    }
  }

  @Override
  public List<GetResourcesResponseItem> getEntityResourcesByName(
      Optional<String> entityName,
      Optional<String> workspaceName,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      if (!entityName.isPresent()) {
        return Collections.emptyList();
      }
      List<GetResourcesResponseItem> responseItems =
          getGetResourcesResponseItems(
              Optional.empty(),
              entityName,
              workspaceName,
              modelDBServiceResourceTypes,
              authServiceChannel);
      if (!responseItems.isEmpty()) {
        return responseItems;
      } else {
        StringBuilder errorMessage =
            new StringBuilder("Failed to locate ")
                .append(modelDBServiceResourceTypes.name())
                .append(" resources in UAC for ")
                .append(modelDBServiceResourceTypes.name())
                .append(" Name ")
                .append(entityName.get());
        throw new NotFoundException(errorMessage.toString());
      }
    } catch (StatusRuntimeException ex) {
      LOGGER.trace(ex);
      throw ex;
    }
  }

  private List<GetResourcesResponseItem> getGetResourcesResponseItems(
      Optional<String> entityId,
      Optional<String> entityName,
      Optional<String> workspaceName,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      AuthServiceChannel authServiceChannel) {
    var resourceType =
        ResourceType.newBuilder()
            .setModeldbServiceResourceType(modelDBServiceResourceTypes)
            .build();
    Resources.Builder resources =
        Resources.newBuilder().setResourceType(resourceType).setService(Service.MODELDB_SERVICE);
    entityId.ifPresent(resources::addResourceIds);

    final var getResourcesBuilder = GetResources.newBuilder().setResources(resources);
    entityName.ifPresent(getResourcesBuilder::setResourceName);
    workspaceName.ifPresent(getResourcesBuilder::setWorkspaceName);

    final var response =
        authServiceChannel
            .getCollaboratorServiceBlockingStub()
            .getResources(getResourcesBuilder.build());
    return response.getItemList();
  }

  @Override
  public GeneratedMessageV3 getTeamById(String teamId) {
    return getTeamById(true, teamId);
  }

  public GeneratedMessageV3 getTeamById(boolean retry, String teamId) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      var getTeamById = GetTeamById.newBuilder().setTeamId(teamId).build();
      var getTeamByIdResponse =
          authServiceChannel.getTeamServiceBlockingStub().getTeamById(getTeamById);
      return getTeamByIdResponse.getTeam();
    } catch (StatusRuntimeException ex) {
      return (GeneratedMessageV3)
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<GeneratedMessageV3>) (retry1) -> getTeamById(teamId),
              timeout);
    }
  }

  @Override
  public GeneratedMessageV3 getOrgById(String orgId) {
    return getOrgById(true, orgId);
  }

  private GeneratedMessageV3 getOrgById(boolean retry, String orgId) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      GetOrganizationById getOrgById = GetOrganizationById.newBuilder().setOrgId(orgId).build();
      var getOrgByIdResponse =
          authServiceChannel.getOrganizationServiceBlockingStub().getOrganizationById(getOrgById);
      return getOrgByIdResponse.getOrganization();
    } catch (StatusRuntimeException ex) {
      return (GeneratedMessageV3)
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<GeneratedMessageV3>)
                  (retry1) -> getOrgById(retry1, orgId),
              timeout);
    }
  }

  /**
   * getResourceItems method is the main roleService method at MDB which actually call to the UAC
   * using endpoint getResources
   *
   * @param workspace: workspace
   * @param resourceIds: requested resource ids
   * @param modelDBServiceResourceTypes: modelDBServiceResourceTypes like PROJECT, REPOSITORY
   * @return {@link List}: list of the resource details
   */
  @Override
  public List<GetResourcesResponseItem> getResourceItems(
      Workspace workspace,
      Set<String> resourceIds,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      var resourceType =
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

      var builder = GetResources.newBuilder().setResources(resources.build());
      if (workspace != null) {
        builder.setWorkspaceId(workspace.getId());
      }
      final var response =
          authServiceChannel.getCollaboratorServiceBlockingStub().getResources(builder.build());
      return response.getItemList();
    } catch (StatusRuntimeException ex) {
      LOGGER.trace(ex);
      throw ex;
    }
  }

  @Override
  public List<GetResourcesResponseItem> getResourceItemsSpecialPersonalWorkspace(
      Workspace workspace,
      Set<String> resourceIds,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      var resourceType =
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

      var builder = GetResources.newBuilder().setResources(resources.build());
      if (workspace != null) {
        builder.setWorkspaceId(workspace.getId());
      }
      final var response =
          authServiceChannel
              .getCollaboratorServiceBlockingStub()
              .getResourcesSpecialPersonalWorkspace(builder.build());
      return response.getItemList();
    } catch (StatusRuntimeException ex) {
      LOGGER.trace(ex);
      throw ex;
    }
  }

  @Override
  public List<String> getWorkspaceRoleBindings(
      String workspaceId,
      WorkspaceTypeEnum.WorkspaceType workspaceType,
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
              var globalSharingRoleName =
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
            return workspaceRoleBindingList;
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

  private List<String> getReadOnlyAccessibleResourceIds(
      boolean isHostUser,
      CollaboratorBase userInfo,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {

    Set<String> resourceIdsSet = new HashSet<>();
    if (userInfo != null && userInfo.getVertaId() != null) {
      List<String> accessibleResourceIds;
      if (isHostUser) {
        accessibleResourceIds =
            getAllowedResources(
                modelDBServiceResourceTypes,
                ModelDBActionEnum.ModelDBServiceActions.READ,
                userInfo);
      } else {
        accessibleResourceIds =
            getSelfAllowedResources(
                modelDBServiceResourceTypes, ModelDBActionEnum.ModelDBServiceActions.READ);
      }
      resourceIdsSet.addAll(accessibleResourceIds);
      LOGGER.debug(
          "Accessible {} Ids size is {}",
          modelDBServiceResourceTypes,
          accessibleResourceIds.size());
    }

    return new ArrayList<>(resourceIdsSet);
  }

  @Override
  public List<String> getAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBActionEnum.ModelDBServiceActions modelDBServiceActions,
      CollaboratorBase collaboratorBase) {
    return getAllowedResources(
        true, modelDBServiceResourceTypes, modelDBServiceActions, collaboratorBase);
  }

  private List<String> getAllowedResources(
      boolean retry,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBActionEnum.ModelDBServiceActions modelDBServiceActions,
      CollaboratorBase collaboratorBase) {
    var action =
        Action.newBuilder()
            .setService(Service.MODELDB_SERVICE)
            .setModeldbServiceAction(modelDBServiceActions)
            .build();
    var entity = collaboratorBase.getEntities();
    var getAllowedResourcesRequest =
        GetAllowedResources.newBuilder()
            .addActions(action)
            .addEntities(entity)
            .setResourceType(
                ResourceType.newBuilder()
                    .setModeldbServiceResourceType(modelDBServiceResourceTypes))
            .setService(Service.MODELDB_SERVICE)
            .build();
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      LOGGER.trace(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);
      var getAllowedResourcesResponse =
          authServiceChannel
              .getAuthzServiceBlockingStub()
              .getAllowedResources(getAllowedResourcesRequest);
      LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getAllowedResourcesResponse);

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
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<List<String>>)
                  (retry1) ->
                      getAllowedResources(
                          retry1,
                          modelDBServiceResourceTypes,
                          modelDBServiceActions,
                          collaboratorBase),
              timeout);
    }
  }

  @Override
  public List<String> getSelfAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBActionEnum.ModelDBServiceActions modelDBServiceActions) {
    return getSelfAllowedResources(true, modelDBServiceResourceTypes, modelDBServiceActions);
  }

  private List<String> getSelfAllowedResources(
      boolean retry,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBActionEnum.ModelDBServiceActions modelDBServiceActions) {
    var action =
        Action.newBuilder()
            .setService(Service.MODELDB_SERVICE)
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
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      LOGGER.trace(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);
      var getAllowedResourcesResponse =
          authServiceChannel
              .getAuthzServiceBlockingStub()
              .getSelfAllowedResources(getAllowedResourcesRequest);
      LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getAllowedResourcesResponse);

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
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<List<String>>)
                  (retry1) ->
                      getSelfAllowedResources(
                          retry1, modelDBServiceResourceTypes, modelDBServiceActions),
              timeout);
    }
  }

  @Override
  public List<String> getSelfDirectlyAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBActionEnum.ModelDBServiceActions modelDBServiceActions) {
    return getSelfDirectlyAllowedResources(
        true, modelDBServiceResourceTypes, modelDBServiceActions);
  }

  private List<String> getSelfDirectlyAllowedResources(
      boolean retry,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBActionEnum.ModelDBServiceActions modelDBServiceActions) {
    var action =
        Action.newBuilder()
            .setService(Service.MODELDB_SERVICE)
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
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      LOGGER.trace(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);
      var getAllowedResourcesResponse =
          authServiceChannel
              .getAuthzServiceBlockingStub()
              .getSelfDirectlyAllowedResources(getAllowedResourcesRequest);
      LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getAllowedResourcesResponse);

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
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<List<String>>)
                  (retry1) ->
                      getSelfDirectlyAllowedResources(
                          retry1, modelDBServiceResourceTypes, modelDBServiceActions),
              timeout);
    }
  }

  @Override
  public void isSelfAllowed(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBActionEnum.ModelDBServiceActions modelDBServiceActions,
      String resourceId) {
    isSelfAllowed(true, modelDBServiceResourceTypes, modelDBServiceActions, resourceId);
  }

  private void isSelfAllowed(
      boolean retry,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBActionEnum.ModelDBServiceActions modelDBServiceActions,
      String resourceId) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      LOGGER.trace(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);
      var resourceBuilder =
          Resources.newBuilder()
              .setService(Service.MODELDB_SERVICE)
              .setResourceType(
                  ResourceType.newBuilder()
                      .setModeldbServiceResourceType(modelDBServiceResourceTypes));
      if (resourceId != null) {
        resourceBuilder.addResourceIds(resourceId);
      }
      var isSelfAllowedRequest =
          IsSelfAllowed.newBuilder()
              .addResources(resourceBuilder.build())
              .addActions(
                  Action.newBuilder()
                      .setService(Service.MODELDB_SERVICE)
                      .setModeldbServiceAction(modelDBServiceActions)
                      .build())
              .build();

      var isSelfAllowedResponse =
          authServiceChannel.getAuthzServiceBlockingStub().isSelfAllowed(isSelfAllowedRequest);
      LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, isSelfAllowedResponse);

      if (!isSelfAllowedResponse.getAllowed()) {
        throw new PermissionDeniedException("Access Denied");
      }
    } catch (StatusRuntimeException ex) {
      CommonUtils.retryOrThrowException(
          ex,
          retry,
          (CommonUtils.RetryCallInterface<Void>)
              retry1 -> {
                isSelfAllowed(
                    retry1, modelDBServiceResourceTypes, modelDBServiceActions, resourceId);
                return null;
              },
          timeout);
    }
  }

  @Override
  public List<String> getAccessibleResourceIdsByActions(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBActionEnum.ModelDBServiceActions modelDBServiceActions,
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

  @Override
  public Map<String, Actions> getSelfAllowedActionsBatch(
      List<String> resourceIds, ModelDBServiceResourceTypes type) {
    return getSelfAllowedActionsBatch(true, resourceIds, type);
  }

  private Map<String, Actions> getSelfAllowedActionsBatch(
      boolean retry, List<String> resourceIds, ModelDBServiceResourceTypes type) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      LOGGER.trace(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);
      var getSelfAllowedActionsBatch =
          GetSelfAllowedActionsBatch.newBuilder()
              .setResources(
                  Resources.newBuilder()
                      .setService(Service.MODELDB_SERVICE)
                      .addAllResourceIds(resourceIds)
                      .setResourceType(
                          ResourceType.newBuilder().setModeldbServiceResourceType(type))
                      .build())
              .build();

      var getSelfAllowedActionsBatchResponse =
          authServiceChannel
              .getAuthzServiceBlockingStub()
              .getSelfAllowedActionsBatch(getSelfAllowedActionsBatch);
      LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(
          CommonMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getSelfAllowedActionsBatchResponse);
      return getSelfAllowedActionsBatchResponse.getActionsMap();

    } catch (StatusRuntimeException ex) {
      return (Map<String, Actions>)
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<Map<String, Actions>>)
                  (retry1) -> getSelfAllowedActionsBatch(retry1, resourceIds, type),
              timeout);
    }
  }

  @Override
  public void createRoleBinding(
      String roleName,
      CollaboratorBase collaborator,
      String resourceId,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    String roleBindingName =
        buildRoleBindingName(
            roleName, resourceId, collaborator, modelDBServiceResourceTypes.name());
    RoleScope roleBindingScope = RoleScope.newBuilder().build();

    var newRoleBinding =
        RoleBinding.newBuilder()
            .setName(roleBindingName)
            .setScope(roleBindingScope)
            .setRoleName(roleName)
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

  private void setRoleBindingOnAuthService(boolean retry, RoleBinding roleBinding) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      LOGGER.trace(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);
      var setRoleBindingResponse =
          authServiceChannel
              .getRoleServiceBlockingStubForServiceUser()
              .setRoleBinding(SetRoleBinding.newBuilder().setRoleBinding(roleBinding).build());
      LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, setRoleBindingResponse);
    } catch (StatusRuntimeException ex) {
      CommonUtils.retryOrThrowException(
          ex,
          retry,
          (CommonUtils.RetryCallInterface<Void>)
              (retry1) -> {
                setRoleBindingOnAuthService(retry1, roleBinding);
                return null;
              },
          timeout);
    }
  }

  @Override
  public boolean deleteRoleBindingsUsingServiceUser(List<String> roleBindingNames) {
    return deleteRoleBindingsUsingServiceUser(true, roleBindingNames);
  }

  private boolean deleteRoleBindingsUsingServiceUser(boolean retry, List<String> roleBindingNames) {
    DeleteRoleBindings deleteRoleBindingRequest =
        DeleteRoleBindings.newBuilder().addAllRoleBindingNames(roleBindingNames).build();
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      LOGGER.trace(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);

      // TODO: try using futur stub than blocking stub
      var deleteRoleBindingResponse =
          authServiceChannel
              .getRoleServiceBlockingStubForServiceUser()
              .deleteRoleBindings(deleteRoleBindingRequest);
      LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, deleteRoleBindingResponse);

      return deleteRoleBindingResponse.getStatus();
    } catch (StatusRuntimeException ex) {
      return (Boolean)
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<Boolean>)
                  (retry1) -> deleteRoleBindingsUsingServiceUser(retry1, roleBindingNames),
              timeout);
    }
  }

  @Override
  public GeneratedMessageV3 getTeamByName(String orgId, String teamName) {
    return getTeamByName(true, orgId, teamName);
  }

  private GeneratedMessageV3 getTeamByName(boolean retry, String orgId, String teamName) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      var getTeamByName = GetTeamByName.newBuilder().setTeamName(teamName).setOrgId(orgId).build();
      GetTeamByName.Response getTeamByNameResponse =
          authServiceChannel.getTeamServiceBlockingStub().getTeamByName(getTeamByName);
      return getTeamByNameResponse.getTeam();
    } catch (StatusRuntimeException ex) {
      return (GeneratedMessageV3)
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<GeneratedMessageV3>)
                  (retry1) -> getTeamByName(retry1, orgId, teamName),
              timeout);
    }
  }

  @Override
  public GeneratedMessageV3 getOrgByName(String name) {
    return getOrgByName(true, name);
  }

  private GeneratedMessageV3 getOrgByName(boolean retry, String name) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      GetOrganizationByName getOrgByName =
          GetOrganizationByName.newBuilder().setOrgName(name).build();
      var getOrgByNameResponse =
          authServiceChannel
              .getOrganizationServiceBlockingStub()
              .getOrganizationByName(getOrgByName);
      return getOrgByNameResponse.getOrganization();
    } catch (StatusRuntimeException ex) {
      return (GeneratedMessageV3)
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<GeneratedMessageV3>)
                  (retry1) -> getOrgByName(retry1, name),
              timeout);
    }
  }

  @Override
  public List<Organization> listMyOrganizations() {
    return listMyOrganizations(true);
  }

  private List<Organization> listMyOrganizations(boolean retry) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      var listMyOrganizations = ListMyOrganizations.newBuilder().build();
      var listMyOrganizationsResponse =
          authServiceChannel
              .getOrganizationServiceBlockingStub()
              .listMyOrganizations(listMyOrganizations);
      return listMyOrganizationsResponse.getOrganizationsList();
    } catch (StatusRuntimeException ex) {
      return (List<Organization>)
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<List<Organization>>) this::listMyOrganizations,
              timeout);
    }
  }
}
