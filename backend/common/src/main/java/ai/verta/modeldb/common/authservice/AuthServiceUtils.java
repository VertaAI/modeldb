package ai.verta.modeldb.common.authservice;

import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.CommonUtils.RetryCallInterface;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.dto.UserInfoPaginationDTO;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.uac.*;
import io.grpc.StatusRuntimeException;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuthServiceUtils implements AuthService {
  private final UAC uac;
  private final Integer timeout;
  private final boolean isPermissionV2;

  public AuthServiceUtils(UAC uac, Integer timeout, boolean isPermissionV2) {
    this.uac = uac;
    this.timeout = timeout;
    this.isPermissionV2 = isPermissionV2;
  }

  private static final Logger LOGGER = LogManager.getLogger(AuthServiceUtils.class);

  @Override
  public UserInfo getCurrentLoginUserInfo() {
    return getCurrentLoginUserInfo(true);
  }

  private UserInfo getCurrentLoginUserInfo(boolean retry) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      LOGGER.trace(CommonMessages.AUTH_SERVICE_REQ_SENT_MSG);
      var userInfo =
          authServiceChannel.getUacServiceBlockingStub().getCurrentUser(Empty.newBuilder().build());
      LOGGER.trace(CommonMessages.AUTH_SERVICE_RES_RECEIVED_MSG);

      if (userInfo == null || userInfo.getVertaInfo() == null) {
        throw new NotFoundException("Current user could not be resolved.");
      } else {
        return userInfo;
      }
    } catch (StatusRuntimeException ex) {
      return (UserInfo)
          CommonUtils.retryOrThrowException(ex, retry, this::getCurrentLoginUserInfo, timeout);
    }
  }

  @Override
  public UserInfo getUnsignedUser() {
    return getUnsignedUser(true);
  }

  private UserInfo getUnsignedUser(boolean retry) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      LOGGER.trace(CommonMessages.AUTH_SERVICE_REQ_SENT_MSG);
      var getUserRequest = GetUser.newBuilder().setUsername(CommonConstants.UNSIGNED_USER).build();
      // Get the user info by vertaId form the AuthService
      var userInfo = authServiceChannel.getUacServiceBlockingStub().getUser(getUserRequest);
      LOGGER.trace(CommonMessages.AUTH_SERVICE_RES_RECEIVED_MSG);

      if (userInfo == null || userInfo.getVertaInfo() == null) {
        throw new NotFoundException("Unsigned user not found with the provided metadata");
      } else {
        return userInfo;
      }
    } catch (StatusRuntimeException ex) {
      return (UserInfo)
          CommonUtils.retryOrThrowException(ex, retry, this::getUnsignedUser, timeout);
    }
  }

  @Override
  public UserInfo getUserInfo(String vertaId, CommonConstants.UserIdentifier vertaIdentifier) {
    return getUserInfo(true, vertaId, vertaIdentifier);
  }

  private UserInfo getUserInfo(
      boolean retry, String vertaId, CommonConstants.UserIdentifier vertaIdentifier) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      GetUser getUserRequest;
      if (vertaIdentifier == CommonConstants.UserIdentifier.EMAIL_ID) {
        getUserRequest = GetUser.newBuilder().setEmail(vertaId).build();
      } else if (vertaIdentifier == CommonConstants.UserIdentifier.USER_NAME) {
        getUserRequest = GetUser.newBuilder().setUsername(vertaId).build();
      } else {
        getUserRequest = GetUser.newBuilder().setUserId(vertaId).build();
      }

      LOGGER.trace(CommonMessages.AUTH_SERVICE_REQ_SENT_MSG);
      // Get the user info by vertaId form the AuthService
      var userInfo = authServiceChannel.getUacServiceBlockingStub().getUser(getUserRequest);
      LOGGER.trace(CommonMessages.AUTH_SERVICE_RES_RECEIVED_MSG);

      if (userInfo == null || userInfo.getVertaInfo() == null) {
        throw new NotFoundException("User not found with the provided metadata");
      } else {
        return userInfo;
      }
    } catch (StatusRuntimeException ex) {
      return (UserInfo)
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (RetryCallInterface<UserInfo>)
                  retry1 -> getUserInfo(retry1, vertaId, vertaIdentifier),
              timeout);
    }
  }

  /**
   * @param vertaIdList : vertaId list which is now deprecated
   * @param emailIdList : email id list
   * @param usernameList : username list
   * @return Map from verta_id to userInfo
   */
  @Override
  public Map<String, UserInfo> getUserInfoFromAuthServer(
      Set<String> vertaIdList,
      Set<String> emailIdList,
      List<String> usernameList,
      boolean isServiceUser) {
    return getUserInfoFromAuthServer(true, vertaIdList, emailIdList, usernameList, isServiceUser);
  }

  private Map<String, UserInfo> getUserInfoFromAuthServer(
      boolean retry,
      Set<String> vertaIdList,
      Set<String> emailIdList,
      List<String> usernameList,
      boolean isServiceUser) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
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
      var blockingStub =
          isServiceUser
              ? authServiceChannel.getUacServiceBlockingStubForServiceUser()
              : authServiceChannel.getUacServiceBlockingStub();
      var response = blockingStub.getUsers(getUserRequestBuilder.build());
      LOGGER.trace(CommonMessages.AUTH_SERVICE_RES_RECEIVED_MSG);
      List<UserInfo> userInfoList = response.getUserInfosList();

      Map<String, UserInfo> useInfoMap = new HashMap<>();
      for (UserInfo userInfo : userInfoList) {
        useInfoMap.put(userInfo.getVertaInfo().getUserId(), userInfo);
      }
      return useInfoMap;
    } catch (StatusRuntimeException ex) {
      return (Map<String, UserInfo>)
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (RetryCallInterface<Map<String, UserInfo>>)
                  retry1 ->
                      getUserInfoFromAuthServer(
                          retry1, vertaIdList, emailIdList, usernameList, isServiceUser),
              timeout);
    }
  }

  @Override
  public String getVertaIdFromUserInfo(UserInfo userInfo) {
    if (userInfo != null
        && userInfo.getVertaInfo() != null
        && !userInfo.getVertaInfo().getUserId().isEmpty()) {
      return userInfo.getVertaInfo().getUserId();
    }
    throw new NotFoundException("VertaId not found in userInfo");
  }

  @Override
  public String getUsernameFromUserInfo(UserInfo userInfo) {
    if (userInfo != null
        && userInfo.getVertaInfo() != null
        && !userInfo.getVertaInfo().getUsername().isEmpty()) {
      return userInfo.getVertaInfo().getUsername();
    }
    throw new NotFoundException("Username not found in userInfo");
  }

  @Override
  public Long getWorkspaceIdFromUserInfo(UserInfo userInfo) {
    if (isPermissionV2){
      if (userInfo != null && userInfo.getVertaInfo().getDefaultWorkspaceId() != 0) {
        return userInfo.getVertaInfo().getDefaultWorkspaceId();
      }
    } else {
      if (userInfo != null && !userInfo.getVertaInfo().getWorkspaceId().isEmpty()) {
        return Long.parseLong(userInfo.getVertaInfo().getWorkspaceId());
      }
    }
    throw new NotFoundException("WorkspaceId not found in userInfo");
  }

  @Override
  public boolean isCurrentUser(String vertaID) {
    return getVertaIdFromUserInfo(getCurrentLoginUserInfo()).equals(vertaID);
  }

  @Override
  public UserInfoPaginationDTO getFuzzyUserInfoList(String usernameChar) {
    return getFuzzyUserInfoList(true, usernameChar);
  }

  private UserInfoPaginationDTO getFuzzyUserInfoList(boolean retry, String usernameChar) {
    if (usernameChar.isEmpty()) {
      var paginationDTO = new UserInfoPaginationDTO();
      paginationDTO.setUserInfoList(Collections.emptyList());
      paginationDTO.setTotalRecords(0L);
      return paginationDTO;
    }
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      var getUserRequestBuilder = GetUsersFuzzy.newBuilder().setUsername(usernameChar);

      LOGGER.trace("usernameChar : {}", usernameChar);
      // Get the user info from the Context
      var response =
          authServiceChannel
              .getUacServiceBlockingStub()
              .getUsersFuzzy(getUserRequestBuilder.build());
      LOGGER.trace(CommonMessages.AUTH_SERVICE_RES_RECEIVED_MSG);

      var paginationDTO = new UserInfoPaginationDTO();
      paginationDTO.setUserInfoList(response.getUserInfosList());
      paginationDTO.setTotalRecords(response.getTotalRecords());
      return paginationDTO;
    } catch (StatusRuntimeException ex) {
      return (UserInfoPaginationDTO)
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<UserInfoPaginationDTO>)
                  retry1 -> getFuzzyUserInfoList(retry1, usernameChar),
              timeout);
    }
  }

  @Override
  public Workspace workspaceIdByName(boolean retry, String workspaceName) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      GetWorkspaceByName.Builder getWorkspaceByName =
          GetWorkspaceByName.newBuilder().setName(workspaceName);

      LOGGER.trace("workspace : {}", workspaceName);
      // Get the user info from the Context
      var workspace =
          authServiceChannel
              .getWorkspaceServiceBlockingStub()
              .getWorkspaceByName(getWorkspaceByName.build());
      LOGGER.trace(CommonMessages.AUTH_SERVICE_RES_RECEIVED_MSG);
      return workspace;
    } catch (StatusRuntimeException ex) {
      return (Workspace)
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (RetryCallInterface<Workspace>) retry1 -> workspaceIdByName(retry1, workspaceName),
              timeout);
    }
  }

  @Override
  public Workspace workspaceById(boolean retry, Long workspaceId) {
    try (var authServiceChannel = uac.getBlockingAuthServiceChannel()) {
      GetWorkspaceById.Builder getWorkspaceById = GetWorkspaceById.newBuilder().setId(workspaceId);

      LOGGER.trace("get workspaceById: ID : {}", workspaceId);
      // Get the user info from the Context
      var workspace =
          authServiceChannel
              .getWorkspaceServiceBlockingStub()
              .getWorkspaceById(getWorkspaceById.build());
      LOGGER.trace(CommonMessages.AUTH_SERVICE_RES_RECEIVED_MSG);
      return workspace;
    } catch (StatusRuntimeException ex) {
      return (Workspace)
          CommonUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<Workspace>)
                  retry1 -> workspaceById(retry1, workspaceId),
              timeout);
    }
  }
}
