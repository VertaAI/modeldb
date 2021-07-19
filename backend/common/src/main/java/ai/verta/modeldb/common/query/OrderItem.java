package ai.verta.modeldb.common.query;

import java.util.List;

public interface OrderItem {
  boolean getAscending();

  String getTable();

  String getColumn();

  List<OrderColumn> getColumns();
}
