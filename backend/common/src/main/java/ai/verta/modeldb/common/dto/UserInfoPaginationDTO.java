package ai.verta.modeldb.common.dto;

import ai.verta.uac.UserInfo;
import java.util.List;

public class UserInfoPaginationDTO {

  private List<UserInfo> userInfoList;
  private Long totalRecords;

  public List<UserInfo> getUserInfoList() {
    return userInfoList;
  }

  public void setUserInfoList(List<UserInfo> userInfoList) {
    this.userInfoList = userInfoList;
  }

  public Long getTotalRecords() {
    return totalRecords;
  }

  public void setTotalRecords(Long totalRecords) {
    this.totalRecords = totalRecords;
  }
}
