package ai.verta.modeldb.common.query;

import ai.verta.modeldb.common.config.DatabaseConfig;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.jdbi.v3.core.statement.Query;

public class QueryFilterContext {
  private final List<String> conditions;
  private final List<Consumer<Query>> binds;
  private final List<OrderItem> orderItems;
  private Optional<Integer> pageNumber;
  private Optional<Integer> pageSize;

  public QueryFilterContext() {
    conditions = new CopyOnWriteArrayList<>();
    binds = new CopyOnWriteArrayList<>();
    orderItems = new CopyOnWriteArrayList<>();
    pageNumber = Optional.empty();
    pageSize = Optional.empty();
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

  public Optional<Integer> getPageNumber() {
    return pageNumber;
  }

  public Optional<Integer> getPageSize() {
    return pageSize;
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

  public QueryFilterContext setPageSize(Integer pageSize) {
    this.pageSize = Optional.ofNullable(pageSize);
    return this;
  }

  public QueryFilterContext setPageNumber(Integer pageNumber) {
    this.pageNumber = Optional.ofNullable(pageNumber);
    return this;
  }

  public QueryFilterContext combine(QueryFilterContext other) {
    this.conditions.addAll(other.conditions);
    this.binds.addAll(other.binds);
    this.orderItems.addAll(other.orderItems);
    this.pageNumber = this.pageNumber.or(() -> other.pageNumber);
    this.pageSize = this.pageSize.or(() -> other.pageSize);
    return this;
  }

  public static QueryFilterContext combine(List<QueryFilterContext> contexts) {
    var ret = new QueryFilterContext();
    for (final var context : contexts) {
      ret = ret.combine(context);
    }
    return ret;
  }

  private static long calculatePageIndex(long pageNumber) {
    // page numbers are 1-based, SQL offset is 0-based
    return pageNumber == 0 ? 0L : pageNumber - 1;
  }

  private static long calculateOffset(long pageIndex, long pageLimit) {
    return pageLimit * pageIndex;
  }

  public String getLimitString(DatabaseConfig databaseConfig) {
    if (databaseConfig.getRdbConfiguration().isMssql()) {
      return getMSSQLLimitAndOffset();
    } else {
      return getMySQLLimitAndOffset();
    }
  }

  private String getMySQLLimitAndOffset() {
    return pageSize
        .map(
            size -> {
              var ret = " LIMIT " + size;
              return ret
                  + pageNumber
                      .map(
                          number -> {
                            final var pageIndex = calculatePageIndex(number);
                            final var offset = calculateOffset(pageIndex, size);
                            return " OFFSET " + offset;
                          })
                      .orElse("");
            })
        .orElse("");
  }

  private String getMSSQLLimitAndOffset() {
    if (pageSize.isPresent() && pageSize.get() == 0) {
      return "";
    }
    return pageSize
        .map(
            size ->
                pageNumber
                        .map(
                            number -> {
                              final var pageIndex = calculatePageIndex(number);
                              final var offset = calculateOffset(pageIndex, size);
                              return " OFFSET " + offset;
                            })
                        .orElse("")
                    + " ROWS FETCH NEXT "
                    + size
                    + " ROWS ONLY")
        .orElse("");
  }
}
