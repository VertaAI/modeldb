package ai.verta.modeldb.common.configuration;

import ai.verta.modeldb.common.CommonConstants;
import java.util.Optional;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.stereotype.Component;

/**
 * Condition that will allow spring beans to only be created if the web/grpc server(s) are enabled.
 */
@Component
public class ServerEnabled implements Condition {
  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    return serverIsEnabled();
  }

  public static boolean serverIsEnabled() {
    var enableLiquibase =
        Boolean.parseBoolean(
            Optional.ofNullable(System.getenv(CommonConstants.ENABLE_LIQUIBASE_MIGRATION_ENV_VAR))
                .orElse("false"));
    var runLiquibaseSeparate =
        Boolean.parseBoolean(
            Optional.ofNullable(System.getenv(CommonConstants.RUN_LIQUIBASE_SEPARATE))
                .orElse("false"));

    // We run the web/grpc servers in every case where don't both have liquibase enabled and run it
    // separately.
    return !(enableLiquibase && runLiquibaseSeparate);
  }
}
