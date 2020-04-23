package ai.verta.modeldb.lineage;

import ai.verta.modeldb.LineageEntry;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.VersioningLineageEntry;
import com.google.protobuf.Any;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

public class VersioningBlobEntryContainer extends LineageEntryContainer {
  private static final Logger LOGGER = LogManager.getLogger(VersioningBlobEntryContainer.class);

  private final Long repositoryId;
  private final String blobSha;
  private final String blobType;

  public VersioningBlobEntryContainer(Long repositoryId, String blobSha, String blobType) {
    this.repositoryId = repositoryId;
    this.blobSha = blobSha;
    this.blobType = blobType;
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
    return Objects.equals(blobType, that.blobType)
        && Objects.equals(blobSha, that.blobSha)
        && Objects.equals(repositoryId, that.repositoryId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(blobType, blobSha, repositoryId);
  }

  @Override
  public LineageEntry toProto(
      Session session, BlobHashToCommitHashFunction blobHashToCommitHashFunction) {
    VersioningLineageEntry result;
    try {
      result = blobHashToCommitHashFunction.apply(session, this);
    } catch (ModelDBException e) {
      String errorMessage = "Unexpected blob to commit conversion from the database error";
      LOGGER.error(errorMessage);
      Status status =
          Status.newBuilder()
              .setCode(Code.INTERNAL_VALUE)
              .setMessage(errorMessage)
              .addDetails(Any.pack(LineageEntry.getDefaultInstance()))
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
    return LineageEntry.newBuilder().setBlob(result).build();
  }

  public Long getRepositoryId() {
    return repositoryId;
  }

  public String getBlobSha() {
    return blobSha;
  }

  public String getBlobType() {
    return blobType;
  }
}
