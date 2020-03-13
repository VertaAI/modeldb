package ai.verta.modeldb.entities;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.utils.RdbmsUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
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
    setProject_visibility(project.getProjectVisibilityValue());
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

  public void setProject_visibility(Integer project_visibility) {
    this.project_visibility = project_visibility;
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

  public Project getProtoObject() throws InvalidProtocolBufferException {
    Project.Builder projectBuilder =
        Project.newBuilder()
            .setId(getId())
            .setName(getName())
            .setShortName(getShort_name())
            .setDescription(getDescription())
            .setDateCreated(getDate_created())
            .setDateUpdated(getDate_updated())
            .setProjectVisibilityValue(getProject_visibility())
            .addAllAttributes(
                RdbmsUtils.convertAttributeEntityListFromAttributes(getAttributeMapping()))
            .addAllTags(RdbmsUtils.convertTagsMappingListFromTagList(getTags()))
            .addAllArtifacts(
                RdbmsUtils.convertArtifactEntityListFromArtifacts(
                    getArtifactMapping(ModelDBConstants.ARTIFACTS)))
            .setOwner(getOwner())
            .setReadmeText(getReadme_text())
            .setWorkspaceId(getWorkspace())
            .setWorkspaceTypeValue(getWorkspace_type());

    if (getCode_version_snapshot() != null) {
      projectBuilder.setCodeVersionSnapshot(getCode_version_snapshot().getProtoObject());
    }

    return projectBuilder.build();
  }
}
