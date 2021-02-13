package ai.verta.modeldb.reconcilers;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.entities.ExperimentEntity;
import ai.verta.modeldb.entities.ProjectEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public class SoftDeleteProjects extends DBSoftDeleter {

  private final RoleService roleService;

  public SoftDeleteProjects(ReconcilerConfig config, RoleService roleService) {
    super(config);
    this.roleService = roleService;
  }

  @Override
  protected String selfTableName() {
    return ProjectEntity.class.getSimpleName();
  }

  @Override
  protected Set<String> childrenTableName() {
    return Collections.singleton(ExperimentEntity.class.getSimpleName());
  }

  @Override
  protected void deleteExternal(Set<String> ids) {
    roleService.deleteEntityResourcesWithServiceUser(
        new ArrayList<>(ids), ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT);
  }
}
