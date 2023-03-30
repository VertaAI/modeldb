package ai.verta.modeldb.experimentrun.subtypes;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.subtypes.KeyValueHandler;
import com.google.protobuf.Value;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;

public class KeyValueBaseHandler extends KeyValueHandler<String> {
  public KeyValueBaseHandler(
      FutureExecutor executor, FutureJdbi jdbi, String fieldType, String entityName) {
    super(executor, jdbi, fieldType, entityName);
  }

  @Override
  protected AbstractMap.SimpleEntry<String, KeyValue> getSimpleEntryFromResultSet(ResultSet rs)
      throws SQLException {
    return new AbstractMap.SimpleEntry<>(
        rs.getString(ENTITY_ID_PARAM_QUERY),
        KeyValue.newBuilder()
            .setKey(rs.getString("k"))
            .setValue(
                (Value.Builder)
                    CommonUtils.getProtoObjectFromString(rs.getString("v"), Value.newBuilder()))
            .setValueTypeValue(rs.getInt("t"))
            .build());
  }

  @Override
  protected String getTableName() {
    return "keyvalue";
  }

  @Override
  protected void setEntityIdReferenceColumn(String entityName) {
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
}
