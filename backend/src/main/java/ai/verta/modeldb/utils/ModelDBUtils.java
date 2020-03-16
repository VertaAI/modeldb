package ai.verta.modeldb.utils;

import ai.verta.modeldb.Artifact;
import ai.verta.modeldb.CollaboratorUserInfo;
import ai.verta.modeldb.CollaboratorUserInfo.Builder;
import ai.verta.modeldb.GetHydratedProjects;
import ai.verta.modeldb.KeyValueQuery;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.OperatorEnum;
import ai.verta.modeldb.ValueTypeEnum;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.collaborator.CollaboratorBase;
import ai.verta.modeldb.collaborator.CollaboratorOrg;
import ai.verta.modeldb.collaborator.CollaboratorTeam;
import ai.verta.modeldb.collaborator.CollaboratorUser;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.uac.Action;
import ai.verta.uac.Actions;
import ai.verta.uac.EntitiesEnum.EntitiesTypes;
import ai.verta.uac.GetCollaboratorResponse;
import ai.verta.uac.ShareViaEnum;
import ai.verta.uac.UserInfo;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

public class ModelDBUtils {

  private static final Logger LOGGER = LogManager.getLogger(ModelDBUtils.class);

  private ModelDBUtils() {}

  public static Map<String, Object> readYamlProperties(String filePath) throws IOException {
    LOGGER.info("Reading File {} as YAML", filePath);
    InputStream inputStream = new FileInputStream(new File(filePath));
    Yaml yaml = new Yaml();
    @SuppressWarnings("unchecked")
    Map<String, Object> prop = (Map<String, Object>) yaml.load(inputStream);
    LOGGER.debug("YAML map {}", prop);
    return prop;
  }

  public static String getStringFromProtoObject(MessageOrBuilder object)
      throws InvalidProtocolBufferException {
    return JsonFormat.printer().preservingProtoFieldNames().print(object);
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
      Status status =
          Status.newBuilder().setCode(Code.INTERNAL_VALUE).setMessage(e.getMessage()).build();
      throw StatusProto.toStatusRuntimeException(status);
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
      LOGGER.warn(errorMessage);
      Status status =
          Status.newBuilder().setCode(Code.INVALID_ARGUMENT_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
    return entityName;
  }

  public static List<String> checkEntityTagsLength(List<String> tags) {
    for (String tag : tags) {
      if (tag.isEmpty()) {
        String errorMessage = "Invalid tag found, Tag shouldn't be empty";
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      } else if (tag.length() > ModelDBConstants.TAG_LENGTH) {
        String errorMessage =
            "Tag name can not be more than "
                + ModelDBConstants.TAG_LENGTH
                + " characters. Limit exceeded tag is: "
                + tag;
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(errorMessage)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
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
      List<GetCollaboratorResponse> entityCollaboratorList) {

    Set<String> vertaIdSet = new HashSet<>();
    Set<String> emailIdSet = new HashSet<>();

    for (GetCollaboratorResponse collaborator : entityCollaboratorList) {
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
      List<GetCollaboratorResponse> collaboratorList,
      Map<String, UserInfo> userInfoMap) {

    return getHydratedCollaboratorUserInfoByAuthz(
        authService, roleService, collaboratorList, userInfoMap);
  }

  private static List<CollaboratorUserInfo> getHydratedCollaboratorUserInfoByAuthz(
      AuthService authService,
      RoleService roleService,
      List<GetCollaboratorResponse> collaboratorList,
      Map<String, UserInfo> userInfoMap) {

    List<CollaboratorUserInfo> collaboratorUserInfos = new ArrayList<>();

    if (collaboratorList != null && !collaboratorList.isEmpty()) {
      for (GetCollaboratorResponse collaborator : collaboratorList) {

        try {
          CollaboratorBase collaborator1 = null;
          switch (collaborator.getAuthzEntityType()) {
            case USER:
              UserInfo userInfoValue = userInfoMap.get(collaborator.getVertaId());
              if (userInfoValue != null) {
                collaborator1 = new CollaboratorUser(authService, userInfoValue);
              } else {
                LOGGER.error("skipping " + collaborator.getVertaId() + " because it is not found");
              }
              break;
            case ORGANIZATION:
              collaborator1 = new CollaboratorOrg(collaborator.getVertaId(), roleService);
              break;
            case TEAM:
              collaborator1 = new CollaboratorTeam(collaborator.getVertaId(), roleService);
              break;
            default:
              Status status =
                  Status.newBuilder()
                      .setCode(Code.INTERNAL.getNumber())
                      .setMessage(ModelDBConstants.INTERNAL_ERROR)
                      .addDetails(Any.pack(GetHydratedProjects.Response.getDefaultInstance()))
                      .build();
              throw StatusProto.toStatusRuntimeException(status);
          }

          final Builder builder = CollaboratorUserInfo.newBuilder();
          if (collaborator1 != null) {
            collaborator1.addToResponse(builder);
            CollaboratorUserInfo collaboratorUserInfo =
                builder
                    .setCanDeploy(collaborator.getCanDeploy())
                    .setCollaboratorType(collaborator.getCollaboratorType())
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
            LOGGER.error("skipping " + collaborator.getVertaId() + " because it is not found");
          } else {
            LOGGER.error(ex.getMessage(), ex);
            throw ex;
          }
        }
      }
    }

    return collaboratorUserInfos;
  }

  /**
   * This is the common method for all to throw an exception to the UI
   *
   * @param errorMessage : error message throws by any service
   * @param errorCode : error code like Code.NOT_FOUND, Code.INTERNAL etc.
   * @param defaultResponse : Method reference to identify the error block
   */
  public static void logAndThrowError(String errorMessage, int errorCode, Any defaultResponse) {
    LOGGER.warn(errorMessage);
    Status status =
        Status.newBuilder()
            .setCode(errorCode)
            .setMessage(errorMessage)
            .addDetails(defaultResponse)
            .build();
    throw StatusProto.toStatusRuntimeException(status);
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
    return actions.values().stream()
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
        .getActionsList();
  }

  public static List<KeyValueQuery> getKeyValueQueriesByWorkspace(
      RoleService roleService, UserInfo userInfo, String workspaceName) {
    List<KeyValueQuery> workspaceQueries = new ArrayList<>();
    WorkspaceDTO workspaceDTO = roleService.getWorkspaceDTOByWorkspaceName(userInfo, workspaceName);

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
}
