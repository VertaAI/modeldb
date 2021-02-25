package ai.verta.modeldb.utils;

import ai.verta.common.Artifact;
import ai.verta.common.EntitiesEnum.EntitiesTypes;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.OperatorEnum;
import ai.verta.common.ValueTypeEnum;
import ai.verta.common.WorkspaceTypeEnum.WorkspaceType;
import ai.verta.modeldb.CollaboratorUserInfo;
import ai.verta.modeldb.CollaboratorUserInfo.Builder;
import ai.verta.modeldb.DatasetVisibilityEnum.DatasetVisibility;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ProjectVisibility;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.CommonUtils.RetryCallInterface;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.collaborator.CollaboratorBase;
import ai.verta.modeldb.common.collaborator.CollaboratorOrg;
import ai.verta.modeldb.common.collaborator.CollaboratorTeam;
import ai.verta.modeldb.common.collaborator.CollaboratorUser;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.exceptions.*;
import ai.verta.modeldb.versioning.RepositoryVisibilityEnum.RepositoryVisibility;
import ai.verta.uac.*;
import com.amazonaws.AmazonServiceException;
import com.google.protobuf.*;
import com.google.protobuf.util.JsonFormat;
import com.google.rpc.Code;
import com.google.rpc.Status;
import com.mysql.cj.exceptions.CJCommunicationsException;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.exception.LockAcquisitionException;
import org.yaml.snakeyaml.Yaml;

public class ModelDBUtils {

  private static final Logger LOGGER = LogManager.getLogger(ModelDBUtils.class);
  private static final int STACKTRACE_LENGTH = 4;

  private ModelDBUtils() {}

  public static Map<String, Object> readYamlProperties(String filePath) throws IOException {
    LOGGER.info("Reading File {} as YAML", filePath);
    filePath = appendOptionalTelepresencePath(filePath);
    InputStream inputStream = new FileInputStream(new File(filePath));
    Yaml yaml = new Yaml();
    @SuppressWarnings("unchecked")
    Map<String, Object> prop = (Map<String, Object>) yaml.load(inputStream);
    return prop;
  }

  public static String appendOptionalTelepresencePath(String filePath) {
    String telepresenceRoot = System.getenv("TELEPRESENCE_ROOT");
    if (telepresenceRoot != null) {
      filePath = telepresenceRoot + filePath;
    }
    return filePath;
  }

  public static String getStringFromProtoObject(MessageOrBuilder object)
      throws InvalidProtocolBufferException {
    return JsonFormat.printer().preservingProtoFieldNames().print(object);
  }

  public static String getStringFromProtoObjectSilent(MessageOrBuilder object) {
    try {
      return getStringFromProtoObject(object);
    } catch (InvalidProtocolBufferException e) {
      throw new ModelDBException(e);
    }
  }

  public static Message.Builder getProtoObjectFromString(String jsonString, Message.Builder builder)
      throws InvalidProtocolBufferException {
    JsonFormat.parser().merge(jsonString, builder);
    return builder;
  }

  public static boolean isValidEmail(String email) {
    String emailRegex =
        "^[a-zA-Z0-9_+&*-]+(?:\\."
            + "[a-zA-Z0-9_+&*-]+)*@"
            + "(?:[a-zA-Z0-9-]+\\.)+[a-z"
            + "A-Z]{2,7}$";

    Pattern pat = Pattern.compile(emailRegex);
    if (email == null) return false;
    return pat.matcher(email).matches();
  }

  private static String getMd5String(String inputString) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] messageDigest = md.digest(inputString.getBytes());
      BigInteger no = new BigInteger(1, messageDigest);
      String hashtext = no.toString(16);
      StringBuilder outputStringBuilder = new StringBuilder(hashtext);
      while (outputStringBuilder.toString().length() < 32) {
        outputStringBuilder.append("0").append(outputStringBuilder.toString());
      }
      return outputStringBuilder.toString();
    }

    // For specifying wrong message digest algorithms
    catch (NoSuchAlgorithmException e) {
      throw new InternalErrorException(e.getMessage());
    }
  }

  public static String convertToProjectShortName(String projectName) {
    // replace all the special characters with `-` a part of alpha numeric characters, dots,
    // underscore and hyphen
    String projectShortName = projectName.replaceAll("[^a-zA-Z0-9_.-]+", "-");
    if (projectShortName.length() > ModelDBConstants.NAME_MAX_LENGTH) {
      String first10Characters = projectShortName.substring(0, 10);
      String remainingCharacters = projectShortName.substring(11);
      String md5RemainingCharacters = ModelDBUtils.getMd5String(remainingCharacters);
      projectShortName = first10Characters + md5RemainingCharacters;
    }
    return projectShortName;
  }

  public static String checkEntityNameLength(String entityName) {
    if (entityName != null && entityName.length() > ModelDBConstants.NAME_LENGTH) {
      String errorMessage =
          "Entity name can not be more than " + ModelDBConstants.NAME_LENGTH + " characters";
      throw new InvalidArgumentException(errorMessage);
    }
    return entityName;
  }

  public static List<String> checkEntityTagsLength(List<String> tags) {
    for (String tag : tags) {
      if (tag.isEmpty()) {
        String errorMessage = "Invalid tag found, Tag shouldn't be empty";
        throw new InvalidArgumentException(errorMessage);
      } else if (tag.length() > ModelDBConstants.TAG_LENGTH) {
        String errorMessage =
            "Tag name can not be more than "
                + ModelDBConstants.TAG_LENGTH
                + " characters. Limit exceeded tag is: "
                + tag;
        throw new InvalidArgumentException(errorMessage);
      }
    }
    return tags;
  }

  /**
   * VERTA <br>
   * Function to divide the list of collaborators by one's which can be resolved by email_id and
   * ones which can be resolved by user_id. <br>
   * The output just contains email_id or user_id as appropriate instead of entire collaborator
   * object.
   *
   * @param entityCollaboratorList : list of collaborators
   * @return map with keys `user_id` and `email_id` and values being a list of user_ids or
   *     email_ids.
   */
  public static Map<String, List<String>> getVertaIdOrEmailIdMapFromCollaborator(
      List<GetCollaboratorResponseItem> entityCollaboratorList) {

    Set<String> vertaIdSet = new HashSet<>();
    Set<String> emailIdSet = new HashSet<>();

    for (GetCollaboratorResponseItem collaborator : entityCollaboratorList) {
      if (collaborator.getAuthzEntityType() == EntitiesTypes.USER) {
        if (collaborator.getShareViaType().ordinal() == ShareViaEnum.EMAIL_ID_VALUE
            && ModelDBUtils.isValidEmail(collaborator.getVertaId())) {
          emailIdSet.add(collaborator.getVertaId());
        } else {
          vertaIdSet.add(collaborator.getVertaId());
        }
      }
    }

    Map<String, List<String>> vertaIdAndEmailIdMap = new HashMap<>();
    vertaIdAndEmailIdMap.put(ModelDBConstants.VERTA_ID, new ArrayList<>(vertaIdSet));
    vertaIdAndEmailIdMap.put(ModelDBConstants.EMAILID, new ArrayList<>(emailIdSet));

    return vertaIdAndEmailIdMap;
  }

  /**
   * VERTA<br>
   * Resolves the collaborators by looking them up in the map.
   *
   * @param collaboratorList : List of collaborators to be resolved from the map
   * @param userInfoMap : Map from vertaId to UserInfo, containing vertaIds related to the current
   *     higher RPC called.
   * @return List of CollaboratorUserInfo
   */
  public static List<CollaboratorUserInfo> getHydratedCollaboratorUserInfo(
      AuthService authService,
      RoleService roleService,
      List<GetCollaboratorResponseItem> collaboratorList,
      Map<String, UserInfo> userInfoMap) {

    return getHydratedCollaboratorUserInfoByAuthz(
        authService, roleService, collaboratorList, userInfoMap);
  }

  private static List<CollaboratorUserInfo> getHydratedCollaboratorUserInfoByAuthz(
      AuthService authService,
      RoleService roleService,
      List<GetCollaboratorResponseItem> collaboratorList,
      Map<String, UserInfo> userInfoMap) {

    List<CollaboratorUserInfo> collaboratorUserInfos = new ArrayList<>();

    if (collaboratorList != null && !collaboratorList.isEmpty()) {
      for (GetCollaboratorResponseItem collaborator : collaboratorList) {

        try {
          CollaboratorBase collaborator1 = null;
          switch (collaborator.getAuthzEntityType()) {
            case USER:
              UserInfo userInfoValue = userInfoMap.get(collaborator.getVertaId());
              if (userInfoValue != null) {
                collaborator1 = new CollaboratorUser(authService, userInfoValue);
              } else {
                LOGGER.info("skipping " + collaborator.getVertaId() + " because it is not found");
              }
              break;
            case ORGANIZATION:
              collaborator1 = new CollaboratorOrg(collaborator.getVertaId(), roleService);
              break;
            case TEAM:
              collaborator1 = new CollaboratorTeam(collaborator.getVertaId(), roleService);
              break;
            default:
              throw new InternalErrorException(ModelDBConstants.INTERNAL_ERROR);
          }

          final Builder builder = CollaboratorUserInfo.newBuilder();
          if (collaborator1 != null) {
            collaborator1.addToResponse(builder);
            CollaboratorUserInfo collaboratorUserInfo =
                builder
                    .setCanDeploy(collaborator.getPermission().getCanDeploy())
                    .setCollaboratorType(collaborator.getPermission().getCollaboratorType())
                    .build();
            collaboratorUserInfos.add(collaboratorUserInfo);
          }
        } catch (StatusRuntimeException ex) {
          if (ex.getStatus().getCode().value() == Code.PERMISSION_DENIED_VALUE) {
            LOGGER.info(
                "skipping "
                    + collaborator.getVertaId()
                    + " because the current user doesn't have access to it");
          } else if (ex.getStatus().getCode().value() == Code.NOT_FOUND_VALUE) {
            LOGGER.info("skipping " + collaborator.getVertaId() + " because it is not found");
          } else {
            LOGGER.debug(ex.getMessage(), ex);
            throw ex;
          }
        }
      }
    }

    return collaboratorUserInfos;
  }

  /**
   * Create artifact path using entityId and artifact key
   *
   * @param entityId : Project.id, Experiment.id, ExperimentRun.id
   * @param artifact : Project.artifacts.artifact, Experiment.artifacts.artifact
   * @return {@link String} : updated artifact path with entityID and artifact key.
   */
  private static String getPathForArtifact(String entityId, Artifact artifact) {
    return entityId + "/" + artifact.getKey();
  }

  /**
   * Update artifact path based on pathOnly flag
   *
   * @param entityId : Project.id, Experiment.id, ExperimentRun.id
   * @param artifacts : List of artifact which is comes from request
   * @return {@link List<Artifact>} : updated artifact
   */
  public static List<Artifact> getArtifactsWithUpdatedPath(
      String entityId, List<Artifact> artifacts) {
    List<Artifact> artifactList = new ArrayList<>();
    for (Artifact artifact : artifacts) {
      boolean pathOnly = artifact.getPathOnly();
      String path =
          artifact.getPath().isEmpty()
              ? getPathForArtifact(entityId, artifact)
              : artifact.getPath();
      artifact =
          artifact
              .toBuilder()
              .setKey(artifact.getKey())
              .setPath(path)
              .setPathOnly(pathOnly)
              .setArtifactType(artifact.getArtifactType())
              .setFilenameExtension(artifact.getFilenameExtension())
              .build();
      artifactList.add(artifact);
    }
    return artifactList;
  }

  public static List<Action> getActionsList(List<String> ids, Map<String, Actions> actions) {
    return new ArrayList<>(
        actions.values().stream()
            .findFirst()
            .orElseThrow(
                () -> {
                  Status status =
                      Status.newBuilder()
                          .setCode(Code.INTERNAL_VALUE)
                          .setMessage("Can't find allowed actions of current user for: " + ids)
                          .build();
                  return StatusProto.toStatusRuntimeException(status);
                })
            .getActionsList());
  }

  public static List<KeyValueQuery> getKeyValueQueriesByWorkspace(
      RoleService roleService, UserInfo userInfo, String workspaceName) {
    WorkspaceDTO workspaceDTO = roleService.getWorkspaceDTOByWorkspaceName(userInfo, workspaceName);
    return getKeyValueQueriesByWorkspaceDTO(workspaceDTO);
  }

  public static List<KeyValueQuery> getKeyValueQueriesByWorkspaceDTO(WorkspaceDTO workspaceDTO) {
    List<KeyValueQuery> workspaceQueries = new ArrayList<>();
    if (workspaceDTO != null && workspaceDTO.getWorkspaceId() != null) {
      KeyValueQuery workspacePredicates =
          KeyValueQuery.newBuilder()
              .setKey(ModelDBConstants.WORKSPACE)
              .setValue(Value.newBuilder().setStringValue(workspaceDTO.getWorkspaceId()).build())
              .setOperator(OperatorEnum.Operator.EQ)
              .setValueType(ValueTypeEnum.ValueType.STRING)
              .build();
      workspaceQueries.add(workspacePredicates);
      KeyValueQuery workspaceTypePredicates =
          KeyValueQuery.newBuilder()
              .setKey(ModelDBConstants.WORKSPACE_TYPE)
              .setValue(
                  Value.newBuilder()
                      .setNumberValue(workspaceDTO.getWorkspaceType().getNumber())
                      .build())
              .setOperator(OperatorEnum.Operator.EQ)
              .setValueType(ValueTypeEnum.ValueType.NUMBER)
              .build();
      workspaceQueries.add(workspaceTypePredicates);
    }
    return workspaceQueries;
  }

  public static void scheduleTask(
      TimerTask task, long initialDelay, long frequency, TimeUnit timeUnit) {
    // scheduling the timer instance
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    executor.scheduleAtFixedRate(task, initialDelay, frequency, timeUnit);
  }

  public static Throwable findRootCause(Throwable throwable) {
    if (throwable == null) {
      return null;
    }
    Throwable rootCause = throwable;
    while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
      rootCause = rootCause.getCause();
    }
    return rootCause;
  }

  public static StatusRuntimeException logError(Exception e) {
    return logError(e, null);
  }

  public static <T extends GeneratedMessageV3> StatusRuntimeException logError(
      Exception e, T defaultInstance) {
    Status status;
    StatusRuntimeException statusRuntimeException;
    if (e instanceof StatusRuntimeException) {
      statusRuntimeException = (StatusRuntimeException) e;
    } else {
      Throwable throwable = findRootCause(e);
      // Condition 'throwable != null' covered by below condition 'throwable instanceof
      // SocketException'
      StackTraceElement[] stack = e.getStackTrace();
      if (throwable instanceof SocketException) {
        String errorMessage = "Database Connection not found: ";
        LOGGER.info(errorMessage + "{}", e.getMessage());
        status =
            Status.newBuilder()
                .setCode(Code.UNAVAILABLE_VALUE)
                .setMessage(errorMessage + throwable.getMessage())
                .build();
      } else if (e instanceof LockAcquisitionException) {
        String errorMessage = "Encountered deadlock in database connection.";
        LOGGER.info(errorMessage + "{}", e.getMessage());
        status =
            Status.newBuilder()
                .setCode(Code.ABORTED_VALUE)
                .setMessage(errorMessage + throwable.getMessage())
                .build();
      } else if (e instanceof ModelDBException) {
        ModelDBException modelDBException = (ModelDBException) e;
        logBasedOnTheErrorCode(isClientError(modelDBException.getCode().value()), modelDBException);
        status =
            Status.newBuilder()
                .setCode(modelDBException.getCode().value())
                .setMessage(modelDBException.getMessage())
                .build();
      } else {
        LOGGER.error(
            "Stacktrace with {} elements for {} {}", stack.length, e.getClass(), e.getMessage());
        status =
            Status.newBuilder()
                .setCode(Code.INTERNAL_VALUE)
                .setMessage(ModelDBConstants.INTERNAL_ERROR)
                .build();
      }
      int n = 0;
      boolean isLongStack = stack.length > STACKTRACE_LENGTH;
      if (isLongStack) {
        for (; n < STACKTRACE_LENGTH + 1; ++n) {
          LOGGER.warn("{}: {}", n, stack[n].toString());
        }
      }
      for (; n < stack.length; ++n) {
        if (stack[n].getClassName().startsWith("ai.verta") || !isLongStack) {
          LOGGER.warn("{}: {}", n, stack[n].toString());
        }
      }
      statusRuntimeException = StatusProto.toStatusRuntimeException(status);
    }

    return statusRuntimeException;
  }

  public static <T extends GeneratedMessageV3> void observeError(
      StreamObserver<T> responseObserver, Exception e, T defaultInstance) {
    responseObserver.onError(logError(e, defaultInstance));
  }

  public static void logBasedOnTheErrorCode(boolean isClientError, Throwable e) {
    if (isClientError) {
      LOGGER.info("Exception occurred:{} {}", e.getClass(), e.getMessage());
    } else {
      LOGGER.warn("Exception occurred:{} {}", e.getClass(), e.getMessage());
    }
  }

  public static boolean isClientError(int grpcCodeValue) {
    switch (grpcCodeValue) {
      case 0: // OK : 200 OK
      case 1: // CANCELLED : 499 Client Closed Request
      case 3: // INVALID_ARGUMENT: 400 Bad Request
      case 5: // NOT_FOUND: 404 Not Found
      case 7: // PERMISSION_DENIED: 403 Forbidden
      case 6: // ALREADY_EXISTS: 409 Conflict
      case 8: // RESOURCE_EXHAUSTED: 429 Too Many Requests
      case 9: // FAILED_PRECONDITION: 400 Bad Request
      case 10: // ABORTED: 409 Conflict
      case 11: // OUT_OF_RANGE: 400 Bad Request
      case 16: // UNAUTHENTICATED: 401 Unauthorized
        return true;
      case 2: // UNKNOWN: 500 Internal Server Error
      case 4: // DEADLINE_EXCEEDED: 504 Gateway Timeout
      case 12: // UNIMPLEMENTED: 501 Not Implemented
      case 13: // INTERNAL: 500 Internal Server Error
      case 14: // UNAVAILABLE: 503 Service Unavailable
      case 15: // DATA_LOSS: 500 Internal Server Error
      default:
        return false;
    }
  }

  public static boolean needToRetry(Exception ex) {
    Throwable communicationsException = findCommunicationsFailedCause(ex);
    if ((communicationsException.getCause() instanceof CommunicationsException)
        || (communicationsException.getCause() instanceof SocketException)
        || (communicationsException.getCause() instanceof CJCommunicationsException)) {
      LOGGER.warn(communicationsException.getMessage());
      LOGGER.warn(
          "Detected communication exception of type {}",
          communicationsException.getCause().getClass());
      if (ModelDBHibernateUtil.checkDBConnection()) {
        LOGGER.info("Resetting session Factory");

        ModelDBHibernateUtil.resetSessionFactory();
        LOGGER.info("Resetted session Factory");
      } else {
        LOGGER.warn("DB could not be reached");
      }
      return true;
    } else if ((communicationsException.getCause() instanceof LockAcquisitionException)) {
      LOGGER.warn(communicationsException.getMessage());
      LOGGER.warn("Retrying since could not get lock");
      return true;
    }
    LOGGER.debug(
        "Detected exception of type {}, which is not categorized as retryable",
        ex,
        communicationsException);
    return false;
  }

  public static Throwable findCommunicationsFailedCause(Throwable throwable) {
    if (throwable == null) {
      return null;
    }
    Throwable rootCause = throwable;
    while (rootCause.getCause() != null
        && !(rootCause.getCause() instanceof CJCommunicationsException
            || rootCause.getCause() instanceof CommunicationsException
            || rootCause.getCause() instanceof SocketException
            || rootCause.getCause() instanceof LockAcquisitionException)) {
      rootCause = rootCause.getCause();
    }
    return rootCause;
  }

  /**
   * Throws an error if the workspace type is USER and the workspaceId and userID do not match. Is a
   * NO-OP if userinfo is null.
   */
  public static void checkPersonalWorkspace(
      UserInfo userInfo,
      WorkspaceType workspaceType,
      String workspaceId,
      String resourceNameString) {
    if (userInfo != null
        && workspaceType == WorkspaceType.USER
        && !workspaceId.equals(userInfo.getVertaInfo().getUserId())) {
      throw new PermissionDeniedException(
          "Creation of " + resourceNameString + " in other user's workspace is not permitted");
    }
  }

  public static void checkIfEntityAlreadyExists(
      RoleService roleService,
      Workspace workspace,
      String name,
      List<String> projectEntityIds,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    List<GetResourcesResponseItem> responseItems =
        roleService.getResourceItems(
            workspace, new HashSet<>(projectEntityIds), modelDBServiceResourceTypes);
    for (GetResourcesResponseItem item : responseItems) {
      if (workspace.getId() == item.getWorkspaceId()) {
        // Throw error if it is an insert request and project with same name already exists
        LOGGER.info("{} with name {} already exists", modelDBServiceResourceTypes, name);
        throw new AlreadyExistsException(
            modelDBServiceResourceTypes + " already exists in database");
      }
    }
  }

  public static Set<String> filterWorkspaceOnlyAccessibleIds(
      RoleService roleService,
      Set<String> accessibleAllWorkspaceProjectIds,
      String workspaceName,
      UserInfo userInfo,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    Workspace workspace = roleService.getWorkspaceByWorkspaceName(userInfo, workspaceName);
    List<GetResourcesResponseItem> items =
        roleService.getResourceItems(
            workspace, accessibleAllWorkspaceProjectIds, modelDBServiceResourceTypes);
    return items.stream().map(GetResourcesResponseItem::getResourceId).collect(Collectors.toSet());
  }

  public static String getLocationWithSlashOperator(List<String> locations) {
    return String.join("/", locations);
  }

  public static List<String> getLocationWithSplitSlashOperator(String locationsString) {
    return Arrays.asList(locationsString.split("/"));
  }

  public static String getJoinedLocation(List<String> location) {
    return String.join("#", location);
  }

  public static boolean isEnvSet(String envVar) {
    String envVarVal = System.getenv(envVar);
    return envVarVal != null && !envVarVal.isEmpty();
  }

  public static void validateEntityNameWithColonAndSlash(String name) throws ModelDBException {
    if (name != null
        && !name.isEmpty()
        && (name.contains(":") || name.contains("/") || name.contains("\\"))) {
      throw new ModelDBException(
          "Name can not contain ':' or '/' or '\\\\'", Code.INVALID_ARGUMENT);
    }
  }

  public static ModelDBException getInvalidFieldException(IllegalArgumentException ex) {
    if (ex.getCause() != null
        && ex.getCause().getMessage() != null
        && ex.getCause().getMessage().contains("could not resolve property: ")) {
      String invalidFieldName = ex.getCause().getMessage();
      invalidFieldName = invalidFieldName.substring("could not resolve property: ".length());
      invalidFieldName = invalidFieldName.substring(0, invalidFieldName.indexOf(" of:"));
      return new ModelDBException(
          "Invalid field found in the request : " + invalidFieldName, Code.INVALID_ARGUMENT);
    }
    throw ex;
  }

  public static void logAmazonServiceExceptionErrorCodes(Logger LOGGER, AmazonServiceException e) {
    LOGGER.info("Amazon Service Status Code: " + e.getStatusCode());
    LOGGER.info("Amazon Service Error Code: " + e.getErrorCode());
    LOGGER.info("Amazon Service Error Type: " + e.getErrorType());
    LOGGER.info("Amazon Service Error Message: " + e.getErrorMessage());
  }

  public static ResourceVisibility getResourceVisibility(
      Optional<Workspace> workspace, ProtocolMessageEnum visibility) {
    if (!workspace.isPresent()) {
      return ResourceVisibility.PRIVATE;
    }
    if (workspace.get().getInternalIdCase() == Workspace.InternalIdCase.ORG_ID) {
      if (visibility == ProjectVisibility.ORG_SCOPED_PUBLIC
          || visibility == RepositoryVisibility.ORG_SCOPED_PUBLIC
          || visibility == DatasetVisibility.ORG_SCOPED_PUBLIC) {
        return ResourceVisibility.ORG_DEFAULT;
      } else if (visibility == ProjectVisibility.PRIVATE
          || visibility == RepositoryVisibility.PRIVATE
          || visibility == DatasetVisibility.PRIVATE) {
        return ResourceVisibility.PRIVATE;
      }
      return ResourceVisibility.ORG_DEFAULT;
    }
    return ResourceVisibility.PRIVATE;
  }

  public static ProtocolMessageEnum getOldVisibility(
      ModelDBServiceResourceTypes resourceTypes, ResourceVisibility visibility) {
    switch (resourceTypes) {
      case PROJECT:
        if (visibility == ResourceVisibility.ORG_DEFAULT) {
          return ProjectVisibility.ORG_SCOPED_PUBLIC;
        } else if (visibility == ResourceVisibility.ORG_CUSTOM) {
          return ProjectVisibility.ORG_SCOPED_PUBLIC;
        } else {
          return ProjectVisibility.PRIVATE;
        }
      case REPOSITORY:
        if (visibility == ResourceVisibility.ORG_DEFAULT) {
          return RepositoryVisibility.ORG_SCOPED_PUBLIC;
        } else if (visibility == ResourceVisibility.ORG_CUSTOM) {
          return RepositoryVisibility.ORG_SCOPED_PUBLIC;
        } else {
          return RepositoryVisibility.PRIVATE;
        }
      case DATASET:
        if (visibility == ResourceVisibility.ORG_DEFAULT) {
          return DatasetVisibility.ORG_SCOPED_PUBLIC;
        } else if (visibility == ResourceVisibility.ORG_CUSTOM) {
          return DatasetVisibility.ORG_SCOPED_PUBLIC;
        } else {
          return DatasetVisibility.PRIVATE;
        }
      default:
        return null;
    }
  }

  public static Object retryOrThrowException(
      StatusRuntimeException ex, boolean retry, RetryCallInterface<?> retryCallInterface) {
    return CommonUtils.retryOrThrowException(
        ex, retry, retryCallInterface, Config.getInstance().grpcServer.requestTimeout);
  }

  public static ModelDBServiceResourceTypes getModelDBServiceResourceTypesFromRepository(
      RepositoryEntity repository) {
    ModelDBServiceResourceTypes modelDBServiceResourceTypes =
        ModelDBServiceResourceTypes.REPOSITORY;
    if (repository.isDataset()) {
      modelDBServiceResourceTypes = ModelDBServiceResourceTypes.DATASET;
    }
    return modelDBServiceResourceTypes;
  }

  public static String getStringFromProtoObjectSilent(MessageOrBuilder object) {
    try {
      return getStringFromProtoObject(object);
    } catch (InvalidProtocolBufferException e) {
      throw new ModelDBException(e);
    }
  }
}
