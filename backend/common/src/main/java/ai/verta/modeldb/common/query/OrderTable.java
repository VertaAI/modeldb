package ai.verta.modeldb.common.query;

public class OrderTable implements OrderItem {
  private final String table;
  private final boolean ascending;

  public OrderTable(String table, boolean ascending) {
    this.table = table;
    this.ascending = ascending;
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
}
