package ai.verta.modeldb.authservice;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.common.authservice.RoleService;
import ai.verta.modeldb.common.collaborator.CollaboratorBase;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.uac.GetCollaboratorResponseItem;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.UserInfo;
import ai.verta.uac.Workspace;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.Metadata;
import java.util.List;

public interface MDBRoleService extends RoleService {

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
      ModelDBServiceActions modelDBServiceActions);

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

  WorkspaceDTO getWorkspaceDTOByWorkspaceIdForServiceUser(
      UserInfo currentLoginUserInfo, String workspaceId, Integer workspaceType);

  boolean checkConnectionsBasedOnPrivileges(
      ModelDBServiceResourceTypes serviceResourceTypes,
      ModelDBServiceActions serviceActions,
      String resourceId);
}
