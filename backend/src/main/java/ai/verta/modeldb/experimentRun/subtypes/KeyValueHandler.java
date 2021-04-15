package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.exceptions.AlreadyExistsException;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KeyValueHandler {
  private static Logger LOGGER = LogManager.getLogger(KeyValueHandler.class);

  private final Executor executor;
  private final FutureJdbi jdbi;
  private final String fieldType;
  private final String entityName;

  protected String getTableName() {
    return "keyvalue";
  }

  public KeyValueHandler(Executor executor, FutureJdbi jdbi, String fieldType, String entityName) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.fieldType = fieldType;
    this.entityName = entityName;
  }

  public InternalFuture<List<KeyValue>> getKeyValues(String runId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "select kv_key as k, kv_value as v, value_type as t from "
                        + getTableName()
                        + " where entity_name=:entity_name and field_type=:field_type and experiment_run_id=:run_id")
                .bind("run_id", runId)
                .bind("field_type", fieldType)
                .bind("entity_name", entityName)
                .map(
                    (rs, ctx) -> {
                      try {
                        return KeyValue.newBuilder()
                            .setKey(rs.getString("k"))
                            .setValue(
                                (Value.Builder)
                                    CommonUtils.getProtoObjectFromString(
                                        rs.getString("v"), Value.newBuilder()))
                            .setValueTypeValue(rs.getInt("t"))
                            .build();
                      } catch (InvalidProtocolBufferException e) {
                        LOGGER.error("Error generating builder for {}", rs.getString("v"));
                        throw new ModelDBException(e);
                      }
                    })
                .list());
  }

  public InternalFuture<Void> logKeyValues(String runId, List<KeyValue> kvs) {
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
                                "select id from "
                                    + getTableName()
                                    + " where entity_name=:entity_name and field_type=:field_type and kv_key=:key and experiment_run_id=:run_id")
                            .bind("key", kv.getKey())
                            .bind("field_type", fieldType)
                            .bind("entity_name", entityName)
                            .bind("run_id", runId)
                            .mapTo(Long.class)
                            .findOne()
                            .ifPresent(
                                present -> {
                                  throw new AlreadyExistsException("Key already exists");
                                });

                        handle
                            .createUpdate(
                                "insert into "
                                    + getTableName()
                                    + " (entity_name, field_type, kv_key, kv_value, value_type, experiment_run_id) "
                                    + " values (:entity_name, :field_type, :key, :value, :type, :run_id)")
                            .bind("key", kv.getKey())
                            .bind("value", ModelDBUtils.getStringFromProtoObject(kv.getValue()))
                            .bind("type", kv.getValueTypeValue())
                            .bind("run_id", runId)
                            .bind("field_type", fieldType)
                            .bind("entity_name", entityName)
                            .executeAndReturnGeneratedKeys()
                            .mapTo(Long.class)
                            .one();
                      }),
              executor);
    }

    return currentFuture;
  }

  public InternalFuture<Void> deleteKeyValues(String runId, Optional<List<String>> maybeKeys) {
    return jdbi.useHandle(
        handle -> {
          var sql =
              "delete from "
                  + getTableName()
                  + " where entity_name=:entity_name and field_type=:field_type and experiment_run_id=:run_id";

          if (maybeKeys.isPresent()) {
            sql += " and kv_key in (<keys>)";
          }

          var query =
              handle
                  .createUpdate(sql)
                  .bind("run_id", runId)
                  .bind("field_type", fieldType)
                  .bind("entity_name", entityName);

          if (maybeKeys.isPresent()) {
            query = query.bindList("keys", maybeKeys.get());
          }

          query.execute();
        });
  }
}
