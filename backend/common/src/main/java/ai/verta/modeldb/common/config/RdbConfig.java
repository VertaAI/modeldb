package ai.verta.modeldb.common.config;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.dialect.SQLServer2008Dialect;

@SuppressWarnings({"squid:S116", "squid:S100"})
public class RdbConfig {
  private static final Logger LOGGER = LogManager.getLogger(RdbConfig.class);

  private String RdbDatabaseName;
  // TODO: replace driver with "io.opentracing.contrib.jdbc.TracingDriver" if tracing is enabled
  private String RdbDriver;
  private String RdbDialect;
  private String RdbUrl;
  private String RdbUsername;
  private String RdbPassword;
  private String sslMode = "DISABLED";
  private Boolean sslEnabled = false;

  public void Validate(String base) throws InvalidConfigException {
    if (RdbDatabaseName == null || RdbDatabaseName.isEmpty())
      throw new InvalidConfigException(base + ".RdbDatabaseName", Config.MISSING_REQUIRED);
    if (RdbDriver == null || RdbDriver.isEmpty())
      throw new InvalidConfigException(base + ".RdbDriver", Config.MISSING_REQUIRED);
    if (RdbDialect == null || RdbDialect.isEmpty())
      throw new InvalidConfigException(base + ".RdbDialect", Config.MISSING_REQUIRED);
    if (RdbUrl == null || RdbUrl.isEmpty())
      throw new InvalidConfigException(base + ".RdbUrl", Config.MISSING_REQUIRED);
    if (RdbUsername == null || RdbUsername.isEmpty())
      throw new InvalidConfigException(base + ".RdbUsername", Config.MISSING_REQUIRED);
    if (sslMode == null || sslMode.isEmpty())
      throw new InvalidConfigException(base + ".sslMode", Config.MISSING_REQUIRED);
    if (!isPostgres() && !isMysql() && !isMssql()) {
      throw new InvalidConfigException(base + ".RdbDialect", "Unknown or unsupported dialect.");
    }
  }

  public boolean isPostgres() {
    return RdbDialect.equals(PostgreSQL82Dialect.class.getName());
  }

  public boolean isMysql() {
    return RdbDialect.equals(MySQL5Dialect.class.getName());
  }

  public boolean isMssql() {
    return RdbDialect.equals(SQLServer2008Dialect.class.getName());
  }

  public static String buildDatabaseConnectionString(RdbConfig rdb) {
    if (rdb.isMssql()) {
      return rdb.RdbUrl + ";databaseName=" + rdb.getRdbDatabaseName();
    }
    if (rdb.isPostgres() && rdb.RdbUrl.endsWith("/")) {
      rdb.RdbUrl = rdb.RdbUrl.substring(0, rdb.RdbUrl.length() - 1);
    }
    final var url =
        rdb.RdbUrl
            + "/"
            + rdb.getRdbDatabaseName()
            + "?createDatabaseIfNotExist=true&useUnicode=yes&characterEncoding=UTF-8"
            + "&sslEnabled="
            + rdb.sslEnabled
            + "&sslMode="
            + rdb.sslMode;
    LOGGER.trace("Using db URL: {}", url);
    return url;
  }

  public static String buildDatabaseServerConnectionString(RdbConfig rdb) {
    if (rdb.isMssql()) {
      return rdb.RdbUrl;
    }
    if (rdb.isPostgres()) {
      // '/' is compulsory at the end of host and port for postgres
      rdb.RdbUrl += (rdb.RdbUrl.endsWith("/") ? "" : "/");
    }
    final var url =
        rdb.RdbUrl
            + "?createDatabaseIfNotExist=true&useUnicode=yes&characterEncoding=UTF-8"
            + "&sslEnabled="
            + rdb.sslEnabled
            + "&sslMode="
            + rdb.sslMode;
    LOGGER.info("Using db URL: {}", url);
    return url;
  }

  public static String buildDatabaseName(RdbConfig rdb) {
    final var dbName = rdb.getRdbDatabaseName();
    if (dbName.contains("-")) {
      if (rdb.isPostgres()) {
        throw new ModelDBException("Postgres does not support database names containing -");
      }
      if (rdb.isMysql()) {
        return String.format("`%s`", dbName);
      }
      if (rdb.isMssql()) {
        return String.format("\"%s\"", dbName);
      }
    }
    return dbName;
  }

  public String getRdbDatabaseName() {
    return RdbDatabaseName;
  }

  public String getRdbDialect() {
    return RdbDialect;
  }

  public String getRdbUrl() {
    return RdbUrl;
  }

  public String getRdbUsername() {
    return RdbUsername;
  }

  public String getRdbPassword() {
    return RdbPassword;
  }

  public String getSslMode() {
    return sslMode;
  }

  public String getRdbDriver() {
    return RdbDriver;
  }
}
