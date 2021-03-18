package ai.verta.modeldb.audit_log;

import ai.verta.modeldb.common.entities.audit_log.AuditLogLocalEntity;
import ai.verta.modeldb.common.monitoring.AuditLogInterceptor;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class AuditLogLocalDAORdbImpl implements AuditLogLocalDAO {

  private static final Logger LOGGER =
      LogManager.getLogger(AuditLogLocalDAORdbImpl.class.getName());
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();

  // TODO: Remove below method after all services use saveAuditLog
  @Deprecated
  @Override
  public void saveAuditLogs(List<AuditLogLocalEntity> auditLogEntities) {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      saveAuditLogs(session, auditLogEntities);
      transaction.commit();
      LOGGER.debug("Audit logged successfully");
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        saveAuditLogs(auditLogEntities);
      } else {
        throw ex;
      }
    }
  }

  private void saveAuditLogs(Session session, List<AuditLogLocalEntity> auditLogEntities) {
    auditLogEntities.forEach(session::save);
    AuditLogInterceptor.increaseAuditCountStatic();
  }

  @Override
  public void saveAuditLog(AuditLogLocalEntity auditLogLocalEntity) {
    try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      saveAuditLog(session, auditLogLocalEntity);
      transaction.commit();
      LOGGER.debug("Audit logged successfully");
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        saveAuditLog(auditLogLocalEntity);
      } else {
        throw ex;
      }
    }
  }

  private void saveAuditLog(Session session, AuditLogLocalEntity auditLogEntity) {
    session.save(auditLogEntity);
    AuditLogInterceptor.increaseAuditCountStatic();
  }
}
