package ai.verta.modeldb.common.configuration;

import ai.verta.modeldb.common.db.migration.Migration;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class RunLiquibaseSeparately implements Condition {
  private static final Logger LOGGER = LogManager.getLogger(RunLiquibaseSeparately.class);

  private boolean liquibaseRunSeparately() {
    var liquibaseRunSeparately =
        Boolean.parseBoolean(
            Optional.ofNullable(System.getenv(Migration.RUN_MIGRATIONS_SEPARATELY))
                .orElse("false"));
    LOGGER.info("RUN_LIQUIBASE_SEPARATE: {}", liquibaseRunSeparately);
    return liquibaseRunSeparately;
  }

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    return liquibaseRunSeparately();
  }

  public static class RunLiquibaseWithMainService extends RunLiquibaseSeparately {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return !super.matches(context, metadata);
    }
  }
}
