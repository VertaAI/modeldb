package ai.verta.modeldb.versioning;

import ai.verta.modeldb.DatasetVersion;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.dto.CommitPaginationDTO;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.uac.UserInfo;
import java.security.NoSuchAlgorithmException;
import java.util.List;
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
      BlobDAO blobDAO,
      MetadataDAO metadataDAO,
      RepositoryEntity repositoryEntity)
      throws ModelDBException, NoSuchAlgorithmException;

  CommitEntity saveCommitEntity(
      Session session,
      Commit commit,
      String rootSha,
      String author,
      RepositoryEntity repositoryEntity)
      throws ModelDBException, NoSuchAlgorithmException;

  ListCommitsRequest.Response listCommits(
      ListCommitsRequest request, RepositoryFunction getRepository) throws ModelDBException;

  Commit getCommit(String commitHash, RepositoryFunction getRepository) throws ModelDBException;

  CommitEntity getCommitEntity(
      Session session, String commitHash, RepositoryFunction getRepositoryFunction)
      throws ModelDBException;

  boolean deleteCommits(
      RepositoryIdentification repositoryIdentification,
      List<String> commitShas,
      RepositoryDAO repositoryDAO,
      boolean isDatasetVersion)
      throws ModelDBException;

  DeleteCommitRequest.Response deleteCommit(
      DeleteCommitRequest request, RepositoryDAO repositoryDAO) throws ModelDBException;

  void addDeleteDatasetVersionTags(
      MetadataDAO metadataDAO,
      boolean addTags,
      RepositoryEntity repositoryEntity,
      String datasetVersionId,
      List<String> tagsList,
      boolean deleteAll)
      throws ModelDBException;

  CommitPaginationDTO findCommits(
      FindRepositoriesBlobs request,
      UserInfo currentLoginUserInfo,
      boolean idsOnly,
      boolean rootSHAOnly)
      throws ModelDBException;

  CommitPaginationDTO findCommits(
      Session session,
      FindRepositoriesBlobs request,
      UserInfo currentLoginUserInfo,
      boolean idsOnly,
      boolean rootSHAOnly)
      throws ModelDBException;
}
