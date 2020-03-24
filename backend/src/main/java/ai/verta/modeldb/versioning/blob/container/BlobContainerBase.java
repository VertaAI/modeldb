package ai.verta.modeldb.versioning.blob.container;

import ai.verta.modeldb.ModelDBException;

public interface BlobContainerBase {
  void validate() throws ModelDBException;
}
