package ai.verta.modeldb;

import ai.verta.modeldb.artifactStore.storageservice.ArtifactStoreService;
import ai.verta.modeldb.artifactStore.storageservice.nfs.NFSService;
import ai.verta.modeldb.artifactStore.storageservice.s3.S3Service;
import ai.verta.modeldb.authservice.MDBAuthServiceUtils;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.authservice.MDBRoleServiceUtils;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.connections.UAC;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.config.MDBArtifactStoreConfig;
import ai.verta.modeldb.config.MDBConfig;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;

public class ServiceSet {
  private static final Logger LOGGER = LogManager.getLogger(App.class);
  private static final String SCAN_PACKAGES = "scan.packages";

  public ArtifactStoreService artifactStoreService = null;
  public AuthService authService;
  public UAC uac;
  public MDBRoleService mdbRoleService;
  public App app;

  public static ServiceSet fromConfig(
      MDBConfig mdbConfig, MDBArtifactStoreConfig mdbArtifactStoreConfig) throws IOException {
    var set = new ServiceSet();

    // Initialize App.java singleton instance
    set.app = App.getInstance();
    set.app.mdbConfig = mdbConfig;

    set.uac = UAC.FromConfig(mdbConfig);
    set.authService = MDBAuthServiceUtils.FromConfig(mdbConfig, set.uac);
    set.mdbRoleService = MDBRoleServiceUtils.FromConfig(mdbConfig, set.authService, set.uac);

    if (mdbArtifactStoreConfig.isEnabled()) {
      set.artifactStoreService = initializeArtifactStore(mdbArtifactStoreConfig);
    } else {
      System.getProperties().put(SCAN_PACKAGES, "dummyPackageName");
      SpringApplication.run(App.class);
    }

    return set;
  }

  private static ArtifactStoreService initializeArtifactStore(
      MDBArtifactStoreConfig mdbArtifactStoreConfig) throws ModelDBException, IOException {
    // ------------- Start Initialize Cloud storage base on configuration ------------------
    ArtifactStoreService artifactStoreService;

    if (mdbArtifactStoreConfig.getArtifactEndpoint() != null) {
      System.getProperties()
          .put(
              "artifactEndpoint.storeArtifact",
              mdbArtifactStoreConfig.getArtifactEndpoint().getStoreArtifact());
      System.getProperties()
          .put(
              "artifactEndpoint.getArtifact",
              mdbArtifactStoreConfig.getArtifactEndpoint().getGetArtifact());
    }

    if (mdbArtifactStoreConfig.getNFS() != null
        && mdbArtifactStoreConfig.getNFS().getArtifactEndpoint() != null) {
      System.getProperties()
          .put(
              "artifactEndpoint.storeArtifact",
              mdbArtifactStoreConfig.getNFS().getArtifactEndpoint().getStoreArtifact());
      System.getProperties()
          .put(
              "artifactEndpoint.getArtifact",
              mdbArtifactStoreConfig.getNFS().getArtifactEndpoint().getGetArtifact());
    }

    switch (mdbArtifactStoreConfig.getArtifactStoreType()) {
      case "S3":
        if (!mdbArtifactStoreConfig.S3.getS3presignedURLEnabled()) {
          System.setProperty(
              ModelDBConstants.CLOUD_BUCKET_NAME, mdbArtifactStoreConfig.S3.getCloudBucketName());
          System.getProperties()
              .put(SCAN_PACKAGES, "ai.verta.modeldb.artifactStore.storageservice.s3");
          SpringApplication.run(App.class);
          artifactStoreService = App.getInstance().applicationContext.getBean(S3Service.class);
        } else {
          artifactStoreService = new S3Service(mdbArtifactStoreConfig.S3.getCloudBucketName());
          System.getProperties().put(SCAN_PACKAGES, "dummyPackageName");
          SpringApplication.run(App.class);
        }
        break;
      case "NFS":
        String rootDir = mdbArtifactStoreConfig.getNFS().getNfsRootPath();
        LOGGER.trace("NFS server root path {}", rootDir);

        System.getProperties().put("file.upload-dir", rootDir);
        System.getProperties()
            .put(SCAN_PACKAGES, "ai.verta.modeldb.artifactStore.storageservice.nfs");
        SpringApplication.run(App.class);

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
