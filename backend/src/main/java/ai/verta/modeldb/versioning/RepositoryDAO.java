package ai.verta.modeldb.versioning;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import org.hibernate.Session;

public interface RepositoryDAO {

  GetRepositoryRequest.Response getRepository(GetRepositoryRequest request) throws Exception;

  RepositoryEntity getRepositoryById(Session session, RepositoryIdentification id)
      throws ModelDBException;

  SetRepository.Response setRepository(SetRepository request, boolean create)
      throws ModelDBException;

  DeleteRepositoryRequest.Response deleteRepository(DeleteRepositoryRequest request)
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

  ListBranchesRequest.Response listBranches(ListBranchesRequest request) throws ModelDBException;

  ListBranchCommitsRequest.Response listBranchCommits(ListBranchCommitsRequest request)
      throws ModelDBException;
}
