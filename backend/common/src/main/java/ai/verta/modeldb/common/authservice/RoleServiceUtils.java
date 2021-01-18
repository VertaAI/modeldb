package ai.verta.modeldb.common.authservice;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.WorkspaceTypeEnum;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.collaborator.CollaboratorBase;
import ai.verta.modeldb.common.collaborator.CollaboratorOrg;
import ai.verta.modeldb.common.collaborator.CollaboratorUser;
import ai.verta.modeldb.common.exceptions.PermissionDeniedException;
import ai.verta.uac.Action;
import ai.verta.uac.Actions;
import ai.verta.uac.CollaboratorPermissions;
import ai.verta.uac.DeleteResources;
import ai.verta.uac.DeleteRoleBinding;
import ai.verta.uac.DeleteRoleBindings;
import ai.verta.uac.Entities;
import ai.verta.uac.GetAllowedResources;
import ai.verta.uac.GetOrganizationById;
import ai.verta.uac.GetOrganizationByName;
import ai.verta.uac.GetResources;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.GetRoleBindingByName;
import ai.verta.uac.GetRoleByName;
import ai.verta.uac.GetSelfAllowedActionsBatch;
import ai.verta.uac.GetSelfAllowedResources;
import ai.verta.uac.GetTeamById;
import ai.verta.uac.GetTeamByName;
import ai.verta.uac.IsSelfAllowed;
import ai.verta.uac.ModelDBActionEnum;
import ai.verta.uac.Organization;
import ai.verta.uac.ResourceType;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.Resources;
import ai.verta.uac.Role;
import ai.verta.uac.RoleBinding;
import ai.verta.uac.RoleScope;
import ai.verta.uac.ServiceEnum;
import ai.verta.uac.ServiceEnum.Service;
import ai.verta.uac.SetResource;
import ai.verta.uac.SetRoleBinding;
import ai.verta.uac.Workspace;
import com.google.protobuf.GeneratedMessageV3;
import com.google.rpc.Code;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RoleServiceUtils implements RoleService {
  private static final Logger LOGGER = LogManager.getLogger(RoleServiceUtils.class);
  protected AuthService authService;
  private final String host;
  private final Integer port;
  private final String serviceUserEmail;
  private final String serviceUserDevKey;
  private final Context.Key<Metadata> metadataInfo;
  private Integer timeout;

  public RoleServiceUtils(
      AuthService authService,
      String host,
      Integer port,
      String serviceUserEmail,
      String serviceUserDevKey,
      Integer timeout,
      Context.Key<Metadata> metadataInfo) {
    this.authService = authService;
    this.host = host;
    this.port = port;
    this.serviceUserEmail = serviceUserEmail;
    this.serviceUserDevKey = serviceUserDevKey;
    this.timeout = timeout;
    this.metadataInfo = metadataInfo;
  }

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
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
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
      CommonUtils.retryOrThrowException(ex, false, retry -> null, timeout);
    }
    return false;
  }

  private AuthServiceChannel getAuthServiceChannel() {
    return new AuthServiceChannel(host, port, serviceUserEmail, serviceUserDevKey, metadataInfo);
  }

  public boolean deleteResources(Resources resources) {
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      LOGGER.info("Calling CollaboratorService to delete resources");
      DeleteResources deleteResources =
          DeleteResources.newBuilder().setResources(resources).build();
      DeleteResources.Response response =
          authServiceChannel.getCollaboratorServiceBlockingStub().deleteResources(deleteResources);
      LOGGER.info("DeleteResources message sent.  Response: " + response);
      return true;
    } catch (StatusRuntimeException ex) {
      LOGGER.error(ex);
      CommonUtils.retryOrThrowException(ex, false, retry -> null, timeout);
    }
    return false;
  }

  @Override
  public boolean deleteEntityResources(
      List<String> entityIds, ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
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
  public GetResourcesResponseItem getEntityResource(
      String entityId, ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    ResourceType resourceType =
        ResourceType.newBuilder()
            .setModeldbServiceResourceType(modelDBServiceResourceTypes)
            .build();
    Resources resources =
        Resources.newBuilder()
            .setResourceType(resourceType)
            .setService(Service.MODELDB_SERVICE)
            .addResourceIds(entityId)
            .build();
    List<GetResourcesResponseItem> responseItems = getResourceItems(Optional.of(resources));
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
                        && item.getService() == Service.MODELDB_SERVICE)
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

  public List<GetResourcesResponseItem> getResourceItems(Optional<Resources> filterTo) {
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      final GetResources.Builder getResourcesBuilder = GetResources.newBuilder();
      filterTo.ifPresent(getResourcesBuilder::setResources);

      final GetResources.Response response =
          authServiceChannel
              .getCollaboratorServiceBlockingStub()
              .getResources(getResourcesBuilder.build());
      return response.getItemList();
    } catch (StatusRuntimeException ex) {
      LOGGER.error(ex);
      throw ex;
    }
  }

  @Override
  public GeneratedMessageV3 getTeamById(String teamId) {
    return getTeamById(true, teamId);
  }

  public GeneratedMessageV3 getTeamById(boolean retry, String teamId) {
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      GetTeamById getTeamById = GetTeamById.newBuilder().setTeamId(teamId).build();
      GetTeamById.Response getTeamByIdResponse =
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
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      GetOrganizationById getOrgById = GetOrganizationById.newBuilder().setOrgId(orgId).build();
      GetOrganizationById.Response getOrgByIdResponse =
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
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
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
                getAllowedResources(modelDBServiceResourceTypes, ModelDBActionEnum.ModelDBServiceActions.READ, userInfo);
      } else {
        accessibleResourceIds =
                getSelfAllowedResources(modelDBServiceResourceTypes, ModelDBActionEnum.ModelDBServiceActions.READ);
      }
      resourceIdsSet.addAll(accessibleResourceIds);
      LOGGER.debug(
              "Accessible " + modelDBServiceResourceTypes + " Ids size is {}",
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
    Action action =
            Action.newBuilder()
                    .setService(Service.MODELDB_SERVICE)
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
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      LOGGER.info(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);
      Metadata requestHeaders = authServiceChannel.metadataInfo.get();
      GetAllowedResources.Response getAllowedResourcesResponse =
              authServiceChannel
                      .getAuthzServiceBlockingStub(requestHeaders)
                      .getAllowedResources(getAllowedResourcesRequest);
      LOGGER.info(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
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
                                              collaboratorBase), timeout);
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
    Action action =
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
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      LOGGER.info(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);
      Metadata requestHeaders = authServiceChannel.metadataInfo.get();
      GetSelfAllowedResources.Response getAllowedResourcesResponse =
              authServiceChannel
                      .getAuthzServiceBlockingStub(requestHeaders)
                      .getSelfAllowedResources(getAllowedResourcesRequest);
      LOGGER.info(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
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
                                              retry1, modelDBServiceResourceTypes, modelDBServiceActions), timeout);
    }
  }

  @Override
  public RoleBinding getRoleBindingByName(String roleBindingName) {
    return getRoleBindingByName(true, roleBindingName);
  }

  private RoleBinding getRoleBindingByName(boolean retry, String roleBindingName) {
    GetRoleBindingByName getRoleBindingByNameRequest =
            GetRoleBindingByName.newBuilder().setName(roleBindingName).build();
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      LOGGER.info(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);
      GetRoleBindingByName.Response getRoleBindingByNameResponse =
              authServiceChannel
                      .getRoleServiceBlockingStub()
                      .getRoleBindingByName(getRoleBindingByNameRequest);
      LOGGER.info(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(
              CommonMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getRoleBindingByNameResponse);

      return getRoleBindingByNameResponse.getRoleBinding();
    } catch (StatusRuntimeException ex) {
      LOGGER.info(roleBindingName + " : " + ex.getMessage());
      if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
        return (RoleBinding)
                CommonUtils.retryOrThrowException(
                        ex,
                        retry,
                        (CommonUtils.RetryCallInterface<RoleBinding>)
                                (retry1) -> getRoleBindingByName(retry1, roleBindingName), timeout);
      } else if (ex.getStatus().getCode().value() == Code.NOT_FOUND_VALUE) {
        return RoleBinding.newBuilder().build();
      }
      throw ex;
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
    Action action =
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
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      LOGGER.info(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);
      Metadata requestHeaders = authServiceChannel.metadataInfo.get();
      GetSelfAllowedResources.Response getAllowedResourcesResponse =
              authServiceChannel
                      .getAuthzServiceBlockingStub(requestHeaders)
                      .getSelfDirectlyAllowedResources(getAllowedResourcesRequest);
      LOGGER.info(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
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
                                              retry1, modelDBServiceResourceTypes, modelDBServiceActions), timeout);
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
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      LOGGER.info(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);
      Resources.Builder resourceBuilder =
              Resources.newBuilder()
                      .setService(Service.MODELDB_SERVICE)
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
                                      .setService(Service.MODELDB_SERVICE)
                                      .setModeldbServiceAction(modelDBServiceActions)
                                      .build())
                      .build();

      Metadata requestHeaders = authServiceChannel.metadataInfo.get();
      IsSelfAllowed.Response isSelfAllowedResponse =
              authServiceChannel
                      .getAuthzServiceBlockingStub(requestHeaders)
                      .isSelfAllowed(isSelfAllowedRequest);
      LOGGER.info(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
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
                      }, timeout);
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
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      LOGGER.info(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);
      GetSelfAllowedActionsBatch getSelfAllowedActionsBatch =
              GetSelfAllowedActionsBatch.newBuilder()
                      .setResources(
                              Resources.newBuilder()
                                      .setService(Service.MODELDB_SERVICE)
                                      .addAllResourceIds(resourceIds)
                                      .setResourceType(
                                              ResourceType.newBuilder().setModeldbServiceResourceType(type))
                                      .build())
                      .build();

      Metadata requestHeaders = authServiceChannel.metadataInfo.get();
      GetSelfAllowedActionsBatch.Response getSelfAllowedActionsBatchResponse =
              authServiceChannel
                      .getAuthzServiceBlockingStub(requestHeaders)
                      .getSelfAllowedActionsBatch(getSelfAllowedActionsBatch);
      LOGGER.info(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(
              CommonMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getSelfAllowedActionsBatchResponse);
      return getSelfAllowedActionsBatchResponse.getActionsMap();

    } catch (StatusRuntimeException ex) {
      return (Map<String, Actions>)
              CommonUtils.retryOrThrowException(
                      ex,
                      retry,
                      (CommonUtils.RetryCallInterface<Map<String, Actions>>)
                              (retry1) -> getSelfAllowedActionsBatch(retry1, resourceIds, type), timeout);
    }
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
  public String buildPublicRoleBindingName(
          String resourceId, ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    return buildRoleBindingName(
            "PUBLIC_ROLE", resourceId, "PUBLIC", modelDBServiceResourceTypes.name());
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

  private void setRoleBindingOnAuthService(boolean retry, RoleBinding roleBinding) {
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      LOGGER.info(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);
      SetRoleBinding.Response setRoleBindingResponse =
              authServiceChannel
                      .getRoleServiceBlockingStub()
                      .setRoleBinding(SetRoleBinding.newBuilder().setRoleBinding(roleBinding).build());
      LOGGER.info(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, setRoleBindingResponse);
    } catch (StatusRuntimeException ex) {
      CommonUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<Void>)
                      (retry1) -> {
                        setRoleBindingOnAuthService(retry1, roleBinding);
                        return null;
                      }, timeout);
    }
  }

  @Override
  public Role getRoleByName(String roleName, RoleScope roleScope) {
    return getRoleByName(true, roleName, roleScope);
  }

  private Role getRoleByName(boolean retry, String roleName, RoleScope roleScope) {
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      LOGGER.info(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);
      GetRoleByName.Builder getRoleByNameRequest = GetRoleByName.newBuilder().setName(roleName);
      if (roleScope != null) {
        getRoleByNameRequest.setScope(roleScope);
      }
      GetRoleByName.Response getRoleByNameResponse =
              authServiceChannel
                      .getRoleServiceBlockingStub()
                      .getRoleByName(getRoleByNameRequest.build());
      LOGGER.info(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getRoleByNameResponse);

      return getRoleByNameResponse.getRole();
    } catch (StatusRuntimeException ex) {
      return (Role)
              CommonUtils.retryOrThrowException(
                      ex,
                      retry,
                      (CommonUtils.RetryCallInterface<Role>) (retry1) -> getRoleByName(retry1, roleName, roleScope), timeout);
    }
  }

  @Override
  public boolean deleteRoleBinding(String roleBindingId) {
    return deleteRoleBinding(true, roleBindingId);
  }

  private boolean deleteRoleBinding(boolean retry, String roleBindingId) {
    DeleteRoleBinding deleteRoleBindingRequest =
            DeleteRoleBinding.newBuilder().setId(roleBindingId).build();
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      LOGGER.info(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);
      DeleteRoleBinding.Response deleteRoleBindingResponse =
              authServiceChannel
                      .getRoleServiceBlockingStub()
                      .deleteRoleBinding(deleteRoleBindingRequest);
      LOGGER.info(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, deleteRoleBindingResponse);

      return deleteRoleBindingResponse.getStatus();
    } catch (StatusRuntimeException ex) {
      return (Boolean)
              CommonUtils.retryOrThrowException(
                      ex,
                      retry,
                      (CommonUtils.RetryCallInterface<Boolean>) (retry1) -> deleteRoleBinding(retry1, roleBindingId), timeout);
    }
  }

  @Override
  public boolean deleteRoleBindings(List<String> roleBindingNames) {
    return deleteRoleBindings(true, roleBindingNames);
  }

  private boolean deleteRoleBindings(boolean retry, List<String> roleBindingNames) {
    DeleteRoleBindings deleteRoleBindingRequest =
            DeleteRoleBindings.newBuilder().addAllRoleBindingNames(roleBindingNames).build();
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      LOGGER.info(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);

      // TODO: try using futur stub than blocking stub
      DeleteRoleBindings.Response deleteRoleBindingResponse =
              authServiceChannel
                      .getRoleServiceBlockingStub()
                      .deleteRoleBindings(deleteRoleBindingRequest);
      LOGGER.info(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, deleteRoleBindingResponse);

      return deleteRoleBindingResponse.getStatus();
    } catch (StatusRuntimeException ex) {
      return (Boolean)
              CommonUtils.retryOrThrowException(
                      ex,
                      retry,
                      (CommonUtils.RetryCallInterface<Boolean>)
                              (retry1) -> deleteRoleBindings(retry1, roleBindingNames), timeout);
    }
  }

  @Override
  public GeneratedMessageV3 getTeamByName(String orgId, String teamName) {
    return getTeamByName(true, orgId, teamName);
  }

  private GeneratedMessageV3 getTeamByName(boolean retry, String orgId, String teamName) {
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      GetTeamByName getTeamByName =
              GetTeamByName.newBuilder().setTeamName(teamName).setOrgId(orgId).build();
      GetTeamByName.Response getTeamByNameResponse =
              authServiceChannel.getTeamServiceBlockingStub().getTeamByName(getTeamByName);
      return getTeamByNameResponse.getTeam();
    } catch (StatusRuntimeException ex) {
      return (GeneratedMessageV3)
              CommonUtils.retryOrThrowException(
                      ex,
                      retry,
                      (CommonUtils.RetryCallInterface<GeneratedMessageV3>)
                              (retry1) -> getTeamByName(retry1, orgId, teamName), timeout);
    }
  }

  @Override
  public GeneratedMessageV3 getOrgByName(String name) {
    return getOrgByName(true, name);
  }

  private GeneratedMessageV3 getOrgByName(boolean retry, String name) {
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      GetOrganizationByName getOrgByName =
              GetOrganizationByName.newBuilder().setOrgName(name).build();
      GetOrganizationByName.Response getOrgByNameResponse =
              authServiceChannel
                      .getOrganizationServiceBlockingStub()
                      .getOrganizationByName(getOrgByName);
      return getOrgByNameResponse.getOrganization();
    } catch (StatusRuntimeException ex) {
      return (GeneratedMessageV3)
              CommonUtils.retryOrThrowException(
                      ex,
                      retry,
                      (CommonUtils.RetryCallInterface<GeneratedMessageV3>) (retry1) -> getOrgByName(retry1, name), timeout);
    }
  }
}
