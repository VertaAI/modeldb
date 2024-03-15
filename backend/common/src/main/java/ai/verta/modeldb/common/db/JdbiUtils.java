package ai.verta.modeldb.common.db;

import ai.verta.modeldb.common.config.DatabaseConfig;
import ai.verta.modeldb.common.config.RdbConfig;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalJdbi;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.jdbi.v3.core.Jdbi;

public class JdbiUtils {
  public static FutureJdbi initializeFutureJdbi(
      DatabaseConfig databaseConfig, DataSource dataSource) {
    final var jdbi = new InternalJdbi(Jdbi.create(dataSource));
    final var dbExecutor =
        FutureExecutor.initializeExecutor(databaseConfig.getThreadCount(), "jdbi");
    return new FutureJdbi(jdbi, dbExecutor);
  }

  public static FutureJdbi initializeFutureJdbiWithFixedThreadpool(
      DataSource dataSource, int poolSize) {
    var jdbi = new InternalJdbi(Jdbi.create(dataSource));

    ExecutorService executor = Executors.newFixedThreadPool(poolSize);
    var dbExecutor = FutureExecutor.makeCompatibleExecutor(executor, "jdbi");
    return new FutureJdbi(jdbi, dbExecutor);
  }

  public static DataSource initializeDataSource(DatabaseConfig databaseConfig, String poolName) {
    final var hikariDataSource = new HikariDataSource();
    final var dbUrl = RdbConfig.buildDatabaseConnectionString(databaseConfig.getRdbConfiguration());
    hikariDataSource.setJdbcUrl(dbUrl);
    hikariDataSource.setUsername(databaseConfig.getRdbConfiguration().getRdbUsername());
    hikariDataSource.setPassword(databaseConfig.getRdbConfiguration().getRdbPassword());
    hikariDataSource.setMinimumIdle(Integer.parseInt(databaseConfig.getMinConnectionPoolSize()));
    hikariDataSource.setMaximumPoolSize(
        Integer.parseInt(databaseConfig.getMaxConnectionPoolSize()));
    hikariDataSource.setRegisterMbeans(true);
    hikariDataSource.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
    hikariDataSource.setPoolName(poolName);
    hikariDataSource.setLeakDetectionThreshold(databaseConfig.getLeakDetectionThresholdMs());
    hikariDataSource.setConnectionTimeout(databaseConfig.getConnectionTimeoutMillis());
    return hikariDataSource;
  }
}
