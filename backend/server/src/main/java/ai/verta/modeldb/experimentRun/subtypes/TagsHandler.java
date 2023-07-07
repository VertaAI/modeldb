package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.handlers.TagsHandlerBase;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class TagsHandler extends TagsHandlerBase<String> {

  public TagsHandler(FutureExecutor executor, FutureJdbi jdbi, String entityName) {
    super(jdbi, entityName);
  }

  @Override
  protected void setEntityIdReferenceColumn(String entityName) {
    switch (entityName) {
      case "ProjectEntity":
        this.entityIdReferenceColumn = "project_id";
        break;
      case "ExperimentEntity":
        this.entityIdReferenceColumn = "experiment_id";
        break;
      case "ExperimentRunEntity":
        this.entityIdReferenceColumn = "experiment_run_id";
        break;
      default:
        throw new InternalErrorException("Invalid entity name: " + entityName);
    }
  }

  @Override
  protected AbstractMap.SimpleEntry<String, String> getSimpleEntryFromResultSet(ResultSet rs)
      throws SQLException {
    return new AbstractMap.SimpleEntry<>(rs.getString(ENTITY_ID_QUERY_PARAM), rs.getString("tags"));
  }
}
