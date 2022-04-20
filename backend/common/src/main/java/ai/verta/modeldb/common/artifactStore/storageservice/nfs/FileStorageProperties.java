package ai.verta.modeldb.common.artifactStore.storageservice.nfs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {
  private final String uploadDir;

  public FileStorageProperties(String uploadDir) {
    this.uploadDir = uploadDir;
  }

  public String getUploadDir() {
    return uploadDir;
  }

}
