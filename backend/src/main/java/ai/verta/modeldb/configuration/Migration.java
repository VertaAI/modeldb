package ai.verta.modeldb.configuration;

import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.MssqlMigrationUtil;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Migration {

  private static final Logger LOGGER = LogManager.getLogger(Migration.class);

  public static boolean migrate(MDBConfig mdbConfig) throws Exception {
    var liquibaseMigration =
        Boolean.parseBoolean(
            Optional.ofNullable(System.getenv(CommonConstants.ENABLE_LIQUIBASE_MIGRATION_ENV_VAR))
                .orElse("false"));
    var databaseConfig = mdbConfig.getDatabase();
    var modelDBHibernateUtil = ModelDBHibernateUtil.getInstance();
    modelDBHibernateUtil.initializedConfigAndDatabase(mdbConfig, databaseConfig);
    if (liquibaseMigration) {
      LOGGER.info("Liquibase migration starting");
      modelDBHibernateUtil.runLiquibaseMigration(databaseConfig);
      LOGGER.info("Liquibase migration done");

      modelDBHibernateUtil.createOrGetSessionFactory(databaseConfig);

      LOGGER.info("Code migration starting");
      modelDBHibernateUtil.runMigration(databaseConfig, mdbConfig.migrations);
      LOGGER.info("Code migration done");

      if (databaseConfig.getRdbConfiguration().isMssql()) {
        MssqlMigrationUtil.migrateToUTF16ForMssql(mdbConfig.getJdbi());
      }

      var runLiquibaseSeparate =
          Boolean.parseBoolean(
              Optional.ofNullable(System.getenv(CommonConstants.RUN_LIQUIBASE_SEPARATE))
                  .orElse("false"));
      LOGGER.trace("run Liquibase separate: {}", runLiquibaseSeparate);
      if (runLiquibaseSeparate) {
        return true;
      }
    }

    modelDBHibernateUtil.createOrGetSessionFactory(databaseConfig);
    return false;
  }
}
