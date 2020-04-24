package ai.verta.modeldb.lineage;

import ai.verta.modeldb.LineageEntry;
import ai.verta.modeldb.Location;
import ai.verta.modeldb.VersioningLineageEntry;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VersioningBlobEntryContainer extends LineageEntryContainer {
  private static final Logger LOGGER = LogManager.getLogger(VersioningBlobEntryContainer.class);

  private final String location;
  private final String commitSha;
  private final Long repositoryId;

  public VersioningBlobEntryContainer(Long repositoryId, String commitSha, String location) {
    this.repositoryId = repositoryId;
    this.commitSha = commitSha;
    this.location = location;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VersioningBlobEntryContainer that = (VersioningBlobEntryContainer) o;
    return Objects.equals(location, that.location)
        && Objects.equals(commitSha, that.commitSha)
        && Objects.equals(repositoryId, that.repositoryId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(location, commitSha, repositoryId);
  }

  @Override
  public LineageEntry toProto() {
    Location.Builder builder = Location.newBuilder();
    try {
      ModelDBUtils.getProtoObjectFromString(location, builder);
    } catch (InvalidProtocolBufferException e) {
      String errorMessage = "Unexpected location convertion from the database error";
      LOGGER.error(errorMessage);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL_VALUE)
              .setMessage(errorMessage)
              .addDetails(Any.pack(LineageEntry.getDefaultInstance()))
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
    return LineageEntry.newBuilder()
        .setBlob(
            VersioningLineageEntry.newBuilder()
                .setRepositoryId(repositoryId)
                .setCommitSha(commitSha)
                .addAllLocation(builder.getLocationList()))
        .build();
  }
}
