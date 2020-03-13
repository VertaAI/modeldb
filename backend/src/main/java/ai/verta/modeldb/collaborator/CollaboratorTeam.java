package ai.verta.modeldb.collaborator;

import ai.verta.modeldb.CollaboratorUserInfo;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.uac.AddCollaboratorRequest.Response.Builder;
import ai.verta.uac.Entities;
import ai.verta.uac.EntitiesEnum.EntitiesTypes;
import ai.verta.uac.Team;
import com.google.protobuf.GeneratedMessageV3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CollaboratorTeam extends CollaboratorBase {
  private static final Logger LOGGER = LogManager.getLogger(CollaboratorTeam.class);

  public CollaboratorTeam(String teamId) {
    super(Team.newBuilder().setId(teamId).build());
  }

  public CollaboratorTeam(GeneratedMessageV3 hostTeamInfo) {
    super(hostTeamInfo);
  }

  public CollaboratorTeam(String shareWithIdOrEmail, RoleService roleService) {
    super(getProtoMessage(shareWithIdOrEmail, roleService));
  }

  private static GeneratedMessageV3 getProtoMessage(String teamId, RoleService roleService) {
    GeneratedMessageV3 shareWith = roleService.getTeamById(teamId);
    LOGGER.trace("Shared with team : {}", shareWith);
    return shareWith;
  }

  @Override
  public String getNameForBinding() {
    return "Team_" + getId();
  }

  @Override
  public String getId() {
    return getCollaborator().getId();
  }

  @Override
  public void addToResponse(Builder response) {
    response.setCollaboratorTeam(getCollaborator());
  }

  @Override
  public void addToResponse(CollaboratorUserInfo.Builder builder) {
    builder.setCollaboratorTeam(getCollaborator());
    super.addToResponse(builder);
  }

  private Team getCollaborator() {
    return (Team) collaborator;
  }

  @Override
  public EntitiesTypes getAuthzEntityType() {
    return EntitiesTypes.TEAM;
  }

  @Override
  public Entities getEntities() {
    return Entities.newBuilder().addTeamIds(getId()).build();
  }
}
