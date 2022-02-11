package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.CodeVersion;
import ai.verta.modeldb.LogExperimentRunCodeVersion;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.entities.CodeVersionEntity;
import ai.verta.modeldb.exceptions.AlreadyExistsException;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.RdbmsUtils;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LockMode;
import org.jdbi.v3.core.Handle;

public class CodeVersionHandler {

  private static Logger LOGGER = LogManager.getLogger(CodeVersionHandler.class);
  private static final String ENTITY_ID_QUERY_PARAM = "entity_id";

  private final Executor executor;
  private final FutureJdbi jdbi;
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private final String entityTableName;

  public CodeVersionHandler(Executor executor, FutureJdbi jdbi, String entityTableName) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.entityTableName = entityTableName;
  }

  public InternalFuture<Optional<CodeVersion>> getCodeVersion(String entityId) {
    return jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        String.format(
                            "select code_version_snapshot_id from %s where id=:entity_id",
                            entityTableName))
                    .bind(ENTITY_ID_QUERY_PARAM, entityId)
                    .mapTo(Long.class)
                    .findOne())
        .thenApply(
            maybeSnapshotId ->
                maybeSnapshotId.map(
                    snapshotId -> {
                      try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
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

  public void logCodeVersion(Handle handle, LogExperimentRunCodeVersion request) {
    // TODO: input validation
    // Check if it existed before
    var maybeSnapshotId =
        handle
            .createQuery(
                String.format(
                    "select code_version_snapshot_id from %s where id=:entity_id", entityTableName))
            .bind(ENTITY_ID_QUERY_PARAM, request.getId())
            .mapTo(Long.class)
            .findOne();

    // Check if we can overwrite
    if (maybeSnapshotId.isPresent()) {
      if (request.getOverwrite()) {
        try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
          var transaction = session.beginTransaction();
          session
              .createSQLQuery(
                  String.format(
                      "UPDATE %s SET code_version_snapshot_id = null WHERE id=:entity_id",
                      entityTableName))
              .setParameter(ENTITY_ID_QUERY_PARAM, request.getId())
              .executeUpdate();
          final CodeVersionEntity entity =
              session.get(
                  CodeVersionEntity.class, maybeSnapshotId.get(), LockMode.PESSIMISTIC_WRITE);
          session.delete(entity);
          transaction.commit();
        }
      } else {
        throw new AlreadyExistsException("Code version already logged");
      }
    }

    // Create the new snapshot using hibernate
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      final var snapshot =
          RdbmsUtils.generateCodeVersionEntity(
              ModelDBConstants.CODE_VERSION, request.getCodeVersion());
      var transaction = session.beginTransaction();
      session.saveOrUpdate(snapshot);
      transaction.commit();
      var snapshotId = snapshot.getId();

      // Save the snapshot to the ER
      handle
          .createUpdate(
              String.format(
                  "update %s set code_version_snapshot_id=:code_id where id=:entity_id",
                  entityTableName))
          .bind("code_id", snapshotId)
          .bind(ENTITY_ID_QUERY_PARAM, request.getId())
          .execute();
    }
  }

  public InternalFuture<Map<String, CodeVersion>> getCodeVersionMap(List<String> entityIds) {
    return jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        String.format(
                            "select id, code_version_snapshot_id from %s where id IN (<entity_ids>) ",
                            entityTableName))
                    .bindList("entity_ids", entityIds)
                    .map(
                        (rs, ctx) ->
                            new AbstractMap.SimpleEntry<>(
                                rs.getString("id"), rs.getLong("code_version_snapshot_id")))
                    .list())
        .thenCompose(
            maybeSnapshotIds -> {
              Map<String, CodeVersion> codeVersionMap = new HashMap<>();
              for (AbstractMap.SimpleEntry<String, Long> entry : maybeSnapshotIds) {
                if (entry.getValue() != null && entry.getValue() != 0) {
                  try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
                    final CodeVersionEntity entity =
                        session.get(
                            CodeVersionEntity.class, entry.getValue(), LockMode.PESSIMISTIC_WRITE);
                    codeVersionMap.put(entry.getKey(), entity.getProtoObject());
                  }
                }
              }
              return InternalFuture.completedInternalFuture(codeVersionMap);
            },
            executor);
  }
}
