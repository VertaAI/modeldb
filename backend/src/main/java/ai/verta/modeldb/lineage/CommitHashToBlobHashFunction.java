package ai.verta.modeldb.lineage;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.VersioningLineageEntry;
import ai.verta.modeldb.entities.versioning.InternalFolderElementEntity;
import org.hibernate.Session;

/** converts commit hash to blob hash */
public interface CommitHashToBlobHashFunction {
  InternalFolderElementEntity apply(Session session, VersioningLineageEntry versioningLineageEntry)
      throws ModelDBException;
}
