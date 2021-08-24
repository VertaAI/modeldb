package ai.verta.modeldb.experiment;

import ai.verta.common.Artifact;
import ai.verta.common.CodeVersion;
import ai.verta.common.KeyValue;
import ai.verta.modeldb.Experiment;
import ai.verta.modeldb.FindExperiments;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.dto.ExperimentPaginationDTO;
import ai.verta.modeldb.exceptions.PermissionDeniedException;
import ai.verta.modeldb.project.ProjectDAO;
import ai.verta.uac.UserInfo;
import java.util.List;
import java.util.Map;

public interface ExperimentDAO {

  /**
   * Insert Experiment entity in database.
   *
   * @param Experiment experiment
   * @param userInfo
   * @return Experiment insertedExperiment
   */
  Experiment insertExperiment(Experiment experiment, UserInfo userInfo);

  /**
   * @param experimentId : experiment.id
   * @param experimentName : updated experiment name from client request
   * @return {@link Experiment} : updated experiment
   */
  Experiment updateExperimentName(String experimentId, String experimentName);

  /**
   * @param experimentId : experiment.id
   * @param experimentDescription : updated experiment description from client request
   * @return {@link Experiment} : updated experiment
   */
  Experiment updateExperimentDescription(String experimentId, String experimentDescription);

  /**
   * Get Experiment entity using given experimentID from database.
   *
   * @param String experimentId
   * @return Experiment experiment
   */
  Experiment getExperiment(String experimentId);
  /**
   * @param experimentIds : list of experiment ids
   * @return : experiment list
   */
  public List<Experiment> getExperimentsByBatchIds(List<String> experimentIds);

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
   */
  ExperimentPaginationDTO getExperimentsInProject(
      ProjectDAO projectDAO,
      String projectId,
      Integer pageNumber,
      Integer pageLimit,
      Boolean order,
      String sortKey)
      throws PermissionDeniedException;

  /**
   * Add List of Experiment Tags in database.
   *
   * @param String experimentId, List<String> tagsList
   * @return Experiment updatedExperiment
   */
  Experiment addExperimentTags(String experimentId, List<String> tagsList);

  /**
   * Fetch Experiment Tags from database using experimentId.
   *
   * @param String experimentId
   * @return List<String> projectTags
   */
  List<String> getExperimentTags(String experimentId);

  /**
   * Delete Experiment Tags from Experiment entity.
   *
   * @param tagList
   * @param deleteAll
   * @param String experimentId
   * @return Experiment updatedExperiment
   */
  Experiment deleteExperimentTags(
      String experimentId, List<String> experimentTagList, Boolean deleteAll);

  /**
   * Add Attribute in Experiment Attributes list in database.
   *
   * @param String experimentId
   * @param List<KeyValue> attributes
   * @return Experiment updatedExperiment
   */
  Experiment addExperimentAttributes(String experimentId, List<KeyValue> attributes);

  /**
   * Fetch Experiment Attributes from database using experimentId.
   *
   * @param getAll flag
   * @param attributeKeyList
   * @param String experimentId
   * @return List<KeyValue> experimentAttributes.
   */
  List<KeyValue> getExperimentAttributes(
      String experimentId, List<String> attributeKeyList, Boolean getAll);

  /**
   * Delete Experiment Attributes in database using experimentId.
   *
   * @param deleteAll
   * @param attributeKeyList
   * @param String experimentId
   * @return Experiment experiment
   */
  Experiment deleteExperimentAttributes(
      String experimentId, List<String> attributeKeyList, Boolean deleteAll);

  /**
   * Delete the Experiments from database using experimentIds list.
   *
   * @param experimentIds : list of experimentRunId
   * @return {@link List} : return deleted ids
   */
  List<String> deleteExperiments(List<String> experimentIds) throws PermissionDeniedException;

  /**
   * Return experiment using given key value list. keyValue has key as ModelDBConstants.PROJECT_ID
   * etc. and value as experiment.projectId
   *
   * @param keyValue --> list of KeyValue
   * @return Experiment entity.
   */
  @Deprecated
  // This should be deprecated, there should be no assumption on the length of data in a generic
  // function
  // asserts if required should be at the caller which has more context about the data being queries
  Experiment getExperiment(List<KeyValue> keyValues);

  /**
   * Return experiment using given key value list. keyValue has key as ModelDBConstants.PROJECT_ID
   * etc. and value as experiment.projectId
   *
   * @param keyValue --> list of KeyValue
   * @return Experiment entity.
   */
  List<Experiment> getExperiments(List<KeyValue> keyValues);

  /**
   * Deep copy experiments in database. In current scope we deep copy associated ExperimentRuns
   *
   * @param srcExperiment
   * @param newProject
   * @param newOwner
   * @return
   */
  Experiment deepCopyExperimentForUser(
      Experiment srcExperiment, Project newProject, UserInfo newOwner);

  /**
   * @param experimentId : experiment.id
   * @param updatedCodeVersion : updated experiment code version snapshot from client request
   * @return {@link Experiment} : updated experiment
   */
  Experiment logExperimentCodeVersion(String experimentId, CodeVersion updatedCodeVersion);

  /**
   * Return list of experiments based on FindExperiments queryParameters
   *
   * @param queryParameters : queryParameters --> query parameters for filtering experiments
   * @return ExperimentPaginationDTO : experimentPaginationDTO contains the list of experiments
   *     based on filter queryParameters & total_pages count
   */
  ExperimentPaginationDTO findExperiments(
      ProjectDAO projectDAO, UserInfo userInfo, FindExperiments queryParameters)
      throws PermissionDeniedException;

  /**
   * Experiment has artifacts field. Add new Artifact in that artifacts List.
   *
   * @param experimentId : experiment.id
   * @param artifacts : experiment.artifacts
   * @return {@link Experiment} : updated experiment
   */
  Experiment logArtifacts(String experimentId, List<Artifact> artifacts) throws NotFoundException;

  /**
   * Return List<Artifact> artifacts from Experiment entity.
   *
   * @param experimentId : experiment.id
   * @return {@link List<Artifact>} : artifact list from Experiment
   */
  List<Artifact> getExperimentArtifacts(String experimentId);

  /**
   * Deletes the artifact key associated with the experiment
   *
   * @param experimentId : experiment.id
   * @param artifactKey : artifact.key
   * @return {@link Experiment} : updated experiment
   */
  Experiment deleteArtifacts(String experimentId, String artifactKey);

  /**
   * Get Project Id from Experiment entity using given experimentID.
   *
   * @param experimentIds : list of experiment.id
   * @return {@link Map} : key=experiment.id, value=project.id
   */
  Map<String, String> getProjectIdsByExperimentIds(List<String> experimentIds);
}
