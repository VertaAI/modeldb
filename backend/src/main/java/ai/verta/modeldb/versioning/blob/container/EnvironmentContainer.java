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
import java.util.LinkedList;
import java.util.List;
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

  @Override
  public void process(
      Session session, TreeElem rootTree, FileHasher fileHasher, Set<String> blobHashes)
      throws NoSuchAlgorithmException, ModelDBException {
    EnvironmentBlobEntity environmentBlobEntity = new EnvironmentBlobEntity();
    String blobHash = computeSHA(environment);

    String blobType;
    List<Object> entities = new LinkedList<>();

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
        if (!blobHashes.contains(pythonEnvironmentBlobEntity.getBlob_hash())) {
          entities.add(pythonEnvironmentBlobEntity);
          blobHashes.add(pythonEnvironmentBlobEntity.getBlob_hash());

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
        if (!blobHashes.contains(environmentBlobEntity.getBlob_hash())) {
          entities.add(dockerEnvironmentBlobEntity);
          blobHashes.add(environmentBlobEntity.getBlob_hash());
        }
        break;
      default:
        throw new ModelDBException("Blob unknown type", Code.INTERNAL);
    }
    if (!blobHashes.contains(environmentBlobEntity.getBlob_hash())) {
      entities.add(environmentBlobEntity);
      blobHashes.add(environmentBlobEntity.getBlob_hash());

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
