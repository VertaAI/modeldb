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
import ai.verta.modeldb.common.authservice.AuthServiceChannel;
import ai.verta.modeldb.common.authservice.RoleServiceUtils;
import ai.verta.modeldb.common.collaborator.CollaboratorBase;
import ai.verta.modeldb.common.collaborator.CollaboratorOrg;
import ai.verta.modeldb.common.collaborator.CollaboratorTeam;
import ai.verta.modeldb.common.collaborator.CollaboratorUser;
import ai.verta.modeldb.common.config.Config;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.*;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import com.google.rpc.Code;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MDBRoleServiceUtils extends RoleServiceUtils implements MDBRoleService {
  private static final Logger LOGGER = LogManager.getLogger(MDBRoleServiceUtils.class);

  public static MDBRoleService FromConfig(Config config, AuthService authService, UAC uac) {
    if (!config.hasAuth()) return new PublicMDBRoleServiceUtils(authService);
    else return new MDBRoleServiceUtils(config, authService, uac);
  }

  private MDBRoleServiceUtils(Config config, AuthService authService, UAC uac) {
    super(authService, config.getGrpcServer().getRequestTimeout(), uac);
  }

  @Override
  public boolean IsImplemented() {
    return true;
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
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
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
    var getAllowedEntitiesRequest =
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
    LOGGER.trace(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);
    var getAllowedEntitiesResponse =
        authServiceChannel
            .getAuthzServiceBlockingStub(requestHeaders)
            .getAllowedEntities(getAllowedEntitiesRequest);
    LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
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
                    var collaboratorTeam = new CollaboratorTeam(teamId);
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
                    var collaboratorOrg = new CollaboratorOrg(orgId);
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
    try {
      collaboratorCombineFuture.get();

      Set<CollaboratorBase> deployCollaborators = deployCollaboratorsFuture.get();
      Set<CollaboratorBase> readOnlyCollaborators = readOnlyCollaboratorsFuture.get();
      Set<CollaboratorBase> readWriteCollaborators = readWriteCollaboratorsFuture.get();

      readOnlyCollaborators.removeAll(readWriteCollaborators);
      if (readOnlyCollaborators.size() > 0) {
        LOGGER.debug("ReadOnly Collaborators count: " + readOnlyCollaborators.size());
        for (CollaboratorBase collaborator : readOnlyCollaborators) {
          var getCollaboratorResponseBuilder =
              GetCollaboratorResponseItem.newBuilder()
                  .setAuthzEntityType(collaborator.getAuthzEntityType())
                  .setVertaId(collaborator.getId());
          var collPermBuilder = CollaboratorPermissions.newBuilder();
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
          var getCollaboratorResponseBuilder =
              GetCollaboratorResponseItem.newBuilder()
                  .setAuthzEntityType(collaborator.getAuthzEntityType())
                  .setVertaId(collaborator.getId());
          var collPermBuilder = CollaboratorPermissions.newBuilder();
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
    } catch (ExecutionException ex) {
      throw new ModelDBException(ex);
    } catch (InterruptedException ex) {
      // Restore interrupted state...
      Thread.currentThread().interrupt();
      throw new ModelDBException(ex);
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
      return CommonConstants.EMPTY_STRING;
    }
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
  public WorkspaceDTO getWorkspaceDTOByWorkspaceIdForServiceUser(
      UserInfo currentLoginUserInfo, String workspaceId, Integer workspaceType) {
    var workspaceDTO = new WorkspaceDTO();
    workspaceDTO.setWorkspaceId(workspaceId);

    switch (workspaceType) {
      case WorkspaceType.ORGANIZATION_VALUE:
        var organization = (Organization) getOrgById(true, workspaceId, true);
        workspaceDTO.setWorkspaceType(WorkspaceType.ORGANIZATION);
        workspaceDTO.setWorkspaceName(organization.getName());
        return workspaceDTO;
      case WorkspaceType.USER_VALUE:
        workspaceDTO.setWorkspaceType(WorkspaceType.USER);
        if (workspaceId.equalsIgnoreCase(
            authService.getVertaIdFromUserInfo(currentLoginUserInfo))) {
          workspaceDTO.setWorkspaceName(authService.getUsernameFromUserInfo(currentLoginUserInfo));
        } else {
          var userInfo =
              authService.getUserInfo(workspaceId, CommonConstants.UserIdentifier.VERTA_ID);
          workspaceDTO.setWorkspaceName(authService.getUsernameFromUserInfo(userInfo));
        }
        return workspaceDTO;
      default:
        return null;
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
