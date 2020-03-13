package ai.verta.modeldb.entities.versioning;

import ai.verta.modeldb.entities.ExperimentRunEntity;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "versioning_modeldb_entity_mapping")
public class VersioningModeldbEntityMapping implements Serializable {
  private VersioningModeldbEntityMapping() {}

  public VersioningModeldbEntityMapping(
      Long repositoryId,
      String commit,
      String versioningKey,
      String versioningLocation,
      Object entity) {
    this.repository_id = repositoryId;
    this.commit = commit;
    this.versioning_key = versioningKey;
    this.versioning_location = versioningLocation;
    this.entity_type = entity.getClass().getSimpleName();

    if (entity instanceof ExperimentRunEntity) {
      this.experimentRunEntity = (ExperimentRunEntity) entity;
    } else {
      Status status =
          Status.newBuilder()
              .setCode(Code.INVALID_ARGUMENT_VALUE)
              .setMessage("Invalid ModelDB entity found")
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  @Id
  @Column(name = "repository_id")
  private Long repository_id;

  @Id
  @Column(name = "commit", columnDefinition = "varchar", length = 64)
  private String commit;

  @Id
  @Column(name = "versioning_key", columnDefinition = "varchar", length = 50)
  private String versioning_key;

  @Column(name = "versioning_location", columnDefinition = "TEXT")
  private String versioning_location;

  @Id
  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "experiment_run_id")
  private ExperimentRunEntity experimentRunEntity;

  @Id
  @Column(name = "entity_type", length = 50)
  private String entity_type;

  public Long getRepository_id() {
    return repository_id;
  }

  public String getCommit() {
    return commit;
  }

  public String getVersioning_key() {
    return versioning_key;
  }

  public String getVersioning_location() {
    return versioning_location;
  }
}
