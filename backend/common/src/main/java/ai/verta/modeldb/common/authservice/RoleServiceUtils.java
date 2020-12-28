package ai.verta.modeldb.common.authservice;

import ai.verta.common.CollaboratorTypeEnum;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.TernaryEnum;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.collaborator.CollaboratorBase;
import ai.verta.modeldb.common.collaborator.CollaboratorOrg;
import ai.verta.modeldb.common.collaborator.CollaboratorTeam;
import ai.verta.modeldb.common.collaborator.CollaboratorUser;
import ai.verta.uac.Action;
import ai.verta.uac.CollaboratorPermissions;
import ai.verta.uac.DeleteResources;
import ai.verta.uac.Entities;
import ai.verta.uac.GetAllowedEntities;
import ai.verta.uac.GetCollaboratorResponseItem;
import ai.verta.uac.GetOrganizationById;
import ai.verta.uac.GetTeamById;
import ai.verta.uac.ModelDBActionEnum;
import ai.verta.uac.ResourceType;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.Resources;
import ai.verta.uac.ServiceEnum.Service;
import ai.verta.uac.SetResource;
import com.google.protobuf.GeneratedMessageV3;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RoleServiceUtils implements RoleService {
  private static final Logger LOGGER = LogManager.getLogger(RoleServiceUtils.class);
  protected AuthService authService;
  private final String host;
  private final Integer port;
  private final String serviceUserEmail;
  private final String serviceUserDevKey;
  private Integer timeout;

  public RoleServiceUtils(
      AuthService authService,
      String host,
      Integer port,
      String serviceUserEmail,
      String serviceUserDevKey,
      Integer timeout) {
    this.authService = authService;
    this.host = host;
    this.port = port;
    this.serviceUserEmail = serviceUserEmail;
    this.serviceUserDevKey = serviceUserDevKey;
    this.timeout = timeout;
  }

  @Override
  public boolean createWorkspacePermissions(
      Optional<Long> workspaceId,
      Optional<String> workspaceName,
      String resourceId,
      String resourceName,
      Optional<Long> ownerId,
      ModelDBServiceResourceTypes resourceType,
      CollaboratorPermissions permissions,
      ResourceVisibility resourceVisibility) {
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      LOGGER.info("Calling CollaboratorService to create resources");
      ResourceType modeldbServiceResourceType =
          ResourceType.newBuilder().setModeldbServiceResourceType(resourceType).build();
      SetResource.Builder setResourcesBuilder =
          SetResource.newBuilder()
              .setService(Service.MODELDB_SERVICE)
              .setResourceType(modeldbServiceResourceType)
              .setResourceId(resourceId)
              .setResourceName(resourceName)
              .setVisibility(resourceVisibility);

      if (resourceVisibility.equals(ResourceVisibility.ORG_CUSTOM)) {
        setResourcesBuilder.setCollaboratorType(permissions.getCollaboratorType());
        setResourcesBuilder.setCanDeploy(permissions.getCanDeploy());
      }

      if (ownerId.isPresent()) {
        setResourcesBuilder.setOwnerId(ownerId.get());
      }
      if (workspaceId.isPresent()) {
        setResourcesBuilder.setWorkspaceId(workspaceId.get());
      } else if (workspaceName.isPresent()) {
        setResourcesBuilder = setResourcesBuilder.setWorkspaceName(workspaceName.get());
      } else {
        throw new IllegalArgumentException(
            "workspaceId and workspaceName are both empty.  One must be provided.");
      }
      SetResource.Response setResourcesResponse =
          authServiceChannel
              .getCollaboratorServiceBlockingStub()
              .setResource(setResourcesBuilder.build());

      LOGGER.info("SetResources message sent.  Response: " + setResourcesResponse);
      return true;
    } catch (StatusRuntimeException ex) {
      LOGGER.error(ex);
      CommonUtils.retryOrThrowException(ex, false, retry -> null, timeout);
    }
    return false;
  }

  private AuthServiceChannel getAuthServiceChannel() {
    return new AuthServiceChannel(host, port, serviceUserEmail, serviceUserDevKey);
  }

  public boolean deleteResources(Resources resources) {
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      LOGGER.info("Calling CollaboratorService to delete resources");
      DeleteResources deleteResources =
          DeleteResources.newBuilder().setResources(resources).build();
      DeleteResources.Response response =
          authServiceChannel.getCollaboratorServiceBlockingStub().deleteResources(deleteResources);
      LOGGER.info("DeleteResources message sent.  Response: " + response);
      return true;
    } catch (StatusRuntimeException ex) {
      LOGGER.error(ex);
      CommonUtils.retryOrThrowException(ex, false, retry -> null, timeout);
    }
    return false;
  }

  @Override
  public boolean deleteEntityResources(
      List<String> entityIds, ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    ResourceType modeldbServiceResourceType =
        ResourceType.newBuilder()
            .setModeldbServiceResourceType(modelDBServiceResourceTypes)
            .build();
    Resources resources =
        Resources.newBuilder()
            .setResourceType(modeldbServiceResourceType)
            .setService(Service.MODELDB_SERVICE)
            .addAllResourceIds(entityIds)
            .build();
    return deleteResources(resources);
  }

  @Override
  public List<GetCollaboratorResponseItem> getResourceCollaborators(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      String resourceId,
      String resourceOwnerId,
      Metadata requestHeaders) {
    return getResourceCollaborators(
        true, modelDBServiceResourceTypes, resourceId, resourceOwnerId, requestHeaders);
  }

  private List<GetCollaboratorResponseItem> getResourceCollaborators(
      boolean retry,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      String resourceId,
      String resourceOwnerId,
      Metadata requestHeaders) {
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      LOGGER.debug("getting Resource collaborator with authChannel {}", authServiceChannel);
      return getCollaborators(
          authServiceChannel,
          resourceOwnerId,
          resourceId,
          modelDBServiceResourceTypes,
          requestHeaders);
    } catch (StatusRuntimeException ex) {
      return (List<GetCollaboratorResponseItem>)
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<List<GetCollaboratorResponseItem>>)
                  (retry1) ->
                      getResourceCollaborators(
                          retry1,
                          modelDBServiceResourceTypes,
                          resourceId,
                          resourceOwnerId,
                          requestHeaders),
              timeout);
    }
  }

  private List<GetCollaboratorResponseItem> getCollaborators(
      AuthServiceChannel authServiceChannel,
      String resourceOwnerId,
      String resourceId,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      Metadata requestHeaders) {
    List<GetCollaboratorResponseItem> getCollaboratorResponseList = new ArrayList<>();

    try {
      // Run a task specified by a Supplier object asynchronously
      CompletableFuture<Set<CollaboratorBase>> deployCollaboratorsFuture =
          CompletableFuture.supplyAsync(
              () ->
                  getCollaborators(
                      authServiceChannel,
                      resourceOwnerId,
                      resourceId,
                      modelDBServiceResourceTypes,
                      ModelDBActionEnum.ModelDBServiceActions.DEPLOY,
                      requestHeaders));

      CompletableFuture<Set<CollaboratorBase>> readOnlyCollaboratorsFuture =
          CompletableFuture.supplyAsync(
              () ->
                  getCollaborators(
                      authServiceChannel,
                      resourceOwnerId,
                      resourceId,
                      modelDBServiceResourceTypes,
                      ModelDBActionEnum.ModelDBServiceActions.READ,
                      requestHeaders));

      CompletableFuture<Set<CollaboratorBase>> readWriteCollaboratorsFuture =
          CompletableFuture.supplyAsync(
              () ->
                  getCollaborators(
                      authServiceChannel,
                      resourceOwnerId,
                      resourceId,
                      modelDBServiceResourceTypes,
                      ModelDBActionEnum.ModelDBServiceActions.UPDATE,
                      requestHeaders));

      CompletableFuture<Void> collaboratorCombineFuture =
          CompletableFuture.allOf(
              deployCollaboratorsFuture, readOnlyCollaboratorsFuture, readWriteCollaboratorsFuture);

      // Wait for all task complete
      collaboratorCombineFuture.get();

      Set<CollaboratorBase> deployCollaborators = deployCollaboratorsFuture.get();
      Set<CollaboratorBase> readOnlyCollaborators = readOnlyCollaboratorsFuture.get();
      Set<CollaboratorBase> readWriteCollaborators = readWriteCollaboratorsFuture.get();

      readOnlyCollaborators.removeAll(readWriteCollaborators);
      if (readOnlyCollaborators.size() > 0) {
        LOGGER.debug("ReadOnly Collaborators count: " + readOnlyCollaborators.size());
        for (CollaboratorBase collaborator : readOnlyCollaborators) {
          GetCollaboratorResponseItem.Builder getCollaboratorResponseBuilder =
              GetCollaboratorResponseItem.newBuilder()
                  .setAuthzEntityType(collaborator.getAuthzEntityType())
                  .setVertaId(collaborator.getId());
          CollaboratorPermissions.Builder collPermBuilder = CollaboratorPermissions.newBuilder();
          collPermBuilder.setCollaboratorType(CollaboratorTypeEnum.CollaboratorType.READ_ONLY);
          if (deployCollaborators.contains(collaborator)) {
            collPermBuilder.setCanDeploy(TernaryEnum.Ternary.TRUE);
          } else {
            collPermBuilder.setCanDeploy(TernaryEnum.Ternary.FALSE);
          }
          getCollaboratorResponseBuilder.setPermission(collPermBuilder.build());

          getCollaboratorResponseList.add(getCollaboratorResponseBuilder.build());
        }
      }

      if (readWriteCollaborators.size() > 0) {
        LOGGER.debug("ReadWrite Collaborators count: " + readWriteCollaborators.size());
        for (CollaboratorBase collaborator : readWriteCollaborators) {
          GetCollaboratorResponseItem.Builder getCollaboratorResponseBuilder =
              GetCollaboratorResponseItem.newBuilder()
                  .setAuthzEntityType(collaborator.getAuthzEntityType())
                  .setVertaId(collaborator.getId());
          CollaboratorPermissions.Builder collPermBuilder = CollaboratorPermissions.newBuilder();
          collPermBuilder.setCollaboratorType(CollaboratorTypeEnum.CollaboratorType.READ_WRITE);
          if (deployCollaborators.contains(collaborator)) {
            collPermBuilder.setCanDeploy(TernaryEnum.Ternary.TRUE);
          } else {
            collPermBuilder.setCanDeploy(TernaryEnum.Ternary.FALSE);
          }
          getCollaboratorResponseBuilder.setPermission(collPermBuilder.build());
          getCollaboratorResponseList.add(getCollaboratorResponseBuilder.build());
        }
      }
    } catch (InterruptedException | ExecutionException ex) {
      Status status =
          Status.newBuilder().setCode(Code.INTERNAL_VALUE).setMessage(ex.getMessage()).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
    LOGGER.debug("Total Collaborators count: " + getCollaboratorResponseList.size());
    return getCollaboratorResponseList;
  }

  private Set<CollaboratorBase> getCollaborators(
      AuthServiceChannel authServiceChannel,
      String resourceOwnerId,
      String resourceId,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBActionEnum.ModelDBServiceActions modelDBServiceActions,
      Metadata requestHeaders) {
    GetAllowedEntities getAllowedEntitiesRequest =
        GetAllowedEntities.newBuilder()
            .addActions(
                Action.newBuilder()
                    .setModeldbServiceAction(modelDBServiceActions)
                    .setService(Service.MODELDB_SERVICE)
                    .build())
            .addResources(
                Resources.newBuilder()
                    .addResourceIds(resourceId)
                    .setService(Service.MODELDB_SERVICE)
                    .setResourceType(
                        ResourceType.newBuilder()
                            .setModeldbServiceResourceType(modelDBServiceResourceTypes))
                    .build())
            .build();
    LOGGER.info(CommonMessages.CALL_TO_ROLE_SERVICE_MSG);
    GetAllowedEntities.Response getAllowedEntitiesResponse =
        authServiceChannel
            .getAuthzServiceBlockingStub(requestHeaders)
            .getAllowedEntities(getAllowedEntitiesRequest);
    LOGGER.info(CommonMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
    LOGGER.trace(CommonMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getAllowedEntitiesResponse);

    Set<CollaboratorBase> collaborators = new HashSet<>();
    if (getAllowedEntitiesResponse.getEntitiesCount() != 0) {
      for (Entities entities : getAllowedEntitiesResponse.getEntitiesList()) {
        entities.getUserIdsList().stream()
            .filter(id -> !id.equals(resourceOwnerId))
            .forEach(id -> collaborators.add(new CollaboratorUser(authService, id)));
        entities
            .getTeamIdsList()
            .forEach(
                teamId -> {
                  try {
                    CollaboratorTeam collaboratorTeam = new CollaboratorTeam(teamId);
                    collaborators.add(collaboratorTeam);
                  } catch (StatusRuntimeException ex) {
                    if (ex.getStatus().getCode().value() == Code.PERMISSION_DENIED_VALUE) {
                      LOGGER.info(
                          "Current user is not a member of the team : "
                              + teamId
                              + ", "
                              + ex.getMessage(),
                          ex);
                    }
                  }
                });
        entities
            .getOrgIdsList()
            .forEach(
                orgId -> {
                  try {
                    CollaboratorOrg collaboratorOrg = new CollaboratorOrg(orgId);
                    collaborators.add(collaboratorOrg);
                  } catch (StatusRuntimeException ex) {
                    if (ex.getStatus().getCode().value() == Code.PERMISSION_DENIED_VALUE) {
                      LOGGER.info(
                          "Current user is not a member of the organization : "
                              + orgId
                              + ", "
                              + ex.getMessage(),
                          ex);
                    }
                  }
                });
      }
    }

    return collaborators;
  }

  @Override
  public GeneratedMessageV3 getTeamById(String teamId) {
    return getTeamById(true, teamId);
  }

  public GeneratedMessageV3 getTeamById(boolean retry, String teamId) {
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      GetTeamById getTeamById = GetTeamById.newBuilder().setTeamId(teamId).build();
      GetTeamById.Response getTeamByIdResponse =
          authServiceChannel.getTeamServiceBlockingStub().getTeamById(getTeamById);
      return getTeamByIdResponse.getTeam();
    } catch (StatusRuntimeException ex) {
      return (GeneratedMessageV3)
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<GeneratedMessageV3>) (retry1) -> getTeamById(teamId),
              timeout);
    }
  }

  @Override
  public GeneratedMessageV3 getOrgById(String orgId) {
    return getOrgById(true, orgId);
  }

  private GeneratedMessageV3 getOrgById(boolean retry, String orgId) {
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      GetOrganizationById getOrgById = GetOrganizationById.newBuilder().setOrgId(orgId).build();
      GetOrganizationById.Response getOrgByIdResponse =
          authServiceChannel.getOrganizationServiceBlockingStub().getOrganizationById(getOrgById);
      return getOrgByIdResponse.getOrganization();
    } catch (StatusRuntimeException ex) {
      return (GeneratedMessageV3)
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<GeneratedMessageV3>)
                  (retry1) -> getOrgById(retry1, orgId),
              timeout);
    }
  }
}
