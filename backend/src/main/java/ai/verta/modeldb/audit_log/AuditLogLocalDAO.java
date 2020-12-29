package ai.verta.modeldb.audit_log;

import ai.verta.modeldb.entities.audit_log.AuditLogLocalEntity;
import java.util.List;

public interface AuditLogLocalDAO {
  void saveAuditLogs(List<AuditLogLocalEntity> auditLogEntities);
}
