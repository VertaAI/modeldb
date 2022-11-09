package ai.verta.modeldb.common.db.migration;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.zip.CRC32;
import lombok.extern.log4j.Log4j2;

@Log4j2
class MigrationTools {

  private static final int MAX_LOCK_TRIES = 15;

  static String generateLockId(String database, String otherLockInfo) {
    CRC32 crc32 = new CRC32();
    crc32.update((database + ":" + otherLockInfo).getBytes(StandardCharsets.UTF_8));
    return String.valueOf(crc32.getValue());
  }

  static void lockDatabase(MigrationDatastore datastore) throws MigrationException {
    int tries = 0;
    while (tries++ < MAX_LOCK_TRIES) {
      try {
        datastore.lock();
        return;
      } catch (SQLException e) {
        log.warn("failed to lock db. sleeping 1s then trying again. message: " + e.getMessage());
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          log.warn("Interrupted while trying to lock the database. Returning");
          return;
        }
      }
    }
    throw new MigrationException("Failed to lock the database for migrations.");
  }
}
