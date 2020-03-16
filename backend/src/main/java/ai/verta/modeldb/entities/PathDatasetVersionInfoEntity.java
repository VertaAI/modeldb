package ai.verta.modeldb.entities;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.PathDatasetVersionInfo;
import ai.verta.modeldb.utils.RdbmsUtils;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "path_dataset_version_info")
public class PathDatasetVersionInfoEntity {

  public PathDatasetVersionInfoEntity() {}

  public PathDatasetVersionInfoEntity(
      String fieldType, PathDatasetVersionInfo pathDatasetVersionInfo) {

    setLocation_type(pathDatasetVersionInfo.getLocationTypeValue());
    setSize(pathDatasetVersionInfo.getSize());
    setBase_path(pathDatasetVersionInfo.getBasePath());
    setDataset_part_infos(
        RdbmsUtils.convertDatasetPartInfosFromDatasetPartInfoEntityList(
            this,
            ModelDBConstants.PATH_DATSET_VERSION_INFO,
            pathDatasetVersionInfo.getDatasetPartInfosList()));

    this.field_type = fieldType;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "location_type")
  private Integer location_type;

  @Column(name = "size")
  private Long size;

  @Column(name = "base_path")
  private String base_path;

  @OneToMany(
      targetEntity = DatasetPartInfoEntity.class,
      mappedBy = "pathDatasetVersionInfoEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<DatasetPartInfoEntity> dataset_part_infos;

  @Column(name = "field_type", length = 50)
  private String field_type;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Integer getLocation_type() {
    return location_type;
  }

  public void setLocation_type(Integer location_type) {
    this.location_type = location_type;
  }

  public Long getSize() {
    return size;
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public String getBase_path() {
    return base_path;
  }

  public void setBase_path(String base_path) {
    this.base_path = base_path;
  }

  public List<DatasetPartInfoEntity> getDataset_part_infos() {
    return dataset_part_infos;
  }

  public void setDataset_part_infos(List<DatasetPartInfoEntity> dataset_part_infos) {
    this.dataset_part_infos = dataset_part_infos;
  }

  public PathDatasetVersionInfo getProtoObject() {
    return PathDatasetVersionInfo.newBuilder()
        .setLocationTypeValue(getLocation_type())
        .setSize(getSize())
        .setBasePath(getBase_path())
        .addAllDatasetPartInfos(
            RdbmsUtils.convertDatasetPartInfoEntityListFromDatasetPartInfos(
                getDataset_part_infos()))
        .build();
  }
}
