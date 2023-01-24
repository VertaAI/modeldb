package ai.verta.modeldb.authservice;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.common.authservice.RoleServiceUtils;
import ai.verta.modeldb.common.authservice.UACApisUtil;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.uac.*;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import com.google.common.base.Strings;
import io.grpc.StatusRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MDBRoleServiceUtils extends RoleServiceUtils implements MDBRoleService {
  private static final Logger LOGGER = LogManager.getLogger(MDBRoleServiceUtils.class);

  public static MDBRoleService FromConfig(MDBConfig config, UACApisUtil uacApisUtil, UAC uac) {
    if (!config.hasAuth()) return new PublicMDBRoleServiceUtils(uacApisUtil, config);
    else return new MDBRoleServiceUtils(config, uacApisUtil, uac);
  }

  private MDBRoleServiceUtils(MDBConfig config, UACApisUtil uacApisUtil, UAC uac) {
    super(uacApisUtil, config.getGrpcServer().getRequestTimeout(), uac);
  }

  @Override
  public boolean IsImplemented() {
    return true;
  }

  /**
   * Checks permissions of the user wrt the Entity
   *
   * @param resourceId --> value of key like project.id, dataset.id etc.
   * @param modelDBServiceActions --> ModelDBServiceActions.UPDATE, ModelDBServiceActions.DELETE,
   *     ModelDBServiceActions.CREATE
   */
  @Override
  public void validateEntityUserWithUserInfo(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      String resourceId,
      ModelDBServiceActions modelDBServiceActions) {
    // Check User Role
    isSelfAllowed(modelDBServiceResourceTypes, modelDBServiceActions, resourceId);
  }

  @Override
  public Workspace getWorkspaceByWorkspaceName(
      UserInfo currentLoginUserInfo, String workspaceName) {
    if (Strings.isNullOrEmpty(workspaceName)) {
      return Workspace.newBuilder().build();
    }
    try {
      return uacApisUtil.getWorkspaceByName(workspaceName).blockAndGet();
    } catch (StatusRuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ModelDBException(e);
    }
  }
}
