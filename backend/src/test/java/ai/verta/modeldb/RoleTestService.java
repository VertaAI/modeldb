package ai.verta.modeldb;

import ai.verta.uac.Action;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ModelResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.uac.ResourceActionGroup;
import ai.verta.uac.Resources;
import ai.verta.uac.Role;
import ai.verta.uac.ServiceEnum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RoleTestService {
  private static final Logger LOGGER = LogManager.getLogger(RoleTestService.class);

  private List<Action> createActions(List<ModelDBServiceActions> modelDBServiceActionsList) {
    List<Action> actionList = new ArrayList<>();
    for (ModelDBServiceActions modelDBServiceActions : modelDBServiceActionsList) {
      actionList.add(
          Action.newBuilder()
              .setService(ServiceEnum.Service.MODELDB_SERVICE)
              .setModeldbServiceAction(modelDBServiceActions)
              .build());
    }
    return actionList;
  }

  private Resources createResources(ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    return Resources.newBuilder()
        .setService(ServiceEnum.Service.MODELDB_SERVICE)
        .setModeldbServiceResourceType(modelDBServiceResourceTypes)
        .build();
  }

  private Role getRoleObj(
      String roleName,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      List<ModelDBServiceActions> modelDBServiceActionsList) {
    ResourceActionGroup resourceActionGroup =
        ResourceActionGroup.newBuilder()
            .addResources(createResources(modelDBServiceResourceTypes))
            .addAllActions(createActions(modelDBServiceActionsList))
            .build();
    return Role.newBuilder().setName(roleName).addResourceActionGroups(resourceActionGroup).build();
  }

  private List<ModelDBServiceActions> getModelDBServiceActionByRole(String roleName) {
    if (roleName.equals(ModelDBConstants.ROLE_PROJECT_CREATE)
        || roleName.equals(ModelDBConstants.ROLE_DATASET_CREATE)) {
      return Collections.singletonList(ModelDBServiceActions.CREATE);
    } else if (roleName.equals(ModelDBConstants.ROLE_PROJECT_OWNER)
        || roleName.equals(ModelDBConstants.ROLE_DATASET_OWNER)) {
      List<ModelDBServiceActions> modelDBActionList = new ArrayList<>();
      modelDBActionList.add(ModelDBServiceActions.ALL);
      modelDBActionList.add(ModelDBServiceActions.CREATE);
      modelDBActionList.add(ModelDBServiceActions.UPDATE);
      modelDBActionList.add(ModelDBServiceActions.READ);
      modelDBActionList.add(ModelDBServiceActions.DELETE);
      modelDBActionList.add(ModelDBServiceActions.DEPLOY);
      return modelDBActionList;
    } else if (roleName.equals(ModelDBConstants.ROLE_PROJECT_READ_ONLY)
        || roleName.equals(ModelDBConstants.ROLE_DATASET_READ_ONLY)) {
      return Collections.singletonList(ModelDBServiceActions.READ);
    } else if (roleName.equals(ModelDBConstants.ROLE_PROJECT_READ_WRITE)
        || roleName.equals(ModelDBConstants.ROLE_DATASET_READ_WRITE)) {
      List<ModelDBServiceActions> modelDBActionList = new ArrayList<>();
      modelDBActionList.add(ModelDBServiceActions.UPDATE);
      modelDBActionList.add(ModelDBServiceActions.READ);
      return modelDBActionList;
    } else if (roleName.equals(ModelDBConstants.ROLE_PROJECT_PUBLIC_READ)
        || roleName.equals(ModelDBConstants.ROLE_DATASET_PUBLIC_READ)) {
      return Collections.singletonList(ModelDBServiceActions.PUBLIC_READ);
    } else {
      return Collections.singletonList(ModelDBServiceActions.ALL);
    }
  }
}
