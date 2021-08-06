package ai.verta.modeldb.common.config;

public class SpringServerConfig {
  public int port;
  public Long shutdownTimeout = 30L;
  public int threadCount = 32;

  public void Validate(String base) throws InvalidConfigException {
    if (port == 0) throw new InvalidConfigException(base + ".port", Config.MISSING_REQUIRED);
  }
}
