package ai.verta.modeldb.common.config;

import java.util.HashMap;
import java.util.Map;

public class TestConfig {
  public DatabaseConfig database;
  public Map<String, TestUser> testUsers = new HashMap<>();

  public void Validate(Config config, String base) throws InvalidConfigException {
    if (database == null)
      throw new InvalidConfigException(base + ".database", Config.MISSING_REQUIRED);
    database.Validate(base + ".database");

    if (config.hasAuth() && testUsers == null) {
      throw new InvalidConfigException(base + ".testUsers", Config.MISSING_REQUIRED);
    }

    for (Map.Entry<String, TestUser> entry : testUsers.entrySet()) {
      entry.getValue().Validate(config, base + "." + entry.getKey());
    }
  }
}
