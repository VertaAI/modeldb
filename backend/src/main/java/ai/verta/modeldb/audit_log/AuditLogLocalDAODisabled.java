package ai.verta.modeldb.audit_log;

import ai.verta.modeldb.common.entities.audit_log.AuditLogLocalEntity;
import java.util.List;

public class AuditLogLocalDAODisabled implements AuditLogLocalDAO {

  @Deprecated
  @Override
  public void saveAuditLogs(List<AuditLogLocalEntity> auditLogEntities) {}

  @Override
  public void saveAuditLog(AuditLogLocalEntity auditLogLocalEntity) {}
}
