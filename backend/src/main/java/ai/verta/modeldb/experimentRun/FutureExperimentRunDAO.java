package ai.verta.modeldb.experimentRun;

import ai.verta.common.KeyValue;
import ai.verta.common.ModelDBResourceEnum;
import ai.verta.modeldb.*;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.FutureGrpc;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.exceptions.AlreadyExistsException;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.exceptions.PermissionDeniedException;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.*;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
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

  public FutureExperimentRunDAO(Executor executor, FutureJdbi jdbi, UAC uac) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.uac = uac;
  }

  public InternalFuture<List<Observation>> getObservations(GetObservations request) {
    // TODO: support artifacts?

    final var runId = request.getId();
    final var key = request.getObservationKey();

    // Check permissions
    var currentFuture = checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.READ);

    // Validate input
    currentFuture =
        currentFuture.thenRun(
            () -> {
              if (key.isEmpty()) {
                throw new InvalidArgumentException("Empty observation key");
              }
            },
            executor);

    // Query
    return currentFuture.thenCompose(
        unused ->
            jdbi.withHandle(
                handle ->
                    handle
                        .createQuery(
                            "select k.kv_value value, k.value_type type, o.epoch_number epoch from "
                                + "(select keyvaluemapping_id, epoch_number from observation "
                                + "where experiment_run_id =:run_id and entity_name = \"ExperimentRunEntity\") o, "
                                + "(select id, kv_value, value_type from keyvalue where kv_key =:name and entity_name IS NULL) k "
                                + "where o.keyvaluemapping_id = k.id")
                        .bind("run_id", runId)
                        .bind("name", key)
                        .map(
                            (rs, ctx) -> {
                              try {
                                return Observation.newBuilder()
                                    .setEpochNumber(
                                        Value.newBuilder().setNumberValue(rs.getLong("epoch")))
                                    .setAttribute(
                                        KeyValue.newBuilder()
                                            .setKey(key)
                                            .setValue(
                                                (Value.Builder)
                                                    CommonUtils.getProtoObjectFromString(
                                                        rs.getString("value"), Value.newBuilder()))
                                            .setValueTypeValue(rs.getInt("type")))
                                    .build();
                              } catch (InvalidProtocolBufferException e) {
                                LOGGER.error(
                                    "Error generating builder for {}", rs.getString("value"));
                                throw new ModelDBException(e);
                              }
                            })
                        .list()),
        executor);
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
                      jdbi.useHandle(
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
                          }),
                  executor);
    }

    return currentFuture.thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<Void> deleteMetrics(DeleteMetrics request) {
    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    // Check permissions
    var currentFuture = checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.UPDATE);

    currentFuture =
        currentFuture.thenCompose(unused -> deleteKeyValues(runId, "metrics"), executor);

    return currentFuture.thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<Void> deleteHyperparameters(DeleteHyperparameters request) {
    final var runId = request.getId();
    final var now = Calendar.getInstance().getTimeInMillis();

    // Check permissions
    var currentFuture = checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.UPDATE);

    currentFuture =
        currentFuture.thenCompose(unused -> deleteKeyValues(runId, "hyperparameters"), executor);

    return currentFuture.thenCompose(unused -> updateModifiedTimestamp(runId, now), executor);
  }

  public InternalFuture<List<KeyValue>> getMetrics(GetMetrics request) {
    final var runId = request.getId();

    // Check permissions
    var currentFuture = checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.READ);

    // Validate input

    // Query
    return currentFuture.thenCompose(unused -> getKeyValue(runId, "metrics"), executor);
  }

  public InternalFuture<List<KeyValue>> getHyperparameters(GetHyperparameters request) {
    final var runId = request.getId();

    // Check permissions
    var currentFuture = checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.READ);

    // Validate input

    // Query
    return currentFuture.thenCompose(unused -> getKeyValue(runId, "hyperparameters"), executor);
  }

  public InternalFuture<Void> logMetrics(LogMetrics request) {
    final var runId = request.getId();
    final var metrics = request.getMetricsList();
    final var now = Calendar.getInstance().getTimeInMillis();

    // Check permissions
    var currentFuture = checkPermission(runId, ModelDBActionEnum.ModelDBServiceActions.UPDATE);

    currentFuture =
        currentFuture.thenCompose(unused -> logKeyValue(runId, "metrics", metrics), executor);

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
            unused -> logKeyValue(runId, "hyperparameters", hyperparameters), executor);

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

  private InternalFuture<Void> deleteKeyValues(String runId, String fieldType) {
    return jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    "delete from keyvalue "
                        + "where entity_name=\"ExperimentRunEntity\" and field_type=:field_type and experiment_run_id=:runId")
                .bind("run_id", runId)
                .bind("field_type", fieldType)
                .execute());
  }

  private InternalFuture<List<KeyValue>> getKeyValue(String runId, String fieldType) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "select kv_key key, kv_value value, value_type type from keyvalue "
                        + "where entity_name=\"ExperimentRunEntity\" and field_type=:field_type and experiment_run_id=:runId")
                .bind("run_id", runId)
                .bind("field_type", fieldType)
                .map(
                    (rs, ctx) -> {
                      try {
                        return KeyValue.newBuilder()
                            .setKey(rs.getString("key"))
                            .setValue(
                                (Value.Builder)
                                    CommonUtils.getProtoObjectFromString(
                                        rs.getString("value"), Value.newBuilder()))
                            .setValueTypeValue(rs.getInt("type"))
                            .build();
                      } catch (InvalidProtocolBufferException e) {
                        LOGGER.error("Error generating builder for {}", rs.getString("value"));
                        throw new ModelDBException(e);
                      }
                    })
                .list());
  }

  private InternalFuture<Void> logKeyValue(String runId, String fieldType, List<KeyValue> kvs) {
    // Validate input
    var currentFuture =
        InternalFuture.runAsync(
            () -> {
              for (final var metric : kvs) {
                if (metric.getKey().isEmpty()) {
                  throw new InvalidArgumentException("Empty key");
                }
              }
            },
            executor);

    // Log
    for (final var kv : kvs) {
      currentFuture =
          currentFuture.thenCompose(
              unused ->
                  // Insert into KV table
                  jdbi.useHandle(
                      handle -> {
                        handle
                            .createQuery(
                                "select id from keyvalue where entity_name=\"ExperimentRunEntity\" and field_type=:field_type and kv_key=:key and experiment_run_id=:runId")
                            .bind("key", kv.getKey())
                            .bind("field_type", fieldType)
                            .bind("runId", runId)
                            .mapTo(Long.class)
                            .findOne()
                            .ifPresent(
                                present -> {
                                  throw new AlreadyExistsException("Key already exists");
                                });

                        handle
                            .createUpdate(
                                "insert into keyvalue (entity_name, field_type, kv_key, kv_value, value_type, experiment_run_id) "
                                    + "values (\"ExperimentRunEntity\", :field_type, :key, :value, :type, :runId)")
                            .bind("key", kv.getKey())
                            .bind("value", ModelDBUtils.getStringFromProtoObject(kv.getValue()))
                            .bind("type", kv.getValueTypeValue())
                            .bind("runId", runId)
                            .bind("field_type", fieldType)
                            .executeAndReturnGeneratedKeys()
                            .mapTo(Long.class)
                            .one();
                      }),
              executor);
    }

    return currentFuture;
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
