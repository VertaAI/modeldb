package ai.verta.modeldb.authservice;

import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.dto.UserInfoPaginationDTO;
import ai.verta.uac.UserInfo;
import ai.verta.uac.Workspace;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PublicAuthServiceUtils implements AuthService {

  @Override
  public UserInfo getCurrentLoginUserInfo() {
    return null;
  }

  @Override
  public UserInfo getUnsignedUser() {
    return null;
  }

  @Override
  public UserInfo getUserInfo(String vertaId, CommonConstants.UserIdentifier vertaIdentifier) {
    return null;
  }

  /**
   * @param vertaIdList : vertaId list which is now deprecated
   * @param emailIdList : email id list
   * @param usernameList : username list
   * @return Map from verta_id to userInfo
   */
  @Override
  public Map<String, UserInfo> getUserInfoFromAuthServer(
      Set<String> vertaIdList,
      Set<String> emailIdList,
      List<String> usernameList,
      boolean isServiceUser) {
    return new HashMap<>();
  }

  @Override
  public String getVertaIdFromUserInfo(UserInfo userInfo) {
    return "";
  }

  @Override
  public String getUsernameFromUserInfo(UserInfo userInfo) {
    return "";
  }

  @Override
  public Long getWorkspaceIdFromUserInfo(UserInfo userInfo) {
    return 0L;
  }

  @Override
  public boolean isCurrentUser(String vertaID) {
    return true;
  }

  @Override
  public UserInfoPaginationDTO getFuzzyUserInfoList(String username_char) {
    var paginationDTO = new UserInfoPaginationDTO();
    paginationDTO.setUserInfoList(Collections.emptyList());
    paginationDTO.setTotalRecords(0L);
    return paginationDTO;
  }

  @Override
  public Workspace workspaceIdByName(boolean retry, String workspaceName) {
    return Workspace.newBuilder().build();
  }

  @Override
  public Workspace workspaceById(boolean retry, Long workspaceId) {
    return Workspace.newBuilder().build();
  }
}
