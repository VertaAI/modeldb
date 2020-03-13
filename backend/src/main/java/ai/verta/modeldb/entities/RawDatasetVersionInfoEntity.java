package ai.verta.modeldb.entities;

import ai.verta.modeldb.Feature;
import ai.verta.modeldb.RawDatasetVersionInfo;
import ai.verta.modeldb.utils.RdbmsUtils;
import java.util.ArrayList;
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
@Table(name = "raw_dataset_version_info")
public class RawDatasetVersionInfoEntity {

  public RawDatasetVersionInfoEntity() {}

  public RawDatasetVersionInfoEntity(
      String fieldType, RawDatasetVersionInfo rawDatasetVersionInfo) {

    setSize(rawDatasetVersionInfo.getSize());
    List<Feature> features = new ArrayList<>();
    for (String feature : rawDatasetVersionInfo.getFeaturesList()) {
      features.add(Feature.newBuilder().setName(feature).build());
    }
    setFeatures(RdbmsUtils.convertFeatureListFromFeatureMappingList(this, features));
    setNum_records(rawDatasetVersionInfo.getNumRecords());
    setObject_path(rawDatasetVersionInfo.getObjectPath());
    setChecksum(rawDatasetVersionInfo.getChecksum());

    this.field_type = fieldType;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "size")
  private Long size;

  @OneToMany(
      targetEntity = FeatureEntity.class,
      mappedBy = "rawDatasetVersionInfoEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<FeatureEntity> features;

  @Column(name = "num_records")
  private Long num_records;

  @Column(name = "object_path")
  private String object_path;

  @Column(name = "checksum")
  private String checksum;

  @Column(name = "field_type", length = 50)
  private String field_type;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getSize() {
    return size;
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public List<FeatureEntity> getFeatures() {
    return features;
  }

  public void setFeatures(List<FeatureEntity> features) {
    this.features = features;
  }

  public Long getNum_records() {
    return num_records;
  }

  public void setNum_records(Long num_records) {
    this.num_records = num_records;
  }

  public String getObject_path() {
    return object_path;
  }

  public void setObject_path(String object_path) {
    this.object_path = object_path;
  }

  public String getChecksum() {
    return checksum;
  }

  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  public RawDatasetVersionInfo getProtoObject() {
    List<String> features = new ArrayList<>();
    for (FeatureEntity feature : getFeatures()) {
      features.add(feature.getName());
    }
    return RawDatasetVersionInfo.newBuilder()
        .setSize(getSize())
        .addAllFeatures(features)
        .setNumRecords(getNum_records())
        .setObjectPath(getObject_path())
        .setChecksum(getChecksum())
        .build();
  }
}
