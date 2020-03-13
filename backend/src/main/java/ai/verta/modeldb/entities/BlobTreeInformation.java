package ai.verta.modeldb.entities;

/** This class is to store information about blob in a tree */
public interface BlobTreeInformation {

  // this setter will be gone
  void setBaseBlobHash(String folderHash);

  // TODO: refactor dataset to not add it's components to a tree
  // this getter will be gone as there will be no components in a tree
  default boolean hasComponents() {
    return true;
  }

  default String getElementSha() {
    return null;
  }

  default String getElementName() {
    return null;
  }
}
