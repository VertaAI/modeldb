package ai.verta.modeldb.versioning;

import ai.verta.modeldb.AddDatasetTags.Response;
import ai.verta.modeldb.Dataset;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.versioning.BranchEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.uac.UserInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ProtocolStringList;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.hibernate.Session;

public interface RepositoryDAO {

  GetRepositoryRequest.Response getRepository(GetRepositoryRequest request) throws Exception;

  RepositoryEntity getRepositoryById(
      Session session, RepositoryIdentification id, boolean checkWrite) throws ModelDBException;

  RepositoryEntity getRepositoryById(
      Session session, RepositoryIdentification id, boolean checkWrite, boolean checkProtected)
      throws ModelDBException;

  RepositoryEntity getRepositoryById(Session session, RepositoryIdentification id)
      throws ModelDBException;

  SetRepository.Response setRepository(
      CommitDAO commitDAO, SetRepository request, UserInfo userInfo, boolean create)
      throws ModelDBException, InvalidProtocolBufferException, NoSuchAlgorithmException;

  DeleteRepositoryRequest.Response deleteRepository(
      DeleteRepositoryRequest request, CommitDAO commitDAO, ExperimentRunDAO experimentRunDAO)
      throws ModelDBException;

  Boolean deleteRepositories(List<String> repositoryIds, ExperimentRunDAO experimentRunDAO)
      throws ModelDBException;

  Repository createRepository(
      CommitDAO commitDAO, MetadataDAO metadataDAO, Dataset dataset, UserInfo userInfo)
      throws ModelDBException, NoSuchAlgorithmException, InvalidProtocolBufferException;

  ListRepositoriesRequest.Response listRepositories(
      ListRepositoriesRequest request, UserInfo userInfo)
      throws ModelDBException, InvalidProtocolBufferException;

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

  FindRepositories.Response findRepositories(FindRepositories request)
      throws ModelDBException, InvalidProtocolBufferException;

  Response addDatasetTags(MetadataDAO metadataDAO, String id, List<String> tags)
      throws ModelDBException;

  List<String> getDatasetTags(MetadataDAO metadataDAO, String id) throws ModelDBException;

  Dataset deleteDatasetTags(
      MetadataDAO metadataDAO, String id, ProtocolStringList tagsList, boolean deleteAll)
      throws ModelDBException;
}
