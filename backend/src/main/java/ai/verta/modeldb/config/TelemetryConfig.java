package ai.verta.modeldb.config;

public class TelemetryConfig {
  public boolean opt_out = false;
  public int frequency = 1;
  // TODO: add default consumer
  public String consumer;

  public void Validate(String base) throws InvalidConfigException {}
}
