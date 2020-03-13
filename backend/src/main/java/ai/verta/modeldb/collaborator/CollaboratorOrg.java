package ai.verta.modeldb.collaborator;

import ai.verta.modeldb.CollaboratorUserInfo;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.uac.AddCollaboratorRequest.Response.Builder;
import ai.verta.uac.Entities;
import ai.verta.uac.EntitiesEnum.EntitiesTypes;
import ai.verta.uac.Organization;
import com.google.protobuf.GeneratedMessageV3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CollaboratorOrg extends CollaboratorBase {
  private static final Logger LOGGER = LogManager.getLogger(CollaboratorOrg.class);

  public CollaboratorOrg(String orgId) {
    super(Organization.newBuilder().setId(orgId).build());
  }

  public CollaboratorOrg(GeneratedMessageV3 hostOrgInfo) {
    super(hostOrgInfo);
  }

  public CollaboratorOrg(String shareWithIdOrEmail, RoleService roleService) {
    super(getProtoMessage(shareWithIdOrEmail, roleService));
  }

  private static GeneratedMessageV3 getProtoMessage(String orgId, RoleService roleService) {
    GeneratedMessageV3 shareWith = roleService.getOrgById(orgId);
    LOGGER.trace("Shared with organization : {}", shareWith);
    return shareWith;
  }

  @Override
  public String getNameForBinding() {
    return "Org_" + getId();
  }

  @Override
  public EntitiesTypes getAuthzEntityType() {
    return EntitiesTypes.ORGANIZATION;
  }

  @Override
  public String getId() {
    return getCollaborator().getId();
  }

  @Override
  public void addToResponse(Builder response) {
    response.setCollaboratorOrganization(getCollaborator());
  }

  @Override
  public void addToResponse(CollaboratorUserInfo.Builder builder) {
    builder.setCollaboratorOrganization(getCollaborator());
    super.addToResponse(builder);
  }

  private Organization getCollaborator() {
    return (Organization) collaborator;
  }

  @Override
  public Entities getEntities() {
    return Entities.newBuilder().addOrgIds(getId()).build();
  }
}
