package ai.verta.modeldb.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "artifactStore")
public class ArtifactStoreMapping {

  public ArtifactStoreMapping() {}

  public ArtifactStoreMapping(
      String entityName,
      String entityId,
      String clientKey,
      String cloudStorageKey,
      String cloudStorageFilePath) {

    setEntity_id(entityId);
    setEntity_name(entityName);
    setClient_key(clientKey);
    setCloud_storage_key(cloudStorageKey);
    setCloud_storage_file_path(cloudStorageFilePath);
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @Column(name = "client_key", columnDefinition = "TEXT")
  private String client_key;

  @Column(name = "cloud_storage_key", columnDefinition = "TEXT")
  private String cloud_storage_key;

  @Column(name = "cloud_storage_file_path", columnDefinition = "TEXT")
  private String cloud_storage_file_path;

  @Column(name = "entity_id", length = 50)
  private String entity_id;

  @Column(name = "entity_name", length = 50)
  private String entity_name;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getClient_key() {
    return client_key;
  }

  public void setClient_key(String clientKey) {
    this.client_key = clientKey;
  }

  public String getCloud_storage_key() {
    return cloud_storage_key;
  }

  public void setCloud_storage_key(String cloudStorageKey) {
    this.cloud_storage_key = cloudStorageKey;
  }

  public String getCloud_storage_file_path() {
    return cloud_storage_file_path;
  }

  public void setCloud_storage_file_path(String cloudStorageFilePath) {
    this.cloud_storage_file_path = cloudStorageFilePath;
  }

  public String getEntity_id() {
    return entity_id;
  }

  public void setEntity_id(String entity_id) {
    this.entity_id = entity_id;
  }

  public String getEntity_name() {
    return entity_name;
  }

  public void setEntity_name(String entity_name) {
    this.entity_name = entity_name;
  }
}
