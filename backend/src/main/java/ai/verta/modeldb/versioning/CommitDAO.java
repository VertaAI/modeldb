package ai.verta.modeldb.versioning;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.dto.CommitPaginationDTO;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.versioning.CreateCommitRequest.Response;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.hibernate.Session;

public interface CommitDAO {
  Response setCommit(
      String author, Commit commit, BlobFunction setBlobs, RepositoryFunction getRepository)
      throws ModelDBException, NoSuchAlgorithmException;

  CommitEntity saveCommitEntity(
      Session session,
      Commit commit,
      String rootSha,
      String author,
      RepositoryEntity repositoryEntity)
      throws ModelDBException, NoSuchAlgorithmException;

  CommitPaginationDTO fetchCommitEntityList(
      Session session, ListCommitsRequest request, Long repoId) throws ModelDBException;

  ListCommitsRequest.Response listCommits(
      ListCommitsRequest request, RepositoryFunction getRepository) throws ModelDBException;

  Commit getCommit(String commitHash, RepositoryFunction getRepository) throws ModelDBException;

  CommitEntity getCommitEntity(
      Session session, String commitHash, RepositoryFunction getRepositoryFunction)
      throws ModelDBException;

  boolean deleteCommits(RepositoryIdentification repositoryIdentification,
      List<String> commitShas, RepositoryDAO repositoryDAO) throws ModelDBException;

  DeleteCommitRequest.Response deleteCommit(
      DeleteCommitRequest request, RepositoryDAO repositoryDAO) throws ModelDBException;
}
