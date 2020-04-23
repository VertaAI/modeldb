package ai.verta.modeldb.lineage;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.VersioningLineageEntry;
import org.hibernate.Session;

/** converts blob hash to commit hash */
public interface BlobHashToCommitHashFunction {
  VersioningLineageEntry apply(Session session, VersioningBlobEntryContainer versioningLineageEntry)
      throws ModelDBException;
}
