package ai.verta.modeldb.common.authservice;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.uac.*;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import com.google.protobuf.GeneratedMessageV3;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoleService {

  boolean createWorkspacePermissions(
      Optional<Long> workspaceId,
      Optional<String> workspaceName,
      String resourceId,
      String resourceName,
      Optional<Long> ownerId,
      ModelDBServiceResourceTypes resourceType,
      CollaboratorPermissions permissions,
      ResourceVisibility resourceVisibility,
      boolean isServiceUser);

  boolean deleteEntityResourcesWithServiceUser(
      List<String> entityIds, ModelDBServiceResourceTypes modelDBServiceResourceTypes);

  GetResourcesResponseItem getEntityResource(
      Optional<String> entityId,
      Optional<String> workspaceName,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes);

  default GetResourcesResponseItem getEntityResource(
      String entityId, ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    return getEntityResource(Optional.of(entityId), Optional.empty(), modelDBServiceResourceTypes);
  }

  GeneratedMessageV3 getOrgById(String orgId);

  List<GetResourcesResponseItem> getResourceItems(
      Workspace workspace,
      Set<String> resourceIds,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      boolean isServiceUser);

  Collection<String> getAccessibleResourceIds(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      Collection<String> requestedResourceIds);

  List<Resources> getSelfAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions);

  void isSelfAllowed(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBActionEnum.ModelDBServiceActions modelDBServiceActions,
      String resourceId);

  Collection<String> getAccessibleResourceIdsByActions(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      List<String> requestedResourceIds);

  List<Organization> listMyOrganizations();
}
