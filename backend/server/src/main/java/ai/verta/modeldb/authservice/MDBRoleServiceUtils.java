package ai.verta.modeldb.authservice;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.WorkspaceTypeEnum.WorkspaceType;
import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.authservice.RoleServiceUtils;
import ai.verta.modeldb.common.authservice.UACApisUtil;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.uac.*;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import com.google.common.base.Strings;
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
    } catch (Exception e) {
      throw new ModelDBException(e);
    }
  }

  /**
   * Given the workspace id and type, returns WorkspaceDTO which has the id, name and type for the
   * workspace.
   */
  @Override
  public WorkspaceDTO getWorkspaceDTOByWorkspaceIdForServiceUser(
      UserInfo currentLoginUserInfo, String workspaceId, Integer workspaceType) {
    var workspaceDTO = new WorkspaceDTO();
    workspaceDTO.setWorkspaceId(workspaceId);

    switch (workspaceType) {
      case WorkspaceType.ORGANIZATION_VALUE:
        var organization = (Organization) getOrgById(true, workspaceId, true);
        workspaceDTO.setWorkspaceType(WorkspaceType.ORGANIZATION);
        workspaceDTO.setWorkspaceName(organization.getName());
        return workspaceDTO;
      case WorkspaceType.USER_VALUE:
        workspaceDTO.setWorkspaceType(WorkspaceType.USER);
        if (workspaceId.equalsIgnoreCase(
            uacApisUtil.getVertaIdFromUserInfo(currentLoginUserInfo))) {
          workspaceDTO.setWorkspaceName(uacApisUtil.getUsernameFromUserInfo(currentLoginUserInfo));
        } else {
          try {
            var userInfo =
                uacApisUtil
                    .getUserInfo(workspaceId, CommonConstants.UserIdentifier.VERTA_ID)
                    .blockAndGet();
            workspaceDTO.setWorkspaceName(uacApisUtil.getUsernameFromUserInfo(userInfo));
          } catch (Exception e) {
            throw new ModelDBException(e);
          }
        }
        return workspaceDTO;
      default:
        return null;
    }
  }
}
