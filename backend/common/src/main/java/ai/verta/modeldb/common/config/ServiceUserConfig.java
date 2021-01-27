package ai.verta.modeldb.common.config;

public class ServiceUserConfig {
  public String email;
  public String devKey;

  public void Validate(String base) throws InvalidConfigException {
    if (email == null || email.isEmpty())
      throw new InvalidConfigException(base + ".email", Config.MISSING_REQUIRED);
    if (devKey == null || devKey.isEmpty())
      throw new InvalidConfigException(base + ".devKey", Config.MISSING_REQUIRED);
  }
}
