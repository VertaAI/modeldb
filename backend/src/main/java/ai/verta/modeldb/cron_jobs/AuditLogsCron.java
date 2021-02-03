package ai.verta.modeldb.cron_jobs;

import ai.verta.modeldb.authservice.AuthServiceChannel;
import ai.verta.modeldb.common.CommonMessages;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.entities.audit_log.AuditLogLocalEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.versioning.*;
import com.google.rpc.Code;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class AuditLogsCron extends TimerTask {
  private static final Logger LOGGER = LogManager.getLogger(AuditLogsCron.class);
  private final ModelDBHibernateUtil modelDBHibernateUtil = ModelDBHibernateUtil.getInstance();
  private final Integer recordUpdateLimit;

  public AuditLogsCron(Integer recordUpdateLimit) {
    this.recordUpdateLimit = recordUpdateLimit;
  }

  /** The action to be performed by this timer task. */
  @Override
  public void run() {
    LOGGER.info("AuditLogsCron wakeup");

    CommonUtils.registeredBackgroundUtilsCount();
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      String alias = "al";
      String getAuditLogsLocalQueryString =
          new StringBuilder("FROM ")
              .append(AuditLogLocalEntity.class.getSimpleName())
              .append(" ")
              .append(alias)
              .toString();

      Query getAuditLogsLocalQuery = session.createQuery(getAuditLogsLocalQueryString);
      getAuditLogsLocalQuery.setMaxResults(this.recordUpdateLimit);
      List<AuditLogLocalEntity> auditLogLocalEntities = getAuditLogsLocalQuery.list();
      LOGGER.info(
          "AuditLogsCron : Getting AuditLogLocalEntity count: {}", auditLogLocalEntities.size());

      if (!auditLogLocalEntities.isEmpty()) {
        Map<String, AuditLogLocalEntity> auditLogLocalEntityMap =
            auditLogLocalEntities.stream()
                .collect(
                    Collectors.toMap(
                        AuditLogLocalEntity::getLocalId,
                        auditLogLocalEntity -> auditLogLocalEntity));
        List<BatchResponseRow> batchResponseRows = sendAuditLogsToUAC(true, auditLogLocalEntities);
        if (!batchResponseRows.isEmpty()) {
          deleteLocalAuditLogs(session, batchResponseRows, auditLogLocalEntityMap);
        }
      }

    } catch (Exception ex) {
      if (ex instanceof StatusRuntimeException) {
        StatusRuntimeException exception = (StatusRuntimeException) ex;
        if (exception.getStatus().getCode().value() == Code.PERMISSION_DENIED_VALUE) {
          LOGGER.warn("AuditLogsCron Exception: {}", ex.getMessage());
        } else {
          LOGGER.warn("AuditLogsCron Exception: ", ex);
        }
      } else {
        LOGGER.warn("AuditLogsCron Exception: ", ex);
      }
    } finally {
      CommonUtils.unregisteredBackgroundUtilsCount();
    }
    LOGGER.info("AuditLogsCron finish tasks and reschedule");
  }

  private void deleteLocalAuditLogs(
      Session session,
      List<BatchResponseRow> batchResponseRows,
      Map<String, AuditLogLocalEntity> auditLogLocalEntityMap) {
    AtomicInteger deletedCount = new AtomicInteger();
    AtomicInteger errorCount = new AtomicInteger();
    session.beginTransaction();
    batchResponseRows.forEach(
        batchResponseRow -> {
          if (batchResponseRow.getAcknowledge()) {
            try {
              LOGGER.debug(
                  "AuditLogsCron: LocalId - {} : getAcknowledge - {}",
                  batchResponseRow.getLocalId(),
                  true);
              AuditLogLocalEntity auditLogLocalEntity =
                  auditLogLocalEntityMap.get(batchResponseRow.getLocalId());
              session.delete(auditLogLocalEntity);
              deletedCount.getAndIncrement();
              LOGGER.debug("AuditLogsCron auditLog deleted: {}", true);
            } catch (Exception ex) {
              LOGGER.debug("AuditLogsCron auditLog deleted: {}", false);
              LOGGER.warn(
                  "AuditLogsCron : LocalId - {} : error - {}",
                  batchResponseRow.getLocalId(),
                  ex.getMessage(),
                  ex);
              errorCount.getAndIncrement();
            }
          } else {
            LOGGER.warn(
                "AuditLogsCron: LocalId - {} : getAcknowledge - {} : Error: {}",
                batchResponseRow.getLocalId(),
                false,
                batchResponseRow.getError());
            errorCount.getAndIncrement();
          }
        });
    session.getTransaction().commit();

    LOGGER.info("AuditLogsCron: successfully deleted audit logs count: {}", deletedCount.get());
    LOGGER.info("AuditLogsCron: Error while deleting audit logs count: {}", errorCount.get());
  }

  private List<BatchResponseRow> sendAuditLogsToUAC(
      boolean retry, List<AuditLogLocalEntity> auditLogLocalEntities) {
    try (AuthServiceChannel authServiceChannel = new AuthServiceChannel()) {
      LOGGER.info(CommonMessages.AUTH_SERVICE_REQ_SENT_MSG);
      List<AuditLog> auditLogs =
          auditLogLocalEntities.stream()
              .map(
                  auditLogLocalEntity ->
                      AuditLog.newBuilder()
                          .setLocalId(auditLogLocalEntity.getLocalId())
                          .setUser(
                              AuditUser.newBuilder()
                                  .setUserId(auditLogLocalEntity.getUserId())
                                  .build())
                          .setAction(auditLogLocalEntity.getAction())
                          .setResource(
                              AuditResource.newBuilder()
                                  .setResourceId(auditLogLocalEntity.getResourceId())
                                  .setResourceService(auditLogLocalEntity.getResourceService())
                                  .setResourceType(auditLogLocalEntity.getResourceType())
                                  .build())
                          .setMetadataBlob(auditLogLocalEntity.getMetadataBlob())
                          .setTsNano(auditLogLocalEntity.getTsNano())
                          .build())
              .collect(Collectors.toList());

      AddAuditLogBatch addAuditLogBatch =
          AddAuditLogBatch.newBuilder().addAllLog(auditLogs).build();
      AddAuditLogBatch.Response response =
          authServiceChannel.getAuditLogServiceBlockingStub().postAuditLogs(addAuditLogBatch);
      return response.getResponseRowsList();
    } catch (StatusRuntimeException ex) {
      return (List<BatchResponseRow>)
          ModelDBUtils.retryOrThrowException(
              ex,
              retry,
              (CommonUtils.RetryCallInterface<List<BatchResponseRow>>)
                  (retry1) -> sendAuditLogsToUAC(retry1, auditLogLocalEntities));
    }
  }
}
