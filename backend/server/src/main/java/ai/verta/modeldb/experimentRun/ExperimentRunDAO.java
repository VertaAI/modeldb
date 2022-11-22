package ai.verta.modeldb.experimentRun;

import ai.verta.common.Artifact;
import ai.verta.common.CodeVersion;
import ai.verta.common.KeyValue;
import ai.verta.modeldb.*;
import ai.verta.modeldb.CommitArtifactPart.Response;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.PermissionDeniedException;
import ai.verta.modeldb.dto.ExperimentRunPaginationDTO;
import ai.verta.modeldb.versioning.CommitFunction;
import ai.verta.modeldb.versioning.EnvironmentBlob;
import ai.verta.modeldb.versioning.RepositoryFunction;
import ai.verta.uac.UserInfo;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.hibernate.Session;

public interface ExperimentRunDAO {

  /**
   * Insert ExperimentRun entity in database.
   *
   * @param ExperimentRun experimentRun
   * @param userInfo
   * @return ExperimentRun insertedExperimentRun
   */
  ExperimentRun insertExperimentRun(ExperimentRun experimentRun, UserInfo userInfo)
      throws ModelDBException, NoSuchAlgorithmException;

  /**
   * Delete the ExperimentRuns from database using experimentRunId list.
   *
   * @param experimentRunIds : list of experimentRun Id
   * @return {@link List} : deleted experiment run ids
   */
  List<String> deleteExperimentRuns(List<String> experimentRunIds);

  /**
   * Get List of ExperimentRun entity using given projectId from database.
   *
   * @param pageNumber --> page number use for pagination.
   * @param pageLimit --> page limit is per page record count.
   * @param sortKey -- > Use this field for filter data.
   * @param order --> this parameter has order like asc OR desc.
   * @param entityKey --> like ModelDBConstants.PROJECT_ID, ModelDBConstants.EXPERIMENT_ID etc.
   * @param entityValue --> like Project.id, experiment.id etc.
   * @return ExperimentRunPaginationDTO experimentRunPaginationDTO contains the experimentRunList &
   *     total_pages count
   */
  ExperimentRunPaginationDTO getExperimentRunsFromEntity(
      String entityKey,
      String entityValue,
      Integer pageNumber,
      Integer pageLimit,
      Boolean order,
      String sortKey)
      throws PermissionDeniedException;

  /**
   * Get ExperimentRun entity using given experimentRunId from database.
   *
   * @param String key --> key like ModelDBConstants.ID, ModelDBConstants.Name etc.
   * @param String value --> value like ExperimentRun.id, ExperimentRun.name etc.
   * @param UserInfo userInfo --> current user info
   * @return List<ExperimentRun> experimentRuns
   */
  List<ExperimentRun> getExperimentRuns(String key, String value, UserInfo userInfo);

  /**
   * Get ExperimentRuns entity using given experimentRunIds from database.
   *
   * @param List<String> experimentIds --> experimentRun.id
   * @return ExperimentRun experimentRun
   */
  List<ExperimentRun> getExperimentRunsByBatchIds(List<String> experimentRunIds);

  /**
   * Get ExperimentRun entity using given experimentRunId from database.
   *
   * @param String experimentId --> experimentRun.id
   * @return ExperimentRun experimentRun
   */
  ExperimentRun getExperimentRun(String experimentRunId) throws ModelDBException;

  boolean isExperimentRunExists(Session session, String experimentRunId);

  /**
   * @param experimentRunId : experimentRun.id
   * @param experimentRunName : updated experimentRun name from client request
   */
  void updateExperimentRunName(String experimentRunId, String experimentRunName);

  /**
   * @param experimentRunId : experimentRun.id
   * @param experimentRunDescription : updated experimentRun description from client request
   * @return {@link ExperimentRun} : updated experimentRun
   */
  ExperimentRun updateExperimentRunDescription(
      String experimentRunId, String experimentRunDescription) throws ModelDBException;

  /**
   * @param experimentRunId : experimentRun.id
   * @param updatedCodeVersion : updated experimentRun code version snapshot from client request
   */
  void logExperimentRunCodeVersion(String experimentRunId, CodeVersion updatedCodeVersion);

  /**
   * @param experimentRunId : experimentRun.id
   * @param parentExperimentRunId : experimentRun parentId from client request
   */
  void setParentExperimentRunId(String experimentRunId, String parentExperimentRunId);

  /**
   * Add List of ExperimentRun Tags in database.
   *
   * @param experimentRunId : ExperimentRun.id
   * @param tagsList : tag list
   * @return ExperimentRun : updatedExperimentRun
   */
  ExperimentRun addExperimentRunTags(String experimentRunId, List<String> tagsList)
      throws ModelDBException;

  /**
   * Delete ExperimentRun Tags from ExperimentRun entity.
   *
   * @param deleteAll : flag
   * @param experimentRunTagList : tags list for deletion
   * @param experimentRunId : ExperimentRun.id
   * @return ExperimentRun updatedExperimentRun
   */
  ExperimentRun deleteExperimentRunTags(
      String experimentRunId, List<String> experimentRunTagList, Boolean deleteAll)
      throws ModelDBException;

  /**
   * ExperimentRun has Observations list field. Add new Observation in that Observations List.
   *
   * @param experimentRunId
   * @param observations
   */
  void logObservations(String experimentRunId, List<Observation> observations);

  /**
   * Return List<Observation> using @param observationKey from Observation field in ExperimentRun.
   *
   * @param experimentRunId
   * @param observationKey
   * @return List<Observation> observation list
   */
  List<Observation> getObservationByKey(String experimentRunId, String observationKey);

  /**
   * ExperimentRun has Metrics list field. Add new metric in that Metrics List.
   *
   * @param experimentRunId
   * @param metrics has KeyValue entity
   */
  void logMetrics(String experimentRunId, List<KeyValue> metrics);

  /**
   * Return List<KeyValue> metrics from ExperimentRun.
   *
   * @param experimentRunId
   * @return List<KeyValue> metric list
   */
  List<KeyValue> getExperimentRunMetrics(String experimentRunId);

  /**
   * Return List<Artifact> dataset from ExperimentRun entity.
   *
   * @param experimentRunId
   * @return List<Artifact> dataset list from experimentRun
   */
  List<Artifact> getExperimentRunDatasets(String experimentRunId) throws ModelDBException;

  /**
   * ExperimentRun has artifacts field. Add new Artifact in that artifacts List.
   *
   * @param experimentRunId
   * @param artifacts
   */
  void logArtifacts(String experimentRunId, List<Artifact> artifacts) throws ModelDBException;

  /**
   * Return List<Artifact> artifacts from ExperimentRun entity.
   *
   * @param experimentRunId
   * @return List<Artifact> artifact list from experimentRun
   */
  List<Artifact> getExperimentRunArtifacts(String experimentRunId);

  /**
   * ExperimentRun has hyperparameters field. Add new hyperparameter in that hyperparameter List.
   *
   * @param experimentRunId
   * @param hyperparameters has KeyValue list.
   */
  void logHyperparameters(String experimentRunId, List<KeyValue> hyperparameters);

  /**
   * ExperimentRun has hyperparameters field, Return List<KeyValue> hyperparameters from
   * ExperimentRun entity.
   *
   * @param experimentRunId
   * @return List<KeyValue> hyperparameter list
   */
  List<KeyValue> getExperimentRunHyperparameters(String experimentRunId);

  /**
   * ExperimentRun has attributes field. Add new attribute in that attribute List.
   *
   * @param experimentRunId
   * @param attributes has KeyValue.
   */
  void logAttributes(String experimentRunId, List<KeyValue> attributes);

  /**
   * ExperimentRun has attributes field, Return List<KeyValue> attributes from ExperimentRun entity.
   *
   * @param getAll flag
   * @param attributeKeyList
   * @param experimentRunId
   * @return List<KeyValue> attribute list
   */
  List<KeyValue> getExperimentRunAttributes(
      String experimentRunId, List<String> attributeKeyList, Boolean getAll);

  /**
   * Return list of experimentRuns based on FindExperimentRuns queryParameters
   *
   * @param projectDAO : projectDAO
   * @param currentLoginUserInfo : current login user info
   * @param queryParameters --> query parameters for filtering experimentRuns
   * @return ExperimentRunPaginationDTO -- experimentRunPaginationDTO contains the list of
   *     experimentRuns based on filter queryParameters & total_pages count
   */
  ExperimentRunPaginationDTO findExperimentRuns(
      UserInfo currentLoginUserInfo, FindExperimentRuns queryParameters)
      throws PermissionDeniedException;

  /**
   * Return sorted list of experimentRuns based on SortExperimentRuns queryParameters
   *
   * @param queryParameters --> query parameters for sorting experimentRuns
   * @return ExperimentRunPaginationDTO -- experimentRunPaginationDTO contains the list of
   *     experimentRuns based on filter queryParameters & total_pages count
   */
  ExperimentRunPaginationDTO sortExperimentRuns(SortExperimentRuns queryParameters)
      throws PermissionDeniedException;

  /**
   * Return "Top n" (e.g. Top 5) experimentRuns after applying the sort queryParameters
   *
   * @param TopExperimentRunsSelector queryParameters --> query parameters for sorting and selecting
   *     "Top n" experimentRuns
   * @return List<ExperimentRun> -- return list of experimentRuns based on top selector
   *     queryParameters
   */
  List<ExperimentRun> getTopExperimentRuns(TopExperimentRunsSelector queryParameters)
      throws PermissionDeniedException;

  /**
   * Fetch ExperimentRun Tags from database using experimentRunId.
   *
   * @param String experimentRunId
   * @return List<String> ExperimentRunTags.
   */
  List<String> getExperimentRunTags(String experimentRunId);

  /**
   * Add attributes in database using experimentRunId.
   *
   * @param experimentRunId : ExperimentRun.id
   * @param attributesList : new attribute list
   */
  void addExperimentRunAttributes(String experimentRunId, List<KeyValue> attributesList);

  /**
   * Delete ExperimentRun Attributes in database using experimentRunId.
   *
   * @param deleteAll: flag
   * @param attributeKeyList : attribute list for deletion
   * @param experimentRunId : ExperimentRun.id
   */
  void deleteExperimentRunAttributes(
      String experimentRunId, List<String> attributeKeyList, Boolean deleteAll);

  /**
   * Log JobId in ExperimentRun entity.
   *
   * @param experimentRunId : ExperimentRun.id
   * @param jobId : job.id
   */
  void logJobId(String experimentRunId, String jobId);

  /**
   * Get JobId from ExperimentRun entity.
   *
   * @param String experimentRunId
   * @return String jobId
   */
  String getJobId(String experimentRunId);

  /**
   * Get ExperimentRun entities matching on key value list.
   *
   * @param keyValues
   * @return
   */
  List<ExperimentRun> getExperimentRuns(List<KeyValue> keyValues);

  /**
   * ExperimentRun has datasets field. Add new dataset in that dataset List.
   *
   * @param experimentRunId
   * @param datasets List of artifacts of type Data and object_id pointing to an existing
   *     datasetVersion
   */
  void logDatasets(String experimentRunId, List<Artifact> datasets, boolean overwrite)
      throws ModelDBException;

  /**
   * Deletes the artifact key associated with the experiment run
   *
   * @param experimentRunId : ExperimentRun.id
   * @param atrifactKey : artifact key
   */
  void deleteArtifacts(String experimentRunId, String atrifactKey);

  /**
   * Get Project Id from ExperimentRun entity using given experimentRunID.
   *
   * @param experimentRunId : experimentRun.id
   * @return {@link String} : project.id
   */
  String getProjectIdByExperimentRunId(String experimentRunId);

  /**
   * Get Project Id list from ExperimentRun entity using given experimentRunID list.
   *
   * @param experimentRunIds : list of experimentRun.id
   * @return {@link Map} : key: experimentRunID, Value: projectID
   */
  Map<String, String> getProjectIdsFromExperimentRunIds(List<String> experimentRunIds);

  /**
   * Get ExperimentRun entity with selected fields using given list of experimentRunID.
   *
   * @param experimentRunIds : list of experimentRun.id
   * @param selectedFields : list of selected field like ExperimentRun.attributes,
   *     ExperimentRun.project_id etc.
   * @return {@link List} : value = experimentRun OR selectedFields_array_objects
   */
  @SuppressWarnings({"squid:S1452"})
  List<?> getSelectedFieldsByExperimentRunIds(
      List<String> experimentRunIds, List<String> selectedFields);

  /**
   * Return the list of experimentRunIds using given project ids
   *
   * @param projectIds : project id list
   * @return {@link List<String>} : list of experimentRun Ids
   */
  List<String> getExperimentRunIdsByProjectIds(List<String> projectIds);

  /**
   * Return the list of experimentRunIds using given experiment ids
   *
   * @param experimentIds : experiment id list
   * @return {@link List<String>} : list of experimentRun Ids
   */
  List<String> getExperimentRunIdsByExperimentIds(List<String> experimentIds);

  void logVersionedInput(LogVersionedInput request)
      throws ModelDBException, NoSuchAlgorithmException;

  void deleteLogVersionedInputs(Session session, Long repoId, String commitHash);

  void deleteLogVersionedInputs(Session session, List<Long> repoIds);

  GetVersionedInput.Response getVersionedInputs(GetVersionedInput request);

  ListCommitExperimentRunsRequest.Response listCommitExperimentRuns(
      ListCommitExperimentRunsRequest request,
      RepositoryFunction repositoryFunction,
      CommitFunction commitFunction)
      throws ModelDBException;

  ListBlobExperimentRunsRequest.Response listBlobExperimentRuns(
      ListBlobExperimentRunsRequest request,
      RepositoryFunction repositoryFunction,
      CommitFunction commitFunction)
      throws ModelDBException;

  Entry<String, String> getExperimentRunArtifactS3PathAndMultipartUploadID(
      String experimentRunId, String key, long partNumber, S3KeyFunction initializeMultipart)
      throws ModelDBException;

  Response commitArtifactPart(CommitArtifactPart request) throws ModelDBException;

  GetCommittedArtifactParts.Response getCommittedArtifactParts(GetCommittedArtifactParts request)
      throws ModelDBException;

  CommitMultipartArtifact.Response commitMultipartArtifact(
      CommitMultipartArtifact request, CommitMultipartFunction commitMultipart)
      throws ModelDBException;

  void deleteExperimentRunKeyValuesEntities(
      String experimentRunId,
      List<String> experimentRunKeyValuesKeys,
      Boolean deleteAll,
      String fieldType);

  void deleteExperimentRunObservationsEntities(
      String experimentRunId, List<String> experimentRunObservationsKeys, Boolean deleteAll);

  ExperimentRunPaginationDTO getExperimentRunsByDatasetVersionId(
      GetExperimentRunsByDatasetVersionId request) throws ModelDBException;

  ExperimentRun cloneExperimentRun(CloneExperimentRun cloneExperimentRun, UserInfo userInfo)
      throws ModelDBException;

  void logEnvironment(String experimentRunId, EnvironmentBlob environmentBlob);
}
