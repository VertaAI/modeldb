package ai.verta.modeldb.authservice;

import ai.verta.common.CollaboratorTypeEnum;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.TernaryEnum;
import ai.verta.common.WorkspaceTypeEnum.WorkspaceType;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.CommonUtils.RetryCallInterface;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.collaborator.CollaboratorBase;
import ai.verta.modeldb.common.collaborator.CollaboratorOrg;
import ai.verta.modeldb.common.collaborator.CollaboratorTeam;
import ai.verta.modeldb.common.collaborator.CollaboratorUser;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.*;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import com.google.protobuf.GeneratedMessageV3;
import com.google.rpc.Code;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RoleServiceUtils extends ai.verta.modeldb.common.authservice.RoleServiceUtils
    implements RoleService {
  private static final Logger LOGGER = LogManager.getLogger(RoleServiceUtils.class);

  public static ai.verta.modeldb.authservice.RoleService FromConfig(
      Config config, AuthService authService) {
    if (!config.hasAuth()) return new PublicRoleServiceUtils(authService);
    else return new RoleServiceUtils(authService);
  }

  public RoleServiceUtils(AuthService authService) {
    this(Config.getInstance(), authService);
  }

  private RoleServiceUtils(Config config, AuthService authService) {
    super(
        authService,
        config.authService.host,
        config.authService.port,
        config.mdb_service_user.email,
        config.mdb_service_user.devKey,
        config.grpcServer.requestTimeout,
        AuthInterceptor.METADATA_INFO);
  }

  @Override
  public boolean IsImplemented() {
    return true;
  }

  @Override
  public void createRoleBinding(
      Role role,
      CollaboratorBase collaborator,
      String resourceId,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    super.createRoleBinding(role, collaborator, resourceId, modelDBServiceResourceTypes);
  }

  @Override
  public void createPublicRoleBinding(
      String resourceId, ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    super.createPublicRoleBinding(resourceId, modelDBServiceResourceTypes);
  }

  @Override
  public String buildPublicRoleBindingName(
      String resourceId, ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    return super.buildPublicRoleBindingName(resourceId, modelDBServiceResourceTypes);
  }

  @Override
  public void isSelfAllowed(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      String resourceId) {
    super.isSelfAllowed(modelDBServiceResourceTypes, modelDBServiceActions, resourceId);
  }

  @Override
  public Map<String, Actions> getSelfAllowedActionsBatch(
      List<String> resourceIds, ModelDBServiceResourceTypes type) {
    return super.getSelfAllowedActionsBatch(resourceIds, type);
  }

  @Override
  public Role getRoleByName(String roleName, RoleScope roleScope) {
    return super.getRoleByName(roleName, roleScope);
  }

  @Override
  public boolean deleteRoleBinding(String roleBindingId) {
    return super.deleteRoleBinding(roleBindingId);
  }

  @Override
  public boolean deleteRoleBindings(List<String> roleBindingNames) {
    return super.deleteRoleBindings(roleBindingNames);
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
              (CommonUtils.RetryCallInterface<List<GetCollaboratorResponseItem>>)
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
    LOGGER.info(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);
    GetAllowedEntities.Response getAllowedEntitiesResponse =
        authServiceChannel
            .getAuthzServiceBlockingStub(requestHeaders)
            .getAllowedEntities(getAllowedEntitiesRequest);
    LOGGER.info(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
    LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getAllowedEntitiesResponse);

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
          collPermBuilder.setCollaboratorType(CollaboratorTypeEnum.CollaboratorType.READ_ONLY);
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
          collPermBuilder.setCollaboratorType(CollaboratorTypeEnum.CollaboratorType.READ_WRITE);
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
      throw new InternalErrorException(ex.getMessage());
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
    return super.getRoleBindingByName(roleBindingName);
  }

  @Override
  public List<String> getSelfAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions) {
    return super.getSelfAllowedResources(modelDBServiceResourceTypes, modelDBServiceActions);
  }

  @Override
  public List<String> getSelfDirectlyAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions) {
    return super.getSelfDirectlyAllowedResources(
        modelDBServiceResourceTypes, modelDBServiceActions);
  }

  @Override
  public List<String> getAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      CollaboratorBase collaboratorBase) {
    return super.getAllowedResources(
        modelDBServiceResourceTypes, modelDBServiceActions, collaboratorBase);
  }

  @Override
  public GeneratedMessageV3 getTeamByName(String orgId, String teamName) {
    return super.getTeamByName(orgId, teamName);
  }

  @Override
  public GeneratedMessageV3 getOrgByName(String name) {
    return super.getOrgByName(name);
  }

  @Override
  public List<String> getAccessibleResourceIds(
      CollaboratorBase hostUserInfo,
      CollaboratorBase currentLoginUserInfo,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      List<String> requestedResourceIds) {
    return super.getAccessibleResourceIds(
        hostUserInfo, currentLoginUserInfo, modelDBServiceResourceTypes, requestedResourceIds);
  }

  @Override
  public List<String> getAccessibleResourceIdsByActions(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      List<String> requestedIdList) {
    return super.getAccessibleResourceIdsByActions(
        modelDBServiceResourceTypes, modelDBServiceActions, requestedIdList);
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
      ModelDBUtils.retryOrThrowException(ex, false, (RetryCallInterface<Void>) (retry1) -> null);
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
                authService.getUserInfo(workspaceName, CommonConstants.UserIdentifier.USER_NAME));
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
              authService.getUserInfo(workspaceId, CommonConstants.UserIdentifier.VERTA_ID);
          workspaceDTO.setWorkspaceName(authService.getUsernameFromUserInfo(userInfo));
        }
        return workspaceDTO;
      default:
        return null;
    }
  }

  @Override
  public List<Organization> listMyOrganizations() {
    return super.listMyOrganizations();
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
    return super.getResourceItems(workspace, resourceIds, modelDBServiceResourceTypes);
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
      LOGGER.info(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);

      RemoveResources.Response removeAllCollaboratorResponse =
          authServiceChannel
              .getRoleServiceBlockingStub()
              .removeResources(removeAllCollaboratorsRequest);
      LOGGER.info(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(
          CommonMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, removeAllCollaboratorResponse);

      return removeAllCollaboratorResponse.getStatus();
    } catch (StatusRuntimeException ex) {
      return (Boolean)
          ModelDBUtils.retryOrThrowException(
              ex,
              retry,
              (RetryCallInterface<Boolean>)
                  (retry1) -> deleteAllResources(retry1, resourceIds, modelDBServiceResourceTypes));
    }
  }

  public boolean checkConnectionsBasedOnPrivileges(
      ModelDBServiceResourceTypes serviceResourceTypes,
      ModelDBServiceActions serviceActions,
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
