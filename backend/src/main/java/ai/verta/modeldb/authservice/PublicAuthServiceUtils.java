package ai.verta.modeldb.authservice;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.uac.UserInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PublicAuthServiceUtils implements AuthService {

  public PublicAuthServiceUtils() {}

  @Override
  public UserInfo getCurrentLoginUserInfo() {
    return null;
  }

  @Override
  public UserInfo getUnsignedUser() {
    return null;
  }

  @Override
  public UserInfo getUserInfo(String vertaId, ModelDBConstants.UserIdentifier vertaIdentifier) {
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
      List<String> vertaIdList, List<String> emailIdList, List<String> usernameList) {
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
}
