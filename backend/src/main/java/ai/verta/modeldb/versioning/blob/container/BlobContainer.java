package ai.verta.modeldb.versioning.blob.container;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.FileHasher;
import ai.verta.modeldb.versioning.TreeElem;
import io.grpc.Status.Code;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.hibernate.Session;

/** contains proto object and saves it to the database */
public abstract class BlobContainer {

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

  public abstract void processAttribute(
      Session session, Long repoId, String commitHash, boolean addAttribute)
      throws ModelDBException;

  public BlobExpanded getBlobExpanded() {
    return blobExpanded;
  }
}
