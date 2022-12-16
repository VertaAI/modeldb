package ai.verta.modeldb.common.query;

import java.util.Collections;
import java.util.List;

public class OrderColumn implements OrderItem {
  public final String column;
  public final boolean ascending;

  public OrderColumn(String column, boolean ascending) {
    this.column = column;
    this.ascending = ascending;
  }

  @Override
  public boolean getAscending() {
    return ascending;
  }

  @Override
  public String getTable() {
    return null;
  }

  @Override
  public String getColumn() {
    return column;
  }

  @Override
  public List<OrderColumn> getColumns() {
    return Collections.emptyList();
  }
}
