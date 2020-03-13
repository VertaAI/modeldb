package ai.verta.modeldb.entities;

import ai.verta.modeldb.Feature;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "feature")
public class FeatureEntity {

  public FeatureEntity() {}

  public FeatureEntity(Object entity, Feature feature) {
    setName(feature.getName());

    if (entity instanceof ProjectEntity) {
      setProjectEntity(entity);
    } else if (entity instanceof ExperimentEntity) {
      setExperimentEntity(entity);
    } else if (entity instanceof ExperimentRunEntity) {
      setExperimentRunEntity(entity);
    } else if (entity instanceof RawDatasetVersionInfoEntity) {
      setRawDatasetVersionInfoEntity(entity);
    }
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "feature", columnDefinition = "TEXT")
  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  private ProjectEntity projectEntity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "experiment_id")
  private ExperimentEntity experimentEntity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "experiment_run_id")
  private ExperimentRunEntity experimentRunEntity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "raw_dataset_version_info_id")
  private RawDatasetVersionInfoEntity rawDatasetVersionInfoEntity;

  @Column(name = "entity_name", length = 50)
  private String entityName;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  private void setExperimentRunEntity(Object experimentRunEntity) {
    this.experimentRunEntity = (ExperimentRunEntity) experimentRunEntity;
    this.entityName = this.experimentRunEntity.getClass().getSimpleName();
  }

  public RawDatasetVersionInfoEntity getRawDatasetVersionInfoEntity() {
    return rawDatasetVersionInfoEntity;
  }

  private void setRawDatasetVersionInfoEntity(Object experimentRunEntity) {
    this.rawDatasetVersionInfoEntity = (RawDatasetVersionInfoEntity) experimentRunEntity;
    this.entityName = this.rawDatasetVersionInfoEntity.getClass().getSimpleName();
  }

  public Feature getProtoObject() {
    return Feature.newBuilder().setName(name).build();
  }
}
