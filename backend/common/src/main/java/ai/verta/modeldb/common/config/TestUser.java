package ai.verta.modeldb.common.config;

public class TestUser {
  public String email;
  public String devKey;

  public void Validate(Config config, String base) throws InvalidConfigException {
    if (email == null) throw new InvalidConfigException(base + ".email", Config.MISSING_REQUIRED);
    if (devKey == null) throw new InvalidConfigException(base + ".devKey", Config.MISSING_REQUIRED);
  }
}
