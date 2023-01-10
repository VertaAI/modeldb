package ai.verta.modeldb.common.config;

import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter(AccessLevel.NONE)
@SuppressWarnings({"squid:S116", "squid:S100"})
public class RdbConfig {

  private static final Logger LOGGER = LogManager.getLogger(RdbConfig.class);

  @JsonProperty private String RdbDatabaseName;
  @JsonProperty private String RdbDriver;
  @JsonProperty private String RdbDialect;
  @JsonProperty private String RdbUrl;
  @JsonProperty private String RdbUsername;
  @JsonProperty private String RdbPassword;
  @Builder.Default @JsonProperty private String sslMode = "DISABLED";
  @Builder.Default @JsonProperty private Boolean sslEnabled = false;
  @JsonProperty private String DBConnectionURL;

  public void validate(String base) throws InvalidConfigException {
    if (RdbDriver == null || RdbDriver.isEmpty()) {
      throw new InvalidConfigException(base + ".RdbDriver", CommonMessages.MISSING_REQUIRED);
    }
    if (RdbDialect == null || RdbDialect.isEmpty()) {
      throw new InvalidConfigException(base + ".RdbDialect", CommonMessages.MISSING_REQUIRED);
    }
    if (RdbUsername == null || RdbUsername.isEmpty()) {
      throw new InvalidConfigException(base + ".RdbUsername", CommonMessages.MISSING_REQUIRED);
    }
    if (!isMysql() && !isMssql() && !isH2()) {
      throw new InvalidConfigException(base + ".RdbDialect", "Unknown or unsupported dialect.");
    }

    if (DBConnectionURL == null) {
      if (RdbDatabaseName == null || RdbDatabaseName.isEmpty()) {
        throw new InvalidConfigException(
            base + ".RdbDatabaseName", CommonMessages.MISSING_REQUIRED);
      }
      if (RdbUrl == null || RdbUrl.isEmpty()) {
        throw new InvalidConfigException(base + ".RdbUrl", CommonMessages.MISSING_REQUIRED);
      }
      if (sslMode == null || sslMode.isEmpty()) {
        throw new InvalidConfigException(base + ".sslMode", CommonMessages.MISSING_REQUIRED);
      }
    }
  }

  public boolean isMysql() {
    return RdbDialect.equals("org.hibernate.dialect.MySQL5Dialect");
  }

  public boolean isMssql() {
    return RdbDialect.equals("org.hibernate.dialect.SQLServer2008Dialect");
  }

  public boolean isH2() {
    return RdbDialect.equals("org.hibernate.dialect.H2Dialect");
  }

  public static String buildDatabaseConnectionString(RdbConfig rdb) {
    if (rdb.isH2()) {
      return buildDatabaseServerConnectionString(rdb);
    }
    if (rdb.DBConnectionURL != null) {
      return rdb.DBConnectionURL;
    }

    if (rdb.isMssql()) {
      return rdb.RdbUrl + ";databaseName=" + rdb.getRdbDatabaseName();
    }
    final var url =
        rdb.RdbUrl
            + "/"
            + rdb.getRdbDatabaseName()
            + "?createDatabaseIfNotExist=true&useUnicode=yes&characterEncoding=UTF-8&allowMultiQueries=true"
            + "&sslEnabled="
            + rdb.sslEnabled
            + "&sslMode="
            + rdb.sslMode;
    LOGGER.trace("Using db URL: {}", url);
    return url;
  }

  public static String buildDatabaseServerConnectionString(RdbConfig rdb) {
    if (rdb.DBConnectionURL != null) {
      return rdb.DBConnectionURL;
    }

    if (rdb.isH2()) {
      return rdb.RdbUrl + ";DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE";
    }

    if (rdb.isMssql()) {
      return rdb.RdbUrl;
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
    String dbName = rdb.getRdbDatabaseName();
    if (rdb.getDBConnectionURL() != null) {
      dbName = getDBNameFromDBConnectionURL(rdb);
    }

    if (dbName.contains("-")) {
      if (rdb.isMysql()) {
        return String.format("`%s`", dbName);
      }
      if (rdb.isMssql()) {
        return String.format("\"%s\"", dbName);
      }
    }
    return dbName;
  }

  private static String getDBNameFromDBConnectionURL(RdbConfig rdb) {
    String regex;
    if (rdb.isMssql()) {
      // Regex reference: https://regex101.com/r/yaU0DY/1
      regex = ";databaseName=([^;]*)";
    } else {
      regex = "^jdbc:mysql:(?://[^/]+/)?(\\w+)";
    }
    Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    var dbName =
        pattern
            .matcher(rdb.DBConnectionURL)
            .results()
            .map(mr -> mr.group(1))
            .collect(Collectors.joining());

    if (dbName.isEmpty()) {
      throw new ModelDBException("Database name not found in the database connection URL");
    }
    return dbName;
  }
}
