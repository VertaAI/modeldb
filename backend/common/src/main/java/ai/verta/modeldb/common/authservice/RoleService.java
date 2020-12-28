package ai.verta.modeldb.common.authservice;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.uac.CollaboratorPermissions;
import ai.verta.uac.GetCollaboratorResponseItem;
import ai.verta.uac.ResourceVisibility;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.Metadata;
import java.util.List;
import java.util.Optional;

public interface RoleService {

  boolean createWorkspacePermissions(
      Optional<Long> workspaceId,
      Optional<String> workspaceName,
      String resourceId,
      String resourceName,
      Optional<Long> ownerId,
      ModelDBServiceResourceTypes resourceType,
      CollaboratorPermissions permissions,
      ResourceVisibility resourceVisibility);

  boolean deleteEntityResources(
      List<String> entityIds, ModelDBServiceResourceTypes modelDBServiceResourceTypes);

  List<GetCollaboratorResponseItem> getResourceCollaborators(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      String resourceId,
      String resourceOwnerId,
      Metadata requestHeaders);

  GeneratedMessageV3 getOrgById(String orgId);

  GeneratedMessageV3 getTeamById(String teamId);
}
