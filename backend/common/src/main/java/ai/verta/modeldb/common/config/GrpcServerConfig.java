package ai.verta.modeldb.common.config;

public class GrpcServerConfig {
  public int port;
  public int requestTimeout = 30;
  public int metrics_port = 8087;
  public boolean quitOnAuditMissing = false;

  public void Validate(String base) throws InvalidConfigException {
    if (port == 0) throw new InvalidConfigException(base + ".port", Config.MISSING_REQUIRED);
  }
}
