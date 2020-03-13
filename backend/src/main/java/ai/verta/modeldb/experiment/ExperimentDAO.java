package ai.verta.modeldb.experiment;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.Artifact;
import ai.verta.modeldb.CodeVersion;
import ai.verta.modeldb.Experiment;
import ai.verta.modeldb.FindExperiments;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.dto.ExperimentPaginationDTO;
import ai.verta.uac.UserInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.Map;

public interface ExperimentDAO {

  /**
   * Insert Experiment entity in database.
   *
   * @param Experiment experiment
   * @return Experiment insertedExperiment
   * @throws InvalidProtocolBufferException
   */
  Experiment insertExperiment(Experiment experiment) throws InvalidProtocolBufferException;

  /**
   * @param experimentId : experiment.id
   * @param experimentName : updated experiment name from client request
   * @return {@link Experiment} : updated experiment
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  Experiment updateExperimentName(String experimentId, String experimentName)
      throws InvalidProtocolBufferException;

  /**
   * @param experimentId : experiment.id
   * @param experimentDescription : updated experiment description from client request
   * @return {@link Experiment} : updated experiment
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  Experiment updateExperimentDescription(String experimentId, String experimentDescription)
      throws InvalidProtocolBufferException;

  /**
   * Get Experiment entity using given experimentID from database.
   *
   * @param String experimentId
   * @return Experiment experiment
   * @throws InvalidProtocolBufferException
   */
  Experiment getExperiment(String experimentId) throws InvalidProtocolBufferException;

  /**
   * @param experimentIds : list of experiment ids
   * @return : experiment list
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  public List<Experiment> getExperimentsByBatchIds(List<String> experimentIds)
      throws InvalidProtocolBufferException;

  /**
   * Get List of Experiment entity using given projectId from database.
   *
   * @param sortKey -- > Use this field getExperimentsInProjector filter data.
   * @param order --> this parameter has order like asc OR desc.
   * @param projectId projectId
   * @param pageNumber page number
   * @param pageLimit page limit
   * @return ExperimentPaginationDTO experimentPaginationDTO contain the experimentList &
   *     total_pages count
   * @throws InvalidProtocolBufferException
   */
  ExperimentPaginationDTO getExperimentsInProject(
      String projectId, Integer pageNumber, Integer pageLimit, Boolean order, String sortKey)
      throws InvalidProtocolBufferException;

  /**
   * Add List of Experiment Tags in database.
   *
   * @param String experimentId, List<String> tagsList
   * @return Experiment updatedExperiment
   * @throws InvalidProtocolBufferException
   */
  Experiment addExperimentTags(String experimentId, List<String> tagsList)
      throws InvalidProtocolBufferException;

  /**
   * Fetch Experiment Tags from database using experimentId.
   *
   * @param String experimentId
   * @return List<String> projectTags
   * @throws InvalidProtocolBufferException
   */
  List<String> getExperimentTags(String experimentId) throws InvalidProtocolBufferException;

  /**
   * Delete Experiment Tags from Experiment entity.
   *
   * @param tagList
   * @param deleteAll
   * @param String experimentId
   * @return Experiment updatedExperiment
   * @throws InvalidProtocolBufferException
   */
  Experiment deleteExperimentTags(
      String experimentId, List<String> experimentTagList, Boolean deleteAll)
      throws InvalidProtocolBufferException;

  /**
   * Add Attribute in Experiment Attributes list in database.
   *
   * @param String experimentId
   * @param List<KeyValue> attributes
   * @return Experiment updatedExperiment
   * @throws InvalidProtocolBufferException
   */
  Experiment addExperimentAttributes(String experimentId, List<KeyValue> attributes)
      throws InvalidProtocolBufferException;

  /**
   * Fetch Experiment Attributes from database using experimentId.
   *
   * @param getAll flag
   * @param attributeKeyList
   * @param String experimentId
   * @return List<KeyValue> experimentAttributes.
   * @throws InvalidProtocolBufferException
   */
  List<KeyValue> getExperimentAttributes(
      String experimentId, List<String> attributeKeyList, Boolean getAll)
      throws InvalidProtocolBufferException;

  /**
   * Delete Experiment Attributes in database using experimentId.
   *
   * @param deleteAll
   * @param attributeKeyList
   * @param String experimentId
   * @return Experiment experiment
   * @throws InvalidProtocolBufferException
   */
  Experiment deleteExperimentAttributes(
      String experimentId, List<String> attributeKeyList, Boolean deleteAll)
      throws InvalidProtocolBufferException;

  /**
   * Delete the Experiment from database using experimentId.
   *
   * <p>TODO : Add logic of Deleting ExperimentRun associated with Experiment.
   *
   * @param String experimentId
   * @return Boolean updated status
   * @throws InvalidProtocolBufferException
   */
  Boolean deleteExperiment(String experimentId) throws InvalidProtocolBufferException;

  /**
   * Delete the Experiments from database using experimentIds list.
   *
   * @param experimentIds : list of experimentRunId
   * @return {@link Boolean} : Boolean updated status
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  Boolean deleteExperiments(List<String> experimentIds) throws InvalidProtocolBufferException;

  /**
   * Return experiment using given key value list. keyValue has key as ModelDBConstants.PROJECT_ID
   * etc. and value as experiment.projectId
   *
   * @param keyValue --> list of KeyValue
   * @return Experiment entity.
   * @throws InvalidProtocolBufferException
   */
  @Deprecated
  // This should be deprecated, there should be no assumption on the length of data in a generic
  // function
  // asserts if required should be at the caller which has more context about the data being queries
  Experiment getExperiment(List<KeyValue> keyValues) throws InvalidProtocolBufferException;

  /**
   * Return experiment using given key value list. keyValue has key as ModelDBConstants.PROJECT_ID
   * etc. and value as experiment.projectId
   *
   * @param keyValue --> list of KeyValue
   * @return Experiment entity.
   * @throws InvalidProtocolBufferException
   */
  List<Experiment> getExperiments(List<KeyValue> keyValues) throws InvalidProtocolBufferException;

  /**
   * Deep copy experiments in database. In current scope we deep copy associated ExperimentRuns
   *
   * @param srcExperiment
   * @param newProject
   * @param newOwner
   * @return
   * @throws InvalidProtocolBufferException
   */
  Experiment deepCopyExperimentForUser(
      Experiment srcExperiment, Project newProject, UserInfo newOwner)
      throws InvalidProtocolBufferException;

  /**
   * @param experimentId : experiment.id
   * @param updatedCodeVersion : updated experiment code version snapshot from client request
   * @return {@link Experiment} : updated experiment
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  Experiment logExperimentCodeVersion(String experimentId, CodeVersion updatedCodeVersion)
      throws InvalidProtocolBufferException;

  /**
   * Return list of experiments based on FindExperiments queryParameters
   *
   * @param queryParameters : queryParameters --> query parameters for filtering experiments
   * @return ExperimentPaginationDTO : experimentPaginationDTO contains the list of experiments
   *     based on filter queryParameters & total_pages count
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  ExperimentPaginationDTO findExperiments(FindExperiments queryParameters)
      throws InvalidProtocolBufferException;

  /**
   * Experiment has artifacts field. Add new Artifact in that artifacts List.
   *
   * @param experimentId : experiment.id
   * @param artifacts : experiment.artifacts
   * @return {@link Experiment} : updated experiment
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  Experiment logArtifacts(String experimentId, List<Artifact> artifacts)
      throws InvalidProtocolBufferException;

  /**
   * Return List<Artifact> artifacts from Experiment entity.
   *
   * @param experimentId : experiment.id
   * @return {@link List<Artifact>} : artifact list from Experiment
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  List<Artifact> getExperimentArtifacts(String experimentId) throws InvalidProtocolBufferException;

  /**
   * Deletes the artifact key associated with the experiment
   *
   * @param experimentId : experiment.id
   * @param artifactKey : artifact.key
   * @return {@link Experiment} : updated experiment
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  Experiment deleteArtifacts(String experimentId, String artifactKey)
      throws InvalidProtocolBufferException;

  /**
   * Get Project Id from Experiment entity using given experimentID.
   *
   * @param experimentIds : list of experiment.id
   * @return {@link Map} : key=experiment.id, value=project.id
   */
  Map<String, String> getProjectIdsByExperimentIds(List<String> experimentIds);
}
