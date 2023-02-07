package ai.verta.modeldb.common.authservice;

import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.OperatorEnum;
import ai.verta.modeldb.common.CommonConstants.UserIdentifier;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.dto.UserInfoPaginationDTO;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureUtil;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.uac.Action;
import ai.verta.uac.Empty;
import ai.verta.uac.GetResources;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.GetSelfAllowedResources;
import ai.verta.uac.GetUser;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    var builder =
        GetResources.newBuilder().setResources(resources.build()).setWorkspaceId(workspaceId);
    return getResources(builder);
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

  public InternalFuture<Workspace> getWorkspaceById(long workspaceId) {
    return FutureUtil.clientRequest(
        uac.getWorkspaceService()
            .getWorkspaceById(GetWorkspaceById.newBuilder().setId(workspaceId).build()),
        executor);
  }

  public InternalFuture<Workspace> getWorkspaceByName(String workspaceName) {
    return FutureUtil.clientRequest(
        uac.getWorkspaceService()
            .getWorkspaceByName(GetWorkspaceByName.newBuilder().setName(workspaceName).build()),
        executor);
  }

  public InternalFuture<List<GetResourcesResponseItem>> getResourceItemsForLoginUserWorkspace(
      Optional<String> workspaceName,
      Long workspaceId,
      Optional<List<String>> resourceIdsOptional,
      ModelDBResourceEnum.ModelDBServiceResourceTypes resourceTypes) {
    var resourceType =
        ResourceType.newBuilder().setModeldbServiceResourceType(resourceTypes).build();
    Resources.Builder resources =
        Resources.newBuilder()
            .setResourceType(resourceType)
            .setService(ServiceEnum.Service.MODELDB_SERVICE);

    resourceIdsOptional.ifPresent(strings -> resources.addAllResourceIds(
        strings.stream().map(String::valueOf).collect(Collectors.toSet())));

    var builder = GetResources.newBuilder().setResources(resources.build());
    workspaceName.ifPresent(builder::setWorkspaceName);
    if (workspaceId != null) {
      builder.setWorkspaceId(workspaceId);
    }
    return FutureUtil.clientRequest(
            uac.getCollaboratorService().getResourcesSpecialPersonalWorkspace(builder.build()),
            executor)
        .thenApply(GetResources.Response::getItemList, executor);
  }

  public InternalFuture<List<GetResourcesResponseItem>> getResourceItemsForWorkspace(
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
    return getResources(builder);
  }

  private InternalFuture<List<GetResourcesResponseItem>> getResources(
      GetResources.Builder builder) {
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
      Set<String> vertaIdList,
      Set<String> emailIdList,
      List<String> usernameList,
      boolean isServiceUser) {
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

  public InternalFuture<UserInfo> getCurrentLoginUserInfo() {
    return FutureUtil.clientRequest(
        uac.getUACService().getCurrentUser(Empty.newBuilder().build()), executor);
  }

  public InternalFuture<UserInfo> getUserInfo(String vertaId, UserIdentifier vertaIdentifier) {
    GetUser getUserRequest;
    if (vertaIdentifier == UserIdentifier.EMAIL_ID) {
      getUserRequest = GetUser.newBuilder().setEmail(vertaId).build();
    } else if (vertaIdentifier == UserIdentifier.USER_NAME) {
      getUserRequest = GetUser.newBuilder().setUsername(vertaId).build();
    } else {
      getUserRequest = GetUser.newBuilder().setUserId(vertaId).build();
    }

    LOGGER.trace(CommonMessages.AUTH_SERVICE_REQ_SENT_MSG);
    // Get the user info from the Context
    return FutureUtil.clientRequest(uac.getUACService().getUser(getUserRequest), executor)
        .thenApply(
            userInfo -> {
              if (userInfo == null) {
                throw new NotFoundException("User not found with the provided metadata");
              }
              return userInfo;
            },
            executor);
  }

  public String getVertaIdFromUserInfo(UserInfo userInfo) {
    if (userInfo != null && !userInfo.getVertaInfo().getUserId().isEmpty()) {
      return userInfo.getVertaInfo().getUserId();
    }
    throw new NotFoundException("VertaId not found in userInfo");
  }

  public String getUsernameFromUserInfo(UserInfo userInfo) {
    if (userInfo != null && !userInfo.getVertaInfo().getUsername().isEmpty()) {
      return userInfo.getVertaInfo().getUsername();
    }
    throw new NotFoundException("Username not found in userInfo");
  }

  public Long getWorkspaceIdFromUserInfo(UserInfo userInfo) {
    if (userInfo != null && !userInfo.getVertaInfo().getWorkspaceId().isEmpty()) {
      return Long.parseLong(userInfo.getVertaInfo().getWorkspaceId());
    }
    throw new NotFoundException("WorkspaceId not found in userInfo");
  }

  public String getOwnerFilterClauseByPredicate(
      String alias,
      KeyValueQuery predicate,
      Map<String, Object> parametersMap,
      ModelDBServiceResourceTypes resourceTypes,
      boolean isHQL,
      boolean isResourceIdNumber) {
    var operator = predicate.getOperator();
    InternalFuture<List<String>> ownerIdsFuture;
    if (operator.equals(OperatorEnum.Operator.CONTAIN)
        || operator.equals(OperatorEnum.Operator.NOT_CONTAIN)) {
      ownerIdsFuture =
          getFuzzyUserInfoList(predicate.getValue().getStringValue())
              .thenApply(
                  userInfoPaginationDTO ->
                      userInfoPaginationDTO.getUserInfoList().stream()
                          .map(userInfo -> userInfo.getVertaInfo().getUserId())
                          .collect(Collectors.toList()),
                  executor);
    } else {
      var ownerIdsArrString = predicate.getValue().getStringValue();
      List<String> ownerIds = new ArrayList<>();
      if (operator.equals(OperatorEnum.Operator.IN)) {
        ownerIds = Arrays.asList(ownerIdsArrString.split(","));
      } else {
        ownerIds.add(ownerIdsArrString);
      }
      ownerIdsFuture = InternalFuture.completedInternalFuture(ownerIds);
    }

    var sqlFuture =
        ownerIdsFuture
            .thenCompose(
                userInfoList -> {
                  if (userInfoList != null && !userInfoList.isEmpty()) {
                    var resourceType =
                        ResourceType.newBuilder()
                            .setModeldbServiceResourceType(resourceTypes)
                            .build();
                    Resources.Builder resources =
                        Resources.newBuilder()
                            .setResourceType(resourceType)
                            .setService(ServiceEnum.Service.MODELDB_SERVICE);

                    var getResourcesBuilder =
                        GetResources.newBuilder().setResources(resources.build());
                    getResourcesBuilder.addAllOwnerIds(
                        userInfoList.stream().map(Long::parseLong).collect(Collectors.toSet()));
                    return getResources(getResourcesBuilder)
                        .thenApply(
                            accessibleAllWorkspaceItems ->
                                accessibleAllWorkspaceItems.stream()
                                    .map(GetResourcesResponseItem::getResourceId)
                                    .collect(Collectors.toSet()),
                            executor)
                        .thenCompose(
                            resourceIds -> {
                              final var valueBindingName =
                                  String.format("fuzzy_id_%d", new Date().getTime());
                              String inClause;
                              if (isHQL) {
                                inClause = ":" + valueBindingName;
                              } else {
                                inClause = "<" + valueBindingName + ">";
                              }

                              parametersMap.put(
                                  valueBindingName,
                                  isResourceIdNumber
                                      ? resourceIds.stream()
                                          .map(Long::parseLong)
                                          .collect(Collectors.toSet())
                                      : resourceIds);
                              String finalSql;
                              if (predicate.getOperator().equals(OperatorEnum.Operator.NOT_CONTAIN)
                                  || predicate.getOperator().equals(OperatorEnum.Operator.NE)) {
                                finalSql = String.format("%s.id NOT IN (%s)", alias, inClause);
                              } else {
                                finalSql = String.format("%s.id IN (%s)", alias, inClause);
                              }
                              return InternalFuture.completedInternalFuture(finalSql);
                            },
                            executor);
                  } else {
                    return InternalFuture.completedInternalFuture(
                        String.format("%s.id = '-1'", alias));
                  }
                },
                executor)
            .thenApply(sql -> sql, executor);

    try {
      return sqlFuture.blockAndGet();
    } catch (Exception ex) {
      throw new ModelDBException(ex);
    }
  }
}
