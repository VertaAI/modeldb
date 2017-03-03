package edu.mit.csail.db.ml.conf;


import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.cli.*;

import java.io.File;

/**
 * Represents the configuration used to set-up the ModelDB Server.
 * The configuration is specified in a file like in [ModelDB_Dir]/server/src/main/resources/reference.conf.
 *
 * Then, this class can be used to parse the file and read the configuration.
 *
 * This is a SINGLETON class.
 */
public class ModelDbConfig {
  /**
   * The type of the database. Currently, only SQLite is supported. However, JOOQ (the library used to interact with
   * the database) supports other relational databases like MySQL and PostgreSQL.
   */
  public enum DatabaseType {
    SQLITE
  }

  public enum MetadataDbType {
    MONGODB
  }

  /**
   * This is the name of the command line argument that will be parsed to read the path of the configuration file.
   */
  private static final String CONF_OPT = "conf";

  /**
   * The singleton instance of the configuration object.
   */
  private static ModelDbConfig instance;

  /**
   * The username used to connect to the database.
   */
  public final String dbUser;

  /**
   * The password used to connect to the database.
   */
  public final String dbPassword;

  /**
   * The hostname to launch the Thrift server.
   */
  public final String thriftHost;

  /**
   * The type of the database to connect to.
   */
  public final DatabaseType dbType;

  /**
   * The JDBC URL of the database.
   */
  public final String jbdcUrl;

  /**
   * The JDBC URL of the database used for running tests.
   */
  public final String jbdcTestUrl;

  /**
   * The port on which to launch the Thrift server.
   */
  public final int thriftPort;

  /* *
   * Host for metadataDb
   */
  public final String metadataDbHost;

  /**
   * Port on which metadataDb is running
   */
  public final int metadataDbPort;

  /**
   * Name of database inside metadataDb
   */
  public final String metadataDbName;

  /**
   * Name of database inside metadataDb
   */
  public final String metadataDbTestDbName;

  /**
   * Type of metadata database
   */
  public final MetadataDbType metadataDbType;

  /**
   * ModelDB Server allows the user to store models in a filesystem. ModelDB Server generates
   * filepaths at which the user can store their models. Each filename is prefixed with the given
   * prefix string to create the filepath.
   */
  public final String fsPrefix;

  /**
   * Creates a configuration object.
   */
  private ModelDbConfig(
    String dbUser,
    String dbPassword,
    String jdbcUrl,
    String jdbcTestUrl,
    String databaseType,
    String thriftHost,
    String thriftPort,
    String metadataDbHost,
    String metadataDbPort,
    String metadataDbName,
    String metadataDbTestDbName,
    String metadataDbType,
    String fsPrefix
  ) {
    this.dbUser = dbUser;
    this.dbPassword = dbPassword;
    this.jbdcUrl = jdbcUrl;
    this.jbdcTestUrl = jdbcTestUrl;
    this.thriftHost = thriftHost;
    this.thriftPort = Integer.parseInt(thriftPort);
    this.metadataDbHost = metadataDbHost;
    this.metadataDbPort = Integer.parseInt(metadataDbPort);
    this.metadataDbName = metadataDbName;
    this.metadataDbTestDbName = metadataDbTestDbName;
    this.fsPrefix = fsPrefix;

    switch (databaseType) {
      case "sqlite": this.dbType = DatabaseType.SQLITE; break;
      default: throw new IllegalArgumentException("Not a value databaseType");
    }

    switch (metadataDbType) {
      case "mongodb": this.metadataDbType = MetadataDbType.MONGODB; break;
      default: throw new IllegalArgumentException("Not a value metadataDbType");
    }
  }

  /**
   * Read the key "modeldb.[keyname]" from the given configuration object.
   * @param config - The configuration object.
   * @param key - Name of the key to lookup.
   * @return The value of the given key.
   */
  private static String getProp(Config config, String key) {
    return config.getString(String.format("modeldb.%s", key));
  }

  /**
   * Parse command line arguments and create the singleton ModelDbConfig object.
   * @param args - The command line arguments. If the CONF_OPT option is present, then it will be used as the
   *             path of the configuration file. Otherwise, the [ModelDB_Dir]/server/src/main/resources/reference.conf
   *             will be read.
   * @return The singleton configuration object.
   */
  public static ModelDbConfig parse(String[] args) throws ParseException {
    Options options = new Options();

    options.addOption(
      Option.builder()
        .argName(CONF_OPT)
        .longOpt(CONF_OPT)
        .desc("Path to configuration (.conf) file.")
        .optionalArg(true)
        .numberOfArgs(1)
        .build()
    );

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    // If the configuration file is given, read from that file. Otherwise, read from the default filepath.
    Config config = cmd.hasOption(CONF_OPT)
      ? ConfigFactory.parseFile(new File(cmd.getOptionValue(CONF_OPT)))
      : ConfigFactory.load();

    // Create the singleton object and return it.
    instance = new ModelDbConfig(
      getProp(config, "db.user"),
      getProp(config, "db.password"),
      getProp(config, "db.jdbcUrl"),
      getProp(config, "db.jdbcTestUrl"),
      getProp(config, "db.databaseType"),
      getProp(config, "thrift.host"),
      getProp(config, "thrift.port"),
      getProp(config, "metadataDb.host"),
      getProp(config, "metadataDb.port"),
      getProp(config, "metadataDb.dbName"),
      getProp(config, "metadataDb.testDbName"),
      getProp(config, "metadataDb.type"),
      getProp(config, "fs.prefix")
    );

    return instance;
  }

  /**
   * @return The singleton configuration.
   * @throws IllegalStateException Thrown if the parse(args) method has not yet been called.
   */
  public static ModelDbConfig getInstance() throws IllegalStateException {
    if (instance == null) {
      throw new IllegalStateException("Call parse() to create a ModelDbConfig before you try to access it.");
    }
    return instance;
  }
}
