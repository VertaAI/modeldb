package ai.verta.modeldb.common.authservice;

import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.dto.UserInfoPaginationDTO;
import ai.verta.uac.UserInfo;
import ai.verta.uac.Workspace;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AuthService {
  UserInfo getCurrentLoginUserInfo();

  UserInfo getUnsignedUser();

  UserInfo getUserInfo(String vertaId, CommonConstants.UserIdentifier vertaIdentifier);

  Map<String, UserInfo> getUserInfoFromAuthServer(
      Set<String> vertaIdList,
      Set<String> emailIdList,
      List<String> usernameList,
      boolean isServiceUser);

  String getVertaIdFromUserInfo(UserInfo userInfo);

  String getUsernameFromUserInfo(UserInfo userInfo);

  Long getWorkspaceIdFromUserInfo(UserInfo userInfo);

  /**
   * returns if the vertaID passed matches the current user
   *
   * @param vertaID : user id
   * @return {@link Boolean} : boolean status
   */
  boolean isCurrentUser(String vertaID);

  UserInfoPaginationDTO getFuzzyUserInfoList(String usernameChar);

  Workspace workspaceIdByName(boolean retry, String workspaceName);

  Workspace workspaceById(boolean retry, Long workspaceId);
}
