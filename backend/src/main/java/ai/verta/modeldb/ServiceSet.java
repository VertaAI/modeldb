package ai.verta.modeldb;

import ai.verta.modeldb.artifactStore.storageservice.ArtifactStoreService;
import ai.verta.modeldb.artifactStore.storageservice.nfs.NFSService;
import ai.verta.modeldb.artifactStore.storageservice.s3.S3Service;
import ai.verta.modeldb.authservice.AuthServiceUtils;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.authservice.RoleServiceUtils;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;

import java.io.IOException;

public class ServiceSet {
  private static final Logger LOGGER = LogManager.getLogger(App.class);

  public ArtifactStoreService artifactStoreService = null;
  public AuthService authService;
  public UAC uac;
  public RoleService roleService;
  public App app;

  public static ServiceSet fromConfig(Config config) throws IOException {
    ServiceSet set = new ServiceSet();
    set.authService = AuthServiceUtils.FromConfig(config);
    set.uac = UAC.FromConfig(config);
    set.roleService = RoleServiceUtils.FromConfig(config, set.authService, set.uac);

    // Initialize App.java singleton instance
    set.app = App.getInstance();

    if (config.artifactStoreConfig.enabled) {
      set.artifactStoreService = initializeArtifactStore(config);
    } else {
      System.getProperties().put("scan.packages", "dummyPackageName");
      SpringApplication.run(App.class);
    }

    return set;
  }

  private static ArtifactStoreService initializeArtifactStore(Config config)
      throws ModelDBException, IOException {
    // ------------- Start Initialize Cloud storage base on configuration ------------------
    ArtifactStoreService artifactStoreService;

    if (config.artifactStoreConfig.artifactEndpoint != null) {
      System.getProperties()
          .put(
              "artifactEndpoint.storeArtifact",
              config.artifactStoreConfig.artifactEndpoint.storeArtifact);
      System.getProperties()
          .put(
              "artifactEndpoint.getArtifact",
              config.artifactStoreConfig.artifactEndpoint.getArtifact);
    }

    if (config.artifactStoreConfig.NFS != null
        && config.artifactStoreConfig.NFS.artifactEndpoint != null) {
      System.getProperties()
          .put(
              "artifactEndpoint.storeArtifact",
              config.artifactStoreConfig.NFS.artifactEndpoint.storeArtifact);
      System.getProperties()
          .put(
              "artifactEndpoint.getArtifact",
              config.artifactStoreConfig.NFS.artifactEndpoint.getArtifact);
    }

    switch (config.artifactStoreConfig.artifactStoreType) {
      case "S3":
        if (!config.artifactStoreConfig.S3.s3presignedURLEnabled) {
          System.setProperty(
              ModelDBConstants.CLOUD_BUCKET_NAME, config.artifactStoreConfig.S3.cloudBucketName);
          System.getProperties()
              .put("scan.packages", "ai.verta.modeldb.artifactStore.storageservice.s3");
          SpringApplication.run(App.class);
          artifactStoreService = App.getInstance().applicationContext.getBean(S3Service.class);
        } else {
          artifactStoreService = new S3Service(config.artifactStoreConfig.S3.cloudBucketName);
          System.getProperties().put("scan.packages", "dummyPackageName");
          SpringApplication.run(App.class);
        }
        break;
      case "NFS":
        String rootDir = config.artifactStoreConfig.NFS.nfsRootPath;
        LOGGER.trace("NFS server root path {}", rootDir);

        System.getProperties().put("file.upload-dir", rootDir);
        System.getProperties()
            .put("scan.packages", "ai.verta.modeldb.artifactStore.storageservice.nfs");
        SpringApplication.run(App.class, new String[0]);

        artifactStoreService = App.getInstance().applicationContext.getBean(NFSService.class);
        break;
      default:
        throw new ModelDBException("Configure valid artifact store name in config.yaml file.");
    }
    // ------------- Finish Initialize Cloud storage base on configuration ------------------

    LOGGER.info(
        "ArtifactStore service initialized and resolved storage dependency before server start");
    return artifactStoreService;
  }

  private ServiceSet() {}
}
