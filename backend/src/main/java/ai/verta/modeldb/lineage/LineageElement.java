package ai.verta.modeldb.lineage;

import ai.verta.modeldb.LineageEntry;
import ai.verta.modeldb.Location;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.VersioningLineageEntry;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.protobuf.InvalidProtocolBufferException;

public abstract class LineageElement {
  static LineageElement fromProto(LineageEntry lineageEntry)
      throws InvalidProtocolBufferException, ModelDBException {
    switch (lineageEntry.getDescriptionCase()) {
      case EXPERIMENT_RUN:
        return new ExperimentRunElement(lineageEntry.getExperimentRun());
      case BLOB:
        VersioningLineageEntry blob = lineageEntry.getBlob();
        return new VersioningBlobElement(
            blob.getRepositoryId(),
            blob.getCommitSha(),
            ModelDBUtils.getStringFromProtoObject(
                Location.newBuilder().addAllLocation(blob.getLocationList())));
      default:
        throw new ModelDBException("Unknown lineage type");
    }
  }

  public abstract LineageEntry toProto();
}
