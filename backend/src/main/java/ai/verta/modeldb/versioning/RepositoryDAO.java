package ai.verta.modeldb.versioning;

import ai.verta.modeldb.AddDatasetTags;
import ai.verta.modeldb.Dataset;
import ai.verta.modeldb.FindDatasets;
import ai.verta.modeldb.GetDatasetById;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.dto.DatasetPaginationDTO;
import ai.verta.modeldb.entities.versioning.BranchEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEnums;
import ai.verta.modeldb.experimentRun.ExperimentRunDAO;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.UserInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import org.hibernate.Session;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public interface RepositoryDAO {

  GetRepositoryRequest.Response getRepository(GetRepositoryRequest request) throws Exception;

  RepositoryEntity getRepositoryById(
      Session session, RepositoryIdentification id, boolean checkWrite)
      throws ModelDBException, ExecutionException, InterruptedException;

  RepositoryEntity getRepositoryById(
      Session session,
      RepositoryIdentification id,
      boolean checkWrite,
      boolean canNotOperateOnProtected,
      RepositoryEnums.RepositoryTypeEnum repositoryType)
      throws ModelDBException, ExecutionException, InterruptedException;

  RepositoryEntity getRepositoryById(Session session, RepositoryIdentification id)
      throws ModelDBException, ExecutionException, InterruptedException;

  RepositoryEntity getProtectedRepositoryById(RepositoryIdentification id, boolean checkWrite)
      throws ModelDBException, ExecutionException, InterruptedException;

  SetRepository.Response setRepository(SetRepository request, UserInfo userInfo, boolean create)
      throws ModelDBException, InvalidProtocolBufferException, NoSuchAlgorithmException,
          ExecutionException, InterruptedException;

  DeleteRepositoryRequest.Response deleteRepository(
      DeleteRepositoryRequest request,
      CommitDAO commitDAO,
      ExperimentRunDAO experimentRunDAO,
      boolean canNotOperateOnProtected,
      RepositoryEnums.RepositoryTypeEnum repositoryType)
      throws ModelDBException, ExecutionException, InterruptedException;

  Boolean deleteRepositories(List<String> repositoryIds, ExperimentRunDAO experimentRunDAO)
      throws ModelDBException;

  void deleteRepositories(
      Session session, ExperimentRunDAO experimentRunDAO, List<String> allowedRepositoryIds);

  Dataset createOrUpdateDataset(
      Dataset dataset, String workspaceName, boolean create, UserInfo userInfo)
      throws ModelDBException, NoSuchAlgorithmException, InvalidProtocolBufferException,
          ExecutionException, InterruptedException;

  ListRepositoriesRequest.Response listRepositories(
      ListRepositoriesRequest request, UserInfo userInfo)
      throws ModelDBException, InvalidProtocolBufferException, ExecutionException,
          InterruptedException;

  ListTagsRequest.Response listTags(ListTagsRequest request)
      throws ModelDBException, ExecutionException, InterruptedException;

  SetTagRequest.Response setTag(SetTagRequest request)
      throws ModelDBException, ExecutionException, InterruptedException;

  GetTagRequest.Response getTag(GetTagRequest request)
      throws ModelDBException, ExecutionException, InterruptedException;

  DeleteTagRequest.Response deleteTag(DeleteTagRequest request)
      throws ModelDBException, ExecutionException, InterruptedException;

  SetBranchRequest.Response setBranch(
      SetBranchRequest request,
      boolean canNotOperateOnProtected,
      RepositoryEnums.RepositoryTypeEnum repositoryType)
      throws ModelDBException, ExecutionException, InterruptedException;

  BranchEntity getBranchEntity(Session session, Long repoId, String branchName)
      throws ModelDBException;

  GetBranchRequest.Response getBranch(
      GetBranchRequest request,
      boolean canNotOperateOnProtected,
      RepositoryEnums.RepositoryTypeEnum repositoryType)
      throws ModelDBException, ExecutionException, InterruptedException;

  DeleteBranchRequest.Response deleteBranch(DeleteBranchRequest request)
      throws ModelDBException, ExecutionException, InterruptedException;

  void deleteBranchByCommit(Session session, Long repoId, String commitHash);

  ListBranchesRequest.Response listBranches(ListBranchesRequest request)
      throws ModelDBException, ExecutionException, InterruptedException;

  ListCommitsLogRequest.Response listCommitsLog(ListCommitsLogRequest request)
      throws ModelDBException, ExecutionException, InterruptedException;

  FindRepositories.Response findRepositories(FindRepositories request)
      throws ModelDBException, InvalidProtocolBufferException, ExecutionException,
          InterruptedException;

  AddDatasetTags.Response addDatasetTags(MetadataDAO metadataDAO, String id, List<String> tags)
      throws ModelDBException, InvalidProtocolBufferException, ExecutionException,
          InterruptedException;

  void addRepositoryTags(
      MetadataDAO metadataDAO,
      RepositoryIdentification repositoryIdentification,
      List<String> tags,
      boolean canNotOperateOnProtected,
      RepositoryEnums.RepositoryTypeEnum repositoryType)
      throws ModelDBException, ExecutionException, InterruptedException;

  Dataset deleteDatasetTags(
      MetadataDAO metadataDAO, String id, List<String> tagsList, boolean deleteAll)
      throws ModelDBException, InvalidProtocolBufferException, ExecutionException,
          InterruptedException;

  void deleteRepositoryTags(
      MetadataDAO metadataDAO,
      RepositoryIdentification repositoryIdentification,
      List<String> tagsList,
      boolean deleteAll,
      boolean canNotOperateOnProtected,
      RepositoryEnums.RepositoryTypeEnum repositoryType)
      throws ModelDBException, ExecutionException, InterruptedException;

  DatasetPaginationDTO findDatasets(
      MetadataDAO metadataDAO, FindDatasets build, UserInfo userInfo, ResourceVisibility aPrivate)
      throws InvalidProtocolBufferException, ExecutionException, InterruptedException;

  GetDatasetById.Response getDatasetById(MetadataDAO metadataDAO, String id)
      throws ModelDBException, InvalidProtocolBufferException, ExecutionException,
          InterruptedException;

  void deleteRepositoryAttributes(
      Long repositoryId,
      List<String> attributesKeys,
      boolean deleteAll,
      boolean canNotOperateOnProtected,
      RepositoryEnums.RepositoryTypeEnum repositoryType)
      throws ModelDBException, ExecutionException, InterruptedException;
}
