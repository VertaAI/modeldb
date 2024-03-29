package ai.verta.modeldb.batchProcess;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.metadata.IDTypeEnum;
import ai.verta.modeldb.metadata.IdentificationType;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.metadata.MetadataDAORdbImpl;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.VersioningUtils;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PopulateVersionMigration {
  private PopulateVersionMigration() {}

  private static final Logger LOGGER = LogManager.getLogger(PopulateVersionMigration.class);
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private static MetadataDAO metadataDAO;
  private static int recordUpdateLimit = 100;

  public static void execute(int recordUpdateLimit) {
    PopulateVersionMigration.recordUpdateLimit = recordUpdateLimit;
    metadataDAO = new MetadataDAORdbImpl();
    migrateVersionOfDatasetVersions();
  }

  private static void migrateVersionOfDatasetVersions() {
    LOGGER.debug("DatasetVersion version migration started");

    Long count;
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var repoCountQueryStr =
          "SELECT COUNT(r) FROM RepositoryEntity r WHERE r.deleted = false  AND r.datasetRepositoryMappingEntity IS NOT EMPTY ";
      var repoCountQuery = session.createQuery(repoCountQueryStr);
      count = (Long) repoCountQuery.getSingleResult();
    }

    var lowerBound = 0;
    final int pagesize = recordUpdateLimit;
    LOGGER.debug("Total Datasets {}", count);

    while (lowerBound < count) {
      try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
        LOGGER.debug("starting DatasetVersion version migration");
        String repoQuery =
            "SELECT r FROM RepositoryEntity r "
                + "WHERE r.deleted = false  AND r.datasetRepositoryMappingEntity IS NOT EMPTY "
                + "ORDER BY r.date_created";
        var repoTypedQuery = session.createQuery(repoQuery);
        repoTypedQuery.setFirstResult(lowerBound);
        repoTypedQuery.setMaxResults(pagesize);
        List<RepositoryEntity> datasetEntities = repoTypedQuery.list();

        LOGGER.debug("got datasets");
        if (datasetEntities.size() > 0) {
          Set<Long> datasetIds = new HashSet<>();
          for (RepositoryEntity datasetsEntity : datasetEntities) {
            datasetIds.add(datasetsEntity.getId());
          }
          datasetEntities.clear();

          for (Long datasetId : datasetIds) {
            String commitCountQueryStr =
                "SELECT count(c) FROM CommitEntity c "
                    + "INNER JOIN c.repository r WHERE r.id = :datasetId  AND c.parent_commits IS NOT EMPTY ";
            var commitCountQuery = session.createQuery(commitCountQueryStr);
            commitCountQuery.setParameter("datasetId", datasetId);
            Long commitCount = (Long) commitCountQuery.getSingleResult();

            var commitLowerBound = 0;
            final int commitPagesize = recordUpdateLimit;
            LOGGER.debug("Total DatasetVersions {}", commitCount);

            var version = 0L;
            while (commitLowerBound < commitCount) {
              String commitQuery =
                  "SELECT c FROM CommitEntity c "
                      + "INNER JOIN c.repository r WHERE r.id = :datasetId  AND c.parent_commits IS NOT EMPTY "
                      + "ORDER BY c.date_created";
              var commitTypedQuery = session.createQuery(commitQuery);
              commitTypedQuery.setParameter("datasetId", datasetId);
              commitTypedQuery.setFirstResult(commitLowerBound);
              commitTypedQuery.setMaxResults(commitPagesize);
              List<CommitEntity> commitEntities = commitTypedQuery.list();

              var transaction = session.beginTransaction();
              try {
                for (CommitEntity commitEntity : commitEntities) {
                  List<String> location =
                      Collections.singletonList(ModelDBConstants.DEFAULT_VERSIONING_BLOB_LOCATION);
                  String compositeId =
                      VersioningUtils.getVersioningCompositeId(
                          datasetId, commitEntity.getCommit_hash(), location);
                  version = version + 1;
                  metadataDAO.addProperty(
                      session,
                      IdentificationType.newBuilder()
                          .setIdType(IDTypeEnum.IDType.VERSIONING_REPO_COMMIT_BLOB)
                          .setStringId(compositeId)
                          .build(),
                      ModelDBConstants.VERSION,
                      String.valueOf(version));
                }
                LOGGER.debug(
                    "All datasetVersion's version are migrated for the Dataset '{}' ", datasetId);
              } finally {
                transaction.commit();
              }
              commitLowerBound += commitPagesize;
            }
          }

        } else {
          LOGGER.debug("Datasets total count 0");
        }
        lowerBound += pagesize;
      } catch (Exception ex) {
        if (ModelDBUtils.needToRetry(ex)) {
          migrateVersionOfDatasetVersions();
        } else {
          throw ex;
        }
      }
    }

    LOGGER.debug("DatasetVersion version migration finished");
  }
}
