package ai.verta.modeldb.utils;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.dto.UserInfoPaginationDTO;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.Future;
import ai.verta.modeldb.common.futures.FutureExecutor;
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
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UACApisUtil {
  private static final Logger LOGGER = LogManager.getLogger(UACApisUtil.class);
  protected final FutureExecutor executor;
  protected final UAC uac;

  public UACApisUtil(FutureExecutor executor, UAC uac) {
    this.executor = executor;
    this.uac = uac;
  }

  public Future<List<Resources>> getAllowedEntitiesByResourceType(
      ModelDBActionEnum.ModelDBServiceActions action,
      ModelDBResourceEnum.ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    ListenableFuture<GetSelfAllowedResources.Response> listenableFuture =
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
                    .build());
    return Future.fromListenableFuture(listenableFuture)
        .thenCompose(a -> Future.of(a.getResourcesList()));
  }

  public Future<List<GetResourcesResponseItem>> getAllowedResourceItems(
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

    ListenableFuture<GetResources.Response> listenableFuture =
        uac.getCollaboratorService()
            .getResources(
                GetResources.newBuilder()
                    .setResources(resources.build())
                    .setWorkspaceId(workspaceId)
                    .build());
    return Future.fromListenableFuture(listenableFuture)
        .thenCompose(a -> Future.of(a.getItemList()));
  }

  public Future<List<String>> getAccessibleProjectIdsBasedOnWorkspace(
      String workspaceName, Optional<String> projectId) {
    var requestProjectIds = new ArrayList<String>();
    if (projectId.isPresent() && !projectId.get().isEmpty()) {
      requestProjectIds.add(projectId.get());
    }
    ListenableFuture<Workspace> listenableFuture =
        uac.getWorkspaceService()
            .getWorkspaceByName(GetWorkspaceByName.newBuilder().setName(workspaceName).build());
    return Future.fromListenableFuture(listenableFuture)
        .thenCompose(
            workspace -> {
              return getAllowedResourceItems(
                      Optional.of(requestProjectIds),
                      workspace.getId(),
                      ModelDBServiceResourceTypes.PROJECT)
                  .thenCompose(
                      getResourcesItems -> {
                        List<String> thing =
                            getResourcesItems.stream()
                                .map(GetResourcesResponseItem::getResourceId)
                                .collect(Collectors.toList());
                        return Future.of(thing);
                      });
            });
  }

  public Future<Workspace> getWorkspaceById(long workspaceId) {
    ListenableFuture<Workspace> listenableFuture =
        uac.getWorkspaceService()
            .getWorkspaceById(GetWorkspaceById.newBuilder().setId(workspaceId).build());
    return Future.fromListenableFuture(listenableFuture);
  }

  public Future<List<GetResourcesResponseItem>> getResourceItemsForLoginUserWorkspace(
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
    ListenableFuture<GetResources.Response> listenableFuture =
        uac.getCollaboratorService().getResourcesSpecialPersonalWorkspace(builder.build());
    return Future.fromListenableFuture(listenableFuture)
        .thenCompose(a -> Future.of(a.getItemList()));
  }

  public Future<List<GetResourcesResponseItem>> getResourceItemsForWorkspace(
      Optional<String> workspaceName,
      Optional<Collection<String>> resourceIdsOptional,
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
    return Future.fromListenableFuture(uac.getCollaboratorService().getResources(builder.build()))
        .thenCompose(a -> Future.of(a.getItemList()));
  }

  public Future<UserInfoPaginationDTO> getFuzzyUserInfoList(String usernameChar) {
    LOGGER.trace("usernameChar : {}", usernameChar);
    if (usernameChar.isEmpty()) {
      var paginationDTO = new UserInfoPaginationDTO();
      paginationDTO.setUserInfoList(Collections.emptyList());
      paginationDTO.setTotalRecords(0L);
      return Future.of(paginationDTO);
    }
    ListenableFuture<GetUsersFuzzy.Response> listenableFuture =
        uac.getUACService()
            .getUsersFuzzy(GetUsersFuzzy.newBuilder().setUsername(usernameChar).build());
    Future<GetUsersFuzzy.Response> responseFuture = Future.fromListenableFuture(listenableFuture);
    return responseFuture.thenCompose(
        t ->
            Future.of(
                ((Function<? super GetUsersFuzzy.Response, ? extends UserInfoPaginationDTO>)
                        response -> {
                          LOGGER.trace(CommonMessages.AUTH_SERVICE_RES_RECEIVED_MSG);
                          var paginationDTO = new UserInfoPaginationDTO();
                          paginationDTO.setUserInfoList(response.getUserInfosList());
                          paginationDTO.setTotalRecords(response.getTotalRecords());
                          return paginationDTO;
                        })
                    .apply(t)));
  }

  public Future<Map<String, UserInfo>> getUserInfoFromAuthServer(
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
    Future<GetUsers.Response> responseFuture =
        Future.fromListenableFuture(uac.getUACService().getUsers(getUserRequestBuilder.build()));
    return responseFuture.thenCompose(
        t ->
            Future.of(
                ((Function<? super GetUsers.Response, ? extends Map<String, UserInfo>>)
                        response -> {
                          LOGGER.trace(CommonMessages.AUTH_SERVICE_RES_RECEIVED_MSG);
                          List<UserInfo> userInfoList = response.getUserInfosList();

                          Map<String, UserInfo> useInfoMap = new HashMap<>();
                          for (UserInfo userInfo : userInfoList) {
                            useInfoMap.put(userInfo.getVertaInfo().getUserId(), userInfo);
                          }
                          return useInfoMap;
                        })
                    .apply(t)));
  }

  public Future<GetResourcesResponseItem> getEntityResource(
      String entityId, ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    Future<List<GetResourcesResponseItem>> listFuture =
        getResourceItemsForWorkspace(
            Optional.empty(),
            Optional.of(Collections.singletonList(entityId)),
            Optional.empty(),
            modelDBServiceResourceTypes);
    return listFuture.thenCompose(
        t ->
            Future.of(
                ((Function<
                            ? super List<GetResourcesResponseItem>,
                            ? extends GetResourcesResponseItem>)
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
                          Optional<GetResourcesResponseItem> responseItem =
                              responseItems.stream().findFirst();
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
                        })
                    .apply(t)));
  }
}
