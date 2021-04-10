package ai.verta.modeldb.experimentRun;

import ai.verta.modeldb.LogObservations;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.protobuf.Value;

import java.time.Instant;
import java.util.concurrent.Executor;

public class FutureExperimentRunDAO {
  private final Executor executor;
  private final FutureJdbi jdbi;

  public FutureExperimentRunDAO(Executor executor, FutureJdbi jdbi) {
    this.executor = executor;
    this.jdbi = jdbi;
  }

  public InternalFuture<Void> logObservations(LogObservations request) {
    // TODO: check permission
    // TODO: check expt run exists
    // TODO: update timestamp
    // TODO: support artifacts?
    // TODO: check that the key is not empty

    // Create initial future
    var currentFuture = InternalFuture.runAsync(() -> {}, executor);

    final var runId = request.getId();
    final var observations = request.getObservationsList();
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
                      // Otherwise, infer at runtime
                      final var sql =
                          "select max(o.epoch_number) from "
                              + "(select keyvaluemapping_id, epoch_number from observation "
                              + "where experiment_run_id =:run_id and entity_name = 'ExperimentRunEntity') o, "
                              + "(select id from keyvalue where kv_key =:name and entity_name IS NULL) k "
                              + "where o.keyvaluemapping_id = k.id";
                      return jdbi.withHandle(
                          handle ->
                              handle
                                  .createQuery(sql)
                                  .bind("run_id", runId)
                                  .bind("name", attribute.getKey())
                                  .mapTo(Long.class)
                                  .one());
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
                                        "insert into keyvalue (field_type, kv_key, kv_value, value_type)"
                                            + "values ('attributes', :key, :value, :type)")
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
                                        "insert into observation (entity_name, field_type, timestamp, experiment_run_id, keyvaluemapping_id, epoch_number)"
                                            + "values ('ExperimentRunEntity', 'observations', :timestamp, :run_id, :kvid, :epoch")
                                    .bind(
                                        "timestamp",
                                        observation.getTimestamp() == 0
                                            ? Instant.now()
                                            : Instant.ofEpochMilli(observation.getTimestamp()))
                                    .bind("run_id", runId)
                                    .bind("kvid", kvId)
                                    .bind("epoch", epoch)
                                    .executeAndReturnGeneratedKeys()
                                    .mapTo(Long.class)
                                    .one();

                            return null;
                          }),
                  executor);
    }

    return currentFuture;
  }
}
