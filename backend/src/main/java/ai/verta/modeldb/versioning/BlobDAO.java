package ai.verta.modeldb.versioning;

import ai.verta.common.ArtifactPart;
import ai.verta.common.KeyValue;
import ai.verta.modeldb.*;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.experimentRun.CommitMultipartFunction;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model.AutogenBlobDiff;
import ai.verta.modeldb.versioning.blob.container.BlobContainer;
import com.google.protobuf.ProtocolStringList;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.hibernate.Session;

public interface BlobDAO {

  String setBlobs(Session session, List<BlobContainer> blobsList, FileHasher fileHasher)
      throws NoSuchAlgorithmException, ModelDBException;

  void setBlobsAttributes(
      Session session,
      Long repoId,
      String commitHash,
      List<BlobContainer> blobsList,
      boolean addAttribute)
      throws ModelDBException;

  DatasetVersion addUpdateDatasetVersionAttributes(
      RepositoryDAO repositoryDAO,
      CommitDAO commitDAO,
      MetadataDAO metadataDAO,
      Long repoId,
      String commitHash,
      List<KeyValue> attributes,
      boolean addAttribute)
      throws ModelDBException, ExecutionException, InterruptedException;

  void addUpdateBlobAttributes(
      CommitDAO commitDAO,
      RepositoryEntity repositoryEntity,
      String commitHash,
      List<KeyValue> attributes,
      boolean addAttribute)
      throws ModelDBException, ExecutionException, InterruptedException;

  DatasetVersion deleteDatasetVersionAttributes(
      RepositoryDAO repositoryDAO,
      CommitDAO commitDAO,
      MetadataDAO metadataDAO,
      Long repoId,
      String commiHash,
      List<String> attributesKeys,
      List<String> location,
      boolean deleteAll)
      throws ModelDBException, ExecutionException, InterruptedException;

  void deleteBlobAttributes(
      CommitDAO commitDAO,
      RepositoryEntity repositoryEntity,
      String commiHash,
      List<String> attributesKeys,
      List<String> location,
      boolean deleteAll)
      throws ModelDBException, ExecutionException, InterruptedException;

  List<KeyValue> getDatasetVersionAttributes(
      RepositoryDAO repositoryDAO,
      CommitDAO commitDAO,
      Long repoId,
      String commitHash,
      List<String> location,
      List<String> attributeKeysList)
      throws ModelDBException, ExecutionException, InterruptedException;

  List<KeyValue> getBlobAttributes(
      Long repositoryId, String commitHash, List<String> location, List<String> attributeKeysList)
      throws ModelDBException;

  GetCommitComponentRequest.Response getCommitComponent(
      RepositoryFunction repositoryFunction, String commitHash, ProtocolStringList locationList)
      throws NoSuchAlgorithmException, ModelDBException, ExecutionException, InterruptedException;

  ListCommitBlobsRequest.Response getCommitBlobsList(
      RepositoryFunction repositoryFunction, String commitHash, List<String> locationList)
      throws NoSuchAlgorithmException, ModelDBException, ExecutionException, InterruptedException;

  DatasetVersion convertToDatasetVersion(
      RepositoryDAO repositoryDAO,
      MetadataDAO metadataDAO,
      RepositoryEntity repositoryEntity,
      String commitHash,
      boolean checkWrite)
      throws ModelDBException, ExecutionException, InterruptedException;

  Map<String, BlobExpanded> getCommitBlobMap(
      Session session, String folderHash, List<String> locationList) throws ModelDBException;

  Map<String, Map.Entry<BlobExpanded, String>> getCommitBlobMapWithHash(
      Session session, String folderHash, List<String> locationList, List<BlobType> blobTypeList)
      throws ModelDBException;

  ComputeRepositoryDiffRequest.Response computeRepositoryDiff(
      RepositoryDAO repositoryDAO, ComputeRepositoryDiffRequest request)
      throws ModelDBException, ExecutionException, InterruptedException;

  List<BlobContainer> convertBlobDiffsToBlobs(
      List<AutogenBlobDiff> blobDiffs,
      RepositoryFunction repositoryFunction,
      CommitFunction commitFunction)
      throws ModelDBException, ExecutionException, InterruptedException;

  MergeRepositoryCommitsRequest.Response mergeCommit(
      RepositoryDAO repositoryDAO, MergeRepositoryCommitsRequest request)
      throws ModelDBException, NoSuchAlgorithmException, ExecutionException, InterruptedException;

  RevertRepositoryCommitsRequest.Response revertCommit(
      RepositoryDAO repositoryDAO, RevertRepositoryCommitsRequest request)
      throws ModelDBException, NoSuchAlgorithmException, ExecutionException, InterruptedException;

  GetUrlForDatasetBlobVersioned.Response getUrlForVersionedDatasetBlob(
      ArtifactStoreDAO artifactStoreDAO,
      RepositoryDAO repositoryDAO,
      String datasetId,
      CommitFunction commitFunction,
      GetUrlForDatasetBlobVersioned request)
      throws ModelDBException, ExecutionException, InterruptedException;

  GetUrlForBlobVersioned.Response getUrlForVersionedBlob(
      ArtifactStoreDAO artifactStoreDAO,
      RepositoryFunction repositoryFunction,
      CommitFunction commitFunction,
      GetUrlForBlobVersioned request)
      throws ModelDBException, ExecutionException, InterruptedException;

  void commitVersionedDatasetBlobArtifactPart(
      RepositoryDAO repositoryDAO,
      String datasetId,
      CommitFunction commitFunction,
      CommitVersionedDatasetBlobArtifactPart request)
      throws ModelDBException;

  CommitVersionedBlobArtifactPart.Response commitVersionedBlobArtifactPart(
      RepositoryFunction repositoryFunction,
      CommitFunction commitFunction,
      List<String> location,
      String pathDatasetComponentBlobPath,
      ArtifactPart artifactPart)
      throws ModelDBException;

  GetCommittedVersionedDatasetBlobArtifactParts.Response
      getCommittedVersionedDatasetBlobArtifactParts(
          RepositoryDAO repositoryDAO,
          String datasetId,
          CommitFunction commitFunction,
          GetCommittedVersionedDatasetBlobArtifactParts request)
          throws ModelDBException;

  GetCommittedVersionedBlobArtifactParts.Response getCommittedVersionedBlobArtifactParts(
      RepositoryFunction repositoryFunction,
      CommitFunction commitFunction,
      List<String> location,
      String pathDatasetComponentBlobPath)
      throws ModelDBException;

  void commitMultipartVersionedDatasetBlobArtifact(
      RepositoryDAO repositoryDAO,
      String datasetId,
      CommitFunction commitFunction,
      CommitMultipartVersionedDatasetBlobArtifact request,
      CommitMultipartFunction commitMultipartFunction)
      throws ModelDBException;

  CommitMultipartVersionedBlobArtifact.Response commitMultipartVersionedBlobArtifact(
      RepositoryFunction repositoryFunction,
      CommitFunction commitFunction,
      List<String> location,
      String pathDatasetComponentBlobPath,
      CommitMultipartFunction commitMultipartFunction)
      throws ModelDBException;

  FindRepositoriesBlobs.Response findRepositoriesBlobs(
      CommitDAO commitDAO, FindRepositoriesBlobs request) throws ModelDBException;
}
