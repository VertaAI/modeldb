package ai.verta.modeldb.project;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.dto.UserInfoPaginationDTO;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.uac.GetResources;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.GetUsers;
import ai.verta.uac.GetUsersFuzzy;
import ai.verta.uac.GetWorkspaceById;
import ai.verta.uac.ResourceType;
import ai.verta.uac.Resources;
import ai.verta.uac.ServiceEnum;
import ai.verta.uac.UserInfo;
import ai.verta.uac.Workspace;
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

  public InternalFuture<Workspace> getWorkspaceById(long workspaceId) {
    return FutureGrpc.ClientRequest(
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
    return FutureGrpc.ClientRequest(
            uac.getCollaboratorService().getResourcesSpecialPersonalWorkspace(builder.build()),
            executor)
        .thenApply(GetResources.Response::getItemList, executor);
  }

  public InternalFuture<List<GetResourcesResponseItem>> getResourceItemsForWorkspace(
      String workspaceName,
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
    builder.setWorkspaceName(workspaceName);
    resourceName.ifPresent(builder::setResourceName);
    return FutureGrpc.ClientRequest(
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
    return FutureGrpc.ClientRequest(
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
    return FutureGrpc.ClientRequest(
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
}
