package ai.verta.modeldb.versioning.blob.container;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.versioning.BlobInfoEntity;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.FileHasher;
import ai.verta.modeldb.versioning.TreeElem;
import io.grpc.Status.Code;
import io.grpc.protobuf.StatusProto;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

/** contains proto object and saves it to the database */
public abstract class BlobContainer {

  private static final Logger LOGGER = LogManager.getLogger(BlobContainer.class);

  private final BlobExpanded blobExpanded;

  public BlobContainer(BlobExpanded blobExpanded) {
    this.blobExpanded = blobExpanded;
  }

  public static BlobContainer create(BlobExpanded blobExpanded) throws ModelDBException {
    switch (blobExpanded.getBlob().getContentCase()) {
      case DATASET:
        return new DatasetContainer(blobExpanded);
      case ENVIRONMENT:
        return new EnvironmentContainer(blobExpanded);
      case CODE:
        return new CodeContainer(blobExpanded);
      case CONFIG:
        return new ConfigContainer(blobExpanded);
      case CONTENT_NOT_SET:
      default:
        throw new ModelDBException("Unknown blob type", Code.INVALID_ARGUMENT);
    }
  }

  /**
   * get location list used in in tree representation
   *
   * @return location list with a first element root folder
   */
  public List<String> getLocationList() {
    List<String> result = new LinkedList<>();
    // empty dir represents root folder
    result.add("");
    result.addAll(blobExpanded.getLocationList());
    return result;
  }

  /**
   * @param rootTree : Each blob or folder need to be converted to a tree element. the process is
   *     bootstrapped with an empty tree for each BlobExpanded
   * @param fileHasher get sha of the blob or string
   * @throws NoSuchAlgorithmException for no hashing algorithm
   */
  public abstract void process(
      Session session, TreeElem rootTree, FileHasher fileHasher, Set<String> blobHashes)
      throws NoSuchAlgorithmException, ModelDBException;

  protected String process(Session session, String contentHash, Set<String> blobHashes) {
    StringBuilder sb = new StringBuilder();
    sb.append("common_info");
    String description = blobExpanded.getBlob().getDescription();
    sb.append(":description:").append(description).append(":content_hash:").append(contentHash);
    try {
      String sha = FileHasher.getSha(sb.toString());
      if (!blobHashes.contains(sha)) {
        session.saveOrUpdate(new BlobInfoEntity(sha, description));
      }
      return sha;
    } catch (NoSuchAlgorithmException e) {
      String message = "unexpected algorithm not found exception";
      LOGGER.error(message + " {}", e.getMessage());
      com.google.rpc.Status status =
          com.google.rpc.Status.newBuilder()
              .setCode(com.google.rpc.Code.INTERNAL_VALUE)
              .setMessage(message)
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }
}
