package ai.verta.modeldb.configuration;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class EnableReconcilers implements Condition {
  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    return !liquibaseRunSeparately();
  }

  boolean liquibaseRunSeparately() {
    return "true".equalsIgnoreCase("true");
  }
}
