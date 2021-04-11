package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.exceptions.AlreadyExistsException;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.Executor;

public class KeyValueHandler {
  private static Logger LOGGER = LogManager.getLogger(KeyValueHandler.class);

  private final Executor executor;
  private final FutureJdbi jdbi;
  private final String fieldType;

  public KeyValueHandler(Executor executor, FutureJdbi jdbi, String fieldType) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.fieldType = fieldType;
  }

  public InternalFuture<List<KeyValue>> getKeyValue(String runId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "select kv_key key, kv_value value, value_type type from keyvalue "
                        + "where entity_name=\"ExperimentRunEntity\" and field_type=:field_type and experiment_run_id=:run_id")
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

  public InternalFuture<Void> logKeyValue(String runId, List<KeyValue> kvs) {
    // Validate input
    var currentFuture =
        InternalFuture.runAsync(
            () -> {
              for (final var kv : kvs) {
                if (kv.getKey().isEmpty()) {
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
                                "select id from keyvalue where entity_name=\"ExperimentRunEntity\" and field_type=:field_type and kv_key=:key and experiment_run_id=:run_id")
                            .bind("key", kv.getKey())
                            .bind("field_type", fieldType)
                            .bind("run_id", runId)
                            .mapTo(Long.class)
                            .findOne()
                            .ifPresent(
                                present -> {
                                  throw new AlreadyExistsException("Key already exists");
                                });

                        handle
                            .createUpdate(
                                "insert into keyvalue (entity_name, field_type, kv_key, kv_value, value_type, experiment_run_id) "
                                    + "values (\"ExperimentRunEntity\", :field_type, :key, :value, :type, :run_id)")
                            .bind("key", kv.getKey())
                            .bind("value", ModelDBUtils.getStringFromProtoObject(kv.getValue()))
                            .bind("type", kv.getValueTypeValue())
                            .bind("run_id", runId)
                            .bind("field_type", fieldType)
                            .executeAndReturnGeneratedKeys()
                            .mapTo(Long.class)
                            .one();
                      }),
              executor);
    }

    return currentFuture;
  }

  public InternalFuture<Void> deleteKeyValues(String runId) {
    return jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    "delete from keyvalue "
                        + "where entity_name=\"ExperimentRunEntity\" and field_type=:field_type and experiment_run_id=:run_id")
                .bind("run_id", runId)
                .bind("field_type", fieldType)
                .execute());
  }
}
