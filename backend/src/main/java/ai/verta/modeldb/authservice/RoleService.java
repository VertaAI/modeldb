package ai.verta.modeldb.authservice;

import ai.verta.modeldb.collaborator.CollaboratorBase;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.uac.Actions;
import ai.verta.uac.GetCollaboratorResponse;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ModelResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.uac.Organization;
import ai.verta.uac.Role;
import ai.verta.uac.RoleBinding;
import ai.verta.uac.RoleScope;
import ai.verta.uac.UserInfo;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ProtocolMessageEnum;
import io.grpc.Metadata;
import java.util.List;
import java.util.Map;

public interface RoleService {

  boolean IsImplemented();

  String buildRoleBindingName(
      String roleName, String resourceId, String userId, String resourceTypeName);

  String buildRoleBindingName(
      String roleName, String resourceId, CollaboratorBase collaborator, String resourceTypeName);

  void createRoleBinding(
      Role role,
      CollaboratorBase collaborator,
      String resourceId,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes);

  void isSelfAllowed(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      String resourceId);

  Map<String, Actions> getSelfAllowedActionsBatch(
      List<String> resourceIds, ModelDBServiceResourceTypes type);

  Role getRoleByName(String roleName, RoleScope roleScope);

  boolean deleteRoleBinding(String roleBindingId);

  List<GetCollaboratorResponse> getResourceCollaborators(
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

  String buildReadWriteRoleBindingName(
      String resourceId,
      CollaboratorBase collaborator,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes);

  String buildProjectDeployRoleBindingName(
      String resourceId,
      CollaboratorBase collaborator,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes);

  String buildAdminRoleBindingName(
      String resourceId,
      CollaboratorBase shareWithCollaborator,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes);

  void removeResourceRoleBindings(
      String resourceId,
      String resourceOwnerId,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes);

  RoleBinding getRoleBindingByName(String roleBindingName);

  List<String> getSelfAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions);

  List<String> getSelfDirectlyAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions);

  List<String> getAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      CollaboratorBase collaboratorBase);

  GeneratedMessageV3 getTeamById(String teamId);

  GeneratedMessageV3 getTeamByName(String orgId, String teamName);

  GeneratedMessageV3 getOrgById(String orgId);

  GeneratedMessageV3 getOrgByName(String name);

  List<String> getAccessibleResourceIds(
      CollaboratorBase hostUserInfo,
      CollaboratorBase currentLoginUserInfo,
      ProtocolMessageEnum resourceVisibility,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      List<String> requestedResourceIds);

  List<String> getAccessibleResourceIdsByActions(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      List<String> requestedIdList);

  /**
   * from the name for workspace, get the workspace id and type. if no workspace is present assume
   * user's personal workspace
   *
   * @param currentLoginUserInfo : current login userInfo
   * @param workspaceName : orgName or username
   * @return {@link WorkspaceDTO} : workspace dto
   */
  WorkspaceDTO getWorkspaceDTOByWorkspaceName(UserInfo currentLoginUserInfo, String workspaceName);

  List<Organization> listMyOrganizations();
}
