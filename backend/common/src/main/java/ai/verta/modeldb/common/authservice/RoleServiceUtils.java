package ai.verta.modeldb.common.authservice;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.collaborator.CollaboratorBase;
import ai.verta.modeldb.common.collaborator.CollaboratorUser;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.exceptions.PermissionDeniedException;
import ai.verta.uac.*;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ServiceEnum.Service;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.StatusRuntimeException;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RoleServiceUtils implements RoleService {
  private static final Logger LOGGER = LogManager.getLogger(RoleServiceUtils.class);
  protected final UAC uac;
  protected UACApisUtil uacApisUtil;
  private final Integer timeout;

  public RoleServiceUtils(UACApisUtil uacApisUtil, Integer timeout, UAC uac) {
    this.uacApisUtil = uacApisUtil;
    this.timeout = timeout;
    this.uac = uac;
  }

  public static boolean checkAllResourceAllowed(Collection<Resources> resources) {
    return !resources.isEmpty() && resources.stream().allMatch(Resources::getAllResourceIds);
  }

  public static Set<String> getResourceIds(Collection<Resources> resources) {
    return resources.stream()
        .flatMap(resources1 -> resources1.getResourceIdsList().stream())
        .collect(Collectors.toSet());
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
      ResourceVisibility resourceVisibility,
      boolean isServiceUser) {
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

      ownerId.ifPresent(setResourcesBuilder::setOwnerId);
      workspaceId.ifPresent(setResourcesBuilder::setWorkspaceId);
      workspaceName.ifPresent(setResourcesBuilder::setWorkspaceName);

      var blockingStub =
          isServiceUser
              ? authServiceChannel.getCollaboratorServiceBlockingStubForServiceUser()
              : authServiceChannel.getCollaboratorServiceBlockingStub();
      var setResourcesResponse = blockingStub.setResource(setResourcesBuilder.build());

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
        String errorMessage =
            String.format("Failed to locate %s resources in UAC for %s ID %s",
                modelDBServiceResourceTypes.name(),
                modelDBServiceResourceTypes.name(),
                entityId);
        throw new NotFoundException(errorMessage);
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
  public GeneratedMessageV3 getOrgById(String orgId) {
    return getOrgById(true, orgId, false);
  }

  protected GeneratedMessageV3 getOrgById(boolean retry, String orgId, boolean isServiceUser) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      GetOrganizationById getOrgById = GetOrganizationById.newBuilder().setOrgId(orgId).build();
      var blockingStub =
          isServiceUser
              ? authServiceChannel.getOrganizationServiceBlockingStubForServiceUser()
              : authServiceChannel.getOrganizationServiceBlockingStub();
      var getOrgByIdResponse = blockingStub.getOrganizationById(getOrgById);
      return getOrgByIdResponse.getOrganization();
    } catch (StatusRuntimeException ex) {
      return (GeneratedMessageV3)
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<GeneratedMessageV3>)
                  retry1 -> getOrgById(retry1, orgId, isServiceUser),
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
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      boolean isServiceUser) {
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
      var blockingStub =
          isServiceUser
              ? authServiceChannel.getCollaboratorServiceBlockingStubForServiceUser()
              : authServiceChannel.getCollaboratorServiceBlockingStub();
      final var response = blockingStub.getResources(builder.build());
      return response.getItemList();
    } catch (StatusRuntimeException ex) {
      LOGGER.trace(ex);
      throw ex;
    }
  }

  @Override
  public String buildRoleBindingName(
      String roleName, String resourceId, String userId, String resourceTypeName) {
    return buildRoleBindingName(
        roleName, resourceId, new CollaboratorUser(uacApisUtil, userId), resourceTypeName);
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
  public Collection<String> getAccessibleResourceIds(
      CollaboratorBase hostUserInfo,
      CollaboratorBase currentLoginUserInfo,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      Collection<String> requestedResourceIds) {
    List<Resources> accessibleResources;
    if (hostUserInfo != null) {
      accessibleResources =
          getReadOnlyAccessibleResourceIds(true, hostUserInfo, modelDBServiceResourceTypes);
    } else {
      accessibleResources =
          getReadOnlyAccessibleResourceIds(
              false, currentLoginUserInfo, modelDBServiceResourceTypes);
    }

    return getAccessibleResourceIdsFromAllowedResources(requestedResourceIds, accessibleResources);
  }

  public static Set<String> getAccessibleResourceIdsFromAllowedResources(
      Collection<String> requestedResourceIds, Collection<Resources> accessibleResources) {
    boolean allowedAllResources = checkAllResourceAllowed(accessibleResources);
    Set<String> accessibleResourceIds;
    if (allowedAllResources) {
      accessibleResourceIds = new HashSet<>(requestedResourceIds);
    } else {
      accessibleResourceIds = getResourceIds(accessibleResources);
      if (requestedResourceIds != null && !requestedResourceIds.isEmpty()) {
        accessibleResourceIds.retainAll(requestedResourceIds);
      }
    }
    return accessibleResourceIds;
  }

  private List<Resources> getReadOnlyAccessibleResourceIds(
      boolean isHostUser,
      CollaboratorBase userInfo,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {

    Set<Resources> resourceIdsSet = new HashSet<>();
    if (userInfo != null && userInfo.getVertaId() != null) {
      List<Resources> accessibleResourceIds;
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
  public List<Resources> getAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      CollaboratorBase collaboratorBase) {
    return getAllowedResources(
        true, modelDBServiceResourceTypes, modelDBServiceActions, collaboratorBase);
  }

  private List<Resources> getAllowedResources(
      boolean retry,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
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

      return getAllowedResourcesResponse.getResourcesList();
    } catch (StatusRuntimeException ex) {
      return (List<Resources>)
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<List<Resources>>)
                  retry1 ->
                      getAllowedResources(
                          retry1,
                          modelDBServiceResourceTypes,
                          modelDBServiceActions,
                          collaboratorBase),
              timeout);
    }
  }

  @Override
  public List<Resources> getSelfAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions) {
    return getSelfAllowedResources(true, modelDBServiceResourceTypes, modelDBServiceActions);
  }

  private List<Resources> getSelfAllowedResources(
      boolean retry,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions) {
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

      return getAllowedResourcesResponse.getResourcesList();
    } catch (StatusRuntimeException ex) {
      return (List<Resources>)
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<List<Resources>>)
                  retry1 ->
                      getSelfAllowedResources(
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
  public Collection<String> getAccessibleResourceIdsByActions(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      List<String> requestedResourceIds) {
    if (requestedResourceIds.size() == 1) {
      isSelfAllowed(
          modelDBServiceResourceTypes, modelDBServiceActions, requestedResourceIds.get(0));
      return requestedResourceIds;
    }
    List<Resources> accessibleResources =
        getSelfAllowedResources(modelDBServiceResourceTypes, modelDBServiceActions);

    // Validate if current user has access to the entity or not
    return getAccessibleResourceIdsFromAllowedResources(requestedResourceIds, accessibleResources);
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
              retry1 -> {
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
                  retry1 -> deleteRoleBindingsUsingServiceUser(retry1, roleBindingNames),
              timeout);
    }
  }

  private GeneratedMessageV3 getTeamByName(boolean retry, String orgId, String teamName) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      var getTeamByName = GetTeamByName.newBuilder().setTeamName(teamName).setOrgId(orgId).build();
      var getTeamByNameResponse =
          authServiceChannel.getTeamServiceBlockingStub().getTeamByName(getTeamByName);
      return getTeamByNameResponse.getTeam();
    } catch (StatusRuntimeException ex) {
      return (GeneratedMessageV3)
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<GeneratedMessageV3>)
                  retry1 -> getTeamByName(retry1, orgId, teamName),
              timeout);
    }
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
                  retry1 -> getOrgByName(retry1, name),
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
          CommonUtils.retryOrThrowException(ex, retry, this::listMyOrganizations, timeout);
    }
  }
}
