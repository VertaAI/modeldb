package ai.verta.modeldb.entities.versioning;

import ai.verta.common.KeyValue;
import ai.verta.common.WorkspaceTypeEnum;
import ai.verta.modeldb.DatasetVisibilityEnum.DatasetVisibility;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.common.authservice.UACApisUtil;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEnums.RepositoryModifierEnum;
import ai.verta.modeldb.entities.versioning.RepositoryEnums.RepositoryTypeEnum;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.modeldb.versioning.Repository;
import ai.verta.modeldb.versioning.RepositoryVisibilityEnum.RepositoryVisibility;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.GetResourcesResponseItem.OwnerTrackingCase;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.Workspace;
import java.io.Serializable;
import java.util.*;
import javax.persistence.*;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "repository")
public class RepositoryEntity implements Serializable {

  public RepositoryEntity() {}

  public RepositoryEntity(Repository repository, RepositoryTypeEnum repositoryTypeEnum) {
    this.name = ModelDBUtils.checkEntityNameLength(repository.getName());
    this.description = repository.getDescription();
    if (repository.getDateCreated() != 0L) {
      this.date_created = repository.getDateCreated();
    } else {
      this.date_created = new Date().getTime();
    }
    if (repository.getDateUpdated() != 0L) {
      this.date_updated = repository.getDateUpdated();
    } else {
      this.date_updated = new Date().getTime();
    }
    this.repositoryVisibility = repository.getVisibility();

    setAttributeMapping(
        RdbmsUtils.convertAttributesFromAttributeEntityList(
            this, ModelDBConstants.ATTRIBUTES, repository.getAttributesList()));
    switch (repositoryTypeEnum) {
      case DATASET:
        this.datasetRepositoryMappingEntity = new DatasetRepositoryMappingEntity(this);
        this.repositoryAccessModifier = RepositoryModifierEnum.PROTECTED.ordinal();
        break;
      default:
        this.repositoryAccessModifier = RepositoryModifierEnum.REGULAR.ordinal();
        break;
    }

    this.version_number = repository.getVersionNumber();
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UNSIGNED")
  private Long id;

  @Column(name = "name", columnDefinition = "varchar", length = 256)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "date_created")
  private Long date_created;

  @Column(name = "date_updated")
  private Long date_updated;

  @Column(name = "workspace_service_id")
  private Long workspaceServiceId;

  @Column(name = "workspace_id")
  private String workspace_id;

  @Column(name = "workspace_type", columnDefinition = "varchar")
  private Integer workspace_type;

  @OrderBy("date_created")
  @ManyToMany(mappedBy = "repository")
  private Set<CommitEntity> commits = new HashSet<>();

  @Column(name = "owner")
  private String owner;

  @Column(name = "repository_visibility")
  private Integer repository_visibility = null;

  @Transient private ResourceVisibility repositoryVisibility = ResourceVisibility.PRIVATE;

  @Column(name = "repository_access_modifier")
  private Integer repositoryAccessModifier = null;

  @Column(name = "deleted")
  private Boolean deleted = false;

  @OneToMany(
      targetEntity = AttributeEntity.class,
      mappedBy = "repositoryEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<AttributeEntity> attributeMapping;

  @OneToOne(mappedBy = "repositoryEntity", cascade = CascadeType.ALL)
  private DatasetRepositoryMappingEntity datasetRepositoryMappingEntity;

  @Column(name = "created")
  private Boolean created = false;

  @Column(name = "visibility_migration")
  private Boolean visibility_migration = false;

  @Column(name = "version_number")
  private Long version_number;

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Long getDate_created() {
    return date_created;
  }

  public Long getDate_updated() {
    return date_updated;
  }

  public void setDate_updated(Long date_updated) {
    this.date_updated = date_updated;
  }

  public Long getWorkspaceServiceId() {
    return workspaceServiceId;
  }

  public void setWorkspaceServiceId(Long workspaceServiceId) {
    this.workspaceServiceId = workspaceServiceId;
  }

  public String getWorkspace_id() {
    return workspace_id;
  }

  public Set<CommitEntity> getCommits() {
    return commits;
  }

  public Integer getWorkspace_type() {
    return workspace_type;
  }

  public Boolean getDeleted() {
    return deleted;
  }

  public void setDeleted(Boolean deleted) {
    this.deleted = deleted;
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

  public void setCreated(Boolean created) {
    this.created = created;
  }

  public ResourceVisibility getRepositoryVisibility() {
    return repositoryVisibility;
  }

  public void setVisibility_migration(Boolean visibility_migration) {
    this.visibility_migration = visibility_migration;
  }

  public void increaseVersionNumber() {
    this.version_number = this.version_number + 1L;
  }

  public Repository toProto(
      MDBRoleService mdbRoleService,
      UACApisUtil uacApisUtil,
      Map<Long, Workspace> cacheWorkspaceMap,
      Map<String, GetResourcesResponseItem> getResourcesMap) {
    final var builder = Repository.newBuilder().setId(this.id);
    builder
        .setName(this.name)
        .setDateCreated(this.date_created)
        .setDateUpdated(this.date_updated)
        .addAllAttributes(
            RdbmsUtils.convertAttributeEntityListFromAttributes(getAttributeMapping()))
        .setVersionNumber(this.version_number);

    var modelDBServiceResourceTypes =
        ModelDBUtils.getModelDBServiceResourceTypesFromRepository(this);

    GetResourcesResponseItem responseItem;
    if (getResourcesMap != null
        && !getResourcesMap.isEmpty()
        && getResourcesMap.containsKey(String.valueOf(this.id))) {
      responseItem = getResourcesMap.get(String.valueOf(this.id));
    } else {
      responseItem =
          mdbRoleService.getEntityResource(
              Optional.of(String.valueOf(this.id)), Optional.empty(), modelDBServiceResourceTypes);
      if (getResourcesMap == null) {
        getResourcesMap = new HashMap<>();
      }
      getResourcesMap.put(String.valueOf(this.id), responseItem);
    }
    builder.setVisibility(responseItem.getVisibility());
    builder.setWorkspaceServiceId(responseItem.getWorkspaceId());
    if (responseItem.getOwnerTrackingCase() == OwnerTrackingCase.GROUP_OWNER_ID) {
      builder.setGroupOwnerId(responseItem.getGroupOwnerId());
      builder.setOwner("");
    } else {
      builder.setOwnerId(responseItem.getOwnerId());
      builder.setOwner(String.valueOf(responseItem.getOwnerId()));
    }
    builder.setCustomPermission(responseItem.getCustomPermission());

    RepositoryVisibility visibility;
    if (isDataset()) {
      var datasetVisibility =
          (DatasetVisibility)
              ModelDBUtils.getOldVisibility(
                  modelDBServiceResourceTypes, responseItem.getVisibility());
      visibility = RepositoryVisibility.forNumber(datasetVisibility.getNumber());
    } else {
      visibility =
          (RepositoryVisibility)
              ModelDBUtils.getOldVisibility(
                  modelDBServiceResourceTypes, responseItem.getVisibility());
    }

    builder.setRepositoryVisibility(visibility);

    Workspace workspace;
    if (cacheWorkspaceMap.containsKey(responseItem.getWorkspaceId())) {
      workspace = cacheWorkspaceMap.get(responseItem.getWorkspaceId());
    } else {
      try {
        workspace = uacApisUtil.getWorkspaceById(responseItem.getWorkspaceId()).blockAndGet();
        cacheWorkspaceMap.put(workspace.getId(), workspace);
      } catch (Exception e) {
        throw new ModelDBException(e);
      }
    }

    switch (workspace.getInternalIdCase()) {
      case ORG_ID:
        builder.setWorkspaceId(workspace.getOrgId());
        builder.setWorkspaceTypeValue(WorkspaceTypeEnum.WorkspaceType.ORGANIZATION_VALUE);
        break;
      case USER_ID:
        builder.setWorkspaceId(workspace.getUserId());
        builder.setWorkspaceTypeValue(WorkspaceTypeEnum.WorkspaceType.USER_VALUE);
        break;
      default:
        // Do nothing
        break;
    }

    if (description != null) {
      builder.setDescription(description);
    }

    return builder.build();
  }

  public void update(Repository repository) {
    this.name = ModelDBUtils.checkEntityNameLength(repository.getName());
    this.description = repository.getDescription();
    this.repositoryVisibility = repository.getVisibility();
    update();
    updateAttribute(repository.getAttributesList());
  }

  public void update() {
    this.date_updated = new Date().getTime();
    increaseVersionNumber();
  }

  public String getOwner() {
    return owner;
  }

  public Integer getRepository_visibility() {
    return repository_visibility;
  }

  private void updateAttribute(List<KeyValue> attributes) {
    if (attributes != null && !attributes.isEmpty()) {
      for (KeyValue attribute : attributes) {
        var updatedAttributeObj =
            RdbmsUtils.generateAttributeEntity(this, ModelDBConstants.ATTRIBUTES, attribute);

        List<AttributeEntity> existingAttributes = this.getAttributeMapping();
        if (!existingAttributes.isEmpty()) {
          var doesExist = false;
          for (AttributeEntity existingAttribute : existingAttributes) {
            if (existingAttribute.getKey().equals(attribute.getKey())) {
              existingAttribute.setKey(updatedAttributeObj.getKey());
              existingAttribute.setValue(updatedAttributeObj.getValue());
              existingAttribute.setValue_type(updatedAttributeObj.getValue_type());
              doesExist = true;
              break;
            }
          }
          if (!doesExist) {
            this.setAttributeMapping(Collections.singletonList(updatedAttributeObj));
          }
        } else {
          this.setAttributeMapping(Collections.singletonList(updatedAttributeObj));
        }
      }
    }
  }

  public boolean isDataset() {
    return datasetRepositoryMappingEntity != null;
  }

  public boolean isProtected() {
    return Objects.equals(repositoryAccessModifier, RepositoryModifierEnum.PROTECTED.ordinal());
  }
}
