package ai.verta.modeldb.entities.audit_log;

import ai.verta.uac.versioning.AuditLog;
import ai.verta.uac.versioning.AuditLog.Builder;
import ai.verta.uac.versioning.AuditResource;
import ai.verta.uac.versioning.AuditUser;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "audit_service_local_audit_log")
public class AuditLogLocalEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "local_id")
  private String localId;

  @Column(name = "user_id")
  private String userId;

  @Column(name = "action")
  private String action;

  @Column(name = "resource_id")
  private String resourceId;

  @Column(name = "resource_type")
  private String resourceType;

  @Column(name = "resource_service")
  private String resourceService;

  @Column(name = "ts_nano")
  private Long tsNano;

  @Column(name = "metadata_blob", columnDefinition = "text")
  private String metadataBlob;

  public AuditLogLocalEntity() {}

  public AuditLogLocalEntity(
      String serviceName,
      String userId,
      String action,
      String resourceId,
      String resourceType,
      String resourceService,
      String metadataBlob) {
    tsNano = System.currentTimeMillis() * 1000000;
    localId = String.format("%s_%s", serviceName, UUID.randomUUID());
    this.userId = userId;
    this.action = action;
    this.resourceId = resourceId;
    this.resourceType = resourceType;
    this.resourceService = resourceService;
    this.metadataBlob = metadataBlob;
  }

  public String getLocalId() {
    return localId;
  }

  public String getUserId() {
    return userId;
  }

  public String getAction() {
    return action;
  }

  public String getResourceId() {
    return resourceId;
  }

  public String getResourceType() {
    return resourceType;
  }

  public String getResourceService() {
    return resourceService;
  }

  public Long getTsNano() {
    return tsNano;
  }

  public String getMetadataBlob() {
    return metadataBlob;
  }

  public AuditLog toProto() {
    final AuditResource.Builder resource = AuditResource.newBuilder().setResourceId(resourceId);
    if (resourceType != null) {
      resource.setResourceType(resourceType);
    }
    if (resourceService != null) {
      resource.setResourceService(resourceService);
    }
    Builder builder =
        AuditLog.newBuilder()
            .setLocalId(localId)
            .setUser(AuditUser.newBuilder().setUserId(userId))
            .setAction(action)
            .setResource(resource);
    if (tsNano != null) {
      builder.setTsNano(tsNano);
    }
    if (metadataBlob != null) {
      builder.setMetadataBlob(metadataBlob);
    }
    return builder.build();
  }
}
