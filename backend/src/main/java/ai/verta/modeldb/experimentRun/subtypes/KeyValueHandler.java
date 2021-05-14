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
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KeyValueHandler {
  private static Logger LOGGER = LogManager.getLogger(KeyValueHandler.class);

  private final Executor executor;
  private final FutureJdbi jdbi;
  private final String fieldType;
  private final String entityName;
  private final String entityIdReferenceColumn;

  protected String getTableName() {
    return "keyvalue";
  }

  public KeyValueHandler(Executor executor, FutureJdbi jdbi, String fieldType, String entityName) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.fieldType = fieldType;
    this.entityName = entityName;

    switch (entityName) {
      case "ProjectEntity":
        this.entityIdReferenceColumn = "project_id";
        break;
      case "ExperimentRunEntity":
        this.entityIdReferenceColumn = "experiment_run_id";
        break;
      default:
        throw new InternalErrorException("Invalid entity name: " + entityName);
    }
  }

  public InternalFuture<List<KeyValue>> getKeyValues(
      String entityId, List<String> attrKeys, boolean getAll) {
    return jdbi.withHandle(
        handle -> {
          var queryString =
              "select kv_key as k, kv_value as v, value_type as t from "
                  + getTableName()
                  + " where entity_name=:entity_name and field_type=:field_type and "
                  + entityIdReferenceColumn
                  + "=:entity_id";

          if (!attrKeys.isEmpty() && !getAll) {
            queryString += " AND kv_key IN (<keys>)";
          }
          var query = handle.createQuery(queryString);
          if (!attrKeys.isEmpty() && !getAll) {
            query.bindList("keys", attrKeys);
          }
          return query
              .bind("entity_id", entityId)
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
              .list();
        });
  }

  public InternalFuture<MapSubtypes<KeyValue>> getKeyValuesMap(Set<String> entityIds) {
    return jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "select kv_key as k, kv_value as v, value_type as t, "
                            + entityIdReferenceColumn
                            + " as entity_id from "
                            + getTableName()
                            + " where entity_name=:entity_name and field_type=:field_type and "
                            + entityIdReferenceColumn
                            + " in (<entity_ids>)")
                    .bindList("entity_ids", entityIds)
                    .bind("field_type", fieldType)
                    .bind("entity_name", entityName)
                    .map(
                        (rs, ctx) -> {
                          try {
                            return new AbstractMap.SimpleEntry<>(
                                rs.getString("entity_id"),
                                KeyValue.newBuilder()
                                    .setKey(rs.getString("k"))
                                    .setValue(
                                        (Value.Builder)
                                            CommonUtils.getProtoObjectFromString(
                                                rs.getString("v"), Value.newBuilder()))
                                    .setValueTypeValue(rs.getInt("t"))
                                    .build());
                          } catch (InvalidProtocolBufferException e) {
                            LOGGER.error("Error generating builder for {}", rs.getString("v"));
                            throw new ModelDBException(e);
                          }
                        })
                    .list())
        .thenApply(MapSubtypes::from, executor);
  }

  public InternalFuture<Void> logKeyValues(String entityId, List<KeyValue> kvs) {
    // Validate input
    return InternalFuture.runAsync(
            () -> {
              Set<String> keySet = new HashSet<>();
              for (final var kv : kvs) {
                if (kv.getKey().isEmpty()) {
                  throw new InvalidArgumentException("Empty key");
                }
                if (keySet.contains(kv.getKey())) {
                  throw new InvalidArgumentException(
                      "Multiple Key " + kv.getKey() + " found in " + getTableName());
                }
                keySet.add(kv.getKey());
              }
            },
            executor)
        .thenCompose(
            unused ->
                // Check for conflicts
                jdbi.useHandle(
                    handle -> {
                      for (final var kv : kvs) {
                        handle
                            .createQuery(
                                "select id from "
                                    + getTableName()
                                    + " where entity_name=:entity_name and field_type=:field_type and kv_key=:key and "
                                    + entityIdReferenceColumn
                                    + "=:entity_id")
                            .bind("key", kv.getKey())
                            .bind("field_type", fieldType)
                            .bind("entity_name", entityName)
                            .bind("entity_id", entityId)
                            .mapTo(Long.class)
                            .findOne()
                            .ifPresent(
                                present -> {
                                  throw new AlreadyExistsException(
                                      "Key " + kv.getKey() + " already exists");
                                });
                      }
                    }),
            executor)
        .thenCompose(
            unused ->
                // Log
                jdbi.useHandle(
                    handle -> {
                      for (final var kv : kvs) {
                        handle
                            .createUpdate(
                                "insert into "
                                    + getTableName()
                                    + " (entity_name, field_type, kv_key, kv_value, value_type, "
                                    + entityIdReferenceColumn
                                    + ") "
                                    + "values (:entity_name, :field_type, :key, :value, :type, :entity_id)")
                            .bind("key", kv.getKey())
                            .bind("value", ModelDBUtils.getStringFromProtoObject(kv.getValue()))
                            .bind("type", kv.getValueTypeValue())
                            .bind("entity_id", entityId)
                            .bind("field_type", fieldType)
                            .bind("entity_name", entityName)
                            .execute();
                      }
                    }),
            executor);
  }

  public InternalFuture<Void> deleteKeyValues(String entityId, Optional<List<String>> maybeKeys) {
    return jdbi.useHandle(
        handle -> {
          var sql =
              "delete from "
                  + getTableName()
                  + " where entity_name=:entity_name and field_type=:field_type and "
                  + entityIdReferenceColumn
                  + "=:entity_id";

          if (maybeKeys.isPresent()) {
            sql += " and kv_key in (<keys>)";
          }

          var query =
              handle
                  .createUpdate(sql)
                  .bind("entity_id", entityId)
                  .bind("field_type", fieldType)
                  .bind("entity_name", entityName);

          if (maybeKeys.isPresent()) {
            query = query.bindList("keys", maybeKeys.get());
          }

          query.execute();
        });
  }

  // TODO: We might end up removing this update since ERs don't have them.
  // Comment: https://github.com/VertaAI/modeldb/pull/2118#discussion_r613762413
  public InternalFuture<Void> updateKeyValue(String entityId, KeyValue kv) {
    var currentFuture =
        InternalFuture.runAsync(
            () -> {
              if (kv.getKey().isEmpty()) {
                throw new InvalidArgumentException("Empty key");
              }
            },
            executor);

    currentFuture =
        currentFuture.thenCompose(
            unused ->
                // Update into KV table
                jdbi.useHandle(
                    handle -> {
                      handle
                          .createUpdate(
                              "Update "
                                  + getTableName()
                                  + " SET kv_key=:key, kv_value=:value, value_type=:type "
                                  + " where entity_name=:entity_name and field_type=:field_type and kv_key=:key and "
                                  + entityIdReferenceColumn
                                  + "=:entity_id")
                          .bind("key", kv.getKey())
                          .bind("value", ModelDBUtils.getStringFromProtoObject(kv.getValue()))
                          .bind("type", kv.getValueTypeValue())
                          .bind("entity_id", entityId)
                          .bind("field_type", fieldType)
                          .bind("entity_name", entityName)
                          .execute();
                    }),
            executor);
    return currentFuture;
  }
}
