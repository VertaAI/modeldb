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

public class VersioningBlobElement extends LineageElement {
  private static final Logger LOGGER = LogManager.getLogger(VersioningBlobElement.class);

  private final Long repositoryId;
  private final String blobSha;
  private final String blobType;

  public VersioningBlobElement(Long repositoryId, String blobSha, String blobType) {
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
    VersioningBlobElement that = (VersioningBlobElement) o;
    return Objects.equals(blobType, that.blobType)
        && Objects.equals(blobSha, that.blobSha)
        && Objects.equals(repositoryId, that.repositoryId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(blobType, blobSha, repositoryId);
  }

  @Override
  public LineageEntry toProto(Session session, CommitInBlobHashFunction commitInBlobHashFunction) {
    VersioningLineageEntry result;
    try {
      result = commitInBlobHashFunction.apply(session, this);
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
