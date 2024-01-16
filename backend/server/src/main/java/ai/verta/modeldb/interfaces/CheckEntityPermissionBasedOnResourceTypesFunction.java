package ai.verta.modeldb.interfaces;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.utils.InternalFuture;
import ai.verta.uac.ModelDBActionEnum;
import java.util.List;

public interface CheckEntityPermissionBasedOnResourceTypesFunction {
  InternalFuture<Boolean> getEntityPermissionBasedOnResourceTypes(
      List<String> ids,
      ModelDBActionEnum.ModelDBServiceActions action,
      ModelDBResourceEnum.ModelDBServiceResourceTypes modelDBServiceResourceTypes);
}
