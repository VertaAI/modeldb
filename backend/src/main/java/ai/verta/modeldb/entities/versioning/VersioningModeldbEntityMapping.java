package ai.verta.modeldb.entities.versioning;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.entities.config.ConfigBlobEntity;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
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
      Integer versioningBlobType,
      String blobHash,
      Object entity)
      throws ModelDBException, NoSuchAlgorithmException {
    this.repository_id = repositoryId;
    this.commit = commit;
    this.versioning_key = versioningKey;
    this.versioning_location = versioningLocation;
    this.entity_type = entity.getClass().getSimpleName();
    this.versioning_blob_type = versioningBlobType;

    if (entity instanceof ExperimentRunEntity) {
      this.experimentRunEntity = (ExperimentRunEntity) entity;
    } else {
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL_VALUE)
              .setMessage("Invalid ModelDB entity found")
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }

    if (blobHash != null) {
      this.blob_hash = blobHash;
      /*BlobContainer blobContainer = BlobContainer.create(blobExpanded);
      if (blobContainer instanceof ConfigContainer) {
        ConfigContainer configContainer = (ConfigContainer) blobContainer;
        List<ConfigBlobEntity> allHyperparameterBlobEntities = configContainer.fetchHyperparameterBlobEntities();
        System.out.println("test");
      } else {
        Status status =
                Status.newBuilder()
                        .setCode(Code.INTERNAL_VALUE)
                        .setMessage("Invalid blob type found")
                        .build();
        throw StatusProto.toStatusRuntimeException(status);
      }*/
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
  @Column(name = "versioning_blob_type")
  private Integer versioning_blob_type;

  @Id
  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "experiment_run_id")
  private ExperimentRunEntity experimentRunEntity;

  @Id
  @Column(name = "entity_type", length = 50)
  private String entity_type;

  @Column(name = "blob_hash")
  private String blob_hash;

  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "config_blob_hash")
  private ConfigBlobEntity configBlobEntity;

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

  public String getBlob_hash() {
    return blob_hash;
  }
}
