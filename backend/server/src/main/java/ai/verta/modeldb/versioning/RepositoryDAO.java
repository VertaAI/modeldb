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
import ai.verta.modeldb.experimentrun.FutureExperimentRunDAO;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.UserInfo;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import org.hibernate.Session;

public interface RepositoryDAO {

  GetRepositoryRequest.Response getRepository(GetRepositoryRequest request) throws Exception;

  RepositoryEntity getRepositoryById(
      Session session, RepositoryIdentification id, boolean checkWrite) throws ModelDBException;

  RepositoryEntity getRepositoryById(
      Session session,
      RepositoryIdentification id,
      boolean checkWrite,
      boolean canNotOperateOnProtected,
      RepositoryEnums.RepositoryTypeEnum repositoryType)
      throws ModelDBException;

  RepositoryEntity getRepositoryById(Session session, RepositoryIdentification id)
      throws ModelDBException;

  RepositoryEntity getProtectedRepositoryById(RepositoryIdentification id, boolean checkWrite)
      throws ModelDBException;

  SetRepository.Response setRepository(SetRepository request, UserInfo userInfo, boolean create)
      throws ModelDBException, NoSuchAlgorithmException;

  DeleteRepositoryRequest.Response deleteRepository(
      DeleteRepositoryRequest request,
      CommitDAO commitDAO,
      FutureExperimentRunDAO futureExperimentRunDAO,
      boolean canNotOperateOnProtected,
      RepositoryEnums.RepositoryTypeEnum repositoryType)
      throws ModelDBException;

  Boolean deleteRepositories(
      List<String> repositoryIds, FutureExperimentRunDAO futureExperimentRunDAO)
      throws ModelDBException;

  void deleteRepositories(
      Session session,
      FutureExperimentRunDAO futureExperimentRunDAO,
      Collection<String> allowedRepositoryIds);

  Dataset createOrUpdateDataset(
      Dataset dataset, String workspaceName, boolean create, UserInfo userInfo)
      throws ModelDBException, NoSuchAlgorithmException;

  ListRepositoriesRequest.Response listRepositories(
      ListRepositoriesRequest request, UserInfo userInfo) throws ModelDBException;

  ListTagsRequest.Response listTags(ListTagsRequest request) throws ModelDBException;

  SetTagRequest.Response setTag(SetTagRequest request) throws ModelDBException;

  GetTagRequest.Response getTag(GetTagRequest request) throws ModelDBException;

  DeleteTagRequest.Response deleteTag(DeleteTagRequest request) throws ModelDBException;

  SetBranchRequest.Response setBranch(
      SetBranchRequest request,
      boolean canNotOperateOnProtected,
      RepositoryEnums.RepositoryTypeEnum repositoryType)
      throws ModelDBException;

  BranchEntity getBranchEntity(Session session, Long repoId, String branchName)
      throws ModelDBException;

  GetBranchRequest.Response getBranch(
      GetBranchRequest request,
      boolean canNotOperateOnProtected,
      RepositoryEnums.RepositoryTypeEnum repositoryType)
      throws ModelDBException;

  DeleteBranchRequest.Response deleteBranch(DeleteBranchRequest request) throws ModelDBException;

  void deleteBranchByCommit(Session session, Long repoId, String commitHash);

  ListBranchesRequest.Response listBranches(ListBranchesRequest request) throws ModelDBException;

  ListCommitsLogRequest.Response listCommitsLog(ListCommitsLogRequest request)
      throws ModelDBException;

  FindRepositories.Response findRepositories(FindRepositories request) throws Exception;

  AddDatasetTags.Response addDatasetTags(MetadataDAO metadataDAO, String id, List<String> tags)
      throws ModelDBException;

  void addRepositoryTags(
      MetadataDAO metadataDAO,
      RepositoryIdentification repositoryIdentification,
      List<String> tags,
      boolean canNotOperateOnProtected,
      RepositoryEnums.RepositoryTypeEnum repositoryType)
      throws ModelDBException;

  Dataset deleteDatasetTags(
      MetadataDAO metadataDAO, String id, List<String> tagsList, boolean deleteAll)
      throws ModelDBException;

  void deleteRepositoryTags(
      MetadataDAO metadataDAO,
      RepositoryIdentification repositoryIdentification,
      List<String> tagsList,
      boolean deleteAll,
      boolean canNotOperateOnProtected,
      RepositoryEnums.RepositoryTypeEnum repositoryType)
      throws ModelDBException;

  DatasetPaginationDTO findDatasets(
      MetadataDAO metadataDAO, FindDatasets build, UserInfo userInfo, ResourceVisibility aPrivate);

  GetDatasetById.Response getDatasetById(MetadataDAO metadataDAO, String id)
      throws ModelDBException;

  void deleteRepositoryAttributes(
      Long repositoryId,
      List<String> attributesKeys,
      boolean deleteAll,
      boolean canNotOperateOnProtected,
      RepositoryEnums.RepositoryTypeEnum repositoryType)
      throws ModelDBException;
}
