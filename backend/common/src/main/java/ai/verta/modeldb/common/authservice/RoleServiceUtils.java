package ai.verta.modeldb.common.authservice;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.uac.CollaboratorPermissions;
import ai.verta.uac.DeleteResources;
import ai.verta.uac.GetOrganizationById;
import ai.verta.uac.GetResources;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.GetTeamById;
import ai.verta.uac.ResourceType;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.Resources;
import ai.verta.uac.ServiceEnum.Service;
import ai.verta.uac.SetResource;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RoleServiceUtils implements RoleService {
  private static final Logger LOGGER = LogManager.getLogger(RoleServiceUtils.class);
  protected AuthService authService;
  private final String host;
  private final Integer port;
  private final String serviceUserEmail;
  private final String serviceUserDevKey;
  private final Context.Key<Metadata> metadataInfo;
  private Integer timeout;

  public RoleServiceUtils(
      AuthService authService,
      String host,
      Integer port,
      String serviceUserEmail,
      String serviceUserDevKey,
      Integer timeout,
      Context.Key<Metadata> metadataInfo) {
    this.authService = authService;
    this.host = host;
    this.port = port;
    this.serviceUserEmail = serviceUserEmail;
    this.serviceUserDevKey = serviceUserDevKey;
    this.timeout = timeout;
    this.metadataInfo = metadataInfo;
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
    return new AuthServiceChannel(host, port, serviceUserEmail, serviceUserDevKey, metadataInfo);
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
  public GetResourcesResponseItem getEntityResource(
      String entityId, ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    ResourceType resourceType =
        ResourceType.newBuilder()
            .setModeldbServiceResourceType(modelDBServiceResourceTypes)
            .build();
    Resources resources =
        Resources.newBuilder()
            .setResourceType(resourceType)
            .setService(Service.MODELDB_SERVICE)
            .addResourceIds(entityId)
            .build();
    List<GetResourcesResponseItem> responseItems = getResourceItems(Optional.of(resources));
    if (responseItems.size() > 1) {
      LOGGER.warn(
          "Role service returned {}"
              + " resource response items fetching {} resource, but only expected 1. ID: {}",
          responseItems.size(),
          modelDBServiceResourceTypes.name(),
          entityId);
    }
    final Optional<GetResourcesResponseItem> responseItem =
        responseItems.stream()
            .filter(
                item ->
                    item.getResourceType().getModeldbServiceResourceType()
                            == modelDBServiceResourceTypes
                        && item.getService() == Service.MODELDB_SERVICE)
            .findFirst();
    if (responseItem.isPresent()) {
      return responseItem.get();
    }
    throw new IllegalArgumentException(
        "Failed to locate "
            + modelDBServiceResourceTypes.name()
            + " resources in UAC for "
            + modelDBServiceResourceTypes.name()
            + " ID "
            + entityId);
  }

  public List<GetResourcesResponseItem> getResourceItems(Optional<Resources> filterTo) {
    try (AuthServiceChannel authServiceChannel = getAuthServiceChannel()) {
      final GetResources.Builder getResourcesBuilder = GetResources.newBuilder();
      filterTo.ifPresent(getResourcesBuilder::setResources);

      final GetResources.Response response =
          authServiceChannel
              .getCollaboratorServiceBlockingStub()
              .getResources(getResourcesBuilder.build());
      return response.getItemList();
    } catch (StatusRuntimeException ex) {
      LOGGER.error(ex);
      throw ex;
    }
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
