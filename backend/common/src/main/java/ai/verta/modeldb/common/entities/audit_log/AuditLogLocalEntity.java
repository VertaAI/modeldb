package ai.verta.modeldb.common.entities.audit_log;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.uac.Action;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ResourceType;
import ai.verta.uac.ServiceEnum.Service;
import ai.verta.uac.versioning.AuditLog;
import ai.verta.uac.versioning.AuditResource;
import ai.verta.uac.versioning.AuditUser;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import io.grpc.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
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
  private Integer action;

  @ElementCollection
  @CollectionTable(
      name = "audit_resource_workspace_mapping",
      joinColumns = {@JoinColumn(name = "audit_log_id", referencedColumnName = "id")})
  @MapKeyColumn(name = "resource_id")
  @Column(name = "workspace_id")
  private Map<String, Long> resourceIdWorkspaceIdMap;

  @Column(name = "resource_type")
  private Integer resourceType;

  @Column(name = "resource_service")
  private Integer resourceService;

  @Column(name = "ts_nano")
  private Long tsNano;

  @Column(name = "method_name", columnDefinition = "text")
  private String methodName;

  @Column(name = "request", columnDefinition = "longtext")
  private String request;

  @Column(name = "response", columnDefinition = "longtext")
  private String response;

  private AuditLogLocalEntity() {}

  public AuditLogLocalEntity(
      String serviceName,
      String userId,
      ModelDBServiceActions action,
      Map<String, Long> resourceIdWorkspaceIdMap,
      ModelDBServiceResourceTypes resourceType,
      Service resourceService,
      String methodName,
      String request,
      String response) {
    tsNano = System.currentTimeMillis() * 1000000;
    localId = String.format("%s_%s", serviceName, UUID.randomUUID());
    this.userId = userId;
    this.action = action.getNumber();
    this.resourceIdWorkspaceIdMap = resourceIdWorkspaceIdMap;
    this.resourceType = resourceType.getNumber();
    this.resourceService = resourceService.getNumber();
    this.methodName = methodName;
    this.request = request;
    this.response = response;
  }

  public String getLocalId() {
    return localId;
  }

  public AuditLog toProto() {
    List<AuditResource> auditResources = new ArrayList<>();
    for (Map.Entry<String, Long> resourceId : resourceIdWorkspaceIdMap.entrySet()) {
      final AuditResource.Builder resource =
          AuditResource.newBuilder()
              .setResourceId(resourceId.getKey())
              .setWorkspaceId(resourceId.getValue());
      if (resourceType != null) {
        resource.setResourceType(
            ResourceType.newBuilder().setModeldbServiceResourceTypeValue(resourceType).build());
      }
      if (resourceService != null) {
        resource.setResourceServiceValue(resourceService);
      }
      auditResources.add(resource.build());
    }

    AuditLog.Builder builder =
        AuditLog.newBuilder()
            .setLocalId(localId)
            .setUser(AuditUser.newBuilder().setUserId(userId))
            .setAction(
                Action.newBuilder()
                    .setModeldbServiceAction(ModelDBServiceActions.forNumber(action))
                    .setServiceValue(resourceService)
                    .build())
            .addAllResource(auditResources);
    if (tsNano != null) {
      builder.setTsNano(tsNano);
    }
    builder.setMethodName(methodName);

    try {
      Value.Builder requestBuilder = Value.newBuilder();
      CommonUtils.getProtoObjectFromString(request, requestBuilder);
      builder.setRequest(requestBuilder.build());
    } catch (InvalidProtocolBufferException e) {
      throw new ModelDBException(e.getMessage(), Status.Code.INTERNAL);
    }

    try {
      Value.Builder responseBuilder = Value.newBuilder();
      CommonUtils.getProtoObjectFromString(response, responseBuilder);
      builder.setResponse(responseBuilder.build());
    } catch (InvalidProtocolBufferException e) {
      throw new ModelDBException(e.getMessage(), Status.Code.INTERNAL);
    }

    return builder.build();
  }
}
