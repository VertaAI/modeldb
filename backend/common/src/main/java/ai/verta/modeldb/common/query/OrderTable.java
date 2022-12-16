package ai.verta.modeldb.common.query;

import java.util.List;

public class OrderTable implements OrderItem {
  private final String table;
  private final boolean ascending;
  private final List<OrderColumn> orderColumns;

  public OrderTable(String table, boolean ascending, List<OrderColumn> orderColumns) {
    this.table = table;
    this.ascending = ascending;
    this.orderColumns = orderColumns;
  }

  @Override
  public boolean getAscending() {
    return ascending;
  }

  @Override
  public String getTable() {
    return table;
  }

  @Override
  public String getColumn() {
    return null;
  }

  @Override
  public List<OrderColumn> getColumns() {
    return orderColumns;
  }
}
