package ai.verta.modeldb.common.config;

@SuppressWarnings({"squid:S116", "squid:S100"})
public class CronJobConfig {
  private int initial_delay = 30;
  private int frequency = 10;
  private int record_update_limit = 100;

  public void Validate(String base) throws InvalidConfigException {
    // Do nothing
  }

  public int getInitial_delay() {
    return initial_delay;
  }

  public int getFrequency() {
    return frequency;
  }

  public int getRecord_update_limit() {
    return record_update_limit;
  }
}
