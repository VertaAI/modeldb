package ai.verta.modeldb.common.db.migration;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Migrator {
  private final Connection connection;
  private final String resourcesDirectory;

  public Migrator(Connection connection, String resourcesDirectory) {
    this.connection = connection;
    this.resourcesDirectory = resourcesDirectory;
  }

  public void executeMigration(Migration migration) throws IOException, SQLException {
    String sql =
        Resources.toString(
            Resources.getResource(resourcesDirectory + "/" + migration.getFilename()),
            StandardCharsets.UTF_8);
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate(sql);
    }
  }
}
