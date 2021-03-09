package ai.verta.modeldb.audit_log;

import ai.verta.modeldb.entities.audit_log.AuditLogLocalEntity;
import java.util.List;

public interface AuditLogLocalDAO {

  // TODO: Remove below method after all services use saveAuditLog
  @Deprecated
  void saveAuditLogs(List<AuditLogLocalEntity> auditLogEntities);

  void saveAuditLog(AuditLogLocalEntity auditLogLocalEntity);
}
