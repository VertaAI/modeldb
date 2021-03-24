package ai.verta.modeldb.common.config;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.google.rpc.Code;

public class ArtifactStoreConfig {
  public String artifactStoreType;
  public boolean pickArtifactStoreHostFromConfig = false;
  public boolean enabled = true;
  public String protocol = "https";
  public String host = "";
  public NFSConfig NFS;
  public NFSEndpointConfig artifactEndpoint;

}
