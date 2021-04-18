package ai.verta.modeldb.common.query;

import org.jdbi.v3.core.statement.Query;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class QueryFilterContext {
  public final List<String> conditions;
  public final List<Consumer<Query>> binds;

  public QueryFilterContext() {
    conditions = new LinkedList<>();
    binds = new LinkedList<>();
  }

  public QueryFilterContext(List<String> conditions, List<Consumer<Query>> binds) {
    this.conditions = conditions;
    this.binds = binds;
  }
}
