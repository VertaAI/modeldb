package ai.verta.modeldb.lineage;

import ai.verta.modeldb.LineageEntry;
import ai.verta.modeldb.Location;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.VersioningLineageEntry;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.protobuf.InvalidProtocolBufferException;

public abstract class LineageEntryContainer {
  static LineageEntryContainer fromProto(LineageEntry lineageEntry)
      throws InvalidProtocolBufferException, ModelDBException {
    switch (lineageEntry.getType()) {
      case EXPERIMENT_RUN:
        return new ExperimentRunEntryContainer(lineageEntry.getExternalId());
      case BLOB:
        VersioningLineageEntry blob = lineageEntry.getBlob();
        return new VersioningBlobEntryContainer(
            blob.getRepositoryId(),
            blob.getCommitSha(),
            ModelDBUtils.getStringFromProtoObject(
                Location.newBuilder().addAllLocation(blob.getLocationList())));
        // TODO: dataset version
      default:
        throw new ModelDBException("Unknown lineage type");
    }
  }

  public abstract LineageEntry toProto();
}
