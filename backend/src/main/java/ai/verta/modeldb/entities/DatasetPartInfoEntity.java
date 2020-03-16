package ai.verta.modeldb.entities;

import ai.verta.modeldb.DatasetPartInfo;
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
@Table(name = "dataset_part_info")
public class DatasetPartInfoEntity {

  public DatasetPartInfoEntity() {}

  public DatasetPartInfoEntity(Object entity, String fieldType, DatasetPartInfo datasetPartInfo) {

    setPath(datasetPartInfo.getPath());
    setSize(datasetPartInfo.getSize());
    setChecksum(datasetPartInfo.getChecksum());
    setLast_modified_at_source(datasetPartInfo.getLastModifiedAtSource());

    if (entity instanceof PathDatasetVersionInfoEntity) {
      setPathDatasetVersionInfoEntity(entity);
    }

    this.field_type = fieldType;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "path")
  private String path;

  @Column(name = "size")
  private Long size;

  @Column(name = "checksum")
  private String checksum;

  @Column(name = "last_modified_at_source")
  private Long last_modified_at_source;

  @Column(name = "entity_name", length = 50)
  private String entity_name;

  @Column(name = "field_type", length = 50)
  private String field_type;

  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "path_dataset_version_info_id")
  private PathDatasetVersionInfoEntity pathDatasetVersionInfoEntity;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Long getSize() {
    return size;
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public String getChecksum() {
    return checksum;
  }

  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  public Long getLast_modified_at_source() {
    return last_modified_at_source;
  }

  public void setLast_modified_at_source(Long last_modified_at_source) {
    this.last_modified_at_source = last_modified_at_source;
  }

  public PathDatasetVersionInfoEntity getPathDatasetVersionInfoEntity() {
    return pathDatasetVersionInfoEntity;
  }

  public void setPathDatasetVersionInfoEntity(Object pathDatasetVersionInfoEntity) {
    this.pathDatasetVersionInfoEntity = (PathDatasetVersionInfoEntity) pathDatasetVersionInfoEntity;
    this.entity_name = this.pathDatasetVersionInfoEntity.getClass().getSimpleName();
  }

  public DatasetPartInfo getProtoObject() {
    return DatasetPartInfo.newBuilder()
        .setPath(getPath())
        .setSize(getSize())
        .setChecksum(getChecksum())
        .setLastModifiedAtSource(getLast_modified_at_source())
        .build();
  }
}
