package ai.verta.modeldb.reconcilers;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.reconcilers.ReconcileResult;
import ai.verta.modeldb.common.reconcilers.Reconciler;
import ai.verta.modeldb.common.reconcilers.ReconcilerConfig;
import ai.verta.modeldb.entities.versioning.BranchEntity;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.entities.versioning.TagsEntity;
import ai.verta.modeldb.metadata.IDTypeEnum;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.VersioningUtils;
import io.opentelemetry.api.OpenTelemetry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.OptimisticLockException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class SoftDeleteRepositories extends Reconciler<String> {
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private final MDBRoleService mdbRoleService;
  private final boolean isDataset;

  public SoftDeleteRepositories(
      ReconcilerConfig config,
      MDBRoleService mdbRoleService,
      boolean isDataset,
      FutureJdbi futureJdbi,
      OpenTelemetry openTelemetry) {
    super(config, futureJdbi, openTelemetry, true);
    this.mdbRoleService = mdbRoleService;
    this.isDataset = isDataset;
  }

  @Override
  public void resync() {
    String queryString;
    if (isDataset) {
      queryString =
          String.format(
              "select rp.id from %s rp where rp.deleted=:deleted AND rp.datasetRepositoryMappingEntity IS NOT EMPTY",
              RepositoryEntity.class.getSimpleName());
    } else {
      queryString =
          String.format(
              "select rp.id from %s rp where rp.deleted=:deleted AND rp.datasetRepositoryMappingEntity IS EMPTY",
              RepositoryEntity.class.getSimpleName());
    }

    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var deletedQuery = session.createQuery(queryString);
      deletedQuery.setParameter("deleted", true);
      deletedQuery.setMaxResults(config.getMaxSync());
      deletedQuery.stream().forEach(id -> this.insert(String.valueOf(id)));
    }
  }

  @Override
  protected ReconcileResult reconcile(Set<String> ids) {
    logger.debug("Reconciling repositories " + ids.toString());

    if (isDataset) {
      mdbRoleService.deleteEntityResourcesWithServiceUser(
          new ArrayList<>(ids), ModelDBServiceResourceTypes.DATASET);
    } else {
      mdbRoleService.deleteEntityResourcesWithServiceUser(
          new ArrayList<>(ids), ModelDBServiceResourceTypes.REPOSITORY);
    }

    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      deleteRepositories(session, ids);
    }

    return new ReconcileResult();
  }

  private void deleteRepositories(Session session, Set<String> ids) {
    logger.trace("Repository deleting");
    var repositoriesQueryString =
        String.format("from %s where id in (:ids)", RepositoryEntity.class.getSimpleName());

    var repositoryDeleteQuery = session.createQuery(repositoriesQueryString);
    repositoryDeleteQuery.setParameter(
        "ids", ids.stream().map(Long::parseLong).collect(Collectors.toList()));
    List<RepositoryEntity> repositoryEntities = repositoryDeleteQuery.list();

    if (!repositoryEntities.isEmpty()) {
      for (RepositoryEntity repository : repositoryEntities) {
        Transaction transaction = null;
        try {
          var modelDBServiceResourceTypes =
              ModelDBUtils.getModelDBServiceResourceTypesFromRepository(repository);
          mdbRoleService.deleteEntityResourcesWithServiceUser(
              Collections.singletonList(String.valueOf(repository.getId())),
              modelDBServiceResourceTypes);

          transaction = session.beginTransaction();

          var deleteTagsHql = "DELETE TagsEntity te where te.id.repository_id = :repoId ";
          var deleteTagsQuery = session.createQuery(deleteTagsHql);
          deleteTagsQuery.setParameter("repoId", repository.getId());
          deleteTagsQuery.executeUpdate();

          deleteLabels(
              session, String.valueOf(repository.getId()), IDTypeEnum.IDType.VERSIONING_REPOSITORY);

          var getRepositoryBranchesHql =
              "From BranchEntity br where br.id.repository_id = :repoId ";
          var query = session.createQuery(getRepositoryBranchesHql);
          query.setParameter("repoId", repository.getId());
          List<BranchEntity> branchEntities = query.list();

          List<String> branches =
              branchEntities.stream()
                  .map(branchEntity -> branchEntity.getId().getBranch())
                  .collect(Collectors.toList());

          if (!branches.isEmpty()) {
            var deleteBranchesHQL =
                "DELETE FROM BranchEntity br where br.id.repository_id = :repositoryId AND br.id.branch IN (:branches)";
            var deleteBranchQuery = session.createQuery(deleteBranchesHQL);
            deleteBranchQuery.setParameter("repositoryId", repository.getId());
            deleteBranchQuery.setParameterList("branches", branches);
            deleteBranchQuery.executeUpdate();
          }

          var commitQueryString =
              "SELECT cm FROM CommitEntity cm LEFT JOIN cm.repository repo WHERE repo.id = :repoId ORDER BY cm.date_created DESC ";
          Query<CommitEntity> commitEntityQuery = session.createQuery(commitQueryString);
          commitEntityQuery.setParameter("repoId", repository.getId());
          List<CommitEntity> commitEntities = commitEntityQuery.list();

          commitEntities.forEach(
              commitEntity -> {
                if (commitEntity.getRepository().contains(repository)) {
                  commitEntity.getRepository().remove(repository);
                  if (commitEntity.getRepository().isEmpty()) {
                    if (repository.isDataset()) {
                      String compositeId =
                          VersioningUtils.getVersioningCompositeId(
                              repository.getId(),
                              commitEntity.getCommit_hash(),
                              Collections.singletonList(
                                  ModelDBConstants.DEFAULT_VERSIONING_BLOB_LOCATION));
                      deleteLabels(
                          session, compositeId, IDTypeEnum.IDType.VERSIONING_REPO_COMMIT_BLOB);
                      deleteAttribute(session, compositeId);
                    } else {
                      deleteLabels(
                          session,
                          commitEntity.getCommit_hash(),
                          IDTypeEnum.IDType.VERSIONING_COMMIT);
                    }
                    deleteTagEntities(session, repository.getId(), commitEntity.getCommit_hash());
                    session.delete(commitEntity);
                  } else {
                    session.update(commitEntity);
                  }
                }
              });
          session.delete(repository);
          transaction.commit();
        } catch (OptimisticLockException ex) {
          logger.error(
              "SoftDeleteRepositories : deleteRepositories : Exception: {}", ex.getMessage());
          if (transaction != null && transaction.getStatus().canRollback()) {
            transaction.rollback();
          }

          deleteRepositories(session, ids);
        } catch (Exception ex) {
          logger.error("SoftDeleteRepositories : deleteRepositories : Exception: ", ex);
          if (transaction != null && transaction.getStatus().canRollback()) {
            transaction.rollback();
          }
        }
      }
    }
    logger.trace(
        "Repository Deleted successfully : Deleted repositories count {}",
        repositoryEntities.size());
  }

  public static void deleteLabels(Session session, Object entityHash, IDTypeEnum.IDType idType) {
    var deleteLabelsQueryString =
        "DELETE LabelsMappingEntity lm where lm.id.entity_hash = :entityHash AND lm.id.entity_type = :entityType";
    var deleteLabelsQuery =
        session
            .createQuery(deleteLabelsQueryString)
            .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
    deleteLabelsQuery.setParameter("entityHash", entityHash);
    deleteLabelsQuery.setParameter("entityType", idType.getNumber());
    deleteLabelsQuery.executeUpdate();
  }

  public static void deleteAttribute(Session session, String entityHash) {
    var deleteAllAttributes =
        "delete from AttributeEntity at WHERE at.entity_hash = :entityHash AND at.entity_name = :entityName";
    var deleteLabelsQuery =
        session
            .createQuery(deleteAllAttributes)
            .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
    deleteLabelsQuery.setParameter("entityHash", entityHash);
    deleteLabelsQuery.setParameter("entityName", ModelDBConstants.BLOB);
    deleteLabelsQuery.executeUpdate();
  }

  private static void deleteTagEntities(Session session, Long repoId, String commitHash) {
    String getTagsHql =
        "From TagsEntity te where te.id.repository_id = :repoId AND te.commit_hash = :commitHash";
    Query<TagsEntity> getTagsQuery = session.createQuery(getTagsHql, TagsEntity.class);
    getTagsQuery.setParameter("repoId", repoId);
    getTagsQuery.setParameter("commitHash", commitHash);
    List<TagsEntity> tagsEntities = getTagsQuery.list();
    tagsEntities.forEach(session::delete);
  }
}
