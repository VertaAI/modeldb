package ai.verta.modeldb.common.configuration;

import ai.verta.modeldb.common.CommonConstants;
import java.util.Optional;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration
public class MigrationSetupConfig implements Condition {

  public MigrationSetupConfig() {}

  public boolean isMigration() {
    return Boolean.parseBoolean(
        Optional.ofNullable(System.getenv(CommonConstants.ENABLE_LIQUIBASE_MIGRATION_ENV_VAR))
            .orElse("false"));
  }

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    return !isMigration();
  }
}
