package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.CodeVersion;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.futures.Future;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.Handle;
import ai.verta.modeldb.entities.CodeVersionEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.RdbmsUtils;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LockMode;

public class CodeVersionHandler {
  private static Logger LOGGER = LogManager.getLogger(CodeVersionHandler.class);
  private static final String ENTITY_ID_QUERY_PARAM = "entity_id";

  private final FutureExecutor executor;
  private final FutureJdbi jdbi;
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private final String entityTableName;

  public CodeVersionHandler(FutureExecutor executor, FutureJdbi jdbi, String entityTableName) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.entityTableName = entityTableName;
  }

  public Future<Optional<CodeVersion>> getCodeVersion(String entityId) {
    Future<Optional<Long>> optionalFuture =
        jdbi.call(
            handle ->
                handle
                    .createQuery(
                        String.format(
                            "select code_version_snapshot_id from %s where id=:entity_id",
                            entityTableName))
                    .bind(ENTITY_ID_QUERY_PARAM, entityId)
                    .mapTo(Long.class)
                    .findOne());
    return optionalFuture.thenCompose(
        t ->
            Future.of(
                ((Function<? super Optional<Long>, ? extends Optional<CodeVersion>>)
                        maybeSnapshotId ->
                            maybeSnapshotId.map(
                                snapshotId -> {
                                  try (var session =
                                      modelDBHibernateUtil.getSessionFactory().openSession()) {
                                    final CodeVersionEntity entity =
                                        session.get(
                                            CodeVersionEntity.class,
                                            maybeSnapshotId.get(),
                                            LockMode.PESSIMISTIC_WRITE);
                                    return entity.getProtoObject();
                                  }
                                }))
                    .apply(t)));
  }

  public void logCodeVersion(
      Handle handle, String entityId, boolean overwrite, CodeVersion codeVersion) {
    // TODO: input validation
    // Check if it existed before
    var maybeSnapshotId =
        handle
            .createQuery(
                String.format(
                    "select code_version_snapshot_id from %s where id=:entity_id", entityTableName))
            .bind(ENTITY_ID_QUERY_PARAM, entityId)
            .mapTo(Long.class)
            .findOne();

    // Check if we can overwrite
    if (maybeSnapshotId.isPresent()) {
      if (overwrite) {
        try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
          var transaction = session.beginTransaction();
          session
              .createSQLQuery(
                  String.format(
                      "UPDATE %s SET code_version_snapshot_id = null WHERE id=:entity_id",
                      entityTableName))
              .setParameter(ENTITY_ID_QUERY_PARAM, entityId)
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
          RdbmsUtils.generateCodeVersionEntity(ModelDBConstants.CODE_VERSION, codeVersion);
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
          .bind(ENTITY_ID_QUERY_PARAM, entityId)
          .execute();
    }
  }

  public Future<Map<String, CodeVersion>> getCodeVersionMap(List<String> entityIds) {
    return jdbi.call(
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
              return Future.of(codeVersionMap);
            });
  }
}
