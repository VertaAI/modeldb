package ai.verta.modeldb.configuration;

import ai.verta.modeldb.common.MssqlMigrationUtil;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Migration {
  private final Logger LOGGER = LogManager.getLogger(Migration.class);
  private final MigrationSetupConfig migrationSetupConfig;

  public Migration(MigrationSetupConfig migrationSetupConfig) {
    this.migrationSetupConfig = migrationSetupConfig;
  }

  public void migrate() throws Exception {
    var liquibaseMigration = migrationSetupConfig.isMigration();
    var databaseConfig = migrationSetupConfig.getDatabase();
    var modelDBHibernateUtil = ModelDBHibernateUtil.getInstance();
    modelDBHibernateUtil.initializedConfigAndDatabase(
        migrationSetupConfig.getMdbConfig(), databaseConfig);
    if (liquibaseMigration) {
      LOGGER.info("Liquibase migration starting");
      modelDBHibernateUtil.runLiquibaseMigration(databaseConfig);
      LOGGER.info("Liquibase migration done");

      modelDBHibernateUtil.createOrGetSessionFactory(databaseConfig);

      LOGGER.info("Code migration starting");
      modelDBHibernateUtil.runMigration(databaseConfig, migrationSetupConfig.getMigrations());
      LOGGER.info("Code migration done");

      if (databaseConfig.getRdbConfiguration().isMssql()) {
        MssqlMigrationUtil.migrateToUTF16ForMssql(migrationSetupConfig.getJdbi());
      }
    }

    modelDBHibernateUtil.createOrGetSessionFactory(databaseConfig);
  }
}
