package ai.verta.modeldb.experimentRun;

import ai.verta.common.KeyValue;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.*;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.exceptions.PermissionDeniedException;
import ai.verta.modeldb.experimentRun.subtypes.KeyValueHandler;
import ai.verta.modeldb.experimentRun.subtypes.ObservationHandler;
import ai.verta.uac.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executor;

public class FutureExperimentRunDAO {
  private static Logger LOGGER = LogManager.getLogger(FutureExperimentRunDAO.class);

  private final Executor executor;
  private final FutureJdbi jdbi;
  private final UAC uac;

  private final KeyValueHandler metricsHandler;
  private final KeyValueHandler hyperparametersHandler;
  private final ObservationHandler observationHandler;

  public FutureExperimentRunDAO(Executor executor, FutureJdbi jdbi, UAC uac) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.uac = uac;

    metricsHandler = new KeyValueHandler(executor, jdbi, "metrics");
    hyperparametersHandler = new KeyValueHandler(executor, jdbi, "hyperparameters");
    observationHandler = new ObservationHandler(executor, jdbi);
  }

  public InternalFuture<List<Observation>> getObservations(GetObservations request) {
    // TODO: support artifacts?

    final var runId = request.getId();
    final var key = request.getObservationKey();

    // Check permissions
    var currentFuture = checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.READ);

    return currentFuture.thenCompose(
        unused -> observationHandler.getObservations(runId, key), executor);
  }

  public InternalFuture<Void> logObservations(LogObservations request) {
    // TODO: support artifacts?

    final var runId = request.getId();
    final var observations = request.getObservationsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    // Check permissions
    var currentFuture = checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.UPDATE);

    currentFuture =
        currentFuture.thenCompose(
            unused -> observationHandler.logObservations(runId, observations, now), executor);

    return currentFuture.thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<Void> deleteMetrics(DeleteMetrics request) {
    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    // Check permissions
    var currentFuture = checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.UPDATE);

    currentFuture =
        currentFuture.thenCompose(unused -> metricsHandler.deleteKeyValues(runId), executor);

    return currentFuture.thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<Void> deleteHyperparameters(DeleteHyperparameters request) {
    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    // Check permissions
    var currentFuture = checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.UPDATE);

    currentFuture =
        currentFuture.thenCompose(
            unused -> hyperparametersHandler.deleteKeyValues(runId), executor);

    return currentFuture.thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<List<KeyValue>> getMetrics(GetMetrics request) {
    final var runId = request.getId();

    // Check permissions
    var currentFuture = checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.READ);

    // Validate input

    // Query
    return currentFuture.thenCompose(unused -> metricsHandler.getKeyValue(runId), executor);
  }

  public InternalFuture<List<KeyValue>> getHyperparameters(GetHyperparameters request) {
    final var runId = request.getId();

    // Check permissions
    var currentFuture = checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.READ);

    // Validate input

    // Query
    return currentFuture.thenCompose(unused -> hyperparametersHandler.getKeyValue(runId), executor);
  }

  public InternalFuture<Void> logMetrics(LogMetrics request) {
    final var runId = request.getId();
    final var metrics = request.getMetricsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    // Check permissions
    var currentFuture = checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.UPDATE);

    currentFuture =
        currentFuture.thenCompose(unused -> metricsHandler.logKeyValue(runId, metrics), executor);

    return currentFuture.thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<Void> logHyperparameters(LogHyperparameters request) {
    final var runId = request.getId();
    final var hyperparameters = request.getHyperparametersList();
    final var now = Calendar.getInstance().getTimeInMillis();

    // Check permissions
    var currentFuture = checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.UPDATE);

    currentFuture =
        currentFuture.thenCompose(
            unused -> hyperparametersHandler.logKeyValue(runId, hyperparameters), executor);

    return currentFuture.thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  private InternalFuture<Void> updateModifiedTimestamp(String runId, Long now) {
    return jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    "update experiment_run set date_updated=greatest(date_updated, :now) where id=:run_id")
                .bind("run_id", runId)
                .bind("now", now)
                .execute());
  }

  private InternalFuture<Void> checkProjectPermission(
      String projId, ModelDBActionEnum.ModelDBServiceActions action) {
    return FutureGrpc.ClientRequest(
            uac.getAuthzService()
                .isSelfAllowed(
                    IsSelfAllowed.newBuilder()
                        .addActions(
                            Action.newBuilder()
                                .setModeldbServiceAction(action)
                                .setService(ServiceEnum.Service.MODELDB_SERVICE))
                        .addResources(
                            Resources.newBuilder()
                                .setService(ServiceEnum.Service.MODELDB_SERVICE)
                                .setResourceType(
                                    ResourceType.newBuilder()
                                        .setModeldbServiceResourceType(
                                            ModelDBResourceEnum.ModelDBServiceResourceTypes
                                                .PROJECT))
                                .addResourceIds(projId))
                        .build()),
            executor)
        .thenAccept(
            response -> {
              if (!response.getAllowed()) {
                throw new PermissionDeniedException("Permission denied");
              }
            },
            executor);
  }

  private InternalFuture<Void> checkPermission(
      String runId, ModelDBActionEnum.ModelDBServiceActions action) {

    var futureMaybeProjectId =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("select project_id from experiment_run where id=:id and deleted=0")
                    .bind("id", runId)
                    .mapTo(String.class)
                    .findOne());

    return futureMaybeProjectId.thenCompose(
        maybeProjectId -> {
          if (maybeProjectId.isEmpty()) {
            throw new NotFoundException("Experiment run not found");
          }

          switch (action) {
            default:
              return checkProjectPermission(maybeProjectId.get(), action);
          }
        },
        executor);
  }
}
