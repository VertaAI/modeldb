package ai.verta.modeldb.entities;

import ai.verta.modeldb.App;
import ai.verta.modeldb.Artifact;
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

@Entity
@Table(name = "artifact")
public class ArtifactEntity {

  public ArtifactEntity() {}

  public ArtifactEntity(Object entity, String fieldType, Artifact artifact) {
    App app = App.getInstance();
    setKey(artifact.getKey());
    setPath(artifact.getPath());
    if (!artifact.getPathOnly()) {
      setStore_type_path(app.getStoreTypePathPrefix() + artifact.getPath());
    }
    setArtifact_type(artifact.getArtifactTypeValue());
    setPath_only(artifact.getPathOnly());
    setLinked_artifact_id(artifact.getLinkedArtifactId());
    setFilename_extension(artifact.getFilenameExtension());

    if (entity instanceof ProjectEntity) {
      setProjectEntity(entity);
    } else if (entity instanceof ExperimentEntity) {
      setExperimentEntity(entity);
    } else if (entity instanceof ExperimentRunEntity) {
      setExperimentRunEntity(entity);
    } else if (entity instanceof CodeVersionEntity) {
      // OneToOne mapping, do nothing
    } else if (entity instanceof ObservationEntity) {
      // OneToOne mapping, do nothing
    } else {
      throw new IllegalStateException("unexpected entity class " + entity.getClass());
    }

    this.field_type = fieldType;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "ar_key", columnDefinition = "TEXT")
  private String key;

  @Column(name = "ar_path", columnDefinition = "TEXT")
  private String path;

  @Column(name = "store_type_path", columnDefinition = "TEXT")
  private String store_type_path;

  @Column(name = "artifact_type")
  private Integer artifact_type;

  @Column(name = "path_only")
  private Boolean path_only;

  @Column(name = "linked_artifact_id")
  private String linked_artifact_id;

  @Column(name = "filename_extension", length = 50)
  private String filename_extension;

  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  private ProjectEntity projectEntity;

  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "experiment_id")
  private ExperimentEntity experimentEntity;

  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "experiment_run_id")
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

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getStore_type_path() {
    return store_type_path;
  }

  public void setStore_type_path(String store_type_path) {
    this.store_type_path = store_type_path;
  }

  public Integer getArtifact_type() {
    return artifact_type;
  }

  public void setArtifact_type(Integer artifactType) {
    this.artifact_type = artifactType;
  }

  public Boolean getPath_only() {
    return path_only;
  }

  public void setPath_only(Boolean path_only) {
    this.path_only = path_only;
  }

  public String getLinked_artifact_id() {
    return linked_artifact_id;
  }

  public void setLinked_artifact_id(String linked_artifact_id) {
    this.linked_artifact_id = linked_artifact_id;
  }

  public String getFilename_extension() {
    return filename_extension;
  }

  public void setFilename_extension(String filename_extension) {
    this.filename_extension = filename_extension;
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

  private void setExperimentEntity(Object entity) {
    this.experimentEntity = (ExperimentEntity) entity;
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

  public Artifact getProtoObject() {
    return Artifact.newBuilder()
        .setKey(key)
        .setPath(path)
        .setArtifactTypeValue(artifact_type)
        .setPathOnly(path_only)
        .setLinkedArtifactId(linked_artifact_id)
        .setFilenameExtension(filename_extension)
        .build();
  }
}
