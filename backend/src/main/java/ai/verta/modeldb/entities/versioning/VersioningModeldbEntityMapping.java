package ai.verta.modeldb.entities.versioning;

import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.entities.config.ConfigBlobEntity;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Entity
@Table(name = "versioning_modeldb_entity_mapping")
public class VersioningModeldbEntityMapping implements Serializable {
  private VersioningModeldbEntityMapping() {}

  private static final Logger LOGGER = LogManager.getLogger(VersioningModeldbEntityMapping.class);

  public VersioningModeldbEntityMapping(
      Long repositoryId,
      String commit,
      String versioningKey,
      String versioningLocation,
      Integer versioningBlobType,
      String blobHash,
      Object entity) {
    this.repository_id = repositoryId;
    this.commit = commit;
    this.versioning_key = versioningKey;
    this.versioning_location = versioningLocation;
    this.entity_type = entity.getClass().getSimpleName();
    this.versioning_blob_type = versioningBlobType;

    if (entity instanceof ExperimentRunEntity) {
      this.experimentRunEntity = (ExperimentRunEntity) entity;
    } else {
      LOGGER.warn("ExperimentRunEntity Expected : found {}", entity.getClass());
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL_VALUE)
              .setMessage("Invalid ModelDB entity found")
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }

    if (blobHash != null) {
      this.blob_hash = blobHash;
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

  @ManyToMany(targetEntity = ConfigBlobEntity.class, cascade = CascadeType.PERSIST)
  @JoinTable(
      name = "versioning_modeldb_entity_mapping_config_blob",
      joinColumns = {
        @JoinColumn(
            name = "versioning_modeldb_entity_mapping_repository_id",
            referencedColumnName = "repository_id"),
        @JoinColumn(
            name = "versioning_modeldb_entity_mapping_commit",
            referencedColumnName = "commit"),
        @JoinColumn(
            name = "versioning_modeldb_entity_mapping_versioning_key",
            referencedColumnName = "versioning_key"),
        @JoinColumn(
            name = "versioning_modeldb_entity_mapping_experiment_run_id",
            referencedColumnName = "experiment_run_id"),
        @JoinColumn(
            name = "versioning_modeldb_entity_mapping_entity_type",
            referencedColumnName = "entity_type")
      },
      inverseJoinColumns = {
        @JoinColumn(name = "config_blob_entity_blob_hash", referencedColumnName = "blob_hash"),
        @JoinColumn(
            name = "config_blob_entity_config_seq_number",
            referencedColumnName = "config_seq_number")
      })
  private Set<ConfigBlobEntity> config_blob_entities = new HashSet<>();

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

  public Integer getVersioning_blob_type() {
    return versioning_blob_type;
  }

  public String getBlob_hash() {
    return blob_hash;
  }

  public void setConfig_blob_entities(Set<ConfigBlobEntity> config_blob_entities) {
    this.config_blob_entities = config_blob_entities;
  }

  public Set<ConfigBlobEntity> getConfig_blob_entities() {
    return config_blob_entities;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VersioningModeldbEntityMapping that = (VersioningModeldbEntityMapping) o;
    return Objects.equals(repository_id, that.repository_id)
        && Objects.equals(commit, that.commit)
        && Objects.equals(versioning_key, that.versioning_key)
        && Objects.equals(versioning_location, that.versioning_location)
        && Objects.equals(versioning_blob_type, that.versioning_blob_type)
        && Objects.equals(experimentRunEntity, that.experimentRunEntity)
        && Objects.equals(entity_type, that.entity_type)
        && Objects.equals(blob_hash, that.blob_hash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        repository_id,
        commit,
        versioning_key,
        versioning_location,
        versioning_blob_type,
        experimentRunEntity,
        entity_type,
        blob_hash);
  }
}
