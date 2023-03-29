package ai.verta.modeldb.common;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.Handle;
import ai.verta.modeldb.common.futures.InternalFuture;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MssqlMigrationUtil {

  private static final Logger LOGGER = LogManager.getLogger(MssqlMigrationUtil.class);
  private static final String TABLE_NAME = "tableName";

  private MssqlMigrationUtil() {}

  public static void migrateToUTF16ForMssql(FutureJdbi futureJdbi) {
    InternalFuture<Void> result =
        futureJdbi.useTransaction(
            handle -> {
              LOGGER.debug("Fetching column to change column type");
              List<Map<String, Object>> returnResults = fetchChangeColumnsListForMSSQL(handle);
              LOGGER.debug("Fetched column to change column type: {}", returnResults.size());

              if (returnResults.isEmpty()) {
                return;
              }

              LOGGER.debug("Fetch foreign key constraints");
              Map<String, Map<String, Map.Entry<String, String>>> fkConstraintsMap =
                  getAllTableForeignKeyConstraints(handle);
              LOGGER.debug("foreign key constraints fetched: {}", fkConstraintsMap.size());
              LOGGER.debug("deleting foreign key constraints");
              deleteForeignKeyConstraints(handle, fkConstraintsMap);
              LOGGER.debug("foreign key constraints deleted");

              LOGGER.debug("Fetch default key constraints");
              Map<String, Map<String, Map.Entry<String, String>>> dkConstraintsMap =
                  getAllTableDefaultKeyConstraints(handle);
              LOGGER.debug("default key constraints fetched: {}", dkConstraintsMap.size());

              LOGGER.debug("deleting default key constraints");
              deleteDefaultKeyConstraints(handle, dkConstraintsMap);
              LOGGER.debug("default key constraints deleted");

              LOGGER.debug("Fetch primary key constraints");
              Map<String, Map.Entry<String, Set<String>>> tableWisePrimarySet =
                  getAllTablePrimaryAndUniqueConstraints(handle);
              LOGGER.debug("primary key constraints fetched: {}", tableWisePrimarySet.size());

              LOGGER.debug("deleting primary key constraints");
              deletePrimaryAndUniqueConstraints(handle, tableWisePrimarySet);
              LOGGER.debug("primary key constraints deleted");

              LOGGER.debug("Fetch table indexes");
              Map<String, Map<String, Set<String>>> tableWiseIndexesMap =
                  getAllTableIndexesForMSSQL(handle);
              LOGGER.debug("table indexes fetched: {}", tableWiseIndexesMap.size());

              LOGGER.debug("deleting table indexes");
              dropAllTableIndexes(handle, tableWiseIndexesMap);
              LOGGER.debug("table indexes deleted");

              LOGGER.debug("Migrating all string column with to support MSSQL");
              migrateAllColumnsToSupportMSSQL(handle, returnResults);
              LOGGER.debug("Migrated all string column with to support MSSQL");

              LOGGER.debug("recreating primary keys constraints");
              recreatePrimaryAndUniqueConstraints(handle, tableWisePrimarySet);
              LOGGER.debug("primary keys constraints recreated successfully");

              LOGGER.debug("recreating foreign keys constraints");
              recreateAllForeignKeyConstraints(handle, fkConstraintsMap);
              LOGGER.debug("foreign keys constraints recreated successfully");

              LOGGER.debug("recreating default keys constraints");
              recreateAllDefaultKeyConstraints(handle, dkConstraintsMap);
              LOGGER.debug("default keys constraints recreated successfully");

              LOGGER.debug("recreating all tables indexes");
              recreateAllTableIndexes(handle, tableWiseIndexesMap);
              LOGGER.debug("all tables indexes recreated successfully");
            });
    try {
      result.blockAndGet();
    } catch (Exception e) {
      throw new ModelDBException(e);
    }
  }

  private static void recreatePrimaryAndUniqueConstraints(
      Handle handle, Map<String, Map.Entry<String, Set<String>>> tableWisePrimarySet) {
    for (Map.Entry<String, Map.Entry<String, Set<String>>> primaryKeyConstraint :
        tableWisePrimarySet.entrySet()) {
      var constraintName = primaryKeyConstraint.getKey();
      var tableName = primaryKeyConstraint.getValue().getKey();
      var type = "PK"; // primary_key_constraints
      String query =
          "IF (OBJECT_ID('%s', '%s') IS NULL) BEGIN "
              + "ALTER TABLE \"%s\" ADD CONSTRAINT %s PRIMARY KEY (%s) "
              + "END";
      if (constraintName.toLowerCase().startsWith("ck_")
          || constraintName.toLowerCase().startsWith("uq_")) {
        type = "UQ"; // unique_constraints
        query =
            "IF (OBJECT_ID('%s', '%s') IS NULL) BEGIN "
                + "ALTER TABLE \"%s\" ADD CONSTRAINT %s UNIQUE (%s) "
                + "END";
      }
      final String format =
          String.format(
              query,
              constraintName,
              type,
              tableName,
              constraintName,
              primaryKeyConstraint.getValue().getValue().stream()
                  .map(value -> String.format("\"%s\"", value))
                  .collect(Collectors.joining(",")));
      try (var updateQuery = handle.createUpdate(format)) {
        updateQuery.execute();
      }
    }
  }

  private static void recreateAllForeignKeyConstraints(
      Handle handle,
      Map<String, Map<String, Map.Entry<String, String>>> foreignKeyAndDefaultKeyConstraintsMap) {
    for (Map.Entry<String, Map<String, Map.Entry<String, String>>> tableConstraintsMap :
        foreignKeyAndDefaultKeyConstraintsMap.entrySet()) {
      Map.Entry<String, String> baseTableMap = tableConstraintsMap.getValue().get(TABLE_NAME);
      Map.Entry<String, String> refTableMap = tableConstraintsMap.getValue().get("refTableName");
      try (var updateQuery =
          handle.createUpdate(
              String.format(
                  "IF (OBJECT_ID('%s', 'F') IS NULL) BEGIN "
                      + "ALTER TABLE \"%s\" ADD CONSTRAINT \"%s\" FOREIGN KEY (%s) REFERENCES \"%s\"(%s) "
                      + "END",
                  tableConstraintsMap.getKey(),
                  baseTableMap.getKey(),
                  tableConstraintsMap.getKey(),
                  baseTableMap.getValue(),
                  refTableMap.getKey(),
                  refTableMap.getValue()))) {
        updateQuery.execute();
      }
    }
  }

  private static void recreateAllDefaultKeyConstraints(
      Handle handle,
      Map<String, Map<String, Map.Entry<String, String>>> foreignKeyAndDefaultKeyConstraintsMap) {
    for (Map.Entry<String, Map<String, Map.Entry<String, String>>> tableConstraintsMap :
        foreignKeyAndDefaultKeyConstraintsMap.entrySet()) {
      for (Map.Entry<String, Map.Entry<String, String>> entry :
          tableConstraintsMap.getValue().entrySet())
        try (var updateQuery =
            handle.createUpdate(
                String.format(
                    "IF (OBJECT_ID('%s', 'D') IS NULL) BEGIN "
                        + "ALTER TABLE \"%s\" ADD CONSTRAINT \"%s\" default %s for %s "
                        + "END",
                    tableConstraintsMap.getKey(),
                    entry.getKey(),
                    tableConstraintsMap.getKey(),
                    entry.getValue().getValue(),
                    entry.getValue().getKey()))) {
          updateQuery.execute();
        }
    }
  }

  private static void recreateAllTableIndexes(
      Handle handle, Map<String, Map<String, Set<String>>> tableWiseIndexesMap) {
    for (Map.Entry<String, Map<String, Set<String>>> tableIndexesMap :
        tableWiseIndexesMap.entrySet()) {
      for (Map.Entry<String, Set<String>> indexesMap : tableIndexesMap.getValue().entrySet()) {
        try (var updateQuery =
            handle.createUpdate(
                String.format(
                    "IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = '%s') BEGIN "
                        + "CREATE NONCLUSTERED INDEX \"%s\" ON \"%s\" (%s) "
                        + "END",
                    indexesMap.getKey(),
                    indexesMap.getKey(),
                    tableIndexesMap.getKey(),
                    indexesMap.getValue().stream()
                        .map(word -> "\"" + word + "\"")
                        .collect(Collectors.joining(","))))) {
          updateQuery.execute();
        }
      }
    }
  }

  private static void migrateAllColumnsToSupportMSSQL(
      Handle handle, List<Map<String, Object>> returnResults) {
    for (Map<String, Object> result : returnResults) {
      String dataType = "nvarchar(255)";
      var maxLength =
          result.get("max_length").equals("-1") ? "(max)" : "(" + result.get("max_length") + ")";
      if (result.get("data_type").equals("varchar") || result.get("data_type").equals("text")) {
        dataType = "nvarchar" + maxLength;
      } else if (result.get("data_type").equals("char")) {
        dataType = "nchar" + maxLength;
      }

      if (!((boolean) result.get("is_nullable"))) {
        dataType += " NOT NULL ";
      }

      String tableName = String.valueOf(result.get("table"));
      String columnName = String.valueOf(result.get("column_name"));

      try (var updateQuery =
          handle.createUpdate(
              String.format(
                  "ALTER TABLE \"%s\" ALTER COLUMN \"%s\" %s;", tableName, columnName, dataType))) {
        updateQuery.execute();
      }
    }
  }

  private static void dropAllTableIndexes(
      Handle handle, Map<String, Map<String, Set<String>>> tableWiseIndexesMap) {
    for (Map.Entry<String, Map<String, Set<String>>> tableIndexesMap :
        tableWiseIndexesMap.entrySet()) {
      for (Map.Entry<String, Set<String>> indexesMap : tableIndexesMap.getValue().entrySet()) {
        final String format =
            String.format(
                "IF EXISTS (SELECT * FROM sys.indexes WHERE name = '%s') BEGIN "
                    + "DROP INDEX \"%s\" ON \"%s\" "
                    + "END",
                indexesMap.getKey(), indexesMap.getKey(), tableIndexesMap.getKey());
        try (var updateQuery = handle.createUpdate(format)) {
          updateQuery.execute();
        }
      }
    }
  }

  private static Map<String, Map<String, Set<String>>> getAllTableIndexesForMSSQL(Handle handle) {
    Map<String, Map<String, Set<String>>> tableWiseIndexesMap = new HashMap<>();
    var queryStr =
        "SELECT\n"
            + "     ix.name as [indexName],\n"
            + "     tab.name as [tableName],\n"
            + "     COL_NAME(ix.object_id, ixc.column_id) as [columnName]\n"
            + "FROM\n"
            + "     sys.indexes ix \n"
            + "INNER JOIN\n"
            + "     sys.index_columns ixc \n"
            + "        ON  ix.object_id = ixc.object_id \n"
            + "        and ix.index_id = ixc.index_id\n"
            + "INNER JOIN\n"
            + "     sys.tables tab \n"
            + "        ON ix.object_id = tab.object_id \n"
            + "WHERE\n"
            + "     ix.is_primary_key = 0          /* Remove Primary Keys */\n"
            + "     AND ix.is_unique = 0           /* Remove Unique Keys */\n"
            + "     AND ix.is_unique_constraint = 0 /* Remove Unique Constraints */\n"
            + "     AND tab.is_ms_shipped = 0      /* Remove SQL Server Default Tables */\n"
            /*+ "     AND COL_NAME(ix.object_id, ixc.column_id) IN (<columnNames>) \n"*/
            + "ORDER BY\n"
            + "    ix.name, tab.name";
    try (var query = handle.createQuery(queryStr)) {
      /*.bindList("columnNames", columnNames)*/
      query
          .map(
              (rs, ctx) -> {
                var tableName = rs.getString("tableName");
                var indexName = rs.getString("indexName");
                var columnName = rs.getString("columnName");

                Map<String, Set<String>> indexesMap = tableWiseIndexesMap.get(tableName);
                if (indexesMap == null) {
                  indexesMap = new HashMap<>();
                }
                Set<String> indexColumns = indexesMap.get(indexName);
                if (indexColumns == null) {
                  indexColumns = new HashSet<>();
                }
                indexColumns.add(columnName);
                indexesMap.put(indexName, indexColumns);
                tableWiseIndexesMap.put(tableName, indexesMap);
                return rs;
              })
          .list();
    }
    return tableWiseIndexesMap;
  }

  private static void deletePrimaryAndUniqueConstraints(
      Handle handle, Map<String, Map.Entry<String, Set<String>>> tableWisePrimarySet) {
    for (Map.Entry<String, Map.Entry<String, Set<String>>> primaryKey :
        tableWisePrimarySet.entrySet()) {
      var constraintName = primaryKey.getKey();
      var tableName = primaryKey.getValue().getKey();
      var type = "PK"; // primary_key_constraints
      if (constraintName.toLowerCase().startsWith("ck_")
          || constraintName.toLowerCase().startsWith("uq_")) {
        type = "UQ"; // unique_constraints
      }
      var queryStr =
          String.format(
              "IF (OBJECT_ID('%s', '%s') IS NOT NULL) "
                  + "BEGIN "
                  + "ALTER TABLE \"%s\" DROP CONSTRAINT \"%s\"; "
                  + "END",
              constraintName, type, tableName, constraintName);
      try (var updateQuery = handle.createUpdate(queryStr)) {
        updateQuery.execute();
      }
    }
  }

  private static Map<String, Map.Entry<String, Set<String>>> getAllTablePrimaryAndUniqueConstraints(
      Handle handle) {
    Map<String, Map.Entry<String, Set<String>>> tableWisePrimaryMap = new HashMap<>();
    var queryStr =
        "SELECT \n"
            + "    tab.name AS [table]\n"
            + "    ,constr.name AS [constraint_name]\n"
            + "    ,clmns.name AS [column_name]\n"
            + "FROM SysObjects AS tab\n"
            + "INNER JOIN SysObjects AS constr ON(constr.parent_obj = tab.id AND constr.type = 'K')\n"
            + "INNER JOIN sys.indexes AS i ON( (i.index_id > 0 and i.is_hypothetical = 0) AND (i.object_id=tab.id) AND i.name = constr.name )\n"
            + "INNER JOIN sys.index_columns AS ic ON (ic.column_id > 0 and (ic.key_ordinal > 0 or ic.partition_ordinal = 0 or ic.is_included_column != 0)) \n"
            + "                                    AND (ic.index_id=CAST(i.index_id AS int) \n"
            + "                                    AND ic.object_id=i.object_id)\n"
            + "INNER JOIN sys.columns AS clmns ON clmns.object_id = ic.object_id and clmns.column_id = ic.column_id\n"
            + "WHERE tab.xtype = 'U'  \n" // AND clmns.name IN (<columnNames>)
            + "ORDER BY tab.name";
    try (var query = handle.createQuery(queryStr)) {
      /*.bindList("columnNames", columnNames)*/
      query
          .map(
              (rs, ctx) -> {
                var tableName = rs.getString("table");
                var constraintName = rs.getString("constraint_name");
                var columnName = rs.getString("column_name");

                Map.Entry<String, Set<String>> constraintsMap =
                    tableWisePrimaryMap.get(constraintName);
                Set<String> columns;
                if (constraintsMap == null) {
                  columns = new HashSet<>();
                  columns.add(columnName);
                } else {
                  columns = constraintsMap.getValue();
                }
                columns.add(columnName);
                tableWisePrimaryMap.put(
                    constraintName, new AbstractMap.SimpleEntry<>(tableName, columns));
                return rs;
              })
          .list();
    }
    return tableWisePrimaryMap;
  }

  private static void deleteForeignKeyConstraints(
      Handle handle, Map<String, Map<String, Map.Entry<String, String>>> tableWiseConstraintsMap) {
    for (Map.Entry<String, Map<String, Map.Entry<String, String>>> tableConstraintsMap :
        tableWiseConstraintsMap.entrySet()) {
      Map.Entry<String, String> baseTableMap = tableConstraintsMap.getValue().get("tableName");
      var queryStr =
          String.format(
              "IF (OBJECT_ID('%s', 'F') IS NOT NULL) BEGIN "
                  + "ALTER TABLE %s DROP CONSTRAINT \"%s\"; "
                  + "END",
              tableConstraintsMap.getKey(), baseTableMap.getKey(), tableConstraintsMap.getKey());
      try (var updateQuery = handle.createUpdate(queryStr)) {
        updateQuery.execute();
      }
    }
  }

  private static void deleteDefaultKeyConstraints(
      Handle handle, Map<String, Map<String, Map.Entry<String, String>>> tableWiseConstraintsMap) {
    for (Map.Entry<String, Map<String, Map.Entry<String, String>>> tableConstraintsMap :
        tableWiseConstraintsMap.entrySet()) {
      for (Map.Entry<String, Map.Entry<String, String>> constraintEntry :
          tableConstraintsMap.getValue().entrySet()) {
        var queryStr =
            String.format(
                "IF (OBJECT_ID('%s', 'D') IS NOT NULL) BEGIN "
                    + "ALTER TABLE \"%s\" DROP CONSTRAINT \"%s\"; "
                    + "END",
                tableConstraintsMap.getKey(),
                constraintEntry.getKey(),
                tableConstraintsMap.getKey());
        try (var updateQuery = handle.createUpdate(queryStr)) {
          updateQuery.execute();
        }
      }
    }
  }

  private static Map<String, Map<String, Map.Entry<String, String>>>
      getAllTableForeignKeyConstraints(Handle handle) {
    Map<String, Map<String, Map.Entry<String, String>>> tableWiseConstraintsMap = new HashMap<>();
    var queryStr =
        "SELECT  obj.name AS FK_NAME,\n"
            + "    tab1.name AS [tableName],\n"
            + "    col1.name AS [column],\n"
            + "    tab2.name AS [referenced_table],\n"
            + "    col2.name AS [referenced_column]\n"
            + "FROM sys.foreign_key_columns fkc\n"
            + "INNER JOIN sys.objects obj\n"
            + "    ON obj.object_id = fkc.constraint_object_id\n"
            + "INNER JOIN sys.tables tab1\n"
            + "    ON tab1.object_id = fkc.parent_object_id\n"
            + "INNER JOIN sys.schemas sch\n"
            + "    ON tab1.schema_id = sch.schema_id\n"
            + "INNER JOIN sys.columns col1\n"
            + "    ON col1.column_id = parent_column_id AND col1.object_id = tab1.object_id\n"
            + "INNER JOIN sys.tables tab2\n"
            + "    ON tab2.object_id = fkc.referenced_object_id\n"
            + "INNER JOIN sys.columns col2\n"
            + "    ON col2.column_id = referenced_column_id AND col2.object_id = tab2.object_id ";
    try (var query = handle.createQuery(queryStr)) { // WHERE col1.name IN (<columnNames>)
      /*.bindList("columnNames", columnNames)*/
      query
          .map(
              (rs, ctx) -> {
                var fkName = rs.getString("FK_NAME");
                var tableName = rs.getString("tableName");
                var refTableName = rs.getString("referenced_table");
                var columnName = rs.getString("column");
                var refColumnName = rs.getString("referenced_column");

                Map<String, Map.Entry<String, String>> constraintsMap =
                    tableWiseConstraintsMap.get(fkName);
                if (constraintsMap == null) {
                  constraintsMap = new HashMap<>();
                }
                constraintsMap.put(
                    "tableName", new AbstractMap.SimpleEntry<>(tableName, columnName));
                constraintsMap.put(
                    "refTableName", new AbstractMap.SimpleEntry<>(refTableName, refColumnName));
                tableWiseConstraintsMap.put(fkName, constraintsMap);
                return rs;
              })
          .list();
    }
    return tableWiseConstraintsMap;
  }

  private static Map<String, Map<String, Map.Entry<String, String>>>
      getAllTableDefaultKeyConstraints(Handle handle) {
    Map<String, Map<String, Map.Entry<String, String>>> tableWiseConstraintsMap = new HashMap<>();
    var queryStr =
        "SELECT  \n"
            + "        b.name AS tableName,\n"
            + "        d.name AS columnName,\n"
            + "        a.name AS CONSTRAINT_NAME,\n"
            + "        c.text AS DEFAULT_VALUE\n"
            + "FROM sys.sysobjects a INNER JOIN\n"
            + "        (SELECT name, id\n"
            + "         FROM sys.sysobjects \n"
            + "         WHERE xtype = 'U') b on (a.parent_obj = b.id)\n"
            + "                      INNER JOIN sys.syscomments c ON (a.id = c.id)\n"
            + "                      INNER JOIN sys.syscolumns d ON (d.cdefault = a.id)                                          \n"
            + " WHERE a.xtype = 'D'        \n"
            + " ORDER BY b.name, a.name";
    try (var query = handle.createQuery(queryStr)) {
      query
          .map(
              (rs, ctx) -> {
                var fkName = rs.getString("CONSTRAINT_NAME");
                var tableName = rs.getString("tableName");
                var columnName = rs.getString("columnName");
                var defaultValue = rs.getString("DEFAULT_VALUE");

                Map<String, Map.Entry<String, String>> constraintsMap =
                    tableWiseConstraintsMap.get(fkName);
                if (constraintsMap == null) {
                  constraintsMap = new HashMap<>();
                }
                constraintsMap.put(
                    tableName, new AbstractMap.SimpleEntry<>(columnName, defaultValue));
                tableWiseConstraintsMap.put(fkName, constraintsMap);
                return rs;
              })
          .list();
    }
    return tableWiseConstraintsMap;
  }

  private static List<Map<String, Object>> fetchChangeColumnsListForMSSQL(Handle handle) {
    var queryStr =
        "select t.name as [table], c.column_id, "
            + "c.name as column_name, c.is_nullable, type_name(user_type_id) as data_type, max_length , c.object_id "
            + "from sys.columns c join sys.tables t on t.object_id = c.object_id "
            + "where type_name(user_type_id) in ('text', 'varchar', 'char') order by [table], c.column_id;";
    try (var query = handle.createQuery(queryStr)) {
      return query
          .map(
              (rs, ctx) -> {
                Map<String, Object> objects = new HashMap<>();
                objects.put("table", rs.getString("table"));
                objects.put("column_id", rs.getString("column_id"));
                objects.put("column_name", rs.getString("column_name"));
                objects.put("is_nullable", rs.getBoolean("is_nullable"));
                objects.put("data_type", rs.getString("data_type"));
                objects.put("max_length", rs.getString("max_length"));
                objects.put("object_id", rs.getString("object_id"));
                return objects;
              })
          .list();
    }
  }
}
