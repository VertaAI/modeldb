package ai.verta.modeldb.versioning;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.versioning.blob.container.BlobContainer;
import com.google.protobuf.ProtocolStringList;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;

public interface BlobDAO {

  String setBlobs(List<BlobContainer> blobsList, FileHasher fileHasher)
      throws NoSuchAlgorithmException, ModelDBException;

  GetCommitComponentRequest.Response getCommitComponent(
      RepositoryFunction repositoryFunction, String commitHash, ProtocolStringList locationList)
      throws NoSuchAlgorithmException, ModelDBException;

  ListCommitBlobsRequest.Response getCommitBlobsList(
      RepositoryFunction repositoryFunction, String commitHash, ProtocolStringList locationList)
      throws NoSuchAlgorithmException, ModelDBException;

  Map<String, BlobExpanded> getCommitBlobMap(
      Session session, String folderHash, List<String> locationList) throws ModelDBException;

  ComputeRepositoryDiffRequest.Response computeRepositoryDiff(
      RepositoryFunction repositoryFunction, ComputeRepositoryDiffRequest request)
      throws ModelDBException;

  List<BlobContainer> convertBlobDiffsToBlobs(
      CreateCommitRequest request,
      RepositoryFunction repositoryFunction,
      CommitFunction commitFunction)
      throws ModelDBException;
}
