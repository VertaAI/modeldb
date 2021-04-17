package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.modeldb.CodeVersion;
import ai.verta.modeldb.GetExperimentRunCodeVersion;
import ai.verta.modeldb.LogExperimentRunCodeVersion;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.entities.CodeVersionEntity;
import ai.verta.modeldb.exceptions.AlreadyExistsException;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.RdbmsUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Optional;
import java.util.concurrent.Executor;

public class CodeVersionHandler {
  private static Logger LOGGER = LogManager.getLogger(CodeVersionHandler.class);

  private final Executor executor;
  private final FutureJdbi jdbi;
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();

  public CodeVersionHandler(Executor executor, FutureJdbi jdbi) {
    this.executor = executor;
    this.jdbi = jdbi;
  }

  public InternalFuture<Optional<CodeVersion>> getCodeVersion(GetExperimentRunCodeVersion request) {
    return jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "select code_version_snapshot_id from experiment_run where id=:run_id")
                    .bind("run_id", request.getId())
                    .mapTo(Long.class)
                    .findOne())
        .thenApply(
            maybeSnapshotId ->
                maybeSnapshotId.map(
                    snapshotId -> {
                      try (Session session =
                          modelDBHibernateUtil.getSessionFactory().openSession()) {
                        final CodeVersionEntity entity =
                            session.get(
                                CodeVersionEntity.class,
                                maybeSnapshotId.get(),
                                LockMode.PESSIMISTIC_WRITE);
                        return entity.getProtoObject();
                      }
                    }),
            executor);
  }

  public InternalFuture<Void> logCodeVersion(LogExperimentRunCodeVersion request) {
    // TODO: input validation
    return jdbi.withHandle(
            // Check if it existed before
            handle ->
                handle
                    .createQuery(
                        "select code_version_snapshot_id from experiment_run where id=:run_id")
                    .bind("run_id", request.getId())
                    .mapTo(Long.class)
                    .findOne())
        .thenAccept(
            // Check if we can overwrite
            maybeSnapshotId -> {
              if (maybeSnapshotId.isPresent()) {
                if (request.getOverwrite()) {
                  try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
                    final CodeVersionEntity entity =
                        session.get(
                            CodeVersionEntity.class,
                            maybeSnapshotId.get(),
                            LockMode.PESSIMISTIC_WRITE);
                    Transaction transaction = session.beginTransaction();
                    session.delete(entity);
                    transaction.commit();
                  }
                } else {
                  throw new AlreadyExistsException("Code version already logged");
                }
              }
            },
            executor)
        .thenApply(
            // Create the new snapshot using hibernate
            unused -> {
              try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
                final var snapshot =
                    RdbmsUtils.generateCodeVersionEntity(
                        ModelDBConstants.CODE_VERSION, request.getCodeVersion());
                Transaction transaction = session.beginTransaction();
                session.saveOrUpdate(snapshot);
                transaction.commit();
                return snapshot.getId();
              }
            },
            executor)
        .thenCompose(
            // Save the snapshot to the ER
            snapshotId ->
                jdbi.useHandle(
                    handle ->
                        handle
                            .createUpdate(
                                "update experiment_run set code_version_snapshot_id=:code_id where id=:run_id")
                            .bind("code_id", snapshotId)
                            .bind("run_id", request.getId())
                            .execute()),
            executor);
  }
}
