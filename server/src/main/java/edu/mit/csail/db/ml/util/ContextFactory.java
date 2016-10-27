package edu.mit.csail.db.ml.util;

import edu.mit.csail.db.ml.conf.ModelDbConfig;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ContextFactory {
  public static DSLContext create(String username, String password, String jdbcUrl, ModelDbConfig.DatabaseType dbType)
    throws SQLException, IllegalArgumentException {
    Connection conn = DriverManager.getConnection(jdbcUrl, username, password);

    switch (dbType) {
      case SQLITE: return DSL.using(conn, SQLDialect.SQLITE);
    }

    throw new IllegalArgumentException("Cannot connect to DatabaseType: " + dbType);
  }
}
