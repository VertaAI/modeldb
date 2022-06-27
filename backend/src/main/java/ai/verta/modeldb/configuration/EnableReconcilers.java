package ai.verta.modeldb.configuration;

import ai.verta.modeldb.common.CommonConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class EnableReconcilers implements Condition {
  private static final Logger LOGGER = LogManager.getLogger(EnableReconcilers.class);

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    return liquibaseMigrationDisabled() || !liquibaseRunSeparately();
  }

  boolean liquibaseMigrationDisabled() {
    var liquibaseMigrationDisabled =
        "false".equalsIgnoreCase(System.getenv(CommonConstants.ENABLE_LIQUIBASE_MIGRATION_ENV_VAR));
    LOGGER.info("liquibase migration disabled: {}", liquibaseMigrationDisabled);
    return liquibaseMigrationDisabled;
  }

  boolean liquibaseRunSeparately() {
    var liquibaseRunSeparately =
        "true".equalsIgnoreCase(System.getenv(CommonConstants.RUN_LIQUIBASE_SEPARATE));
    LOGGER.info("liquibase run separately: {}", liquibaseRunSeparately);
    return liquibaseRunSeparately;
  }
}
