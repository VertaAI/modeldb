package ai.verta.modeldb.config;

public class TestConfig {
  public DatabaseConfig database;
  public Object testUsers;

  public void Validate(String base) throws InvalidConfigException {
    if (database == null)
      throw new InvalidConfigException(base + ".database", Config.MISSING_REQUIRED);
    database.Validate(base + ".database");

    if (Config.getInstance().hasAuth() && testUsers == null) {
      throw new InvalidConfigException(base + ".testUsers", Config.MISSING_REQUIRED);
    }
  }
}
