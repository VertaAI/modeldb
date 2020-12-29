package ai.verta.modeldb.entities;

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
@Table(name = "tag_mapping")
public class TagsMapping {

  public TagsMapping() {}

  public TagsMapping(Object entity, String tag) {
    setTag(tag);

    if (entity instanceof ProjectEntity) {
      setProjectEntity(entity);
    } else if (entity instanceof ExperimentEntity) {
      setExperimentEntity(entity);
    } else if (entity instanceof ExperimentRunEntity) {
      setExperimentRunEntity(entity);
    } else if (entity instanceof DatasetEntity) {
      setDatasetEntity(entity);
    } else if (entity instanceof DatasetVersionEntity) {
      setDatasetVersionEntity(entity);
    }
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "tags", columnDefinition = "TEXT")
  private String tags;

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
  @JoinColumn(name = "dataset_id")
  private DatasetEntity datasetEntity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dataset_version_id")
  private DatasetVersionEntity datasetVersionEntity;

  @Column(name = "entity_name", length = 50)
  private String entityName;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTag() {
    return tags;
  }

  public void setTag(String tag) {
    this.tags = tag;
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

  public DatasetEntity getDatasetEntity() {
    return datasetEntity;
  }

  private void setDatasetEntity(Object datasetEntity) {
    this.datasetEntity = (DatasetEntity) datasetEntity;
    this.entityName = this.datasetEntity.getClass().getSimpleName();
  }

  public DatasetVersionEntity getDatasetVersionEntity() {
    return datasetVersionEntity;
  }

  private void setDatasetVersionEntity(Object datasetVersionEntity) {
    this.datasetVersionEntity = (DatasetVersionEntity) datasetVersionEntity;
    this.entityName = this.datasetVersionEntity.getClass().getSimpleName();
  }
}
