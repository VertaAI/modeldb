package ai.verta.modeldb.common.db.migration;

import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

public class H2MigrationDatastore implements MigrationDatastore {
  private final ReentrantLock lock = new ReentrantLock();

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
}
