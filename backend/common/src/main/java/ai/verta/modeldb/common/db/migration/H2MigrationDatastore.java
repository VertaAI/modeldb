package ai.verta.modeldb.common.db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

public class H2MigrationDatastore implements MigrationDatastore {
  private final ReentrantLock lock = new ReentrantLock();
  private final Connection connection;

  public H2MigrationDatastore(Connection connection) {
    this.connection = connection;
  }

  @Override
  public void lock() throws SQLException {
    if (!lock.tryLock()) {
      throw new SQLException("Failed to lock the H2 database");
    }
  }

  @Override
  public void unlock() {
    lock.unlock();
  }

  @Override
  public void ensureMigrationTableExists() throws SQLException {
    try (PreparedStatement ps =
        connection.prepareStatement(
            "CREATE TABLE IF NOT EXISTS "
                + MigrationDatastore.SCHEMA_MIGRATIONS_TABLE
                + " (version bigint not null primary key, dirty boolean not null)")) {
      ps.execute();
    }
  }
}
