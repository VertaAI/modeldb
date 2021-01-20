package ai.verta.modeldb.common;

import ai.verta.modeldb.common.authservice.RoleService;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.ResourceVisibility;
import java.util.Map;
import java.util.Optional;

public interface ResourceEntity {
  String getOwner();

  String getStringId();

  Optional<Long> getWorkspaceId(Map<String, GetResourcesResponseItem> responseItemMap);

  String getOldWorkspaceId();

  Integer getWorkspaceType();

  String getName();

  ResourceVisibility getResourceVisibility();

  void setVisibilityMigration(boolean visibilityMigration);

  void deleteRoleBindings(RoleService roleService);
}
