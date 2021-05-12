package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.Observation;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ObservationHandler {
  private static Logger LOGGER = LogManager.getLogger(KeyValueHandler.class);

  private final Executor executor;
  private final FutureJdbi jdbi;

  public ObservationHandler(Executor executor, FutureJdbi jdbi) {
    this.executor = executor;
    this.jdbi = jdbi;
  }

  public InternalFuture<List<Observation>> getObservations(String runId, String key) {
    // TODO: support artifacts?

    // Validate input
    var currentFuture =
        InternalFuture.runAsync(
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

  public InternalFuture<Void> logObservations(
      String runId, List<Observation> observations, long now) {
    // TODO: support artifacts?

    // Validate input
    var currentFuture =
        InternalFuture.runAsync(
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
                                            + "values (:field_type, :key, :value, :type)")
                                    .bind("field_type", "\"attributes\"")
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
                                        + "values (:entity_name, :field_type, :timestamp, :run_id, :kvid, :epoch)")
                                .bind(
                                    "timestamp",
                                    observation.getTimestamp() == 0
                                        ? now
                                        : observation.getTimestamp())
                                .bind("entity_name", "\"ExperimentRunEntity\"")
                                .bind("field_type", "\"observations\"")
                                .bind("run_id", runId)
                                .bind("kvid", kvId)
                                .bind("epoch", epoch)
                                .executeAndReturnGeneratedKeys()
                                .mapTo(Long.class)
                                .one();
                          }),
                  executor);
    }

    return currentFuture;
  }

  public InternalFuture<Void> deleteObservations(String runId, Optional<List<String>> maybeKeys) {
    return jdbi.useHandle(
        handle -> {
          // Delete from keyvalue
          var sql =
              "delete from keyvalue where id in "
                  + "(select keyvaluemapping_id from observation where entity_name=\"ExperimentRunEntity\" and field_type=\"observations\" and experiment_run_id=:run_id)";

          if (maybeKeys.isPresent()) {
            sql += " and kv_key in (<keys>)";
          }

          var query = handle.createUpdate(sql).bind("run_id", runId);

          if (maybeKeys.isPresent()) {
            query = query.bindList("keys", maybeKeys.get());
          }

          query.execute();

          // Delete from observations by finding missing keyvalue matches
          sql =
              "delete from observation where id in "
                  + "(select o.id from observation o left join keyvalue k on o.keyvaluemapping_id=k.id where o.experiment_run_id=:run_id and o.keyvaluemapping_id is null)";
          query = handle.createUpdate(sql).bind("run_id", runId);
          query.execute();
        });
  }
}
