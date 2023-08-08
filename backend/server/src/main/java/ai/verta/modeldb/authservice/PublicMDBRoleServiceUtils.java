package ai.verta.modeldb.authservice;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.common.authservice.UACApisUtil;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.metadata.MetadataDAORdbImpl;
import ai.verta.modeldb.versioning.CommitDAORdbImpl;
import ai.verta.modeldb.versioning.RepositoryDAO;
import ai.verta.modeldb.versioning.RepositoryDAORdbImpl;
import ai.verta.uac.*;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import com.google.protobuf.GeneratedMessageV3;
import java.util.*;

public class PublicMDBRoleServiceUtils implements MDBRoleService {

  private final RepositoryDAO repositoryDAO;
  private final MetadataDAO metadataDAO;

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
  public void isSelfAllowed(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      String resourceId) {
    // Do nothing
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
      if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.DATASET)) {
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
  public GeneratedMessageV3 getOrgById(String orgId) {
    return null;
  }

  @Override
  public Collection<String> getAccessibleResourceIds(
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
