package ai.verta.modeldb.common.futures;

import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

public class InternalJdbi {
  private final Jdbi jdbi;
  private static final Logger LOGGER = LogManager.getLogger(InternalJdbi.class);

  public InternalJdbi(Jdbi jdbi) {
    this.jdbi = jdbi;
    SqlLogger sqlLogger =
        new SqlLogger() {
          @Override
          public void logAfterExecution(StatementContext context) {
            LOGGER.info(
                "sql {}, parameters {}", context.getRenderedSql(), context.getBinding().toString());
          }
        };
    jdbi.setSqlLogger(sqlLogger);
  }

  public ConfigRegistry getConfig() {
    return jdbi.getConfig();
  }

  public <C extends JdbiConfig<C>> C getConfig(Class<C> configClass) {
    return jdbi.getConfig(configClass);
  }

  public <C extends JdbiConfig<C>> void configure(Class<C> configClass, Consumer<C> configurer) {
    jdbi.configure(configClass, configurer);
  }

  public <R, X extends Exception> R withHandle(HandleCallback<R, X> callback) throws X {
    return jdbi.withHandle(h -> callback.withHandle(new Handle(h)));
  }

  public <X extends Exception> void useHandle(final HandleConsumer<X> consumer) throws X {
    jdbi.useHandle(h -> consumer.useHandle(new Handle(h)));
  }

  public <R, X extends Exception> R inTransaction(final HandleCallback<R, X> callback) throws X {
    return jdbi.inTransaction(
        TransactionIsolationLevel.SERIALIZABLE, h -> callback.withHandle(new Handle(h)));
  }

  public <X extends Exception> void useTransaction(final HandleConsumer<X> callback) throws X {

    jdbi.useTransaction(
        TransactionIsolationLevel.SERIALIZABLE, h -> callback.useHandle(new Handle(h)));
  }
}
