package ai.verta.modeldb.common.query;

import org.jdbi.v3.core.statement.Query;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class QueryFilterContext {
  private final List<String> conditions;
  private final List<Consumer<Query>> binds;
  private final List<OrderItem> orderItems;

  public QueryFilterContext() {
    conditions = new LinkedList<>();
    binds = new LinkedList<>();
    orderItems = new LinkedList<>();
  }

  public List<String> getConditions() {
    return conditions;
  }

  public List<Consumer<Query>> getBinds() {
    return binds;
  }

  public List<OrderItem> getOrderItems() {
    return orderItems;
  }

  public QueryFilterContext addCondition(String condition) {
    this.conditions.add(condition);
    return this;
  }

  public QueryFilterContext addBind(Consumer<Query> bind) {
    this.binds.add(bind);
    return this;
  }

  public QueryFilterContext addOrderItem(OrderItem item) {
    this.orderItems.add(item);
    return this;
  }

  public QueryFilterContext combine(QueryFilterContext other) {
    this.conditions.addAll(other.conditions);
    this.binds.addAll(other.binds);
    this.orderItems.addAll(other.orderItems);
    return this;
  }

  public static QueryFilterContext combine(List<QueryFilterContext> contexts) {
    var ret = new QueryFilterContext();
    for (final var context : contexts) {
      ret = ret.combine(context);
    }
    return ret;
  }
}
