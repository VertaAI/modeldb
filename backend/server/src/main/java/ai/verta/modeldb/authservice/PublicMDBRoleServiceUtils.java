package ai.verta.modeldb.authservice;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.common.authservice.UACApisUtil;
import ai.verta.modeldb.common.collaborator.CollaboratorBase;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.metadata.MetadataDAORdbImpl;
import ai.verta.modeldb.project.FutureProjectDAO;
import ai.verta.modeldb.versioning.CommitDAORdbImpl;
import ai.verta.modeldb.versioning.RepositoryDAO;
import ai.verta.modeldb.versioning.RepositoryDAORdbImpl;
import ai.verta.uac.*;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.Metadata;
import java.util.*;

public class PublicMDBRoleServiceUtils implements MDBRoleService {

  private FutureProjectDAO futureProjectDAO;
  private RepositoryDAO repositoryDAO;
  private MetadataDAO metadataDAO;

  public PublicMDBRoleServiceUtils(UACApisUtil uacApisUtil, MDBConfig mdbConfig) {
    this.metadataDAO = new MetadataDAORdbImpl();
    var commitDAO = new CommitDAORdbImpl(uacApisUtil, this);
    this.repositoryDAO =
        new RepositoryDAORdbImpl(uacApisUtil, this, commitDAO, metadataDAO, mdbConfig);
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
      String roleName,
      CollaboratorBase collaborator,
      String resourceId,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    // Do nothing
  }

  @Override
  public void isSelfAllowed(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      String resourceId) {
    // Do nothing
  }

  @Override
  public Map<String, Actions> getSelfAllowedActionsBatch(
      List<String> resourceIds, ModelDBServiceResourceTypes type) {
    return new HashMap<>();
  }

  @Override
  public boolean deleteRoleBindingsUsingServiceUser(List<String> roleBindingNames) {
    return true;
  }

  @Override
  public List<GetCollaboratorResponseItem> getResourceCollaborators(
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
      ModelDBServiceActions modelDBServiceActions) {
    if (resourceId != null && !resourceId.isEmpty()) {
      if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.PROJECT)) {
        try {
          if (futureProjectDAO.getProjectById(resourceId).blockAndGet() != null) {
            throw new NotFoundException(ModelDBMessages.PROJECT_NOT_FOUND_FOR_ID);
          }
        } catch (Exception e) {
          throw new ModelDBException(e);
        }
      } else if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.DATASET)) {
        repositoryDAO.getDatasetById(metadataDAO, resourceId);
      }
    }
  }

  @Override
  public List<Resources> getSelfAllowedResources(
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
  public List<Resources> getAllowedResources(
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
  public Collection<String> getAccessibleResourceIds(
      CollaboratorBase hostUserInfo,
      CollaboratorBase currentLoginUserInfo,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      Collection<String> requestedResourceIds) {
    return requestedResourceIds;
  }

  @Override
  public Collection<String> getAccessibleResourceIdsByActions(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      List<String> requestedIdList) {
    return requestedIdList;
  }

  @Override
  public Workspace getWorkspaceByWorkspaceName(
      UserInfo currentLoginUserInfo, String workspaceName) {
    return Workspace.newBuilder().build();
  }

  @Override
  public List<Organization> listMyOrganizations() {
    return Collections.emptyList();
  }

  @Override
  public GetResourcesResponseItem getEntityResource(
      Optional<String> entityId,
      Optional<String> workspaceName,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    return GetResourcesResponseItem.newBuilder().setVisibility(ResourceVisibility.PRIVATE).build();
  }

  @Override
  public List<GetResourcesResponseItem> getEntityResourcesByName(
      Optional<String> entityName,
      Optional<String> workspaceName,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    return Collections.emptyList();
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
      ResourceVisibility resourceVisibility,
      boolean isServiceUser) {
    return false;
  }

  @Override
  public boolean deleteEntityResourcesWithServiceUser(
      List<String> entityIds, ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    return true;
  }

  @Override
  public List<GetResourcesResponseItem> getResourceItems(
      Workspace workspace,
      Set<String> resourceIds,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      boolean isServiceUser) {
    return Collections.emptyList();
  }
}
