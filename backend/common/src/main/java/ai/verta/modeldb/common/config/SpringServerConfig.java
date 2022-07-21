package ai.verta.modeldb.common.config;

@SuppressWarnings({"squid:S100"})
public class SpringServerConfig {
  private int port;
  private Long shutdownTimeout = 30L;
  private int threadCount = 32;

  public void validate(String base) throws InvalidConfigException {
    if (port == 0) throw new InvalidConfigException(base + ".port", Config.MISSING_REQUIRED);
  }

  public Long getShutdownTimeout() {
    return shutdownTimeout;
  }

  public int getPort() {
    return port;
  }

  public int getThreadCount() {
    return threadCount;
  }
}
