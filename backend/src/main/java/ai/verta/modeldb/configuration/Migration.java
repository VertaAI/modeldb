package ai.verta.modeldb.configuration;

import ai.verta.modeldb.common.MssqlMigrationUtil;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Migration {
  private final Logger LOGGER = LogManager.getLogger(Migration.class);
  private final MigrationsIncludedInAppStartup migrationsIncludedInAppStartup;

  public Migration(MigrationsIncludedInAppStartup migrationsIncludedInAppStartup) {
    this.migrationsIncludedInAppStartup = migrationsIncludedInAppStartup;
  }

  public void migrate() throws Exception {
    var liquibaseMigration = migrationsIncludedInAppStartup.isMigration();
    var databaseConfig = migrationsIncludedInAppStartup.getDatabase();
    var modelDBHibernateUtil = ModelDBHibernateUtil.getInstance();
    modelDBHibernateUtil.initializedConfigAndDatabase(
        migrationsIncludedInAppStartup.getMdbConfig(), databaseConfig);
    if (liquibaseMigration) {
      LOGGER.info("Liquibase migration starting");
      modelDBHibernateUtil.runLiquibaseMigration(databaseConfig);
      LOGGER.info("Liquibase migration done");

      LOGGER.info("Code migration starting");
      modelDBHibernateUtil.runMigration(
          databaseConfig, migrationsIncludedInAppStartup.getMigrations());
      LOGGER.info("Code migration done");

      if (databaseConfig.getRdbConfiguration().isMssql()) {
        MssqlMigrationUtil.migrateToUTF16ForMssql(migrationsIncludedInAppStartup.getJdbi());
      }
    }
  }
}
