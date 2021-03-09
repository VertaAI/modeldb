package ai.verta.modeldb.audit_log;

import ai.verta.modeldb.entities.audit_log.AuditLogLocalEntity;
import java.util.List;

public interface AuditLogLocalDAO {

  @Deprecated
  void saveAuditLogs(List<AuditLogLocalEntity> auditLogEntities);

  void saveAuditLog(AuditLogLocalEntity auditLogEntity);
}
