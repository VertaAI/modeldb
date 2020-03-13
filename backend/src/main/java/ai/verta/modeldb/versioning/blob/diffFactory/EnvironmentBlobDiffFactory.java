package ai.verta.modeldb.versioning.blob.diffFactory;

import static ai.verta.modeldb.versioning.blob.diffFactory.ConfigBlobDiffFactory.removeCommon;

import ai.verta.modeldb.versioning.BlobDiff;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.DockerEnvironmentDiff;
import ai.verta.modeldb.versioning.EnvironmentBlob;
import ai.verta.modeldb.versioning.EnvironmentDiff;
import ai.verta.modeldb.versioning.EnvironmentVariablesBlob;
import ai.verta.modeldb.versioning.PythonEnvironmentBlob;
import ai.verta.modeldb.versioning.PythonEnvironmentDiff;
import ai.verta.modeldb.versioning.PythonRequirementEnvironmentBlob;
import com.google.protobuf.ProtocolStringList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EnvironmentBlobDiffFactory extends BlobDiffFactory {

  public EnvironmentBlobDiffFactory(BlobExpanded blobExpanded) {
    super(blobExpanded);
  }

  @Override
  protected boolean subtypeEqual(BlobDiffFactory blobDiffFactory) {
    return blobDiffFactory
        .getBlobExpanded()
        .getBlob()
        .getEnvironment()
        .getContentCase()
        .equals(getBlobExpanded().getBlob().getEnvironment().getContentCase());
  }

  @Override
  protected void add(BlobDiff.Builder blobBuilder) {
    modify(blobBuilder, true);
  }

  @Override
  protected void delete(BlobDiff.Builder blobBuilder) {
    modify(blobBuilder, false);
  }

  private void modify(BlobDiff.Builder blobBuilder, boolean add) {
    final EnvironmentDiff.Builder environmentBuilder = EnvironmentDiff.newBuilder();
    final EnvironmentBlob environment = getBlobExpanded().getBlob().getEnvironment();
    List<EnvironmentVariablesBlob> environmentValiablesList =
        environment.getEnvironmentVariablesList();
    ProtocolStringList commandLineList = environment.getCommandLineList();
    final EnvironmentDiff environmentDiff = blobBuilder.getEnvironment();
    if (add) {
      if (environmentDiff.getEnvironmentVariablesACount() != 0
          || environmentDiff.getCommandLineACount() != 0) {
        Set<EnvironmentVariablesBlob> environmentVariablesBlobsA =
            new HashSet<>(environmentDiff.getEnvironmentVariablesAList());
        Set<EnvironmentVariablesBlob> environmentVariablesBlobsB =
            new HashSet<>(environmentValiablesList);
        removeCommon(environmentVariablesBlobsA, environmentVariablesBlobsB);
        environmentBuilder.addAllEnvironmentVariablesA(environmentVariablesBlobsA);
        environmentBuilder.addAllEnvironmentVariablesB(environmentVariablesBlobsB);
        if (!commandLineList.equals(environmentDiff.getCommandLineAList())) {
          environmentBuilder.addAllCommandLineA(environmentDiff.getCommandLineAList());
          environmentBuilder.addAllCommandLineB(commandLineList);
        }
      } else {
        environmentBuilder.addAllEnvironmentVariablesB(environmentValiablesList);
        environmentBuilder.addAllCommandLineB(commandLineList);
      }
    } else {
      environmentBuilder.addAllEnvironmentVariablesA(environmentValiablesList);
      environmentBuilder.addAllCommandLineA(commandLineList);
    }
    switch (environment.getContentCase()) {
      case PYTHON:
        PythonEnvironmentDiff.Builder pythonDiff;
        if (blobBuilder.hasEnvironment()) {
          pythonDiff = environmentDiff.getPython().toBuilder();
        } else {
          pythonDiff = PythonEnvironmentDiff.newBuilder();
        }
        final PythonEnvironmentBlob python = environment.getPython();
        if (add) {
          if (pythonDiff.hasA()) {
            Set<PythonRequirementEnvironmentBlob> pythonRequirementsBlobsA =
                new HashSet<>(pythonDiff.getA().getRequirementsList());
            Set<PythonRequirementEnvironmentBlob> pythonRequirementsBlobsB =
                new HashSet<>(python.getRequirementsList());
            removeCommon(pythonRequirementsBlobsA, pythonRequirementsBlobsB);
            Set<PythonRequirementEnvironmentBlob> pythonConstraintsBlobsA =
                new HashSet<>(pythonDiff.getA().getConstraintsList());
            Set<PythonRequirementEnvironmentBlob> pythonConstraintsBlobsB =
                new HashSet<>(python.getConstraintsList());
            removeCommon(pythonConstraintsBlobsA, pythonConstraintsBlobsB);
            final PythonEnvironmentBlob.Builder aBuilder = pythonDiff.getA().toBuilder();
            final PythonEnvironmentBlob.Builder bBuilder = python.toBuilder();
            pythonDiff.setA(
                aBuilder
                    .clearRequirements()
                    .clearConstraints()
                    .addAllRequirements(pythonRequirementsBlobsA)
                    .addAllConstraints(pythonConstraintsBlobsA));
            pythonDiff.setB(
                bBuilder
                    .clearRequirements()
                    .clearConstraints()
                    .addAllRequirements(pythonRequirementsBlobsB)
                    .addAllConstraints(pythonConstraintsBlobsB));
          } else {
            pythonDiff.setB(python);
          }
        } else {
          pythonDiff.setA(python);
        }

        environmentBuilder.setPython(pythonDiff).build();
        break;
      case DOCKER:
        DockerEnvironmentDiff.Builder dockerBuilder;
        if (blobBuilder.hasEnvironment()) {
          dockerBuilder = blobBuilder.getEnvironment().getDocker().toBuilder();
        } else {
          dockerBuilder = DockerEnvironmentDiff.newBuilder();
        }
        if (add) {
          dockerBuilder.setB(environment.getDocker());
        } else {
          dockerBuilder.setA(environment.getDocker());
        }

        environmentBuilder.setDocker(dockerBuilder).build();
        break;
    }
    blobBuilder.setEnvironment(environmentBuilder.build());
  }
}
