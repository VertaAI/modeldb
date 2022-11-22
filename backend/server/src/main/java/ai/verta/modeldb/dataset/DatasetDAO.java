package ai.verta.modeldb.dataset;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.Dataset;
import ai.verta.modeldb.FindDatasets;
import ai.verta.modeldb.dto.DatasetPaginationDTO;
import ai.verta.modeldb.entities.DatasetEntity;
import ai.verta.uac.ResourceVisibility;
import ai.verta.uac.UserInfo;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;

public interface DatasetDAO {

  /**
   * Create and log a dataset.
   *
   * @param dataset : newDataset
   * @param userInfo : current login user
   * @return {@link Dataset} : dataset
   */
  Dataset createDataset(Dataset dataset, UserInfo userInfo);

  /**
   * Get datasets matching the IDs
   *
   * @param sharedDatasetIds : dataset id list
   * @return {@link List<Dataset>} : dataset list
   */
  List<Dataset> getDatasetByIds(List<String> sharedDatasetIds);

  /**
   * Fetch all the dataset based on user details and filter parameters.
   *
   * @param userInfo : {@link UserInfo}
   * @param pageNumber : page number use for pagination.
   * @param pageLimit : page limit is per page record count.
   * @param order : this parameter has order like asc OR desc.
   * @param sortKey : Use this field for filter data.
   * @param datasetVisibility : ResourceVisibility.PRIVATE, ResourceVisibility.PUBLIC
   * @return {@link DatasetPaginationDTO} : datasetPaginationDTO contains the experimentRunList &
   *     total_pages count
   */
  DatasetPaginationDTO getDatasets(
      UserInfo userInfo,
      Integer pageNumber,
      Integer pageLimit,
      Boolean order,
      String sortKey,
      ResourceVisibility datasetVisibility);

  /**
   * Delete the Datasets in database using datasetIds.
   *
   * @param datasetIds : list of dataset.id
   * @return {@link Boolean} : updated status
   */
  Boolean deleteDatasets(List<String> datasetIds);

  /**
   * Get dataset with the matching ID.
   *
   * @param datasetId : id of the dataset to get
   * @return {@link Dataset} dataset with the matching id
   */
  Dataset getDatasetById(String datasetId);

  DatasetEntity getDatasetEntity(Session session, String datasetId);

  /**
   * Return list of datasets based on FindDatasets queryParameters
   *
   * @param queryParameters : queryParameters --> query parameters for filtering datasets
   * @param userInfo : userInfo
   * @param resourceVisibility : ResourceVisibility.PRIVATE, ResourceVisibility.PUBLIC
   * @return {@link DatasetPaginationDTO} : datasetPaginationDTO contains the list of datasets based
   *     on filter queryParameters & total_pages count
   */
  DatasetPaginationDTO findDatasets(
      FindDatasets queryParameters, UserInfo userInfo, ResourceVisibility resourceVisibility);

  /**
   * Fetch the Dataset based on key and value from database.
   *
   * @param key : key like ModelDBConstants.ID,ModelDBConstants.NAME etc.
   * @param value : value is dataset.Id, dataset.name etc.
   * @param userInfo : current login userInfo
   * @return Dataset dataset : based on search return dataset entity.
   */
  List<Dataset> getDatasets(String key, String value, UserInfo userInfo);

  /**
   * Update dataset name
   *
   * @param datasetId : dataset.id
   * @param datasetName : Dataset.name
   * @return {@link Dataset} : updated Dataset
   */
  Dataset updateDatasetName(String datasetId, String datasetName);

  /**
   * Update dataset description
   *
   * @param datasetId : dataset.id
   * @param datasetDescription : Dataset.description
   * @return {@link Dataset} : updated Dataset
   */
  Dataset updateDatasetDescription(String datasetId, String datasetDescription);

  /**
   * Update Dataset Tags in database using datasetId.
   *
   * @param datasetId : dataset.id
   * @param tagsList : List<String> new added tags
   * @return {@link Dataset} Dataset : updated Dataset entity
   */
  Dataset addDatasetTags(String datasetId, List<String> tagsList);

  /**
   * Get dataset tags from database
   *
   * @param datasetId : dataset.id
   * @return {@link List<String>} dataset.tags
   */
  List<String> getDatasetTags(String datasetId);

  /**
   * Delete Dataset Tags in database using datasetId.
   *
   * @param datasetTagList : tag list for deletion
   * @param deleteAll : flag for identification of delete all tag
   * @param datasetId : dataset.id
   * @return Dataset : dataset
   */
  Dataset deleteDatasetTags(String datasetId, List<String> datasetTagList, Boolean deleteAll);

  /**
   * Add attributes in database using datasetId
   *
   * @param datasetId : dataset.id
   * @param attributesList : new attribute list
   * @return {@link Dataset} updatedDataset : updated Dataset entity
   */
  Dataset addDatasetAttributes(String datasetId, List<KeyValue> attributesList);

  /**
   * Update Dataset Attributes in database using datasetId.
   *
   * <p>updatedCount success updated response from database. if there is no any object update then
   * its return zero. If updated new data is same as old data then it also return zero.
   *
   * @param datasetId : dataset.id
   * @param attribute : attribute for update
   * @return {@link Dataset} updatedDataset : updated Dataset entity
   */
  Dataset updateDatasetAttributes(String datasetId, KeyValue attribute);

  /**
   * Fetch Dataset Attributes from database using datasetId.
   *
   * @param datasetId : dataset.id
   * @param attributeKeyList : attribute keys
   * @param getAll : flag
   * @return {@link List<KeyValue>} datasetAttributes
   */
  List<KeyValue> getDatasetAttributes(
      String datasetId, List<String> attributeKeyList, Boolean getAll);

  /**
   * Delete Dataset Attributes in database using datasetId.
   *
   * @param datasetId : dataset.id
   * @param attributeKeyList : attribute keys
   * @param deleteAll : flag
   * @return {@link Dataset} updatedDataset
   */
  Dataset deleteDatasetAttributes(
      String datasetId, List<String> attributeKeyList, Boolean deleteAll);

  /**
   * Getting all the owners with respected to dataset ids and returned by this method.
   *
   * @param datasetIds : List<String> list of accessible dataset Id
   * @return {@link Map} : Map<String,String> where key= datasetId, value= dataset owner Id
   */
  Map<String, String> getOwnersByDatasetIds(List<String> datasetIds);

  List<String> getWorkspaceDatasetIDs(String workspaceName, UserInfo currentLoginUserInfo);

  /**
   * Checks if dataset with the id exists with delete flag false
   *
   * @param datasetId
   * @return
   */
  boolean datasetExistsInDB(String datasetId);
}
