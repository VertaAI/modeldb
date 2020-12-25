package ai.verta.modeldb.common.authservice;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.uac.CollaboratorPermissions;
import ai.verta.uac.ResourceVisibility;
import java.util.Optional;

public class PublicRoleServiceUtils implements RoleService {

  @Override
  public boolean createWorkspacePermissions(
      Optional<Long> workspaceId,
      Optional<String> workspaceName,
      String resourceId,
      String resourceName,
      Optional<Long> ownerId,
      ModelDBServiceResourceTypes resourceType,
      CollaboratorPermissions permissions,
      ResourceVisibility resourceVisibility) {
    return true;
  }
}
