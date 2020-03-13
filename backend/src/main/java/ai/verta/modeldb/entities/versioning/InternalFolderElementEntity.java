package ai.verta.modeldb.entities.versioning;

import ai.verta.modeldb.versioning.FolderElement;
import ai.verta.modeldb.versioning.InternalFolderElement;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "folder_element")
public class InternalFolderElementEntity implements Serializable {
  public InternalFolderElementEntity() {}

  public InternalFolderElementEntity(
      InternalFolderElement internalFolderElement, String folderHash, String elementType) {
    this.folder_hash = folderHash;
    this.element_sha = internalFolderElement.getElementSha();
    this.element_type = elementType;
    this.element_name = internalFolderElement.getElementName();
  }

  public InternalFolderElementEntity(
      String folderHash, String elementSha, String elementType, String elementName) {
    this.folder_hash = folderHash;
    this.element_sha = elementSha;
    this.element_type = elementType;
    this.element_name = elementName;
  }

  @Id
  @Column(name = "folder_hash", nullable = false)
  private String folder_hash;

  @Column(name = "element_sha", nullable = false)
  private String element_sha;

  @Column(name = "element_type")
  private String element_type;

  @Id
  @Column(name = "element_name", nullable = false)
  private String element_name;

  public String getFolder_hash() {
    return folder_hash;
  }

  public String getElement_sha() {
    return element_sha;
  }

  public String getElement_type() {
    return element_type;
  }

  public String getElement_name() {
    return element_name;
  }

  public FolderElement toProto() {
    return FolderElement.newBuilder()
        .setCreatedByCommit(this.element_sha)
        .setElementName(this.element_name)
        .build();
  }
}
