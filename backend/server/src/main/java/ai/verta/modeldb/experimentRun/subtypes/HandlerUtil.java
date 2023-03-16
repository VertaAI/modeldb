package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.modeldb.common.futures.Handle;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.Logger;

public abstract class HandlerUtil {

  protected String buildInsertQuery(Map<String, Object> rmValueMap, String entityName) {
    // Created comma separated field names from keys of above map
    String[] fieldsArr = rmValueMap.keySet().toArray(new String[0]);
    var commaFields = String.join(",", fieldsArr);

    // Created comma separated query bind arguments for the values
    // based on the
    // keys of
    // above the map
    // Ex: VALUES (:project_id, :experiment_id, :name) etc.
    var bindArguments =
        String.join(",", Arrays.stream(fieldsArr).map(s -> ":" + s).toArray(String[]::new));

    return String.format(
        "insert into %s (%s) values (%s) ", entityName, commaFields, bindArguments);
  }

  protected void buildUpdateQueryAndExecute(
      Logger LOGGER,
      Handle handleForTransaction,
      long entityId,
      Map<String, Object> rmValueMap,
      String entityName) {
    StringBuilder queryStrBuilder = new StringBuilder(String.format("UPDATE %s SET ", entityName));

    AtomicInteger count = new AtomicInteger();
    rmValueMap.forEach(
        (key, value) -> {
          queryStrBuilder.append(key).append(" = :").append(key);
          if (count.get() < rmValueMap.size() - 1) {
            queryStrBuilder.append(", ");
          }
          count.getAndIncrement();
        });

    queryStrBuilder.append(" WHERE id = :id ");

    LOGGER.trace(
        String.format("updated %s query string: %s", entityName, queryStrBuilder.toString()));
    try (var query = handleForTransaction.createUpdate(queryStrBuilder.toString())) {
      // Inserting fields arguments based on the keys and value of map
      for (Map.Entry<String, Object> objectEntry : rmValueMap.entrySet()) {
        query.bind(objectEntry.getKey(), objectEntry.getValue());
      }

      query.bind("id", entityId).execute();
    }
  }
}
