package ai.verta.modeldb.authservice;

import ai.verta.modeldb.collaborator.CollaboratorBase;
import ai.verta.modeldb.dataset.DatasetDAO;
import ai.verta.modeldb.dataset.DatasetDAORdbImpl;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.experiment.ExperimentDAO;
import ai.verta.modeldb.experiment.ExperimentDAORdbImpl;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.experimentRun.ExperimentRunDAORdbImpl;
import ai.verta.modeldb.project.ProjectDAO;
import ai.verta.modeldb.project.ProjectDAORdbImpl;
import ai.verta.modeldb.versioning.BlobDAORdbImpl;
import ai.verta.modeldb.versioning.CommitDAORdbImpl;
import ai.verta.modeldb.versioning.RepositoryDAORdbImpl;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PublicRoleServiceUtils implements RoleService {

  private ProjectDAO projectDAO;
  private DatasetDAO datasetDAO;

  public PublicRoleServiceUtils(AuthService authService) {
    ExperimentDAO experimentDAO = new ExperimentDAORdbImpl(authService);
    ExperimentRunDAO experimentRunDAO =
        new ExperimentRunDAORdbImpl(
            authService,
            new RepositoryDAORdbImpl(authService, this),
            new CommitDAORdbImpl(),
            new BlobDAORdbImpl());
    this.projectDAO = new ProjectDAORdbImpl(authService, this, experimentDAO, experimentRunDAO);
    this.datasetDAO = new DatasetDAORdbImpl(authService, this);
  }

  @Override
  public boolean IsImplemented() {
    return false;
  }

  @Override
  public String buildRoleBindingName(
      String roleName, String resourceId, String userId, String resourceTypeName) {
    return null;
  }

  @Override
  public String buildRoleBindingName(
      String roleName, String resourceId, CollaboratorBase collaborator, String resourceTypeName) {
    return null;
  }

  @Override
  public void createRoleBinding(
      Role role,
      CollaboratorBase collaborator,
      String resourceId,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {}

  @Override
  public void isSelfAllowed(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      String resourceId) {}

  @Override
  public Map<String, Actions> getSelfAllowedActionsBatch(
      List<String> resourceIds, ModelDBServiceResourceTypes type) {
    return new HashMap<>();
  }

  @Override
  public Role getRoleByName(String roleName, RoleScope roleScope) {
    return null;
  }

  @Override
  public boolean deleteRoleBinding(String roleBindingId) {
    return true;
  }

  @Override
  public List<GetCollaboratorResponse> getResourceCollaborators(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      String resourceId,
      String resourceOwnerId,
      Metadata requestHeaders) {
    return Collections.emptyList();
  }

  /**
   * Checks permissions of the user wrt the Entity
   *
   * @param modelDBServiceResourceTypes : modelDBServiceResourceTypes
   * @param resourceId --> value of key like project.id, dataset.id etc.
   * @param modelDBServiceActions --> ModelDBServiceActions.UPDATE, ModelDBServiceActions.DELETE,
   */
  @Override
  public void validateEntityUserWithUserInfo(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      String resourceId,
      ModelDBServiceActions modelDBServiceActions)
      throws InvalidProtocolBufferException {
    if (resourceId != null && !resourceId.isEmpty()) {
      if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.PROJECT)) {
        projectDAO.getProjectByID(resourceId);
      } else if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.DATASET)) {
        datasetDAO.getDatasetById(resourceId);
      }
    }
  }

  @Override
  public String buildReadOnlyRoleBindingName(
      String resourceId,
      CollaboratorBase collaborator,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    return null;
  }

  @Override
  public String buildReadWriteRoleBindingName(
      String resourceId,
      CollaboratorBase collaborator,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    return null;
  }

  @Override
  public String buildProjectDeployRoleBindingName(
      String resourceId,
      CollaboratorBase collaborator,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    return null;
  }

  @Override
  public void removeResourceRoleBindings(
      String resourceId,
      String resourceOwnerId,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {}

  @Override
  public RoleBinding getRoleBindingByName(String roleBindingName) {
    return null;
  }

  @Override
  public List<String> getSelfAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions) {
    return Collections.emptyList();
  }

  @Override
  public List<String> getSelfDirectlyAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions) {
    return Collections.emptyList();
  }

  @Override
  public List<String> getAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      CollaboratorBase collaboratorBase) {
    return Collections.emptyList();
  }

  @Override
  public GeneratedMessageV3 getTeamById(String teamId) {
    return null;
  }

  @Override
  public GeneratedMessageV3 getTeamByName(String orgId, String teamName) {
    return null;
  }

  @Override
  public GeneratedMessageV3 getOrgById(String orgId) {
    return null;
  }

  @Override
  public GeneratedMessageV3 getOrgByName(String name) {
    return null;
  }

  @Override
  public List<String> getAccessibleResourceIds(
      CollaboratorBase hostUserInfo,
      CollaboratorBase currentLoginUserInfo,
      ProtocolMessageEnum resourceVisibility,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      List<String> requestedResourceIds) {
    return requestedResourceIds;
  }

  @Override
  public List<String> getAccessibleResourceIdsByActions(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      List<String> requestedIdList) {
    return requestedIdList;
  }

  @Override
  public WorkspaceDTO getWorkspaceDTOByWorkspaceName(
      UserInfo currentLoginUserInfo, String workspaceName) {
    WorkspaceDTO workspaceDTO = new WorkspaceDTO();
    workspaceDTO.setWorkspaceName(workspaceName);
    return workspaceDTO;
  }

  @Override
  public List<Organization> listMyOrganizations() {
    return Collections.emptyList();
  }

  @Override
  public String buildAdminRoleBindingName(
      String resourceId,
      CollaboratorBase shareWithCollaborator,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    return null;
  }
}
