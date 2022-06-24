package ai.verta.modeldb.configuration;

import ai.verta.modeldb.common.CommonConstants;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class EnableReconcilers implements Condition {
  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    return liquibaseMigrationDisabled() || !liquibaseRunSeparately();
  }

  boolean liquibaseMigrationDisabled() {
    return "false"
        .equalsIgnoreCase(System.getenv(CommonConstants.ENABLE_LIQUIBASE_MIGRATION_ENV_VAR));
  }

  boolean liquibaseRunSeparately() {
    return "true".equalsIgnoreCase(System.getenv(CommonConstants.RUN_LIQUIBASE_SEPARATE));
  }
}
