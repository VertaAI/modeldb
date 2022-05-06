package ai.verta.modeldb.common.futures;

import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.Update;

public class Handle {
  private final org.jdbi.v3.core.Handle handle;

  public Handle(org.jdbi.v3.core.Handle handle) {
    this.handle = handle;
  }

  public Query createQuery(String query) {
    return handle.createQuery(query);
  }

  public Update createUpdate(String query) {
    return handle.createUpdate(query);
  }
}
