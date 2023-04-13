package ai.verta.modeldb.utils;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.config.ArtifactStoreConfig;
import ai.verta.modeldb.entities.ArtifactEntity;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ArtifactPathMigrationUtils {
  private ArtifactPathMigrationUtils() {}

  private static final Logger LOGGER = LogManager.getLogger(ArtifactPathMigrationUtils.class);
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();

  public static void migration(Map<String, Object> propertiesMap) {
    LOGGER.debug("Artifact path migration start");
    Map<String, Object> artifactStoreConfigMap =
        (Map<String, Object>) propertiesMap.get(ModelDBConstants.ARTIFACT_STORE_CONFIG);

    String artifactStoreType =
        (String) artifactStoreConfigMap.get(ModelDBConstants.ARTIFACT_STORE_TYPE);
    String storeTypePathPrefix = null;
    if (artifactStoreType.equals(ArtifactStoreConfig.S3_TYPE_STORE)) {
      Map<String, Object> s3ConfigMap =
          (Map<String, Object>) artifactStoreConfigMap.get(ArtifactStoreConfig.S3_TYPE_STORE);
      String cloudBucketName = (String) s3ConfigMap.get(ModelDBConstants.CLOUD_BUCKET_NAME);
      LOGGER.trace("S3 cloud bucket name {}", cloudBucketName);
      storeTypePathPrefix = "s3://" + cloudBucketName + ModelDBConstants.PATH_DELIMITER;
    } else if (artifactStoreType.equals(ModelDBConstants.NFS)) {
      Map<String, Object> nfsConfigMap =
          (Map<String, Object>) artifactStoreConfigMap.get(ModelDBConstants.NFS);
      String rootDir = (String) nfsConfigMap.get(ModelDBConstants.NFS_ROOT_PATH);
      LOGGER.trace("NFS server root path {}", rootDir);
      storeTypePathPrefix = "nfs://" + rootDir + ModelDBConstants.PATH_DELIMITER;
    }

    Long count = getArtifactEntityCount();

    var lowerBound = 0;
    final var pagesize = 500;
    LOGGER.debug("Total artifactEntities {}", count);

    while (lowerBound < count) {

      try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
        var transaction = session.beginTransaction();
        var queryString =
            "FROM ArtifactEntity ae WHERE ae."
                + ModelDBConstants.STORE_TYPE_PATH
                + " IS NULL ORDER BY ae.id asc";
        var query = session.createQuery(queryString);
        query.setFirstResult(lowerBound);
        query.setMaxResults(pagesize);
        List<ArtifactEntity> artifactEntities = query.list();
        for (ArtifactEntity artifactEntity : artifactEntities) {
          if (!artifactEntity.getPath_only()) {
            String storeTypePath = storeTypePathPrefix + artifactEntity.getPath();
            artifactEntity.setStore_type_path(storeTypePath);
            session.update(artifactEntity);
          }
        }
        LOGGER.debug("finished processing page lower boundary {}", lowerBound);
        transaction.commit();
        lowerBound += pagesize;
      }
    }

    LOGGER.debug("Artifact path migration stop");
  }

  private static Long getArtifactEntityCount() {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var query =
          session.createQuery(
              "SELECT count(*) FROM ArtifactEntity ae WHERE ae."
                  + ModelDBConstants.STORE_TYPE_PATH
                  + " IS NULL");
      return (Long) query.uniqueResult();
    }
  }
}
