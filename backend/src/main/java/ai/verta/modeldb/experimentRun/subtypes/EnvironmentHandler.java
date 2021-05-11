package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.EnvironmentBlob;
import ai.verta.modeldb.versioning.PythonEnvironmentBlob;
import ai.verta.modeldb.versioning.PythonRequirementEnvironmentBlob;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EnvironmentHandler {
  private static Logger LOGGER = LogManager.getLogger(EnvironmentHandler.class);

  private final Executor executor;
  private final FutureJdbi jdbi;
  private final String entityName;

  public EnvironmentHandler(Executor executor, FutureJdbi jdbi, String entityName) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.entityName = entityName;
  }

  public String getEnvironmentStringFromBlob(EnvironmentBlob runEnvironmentBlob)
      throws InvalidProtocolBufferException {
    if (runEnvironmentBlob != null) {
      EnvironmentBlob sortedEnvironmentBlob = sortPythonEnvironmentBlob(runEnvironmentBlob);
      return ModelDBUtils.getStringFromProtoObject(sortedEnvironmentBlob);
    }
    return null;
  }

  private EnvironmentBlob sortPythonEnvironmentBlob(EnvironmentBlob environmentBlob) {
    EnvironmentBlob.Builder builder = environmentBlob.toBuilder();
    if (builder.hasPython()) {
      PythonEnvironmentBlob.Builder pythonEnvironmentBlobBuilder = builder.getPython().toBuilder();

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
        handle ->
            handle
                .createUpdate(
                    "UPDATE experiment_run SET environment = :environment WHERE id = :runId")
                .bind("runId", runId)
                .bind("environment", getEnvironmentStringFromBlob(environmentBlob))
                .execute());
  }
}
