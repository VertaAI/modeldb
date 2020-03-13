package ai.verta.modeldb.entities;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.Value.Builder;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Entity
@Table(name = "attribute")
public class AttributeEntity {

  private static Logger LOGGER = LogManager.getLogger(AttributeEntity.class);

  public AttributeEntity() {}

  public AttributeEntity(Object entity, String fieldType, KeyValue keyValue)
      throws InvalidProtocolBufferException {
    setKey(keyValue.getKey());
    setValue(ModelDBUtils.getStringFromProtoObject(keyValue.getValue()));
    setValue_type(keyValue.getValueTypeValue());

    if (entity instanceof ProjectEntity) {
      setProjectEntity(entity);
    } else if (entity instanceof ExperimentEntity) {
      setExperimentEntity(entity);
    } else if (entity instanceof ExperimentRunEntity) {
      setExperimentRunEntity(entity);
    } else if (entity instanceof ObservationEntity) {
      // OneToOne mapping, do nothing
    } else if (entity instanceof JobEntity) {
      setJobEntity(entity);
    } else if (entity instanceof DatasetEntity) {
      setDatasetEntity(entity);
    } else if (entity instanceof DatasetVersionEntity) {
      setDatasetVersionEntity(entity);
    }

    this.field_type = fieldType;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "kv_key", columnDefinition = "TEXT")
  private String key;

  @Column(name = "kv_value", columnDefinition = "TEXT")
  private String value;

  @Column(name = "value_type")
  private Integer value_type;

  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  private ProjectEntity projectEntity;

  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "experiment_id")
  private ExperimentEntity experimentEntity;

  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "experiment_run_id")
  private ExperimentRunEntity experimentRunEntity;

  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "job_id")
  private JobEntity jobEntity;

  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "dataset_id")
  private DatasetEntity datasetEntity;

  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "dataset_version_id")
  private DatasetVersionEntity datasetVersionEntity;

  @Column(name = "entity_name", length = 50)
  private String entity_name;

  @Column(name = "field_type", length = 50)
  private String field_type;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Integer getValue_type() {
    return value_type;
  }

  public void setValue_type(Integer valueType) {
    this.value_type = valueType;
  }

  public ProjectEntity getProjectEntity() {
    return projectEntity;
  }

  private void setProjectEntity(Object entity) {
    this.projectEntity = (ProjectEntity) entity;
    this.entity_name = this.projectEntity.getClass().getSimpleName();
  }

  public ExperimentEntity getExperimentEntity() {
    return experimentEntity;
  }

  private void setExperimentEntity(Object experimentEntity) {
    this.experimentEntity = (ExperimentEntity) experimentEntity;
    this.entity_name = this.experimentEntity.getClass().getSimpleName();
  }

  public ExperimentRunEntity getExperimentRunEntity() {
    return experimentRunEntity;
  }

  private void setExperimentRunEntity(Object entity) {
    this.experimentRunEntity = (ExperimentRunEntity) entity;
    this.entity_name = this.experimentRunEntity.getClass().getSimpleName();
  }

  public JobEntity getJobEntity() {
    return jobEntity;
  }

  public void setJobEntity(Object entity) {
    this.jobEntity = (JobEntity) entity;
    this.entity_name = this.jobEntity.getClass().getSimpleName();
  }

  public DatasetEntity getDatasetEntity() {
    return datasetEntity;
  }

  public void setDatasetEntity(Object entity) {
    this.datasetEntity = (DatasetEntity) entity;
    this.entity_name = this.datasetEntity.getClass().getSimpleName();
  }

  public DatasetVersionEntity getDatasetVersionEntity() {
    return datasetVersionEntity;
  }

  public void setDatasetVersionEntity(Object entity) {
    this.datasetVersionEntity = (DatasetVersionEntity) entity;
    this.entity_name = this.datasetVersionEntity.getClass().getSimpleName();
  }

  public String getField_type() {
    return field_type;
  }

  public KeyValue getProtoObj() throws InvalidProtocolBufferException {
    Builder valueBuilder = Value.newBuilder();
    try {
      valueBuilder = (Builder) ModelDBUtils.getProtoObjectFromString(value, valueBuilder);
    } catch (InvalidProtocolBufferException e) {
      LOGGER.warn("Error generating builder for {}", value);
      throw e;
    }
    return KeyValue.newBuilder()
        .setKey(key)
        .setValue(valueBuilder.build())
        .setValueTypeValue(value_type)
        .build();
  }
}
