package ai.verta.modeldb.common.collaborator;

import ai.verta.common.EntitiesEnum.EntitiesTypes;
import ai.verta.modeldb.common.authservice.UACApisUtil;
import ai.verta.uac.AddCollaboratorRequest.Response.Builder;
import ai.verta.uac.Entities;
import ai.verta.uac.UserInfo;
import ai.verta.uac.VertaUserInfo;
import com.google.protobuf.GeneratedMessageV3;
import java.util.Objects;

public class CollaboratorUser extends CollaboratorBase {
  private UACApisUtil uacApisUtil;

  public CollaboratorUser(UACApisUtil uacApisUtil, GeneratedMessageV3 shareWith) {
    super(shareWith);
    this.uacApisUtil = uacApisUtil;
  }

  public CollaboratorUser(UACApisUtil uacApisUtil, String vertaId) {
    super(
        UserInfo.newBuilder()
            .setVertaInfo(VertaUserInfo.newBuilder().setUserId(vertaId).build())
            .build());
    this.uacApisUtil = uacApisUtil;
  }

  @Override
  public boolean isUser() {
    return true;
  }

  @Override
  public String getVertaId() {
    return uacApisUtil.getVertaIdFromUserInfo(getCollaborator());
  }

  @Override
  public String getNameForBinding() {
    return "User_" + getVertaId();
  }

  @Override
  public Entities getEntities() {
    return Entities.newBuilder().addUserIds(getVertaId()).build();
  }

  /** Returns the vertaId for user */
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

  private UserInfo getCollaborator() {
    return (UserInfo) collaborator;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CollaboratorUser)) return false;
    if (!super.equals(o)) return false;
    CollaboratorUser that = (CollaboratorUser) o;
    return Objects.equals(uacApisUtil, that.uacApisUtil);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), uacApisUtil);
  }
}
