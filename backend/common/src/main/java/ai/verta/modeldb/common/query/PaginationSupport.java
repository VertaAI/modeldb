package ai.verta.modeldb.common.query;

import ai.verta.common.Pagination;
import ai.verta.modeldb.common.config.DatabaseConfig;

public class PaginationSupport {
  // getLimitString is used with JDBI queries, because the query objects do not have methods
  // to set max results and first result, so the string can be manually added to the query.
  public static String getLimitString(
      DatabaseConfig databaseConfig, int pageNumber, int pageLimit) {
    final var pageIndex = calculatePageIndex(pageNumber);
    final var offset = calculateOffset(pageIndex, pageLimit);
    if (databaseConfig.getRdbConfiguration().isMssql()) {
      return " OFFSET " + offset + " ROWS FETCH NEXT " + pageLimit + " ROWS ONLY";
    }
    return " LIMIT " + pageLimit + " OFFSET " + offset;
  }

  public static String addPagination(
      Pagination pagination, String sql, DatabaseConfig databaseConfig) {
    if (pagination.getPageNumber() != 0 && pagination.getPageLimit() != 0) {
      return sql
          + getLimitString(databaseConfig, pagination.getPageNumber(), pagination.getPageLimit());
    }
    return sql;
  }

  private static int calculatePageIndex(int pageNumber) {
    // page numbers are 1-based, SQL offset is 0-based
    return pageNumber == 0 ? 0 : pageNumber - 1;
  }

  private static int calculateOffset(int pageIndex, int pageLimit) {
    return pageLimit * pageIndex;
  }
}
