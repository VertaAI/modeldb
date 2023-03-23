package ai.verta.modeldb.experimentRun.subtypes;

import java.util.Arrays;
import java.util.Map;

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
}
