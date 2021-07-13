package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.Observation;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import java.util.AbstractMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

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
                            "select k.kv_value _value, k.value_type _type, o.epoch_number epoch from "
                                + "(select keyvaluemapping_id, epoch_number from observation "
                                + "where experiment_run_id =:run_id and entity_name = :entity_name) o, "
                                + "(select id, kv_value, value_type from keyvalue where kv_key =:name and entity_name IS NULL) k "
                                + "where o.keyvaluemapping_id = k.id")
                        .bind("run_id", runId)
                        .bind("entity_name", "ExperimentRunEntity")
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
                                                        rs.getString("_value"), Value.newBuilder()))
                                            .setValueTypeValue(rs.getInt("_type")))
                                    .build();
                              } catch (InvalidProtocolBufferException e) {
                                LOGGER.error(
                                    "Error generating builder for {}", rs.getString("_value"));
                                throw new ModelDBException(e);
                              }
                            })
                        .list()),
        executor);
  }

  public InternalFuture<MapSubtypes<Observation>> getObservationsMap(Set<String> runIds) {
    return jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "select k.kv_key _key, k.kv_value _value, k.value_type _type, o.epoch_number epoch, o.experiment_run_id run_id, o.timestamp "
                            + " from observation as o"
                            + " join keyvalue as k"
                            + " on o.keyvaluemapping_id = k.id"
                            + " where o.experiment_run_id in (<run_ids>) and o.entity_name = :entityName and k.entity_name IS NULL")
                    .bindList("run_ids", runIds)
                    .bind("entityName", "ExperimentRunEntity")
                    .map(
                        (rs, ctx) -> {
                          try {
                            return new AbstractMap.SimpleEntry<>(
                                rs.getString("run_id"),
                                Observation.newBuilder()
                                    .setTimestamp(rs.getLong("timestamp"))
                                    .setEpochNumber(
                                        Value.newBuilder().setNumberValue(rs.getLong("epoch")))
                                    .setAttribute(
                                        KeyValue.newBuilder()
                                            .setKey(rs.getString("_key"))
                                            .setValue(
                                                (Value.Builder)
                                                    CommonUtils.getProtoObjectFromString(
                                                        rs.getString("_value"), Value.newBuilder()))
                                            .setValueTypeValue(rs.getInt("_type")))
                                    .build());
                          } catch (InvalidProtocolBufferException e) {
                            LOGGER.error("Error generating builder for {}", rs.getString("_value"));
                            throw new ModelDBException(e);
                          }
                        })
                    .list())
        .thenApply(MapSubtypes::from, executor);
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
                              + "where experiment_run_id =:run_id and entity_name = :entity_name) o, "
                              + "(select id from keyvalue where kv_key =:name and entity_name IS NULL) k "
                              + "where o.keyvaluemapping_id = k.id";
                      return jdbi.withHandle(
                          handle ->
                              handle
                                  .createQuery(sql)
                                  .bind("run_id", runId)
                                  .bind("entity_name", "ExperimentRunEntity")
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
                                    .bind("field_type", "attributes")
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
                                .bind("entity_name", "ExperimentRunEntity")
                                .bind("field_type", "observations")
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
          handle.useTransaction(
              TransactionIsolationLevel.SERIALIZABLE,
              handle1 -> {
                // Delete from keyvalue
                var fetchQueryString = "select o.id, o.keyvaluemapping_id from observation as o ";
                if (maybeKeys.isPresent()) {
                  fetchQueryString += " INNER JOIN keyvalue as kv ON o.keyvaluemapping_id = kv.id ";
                }
                fetchQueryString +=
                    " WHERE o.experiment_run_id = :run_id " + " AND o.entity_name = :entityName";

                if (maybeKeys.isPresent()) {
                  fetchQueryString += " AND kv.kv_key IN (<keys>)";
                }
                var query =
                    handle
                        .createQuery(fetchQueryString)
                        .bind("run_id", runId)
                        .bind("entityName", "ExperimentRunEntity");

                if (maybeKeys.isPresent()) {
                  query = query.bindList("keys", maybeKeys.get());
                }
                var observationKVMappingList =
                    query
                        .map(
                            (rs, ctx) ->
                                new AbstractMap.SimpleEntry<>(
                                    rs.getLong("id"), rs.getLong("keyvaluemapping_id")))
                        .list();

                // Remove foreignKey constraint first
                handle
                    .createUpdate(
                        "update observation set keyvaluemapping_id = null where id in (<ob_ids>)")
                    .bindList(
                        "ob_ids",
                        observationKVMappingList.stream()
                            .map(AbstractMap.SimpleEntry::getKey)
                            .collect(Collectors.toList()))
                    .execute();

                // Delete KeyValue mapped with Observation by Ids
                handle
                    .createUpdate("delete from keyvalue where id in (<kv_ids>)")
                    .bind("entity_name", "ExperimentRunEntity")
                    .bind("field_type", "observations")
                    .bindList(
                        "kv_ids",
                        observationKVMappingList.stream()
                            .map(AbstractMap.SimpleEntry::getValue)
                            .collect(Collectors.toList()))
                    .execute();

                // Delete from observations by Ids
                handle
                    .createUpdate("delete from observation where id in (<ob_ids>)")
                    .bindList(
                        "ob_ids",
                        observationKVMappingList.stream()
                            .map(AbstractMap.SimpleEntry::getKey)
                            .collect(Collectors.toList()))
                    .execute();
              });
        });
  }
}
