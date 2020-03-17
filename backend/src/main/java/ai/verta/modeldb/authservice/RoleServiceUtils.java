package ai.verta.modeldb.authservice;

import ai.verta.common.TernaryEnum;
import ai.verta.modeldb.DatasetVisibilityEnum.DatasetVisibility;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.ProjectVisibility;
import ai.verta.modeldb.WorkspaceTypeEnum.WorkspaceType;
import ai.verta.modeldb.collaborator.CollaboratorBase;
import ai.verta.modeldb.collaborator.CollaboratorOrg;
import ai.verta.modeldb.collaborator.CollaboratorTeam;
import ai.verta.modeldb.collaborator.CollaboratorUser;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.uac.Action;
import ai.verta.uac.Actions;
import ai.verta.uac.CollaboratorTypeEnum.CollaboratorType;
import ai.verta.uac.DeleteRoleBinding;
import ai.verta.uac.Entities;
import ai.verta.uac.GetAllowedEntities;
import ai.verta.uac.GetAllowedResources;
import ai.verta.uac.GetCollaboratorResponse;
import ai.verta.uac.GetOrganizationById;
import ai.verta.uac.GetOrganizationByName;
import ai.verta.uac.GetRoleBindingByName;
import ai.verta.uac.GetRoleByName;
import ai.verta.uac.GetSelfAllowedActionsBatch;
import ai.verta.uac.GetSelfAllowedResources;
import ai.verta.uac.GetTeamById;
import ai.verta.uac.GetTeamByName;
import ai.verta.uac.IsSelfAllowed;
import ai.verta.uac.ListMyOrganizations;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ModelResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.uac.Organization;
import ai.verta.uac.Resources;
import ai.verta.uac.Role;
import ai.verta.uac.RoleBinding;
import ai.verta.uac.RoleScope;
import ai.verta.uac.ServiceEnum;
import ai.verta.uac.SetRoleBinding;
import ai.verta.uac.UserInfo;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.ProtocolMessageEnum;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RoleServiceUtils implements RoleService {
  private static final Logger LOGGER = LogManager.getLogger(RoleServiceUtils.class);
  private AuthService authService;

  public RoleServiceUtils(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public boolean IsImplemented() {
    return true;
  }

  @Override
  public String buildRoleBindingName(
      String roleName, String resourceId, String userId, String resourceTypeName) {
    return buildRoleBindingName(
        roleName, resourceId, new CollaboratorUser(authService, userId), resourceTypeName);
  }

  @Override
  public String buildRoleBindingName(
      String roleName, String resourceId, CollaboratorBase collaborator, String resourceTypeName) {
    return roleName
        + "_"
        + resourceTypeName
        + "_"
        + resourceId
        + "_"
        + collaborator.getNameForBinding();
  }

  @Override
  public void createRoleBinding(
      Role role,
      CollaboratorBase collaborator,
      String resourceId,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    String roleBindingName =
        buildRoleBindingName(
            role.getName(), resourceId, collaborator, modelDBServiceResourceTypes.name());

    RoleBinding newRoleBinding =
        RoleBinding.newBuilder()
            .setName(roleBindingName)
            .setScope(role.getScope())
            .setRoleId(role.getId())
            .addEntities(collaborator.getEntities())
            .addResources(
                Resources.newBuilder()
                    .setService(ServiceEnum.Service.MODELDB_SERVICE)
                    .setModeldbServiceResourceType(modelDBServiceResourceTypes)
                    .addResourceIds(resourceId)
                    .build())
            .build();
    try {
      setRoleBindingOnAuthService(newRoleBinding);
    } catch (StatusRuntimeException ex) {
      LOGGER.warn(ex.getMessage(), ex);
      if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
        Status status =
            Status.newBuilder()
                .setCode(Code.UNAVAILABLE_VALUE)
                .setMessage("UAC Service unavailable : " + ex.getMessage())
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      throw ex;
    }
  }

  @Override
  public void isSelfAllowed(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      String resourceId) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);
      Resources.Builder resourceBuilder =
          Resources.newBuilder()
              .setService(ServiceEnum.Service.MODELDB_SERVICE)
              .setModeldbServiceResourceType(modelDBServiceResourceTypes);
      if (resourceId != null) {
        resourceBuilder.addResourceIds(resourceId);
      }
      IsSelfAllowed isSelfAllowedRequest =
          IsSelfAllowed.newBuilder()
              .addResources(resourceBuilder.build())
              .addActions(
                  Action.newBuilder()
                      .setService(ServiceEnum.Service.MODELDB_SERVICE)
                      .setModeldbServiceAction(modelDBServiceActions)
                      .build())
              .build();

      IsSelfAllowed.Response isSelfAllowedResponse =
          authServiceChannel.getAuthzServiceBlockingStub().isSelfAllowed(isSelfAllowedRequest);
      LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, isSelfAllowedResponse);

      if (!isSelfAllowedResponse.getAllowed()) {
        Status status =
            Status.newBuilder()
                .setCode(Code.PERMISSION_DENIED_VALUE)
                .setMessage("Access Denied")
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    } catch (StatusRuntimeException ex) {
      LOGGER.warn(ex.getMessage(), ex);
      if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
        Status status =
            Status.newBuilder()
                .setCode(Code.UNAVAILABLE_VALUE)
                .setMessage("UAC Service unavailable : " + ex.getMessage())
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      throw ex;
    }
  }

  @Override
  public Map<String, Actions> getSelfAllowedActionsBatch(
      List<String> resourceIds, ModelDBServiceResourceTypes type) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);
      GetSelfAllowedActionsBatch getSelfAllowedActionsBatch =
          GetSelfAllowedActionsBatch.newBuilder()
              .setResources(
                  Resources.newBuilder()
                      .setService(ServiceEnum.Service.MODELDB_SERVICE)
                      .addAllResourceIds(resourceIds)
                      .setModeldbServiceResourceType(type)
                      .build())
              .build();

      GetSelfAllowedActionsBatch.Response getSelfAllowedActionsBatchResponse =
          authServiceChannel
              .getAuthzServiceBlockingStub()
              .getSelfAllowedActionsBatch(getSelfAllowedActionsBatch);
      LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(
          ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getSelfAllowedActionsBatchResponse);
      return getSelfAllowedActionsBatchResponse.getActionsMap();

    } catch (StatusRuntimeException ex) {
      LOGGER.warn(ex.getMessage(), ex);
      if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
        Status status =
            Status.newBuilder()
                .setCode(Code.UNAVAILABLE_VALUE)
                .setMessage("UAC Service unavailable : " + ex.getMessage())
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      throw ex;
    }
  }

  @Override
  public Role getRoleByName(String roleName, RoleScope roleScope) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);
      GetRoleByName.Builder getRoleByNameRequest = GetRoleByName.newBuilder().setName(roleName);
      if (roleScope != null) {
        getRoleByNameRequest.setScope(roleScope);
      }
      GetRoleByName.Response getRoleByNameResponse =
          authServiceChannel
              .getRoleServiceBlockingStub()
              .getRoleByName(getRoleByNameRequest.build());
      LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getRoleByNameResponse);

      return getRoleByNameResponse.getRole();
    } catch (StatusRuntimeException ex) {
      LOGGER.warn(ex.getMessage(), ex);
      if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
        Status status =
            Status.newBuilder()
                .setCode(Code.UNAVAILABLE_VALUE)
                .setMessage("UAC Service unavailable : " + ex.getMessage())
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      throw ex;
    }
  }

  @Override
  public boolean deleteRoleBinding(String roleBindingId) {
    DeleteRoleBinding deleteRoleBindingRequest =
        DeleteRoleBinding.newBuilder().setId(roleBindingId).build();
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);
      DeleteRoleBinding.Response deleteRoleBindingResponse =
          authServiceChannel
              .getRoleServiceBlockingStub()
              .deleteRoleBinding(deleteRoleBindingRequest);
      LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, deleteRoleBindingResponse);

      return deleteRoleBindingResponse.getStatus();
    } catch (StatusRuntimeException ex) {
      LOGGER.warn(ex.getMessage(), "Exception deleting rolebinding", roleBindingId);
      if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
        Status status =
            Status.newBuilder()
                .setCode(Code.UNAVAILABLE_VALUE)
                .setMessage("UAC Service unavailable : " + ex.getMessage())
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      throw ex;
    }
  }

  private void setRoleBindingOnAuthService(RoleBinding roleBinding) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);
      SetRoleBinding.Response setRoleBindingResponse =
          authServiceChannel
              .getRoleServiceBlockingStub()
              .setRoleBinding(SetRoleBinding.newBuilder().setRoleBinding(roleBinding).build());
      LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, setRoleBindingResponse);
    } catch (StatusRuntimeException ex) {
      LOGGER.warn(ex.getMessage(), ex);
      if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
        Status status =
            Status.newBuilder()
                .setCode(Code.UNAVAILABLE_VALUE)
                .setMessage("UAC Service unavailable : " + ex.getMessage())
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      throw ex;
    }
  }

  @Override
  public List<GetCollaboratorResponse> getResourceCollaborators(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      String resourceId,
      String resourceOwnerId) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      return getCollaborators(
          authServiceChannel, resourceOwnerId, resourceId, modelDBServiceResourceTypes);
    } catch (StatusRuntimeException ex) {
      LOGGER.warn(ex.getMessage(), ex);
      if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
        Status status =
            Status.newBuilder()
                .setCode(Code.UNAVAILABLE_VALUE)
                .setMessage("UAC Service unavailable : " + ex.getMessage())
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      throw ex;
    }
  }

  private Set<CollaboratorBase> getCollaborators(
      AuthServiceChannel authServiceChannel,
      String resourceOwnerId,
      String resourceId,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions) {
    GetAllowedEntities getAllowedEntitiesRequest =
        GetAllowedEntities.newBuilder()
            .addActions(
                Action.newBuilder()
                    .setModeldbServiceAction(modelDBServiceActions)
                    .setService(ServiceEnum.Service.MODELDB_SERVICE)
                    .build())
            .addResources(
                Resources.newBuilder()
                    .addResourceIds(resourceId)
                    .setService(ServiceEnum.Service.MODELDB_SERVICE)
                    .setModeldbServiceResourceType(modelDBServiceResourceTypes)
                    .build())
            .build();
    LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);
    GetAllowedEntities.Response getAllowedEntitiesResponse =
        authServiceChannel
            .getAuthzServiceBlockingStub()
            .getAllowedEntities(getAllowedEntitiesRequest);
    LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
    LOGGER.trace(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getAllowedEntitiesResponse);

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
                      LOGGER.warn(
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
                      LOGGER.warn(
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

  private List<GetCollaboratorResponse> getCollaborators(
      AuthServiceChannel authServiceChannel,
      String resourceOwnerId,
      String resourceId,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    List<GetCollaboratorResponse> getCollaboratorResponseList = new ArrayList<>();

    Set<CollaboratorBase> deployCollaborators =
        getCollaborators(
            authServiceChannel,
            resourceOwnerId,
            resourceId,
            modelDBServiceResourceTypes,
            ModelDBServiceActions.DEPLOY);
    Set<CollaboratorBase> readOnlyCollaborators =
        getCollaborators(
            authServiceChannel,
            resourceOwnerId,
            resourceId,
            modelDBServiceResourceTypes,
            ModelDBServiceActions.READ);
    Set<CollaboratorBase> readWriteCollaborators =
        getCollaborators(
            authServiceChannel,
            resourceOwnerId,
            resourceId,
            modelDBServiceResourceTypes,
            ModelDBServiceActions.UPDATE);

    readOnlyCollaborators.removeAll(readWriteCollaborators);
    if (readOnlyCollaborators.size() > 0) {
      LOGGER.debug("ReadOnly Collaborators count: " + readOnlyCollaborators.size());
      for (CollaboratorBase collaborator : readOnlyCollaborators) {
        GetCollaboratorResponse.Builder getCollaboratorResponseBuilder =
            GetCollaboratorResponse.newBuilder()
                .setAuthzEntityType(collaborator.getAuthzEntityType())
                .setVertaId(collaborator.getId())
                .setCollaboratorType(CollaboratorType.READ_ONLY);
        if (deployCollaborators.contains(collaborator)) {
          getCollaboratorResponseBuilder.setCanDeploy(TernaryEnum.Ternary.TRUE);
        } else {
          getCollaboratorResponseBuilder.setCanDeploy(TernaryEnum.Ternary.FALSE);
        }

        getCollaboratorResponseList.add(getCollaboratorResponseBuilder.build());
      }
    }

    if (readWriteCollaborators.size() > 0) {
      LOGGER.debug("ReadWrite Collaborators count: " + readWriteCollaborators.size());
      for (CollaboratorBase collaborator : readWriteCollaborators) {
        GetCollaboratorResponse.Builder getCollaboratorResponseBuilder =
            GetCollaboratorResponse.newBuilder()
                .setAuthzEntityType(collaborator.getAuthzEntityType())
                .setVertaId(collaborator.getId())
                .setCollaboratorType(CollaboratorType.READ_WRITE);
        if (deployCollaborators.contains(collaborator)) {
          getCollaboratorResponseBuilder.setCanDeploy(TernaryEnum.Ternary.TRUE);
        } else {
          getCollaboratorResponseBuilder.setCanDeploy(TernaryEnum.Ternary.FALSE);
        }
        getCollaboratorResponseList.add(getCollaboratorResponseBuilder.build());
      }
    }
    LOGGER.debug("Total Collaborators count: " + getCollaboratorResponseList.size());
    return getCollaboratorResponseList;
  }

  /**
   * Checks permissions of the user wrt the Entity
   *
   * @param resourceId --> value of key like project.id, dataset.id etc.
   * @param modelDBServiceActions --> ModelDBServiceActions.UPDATE, ModelDBServiceActions.DELETE,
   *     ModelDBServiceActions.CREATE
   */
  @Override
  public void validateEntityUserWithUserInfo(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      String resourceId,
      ModelDBServiceActions modelDBServiceActions) {
    // Check User Role
    isSelfAllowed(modelDBServiceResourceTypes, modelDBServiceActions, resourceId);
  }

  @Override
  public String buildReadOnlyRoleBindingName(
      String resourceId,
      CollaboratorBase collaborator,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.PROJECT)) {
      return buildRoleBindingName(
          ModelDBConstants.ROLE_PROJECT_READ_ONLY,
          resourceId,
          collaborator,
          ModelDBServiceResourceTypes.PROJECT.name());
    } else if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.DATASET)) {
      return buildRoleBindingName(
          ModelDBConstants.ROLE_DATASET_READ_ONLY,
          resourceId,
          collaborator,
          ModelDBServiceResourceTypes.DATASET.name());
    } else {
      return ModelDBConstants.EMPTY_STRING;
    }
  }

  @Override
  public String buildReadWriteRoleBindingName(
      String resourceId,
      CollaboratorBase collaborator,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.PROJECT)) {
      return buildRoleBindingName(
          ModelDBConstants.ROLE_PROJECT_READ_WRITE,
          resourceId,
          collaborator,
          ModelDBServiceResourceTypes.PROJECT.name());
    } else if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.DATASET)) {
      return buildRoleBindingName(
          ModelDBConstants.ROLE_DATASET_READ_WRITE,
          resourceId,
          collaborator,
          ModelDBServiceResourceTypes.DATASET.name());
    } else {
      return ModelDBConstants.EMPTY_STRING;
    }
  }

  @Override
  public String buildAdminRoleBindingName(
      String resourceId,
      CollaboratorBase collaborator,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.PROJECT)) {
      return buildRoleBindingName(
          ModelDBConstants.ROLE_PROJECT_ADMIN,
          resourceId,
          collaborator,
          ModelDBServiceResourceTypes.PROJECT.name());
    } else if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.DATASET)) {
      return buildRoleBindingName(
          ModelDBConstants.ROLE_DATASET_ADMIN,
          resourceId,
          collaborator,
          ModelDBServiceResourceTypes.DATASET.name());
    } else {
      return ModelDBConstants.EMPTY_STRING;
    }
  }

  @Override
  public String buildProjectDeployRoleBindingName(
      String resourceId,
      CollaboratorBase collaborator,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    if (modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.PROJECT)) {
      return buildRoleBindingName(
          ModelDBConstants.ROLE_PROJECT_DEPLOY,
          resourceId,
          collaborator,
          ModelDBServiceResourceTypes.PROJECT.name());
    } else {
      return ModelDBConstants.EMPTY_STRING;
    }
  }

  @Override
  public void removeResourceRoleBindings(
      String resourceId,
      String resourceOwnerId,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {

    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      Set<CollaboratorBase> readCollaborators =
          getCollaborators(
              authServiceChannel,
              resourceOwnerId,
              resourceId,
              modelDBServiceResourceTypes,
              ModelDBServiceActions.READ);
      Set<CollaboratorBase> readWriteCollaborators =
          getCollaborators(
              authServiceChannel,
              resourceOwnerId,
              resourceId,
              modelDBServiceResourceTypes,
              ModelDBServiceActions.UPDATE);
      Set<CollaboratorBase> deployCollaborators =
          getCollaborators(
              authServiceChannel,
              resourceOwnerId,
              resourceId,
              modelDBServiceResourceTypes,
              ModelDBServiceActions.DEPLOY);

      Set<CollaboratorBase> finalCollaborators = new HashSet<>();
      finalCollaborators.addAll(readCollaborators);
      finalCollaborators.addAll(readWriteCollaborators);
      if (finalCollaborators.size() > 0) {
        for (CollaboratorBase collaborator : finalCollaborators) {

          String readOnlyRoleBindingName =
              buildReadOnlyRoleBindingName(resourceId, collaborator, modelDBServiceResourceTypes);
          RoleBinding readOnlyRoleBinding = getRoleBindingByName(readOnlyRoleBindingName);
          if (readOnlyRoleBinding != null && !readOnlyRoleBinding.getId().isEmpty()) {
            deleteRoleBinding(readOnlyRoleBinding.getId());
          }

          String readWriteRoleBindingName =
              buildReadWriteRoleBindingName(resourceId, collaborator, modelDBServiceResourceTypes);
          RoleBinding readWriteRoleBinding = getRoleBindingByName(readWriteRoleBindingName);
          if (readWriteRoleBinding != null && !readWriteRoleBinding.getId().isEmpty()) {
            deleteRoleBinding(readWriteRoleBinding.getId());
          }

          if (deployCollaborators.contains(collaborator)
              && modelDBServiceResourceTypes.equals(ModelDBServiceResourceTypes.PROJECT)) {
            String deployRoleBindingName =
                buildProjectDeployRoleBindingName(
                    resourceId, collaborator, modelDBServiceResourceTypes);
            RoleBinding deployProjectRoleBinding = getRoleBindingByName(deployRoleBindingName);
            if (deployProjectRoleBinding != null && !deployProjectRoleBinding.getId().isEmpty()) {
              deleteRoleBinding(deployProjectRoleBinding.getId());
            }
          }
        }
      }
    } catch (StatusRuntimeException ex) {
      LOGGER.warn(ex.getMessage(), ex);
      if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
        Status status =
            Status.newBuilder()
                .setCode(Code.UNAVAILABLE_VALUE)
                .setMessage("UAC Service unavailable : " + ex.getMessage())
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      throw ex;
    }
  }

  @Override
  public RoleBinding getRoleBindingByName(String roleBindingName) {
    GetRoleBindingByName getRoleBindingByNameRequest =
        GetRoleBindingByName.newBuilder().setName(roleBindingName).build();
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);
      GetRoleBindingByName.Response getRoleBindingByNameResponse =
          authServiceChannel
              .getRoleServiceBlockingStub()
              .getRoleBindingByName(getRoleBindingByNameRequest);
      LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(
          ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getRoleBindingByNameResponse);

      return getRoleBindingByNameResponse.getRoleBinding();
    } catch (StatusRuntimeException ex) {
      LOGGER.warn(roleBindingName + " : " + ex.getMessage());
      if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
        Status status =
            Status.newBuilder()
                .setCode(Code.UNAVAILABLE_VALUE)
                .setMessage("UAC Service unavailable : " + ex.getMessage())
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      } else if (ex.getStatus().getCode().value() == Code.NOT_FOUND_VALUE) {
        return RoleBinding.newBuilder().build();
      }
      throw ex;
    }
  }

  @Override
  public List<String> getSelfAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions) {
    Action action =
        Action.newBuilder()
            .setService(ServiceEnum.Service.MODELDB_SERVICE)
            .setModeldbServiceAction(modelDBServiceActions)
            .build();
    GetSelfAllowedResources getAllowedResourcesRequest =
        GetSelfAllowedResources.newBuilder().addActions(action).build();
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);
      GetSelfAllowedResources.Response getAllowedResourcesResponse =
          authServiceChannel
              .getAuthzServiceBlockingStub()
              .getSelfAllowedResources(getAllowedResourcesRequest);
      LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(
          ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getAllowedResourcesResponse);

      if (getAllowedResourcesResponse.getResourcesList().size() > 0) {
        List<String> resourcesIds = new ArrayList<>();
        for (Resources resources : getAllowedResourcesResponse.getResourcesList()) {
          if (resources.getModeldbServiceResourceType().equals(modelDBServiceResourceTypes)) {
            resourcesIds.addAll(resources.getResourceIdsList());
          }
        }
        return resourcesIds;
      } else {
        return Collections.emptyList();
      }
    } catch (StatusRuntimeException ex) {
      LOGGER.warn(ex.getMessage(), ex);
      if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
        Status status =
            Status.newBuilder()
                .setCode(Code.UNAVAILABLE_VALUE)
                .setMessage("UAC Service unavailable : " + ex.getMessage())
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      throw ex;
    }
  }

  @Override
  public List<String> getSelfDirectlyAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions) {
    Action action =
        Action.newBuilder()
            .setService(ServiceEnum.Service.MODELDB_SERVICE)
            .setModeldbServiceAction(modelDBServiceActions)
            .build();
    GetSelfAllowedResources getAllowedResourcesRequest =
        GetSelfAllowedResources.newBuilder().addActions(action).build();
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);
      GetSelfAllowedResources.Response getAllowedResourcesResponse =
          authServiceChannel
              .getAuthzServiceBlockingStub()
              .getSelfDirectlyAllowedResources(getAllowedResourcesRequest);
      LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(
          ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getAllowedResourcesResponse);

      if (getAllowedResourcesResponse.getResourcesList().size() > 0) {
        List<String> getSelfDirectlyAllowedResourceIds = new ArrayList<>();
        for (Resources resources : getAllowedResourcesResponse.getResourcesList()) {
          if (resources.getModeldbServiceResourceType().equals(modelDBServiceResourceTypes)) {
            getSelfDirectlyAllowedResourceIds.addAll(resources.getResourceIdsList());
          }
        }
        return getSelfDirectlyAllowedResourceIds;
      } else {
        return Collections.emptyList();
      }
    } catch (StatusRuntimeException ex) {
      LOGGER.warn(ex.getMessage(), ex);
      if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
        Status status =
            Status.newBuilder()
                .setCode(Code.UNAVAILABLE_VALUE)
                .setMessage("UAC Service unavailable : " + ex.getMessage())
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      throw ex;
    }
  }

  @Override
  public List<String> getAllowedResources(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      CollaboratorBase collaboratorBase) {
    Action action =
        Action.newBuilder()
            .setService(ServiceEnum.Service.MODELDB_SERVICE)
            .setModeldbServiceAction(modelDBServiceActions)
            .build();
    Entities entity = collaboratorBase.getEntities();
    GetAllowedResources getAllowedResourcesRequest =
        GetAllowedResources.newBuilder().addActions(action).addEntities(entity).build();
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(ModelDBMessages.CALL_TO_ROLE_SERVICE_MSG);
      GetAllowedResources.Response getAllowedResourcesResponse =
          authServiceChannel
              .getAuthzServiceBlockingStub()
              .getAllowedResources(getAllowedResourcesRequest);
      LOGGER.info(ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_MSG);
      LOGGER.trace(
          ModelDBMessages.ROLE_SERVICE_RES_RECEIVED_TRACE_MSG, getAllowedResourcesResponse);

      if (getAllowedResourcesResponse.getResourcesList().size() > 0) {
        List<String> resourcesIds = new ArrayList<>();
        for (Resources resources : getAllowedResourcesResponse.getResourcesList()) {
          if (resources.getModeldbServiceResourceType().equals(modelDBServiceResourceTypes)) {
            resourcesIds.addAll(resources.getResourceIdsList());
          }
        }
        return resourcesIds;
      } else {
        return Collections.emptyList();
      }
    } catch (StatusRuntimeException ex) {
      LOGGER.warn(ex.getMessage(), ex);
      if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
        Status status =
            Status.newBuilder()
                .setCode(Code.UNAVAILABLE_VALUE)
                .setMessage("UAC Service unavailable : " + ex.getMessage())
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      throw ex;
    }
  }

  @Override
  public GeneratedMessageV3 getTeamById(String teamId) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      GetTeamById getTeamById = GetTeamById.newBuilder().setTeamId(teamId).build();
      GetTeamById.Response getTeamByIdResponse =
          authServiceChannel.getTeamServiceBlockingStub().getTeamById(getTeamById);
      return getTeamByIdResponse.getTeam();
    } catch (StatusRuntimeException ex) {
      LOGGER.warn(ex.getMessage(), ex);
      if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
        Status status =
            Status.newBuilder()
                .setCode(Code.UNAVAILABLE_VALUE)
                .setMessage("UAC Service unavailable : " + ex.getMessage())
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      throw ex;
    }
  }

  @Override
  public GeneratedMessageV3 getTeamByName(String orgId, String teamName) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      GetTeamByName getTeamByName =
          GetTeamByName.newBuilder().setTeamName(teamName).setOrgId(orgId).build();
      GetTeamByName.Response getTeamByNameResponse =
          authServiceChannel.getTeamServiceBlockingStub().getTeamByName(getTeamByName);
      return getTeamByNameResponse.getTeam();
    } catch (StatusRuntimeException ex) {
      LOGGER.warn(ex.getMessage(), ex);
      if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
        Status status =
            Status.newBuilder()
                .setCode(Code.UNAVAILABLE_VALUE)
                .setMessage("UAC Service unavailable : " + ex.getMessage())
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      throw ex;
    }
  }

  @Override
  public GeneratedMessageV3 getOrgById(String orgId) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      GetOrganizationById getOrgById = GetOrganizationById.newBuilder().setOrgId(orgId).build();
      GetOrganizationById.Response getOrgByIdResponse =
          authServiceChannel.getOrganizationServiceBlockingStub().getOrganizationById(getOrgById);
      return getOrgByIdResponse.getOrganization();
    } catch (StatusRuntimeException ex) {
      LOGGER.warn(ex.getMessage(), ex);
      if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
        Status status =
            Status.newBuilder()
                .setCode(Code.UNAVAILABLE_VALUE)
                .setMessage("UAC Service unavailable : " + ex.getMessage())
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      throw ex;
    }
  }

  @Override
  public GeneratedMessageV3 getOrgByName(String name) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      GetOrganizationByName getOrgByName =
          GetOrganizationByName.newBuilder().setOrgName(name).build();
      GetOrganizationByName.Response getOrgByNameResponse =
          authServiceChannel
              .getOrganizationServiceBlockingStub()
              .getOrganizationByName(getOrgByName);
      return getOrgByNameResponse.getOrganization();
    } catch (StatusRuntimeException ex) {
      LOGGER.warn(ex.getMessage());
      if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
        Status status =
            Status.newBuilder()
                .setCode(Code.UNAVAILABLE_VALUE)
                .setMessage("UAC Service unavailable : " + ex.getMessage())
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      throw ex;
    }
  }

  private List<String> getReadOnlyAccessibleResourceIds(
      boolean isHostUser,
      CollaboratorBase userInfo,
      ProtocolMessageEnum resourceVisibility,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {

    Set<String> resourceIdsSet = new HashSet<>();
    if (userInfo != null && userInfo.getVertaId() != null) {
      List<String> accessibleResourceIds;
      if (isHostUser) {
        accessibleResourceIds =
            getAllowedResources(modelDBServiceResourceTypes, ModelDBServiceActions.READ, userInfo);
      } else {
        accessibleResourceIds =
            getSelfAllowedResources(modelDBServiceResourceTypes, ModelDBServiceActions.READ);
      }
      resourceIdsSet.addAll(accessibleResourceIds);
      LOGGER.debug(
          "Accessible " + modelDBServiceResourceTypes + " Ids size is {}",
          accessibleResourceIds.size());
    }

    if (resourceVisibility.equals(ProjectVisibility.PUBLIC)
        || resourceVisibility.equals(DatasetVisibility.PUBLIC)) {
      UserInfo unsignedUserInfo = authService.getUnsignedUser();
      List<String> publicResourceIds =
          getAllowedResources(
              modelDBServiceResourceTypes,
              ModelDBServiceActions.PUBLIC_READ,
              new CollaboratorUser(authService, unsignedUserInfo));
      LOGGER.debug(
          "Public " + modelDBServiceResourceTypes + " Id size is {}", publicResourceIds.size());

      if (resourceIdsSet.size() > 0) {
        resourceIdsSet.retainAll(publicResourceIds);
      } else {
        resourceIdsSet.addAll(publicResourceIds);
      }
      return new ArrayList<>(resourceIdsSet);
    } else {
      return new ArrayList<>(resourceIdsSet);
    }
  }

  @Override
  public List<String> getAccessibleResourceIds(
      CollaboratorBase hostUserInfo,
      CollaboratorBase currentLoginUserInfo,
      ProtocolMessageEnum resourceVisibility,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      List<String> requestedResourceIds) {
    List<String> accessibleResourceIds;
    if (hostUserInfo != null) {
      accessibleResourceIds =
          getReadOnlyAccessibleResourceIds(
              true, hostUserInfo, resourceVisibility, modelDBServiceResourceTypes);
    } else {
      accessibleResourceIds =
          getReadOnlyAccessibleResourceIds(
              false, currentLoginUserInfo, resourceVisibility, modelDBServiceResourceTypes);
    }

    if (requestedResourceIds != null && !requestedResourceIds.isEmpty()) {
      accessibleResourceIds.retainAll(requestedResourceIds);
    }
    return accessibleResourceIds;
  }

  @Override
  public List<String> getAccessibleResourceIdsByActions(
      ModelDBServiceResourceTypes modelDBServiceResourceTypes,
      ModelDBServiceActions modelDBServiceActions,
      List<String> requestedIdList) {
    List<String> allowedResourceIds = new ArrayList<>();
    if (requestedIdList.size() == 1) {
      isSelfAllowed(modelDBServiceResourceTypes, modelDBServiceActions, requestedIdList.get(0));
      allowedResourceIds.addAll(requestedIdList);
    } else {
      List<String> allowedResourceIdList =
          getSelfAllowedResources(modelDBServiceResourceTypes, modelDBServiceActions);
      // Validate if current user has access to the entity or not
      allowedResourceIdList.retainAll(requestedIdList);
      allowedResourceIds.addAll(allowedResourceIdList);
    }
    return allowedResourceIds;
  }

  @Override
  public WorkspaceDTO getWorkspaceDTOByWorkspaceName(
      UserInfo currentLoginUserInfo, String workspaceName) {
    WorkspaceDTO workspaceDTO = new WorkspaceDTO();
    workspaceDTO.setWorkspaceName(workspaceName);

    /*from the name for workspace, get the workspace id and type.
    if no workspace is present assume user's personal workspace*/
    if (workspaceName == null
        || workspaceName.isEmpty()
        || workspaceName.equalsIgnoreCase(
            authService.getUsernameFromUserInfo(currentLoginUserInfo))) {
      String vertaId = authService.getVertaIdFromUserInfo(currentLoginUserInfo);
      workspaceDTO.setWorkspaceId(vertaId);
      workspaceDTO.setWorkspaceType(WorkspaceType.USER);
    } else {
      try {
        workspaceDTO.setWorkspaceId(new CollaboratorOrg(getOrgByName(workspaceName)).getId());
        workspaceDTO.setWorkspaceType(WorkspaceType.ORGANIZATION);
      } catch (StatusRuntimeException e) {
        workspaceDTO.setWorkspaceId(
            new CollaboratorUser(
                    authService,
                    authService.getUserInfo(
                        workspaceName, ModelDBConstants.UserIdentifier.USER_NAME))
                .getId());
        workspaceDTO.setWorkspaceType(WorkspaceType.USER);
      }
    }
    return workspaceDTO;
  }

  @Override
  public List<Organization> listMyOrganizations() {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      ListMyOrganizations listMyOrganizations = ListMyOrganizations.newBuilder().build();
      ListMyOrganizations.Response listMyOrganizationsResponse =
          authServiceChannel
              .getOrganizationServiceBlockingStub()
              .listMyOrganizations(listMyOrganizations);
      return listMyOrganizationsResponse.getOrganizationsList();
    } catch (StatusRuntimeException ex) {
      LOGGER.warn(ex.getMessage());
      if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
        Status status =
            Status.newBuilder()
                .setCode(Code.UNAVAILABLE_VALUE)
                .setMessage("UAC Service unavailable : " + ex.getMessage())
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      throw ex;
    }
  }
}
