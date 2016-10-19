package edu.mit.csail.db.ml.conf;


import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.cli.*;

import java.io.File;

public class ModelDbConfig {
  private static final String CONF_OPT = "c";
  private static ModelDbConfig instance;

  public final String dbUser;
  public final String dbPassword;
  public final String dbName;
  public final String dbType;
  public final String dbHost;
  public final int dbPort;
  public final String thriftHost;
  public final int thriftPort;
  public final String fsPrefix;

  private ModelDbConfig(
    String dbUser,
    String dbPassword,
    String dbName,
    String dbType,
    String dbHost,
    String dbPort,
    String thriftHost,
    String thriftPort,
    String fsPrefix
  ) {
    this.dbUser = dbUser;
    this.dbPassword = dbPassword;
    this.dbName = dbName;
    this.dbType = dbType;
    this.dbHost = dbHost;
    this.dbPort = Integer.parseInt(dbPort);
    this.thriftHost = thriftHost;
    this.thriftPort = Integer.parseInt(thriftPort);
    this.fsPrefix = fsPrefix;
  }

  private static String getProp(Config config, String key) {
    return config.getString(String.format("modeldb.%s", key));
  }

  public static ModelDbConfig parse(String[] args) throws ParseException {
    Options options = new Options();

    options.addOption(CONF_OPT, "conf", false, "Path to configuration (.conf) file.");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    Config config = cmd.hasOption(CONF_OPT)
      ? ConfigFactory.parseFile(new File(cmd.getOptionValue(CONF_OPT)))
      : ConfigFactory.load();

    instance = new ModelDbConfig(
      getProp(config, "db.user"),
      getProp(config, "db.password"),
      getProp(config, "db.name"),
      getProp(config, "db.type"),
      getProp(config, "db.host"),
      getProp(config, "db.port"),
      getProp(config, "thrift.host"),
      getProp(config, "thrift.port"),
      getProp(config, "fs.prefix")
    );

    return instance;
  }

  public static ModelDbConfig getInstance() throws IllegalStateException {
    if (instance == null) {
      throw new IllegalStateException("Call parse() to create a ModelDbConfig before you try to access it.");
    }
    return instance;
  }
}
