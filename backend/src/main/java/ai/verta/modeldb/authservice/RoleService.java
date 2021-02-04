package ai.verta.modeldb.authservice;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.WorkspaceTypeEnum.WorkspaceType;
import ai.verta.modeldb.common.collaborator.CollaboratorBase;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.uac.CollaboratorPermissions;
import ai.verta.uac.GetCollaboratorResponseItem;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.UserInfo;
import ai.verta.uac.Workspace;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Metadata;
import java.util.List;
import java.util.Optional;

public interface RoleService extends ai.verta.modeldb.common.authservice.RoleService {

  boolean IsImplemented();

  List<GetCollaboratorResponseItem> getResourceCollaborators(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      String resourceId,
      String resourceOwnerId,
      Metadata requestHeaders);

  /**
   * Checks permissions of the user wrt the Entity
   *
   * @param resourceId --> value of key like project.id, dataset.id etc.
   * @param modelDBServiceActions --> ModelDBServiceActions.UPDATE, ModelDBServiceActions.DELETE,
   *     ModelDBServiceActions.CREATE
   */
  void validateEntityUserWithUserInfo(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      String resourceId,
      ModelDBServiceActions modelDBServiceActions)
      throws InvalidProtocolBufferException;

  String buildReadOnlyRoleBindingName(
      String resourceId,
      CollaboratorBase collaborator,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes);

  GeneratedMessageV3 getTeamById(String teamId);

  GeneratedMessageV3 getOrgById(String orgId);

  /**
   * from the name for workspace, get the workspace id and type. if no workspace is present assume
   * user's personal workspace
   *
   * @param currentLoginUserInfo : current login userInfo
   * @param workspaceName : orgName or username
   * @return {@link WorkspaceDTO} : workspace dto
   */
  WorkspaceDTO getWorkspaceDTOByWorkspaceName(UserInfo currentLoginUserInfo, String workspaceName);

  /**
   * from the name for workspace, get the workspace id and type. if no workspace is present assume
   * user's personal workspace
   *
   * @param currentLoginUserInfo : current login userInfo
   * @param workspaceName : orgName or username
   * @return {@link Workspace} : workspace
   */
  Workspace getWorkspaceByWorkspaceName(UserInfo currentLoginUserInfo, String workspaceName);

  WorkspaceDTO getWorkspaceDTOByWorkspaceId(
      UserInfo currentLoginUserInfo, String workspaceId, Integer workspaceType);

  GetResourcesResponseItem getEntityResource(
      String entityId, ModelDBServiceResourceTypes modelDBServiceResourceTypes);

  boolean createWorkspacePermissions(
      String workspaceName,
      String resourceId,
      String resourceName,
      Optional<Long> ownerId,
      ModelDBServiceResourceTypes resourceType,
      CollaboratorPermissions permissions,
      ResourceVisibility visibility);

  boolean createWorkspacePermissions(
      Long workspaceId,
      Optional<WorkspaceType> workspaceType,
      String resourceId,
      String resourceName,
      Optional<Long> ownerId,
      ModelDBServiceResourceTypes resourceType,
      CollaboratorPermissions permissions,
      ResourceVisibility projectVisibility);

  void createWorkspacePermissions(
      String workspace_id,
      WorkspaceType forNumber,
      String valueOf,
      String roleRepositoryAdmin,
      ModelDBServiceResourceTypes repository,
      boolean orgScopedPublic,
      String globalSharing);

  boolean deleteAllResources(
      List<String> resourceIds, ModelDBServiceResourceTypes modelDBServiceResourceTypes);

  boolean checkConnectionsBasedOnPrivileges(
      ModelDBServiceResourceTypes serviceResourceTypes,
      ModelDBServiceActions serviceActions,
      String resourceId);
}
