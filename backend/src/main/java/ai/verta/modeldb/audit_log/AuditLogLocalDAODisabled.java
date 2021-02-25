package ai.verta.modeldb.audit_log;

import ai.verta.modeldb.entities.audit_log.AuditLogLocalEntity;
import java.util.List;

public class AuditLogLocalDAODisabled implements AuditLogLocalDAO {
  @Override
  public void saveAuditLogs(List<AuditLogLocalEntity> auditLogEntities) {}

  public void saveAuditLog(AuditLogLocalEntity auditLogLocalEntity) {}
}
