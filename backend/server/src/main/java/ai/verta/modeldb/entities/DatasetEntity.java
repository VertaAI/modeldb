package ai.verta.modeldb.entities;

import ai.verta.common.ModelDBResourceEnum;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.Dataset;
import ai.verta.modeldb.DatasetVisibilityEnum;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.GetResourcesResponseItem.OwnerTrackingCase;
import ai.verta.uac.ResourceVisibility;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.*;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "dataset")
public class DatasetEntity implements Serializable {

  public DatasetEntity() {}

  public DatasetEntity(Dataset dataset) {

    setId(dataset.getId());
    setName(dataset.getName());
    setDescription(dataset.getDescription());
    setTags(RdbmsUtils.convertTagListFromTagMappingList(this, dataset.getTagsList()));
    setDatasetVisibility(dataset.getVisibility());
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

  @Transient private ResourceVisibility datasetVisibility = ResourceVisibility.PRIVATE;

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

  @Column(name = "workspace_id")
  private Long workspaceServiceId;

  @Column(name = "workspace")
  private String workspace;

  @Column(name = "workspace_type")
  private Integer workspace_type;

  @Column(name = "deleted")
  private Boolean deleted = false;

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

  public ResourceVisibility getDatasetVisibility() {
    return datasetVisibility;
  }

  public void setDatasetVisibility(ResourceVisibility datasetVisibility) {
    this.datasetVisibility = datasetVisibility;
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

  public Long getWorkspaceServiceId() {
    return workspaceServiceId;
  }

  public void setWorkspaceServiceId(Long workspaceId) {
    this.workspaceServiceId = workspaceId;
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

  public Boolean getDeleted() {
    return deleted;
  }

  public void setDeleted(Boolean deleted) {
    this.deleted = deleted;
  }

  public Dataset getProtoObject(MDBRoleService mdbRoleService) {
    var datasetBuilder =
        Dataset.newBuilder()
            .setId(getId())
            .setName(getName())
            .setOwner(getOwner())
            .setDescription(getDescription())
            .addAllTags(RdbmsUtils.convertTagsMappingListFromTagList(getTags()))
            .setDatasetTypeValue(getDataset_type())
            .addAllAttributes(
                RdbmsUtils.convertAttributeEntityListFromAttributes(getAttributeMapping()))
            .setTimeCreated(getTime_created())
            .setTimeUpdated(getTime_updated())
            .setWorkspaceId(getWorkspace())
            .setWorkspaceTypeValue(getWorkspace_type());

    GetResourcesResponseItem repositoryResource =
        mdbRoleService.getEntityResource(
            Optional.ofNullable(String.valueOf(this.id)),
            Optional.empty(),
            ModelDBResourceEnum.ModelDBServiceResourceTypes.DATASET);
    datasetBuilder.setVisibility(repositoryResource.getVisibility());
    datasetBuilder.setWorkspaceServiceId(repositoryResource.getWorkspaceId());
    if (repositoryResource.getOwnerTrackingCase() == OwnerTrackingCase.GROUP_OWNER_ID) {
      datasetBuilder.setGroupOwnerId(repositoryResource.getGroupOwnerId());
      datasetBuilder.setOwner("");
    } else {
      datasetBuilder.setOwnerId(repositoryResource.getOwnerId());
      datasetBuilder.setOwner(String.valueOf(repositoryResource.getOwnerId()));
    }
    datasetBuilder.setCustomPermission(repositoryResource.getCustomPermission());

    DatasetVisibilityEnum.DatasetVisibility visibility =
        (DatasetVisibilityEnum.DatasetVisibility)
            ModelDBUtils.getOldVisibility(
                ModelDBServiceResourceTypes.DATASET, repositoryResource.getVisibility());
    datasetBuilder.setDatasetVisibility(visibility);

    return datasetBuilder.build();
  }
}
