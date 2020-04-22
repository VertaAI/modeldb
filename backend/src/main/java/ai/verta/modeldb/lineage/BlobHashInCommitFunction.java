package ai.verta.modeldb.lineage;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.VersioningLineageEntry;
import ai.verta.modeldb.entities.versioning.InternalFolderElementEntity;
import org.hibernate.Session;

public interface BlobHashInCommitFunction {
  InternalFolderElementEntity apply(Session session, VersioningLineageEntry versioningLineageEntry)
      throws ModelDBException;
}
