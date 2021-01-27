package ai.verta.modeldb.common.config;

public class CronJobConfig {
  public int initial_delay = 30;
  public int frequency = 10;
  public int record_update_limit = 100;

  public void Validate(String base) throws InvalidConfigException {}
}
