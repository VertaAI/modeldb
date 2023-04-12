package ai.verta.modeldb.common.configuration;

import ai.verta.modeldb.common.db.migration.Migration;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.stereotype.Component;

@Component
public class EnabledMigration implements Condition {
  private static final Logger LOGGER = LogManager.getLogger(EnabledMigration.class);

  private static boolean isMigration() {
    var enableLiquibaseMigrationEnvVar =
        Boolean.parseBoolean(
            Optional.ofNullable(System.getenv(Migration.ENABLE_MIGRATIONS_ENV_VAR))
                .orElse("false"));
    LOGGER.info("ENABLE_LIQUIBASE_MIGRATION_ENV_VAR: {}", enableLiquibaseMigrationEnvVar);
    return enableLiquibaseMigrationEnvVar;
  }

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    return isMigration();
  }

  public static class MigrationDisabled implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return !isMigration();
    }
  }
}
