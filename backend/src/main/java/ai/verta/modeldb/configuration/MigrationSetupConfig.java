package ai.verta.modeldb.configuration;

import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.config.DatabaseConfig;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.config.MigrationConfig;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class MigrationSetupConfig implements Condition {
  private static final Logger LOGGER = LogManager.getLogger(MigrationSetupConfig.class);

  public MigrationSetupConfig() {}

  private MDBConfig mdbConfig;

  public MigrationSetupConfig(MDBConfig mdbConfig) {
    this.mdbConfig = mdbConfig;
  }

  public MDBConfig getMdbConfig() {
    return mdbConfig;
  }

  public DatabaseConfig getDatabase() {
    return mdbConfig.getDatabase();
  }

  public List<MigrationConfig> getMigrations() {
    return mdbConfig.migrations;
  }

  public FutureJdbi getJdbi() {
    return mdbConfig.getJdbi();
  }

  public boolean isMigration() {
    var enableLiquibaseMigrationEnvVar =
        Boolean.parseBoolean(
            Optional.ofNullable(System.getenv(CommonConstants.ENABLE_LIQUIBASE_MIGRATION_ENV_VAR))
                .orElse("false"));
    LOGGER.info("ENABLE_LIQUIBASE_MIGRATION_ENV_VAR: {}", enableLiquibaseMigrationEnvVar);
    return enableLiquibaseMigrationEnvVar;
  }

  public boolean liquibaseRunSeparately() {
    var liquibaseRunSeparately =
        Boolean.parseBoolean(
            Optional.ofNullable(System.getenv(CommonConstants.RUN_LIQUIBASE_SEPARATE))
                .orElse("false"));
    LOGGER.info("RUN_LIQUIBASE_SEPARATE: {}", liquibaseRunSeparately);
    return liquibaseRunSeparately;
  }

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    return !liquibaseRunSeparately();
  }
}
