package ai.verta.modeldb.lineage;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.VersioningLineageEntry;
import org.hibernate.Session;

public interface CommitInBlobHashFunction {
  VersioningLineageEntry apply(Session session, VersioningBlobElement versioningLineageEntry)
      throws ModelDBException;
}
