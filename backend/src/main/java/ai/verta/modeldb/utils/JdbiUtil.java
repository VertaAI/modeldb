package ai.verta.modeldb.utils;

import ai.verta.modeldb.common.CommonDBUtil;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.MssqlMigrationUtil;
import ai.verta.modeldb.config.MDBConfig;
import io.grpc.health.v1.HealthCheckResponse;
import java.sql.SQLException;
import java.util.concurrent.Executor;
import liquibase.exception.LiquibaseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.exception.JDBCConnectionException;

public class JdbiUtil extends CommonDBUtil {
  private static final Logger LOGGER = LogManager.getLogger(JdbiUtil.class);
  private static JdbiUtil jdbiUtil;
  private static String liquibaseRootFilePath;
  private static boolean isReady = false;
  private static MDBConfig config;

  private JdbiUtil() {}

  public static synchronized JdbiUtil getInstance(MDBConfig mdbConfig)
      throws InterruptedException, SQLException {
    if (jdbiUtil == null) {
      jdbiUtil = new JdbiUtil();
      config = mdbConfig;
      liquibaseRootFilePath = "\\src\\main\\resources\\liquibase\\db-changelog-master.xml";

      // Ensure the monitoring database is created
      createDBIfNotExists(mdbConfig.getDatabase().getRdbConfiguration());

      // Check mdb DB is up or not
      boolean dbConnectionStatus =
          checkDBConnection(
              mdbConfig.getDatabase().getRdbConfiguration(), mdbConfig.getDatabase().getTimeout());
      if (!dbConnectionStatus) {
        checkDBConnectionInLoop(mdbConfig.getDatabase(), true);
      }

      LOGGER.info(CommonMessages.READY_STATUS, isReady);
    }
    return jdbiUtil;
  }

  public static boolean ping() {
    boolean isValid = false;
    try (var connection = getDBConnection(config.getDatabase().getRdbConfiguration())) {
      isValid = connection.isValid(config.getDatabase().getTimeout());
    } catch (JDBCConnectionException | SQLException ex) {
      LOGGER.error(
          "JdbiUtil ping() : ModelDB DB connection not found, got error: {}", ex.getMessage());
    }

    return isValid;
  }

  public static HealthCheckResponse.ServingStatus checkReady() {
    try {
      if (isReady && ping()) {
        return HealthCheckResponse.ServingStatus.SERVING;
      }
      return HealthCheckResponse.ServingStatus.NOT_SERVING;
    } catch (Exception ex) {
      LOGGER.error("Getting error on health checkReady: {}", ex.getMessage(), ex);
      return HealthCheckResponse.ServingStatus.NOT_SERVING;
    }
  }

  public static HealthCheckResponse.ServingStatus checkLive() {
    return HealthCheckResponse.ServingStatus.SERVING;
  }

  public void runLiquibaseMigration()
      throws InterruptedException, LiquibaseException, SQLException {
    runLiquibaseMigration(config.getDatabase(), liquibaseRootFilePath);

    if (config.getDatabase().getRdbConfiguration().isMssql()) {
      MssqlMigrationUtil.migrateToUTF16ForMssql(config.getJdbi());
    }
  }

  public void setIsReady() {
    isReady = true;
  }

  /**
   * If you want to define new migration then add new if check for your migration in `if (migration)
   * {` condition.
   */
  public void runMigration(Executor executor) throws Exception {
    // copy of MDB code
    int recordUpdateLimit = 100;

    LOGGER.info("Finished all the migration tasks");
  }
}
