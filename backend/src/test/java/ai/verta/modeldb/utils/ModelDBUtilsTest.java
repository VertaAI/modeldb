package ai.verta.modeldb.utils;

import ai.verta.common.CollaboratorTypeEnum.CollaboratorType;
import ai.verta.common.EntitiesEnum.EntitiesTypes;
import ai.verta.common.TernaryEnum.Ternary;
import ai.verta.modeldb.CollaboratorUserInfo;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.uac.GetCollaboratorResponseItem;
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
    GetCollaboratorResponseItem.Builder builder1 = GetCollaboratorResponseItem.newBuilder()
            .setAuthzEntityType(EntitiesTypes.USER)
            .setVertaId(USER_ID);
    builder1.getPermissionBuilder().setCollaboratorType(readWrite);
    GetCollaboratorResponseItem.Builder builder2 = GetCollaboratorResponseItem.newBuilder()
            .setAuthzEntityType(EntitiesTypes.ORGANIZATION)
            .setVertaId(ORG_ID);
    GetCollaboratorResponseItem.Builder builder3 = GetCollaboratorResponseItem.newBuilder()
            .setAuthzEntityType(EntitiesTypes.TEAM)
            .setVertaId(TEAM_ID);
    builder3.getPermissionBuilder().setCanDeploy(aTrue);
    List<GetCollaboratorResponseItem> collaboratorList =
        Arrays.asList(builder1.build(),
            builder2.build(),
            builder3.build());
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
