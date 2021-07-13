package ai.verta.modeldb.entities;

import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.WorkspaceTypeEnum;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.ProjectVisibility;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.common.ModelDBConstants;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.uac.GetResourcesResponseItem;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.Workspace;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import javax.persistence.*;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "project")
public class ProjectEntity {

  public ProjectEntity() {}

  public ProjectEntity(Project project) throws InvalidProtocolBufferException {
    setId(project.getId());
    setName(project.getName());
    setShort_name(project.getShortName());
    setDate_created(project.getDateCreated());
    setDate_updated(project.getDateUpdated());
    setDescription(project.getDescription());
    setProjectVisibility(project.getVisibility());
    setAttributeMapping(
        RdbmsUtils.convertAttributesFromAttributeEntityList(
            this, ModelDBConstants.ATTRIBUTES, project.getAttributesList()));
    setTags(RdbmsUtils.convertTagListFromTagMappingList(this, project.getTagsList()));
    setArtifactMapping(
        RdbmsUtils.convertArtifactsFromArtifactEntityList(
            this, ModelDBConstants.ARTIFACTS, project.getArtifactsList()));
    setOwner(project.getOwner());
    setReadme_text(project.getReadmeText());
    if (project.getCodeVersionSnapshot().hasCodeArchive()
        || project.getCodeVersionSnapshot().hasGitSnapshot()) {
      setCode_version_snapshot(
          RdbmsUtils.generateCodeVersionEntity(
              ModelDBConstants.CODE_VERSION, project.getCodeVersionSnapshot()));
    }
    setWorkspace(project.getWorkspaceId());
    setWorkspace_type(project.getWorkspaceTypeValue());
  }

  @Id
  @Column(name = "id", unique = true)
  private String id;

  @Column(name = "name")
  private String name;

  @Column(name = "short_name")
  private String short_name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "date_created")
  private Long date_created;

  @Column(name = "date_updated")
  private Long date_updated;

  @Column(name = "project_visibility")
  private Integer project_visibility;

  @Transient private ResourceVisibility projectVisibility = ResourceVisibility.PRIVATE;

  @OneToMany(
      targetEntity = KeyValueEntity.class,
      mappedBy = "projectEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<KeyValueEntity> keyValueMapping;

  @OneToMany(
      targetEntity = AttributeEntity.class,
      mappedBy = "projectEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<AttributeEntity> attributeMapping;

  @OneToMany(
      targetEntity = TagsMapping.class,
      mappedBy = "projectEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<TagsMapping> tags;

  @OneToMany(
      targetEntity = ArtifactEntity.class,
      mappedBy = "projectEntity",
      cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @OrderBy("id")
  private List<ArtifactEntity> artifactMapping;

  @Column(name = "owner")
  private String owner;

  @Column(name = "readme_text", columnDefinition = "TEXT")
  private String readme_text;

  @OneToOne(targetEntity = CodeVersionEntity.class, cascade = CascadeType.ALL)
  @OrderBy("id")
  private CodeVersionEntity code_version_snapshot;

  @Transient private Map<String, List<ArtifactEntity>> artifactEntityMap = new HashMap<>();

  @Column(name = "workspace_id")
  private Long workspaceServiceId;

  @Column(name = "workspace")
  private String workspace;

  @Column(name = "workspace_type")
  private Integer workspace_type;

  @Column(name = "deleted")
  private Boolean deleted = false;

  @Column(name = "created")
  private Boolean created = false;

  @Column(name = "visibility_migration")
  private Boolean visibility_migration = false;

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

  public String getShort_name() {
    return short_name;
  }

  public void setShort_name(String short_name) {
    this.short_name = short_name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getDate_created() {
    return date_created;
  }

  public void setDate_created(Long dateCreated) {
    this.date_created = dateCreated;
  }

  public Long getDate_updated() {
    return date_updated;
  }

  public void setDate_updated(Long dateUpdated) {
    this.date_updated = dateUpdated;
  }

  public Integer getProject_visibility() {
    return project_visibility;
  }

  public ResourceVisibility getProjectVisibility() {
    return projectVisibility;
  }

  public void setProjectVisibility(ResourceVisibility projectVisibility) {
    this.projectVisibility = projectVisibility;
  }

  public List<TagsMapping> getTags() {
    return tags;
  }

  public void setTags(List<TagsMapping> tags) {
    this.tags = tags;
  }

  private List<ArtifactEntity> getArtifactMapping(String fieldType) {

    if (artifactEntityMap.size() == 0) {
      addArtifactInMap(this.artifactMapping);
    }

    if (artifactMapping != null && artifactEntityMap.containsKey(fieldType)) {
      return artifactEntityMap.get(fieldType);
    } else {
      return Collections.emptyList();
    }
  }

  private void addArtifactInMap(List<ArtifactEntity> artifactMapping) {
    for (ArtifactEntity artifactEntity : artifactMapping) {
      List<ArtifactEntity> artifactEntities = artifactEntityMap.get(artifactEntity.getField_type());
      if (artifactEntities == null) {
        artifactEntities = new ArrayList<>();
      }
      artifactEntities.add(artifactEntity);
      artifactEntityMap.put(artifactEntity.getField_type(), artifactEntities);
    }
  }

  public void setArtifactMapping(List<ArtifactEntity> artifactMapping) {
    if (this.artifactMapping == null) {
      this.artifactMapping = new ArrayList<>();
    }

    if (artifactMapping != null) {
      this.artifactMapping.addAll(artifactMapping);
      if (artifactEntityMap.size() == 0) {
        // Add all artifact in the map
        addArtifactInMap(this.artifactMapping);
      } else {
        // Add new artifact in the map
        addArtifactInMap(artifactMapping);
      }
    }
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getReadme_text() {
    return readme_text;
  }

  public void setReadme_text(String readme_text) {
    this.readme_text = readme_text;
  }

  public CodeVersionEntity getCode_version_snapshot() {
    return code_version_snapshot;
  }

  public void setCode_version_snapshot(CodeVersionEntity code_version_snapshot) {
    this.code_version_snapshot = code_version_snapshot;
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

  public Boolean getCreated() {
    return created;
  }

  public void setCreated(Boolean created) {
    this.created = created;
  }

  public void setVisibility_migration(Boolean visibility_migration) {
    this.visibility_migration = visibility_migration;
  }

  public Project getProtoObject(
      RoleService roleService,
      AuthService authService,
      Map<Long, Workspace> cacheWorkspaceMap,
      Map<String, GetResourcesResponseItem> getResourcesMap)
      throws InvalidProtocolBufferException, ExecutionException, InterruptedException {
    Project.Builder projectBuilder =
        Project.newBuilder()
            .setId(getId())
            .setName(getName())
            .setShortName(getShort_name())
            .setDescription(getDescription())
            .setDateCreated(getDate_created())
            .setDateUpdated(getDate_updated())
            .setVisibility(getProjectVisibility())
            .addAllAttributes(
                RdbmsUtils.convertAttributeEntityListFromAttributes(getAttributeMapping()))
            .addAllTags(RdbmsUtils.convertTagsMappingListFromTagList(getTags()))
            .addAllArtifacts(
                RdbmsUtils.convertArtifactEntityListFromArtifacts(
                    getArtifactMapping(ModelDBConstants.ARTIFACTS)))
            .setOwner(getOwner())
            .setReadmeText(getReadme_text());

    if (getCode_version_snapshot() != null) {
      projectBuilder.setCodeVersionSnapshot(getCode_version_snapshot().getProtoObject());
    }

    GetResourcesResponseItem projectResource;
    if (getResourcesMap != null
        && !getResourcesMap.isEmpty()
        && getResourcesMap.containsKey(this.id)) {
      projectResource = getResourcesMap.get(this.id);
    } else {
      projectResource =
          roleService.getEntityResource(
              Optional.of(this.id), Optional.empty(), ModelDBServiceResourceTypes.PROJECT);
      if (getResourcesMap == null) {
        getResourcesMap = new HashMap<>();
      }
      getResourcesMap.put(this.id, projectResource);
    }
    projectBuilder.setVisibility(projectResource.getVisibility());
    projectBuilder.setWorkspaceServiceId(projectResource.getWorkspaceId());
    projectBuilder.setOwner(String.valueOf(projectResource.getOwnerId()));
    projectBuilder.setCustomPermission(projectResource.getCustomPermission());

    Workspace workspace;
    if (cacheWorkspaceMap.containsKey(projectResource.getWorkspaceId())) {
      workspace = cacheWorkspaceMap.get(projectResource.getWorkspaceId());
    } else {
      workspace = authService.workspaceById(false, projectResource.getWorkspaceId());
      cacheWorkspaceMap.put(workspace.getId(), workspace);
    }
    switch (workspace.getInternalIdCase()) {
      case ORG_ID:
        projectBuilder.setWorkspaceId(workspace.getOrgId());
        projectBuilder.setWorkspaceTypeValue(WorkspaceTypeEnum.WorkspaceType.ORGANIZATION_VALUE);
        break;
      case USER_ID:
        projectBuilder.setWorkspaceId(workspace.getUserId());
        projectBuilder.setWorkspaceTypeValue(WorkspaceTypeEnum.WorkspaceType.USER_VALUE);
        break;
    }

    ProjectVisibility visibility =
        (ProjectVisibility)
            ModelDBUtils.getOldVisibility(
                ModelDBServiceResourceTypes.PROJECT, projectResource.getVisibility());
    projectBuilder.setProjectVisibility(visibility);

    return projectBuilder.build();
  }
}
