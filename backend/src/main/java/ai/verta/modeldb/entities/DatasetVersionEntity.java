package ai.verta.modeldb.entities;

import ai.verta.modeldb.DatasetVersion;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.utils.RdbmsUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "dataset_version")
public class DatasetVersionEntity {

  public DatasetVersionEntity() {}

  public DatasetVersionEntity(DatasetVersion datasetVersion) throws InvalidProtocolBufferException {
    setId(datasetVersion.getId());
    setParent_id(datasetVersion.getParentId());
    setDataset_id(datasetVersion.getDatasetId());
    setTime_logged(datasetVersion.getTimeLogged());
    setDescription(datasetVersion.getDescription());
    setTags(RdbmsUtils.convertTagListFromTagMappingList(this, datasetVersion.getTagsList()));
    setDataset_version_visibility(datasetVersion.getDatasetVersionVisibilityValue());
    setDataset_type(datasetVersion.getDatasetTypeValue());
    setAttributeMapping(
        RdbmsUtils.convertAttributesFromAttributeEntityList(
            this, ModelDBConstants.ATTRIBUTES, datasetVersion.getAttributesList()));
    setOwner(datasetVersion.getOwner());
    setVersion(datasetVersion.getVersion());
    setTime_updated(datasetVersion.getTimeUpdated());

    if (datasetVersion.hasRawDatasetVersionInfo()) {
      setRaw_dataset_version_info(
          RdbmsUtils.generateRawDatasetVersionInfoEntity(
              ModelDBConstants.RAW_DATSET_VERSION_INFO, datasetVersion.getRawDatasetVersionInfo()));
    } else if (datasetVersion.hasPathDatasetVersionInfo()) {
      setPath_dataset_version_info(
          RdbmsUtils.generatePathDatasetVersionInfoEntity(
              ModelDBConstants.PATH_DATSET_VERSION_INFO,
              datasetVersion.getPathDatasetVersionInfo()));
    } else if (datasetVersion.hasQueryDatasetVersionInfo()) {
      setQuery_dataset_version_info(
          RdbmsUtils.generateQueryDatasetVersionInfoEntity(
              ModelDBConstants.PATH_DATSET_VERSION_INFO,
              datasetVersion.getQueryDatasetVersionInfo()));
    }
  }

  @Id
  @Column(name = "id", unique = true)
  private String id; // For backend reference

  @Column(name = "parent_id")
  private String parent_id;

  @Column(name = "dataset_id")
  private String dataset_id;

  @Column(name = "time_logged")
  private Long time_logged;

  @Column(name = "time_updated")
  private Long time_updated;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @OneToMany(
      targetEntity = TagsMapping.class,
      mappedBy = "datasetVersionEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<TagsMapping> tags;

  @Column(name = "dataset_version_visibility")
  private Integer dataset_version_visibility;

  @Column(name = "dataset_type")
  // this acts as a quick check on which type of DataSetInfo to look at for more
  // details.
  private Integer dataset_type;

  @OneToMany(
      targetEntity = KeyValueEntity.class,
      mappedBy = "datasetVersionEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<KeyValueEntity> keyValueMapping;

  @OneToMany(
      targetEntity = AttributeEntity.class,
      mappedBy = "datasetVersionEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<AttributeEntity> attributeMapping;

  @Column(name = "owner")
  private String owner;

  @Column(name = "version")
  private Long version;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "raw_dataset_version_info_id")
  private RawDatasetVersionInfoEntity raw_dataset_version_info;

  @OneToOne(targetEntity = PathDatasetVersionInfoEntity.class, cascade = CascadeType.ALL)
  @OrderBy("id")
  @JoinColumn(name = "path_dataset_version_info_id")
  private PathDatasetVersionInfoEntity path_dataset_version_info;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "query_dataset_version_info_id")
  private QueryDatasetVersionInfoEntity query_dataset_version_info;

  @Column(name = "deleted")
  private Boolean deleted = false;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getParent_id() {
    return parent_id;
  }

  public void setParent_id(String parent_id) {
    this.parent_id = parent_id;
  }

  public String getDataset_id() {
    return dataset_id;
  }

  public void setDataset_id(String dataset_id) {
    this.dataset_id = dataset_id;
  }

  public Long getTime_logged() {
    return time_logged;
  }

  public void setTime_logged(Long time_logged) {
    this.time_logged = time_logged;
  }

  public Long getTime_updated() {
    return time_updated;
  }

  public void setTime_updated(Long time_updated) {
    this.time_updated = time_updated;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<TagsMapping> getTags() {
    return tags;
  }

  public void setTags(List<TagsMapping> tags) {
    this.tags = tags;
  }

  public Integer getDataset_version_visibility() {
    return dataset_version_visibility;
  }

  public void setDataset_version_visibility(Integer dataset_version_visibility) {
    this.dataset_version_visibility = dataset_version_visibility;
  }

  public Integer getDataset_type() {
    return dataset_type;
  }

  public void setDataset_type(Integer dataset_type) {
    this.dataset_type = dataset_type;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public RawDatasetVersionInfoEntity getRaw_dataset_version_info() {
    return raw_dataset_version_info;
  }

  public void setRaw_dataset_version_info(RawDatasetVersionInfoEntity raw_dataset_version_info) {
    this.raw_dataset_version_info = raw_dataset_version_info;
  }

  public PathDatasetVersionInfoEntity getPath_dataset_version_info() {
    return path_dataset_version_info;
  }

  public void setPath_dataset_version_info(PathDatasetVersionInfoEntity path_dataset_version_info) {
    this.path_dataset_version_info = path_dataset_version_info;
  }

  public QueryDatasetVersionInfoEntity getQuery_dataset_version_info() {
    return query_dataset_version_info;
  }

  public void setQuery_dataset_version_info(
      QueryDatasetVersionInfoEntity query_dataset_version_info) {
    this.query_dataset_version_info = query_dataset_version_info;
  }

  public List<AttributeEntity> getAttributeMapping() {
    return attributeMapping;
  }

  public void setAttributeMapping(List<AttributeEntity> attributeMapping) {
    if (this.attributeMapping == null) {
      this.attributeMapping = new ArrayList<>();
    }
    this.attributeMapping.addAll(attributeMapping);
  }

  public Boolean getDeleted() {
    return deleted;
  }

  public void setDeleted(Boolean deleted) {
    this.deleted = deleted;
  }

  public DatasetVersion getProtoObject() throws InvalidProtocolBufferException {
    DatasetVersion.Builder datasetVersionBuilder =
        DatasetVersion.newBuilder()
            .setId(getId())
            .setParentId(getParent_id() != null ? getParent_id() : "")
            .setDatasetId(getDataset_id())
            .setTimeLogged(getTime_logged())
            .setTimeUpdated(getTime_updated())
            .setDescription(getDescription() != null ? getDescription() : "")
            .addAllTags(RdbmsUtils.convertTagsMappingListFromTagList(getTags()))
            .setDatasetVersionVisibilityValue(getDataset_version_visibility())
            .setDatasetTypeValue(getDataset_type())
            .addAllAttributes(
                RdbmsUtils.convertAttributeEntityListFromAttributes(getAttributeMapping()))
            .setOwner(getOwner())
            .setVersion(getVersion());

    if (getRaw_dataset_version_info() != null) {
      datasetVersionBuilder.setRawDatasetVersionInfo(
          getRaw_dataset_version_info().getProtoObject());
    } else if (getPath_dataset_version_info() != null) {
      datasetVersionBuilder.setPathDatasetVersionInfo(
          getPath_dataset_version_info().getProtoObject());
    } else if (getQuery_dataset_version_info() != null) {
      datasetVersionBuilder.setQueryDatasetVersionInfo(
          getQuery_dataset_version_info().getProtoObject());
    }

    return datasetVersionBuilder.build();
  }
}
