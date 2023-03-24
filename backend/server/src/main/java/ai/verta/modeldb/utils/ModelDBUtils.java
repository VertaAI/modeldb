package ai.verta.modeldb.utils;

import ai.verta.common.Artifact;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.App;
import ai.verta.modeldb.DatasetVisibilityEnum.DatasetVisibility;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ProjectVisibility;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.CommonUtils.RetryCallInterface;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.versioning.RepositoryVisibilityEnum.RepositoryVisibility;
import ai.verta.uac.*;
import com.google.protobuf.*;
import com.google.rpc.Code;
import io.grpc.StatusRuntimeException;
import java.math.BigInteger;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.exception.LockAcquisitionException;

public class ModelDBUtils {

  private static final Logger LOGGER = LogManager.getLogger(ModelDBUtils.class);

  private ModelDBUtils() {}

  private static String getMd5String(String inputString) {
    try {
      @SuppressWarnings("squid:S4790")
      var md = MessageDigest.getInstance("MD5");
      byte[] messageDigest = md.digest(inputString.getBytes());
      var no = new BigInteger(1, messageDigest);
      var hashtext = no.toString(16);
      var outputStringBuilder = new StringBuilder(hashtext);
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
      var first10Characters = projectShortName.substring(0, 10);
      var remainingCharacters = projectShortName.substring(11);
      var md5RemainingCharacters = ModelDBUtils.getMd5String(remainingCharacters);
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

  public static boolean needToRetry(Exception ex) {
    if (ex == null) {
      return false;
    }
    Throwable communicationsException = findCommunicationsFailedCause(ex);
    if (communicationsException.getCause() instanceof SocketException) {
      LOGGER.warn(communicationsException.getMessage());
      LOGGER.warn(
          "Detected communication exception of type {}",
          communicationsException.getCause().getClass());
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
    var rootCause = throwable;
    while (rootCause.getCause() != null
        && !(rootCause.getCause() instanceof SocketException
            || rootCause.getCause() instanceof LockAcquisitionException)) {
      rootCause = rootCause.getCause();
    }
    return rootCause;
  }

  public static void checkIfEntityAlreadyExists(
      MDBRoleService mdbRoleService,
      Workspace workspace,
      String name,
      List<String> projectEntityIds,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    List<GetResourcesResponseItem> responseItems =
        mdbRoleService.getResourceItems(
            workspace, new HashSet<>(projectEntityIds), modelDBServiceResourceTypes, false);
    for (GetResourcesResponseItem item : responseItems) {
      if (workspace != null && workspace.getId() == item.getWorkspaceId()) {
        // Throw error if it is an insert request and project with same name already exists
        LOGGER.info("{} with name {} already exists", modelDBServiceResourceTypes, name);
        throw new AlreadyExistsException(
            modelDBServiceResourceTypes + " already exists in database");
      }
    }
  }

  public static Set<String> filterWorkspaceOnlyAccessibleIds(
      MDBRoleService mdbRoleService,
      Set<String> accessibleAllWorkspaceProjectIds,
      String workspaceName,
      UserInfo userInfo,
      ModelDBServiceResourceTypes modelDBServiceResourceTypes) {
    var workspace = mdbRoleService.getWorkspaceByWorkspaceName(userInfo, workspaceName);
    List<GetResourcesResponseItem> items =
        mdbRoleService.getResourceItems(
            workspace, accessibleAllWorkspaceProjectIds, modelDBServiceResourceTypes, false);
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

  public static void validateEntityNameWithColonAndSlash(String name) throws ModelDBException {
    if (name != null
        && !name.isEmpty()
        && (name.contains(":") || name.contains("/") || name.contains("\\"))) {
      throw new ModelDBException(
          "Name can not contain ':' or '/' or '\\\\'", Code.INVALID_ARGUMENT);
    }
  }

  public static ResourceVisibility getResourceVisibility(
      Optional<Workspace> workspace, ProtocolMessageEnum visibility) {
    if (workspace.isEmpty()) {
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
        ex,
        retry,
        retryCallInterface,
        App.getInstance().mdbConfig.getGrpcServer().getRequestTimeout());
  }

  public static ModelDBServiceResourceTypes getModelDBServiceResourceTypesFromRepository(
      RepositoryEntity repository) {
    var modelDBServiceResourceTypes = ModelDBServiceResourceTypes.REPOSITORY;
    if (repository.isDataset()) {
      modelDBServiceResourceTypes = ModelDBServiceResourceTypes.DATASET;
    }
    return modelDBServiceResourceTypes;
  }
}
