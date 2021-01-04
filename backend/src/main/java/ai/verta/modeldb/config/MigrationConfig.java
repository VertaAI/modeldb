package ai.verta.modeldb.config;

public class MigrationConfig {
  public String name;
  public boolean enabled = false;
  public int record_update_limit = 100;

  public void Validate(String base) throws InvalidConfigException {
    if (name == null || name.isEmpty())
      throw new InvalidConfigException(base + ".name", Config.MISSING_REQUIRED);
  }
}
