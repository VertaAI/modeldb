package ai.verta.modeldb.entities;

import ai.verta.modeldb.Dataset;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.utils.RdbmsUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "dataset")
public class DatasetEntity {

  public DatasetEntity() {}

  public DatasetEntity(Dataset dataset) throws InvalidProtocolBufferException {

    setId(dataset.getId());
    setName(dataset.getName());
    setDescription(dataset.getDescription());
    setTags(RdbmsUtils.convertTagListFromTagMappingList(this, dataset.getTagsList()));
    setDataset_visibility(dataset.getDatasetVisibilityValue());
    setDataset_type(dataset.getDatasetTypeValue());
    setAttributeMapping(
        RdbmsUtils.convertAttributesFromAttributeEntityList(
            this, ModelDBConstants.ATTRIBUTES, dataset.getAttributesList()));
    setTime_created(dataset.getTimeCreated());
    setTime_updated(dataset.getTimeUpdated());
    setOwner(dataset.getOwner());
    setWorkspace(dataset.getWorkspaceId());
    setWorkspace_type(dataset.getWorkspaceTypeValue());
  }

  @Id
  @Column(name = "id", unique = true)
  private String id; // For backend reference

  @Column(name = "name")
  private String name;

  @Column(name = "owner")
  private String owner;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @OneToMany(
      targetEntity = TagsMapping.class,
      mappedBy = "datasetEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<TagsMapping> tags;

  @Column(name = "dataset_visibility")
  private Integer dataset_visibility;

  @Column(name = "dataset_type")
  private Integer dataset_type;

  @OneToMany(
      targetEntity = KeyValueEntity.class,
      mappedBy = "datasetEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<KeyValueEntity> keyValueMapping;

  @OneToMany(
      targetEntity = AttributeEntity.class,
      mappedBy = "datasetEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<AttributeEntity> attributeMapping;

  @Column(name = "time_created")
  private Long time_created;

  @Column(name = "time_updated")
  private Long time_updated;

  @Column(name = "workspace")
  private String workspace;

  @Column(name = "workspace_type")
  private Integer workspace_type;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
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

  public Integer getDataset_visibility() {
    return dataset_visibility;
  }

  public void setDataset_visibility(Integer dataset_visibility) {
    this.dataset_visibility = dataset_visibility;
  }

  public Integer getDataset_type() {
    return dataset_type;
  }

  public void setDataset_type(Integer dataset_type) {
    this.dataset_type = dataset_type;
  }

  public Long getTime_created() {
    return time_created;
  }

  public void setTime_created(Long time_created) {
    this.time_created = time_created;
  }

  public Long getTime_updated() {
    return time_updated;
  }

  public void setTime_updated(Long time_updated) {
    this.time_updated = time_updated;
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

  public String getWorkspace() {
    return workspace;
  }

  public void setWorkspace(String workspace) {
    this.workspace = workspace;
  }

  public Integer getWorkspace_type() {
    return workspace_type;
  }

  public void setWorkspace_type(Integer workspace_type) {
    this.workspace_type = workspace_type;
  }

  public Dataset getProtoObject() throws InvalidProtocolBufferException {
    return Dataset.newBuilder()
        .setId(getId())
        .setName(getName())
        .setOwner(getOwner())
        .setDescription(getDescription())
        .addAllTags(RdbmsUtils.convertTagsMappingListFromTagList(getTags()))
        .setDatasetVisibilityValue(getDataset_visibility())
        .setDatasetTypeValue(getDataset_type())
        .addAllAttributes(
            RdbmsUtils.convertAttributeEntityListFromAttributes(getAttributeMapping()))
        .setTimeCreated(getTime_created())
        .setTimeUpdated(getTime_updated())
        .setWorkspaceId(getWorkspace())
        .setWorkspaceTypeValue(getWorkspace_type())
        .build();
  }
}
