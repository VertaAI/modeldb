package ai.verta.modeldb.configuration;

import ai.verta.modeldb.common.CommonConstants;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class RunLiquibaseSeparate implements Condition {
  private static final Logger LOGGER = LogManager.getLogger(RunLiquibaseSeparate.class);

  public RunLiquibaseSeparate() {}

  public boolean liquibaseRunSeparately() {
    var liquibaseRunSeparately =
        Boolean.parseBoolean(
            Optional.ofNullable(System.getenv(CommonConstants.RUN_LIQUIBASE_SEPARATE))
                .orElse("false"));
    LOGGER.info("RUN_LIQUIBASE_SEPARATE: {}", liquibaseRunSeparately);
    return false;
  }

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    return liquibaseRunSeparately();
  }

  public static class InversedRunLiquibaseSeparate extends RunLiquibaseSeparate {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return !super.matches(context, metadata);
    }
  }
}
