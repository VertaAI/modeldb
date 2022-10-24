package ai.verta.modeldb.experimentRun.subtypes;

import static ai.verta.modeldb.entities.config.ConfigBlobEntity.HYPERPARAMETER;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.common.subtypes.MapSubtypes;
import ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob;
import com.google.protobuf.Value;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HyperparametersFromConfigHandler extends KeyValueBaseHandler {
  private static Logger LOGGER = LogManager.getLogger(HyperparametersFromConfigHandler.class);
  private final FutureJdbi jdbi;
  private final FutureExecutor executor;

  public HyperparametersFromConfigHandler(
          FutureExecutor executor, FutureJdbi jdbi, String fieldType, String entityName) {
    super(executor, jdbi, fieldType, entityName);
    this.executor = executor;
    this.jdbi = jdbi;
  }

  public InternalFuture<MapSubtypes<String, KeyValue>> getExperimentRunHyperparameterConfigBlobMap(
      List<String> expRunIds,
      Collection<String> selfAllowedRepositoryIds,
      boolean allowedAllRepositories) {
    if (!allowedAllRepositories) {
      // If all repositories are not allowed and some one send empty selfAllowedRepositoryIds list
      // then this will return empty list from here for security
      if (selfAllowedRepositoryIds == null || selfAllowedRepositoryIds.isEmpty()) {
        return InternalFuture.completedInternalFuture(MapSubtypes.from(new ArrayList<>()));
      }
    }
    return jdbi.withHandle(
            handle -> {
              String queryStr =
                  "SELECT distinct vme.experiment_run_id, hecb.name, hecb.value_type, hecb.int_value, hecb.float_value, hecb.string_value  FROM hyperparameter_element_config_blob hecb "
                      + "INNER JOIN config_blob cb ON cb.hyperparameter_element_config_blob_hash = hecb.blob_hash "
                      + "INNER JOIN versioning_modeldb_entity_mapping vme ON vme.blob_hash = cb.blob_hash "
                      + "WHERE cb.hyperparameter_type = :hyperparameterType AND vme.experiment_run_id IN (<expRunIds>) ";

              if (!allowedAllRepositories) {
                queryStr = queryStr + " AND vme.repository_id IN (<repoIds>)";
              }

              var query = handle.createQuery(queryStr);
              query.bind("hyperparameterType", HYPERPARAMETER);
              query.bindList("expRunIds", expRunIds);
              if (!allowedAllRepositories) {
                query.bindList(
                    "repoIds",
                    selfAllowedRepositoryIds.stream()
                        .map(Long::parseLong)
                        .collect(Collectors.toList()));
              }
              LOGGER.trace(
                  "Final experimentRuns hyperparameter config blob final query : {}", queryStr);
              return query
                  .map(
                      (rs, ctx) -> {
                        var valueBuilder = Value.newBuilder();
                        var valueCase =
                            HyperparameterValuesConfigBlob.ValueCase.forNumber(
                                rs.getInt("value_type"));
                        switch (valueCase) {
                          case INT_VALUE:
                            valueBuilder.setNumberValue(rs.getInt("int_value"));
                            break;
                          case FLOAT_VALUE:
                            valueBuilder.setNumberValue(rs.getDouble("float_value"));
                            break;
                          case STRING_VALUE:
                            valueBuilder.setStringValue(rs.getString("string_value"));
                            break;
                          default:
                            // Do nothing
                            break;
                        }

                        KeyValue hyperparameter =
                            KeyValue.newBuilder()
                                .setKey(rs.getString("name"))
                                .setValue(valueBuilder.build())
                                .build();
                        return new AbstractMap.SimpleEntry<>(
                            rs.getString("experiment_run_id"), hyperparameter);
                      })
                  .list();
            })
        .thenApply(MapSubtypes::from, executor);
  }
}
