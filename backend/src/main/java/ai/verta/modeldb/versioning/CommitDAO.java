package ai.verta.modeldb.versioning;

import ai.verta.modeldb.DatasetVersion;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.dto.CommitPaginationDTO;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.uac.UserInfo;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.hibernate.Session;

public interface CommitDAO {
  CreateCommitRequest.Response setCommit(
      String author,
      Commit commit,
      BlobFunction setBlobs,
      BlobFunction.BlobFunctionAttribute setBlobsAttributes,
      RepositoryFunction getRepository)
      throws ModelDBException, NoSuchAlgorithmException;

  CreateCommitRequest.Response setCommitFromDatasetVersion(
      DatasetVersion datasetVersion,
      RepositoryDAO repositoryDAO,
      BlobDAO blobDAO,
      MetadataDAO metadataDAO,
      RepositoryEntity repositoryEntity)
      throws ModelDBException, NoSuchAlgorithmException, ExecutionException, InterruptedException;

  CommitEntity saveCommitEntity(
      Session session,
      Commit commit,
      String rootSha,
      String author,
      RepositoryEntity repositoryEntity)
      throws ModelDBException, NoSuchAlgorithmException;

  ListCommitsRequest.Response listCommits(
      ListCommitsRequest request, RepositoryFunction getRepository, boolean ascending)
      throws ModelDBException, ExecutionException, InterruptedException;

  Commit getCommit(String commitHash, RepositoryFunction getRepository)
      throws ModelDBException, ExecutionException, InterruptedException;

  CommitEntity getCommitEntity(
      Session session, String commitHash, RepositoryFunction getRepositoryFunction)
      throws ModelDBException, ExecutionException, InterruptedException;

  String getDatasetIdByDatasetVersion(RepositoryDAO repositoryDAO, String commitHash)
      throws ModelDBException;

  void deleteDatasetVersions(
      RepositoryIdentification repositoryIdentification,
      List<String> datasetVersionIds,
      RepositoryDAO repositoryDAO)
      throws ModelDBException, ExecutionException, InterruptedException;

  boolean deleteCommits(
      RepositoryIdentification repositoryIdentification,
      List<String> commitShas,
      RepositoryDAO repositoryDAO)
      throws ModelDBException, ExecutionException, InterruptedException;

  DatasetVersion addDeleteDatasetVersionTags(
      RepositoryDAO repositoryDAO,
      BlobDAO blobDAO,
      MetadataDAO metadataDAO,
      boolean addTags,
      String datasetId,
      String datasetVersionId,
      List<String> tagsList,
      boolean deleteAll)
      throws ModelDBException, ExecutionException, InterruptedException;

  void addDeleteCommitLabels(
      RepositoryEntity repositoryEntity,
      String commitHash,
      MetadataDAO metadataDAO,
      boolean addLabels,
      List<String> labelsList,
      boolean deleteAll)
      throws ModelDBException, ExecutionException, InterruptedException;

  CommitPaginationDTO findCommits(
      FindRepositoriesBlobs request,
      UserInfo currentLoginUserInfo,
      boolean idsOnly,
      boolean rootSHAOnly,
      boolean isDatasetVersion,
      String sortKey,
      boolean ascending)
      throws ModelDBException;

  CommitPaginationDTO findCommits(
      Session session,
      FindRepositoriesBlobs request,
      UserInfo currentLoginUserInfo,
      boolean idsOnly,
      boolean rootSHAOnly,
      boolean isDatasetVersion,
      String sortKey,
      boolean ascending)
      throws ModelDBException;

  boolean isCommitExists(Session session, String commitHash);

  DatasetVersion updateDatasetVersionDescription(
      RepositoryDAO repositoryDAO,
      BlobDAO blobDAO,
      MetadataDAO metadataDAO,
      String datasetId,
      String datasetVersionId,
      String description)
      throws ModelDBException, ExecutionException, InterruptedException;

  DatasetVersion getDatasetVersionById(
      RepositoryDAO repositoryDAO,
      BlobDAO blobDAO,
      MetadataDAO metadataDAO,
      String datasetVersionId)
      throws ModelDBException, ExecutionException, InterruptedException;
}
