package ai.verta.modeldb.experimentRun;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.LogObservations;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.exceptions.PermissionDeniedException;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.*;
import com.google.protobuf.Value;

import java.util.Calendar;
import java.util.concurrent.Executor;

public class FutureExperimentRunDAO {
  private final Executor executor;
  private final FutureJdbi jdbi;
  private final UAC uac;

  public FutureExperimentRunDAO(Executor executor, FutureJdbi jdbi, UAC uac) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.uac = uac;
  }

  public InternalFuture<Void> logObservations(LogObservations request) {
    // TODO: support artifacts?

    final var runId = request.getId();
    final var observations = request.getObservationsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    // Check permissions
    var currentFuture = checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.UPDATE);

    // Validate input
    currentFuture =
        currentFuture.thenRun(
            () -> {
              for (final var observation : observations) {
                if (observation.getAttribute().getKey().isEmpty()) {
                  throw new InvalidArgumentException("Empty observation key");
                }
              }
            },
            executor);

    // Log observations
    for (final var observation : observations) {
      final var attribute = observation.getAttribute();
      currentFuture =
          currentFuture
              .thenCompose(
                  unused -> {
                    // If the epoch is specified, save it directly
                    if (observation.hasEpochNumber()) {
                      if (observation.getEpochNumber().getKindCase()
                          != Value.KindCase.NUMBER_VALUE) {
                        String invalidEpochMessage =
                            "Observations can only have numeric epoch_number, condition not met in "
                                + observation;
                        throw new InvalidArgumentException(invalidEpochMessage);
                      }
                      return InternalFuture.completedInternalFuture(
                          (long) observation.getEpochNumber().getNumberValue());
                    } else {
                      // Otherwise, infer at runtime. We can't do this in the same SQL command as
                      // we'll be updating these tables and some SQL implementations don't support
                      // select together with updates
                      final var sql =
                          "select max(o.epoch_number) from "
                              + "(select keyvaluemapping_id, epoch_number from observation "
                              + "where experiment_run_id =:run_id and entity_name = \"ExperimentRunEntity\") o, "
                              + "(select id from keyvalue where kv_key =:name and entity_name IS NULL) k "
                              + "where o.keyvaluemapping_id = k.id";
                      return jdbi.withHandle(
                          handle ->
                              handle
                                  .createQuery(sql)
                                  .bind("run_id", runId)
                                  .bind("name", attribute.getKey())
                                  .mapTo(Long.class)
                                  .findOne()
                                  .map(x -> x + 1)
                                  .orElse(0L));
                    }
                  },
                  executor)
              .thenCompose(
                  epoch ->
                      // Insert into KV table
                      jdbi.withHandle(
                          handle -> {
                            final var kvId =
                                handle
                                    .createUpdate(
                                        "insert into keyvalue (field_type, kv_key, kv_value, value_type) "
                                            + "values (\"attributes\", :key, :value, :type)")
                                    .bind("key", attribute.getKey())
                                    .bind(
                                        "value",
                                        ModelDBUtils.getStringFromProtoObject(attribute.getValue()))
                                    .bind("type", attribute.getValueTypeValue())
                                    .executeAndReturnGeneratedKeys()
                                    .mapTo(Long.class)
                                    .one();

                            // Insert to observation table
                            // We don't need transaction here since it's fine to add to the kv table
                            // and fail to insert into the observation table, as the value will be
                            // just ignored
                            final var observationId =
                                handle
                                    .createUpdate(
                                        "insert into observation (entity_name, field_type, timestamp, experiment_run_id, keyvaluemapping_id, epoch_number) "
                                            + "values (\"ExperimentRunEntity\", \"observations\", :timestamp, :run_id, :kvid, :epoch)")
                                    .bind(
                                        "timestamp",
                                        observation.getTimestamp() == 0
                                            ? now
                                            : observation.getTimestamp())
                                    .bind("run_id", runId)
                                    .bind("kvid", kvId)
                                    .bind("epoch", epoch)
                                    .executeAndReturnGeneratedKeys()
                                    .mapTo(Long.class)
                                    .one();

                            handle
                                .createUpdate(
                                    "update experiment_run set date_updated=max(date_updated, :now) where id=:run_id")
                                .bind("run_id", runId)
                                .bind("now", now)
                                .execute();

                            return null;
                          }),
                  executor);
    }

    return currentFuture;
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
