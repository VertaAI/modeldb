package ai.verta.modeldb.utils;

import ai.verta.common.CollaboratorTypeEnum.CollaboratorType;
import ai.verta.common.TernaryEnum.Ternary;
import ai.verta.modeldb.CollaboratorUserInfo;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.uac.EntitiesEnum.EntitiesTypes;
import ai.verta.uac.GetCollaboratorResponse;
import ai.verta.uac.Organization;
import ai.verta.uac.Team;
import ai.verta.uac.UserInfo;
import ai.verta.uac.VertaUserInfo;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ModelDBUtilsTest {

  private static final String USER_ID = "123";
  private static final String ORG_ID = "1234";
  private static final String TEAM_ID = "12";
  private static final String ORG_NAME = "org name";
  private static final String TEAM_NAME = "team_name";
  @Mock AuthService authService;
  @Mock RoleService roleService;

  @Test
  public void getHydratedCollaboratorUserInfo() {
    final CollaboratorType readWrite = CollaboratorType.READ_WRITE;

    final Ternary aTrue = Ternary.TRUE;
    List<GetCollaboratorResponse> collaboratorList =
        Arrays.asList(
            GetCollaboratorResponse.newBuilder()
                .setAuthzEntityType(EntitiesTypes.USER)
                .setVertaId(USER_ID)
                .setCollaboratorType(readWrite)
                .build(),
            GetCollaboratorResponse.newBuilder()
                .setAuthzEntityType(EntitiesTypes.ORGANIZATION)
                .setVertaId(ORG_ID)
                .build(),
            GetCollaboratorResponse.newBuilder()
                .setAuthzEntityType(EntitiesTypes.TEAM)
                .setCanDeploy(aTrue)
                .setVertaId(TEAM_ID)
                .build());
    Map<String, UserInfo> userInfoMap = new HashMap<>();
    final UserInfo userInfo =
        UserInfo.newBuilder()
            .setVertaInfo(VertaUserInfo.newBuilder().setUserId(USER_ID).build())
            .build();
    userInfoMap.put(USER_ID, userInfo);
    Organization org = Organization.newBuilder().setId(ORG_ID).setName(ORG_NAME).build();
    Mockito.when(roleService.getOrgById(ORG_ID)).thenReturn(org);
    Team team = Team.newBuilder().setId(TEAM_ID).setName(TEAM_NAME).build();
    Mockito.when(roleService.getTeamById(TEAM_ID)).thenReturn(team);
    List<CollaboratorUserInfo> info =
        ModelDBUtils.getHydratedCollaboratorUserInfo(
            authService, roleService, collaboratorList, userInfoMap);
    Assert.assertEquals(
        Arrays.asList(
            CollaboratorUserInfo.newBuilder()
                .setCollaboratorUserInfo(userInfo)
                .setCollaboratorType(readWrite)
                .setEntityType(EntitiesTypes.USER)
                .build(),
            CollaboratorUserInfo.newBuilder()
                .setCollaboratorOrganization(org)
                .setEntityType(EntitiesTypes.ORGANIZATION)
                .build(),
            CollaboratorUserInfo.newBuilder()
                .setCollaboratorTeam(team)
                .setCanDeploy(aTrue)
                .setEntityType(EntitiesTypes.TEAM)
                .build()),
        info);
  }
}
