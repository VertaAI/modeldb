package ai.verta.modeldb.common.config;

@SuppressWarnings({"squid:S116", "squid:S100"})
public class GrpcServerConfig {
  private int port;
  private int threadCount = 8;
  private int requestTimeout = 30;
  private int metrics_port = 8087;
  private Integer maxInboundMessageSize = 4194304; // bytes

  public void Validate(String base) throws InvalidConfigException {
    if (port == 0) throw new InvalidConfigException(base + ".port", Config.MISSING_REQUIRED);
  }

  public int getPort() {
    return port;
  }

  public int getThreadCount() {
    return threadCount;
  }

  public int getRequestTimeout() {
    return requestTimeout;
  }

  public Integer getMaxInboundMessageSize() {
    return maxInboundMessageSize;
  }
}
