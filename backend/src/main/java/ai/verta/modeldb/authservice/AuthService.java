package ai.verta.modeldb.authservice;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.uac.UserInfo;
import java.util.List;
import java.util.Map;

public interface AuthService {
  UserInfo getCurrentLoginUserInfo();

  UserInfo getUnsignedUser();

  UserInfo getUserInfo(String vertaId, ModelDBConstants.UserIdentifier vertaIdentifier);

  Map<String, UserInfo> getUserInfoFromAuthServer(
      List<String> vertaIdList, List<String> emailIdList, List<String> usernameList);

  String getVertaIdFromUserInfo(UserInfo userInfo);

  String getUsernameFromUserInfo(UserInfo userInfo);

  /**
   * returns if the vertaID passed matches the current user
   *
   * @param vertaID : user id
   * @return {@link Boolean} : boolean status
   */
  boolean isCurrentUser(String vertaID);
}
