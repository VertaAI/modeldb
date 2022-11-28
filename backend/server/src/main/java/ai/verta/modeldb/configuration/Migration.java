package ai.verta.modeldb.configuration;

import ai.verta.modeldb.common.MssqlMigrationUtil;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Migration {
  private final Logger LOGGER = LogManager.getLogger(Migration.class);

  /**
   * Maps the liquibase version to the equivalent migrator version. We will take the max and use it
   * to figure out what migrator version to target, in the case of switching from liquibase over to
   * the new migrator.
   */
  private static final Map<String, Integer> LIQUIBASE_VERSION_TO_MIGRATOR_VERSION =
      Map.of("db_version_2.38", 1);

  public Migration(MDBConfig mdbConfig) throws Exception {
    migrate(mdbConfig);
  }

  public void migrate(MDBConfig config) throws Exception {
    var databaseConfig = config.getDatabase();
    var modelDBHibernateUtil = ModelDBHibernateUtil.getInstance();
    modelDBHibernateUtil.initializedConfigAndDatabase(config, databaseConfig);

    LOGGER.info("Starting Migrations");
    modelDBHibernateUtil.runMigrations(
        config.getDatabase(), config.getJdbi(), "migration", LIQUIBASE_VERSION_TO_MIGRATOR_VERSION);
    LOGGER.info("Migrations Complete");

    // Initialized session factory before code migration and after liquibase migration
    modelDBHibernateUtil.createOrGetSessionFactory(databaseConfig);

    LOGGER.info("Code migration starting");
    modelDBHibernateUtil.runMigration(databaseConfig, config.getMigrations());
    LOGGER.info("Code migration done");

    if (databaseConfig.getRdbConfiguration().isMssql()) {
      MssqlMigrationUtil.migrateToUTF16ForMssql(config.getJdbi());
    }
  }
}
