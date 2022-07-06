package ai.verta.modeldb.configuration;

import ai.verta.modeldb.common.MssqlMigrationUtil;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Migration {
  private final Logger LOGGER = LogManager.getLogger(Migration.class);

  public Migration(MDBConfig mdbConfig) throws Exception {
    migrate(mdbConfig);
  }

  public void migrate(MDBConfig config) throws Exception {
    var databaseConfig = config.getDatabase();
    var modelDBHibernateUtil = ModelDBHibernateUtil.getInstance();
    modelDBHibernateUtil.initializedConfigAndDatabase(config, databaseConfig);

    LOGGER.info("Liquibase migration starting");
    modelDBHibernateUtil.runLiquibaseMigration(databaseConfig);
    LOGGER.info("Liquibase migration done");

    LOGGER.info("Code migration starting");
    modelDBHibernateUtil.runMigration(databaseConfig, config.migrations);
    LOGGER.info("Code migration done");

    if (databaseConfig.getRdbConfiguration().isMssql()) {
      MssqlMigrationUtil.migrateToUTF16ForMssql(config.getJdbi());
    }
  }
}
