package ai.verta.modeldb.dataset;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.Dataset;
import ai.verta.modeldb.DatasetVisibilityEnum.DatasetVisibility;
import ai.verta.modeldb.FindDatasets;
import ai.verta.modeldb.dto.DatasetPaginationDTO;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.uac.UserInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import java.util.Map;

public interface DatasetDAO {

  /**
   * Create and log a dataset.
   *
   * @param dataset : newDataset
   * @param userInfo : current login user
   * @return {@link Dataset} : dataset
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  Dataset createDataset(Dataset dataset, UserInfo userInfo) throws InvalidProtocolBufferException;

  /**
   * Get datasets matching the IDs
   *
   * @param sharedDatasetIds : dataset id list
   * @return {@link List<Dataset>} : dataset list
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  List<Dataset> getDatasetByIds(List<String> sharedDatasetIds)
      throws InvalidProtocolBufferException;

  /**
   * Fetch all the dataset based on user details and filter parameters.
   *
   * @param userInfo : {@link UserInfo}
   * @param pageNumber : page number use for pagination.
   * @param pageLimit : page limit is per page record count.
   * @param order : this parameter has order like asc OR desc.
   * @param sortKey : Use this field for filter data.
   * @param datasetVisibility : DatasetVisibility.PRIVATE, DatasetVisibility.PUBLIC
   * @return {@link DatasetPaginationDTO} : datasetPaginationDTO contains the experimentRunList &
   *     total_pages count
   * @throws InvalidProtocolBufferException invalidProtocolBufferException
   */
  DatasetPaginationDTO getDatasets(
      UserInfo userInfo,
      Integer pageNumber,
      Integer pageLimit,
      Boolean order,
      String sortKey,
      DatasetVisibility datasetVisibility)
      throws InvalidProtocolBufferException;

  /**
   * Delete the Datasets in database using datasetIds.
   *
   * @param datasetIds : list of dataset.id
   * @return {@link Boolean} : updated status
   */
  Boolean deleteDatasets(List<String> datasetIds) throws InvalidProtocolBufferException;

  /**
   * Set the update time of the datasets with the matching ids
   *
   * @param datasetIds : list of dataset Id
   * @param timestamp : timestamp
   * @return {@link Boolean} : status
   */
  Boolean setUpdateTime(List<String> datasetIds, long timestamp);

  /**
   * Get dataset with the matching ID.
   *
   * @param datasetId : id of the dataset to get
   * @return {@link Dataset} dataset with the matching id
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  Dataset getDatasetById(String datasetId) throws InvalidProtocolBufferException;

  /**
   * Return list of datasets based on FindDatasets queryParameters
   *
   * @param queryParameters : queryParameters --> query parameters for filtering datasets
   * @param userInfo : userInfo
   * @param datasetVisibility : DatasetVisibility.PRIVATE, DatasetVisibility.PUBLIC
   * @return {@link DatasetPaginationDTO} : datasetPaginationDTO contains the list of datasets based
   *     on filter queryParameters & total_pages count
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  DatasetPaginationDTO findDatasets(
      FindDatasets queryParameters, UserInfo userInfo, DatasetVisibility datasetVisibility)
      throws InvalidProtocolBufferException;

  /**
   * Fetch the Dataset based on key and value from database.
   *
   * @param key : key like ModelDBConstants.ID,ModelDBConstants.NAME etc.
   * @param value : value is dataset.Id, dataset.name etc.
   * @param userInfo : current login userInfo
   * @return Dataset dataset : based on search return dataset entity.
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  List<Dataset> getDatasets(String key, String value, UserInfo userInfo)
      throws InvalidProtocolBufferException;

  /**
   * Update dataset name
   *
   * @param datasetId : dataset.id
   * @param datasetName : Dataset.name
   * @return {@link Dataset} : updated Dataset
   * @throws InvalidProtocolBufferException invalidProtocolBufferException
   */
  Dataset updateDatasetName(String datasetId, String datasetName)
      throws InvalidProtocolBufferException;

  /**
   * Update dataset description
   *
   * @param datasetId : dataset.id
   * @param datasetDescription : Dataset.description
   * @return {@link Dataset} : updated Dataset
   * @throws InvalidProtocolBufferException invalidProtocolBufferException
   */
  Dataset updateDatasetDescription(String datasetId, String datasetDescription)
      throws InvalidProtocolBufferException;

  /**
   * Update Dataset Tags in database using datasetId.
   *
   * @param datasetId : dataset.id
   * @param tagsList : List<String> new added tags
   * @return {@link Dataset} Dataset : updated Dataset entity
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  Dataset addDatasetTags(String datasetId, List<String> tagsList)
      throws InvalidProtocolBufferException;

  /**
   * Get dataset tags from database
   *
   * @param datasetId : dataset.id
   * @return {@link List<String>} dataset.tags
   * @throws InvalidProtocolBufferException invalidProtocolBufferException
   */
  List<String> getDatasetTags(String datasetId) throws InvalidProtocolBufferException;

  /**
   * Delete Dataset Tags in database using datasetId.
   *
   * @param datasetTagList : tag list for deletion
   * @param deleteAll : flag for identification of delete all tag
   * @param datasetId : dataset.id
   * @return Dataset : dataset
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  Dataset deleteDatasetTags(String datasetId, List<String> datasetTagList, Boolean deleteAll)
      throws InvalidProtocolBufferException;

  /**
   * Add attributes in database using datasetId
   *
   * @param datasetId : dataset.id
   * @param attributesList : new attribute list
   * @return {@link Dataset} updatedDataset : updated Dataset entity
   * @throws InvalidProtocolBufferException invalidProtocolBufferException
   */
  Dataset addDatasetAttributes(String datasetId, List<KeyValue> attributesList)
      throws InvalidProtocolBufferException;

  /**
   * Update Dataset Attributes in database using datasetId.
   *
   * <p>updatedCount success updated response from database. if there is no any object update then
   * its return zero. If updated new data is same as old data then it also return zero.
   *
   * @param datasetId : dataset.id
   * @param attribute : attribute for update
   * @return {@link Dataset} updatedDataset : updated Dataset entity
   * @throws InvalidProtocolBufferException invalidProtocolBufferException
   */
  Dataset updateDatasetAttributes(String datasetId, KeyValue attribute)
      throws InvalidProtocolBufferException;

  /**
   * Fetch Dataset Attributes from database using datasetId.
   *
   * @param datasetId : dataset.id
   * @param attributeKeyList : attribute keys
   * @param getAll : flag
   * @return {@link List<KeyValue>} datasetAttributes
   * @throws InvalidProtocolBufferException invalidProtocolBufferException
   */
  List<KeyValue> getDatasetAttributes(
      String datasetId, List<String> attributeKeyList, Boolean getAll)
      throws InvalidProtocolBufferException;

  /**
   * Delete Dataset Attributes in database using datasetId.
   *
   * @param datasetId : dataset.id
   * @param attributeKeyList : attribute keys
   * @param deleteAll : flag
   * @return {@link Dataset} updatedDataset
   * @throws InvalidProtocolBufferException invalidProtocolBufferException
   */
  Dataset deleteDatasetAttributes(
      String datasetId, List<String> attributeKeyList, Boolean deleteAll)
      throws InvalidProtocolBufferException;

  /**
   * Set/Update dataset visibility
   *
   * @param datasetId : dataset.id
   * @param datasetVisibility : DatasetVisibility.PUBLIC, DatasetVisibility.PRIVATE
   * @return {@link Dataset} : updated Dataset
   * @throws InvalidProtocolBufferException invalidProtocolBufferException
   */
  Dataset setDatasetVisibility(String datasetId, DatasetVisibility datasetVisibility)
      throws InvalidProtocolBufferException;

  /**
   * Getting all the owners with respected to dataset ids and returned by this method.
   *
   * @param datasetIds : List<String> list of accessible dataset Id
   * @return {@link Map} : Map<String,String> where key= datasetId, value= dataset owner Id
   */
  Map<String, String> getOwnersByDatasetIds(List<String> datasetIds);

  /**
   * Sets the workspace of an existing dataset
   *
   * @param datasetId : datasetId
   * @param workspaceDTO : workspaceDTO
   * @return {@link Dataset} : updated dataset
   * @throws InvalidProtocolBufferException : InvalidProtocolBufferException
   */
  Dataset setDatasetWorkspace(String datasetId, WorkspaceDTO workspaceDTO)
      throws InvalidProtocolBufferException;
}
