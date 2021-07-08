package ai.verta.modeldb.entities;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.Observation;
import ai.verta.modeldb.utils.RdbmsUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;

@Entity
@Table(name = "observation")
public class ObservationEntity {

  public ObservationEntity() {}

  public ObservationEntity(Object entity, String fieldType, Observation observation)
      throws InvalidProtocolBufferException {

    setTimestamp(observation.getTimestamp());
    setEpochNumber((long) observation.getEpochNumber().getNumberValue());
    if (observation.getAttribute() != null && !observation.getAttribute().getKey().isEmpty()) {
      setKeyValueMapping(
          RdbmsUtils.generateKeyValueEntity(
              this, ModelDBConstants.ATTRIBUTES, observation.getAttribute()));
    }

    if (observation.getArtifact() != null && !observation.getArtifact().getKey().isEmpty()) {
      setArtifactMapping(
          RdbmsUtils.generateArtifactEntity(
              this, ModelDBConstants.ARTIFACTS, observation.getArtifact()));
    }

    if (entity instanceof ProjectEntity) {
      setProjectEntity(entity);
    } else if (entity instanceof ExperimentEntity) {
      setExperimentEntity(entity);
    } else if (entity instanceof ExperimentRunEntity) {
      setExperimentRunEntity(entity);
    }

    this.field_type = fieldType;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "timestamp", nullable = false)
  private Long timestamp;

  @Column(name = "epoch_number", nullable = false)
  private Long epoch_number;

  @OneToOne(cascade = CascadeType.ALL)
  @OrderBy("id")
  private KeyValueEntity keyValueMapping;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "artifact_id", nullable = true)
  private ArtifactEntity artifactMapping;

  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = true)
  private ProjectEntity projectEntity;

  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "experiment_id", nullable = true)
  private ExperimentEntity experimentEntity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "experiment_run_id", nullable = true)
  private ExperimentRunEntity experimentRunEntity;

  @Column(name = "entity_name", length = 50)
  private String entityName;

  @Column(name = "field_type", length = 50)
  private String field_type;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  public KeyValueEntity getKeyValueMapping() {
    return keyValueMapping;
  }

  public void setKeyValueMapping(KeyValueEntity keyValueMapping) {
    this.keyValueMapping = keyValueMapping;
  }

  public ArtifactEntity getArtifactMapping() {
    return artifactMapping;
  }

  public void setArtifactMapping(ArtifactEntity artifactMapping) {
    this.artifactMapping = artifactMapping;
  }

  public ProjectEntity getProjectEntity() {
    return projectEntity;
  }

  private void setProjectEntity(Object entity) {
    this.projectEntity = (ProjectEntity) entity;
    this.entityName = this.projectEntity.getClass().getSimpleName();
  }

  public ExperimentEntity getExperimentEntity() {
    return experimentEntity;
  }

  private void setExperimentEntity(Object experimentEntity) {
    this.experimentEntity = (ExperimentEntity) experimentEntity;
    this.entityName = this.experimentEntity.getClass().getSimpleName();
  }

  public ExperimentRunEntity getExperimentRunEntity() {
    return experimentRunEntity;
  }

  private void setExperimentRunEntity(Object entity) {
    this.experimentRunEntity = (ExperimentRunEntity) entity;
    this.entityName = this.experimentRunEntity.getClass().getSimpleName();
  }

  public String getField_type() {
    return field_type;
  }

  public void setEpochNumber(Long epochNumber) {
    this.epoch_number = epochNumber;
  }

  public Observation getProtoObject() throws InvalidProtocolBufferException {
    Observation.Builder builder = Observation.newBuilder();
    builder.setTimestamp(timestamp);
    if (epoch_number != null) {
      builder.setEpochNumber(Value.newBuilder().setNumberValue(epoch_number));
    }
    if (keyValueMapping != null) {
      builder.setAttribute(keyValueMapping.getProtoKeyValue());
    }
    if (artifactMapping != null) {
      builder.setArtifact(artifactMapping.getProtoObject());
    }
    return builder.build();
  }
}
