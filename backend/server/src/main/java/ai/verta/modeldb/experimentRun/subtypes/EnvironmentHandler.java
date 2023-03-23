package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.versioning.EnvironmentBlob;
import ai.verta.modeldb.versioning.PythonRequirementEnvironmentBlob;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EnvironmentHandler {

  private final FutureJdbi jdbi;

  public EnvironmentHandler(FutureJdbi jdbi) {
    this.jdbi = jdbi;
  }

  public String getEnvironmentStringFromBlob(EnvironmentBlob runEnvironmentBlob) {
    if (runEnvironmentBlob != null) {
      var sortedEnvironmentBlob = sortPythonEnvironmentBlob(runEnvironmentBlob);
      return CommonUtils.getStringFromProtoObject(sortedEnvironmentBlob);
    }
    return null;
  }

  private EnvironmentBlob sortPythonEnvironmentBlob(EnvironmentBlob environmentBlob) {
    var builder = environmentBlob.toBuilder();
    if (builder.hasPython()) {
      var pythonEnvironmentBlobBuilder = builder.getPython().toBuilder();

      // Compare requirementEnvironmentBlobs
      List<PythonRequirementEnvironmentBlob> requirementEnvironmentBlobs =
          new ArrayList<>(pythonEnvironmentBlobBuilder.getRequirementsList());
      requirementEnvironmentBlobs.sort(
          Comparator.comparing(PythonRequirementEnvironmentBlob::getLibrary));
      pythonEnvironmentBlobBuilder
          .clearRequirements()
          .addAllRequirements(requirementEnvironmentBlobs);

      // Compare
      List<PythonRequirementEnvironmentBlob> constraintsBlobs =
          new ArrayList<>(pythonEnvironmentBlobBuilder.getConstraintsList());
      constraintsBlobs.sort(Comparator.comparing(PythonRequirementEnvironmentBlob::getLibrary));
      pythonEnvironmentBlobBuilder.clearConstraints().addAllConstraints(constraintsBlobs);

      builder.setPython(pythonEnvironmentBlobBuilder.build());
    }
    return builder.build();
  }

  public InternalFuture<Void> logEnvironment(String runId, EnvironmentBlob environmentBlob) {
    return jdbi.useHandle(
        handle -> {
          try (var updateQuery =
              handle.createUpdate(
                  "UPDATE experiment_run SET environment = :environment WHERE id = :runId")) {
            updateQuery
                .bind("runId", runId)
                .bind("environment", getEnvironmentStringFromBlob(environmentBlob))
                .execute();
          }
        });
  }
}
