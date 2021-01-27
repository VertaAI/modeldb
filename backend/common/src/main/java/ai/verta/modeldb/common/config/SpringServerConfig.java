package ai.verta.modeldb.common.config;

import ai.verta.modeldb.config.Config;

public class SpringServerConfig {
  public int port;
  public Long shutdownTimeout = 30L;

  public void Validate(String base) throws InvalidConfigException {
    if (port == 0) throw new InvalidConfigException(base + ".port", Config.MISSING_REQUIRED);
  }
}
