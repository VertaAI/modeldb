package ai.verta.modeldb.versioning;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.versioning.BranchEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.uac.UserInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import java.security.NoSuchAlgorithmException;
import org.hibernate.Session;

public interface RepositoryDAO {

  GetRepositoryRequest.Response getRepository(GetRepositoryRequest request) throws Exception;

  RepositoryEntity getRepositoryById(
      Session session, RepositoryIdentification id, boolean checkWrite) throws ModelDBException;

  RepositoryEntity getRepositoryById(Session session, RepositoryIdentification id)
      throws ModelDBException;

  SetRepository.Response setRepository(
      CommitDAO commitDAO, SetRepository request, UserInfo userInfo, boolean create)
      throws ModelDBException, InvalidProtocolBufferException, NoSuchAlgorithmException;

  DeleteRepositoryRequest.Response deleteRepository(
      DeleteRepositoryRequest request, CommitDAO commitDAO, ExperimentRunDAO experimentRunDAO)
      throws ModelDBException;

  ListRepositoriesRequest.Response listRepositories(
      ListRepositoriesRequest request, UserInfo userInfo) throws ModelDBException;

  ListTagsRequest.Response listTags(ListTagsRequest request) throws ModelDBException;

  SetTagRequest.Response setTag(SetTagRequest request) throws ModelDBException;

  GetTagRequest.Response getTag(GetTagRequest request) throws ModelDBException;

  DeleteTagRequest.Response deleteTag(DeleteTagRequest request) throws ModelDBException;

  SetBranchRequest.Response setBranch(SetBranchRequest request) throws ModelDBException;

  BranchEntity getBranchEntity(Session session, Long repoId, String branchName)
      throws ModelDBException;

  GetBranchRequest.Response getBranch(GetBranchRequest request) throws ModelDBException;

  DeleteBranchRequest.Response deleteBranch(DeleteBranchRequest request) throws ModelDBException;

  void deleteBranchByCommit(Session session, Long repoId, String commitHash);

  ListBranchesRequest.Response listBranches(ListBranchesRequest request) throws ModelDBException;

  ListCommitsLogRequest.Response listCommitsLog(ListCommitsLogRequest request)
      throws ModelDBException;

  FindRepositories.Response findRepositories(FindRepositories request) throws ModelDBException;
}
