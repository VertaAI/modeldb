package ai.verta.modeldb.utils;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.dto.UserInfoPaginationDTO;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.FutureUtil;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.uac.Action;
import ai.verta.uac.GetResources;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.GetSelfAllowedResources;
import ai.verta.uac.GetUsers;
import ai.verta.uac.GetUsersFuzzy;
import ai.verta.uac.GetWorkspaceById;
import ai.verta.uac.GetWorkspaceByName;
import ai.verta.uac.ModelDBActionEnum;
import ai.verta.uac.ResourceType;
import ai.verta.uac.Resources;
import ai.verta.uac.ServiceEnum;
import ai.verta.uac.UserInfo;
import ai.verta.uac.Workspace;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UACApisUtil {
  private static final Logger LOGGER = LogManager.getLogger(UACApisUtil.class);
  protected final Executor executor;
  protected final UAC uac;

  public UACApisUtil(Executor executor, UAC uac) {
    this.executor = executor;
    this.uac = uac;
  }

  public InternalFuture<List<Resources>> getAllowedEntitiesByResourceType(
      ModelDBActionEnum.ModelDBServiceActions action,
      ModelDBResourceEnum.ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    return FutureUtil.clientRequest(
            uac.getAuthzService()
                .getSelfAllowedResources(
                    GetSelfAllowedResources.newBuilder()
                        .addActions(
                            Action.newBuilder()
                                .setModeldbServiceAction(action)
                                .setService(ServiceEnum.Service.MODELDB_SERVICE))
                        .setService(ServiceEnum.Service.MODELDB_SERVICE)
                        .setResourceType(
                            ResourceType.newBuilder()
                                .setModeldbServiceResourceType(modelDBServiceResourceTypes))
                        .build()),
            executor)
        .thenApply(GetSelfAllowedResources.Response::getResourcesList, executor);
  }

  public InternalFuture<List<GetResourcesResponseItem>> getAllowedResourceItems(
      Optional<List<String>> resourceIds,
      Long workspaceId,
      ModelDBResourceEnum.ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    var resourceType =
        ResourceType.newBuilder()
            .setModeldbServiceResourceType(modelDBServiceResourceTypes)
            .build();
    Resources.Builder resources =
        Resources.newBuilder()
            .setResourceType(resourceType)
            .setService(ServiceEnum.Service.MODELDB_SERVICE);

    if (resourceIds.isPresent() && !resourceIds.get().isEmpty()) {
      resources.addAllResourceIds(resourceIds.get());
    }

    return FutureUtil.clientRequest(
            uac.getCollaboratorService()
                .getResources(
                    GetResources.newBuilder()
                        .setResources(resources.build())
                        .setWorkspaceId(workspaceId)
                        .build()),
            executor)
        .thenApply(GetResources.Response::getItemList, executor);
  }

  public InternalFuture<List<String>> getAccessibleProjectIdsBasedOnWorkspace(
      String workspaceName, Optional<String> projectId) {
    var requestProjectIds = new ArrayList<String>();
    if (projectId.isPresent() && !projectId.get().isEmpty()) {
      requestProjectIds.add(projectId.get());
    }
    return FutureUtil.clientRequest(
            uac.getWorkspaceService()
                .getWorkspaceByName(GetWorkspaceByName.newBuilder().setName(workspaceName).build()),
            executor)
        .thenCompose(
            workspace ->
                getAllowedResourceItems(
                        Optional.of(requestProjectIds),
                        workspace.getId(),
                        ModelDBResourceEnum.ModelDBServiceResourceTypes.PROJECT)
                    .thenCompose(
                        getResourcesItems ->
                            InternalFuture.completedInternalFuture(
                                getResourcesItems.stream()
                                    .map(GetResourcesResponseItem::getResourceId)
                                    .collect(Collectors.toList())),
                        executor),
            executor);
  }

  public boolean checkAllResourceAllowed(List<Resources> resources) {
    var allowedAllResources = false;
    if (!resources.isEmpty()) {
      // This should always MODEL_DB_SERVICE be the case unless we have a bug.
      allowedAllResources = resources.get(0).getAllResourceIds();
    }
    return allowedAllResources;
  }

  public InternalFuture<Workspace> getWorkspaceById(long workspaceId) {
    return FutureUtil.clientRequest(
        uac.getWorkspaceService()
            .getWorkspaceById(GetWorkspaceById.newBuilder().setId(workspaceId).build()),
        executor);
  }

  public InternalFuture<List<GetResourcesResponseItem>> getResourceItemsForLoginUserWorkspace(
      String workspaceName,
      Optional<List<String>> resourceIdsOptional,
      ModelDBResourceEnum.ModelDBServiceResourceTypes resourceTypes) {
    var resourceType =
        ResourceType.newBuilder().setModeldbServiceResourceType(resourceTypes).build();
    Resources.Builder resources =
        Resources.newBuilder()
            .setResourceType(resourceType)
            .setService(ServiceEnum.Service.MODELDB_SERVICE);

    if (!resourceIdsOptional.isEmpty() && resourceIdsOptional.isPresent()) {
      resources.addAllResourceIds(
          resourceIdsOptional.get().stream().map(String::valueOf).collect(Collectors.toSet()));
    }

    var builder = GetResources.newBuilder().setResources(resources.build());
    builder.setWorkspaceName(workspaceName);
    return FutureUtil.clientRequest(
            uac.getCollaboratorService().getResourcesSpecialPersonalWorkspace(builder.build()),
            executor)
        .thenApply(GetResources.Response::getItemList, executor);
  }

  public InternalFuture<List<GetResourcesResponseItem>> getResourceItemsForWorkspace(
      Optional<String> workspaceName,
      Optional<List<String>> resourceIdsOptional,
      Optional<String> resourceName,
      ModelDBResourceEnum.ModelDBServiceResourceTypes resourceTypes) {
    var resourceType =
        ResourceType.newBuilder().setModeldbServiceResourceType(resourceTypes).build();
    Resources.Builder resources =
        Resources.newBuilder()
            .setResourceType(resourceType)
            .setService(ServiceEnum.Service.MODELDB_SERVICE);

    if (!resourceIdsOptional.isEmpty() && resourceIdsOptional.isPresent()) {
      resources.addAllResourceIds(
          resourceIdsOptional.get().stream().map(String::valueOf).collect(Collectors.toSet()));
    }

    var builder = GetResources.newBuilder().setResources(resources.build());
    workspaceName.ifPresent(builder::setWorkspaceName);
    resourceName.ifPresent(builder::setResourceName);
    return FutureUtil.clientRequest(
            uac.getCollaboratorService().getResources(builder.build()), executor)
        .thenApply(GetResources.Response::getItemList, executor);
  }

  public InternalFuture<UserInfoPaginationDTO> getFuzzyUserInfoList(String usernameChar) {
    LOGGER.trace("usernameChar : {}", usernameChar);
    if (usernameChar.isEmpty()) {
      var paginationDTO = new UserInfoPaginationDTO();
      paginationDTO.setUserInfoList(Collections.emptyList());
      paginationDTO.setTotalRecords(0L);
      return InternalFuture.completedInternalFuture(paginationDTO);
    }
    return FutureUtil.clientRequest(
            uac.getUACService()
                .getUsersFuzzy(GetUsersFuzzy.newBuilder().setUsername(usernameChar).build()),
            executor)
        .thenApply(
            response -> {
              LOGGER.trace(CommonMessages.AUTH_SERVICE_RES_RECEIVED_MSG);
              var paginationDTO = new UserInfoPaginationDTO();
              paginationDTO.setUserInfoList(response.getUserInfosList());
              paginationDTO.setTotalRecords(response.getTotalRecords());
              return paginationDTO;
            },
            executor);
  }

  public InternalFuture<Map<String, UserInfo>> getUserInfoFromAuthServer(
      Set<String> vertaIdList, Set<String> emailIdList, List<String> usernameList) {
    var getUserRequestBuilder = GetUsers.newBuilder().addAllUserIds(vertaIdList);
    if (emailIdList != null && !emailIdList.isEmpty()) {
      getUserRequestBuilder.addAllEmails(emailIdList);
    }
    if (usernameList != null && !usernameList.isEmpty()) {
      getUserRequestBuilder.addAllUsernames(usernameList);
    }
    LOGGER.trace("vertaIdList : {}", vertaIdList);
    LOGGER.trace("email Id List : {}", emailIdList);
    LOGGER.trace("username Id List : {}", usernameList);
    // Get the user info from the Context
    return FutureUtil.clientRequest(
            uac.getUACService().getUsers(getUserRequestBuilder.build()), executor)
        .thenApply(
            response -> {
              LOGGER.trace(CommonMessages.AUTH_SERVICE_RES_RECEIVED_MSG);
              List<UserInfo> userInfoList = response.getUserInfosList();

              Map<String, UserInfo> useInfoMap = new HashMap<>();
              for (UserInfo userInfo : userInfoList) {
                useInfoMap.put(userInfo.getVertaInfo().getUserId(), userInfo);
              }
              return useInfoMap;
            },
            executor);
  }

  public InternalFuture<GetResourcesResponseItem> getEntityResource(
      String entityId, ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    return getResourceItemsForWorkspace(
            Optional.empty(),
            Optional.of(Collections.singletonList(entityId)),
            Optional.empty(),
            modelDBServiceResourceTypes)
        .thenApply(
            responseItems -> {
              if (responseItems.size() > 1) {
                var mdbServiceTypeName = modelDBServiceResourceTypes.name();
                LOGGER.warn(
                    "Role service returned {}"
                        + " resource response items fetching {} resource, but only expected 1. ID: {}",
                    responseItems.size(),
                    mdbServiceTypeName,
                    entityId);
              }
              Optional<GetResourcesResponseItem> responseItem = responseItems.stream().findFirst();
              if (responseItem.isPresent()) {
                return responseItem.get();
              } else {
                StringBuilder errorMessage =
                    new StringBuilder("Failed to locate ")
                        .append(modelDBServiceResourceTypes.name())
                        .append(" resources in UAC for ")
                        .append(modelDBServiceResourceTypes.name())
                        .append(" ID ")
                        .append(entityId);
                throw new NotFoundException(errorMessage.toString());
              }
            },
            executor);
  }
}
