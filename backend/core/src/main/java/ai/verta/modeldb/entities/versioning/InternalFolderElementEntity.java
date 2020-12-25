package ai.verta.modeldb.entities.versioning;

import ai.verta.modeldb.versioning.InternalFolderElement;
import java.io.Serializable;
import java.util.Objects;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    InternalFolderElementEntity that = (InternalFolderElementEntity) o;
    return folder_hash.equals(that.folder_hash)
        && element_sha.equals(that.element_sha)
        && element_type.equals(that.element_type)
        && element_name.equals(that.element_name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(folder_hash, element_sha, element_type, element_name);
  }
}
