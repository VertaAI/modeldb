package ai.verta.modeldb.blobs;

import static ai.verta.modeldb.blobs.Utils.enforceOneof;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model.*;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

@RunWith(JUnitQuickcheck.class)
public class BlobProtoEquality {
  @Property
  public void protoEqualityAutogenBlob(AutogenBlob b) throws ModelDBException {
    AutogenBlob newb = enforceOneof(b);
    AutogenBlob other = newb == null ? null : AutogenBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenBlobDiff(AutogenBlobDiff b) throws ModelDBException {
    AutogenBlobDiff newb = enforceOneof(b);
    AutogenBlobDiff other = newb == null ? null : AutogenBlobDiff.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenCodeBlob(AutogenCodeBlob b) throws ModelDBException {
    AutogenCodeBlob newb = enforceOneof(b);
    AutogenCodeBlob other = newb == null ? null : AutogenCodeBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenCodeDiff(AutogenCodeDiff b) throws ModelDBException {
    AutogenCodeDiff newb = enforceOneof(b);
    AutogenCodeDiff other = newb == null ? null : AutogenCodeDiff.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenCommandLineEnvironmentDiff(AutogenCommandLineEnvironmentDiff b)
      throws ModelDBException {
    AutogenCommandLineEnvironmentDiff newb = enforceOneof(b);
    AutogenCommandLineEnvironmentDiff other =
        newb == null ? null : AutogenCommandLineEnvironmentDiff.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenConfigBlob(AutogenConfigBlob b) throws ModelDBException {
    AutogenConfigBlob newb = enforceOneof(b);
    AutogenConfigBlob other =
        newb == null ? null : AutogenConfigBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenConfigDiff(AutogenConfigDiff b) throws ModelDBException {
    AutogenConfigDiff newb = enforceOneof(b);
    AutogenConfigDiff other =
        newb == null ? null : AutogenConfigDiff.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenContinuousHyperparameterSetConfigBlob(
      AutogenContinuousHyperparameterSetConfigBlob b) throws ModelDBException {
    AutogenContinuousHyperparameterSetConfigBlob newb = enforceOneof(b);
    AutogenContinuousHyperparameterSetConfigBlob other =
        newb == null
            ? null
            : AutogenContinuousHyperparameterSetConfigBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenDatasetBlob(AutogenDatasetBlob b) throws ModelDBException {
    AutogenDatasetBlob newb = enforceOneof(b);
    AutogenDatasetBlob other =
        newb == null ? null : AutogenDatasetBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenDatasetDiff(AutogenDatasetDiff b) throws ModelDBException {
    AutogenDatasetDiff newb = enforceOneof(b);
    AutogenDatasetDiff other =
        newb == null ? null : AutogenDatasetDiff.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenDiscreteHyperparameterSetConfigBlob(
      AutogenDiscreteHyperparameterSetConfigBlob b) throws ModelDBException {
    AutogenDiscreteHyperparameterSetConfigBlob newb = enforceOneof(b);
    AutogenDiscreteHyperparameterSetConfigBlob other =
        newb == null
            ? null
            : AutogenDiscreteHyperparameterSetConfigBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenDockerEnvironmentBlob(AutogenDockerEnvironmentBlob b)
      throws ModelDBException {
    AutogenDockerEnvironmentBlob newb = enforceOneof(b);
    AutogenDockerEnvironmentBlob other =
        newb == null ? null : AutogenDockerEnvironmentBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenDockerEnvironmentDiff(AutogenDockerEnvironmentDiff b)
      throws ModelDBException {
    AutogenDockerEnvironmentDiff newb = enforceOneof(b);
    AutogenDockerEnvironmentDiff other =
        newb == null ? null : AutogenDockerEnvironmentDiff.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenEnvironmentBlob(AutogenEnvironmentBlob b)
      throws ModelDBException {
    AutogenEnvironmentBlob newb = enforceOneof(b);
    AutogenEnvironmentBlob other =
        newb == null ? null : AutogenEnvironmentBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenEnvironmentDiff(AutogenEnvironmentDiff b)
      throws ModelDBException {
    AutogenEnvironmentDiff newb = enforceOneof(b);
    AutogenEnvironmentDiff other =
        newb == null ? null : AutogenEnvironmentDiff.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenEnvironmentVariablesBlob(AutogenEnvironmentVariablesBlob b)
      throws ModelDBException {
    AutogenEnvironmentVariablesBlob newb = enforceOneof(b);
    AutogenEnvironmentVariablesBlob other =
        newb == null ? null : AutogenEnvironmentVariablesBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenEnvironmentVariablesDiff(AutogenEnvironmentVariablesDiff b)
      throws ModelDBException {
    AutogenEnvironmentVariablesDiff newb = enforceOneof(b);
    AutogenEnvironmentVariablesDiff other =
        newb == null ? null : AutogenEnvironmentVariablesDiff.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenGitCodeBlob(AutogenGitCodeBlob b) throws ModelDBException {
    AutogenGitCodeBlob newb = enforceOneof(b);
    AutogenGitCodeBlob other =
        newb == null ? null : AutogenGitCodeBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenGitCodeDiff(AutogenGitCodeDiff b) throws ModelDBException {
    AutogenGitCodeDiff newb = enforceOneof(b);
    AutogenGitCodeDiff other =
        newb == null ? null : AutogenGitCodeDiff.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenHyperparameterConfigBlob(AutogenHyperparameterConfigBlob b)
      throws ModelDBException {
    AutogenHyperparameterConfigBlob newb = enforceOneof(b);
    AutogenHyperparameterConfigBlob other =
        newb == null ? null : AutogenHyperparameterConfigBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenHyperparameterConfigDiff(AutogenHyperparameterConfigDiff b)
      throws ModelDBException {
    AutogenHyperparameterConfigDiff newb = enforceOneof(b);
    AutogenHyperparameterConfigDiff other =
        newb == null ? null : AutogenHyperparameterConfigDiff.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenHyperparameterSetConfigBlob(AutogenHyperparameterSetConfigBlob b)
      throws ModelDBException {
    AutogenHyperparameterSetConfigBlob newb = enforceOneof(b);
    AutogenHyperparameterSetConfigBlob other =
        newb == null ? null : AutogenHyperparameterSetConfigBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenHyperparameterSetConfigDiff(AutogenHyperparameterSetConfigDiff b)
      throws ModelDBException {
    AutogenHyperparameterSetConfigDiff newb = enforceOneof(b);
    AutogenHyperparameterSetConfigDiff other =
        newb == null ? null : AutogenHyperparameterSetConfigDiff.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenHyperparameterValuesConfigBlob(
      AutogenHyperparameterValuesConfigBlob b) throws ModelDBException {
    AutogenHyperparameterValuesConfigBlob newb = enforceOneof(b);
    AutogenHyperparameterValuesConfigBlob other =
        newb == null
            ? null
            : AutogenHyperparameterValuesConfigBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenNotebookCodeBlob(AutogenNotebookCodeBlob b)
      throws ModelDBException {
    AutogenNotebookCodeBlob newb = enforceOneof(b);
    AutogenNotebookCodeBlob other =
        newb == null ? null : AutogenNotebookCodeBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenNotebookCodeDiff(AutogenNotebookCodeDiff b)
      throws ModelDBException {
    AutogenNotebookCodeDiff newb = enforceOneof(b);
    AutogenNotebookCodeDiff other =
        newb == null ? null : AutogenNotebookCodeDiff.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenPathDatasetBlob(AutogenPathDatasetBlob b)
      throws ModelDBException {
    AutogenPathDatasetBlob newb = enforceOneof(b);
    AutogenPathDatasetBlob other =
        newb == null ? null : AutogenPathDatasetBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenPathDatasetComponentBlob(AutogenPathDatasetComponentBlob b)
      throws ModelDBException {
    AutogenPathDatasetComponentBlob newb = enforceOneof(b);
    AutogenPathDatasetComponentBlob other =
        newb == null ? null : AutogenPathDatasetComponentBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenPathDatasetComponentDiff(AutogenPathDatasetComponentDiff b)
      throws ModelDBException {
    AutogenPathDatasetComponentDiff newb = enforceOneof(b);
    AutogenPathDatasetComponentDiff other =
        newb == null ? null : AutogenPathDatasetComponentDiff.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenPathDatasetDiff(AutogenPathDatasetDiff b)
      throws ModelDBException {
    AutogenPathDatasetDiff newb = enforceOneof(b);
    AutogenPathDatasetDiff other =
        newb == null ? null : AutogenPathDatasetDiff.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenPythonEnvironmentBlob(AutogenPythonEnvironmentBlob b)
      throws ModelDBException {
    AutogenPythonEnvironmentBlob newb = enforceOneof(b);
    AutogenPythonEnvironmentBlob other =
        newb == null ? null : AutogenPythonEnvironmentBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenPythonEnvironmentDiff(AutogenPythonEnvironmentDiff b)
      throws ModelDBException {
    AutogenPythonEnvironmentDiff newb = enforceOneof(b);
    AutogenPythonEnvironmentDiff other =
        newb == null ? null : AutogenPythonEnvironmentDiff.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenPythonRequirementEnvironmentBlob(
      AutogenPythonRequirementEnvironmentBlob b) throws ModelDBException {
    AutogenPythonRequirementEnvironmentBlob newb = enforceOneof(b);
    AutogenPythonRequirementEnvironmentBlob other =
        newb == null
            ? null
            : AutogenPythonRequirementEnvironmentBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenPythonRequirementEnvironmentDiff(
      AutogenPythonRequirementEnvironmentDiff b) throws ModelDBException {
    AutogenPythonRequirementEnvironmentDiff newb = enforceOneof(b);
    AutogenPythonRequirementEnvironmentDiff other =
        newb == null
            ? null
            : AutogenPythonRequirementEnvironmentDiff.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenS3DatasetBlob(AutogenS3DatasetBlob b) throws ModelDBException {
    AutogenS3DatasetBlob newb = enforceOneof(b);
    AutogenS3DatasetBlob other =
        newb == null ? null : AutogenS3DatasetBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenS3DatasetComponentBlob(AutogenS3DatasetComponentBlob b)
      throws ModelDBException {
    AutogenS3DatasetComponentBlob newb = enforceOneof(b);
    AutogenS3DatasetComponentBlob other =
        newb == null ? null : AutogenS3DatasetComponentBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenS3DatasetComponentDiff(AutogenS3DatasetComponentDiff b)
      throws ModelDBException {
    AutogenS3DatasetComponentDiff newb = enforceOneof(b);
    AutogenS3DatasetComponentDiff other =
        newb == null ? null : AutogenS3DatasetComponentDiff.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenS3DatasetDiff(AutogenS3DatasetDiff b) throws ModelDBException {
    AutogenS3DatasetDiff newb = enforceOneof(b);
    AutogenS3DatasetDiff other =
        newb == null ? null : AutogenS3DatasetDiff.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenS3VersionIdDiff(AutogenS3VersionIdDiff b)
      throws ModelDBException {
    AutogenS3VersionIdDiff newb = enforceOneof(b);
    AutogenS3VersionIdDiff other =
        newb == null ? null : AutogenS3VersionIdDiff.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenVersionEnvironmentBlob(AutogenVersionEnvironmentBlob b)
      throws ModelDBException {
    AutogenVersionEnvironmentBlob newb = enforceOneof(b);
    AutogenVersionEnvironmentBlob other =
        newb == null ? null : AutogenVersionEnvironmentBlob.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }

  @Property
  public void protoEqualityAutogenVersionEnvironmentDiff(AutogenVersionEnvironmentDiff b)
      throws ModelDBException {
    AutogenVersionEnvironmentDiff newb = enforceOneof(b);
    AutogenVersionEnvironmentDiff other =
        newb == null ? null : AutogenVersionEnvironmentDiff.fromProto(newb.toProto().build());
    assertEquals(newb, other);
  }
}
