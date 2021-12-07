package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.subtypes.MapSubtypes;
import ai.verta.modeldb.exceptions.AlreadyExistsException;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.utils.RdbmsUtils;
import com.google.protobuf.Value;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;

public class KeyValueHandler {
  private static Logger LOGGER = LogManager.getLogger(KeyValueHandler.class);
  private static final String ENTITY_ID_PARAM_QUERY = "entity_id";
  private static final String FIELD_TYPE_QUERY_PARAM = "field_type";
  private static final String ENTITY_NAME_QUERY_PARAM = "entity_name";
  private static final String KEY_QUERY_PARAM = "key";
  private static final String VALUE_QUERY_PARAM = "value";
  private static final String TYPE_QUERY_PARAM = "type";

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
              String.format(
                  "select kv_key as k, kv_value as v, value_type as t from %s where entity_name=:entity_name and field_type=:field_type and %s =:entity_id",
                  getTableName(), entityIdReferenceColumn);

          if (!attrKeys.isEmpty() && !getAll) {
            queryString += " AND kv_key IN (<keys>)";
          }
          var query = handle.createQuery(queryString);
          if (!attrKeys.isEmpty() && !getAll) {
            query.bindList("keys", attrKeys);
          }
          return query
              .bind(ENTITY_ID_PARAM_QUERY, entityId)
              .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
              .bind(ENTITY_NAME_QUERY_PARAM, entityName)
              .map(
                  (rs, ctx) ->
                      KeyValue.newBuilder()
                          .setKey(rs.getString("k"))
                          .setValue(
                              (Value.Builder)
                                  CommonUtils.getProtoObjectFromString(
                                      rs.getString("v"), Value.newBuilder()))
                          .setValueTypeValue(rs.getInt("t"))
                          .build())
              .list();
        });
  }

  public InternalFuture<MapSubtypes<KeyValue>> getKeyValuesMap(Set<String> entityIds) {
    return jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        String.format(
                            "select kv_key as k, kv_value as v, value_type as t, %s as entity_id from %s where entity_name=:entity_name and field_type=:field_type and %s in (<entity_ids>)",
                            entityIdReferenceColumn, getTableName(), entityIdReferenceColumn))
                    .bindList("entity_ids", entityIds)
                    .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
                    .bind(ENTITY_NAME_QUERY_PARAM, entityName)
                    .map(
                        (rs, ctx) ->
                            new AbstractMap.SimpleEntry<>(
                                rs.getString(ENTITY_ID_PARAM_QUERY),
                                KeyValue.newBuilder()
                                    .setKey(rs.getString("k"))
                                    .setValue(
                                        (Value.Builder)
                                            CommonUtils.getProtoObjectFromString(
                                                rs.getString("v"), Value.newBuilder()))
                                    .setValueTypeValue(rs.getInt("t"))
                                    .build()))
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
                      "Multiple key " + kv.getKey() + " found in request");
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
                                String.format(
                                    "select id from %s where entity_name=:entity_name and field_type=:field_type and kv_key=:key and %s =:entity_id",
                                    getTableName(), entityIdReferenceColumn))
                            .bind(KEY_QUERY_PARAM, kv.getKey())
                            .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
                            .bind(ENTITY_NAME_QUERY_PARAM, entityName)
                            .bind(ENTITY_ID_PARAM_QUERY, entityId)
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
                        insertKeyValue(entityId, handle, kv);
                      }
                    }),
            executor);
  }

  private void insertKeyValue(String entityId, Handle handle, KeyValue kv) {
    var queryString =
        "insert into "
            + getTableName()
            + " (entity_name, field_type, kv_key, kv_value, value_type, "
            + entityIdReferenceColumn
            + ") "
            + "values (:entity_name, :field_type, :key, :value, :type, :entity_id)";
    try (var queryHandler = handle.createUpdate(queryString)) {
      queryHandler
          .bind(KEY_QUERY_PARAM, kv.getKey())
          .bind(VALUE_QUERY_PARAM, RdbmsUtils.getValueForKeyValueTable(kv))
          .bind(TYPE_QUERY_PARAM, kv.getValueTypeValue())
          .bind(ENTITY_ID_PARAM_QUERY, entityId)
          .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
          .bind(ENTITY_NAME_QUERY_PARAM, entityName)
          .execute();
    }
  }

  public InternalFuture<Void> deleteKeyValues(String entityId, Optional<List<String>> maybeKeys) {
    return jdbi.useHandle(
        handle -> {
          var sql =
              String.format(
                  "delete from %s where entity_name=:entity_name and field_type=:field_type and %s =:entity_id",
                  getTableName(), entityIdReferenceColumn);

          if (maybeKeys.isPresent() && !maybeKeys.get().isEmpty()) {
            sql += " and kv_key in (<keys>)";
          }

          var query =
              handle
                  .createUpdate(sql)
                  .bind(ENTITY_ID_PARAM_QUERY, entityId)
                  .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
                  .bind(ENTITY_NAME_QUERY_PARAM, entityName);

          if (maybeKeys.isPresent() && !maybeKeys.get().isEmpty()) {
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
        currentFuture
            .thenCompose(
                unused ->
                    // Check for conflicts
                    keyValueExists(entityId, kv),
                executor)
            .thenCompose(
                exists ->
                    // Update into KV table
                    jdbi.useHandle(
                        handle -> {
                          if (exists) {
                            handle
                                .createUpdate(
                                    String.format(
                                        "Update %s SET kv_key=:key, kv_value=:value, value_type=:type "
                                            + " where entity_name=:entity_name and field_type=:field_type and kv_key=:key and %s =:entity_id",
                                        getTableName(), entityIdReferenceColumn))
                                .bind(KEY_QUERY_PARAM, kv.getKey())
                                .bind(VALUE_QUERY_PARAM, RdbmsUtils.getValueForKeyValueTable(kv))
                                .bind(TYPE_QUERY_PARAM, kv.getValueTypeValue())
                                .bind(ENTITY_ID_PARAM_QUERY, entityId)
                                .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
                                .bind(ENTITY_NAME_QUERY_PARAM, entityName)
                                .execute();
                          } else {
                            insertKeyValue(entityId, handle, kv);
                          }
                        }),
                executor);
    return currentFuture;
  }

  private InternalFuture<Boolean> keyValueExists(String entityId, KeyValue kv) {
    // Check for conflicts
    return jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        String.format(
                            "select id from %s where entity_name=:entity_name and field_type=:field_type and kv_key=:key and %s =:entity_id",
                            getTableName(), entityIdReferenceColumn))
                    .bind(KEY_QUERY_PARAM, kv.getKey())
                    .bind(FIELD_TYPE_QUERY_PARAM, fieldType)
                    .bind(ENTITY_NAME_QUERY_PARAM, entityName)
                    .bind(ENTITY_ID_PARAM_QUERY, entityId)
                    .mapTo(Long.class)
                    .findOne())
        .thenApply(count -> (count.isPresent() && count.get() > 0), executor);
  }
}
