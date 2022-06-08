package ai.verta.modeldb.configuration;

import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.utils.JdbiUtil;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Migration {

  private static final Logger LOGGER = LogManager.getLogger(Migration.class);

  public static boolean migrate(MDBConfig mdbConfig, JdbiUtil jdbiUtil, Executor grpcExecutor)
      throws Exception {
    var liquibaseMigration = Boolean.parseBoolean(Optional.ofNullable("true").orElse("false"));

    var modelDBHibernateUtil = ModelDBHibernateUtil.getInstance();
    if (liquibaseMigration) {
      LOGGER.info("Liquibase migration starting");
      jdbiUtil.runLiquibaseMigration();
      LOGGER.info("Liquibase migration done");

      modelDBHibernateUtil.createOrGetSessionFactory(mdbConfig.getDatabase());
      LOGGER.info("Old code migration using hibernate starting");
      modelDBHibernateUtil.runMigration(mdbConfig.getDatabase(), mdbConfig.migrations);
      LOGGER.info("Old code migration using hibernate done");

      LOGGER.info("Code migration starting");
      jdbiUtil.runMigration(grpcExecutor);
      LOGGER.info("Code migration done");

      boolean runLiquibaseSeparate =
          Boolean.parseBoolean(
              Optional.ofNullable(System.getenv(CommonConstants.RUN_LIQUIBASE_SEPARATE))
                  .orElse("false"));
      LOGGER.trace("run Liquibase separate: {}", runLiquibaseSeparate);
      if (runLiquibaseSeparate) {
        return true;
      }
    }

    modelDBHibernateUtil.createOrGetSessionFactory(mdbConfig.getDatabase());
    jdbiUtil.setIsReady();

    return false;
  }
}
