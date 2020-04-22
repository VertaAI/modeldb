package ai.verta.modeldb.lineage;

import ai.verta.modeldb.LineageEntry;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.VersioningLineageEntry;
import ai.verta.modeldb.entities.versioning.InternalFolderElementEntity;
import org.hibernate.Session;

public abstract class LineageElement {
  static LineageElement fromProto(
      Session session, LineageEntry lineageEntry, BlobHashInCommitFunction blobHashInCommitFunction)
      throws ModelDBException {
    switch (lineageEntry.getDescriptionCase()) {
      case EXPERIMENT_RUN:
        return new ExperimentRunElement(lineageEntry.getExperimentRun());
      case BLOB:
        VersioningLineageEntry blob = lineageEntry.getBlob();
        InternalFolderElementEntity result = blobHashInCommitFunction.apply(session, blob);
        return new VersioningBlobElement(
            blob.getRepositoryId(), result.getElement_sha(), result.getElement_type());
      default:
        throw new ModelDBException("Unknown lineage type");
    }
  }

  public abstract LineageEntry toProto(
      Session session, CommitInBlobHashFunction commitInBlobHashFunction);
}
