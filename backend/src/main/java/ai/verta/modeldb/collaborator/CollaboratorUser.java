package ai.verta.modeldb.collaborator;

import ai.verta.modeldb.CollaboratorUserInfo;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.uac.AddCollaboratorRequest.Response.Builder;
import ai.verta.uac.Entities;
import ai.verta.uac.EntitiesEnum.EntitiesTypes;
import ai.verta.uac.UserInfo;
import ai.verta.uac.VertaUserInfo;
import com.google.protobuf.GeneratedMessageV3;

public class CollaboratorUser extends CollaboratorBase {
  private AuthService authService;

  public CollaboratorUser(AuthService authService, GeneratedMessageV3 shareWith) {
    super(shareWith);
    this.authService = authService;
  }

  public CollaboratorUser(AuthService authService, String vertaId) {
    super(
        UserInfo.newBuilder()
            .setVertaInfo(VertaUserInfo.newBuilder().setUserId(vertaId).build())
            .build());
    this.authService = authService;
  }

  @Override
  public boolean isUser() {
    return true;
  }

  @Override
  public String getVertaId() {
    return authService.getVertaIdFromUserInfo(getCollaborator());
  }

  @Override
  public String getNameForBinding() {
    return "User_" + getVertaId();
  }

  @Override
  public Entities getEntities() {
    return Entities.newBuilder().addUserIds(getVertaId()).build();
  }

  @Override
  public String getId() {
    return getVertaId();
  }

  @Override
  public EntitiesTypes getAuthzEntityType() {
    return EntitiesTypes.USER;
  }

  @Override
  public void addToResponse(Builder response) {
    response.setCollaboratorUserInfo(getCollaborator());
  }

  @Override
  public void addToResponse(CollaboratorUserInfo.Builder builder) {
    builder.setCollaboratorUserInfo(getCollaborator());
    super.addToResponse(builder);
  }

  private UserInfo getCollaborator() {
    return (UserInfo) collaborator;
  }
}
