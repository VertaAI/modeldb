package ai.verta.modeldb.common.config;

@SuppressWarnings({"squid:S100"})
public class ServiceUserConfig {
  private String email;
  private String devKey;

  public void validate(String base) throws InvalidConfigException {
    if (email == null || email.isEmpty())
      throw new InvalidConfigException(base + ".email", Config.MISSING_REQUIRED);
    if (devKey == null || devKey.isEmpty())
      throw new InvalidConfigException(base + ".devKey", Config.MISSING_REQUIRED);
  }

  public String getEmail() {
    return email;
  }

  public String getDevKey() {
    return devKey;
  }
}
