package ai.verta.modeldb.config;

import ai.verta.modeldb.common.config.InvalidConfigException;

public class TelemetryConfig {
  public boolean opt_out = false;
  public int frequency = 1;
  // TODO: add default consumer
  public String consumer;

  public void validate(String base) throws InvalidConfigException {
    // Do nothing
  }
}
