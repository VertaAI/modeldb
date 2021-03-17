package ai.verta.modeldb.common.config;

public class TestConfig {
  public DatabaseConfig database;
  public Object testUsers;

  public void Validate(Config config, String base) throws InvalidConfigException {
    if (database == null)
      throw new InvalidConfigException(base + ".database", Config.MISSING_REQUIRED);
    database.Validate(base + ".database");

    if (config.hasAuth() && testUsers == null) {
      throw new InvalidConfigException(base + ".testUsers", Config.MISSING_REQUIRED);
    }
  }
}
