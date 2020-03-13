package ai.verta.modeldb.versioning.blob.container;

import static ai.verta.modeldb.versioning.blob.factory.BlobFactory.DOCKER_ENVIRONMENT_BLOB;
import static ai.verta.modeldb.versioning.blob.factory.BlobFactory.PYTHON_ENVIRONMENT_BLOB;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.environment.DockerEnvironmentBlobEntity;
import ai.verta.modeldb.entities.environment.EnvironmentBlobEntity;
import ai.verta.modeldb.entities.environment.EnvironmentCommandLineEntity;
import ai.verta.modeldb.entities.environment.EnvironmentVariablesEntity;
import ai.verta.modeldb.entities.environment.PythonEnvironmentBlobEntity;
import ai.verta.modeldb.entities.environment.PythonEnvironmentRequirementBlobEntity;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.DockerEnvironmentBlob;
import ai.verta.modeldb.versioning.EnvironmentBlob;
import ai.verta.modeldb.versioning.EnvironmentVariablesBlob;
import ai.verta.modeldb.versioning.FileHasher;
import ai.verta.modeldb.versioning.PythonEnvironmentBlob;
import ai.verta.modeldb.versioning.PythonRequirementEnvironmentBlob;
import ai.verta.modeldb.versioning.TreeElem;
import ai.verta.modeldb.versioning.VersionEnvironmentBlob;
import io.grpc.Status.Code;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.hibernate.Session;

public class EnvironmentContainer extends BlobContainer {

  public static final int PYTHON_ENV_TYPE = 1;
  public static final int DOCKER_ENV_TYPE = 2;
  private final EnvironmentBlob environment;

  public EnvironmentContainer(BlobExpanded blobExpanded) {
    super(blobExpanded);
    environment = blobExpanded.getBlob().getEnvironment();
  }

  class PythonRequirementKey {

    private final PythonRequirementEnvironmentBlob requirement;
    private final boolean isRequirement;

    public PythonRequirementKey(
        PythonRequirementEnvironmentBlob requirement, boolean isRequirement) {
      this.requirement = requirement;
      this.isRequirement = isRequirement;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      PythonRequirementKey that = (PythonRequirementKey) o;
      return Objects.equals(requirement.getLibrary(), that.requirement.getLibrary())
          && Objects.equals(requirement.getConstraint(), that.requirement.getConstraint())
          && Objects.equals(isRequirement, that.isRequirement);
    }

    @Override
    public int hashCode() {
      return Objects.hash(requirement.getLibrary(), requirement.getConstraint(), isRequirement);
    }
  }

  @Override
  public void validate() throws ModelDBException {
    Set<String> variableNames = new HashSet<>();
    for (EnvironmentVariablesBlob blob : environment.getEnvironmentVariablesList()) {
      validateEnvironmentVariableName(blob.getName());
      variableNames.add(blob.getName());
    }
    if (variableNames.size() != environment.getEnvironmentVariablesCount()) {
      throw new ModelDBException("There are recurring variables", Code.INVALID_ARGUMENT);
    }
    switch (environment.getContentCase()) {
      case DOCKER:
        if (environment.getDocker().getRepository().isEmpty()) {
          throw new ModelDBException(
              "Environment repository path should not be empty", Code.INVALID_ARGUMENT);
        }
        break;
      case PYTHON:
        final PythonEnvironmentBlob python = environment.getPython();
        Set<PythonRequirementKey> pythonRequirementKeys = new HashSet<>();
        for (PythonRequirementEnvironmentBlob requirement : python.getRequirementsList()) {
          if (requirement.getLibrary().isEmpty()) {
            throw new ModelDBException(
                "Requirement library name should not be empty", Code.INVALID_ARGUMENT);
          }
          pythonRequirementKeys.add(new PythonRequirementKey(requirement, true));
        }
        if (pythonRequirementKeys.size() != python.getRequirementsCount()) {
          throw new ModelDBException("There are recurring requirements", Code.INVALID_ARGUMENT);
        }
        for (PythonRequirementEnvironmentBlob constraint : python.getConstraintsList()) {
          if (constraint.getLibrary().isEmpty()) {
            throw new ModelDBException(
                "Constraint library name should not be empty", Code.INVALID_ARGUMENT);
          }
          pythonRequirementKeys.add(new PythonRequirementKey(constraint, false));
        }
        if (pythonRequirementKeys.size()
            != python.getRequirementsCount() + python.getConstraintsCount()) {
          throw new ModelDBException("There are recurring constraints", Code.INVALID_ARGUMENT);
        }
        break;
      default:
        throw new ModelDBException("Blob unknown type", Code.INVALID_ARGUMENT);
    }
  }

  @Override
  public void process(Session session, TreeElem rootTree, FileHasher fileHasher)
      throws NoSuchAlgorithmException, ModelDBException {
    EnvironmentBlobEntity environmentBlobEntity = new EnvironmentBlobEntity();
    String blobHash = computeSHA(environment);

    String blobType;
    List<Object> entities = new LinkedList<>();
    entities.add(environmentBlobEntity);
    switch (environment.getContentCase()) {
      case PYTHON:
        blobType = PYTHON_ENVIRONMENT_BLOB;
        PythonEnvironmentBlob python = environment.getPython();
        final String pythonBlobHash = computeSHA(python);
        environmentBlobEntity.setBlob_hash(FileHasher.getSha((blobHash + pythonBlobHash)));
        environmentBlobEntity.setEnvironment_type(PYTHON_ENV_TYPE);
        PythonEnvironmentBlobEntity pythonEnvironmentBlobEntity = new PythonEnvironmentBlobEntity();
        environmentBlobEntity.setPythonEnvironmentBlobEntity(pythonEnvironmentBlobEntity);
        pythonEnvironmentBlobEntity.setBlob_hash(pythonBlobHash);
        final VersionEnvironmentBlob version = python.getVersion();
        pythonEnvironmentBlobEntity.setMajor(version.getMajor());
        pythonEnvironmentBlobEntity.setMinor(version.getMinor());
        pythonEnvironmentBlobEntity.setPatch(version.getPatch());
        entities.add(pythonEnvironmentBlobEntity);
        for (PythonRequirementEnvironmentBlob pythonRequirementEnvironmentBlob :
            python.getRequirementsList()) {
          PythonEnvironmentRequirementBlobEntity pythonEnvironmentRequirementBlobEntity =
              new PythonEnvironmentRequirementBlobEntity(
                  pythonRequirementEnvironmentBlob, pythonEnvironmentBlobEntity, true);
          entities.add(pythonEnvironmentRequirementBlobEntity);
        }
        for (PythonRequirementEnvironmentBlob pythonRequirementEnvironmentBlob :
            python.getConstraintsList()) {
          PythonEnvironmentRequirementBlobEntity pythonEnvironmentRequirementBlobEntity =
              new PythonEnvironmentRequirementBlobEntity(
                  pythonRequirementEnvironmentBlob, pythonEnvironmentBlobEntity, false);
          entities.add(pythonEnvironmentRequirementBlobEntity);
        }
        break;
      case DOCKER:
        blobType = DOCKER_ENVIRONMENT_BLOB;
        DockerEnvironmentBlob docker = environment.getDocker();
        final String dockerBlobHash = computeSHA(docker);
        environmentBlobEntity.setBlob_hash(FileHasher.getSha((blobHash + dockerBlobHash)));
        environmentBlobEntity.setEnvironment_type(DOCKER_ENV_TYPE);
        DockerEnvironmentBlobEntity dockerEnvironmentBlobEntity =
            new DockerEnvironmentBlobEntity(dockerBlobHash, environment.getDocker());
        environmentBlobEntity.setDockerEnvironmentBlobEntity(dockerEnvironmentBlobEntity);
        entities.add(dockerEnvironmentBlobEntity);
        break;
      default:
        throw new ModelDBException("Blob unknown type", Code.INTERNAL);
    }
    for (EnvironmentVariablesBlob environmentVariablesBlob :
        environment.getEnvironmentVariablesList()) {
      EnvironmentVariablesEntity environmentVariablesBlobEntity =
          new EnvironmentVariablesEntity(environmentVariablesBlob);
      environmentVariablesBlobEntity.setEnvironmentBlobEntity(environmentBlobEntity);
      entities.add(environmentVariablesBlobEntity);
    }
    int count = 0;
    for (String command : environment.getCommandLineList()) {
      EnvironmentCommandLineEntity environmentCommandLineEntity =
          new EnvironmentCommandLineEntity(count++, command);
      environmentCommandLineEntity.setEnvironmentBlobEntity(environmentBlobEntity);
      entities.add(environmentCommandLineEntity);
    }
    for (Object entity : entities) {
      session.saveOrUpdate(entity);
    }
    final List<String> locationList = getLocationList();

    rootTree.push(locationList, environmentBlobEntity.getBlob_hash(), blobType);
  }

  private static final String PATTERN = "[a-zA-Z0-9_-]+";

  private void validateEnvironmentVariableName(String name) throws ModelDBException {
    if (!Pattern.compile(PATTERN).matcher(name).matches()) {
      throw new ModelDBException(
          "Environment variable name: "
              + name
              + " should be not empty, should contain only alphanumeric or underscore",
          Code.INVALID_ARGUMENT);
    }
  }

  private String computeSHA(EnvironmentBlob blob) throws NoSuchAlgorithmException {
    StringBuilder sb = new StringBuilder();
    sb.append("env:");
    for (EnvironmentVariablesBlob environmentVariablesBlob : blob.getEnvironmentVariablesList()) {
      sb.append(":name:")
          .append(environmentVariablesBlob.getName())
          .append("value:")
          .append(environmentVariablesBlob.getValue());
    }
    sb.append(":command_line:");
    for (String commandLine : blob.getCommandLineList()) {
      sb.append(":command:").append(commandLine);
    }
    return FileHasher.getSha(sb.toString());
  }

  private String computeSHA(PythonEnvironmentBlob blob) throws NoSuchAlgorithmException {
    StringBuilder sb = new StringBuilder();
    final VersionEnvironmentBlob version = blob.getVersion();
    sb.append("python:");
    appendVersion(sb, version);
    sb.append(":requirements:");
    for (PythonRequirementEnvironmentBlob pythonRequirementEnvironmentBlob :
        blob.getRequirementsList()) {
      sb.append(":library:")
          .append(pythonRequirementEnvironmentBlob.getLibrary())
          .append(":constraint:")
          .append(pythonRequirementEnvironmentBlob.getConstraint());
      appendVersion(sb, pythonRequirementEnvironmentBlob.getVersion());
    }
    sb.append(":constraints:");
    for (PythonRequirementEnvironmentBlob pythonConstraintEnvironmentBlob :
        blob.getConstraintsList()) {
      sb.append(":library:")
          .append(pythonConstraintEnvironmentBlob.getLibrary())
          .append(":constraint:")
          .append(pythonConstraintEnvironmentBlob.getConstraint());
      appendVersion(sb, pythonConstraintEnvironmentBlob.getVersion());
    }
    return FileHasher.getSha(sb.toString());
  }

  private String computeSHA(DockerEnvironmentBlob blob) throws NoSuchAlgorithmException {
    StringBuilder sb = new StringBuilder();
    sb.append("docker:repository")
        .append(blob.getRepository())
        .append(":sha:")
        .append(blob.getSha())
        .append(":tag:")
        .append(blob.getTag());
    return FileHasher.getSha(sb.toString());
  }

  private void appendVersion(StringBuilder sb, VersionEnvironmentBlob version) {
    sb.append("version:")
        .append(":major:")
        .append(version.getMajor())
        .append(":minor")
        .append(version.getMinor())
        .append(":patch")
        .append(version.getPatch());
  }
}
