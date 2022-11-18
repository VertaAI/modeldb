package ai.verta.modeldb.blobs;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model.*;
import ai.verta.modeldb.versioning.blob.diff.ProtoType;
import ai.verta.modeldb.versioning.blob.visitors.Visitor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Utils {
  public static <T extends ProtoType> T enforceOneof(T b) throws ModelDBException {
    Visitor v =
        new Visitor() {
          @Override
          public AutogenBlob postVisitAutogenBlob(AutogenBlob blob) throws ModelDBException {
            if (blob == null) return null;
            AutogenBlob other = new AutogenBlob();
            if (blob.getDataset() != null) return other.setDataset(blob.getDataset());
            if (blob.getConfig() != null) return other.setConfig(blob.getConfig());
            if (blob.getCode() != null) return other.setCode(blob.getCode());
            if (blob.getEnvironment() != null) return other.setEnvironment(blob.getEnvironment());
            return super.postVisitAutogenBlob(blob);
          }

          @Override
          public AutogenBlobDiff postVisitAutogenBlobDiff(AutogenBlobDiff blob)
              throws ModelDBException {
            if (blob == null) return null;
            AutogenBlobDiff other = new AutogenBlobDiff();
            if (blob.getDataset() != null) return other.setDataset(blob.getDataset());
            if (blob.getConfig() != null) return other.setConfig(blob.getConfig());
            if (blob.getCode() != null) return other.setCode(blob.getCode());
            if (blob.getEnvironment() != null) return other.setEnvironment(blob.getEnvironment());
            return super.postVisitAutogenBlobDiff(blob);
          }

          @Override
          public AutogenDatasetBlob postVisitAutogenDatasetBlob(AutogenDatasetBlob blob)
              throws ModelDBException {
            if (blob == null) return null;
            AutogenDatasetBlob other = new AutogenDatasetBlob();
            if (blob.getPath() != null) return other.setPath(blob.getPath());
            if (blob.getS3() != null) return other.setS3(blob.getS3());
            return super.postVisitAutogenDatasetBlob(blob);
          }

          @Override
          public AutogenDatasetDiff postVisitAutogenDatasetDiff(AutogenDatasetDiff blob)
              throws ModelDBException {
            if (blob == null) return null;
            AutogenDatasetDiff other = new AutogenDatasetDiff();
            if (blob.getPath() != null) return other.setPath(blob.getPath());
            if (blob.getS3() != null) return other.setS3(blob.getS3());
            return super.postVisitAutogenDatasetDiff(blob);
          }

          @Override
          public AutogenEnvironmentBlob postVisitAutogenEnvironmentBlob(AutogenEnvironmentBlob blob)
              throws ModelDBException {
            if (blob == null) return null;
            AutogenEnvironmentBlob other =
                new AutogenEnvironmentBlob()
                    .setCommandLine(blob.getCommandLine())
                    .setEnvironmentVariables(blob.getEnvironmentVariables());
            if (blob.getPython() != null) return other.setPython(blob.getPython());
            if (blob.getDocker() != null) return other.setDocker(blob.getDocker());
            return super.postVisitAutogenEnvironmentBlob(blob);
          }

          @Override
          public AutogenEnvironmentDiff postVisitAutogenEnvironmentDiff(AutogenEnvironmentDiff blob)
              throws ModelDBException {
            if (blob == null) return null;
            AutogenEnvironmentDiff other =
                new AutogenEnvironmentDiff()
                    .setCommandLine(blob.getCommandLine())
                    .setEnvironmentVariables(blob.getEnvironmentVariables());
            if (blob.getPython() != null) return other.setPython(blob.getPython());
            if (blob.getDocker() != null) return other.setDocker(blob.getDocker());
            return super.postVisitAutogenEnvironmentDiff(blob);
          }

          @Override
          public AutogenCodeBlob postVisitAutogenCodeBlob(AutogenCodeBlob blob)
              throws ModelDBException {
            if (blob == null) return null;
            AutogenCodeBlob other = new AutogenCodeBlob();
            if (blob.getNotebook() != null) return other.setNotebook(blob.getNotebook());
            if (blob.getGit() != null) return other.setGit(blob.getGit());
            return super.postVisitAutogenCodeBlob(blob);
          }

          @Override
          public AutogenCodeDiff postVisitAutogenCodeDiff(AutogenCodeDiff blob)
              throws ModelDBException {
            if (blob == null) return null;
            AutogenCodeDiff other = new AutogenCodeDiff();
            if (blob.getNotebook() != null) return other.setNotebook(blob.getNotebook());
            if (blob.getGit() != null) return other.setGit(blob.getGit());
            return super.postVisitAutogenCodeDiff(blob);
          }

          @Override
          public AutogenHyperparameterValuesConfigBlob
              postVisitAutogenHyperparameterValuesConfigBlob(
                  AutogenHyperparameterValuesConfigBlob blob) throws ModelDBException {
            if (blob == null) return null;
            AutogenHyperparameterValuesConfigBlob other =
                new AutogenHyperparameterValuesConfigBlob();
            if (blob.getFloatValue() != null) return other.setFloatValue(blob.getFloatValue());
            if (blob.getIntValue() != null) return other.setIntValue(blob.getIntValue());
            if (blob.getStringValue() != null) return other.setStringValue(blob.getStringValue());
            return super.postVisitAutogenHyperparameterValuesConfigBlob(blob);
          }

          @Override
          public AutogenHyperparameterSetConfigBlob postVisitAutogenHyperparameterSetConfigBlob(
              AutogenHyperparameterSetConfigBlob blob) throws ModelDBException {
            if (blob == null) return null;
            AutogenHyperparameterSetConfigBlob other =
                new AutogenHyperparameterSetConfigBlob().setName(blob.getName());
            if (blob.getDiscrete() != null) return other.setDiscrete(blob.getDiscrete());
            if (blob.getContinuous() != null) return other.setContinuous(blob.getContinuous());
            return super.postVisitAutogenHyperparameterSetConfigBlob(blob);
          }
        };

    return (T) removeEmpty(sanitize(v.genericPostVisitDeep(b)));
  }

  private static ProtoType sanitize(ProtoType b) {
    if (b == null) return b;
    if (b instanceof AutogenBlob) return sanitizeBlob((AutogenBlob) b);
    if (b instanceof AutogenConfigBlob) return sanitizeConfigBlob((AutogenConfigBlob) b);
    if (b instanceof AutogenDatasetBlob) return sanitizeDataset((AutogenDatasetBlob) b);
    if (b instanceof AutogenEnvironmentBlob)
      return sanitizeEnvironmentBlob((AutogenEnvironmentBlob) b);
    if (b instanceof AutogenPathDatasetBlob)
      return sanitizePathDatasetBlob((AutogenPathDatasetBlob) b);
    if (b instanceof AutogenPythonEnvironmentBlob)
      return sanitizePythonEnvironmentBlob((AutogenPythonEnvironmentBlob) b);
    if (b instanceof AutogenS3DatasetBlob) return sanitizeS3DatasetBlob((AutogenS3DatasetBlob) b);
    return b;
  }

  private static ProtoType sanitizeBlob(AutogenBlob b) {
    if (b == null) return b;
    b.setConfig(sanitizeConfigBlob(b.getConfig()));
    b.setDataset(sanitizeDataset(b.getDataset()));
    b.setEnvironment(sanitizeEnvironmentBlob(b.getEnvironment()));
    return b;
  }

  private static AutogenConfigBlob sanitizeConfigBlob(AutogenConfigBlob b) {
    if (b == null) return b;
    if (b.getHyperparameters() != null) {
      Map<String, AutogenHyperparameterConfigBlob> blobMap = new HashMap<>();
      for (AutogenHyperparameterConfigBlob blob : b.getHyperparameters()) {
        blobMap.put(blob.getName(), blob);
      }
      b.setHyperparameters(new LinkedList<AutogenHyperparameterConfigBlob>(blobMap.values()));
    }
    if (b.getHyperparameterSet() != null) {
      Map<String, AutogenHyperparameterSetConfigBlob> blobMap2 = new HashMap<>();
      for (AutogenHyperparameterSetConfigBlob blob : b.getHyperparameterSet()) {
        blobMap2.put(blob.getName(), blob);
      }
      b.setHyperparameterSet(new LinkedList<AutogenHyperparameterSetConfigBlob>(blobMap2.values()));
    }
    return b;
  }

  private static AutogenDatasetBlob sanitizeDataset(AutogenDatasetBlob b) {
    if (b == null) return b;
    b.setS3(sanitizeS3DatasetBlob(b.getS3()));
    b.setPath(sanitizePathDatasetBlob(b.getPath()));
    return b;
  }

  private static AutogenPathDatasetBlob sanitizePathDatasetBlob(AutogenPathDatasetBlob b) {
    if (b == null) return b;
    Map<String, AutogenPathDatasetComponentBlob> blobMap = new HashMap<>();
    for (AutogenPathDatasetComponentBlob blob : b.getComponents()) {
      blobMap.put(blob.getPath(), blob);
    }
    b.setComponents(new LinkedList<AutogenPathDatasetComponentBlob>(blobMap.values()));
    return b;
  }

  private static AutogenEnvironmentBlob sanitizeEnvironmentBlob(AutogenEnvironmentBlob b) {
    if (b == null) return b;
    b.setPython(sanitizePythonEnvironmentBlob(b.getPython()));
    if (b.getEnvironmentVariables() != null) {
      Map<String, AutogenEnvironmentVariablesBlob> blobMap = new HashMap<>();
      for (AutogenEnvironmentVariablesBlob blob : b.getEnvironmentVariables()) {
        blobMap.put(blob.getName(), blob);
      }
      b.setEnvironmentVariables(new LinkedList<AutogenEnvironmentVariablesBlob>(blobMap.values()));
    }
    return b;
  }

  private static AutogenPythonEnvironmentBlob sanitizePythonEnvironmentBlob(
      AutogenPythonEnvironmentBlob b) {
    if (b == null) return b;
    Map<String, AutogenPythonRequirementEnvironmentBlob> blobMap = new HashMap<>();
    if (b.getConstraints() != null) {
      for (AutogenPythonRequirementEnvironmentBlob blob : b.getConstraints()) {
        blobMap.put(
            blob.getLibrary() == null
                ? ""
                : blob.getLibrary() + blob.getConstraint() == null ? "" : blob.getConstraint(),
            blob);
      }
      b.setConstraints(new LinkedList<AutogenPythonRequirementEnvironmentBlob>(blobMap.values()));
    }
    if (b.getRequirements() != null) {
      blobMap = new HashMap<>();
      for (AutogenPythonRequirementEnvironmentBlob blob : b.getRequirements()) {
        blobMap.put(
            blob.getLibrary() == null
                ? ""
                : blob.getLibrary() + blob.getConstraint() == null ? "" : blob.getConstraint(),
            blob);
      }
      b.setRequirements(new LinkedList<AutogenPythonRequirementEnvironmentBlob>(blobMap.values()));
    }
    return b;
  }

  private static AutogenS3DatasetBlob sanitizeS3DatasetBlob(AutogenS3DatasetBlob b) {
    if (b == null) return b;
    Map<String, AutogenS3DatasetComponentBlob> blobMap = new HashMap<>();
    for (AutogenS3DatasetComponentBlob blob : b.getComponents()) {
      if (blob.getPath() == null) continue;
      blobMap.put(blob.getPath().getPath(), blob);
    }
    if (blobMap.isEmpty()) return null;
    b.setComponents(new LinkedList<AutogenS3DatasetComponentBlob>(blobMap.values()));
    return b;
  }

  public static <T> T removeEmpty(T obj) {
    if (obj instanceof ProtoType) {
      if (((ProtoType) obj).isEmpty()) return null;
    } else if (obj instanceof List) {
      Object ret =
          ((List) obj)
              .stream()
                  .map(x -> removeEmpty(x))
                  .filter(x -> x != null)
                  .collect(Collectors.toList());
      if (((List) ret).isEmpty()) return null;
      return (T) ret;
    }
    return obj;
  }
}