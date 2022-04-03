package ai.verta.modeldb.common.futures;

import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.HandleConsumer;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

import java.util.function.Consumer;

public class InternalJdbi {
    private final Jdbi jdbi;

    public InternalJdbi(Jdbi jdbi) {
        this.jdbi = jdbi;
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
        return jdbi.withHandle(callback);
    }

    public <X extends Exception> void useHandle(final HandleConsumer<X> consumer) throws X {
        jdbi.useHandle(consumer);
    }

    public <R, X extends Exception> R inTransaction(final HandleCallback<R, X> callback) throws X {
        return jdbi.inTransaction(TransactionIsolationLevel.SERIALIZABLE, callback);
    }

    public <X extends Exception> void useTransaction(final HandleConsumer<X> callback) throws X {
        jdbi.useTransaction(TransactionIsolationLevel.SERIALIZABLE, callback);
    }
}
