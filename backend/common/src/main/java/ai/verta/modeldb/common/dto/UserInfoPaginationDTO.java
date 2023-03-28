package ai.verta.modeldb.common.dto;

import ai.verta.uac.UserInfo;
import java.util.List;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class UserInfoPaginationDTO {
  private List<UserInfo> userInfoList;
  private Long totalRecords;
}
