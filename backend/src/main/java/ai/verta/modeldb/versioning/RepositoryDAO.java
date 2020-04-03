package ai.verta.modeldb.versioning;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.uac.UserInfo;
import org.hibernate.Session;

public interface RepositoryDAO {

  GetRepositoryRequest.Response getRepository(GetRepositoryRequest request) throws Exception;

  RepositoryEntity getRepositoryById(Session session, RepositoryIdentification id)
      throws ModelDBException;

  SetRepository.Response setRepository(SetRepository request, UserInfo userInfo, boolean create)
      throws ModelDBException;

  DeleteRepositoryRequest.Response deleteRepository(
      DeleteRepositoryRequest request, CommitDAO commitDAO, ExperimentRunDAO experimentRunDAO)
      throws ModelDBException;

  ListRepositoriesRequest.Response listRepositories(ListRepositoriesRequest request)
      throws ModelDBException;

  ListTagsRequest.Response listTags(ListTagsRequest request) throws ModelDBException;

  SetTagRequest.Response setTag(SetTagRequest request) throws ModelDBException;

  GetTagRequest.Response getTag(GetTagRequest request) throws ModelDBException;

  DeleteTagRequest.Response deleteTag(DeleteTagRequest request) throws ModelDBException;

  SetBranchRequest.Response setBranch(SetBranchRequest request) throws ModelDBException;

  GetBranchRequest.Response getBranch(GetBranchRequest request) throws ModelDBException;

  DeleteBranchRequest.Response deleteBranch(DeleteBranchRequest request) throws ModelDBException;

  void deleteBranchByCommit(Long repoId, String commitHash);

  ListBranchesRequest.Response listBranches(ListBranchesRequest request) throws ModelDBException;

  ListCommitsLogRequest.Response listCommitsLog(ListCommitsLogRequest request)
      throws ModelDBException;
}
