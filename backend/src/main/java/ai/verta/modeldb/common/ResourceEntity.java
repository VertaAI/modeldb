package ai.verta.modeldb.common;

import ai.verta.uac.ResourceVisibility;
import java.util.Optional;

public interface ResourceEntity {
  String getOwner();

  String getId();

  Optional<Long> getWorkspaceId();

  Optional<String> getWorkspaceName();

  String getName();

  ResourceVisibility getResourceVisibility();

  void setVisibility_migration(boolean visibilityMigration);

  void deleteRoleBindings();
}
