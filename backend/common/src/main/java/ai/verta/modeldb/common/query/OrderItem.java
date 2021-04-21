package ai.verta.modeldb.common.query;

public interface OrderItem {
  boolean getAscending();

  String getTable();

  String getColumn();
}