package ai.verta.modeldb.experimentRun;

import ai.verta.common.Artifact;
import ai.verta.common.KeyValue;
import ai.verta.modeldb.CodeVersion;
import ai.verta.modeldb.CommitArtifactPart;
import ai.verta.modeldb.CommitArtifactPart.Response;
import ai.verta.modeldb.CommitMultipartArtifact;
import ai.verta.modeldb.Experiment;
import ai.verta.modeldb.ExperimentRun;
import ai.verta.modeldb.FindExperimentRuns;
import ai.verta.modeldb.GetCommittedArtifactParts;
import ai.verta.modeldb.GetExperimentRunsByDatasetVersionId;
import ai.verta.modeldb.GetVersionedInput;
import ai.verta.modeldb.ListBlobExperimentRunsRequest;
import ai.verta.modeldb.ListCommitExperimentRunsRequest;
import ai.verta.modeldb.LogVersionedInput;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.Observation;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.SortExperimentRuns;
import ai.verta.modeldb.TopExperimentRunsSelector;
import ai.verta.modeldb.dto.ExperimentRunPaginationDTO;
import ai.verta.modeldb.project.ProjectDAO;
import ai.verta.modeldb.versioning.CommitFunction;
import ai.verta.modeldb.versioning.RepositoryFunction;
import ai.verta.uac.UserInfo;
import com.google.protobuf.InvalidProtocolBufferException;
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
   * @throws InvalidProtocolBufferException
   */
  ExperimentRun insertExperimentRun(ExperimentRun experimentRun, UserInfo userInfo)
      throws InvalidProtocolBufferException, ModelDBException, NoSuchAlgorithmException;

  /**
   * Delete the ExperimentRuns from database using experimentRunId list.
   *
   * @param experimentRunIds : list of experimentRun Id
   * @return {@link Boolean} : Boolean updated status
   */
  Boolean deleteExperimentRuns(List<String> experimentRunIds);

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
   * @throws InvalidProtocolBufferException
   */
  ExperimentRunPaginationDTO getExperimentRunsFromEntity(
      ProjectDAO projectDAO,
      String entityKey,
      String entityValue,
      Integer pageNumber,
      Integer pageLimit,
      Boolean order,
      String sortKey)
      throws InvalidProtocolBufferException;

  /**
   * Get ExperimentRun entity using given experimentRunId from database.
   *
   * @param String key --> key like ModelDBConstants.ID, ModelDBConstants.Name etc.
   * @param String value --> value like ExperimentRun.id, ExperimentRun.name etc.
   * @param UserInfo userInfo --> current user info
   * @return List<ExperimentRun> experimentRuns
   * @throws InvalidProtocolBufferException
   */
  List<ExperimentRun> getExperimentRuns(String key, String value, UserInfo userInfo)
      throws InvalidProtocolBufferException;

  /**
   * Get ExperimentRuns entity using given experimentRunIds from database.
   *
   * @param List<String> experimentIds --> experimentRun.id
   * @return ExperimentRun experimentRun
   * @throws InvalidProtocolBufferException
   */
  List<ExperimentRun> getExperimentRunsByBatchIds(List<String> experimentRunIds)
      throws InvalidProtocolBufferException;

  /**
   * Get ExperimentRun entity using given experimentRunId from database.
   *
   * @param String experimentId --> experimentRun.id
   * @return ExperimentRun experimentRun
   * @throws InvalidProtocolBufferException
   */
  ExperimentRun getExperimentRun(String experimentRunId)
      throws InvalidProtocolBufferException, ModelDBException;

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
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  ExperimentRun updateExperimentRunDescription(
      String experimentRunId, String experimentRunDescription)
      throws InvalidProtocolBufferException, ModelDBException;

  /**
   * @param experimentRunId : experimentRun.id
   * @param updatedCodeVersion : updated experimentRun code version snapshot from client request
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  void logExperimentRunCodeVersion(String experimentRunId, CodeVersion updatedCodeVersion)
      throws InvalidProtocolBufferException;

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
   * @throws InvalidProtocolBufferException : InvalidProtocolBufferException
   */
  ExperimentRun addExperimentRunTags(String experimentRunId, List<String> tagsList)
      throws InvalidProtocolBufferException, ModelDBException;

  /**
   * Delete ExperimentRun Tags from ExperimentRun entity.
   *
   * @param deleteAll : flag
   * @param experimentRunTagList : tags list for deletion
   * @param experimentRunId : ExperimentRun.id
   * @return ExperimentRun updatedExperimentRun
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  ExperimentRun deleteExperimentRunTags(
      String experimentRunId, List<String> experimentRunTagList, Boolean deleteAll)
      throws InvalidProtocolBufferException, ModelDBException;

  /**
   * ExperimentRun has Observations list field. Add new Observation in that Observations List.
   *
   * @param experimentRunId
   * @param observations
   * @throws InvalidProtocolBufferException
   */
  void logObservations(String experimentRunId, List<Observation> observations)
      throws InvalidProtocolBufferException;

  /**
   * Return List<Observation> using @param observationKey from Observation field in ExperimentRun.
   *
   * @param experimentRunId
   * @param observationKey
   * @return List<Observation> observation list
   * @throws InvalidProtocolBufferException
   */
  List<Observation> getObservationByKey(String experimentRunId, String observationKey)
      throws InvalidProtocolBufferException;

  /**
   * ExperimentRun has Metrics list field. Add new metric in that Metrics List.
   *
   * @param experimentRunId
   * @param metrics has KeyValue entity
   * @throws InvalidProtocolBufferException
   */
  void logMetrics(String experimentRunId, List<KeyValue> metrics)
      throws InvalidProtocolBufferException;

  /**
   * Return List<KeyValue> metrics from ExperimentRun.
   *
   * @param experimentRunId
   * @return List<KeyValue> metric list
   * @throws InvalidProtocolBufferException
   */
  List<KeyValue> getExperimentRunMetrics(String experimentRunId)
      throws InvalidProtocolBufferException;

  /**
   * Return List<Artifact> dataset from ExperimentRun entity.
   *
   * @param experimentRunId
   * @return List<Artifact> dataset list from experimentRun
   * @throws InvalidProtocolBufferException
   */
  List<Artifact> getExperimentRunDatasets(String experimentRunId)
      throws InvalidProtocolBufferException, ModelDBException;

  /**
   * ExperimentRun has artifacts field. Add new Artifact in that artifacts List.
   *
   * @param experimentRunId
   * @param artifacts
   * @throws InvalidProtocolBufferException
   */
  void logArtifacts(String experimentRunId, List<Artifact> artifacts)
      throws InvalidProtocolBufferException;

  /**
   * Return List<Artifact> artifacts from ExperimentRun entity.
   *
   * @param experimentRunId
   * @return List<Artifact> artifact list from experimentRun
   * @throws InvalidProtocolBufferException
   */
  List<Artifact> getExperimentRunArtifacts(String experimentRunId)
      throws InvalidProtocolBufferException;

  /**
   * ExperimentRun has hyperparameters field. Add new hyperparameter in that hyperparameter List.
   *
   * @param experimentRunId
   * @param hyperparameters has KeyValue list.
   * @throws InvalidProtocolBufferException
   */
  void logHyperparameters(String experimentRunId, List<KeyValue> hyperparameters)
      throws InvalidProtocolBufferException;

  /**
   * ExperimentRun has hyperparameters field, Return List<KeyValue> hyperparameters from
   * ExperimentRun entity.
   *
   * @param experimentRunId
   * @return List<KeyValue> hyperparameter list
   * @throws InvalidProtocolBufferException
   */
  List<KeyValue> getExperimentRunHyperparameters(String experimentRunId)
      throws InvalidProtocolBufferException;

  /**
   * ExperimentRun has attributes field. Add new attribute in that attribute List.
   *
   * @param experimentRunId
   * @param attributes has KeyValue.
   * @throws InvalidProtocolBufferException
   */
  void logAttributes(String experimentRunId, List<KeyValue> attributes)
      throws InvalidProtocolBufferException;

  /**
   * ExperimentRun has attributes field, Return List<KeyValue> attributes from ExperimentRun entity.
   *
   * @param getAll flag
   * @param attributeKeyList
   * @param experimentRunId
   * @return List<KeyValue> attribute list
   * @throws InvalidProtocolBufferException
   */
  List<KeyValue> getExperimentRunAttributes(
      String experimentRunId, List<String> attributeKeyList, Boolean getAll)
      throws InvalidProtocolBufferException;

  /**
   * Return list of experimentRuns based on FindExperimentRuns queryParameters
   *
   * @param projectDAO : projectDAO
   * @param currentLoginUserInfo : current login user info
   * @param queryParameters --> query parameters for filtering experimentRuns
   * @return ExperimentRunPaginationDTO -- experimentRunPaginationDTO contains the list of
   *     experimentRuns based on filter queryParameters & total_pages count
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  ExperimentRunPaginationDTO findExperimentRuns(
      ProjectDAO projectDAO, UserInfo currentLoginUserInfo, FindExperimentRuns queryParameters)
      throws InvalidProtocolBufferException;

  /**
   * Return sorted list of experimentRuns based on SortExperimentRuns queryParameters
   *
   * @param queryParameters --> query parameters for sorting experimentRuns
   * @return ExperimentRunPaginationDTO -- experimentRunPaginationDTO contains the list of
   *     experimentRuns based on filter queryParameters & total_pages count
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  ExperimentRunPaginationDTO sortExperimentRuns(
      ProjectDAO projectDAO, SortExperimentRuns queryParameters)
      throws InvalidProtocolBufferException;

  /**
   * Return "Top n" (e.g. Top 5) experimentRuns after applying the sort queryParameters
   *
   * @param TopExperimentRunsSelector queryParameters --> query parameters for sorting and selecting
   *     "Top n" experimentRuns
   * @return List<ExperimentRun> -- return list of experimentRuns based on top selector
   *     queryParameters
   * @throws InvalidProtocolBufferException
   */
  List<ExperimentRun> getTopExperimentRuns(
      ProjectDAO projectDAO, TopExperimentRunsSelector queryParameters)
      throws InvalidProtocolBufferException;

  /**
   * Fetch ExperimentRun Tags from database using experimentRunId.
   *
   * @param String experimentRunId
   * @return List<String> ExperimentRunTags.
   * @throws InvalidProtocolBufferException
   */
  List<String> getExperimentRunTags(String experimentRunId) throws InvalidProtocolBufferException;

  /**
   * Add attributes in database using experimentRunId.
   *
   * @param experimentRunId : ExperimentRun.id
   * @param attributesList : new attribute list
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  void addExperimentRunAttributes(String experimentRunId, List<KeyValue> attributesList)
      throws InvalidProtocolBufferException;

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
   * @throws InvalidProtocolBufferException
   */
  String getJobId(String experimentRunId) throws InvalidProtocolBufferException;

  /**
   * Deep copy experimentRuns in database. We do not clone the artifacts/ data sets, so The cloned
   * experimentRun still point to original artifacts/ data sets.
   *
   * @param srcExperiment
   * @param newProject
   * @param newOwner
   * @return
   * @throws InvalidProtocolBufferException
   */
  ExperimentRun deepCopyExperimentRunForUser(
      ExperimentRun srcExperimentRun,
      Experiment newExperiment,
      Project newProject,
      UserInfo newOwner)
      throws InvalidProtocolBufferException, ModelDBException;

  /**
   * Get ExperimentRun entities matching on key value list.
   *
   * @param keyValues
   * @return
   * @throws InvalidProtocolBufferException
   */
  List<ExperimentRun> getExperimentRuns(List<KeyValue> keyValues)
      throws InvalidProtocolBufferException;

  /**
   * ExperimentRun has datasets field. Add new dataset in that dataset List.
   *
   * @param experimentRunId
   * @param datasets List of artifacts of type Data and object_id pointing to an existing
   *     datasetVersion
   * @throws InvalidProtocolBufferException
   */
  void logDatasets(String experimentRunId, List<Artifact> datasets, boolean overwrite)
      throws InvalidProtocolBufferException, ModelDBException;

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
  List<?> getSelectedFieldsByExperimentRunIds(
      List<String> experimentRunIds, List<String> selectedFields)
      throws InvalidProtocolBufferException;

  /**
   * Return the list of experimentRunIds using given project ids
   *
   * @param projectIds : project id list
   * @return {@link List<String>} : list of experimentRun Ids
   */
  List<String> getExperimentRunIdsByProjectIds(List<String> projectIds)
      throws InvalidProtocolBufferException;

  /**
   * Return the list of experimentRunIds using given experiment ids
   *
   * @param experimentIds : experiment id list
   * @return {@link List<String>} : list of experimentRun Ids
   */
  List<String> getExperimentRunIdsByExperimentIds(List<String> experimentIds)
      throws InvalidProtocolBufferException;

  void logVersionedInput(LogVersionedInput request)
      throws InvalidProtocolBufferException, ModelDBException, NoSuchAlgorithmException;

  void deleteLogVersionedInputs(Session session, Long repoId, String commitHash);

  void deleteLogVersionedInputs(Session session, List<Long> repoIds);

  GetVersionedInput.Response getVersionedInputs(GetVersionedInput request)
      throws InvalidProtocolBufferException;

  ListCommitExperimentRunsRequest.Response listCommitExperimentRuns(
      ProjectDAO projectDAO,
      ListCommitExperimentRunsRequest request,
      RepositoryFunction repositoryFunction,
      CommitFunction commitFunction)
      throws ModelDBException, InvalidProtocolBufferException;

  ListBlobExperimentRunsRequest.Response listBlobExperimentRuns(
      ProjectDAO projectDAO,
      ListBlobExperimentRunsRequest request,
      RepositoryFunction repositoryFunction,
      CommitFunction commitFunction)
      throws ModelDBException, InvalidProtocolBufferException;

  Entry<String, String> getExperimentRunArtifactS3PathAndMultipartUploadID(
      String experimentRunId, String key, long partNumber, S3KeyFunction initializeMultipart)
      throws ModelDBException, InvalidProtocolBufferException;

  Response commitArtifactPart(CommitArtifactPart request)
      throws ModelDBException, InvalidProtocolBufferException;

  GetCommittedArtifactParts.Response getCommittedArtifactParts(GetCommittedArtifactParts request)
      throws ModelDBException, InvalidProtocolBufferException;

  CommitMultipartArtifact.Response commitMultipartArtifact(
      CommitMultipartArtifact request, CommitMultipartFunction commitMultipart)
      throws ModelDBException, InvalidProtocolBufferException;

  void deleteExperimentRunKeyValuesEntities(
      String experimentRunId,
      List<String> experimentRunKeyValuesKeys,
      Boolean deleteAll,
      String fieldType)
      throws InvalidProtocolBufferException;

  void deleteExperimentRunObservationsEntities(
      String experimentRunId, List<String> experimentRunObservationsKeys, Boolean deleteAll)
      throws InvalidProtocolBufferException;

  ExperimentRunPaginationDTO getExperimentRunsByDatasetVersionId(
      ProjectDAO projectDAO, GetExperimentRunsByDatasetVersionId request)
      throws ModelDBException, InvalidProtocolBufferException;
}
