package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.modeldb.common.futures.FutureJdbi;

import java.util.concurrent.Executor;

public class AttributeHandler extends KeyValueHandler {
  public AttributeHandler(Executor executor, FutureJdbi jdbi) {
    super(executor, jdbi, "attributes");
  }

  @Override
  protected String getTableName() {
    return "attribute";
  }
}
