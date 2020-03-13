package ai.verta.modeldb.collaborator;

import ai.verta.modeldb.CollaboratorUserInfo;
import ai.verta.uac.AddCollaboratorRequest.Response.Builder;
import ai.verta.uac.Entities;
import ai.verta.uac.EntitiesEnum.EntitiesTypes;
import com.google.protobuf.GeneratedMessageV3;
import java.util.Objects;

public abstract class CollaboratorBase {
  GeneratedMessageV3 collaborator;

  public CollaboratorBase(GeneratedMessageV3 collaborator) {
    this.collaborator = collaborator;
  }

  public boolean isUser() {
    return false;
  }

  public String getVertaId() {
    return "";
  }

  public abstract String getNameForBinding();

  public abstract Entities getEntities();

  public abstract String getId();

  public abstract EntitiesTypes getAuthzEntityType();

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CollaboratorBase that = (CollaboratorBase) o;
    return Objects.equals(collaborator, that.collaborator);
  }

  @Override
  public int hashCode() {
    return Objects.hash(collaborator);
  }

  public abstract void addToResponse(Builder response);

  public void addToResponse(CollaboratorUserInfo.Builder builder) {
    builder.setEntityType(getAuthzEntityType());
  }

  public GeneratedMessageV3 getCollaboratorMessage() {
    return collaborator;
  }
}
