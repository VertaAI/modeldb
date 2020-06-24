package ai.verta.modeldb.datasetVersion;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.App;
import ai.verta.modeldb.CreateDatasetVersion;
import ai.verta.modeldb.DatasetVersion;
import ai.verta.modeldb.DatasetVisibilityEnum.DatasetVisibility;
import ai.verta.modeldb.FindDatasetVersions;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.dataset.DatasetDAO;
import ai.verta.modeldb.dto.DatasetVersionDTO;
import ai.verta.uac.UserInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Status;
import java.util.Calendar;
import java.util.List;
import org.hibernate.Session;

public interface DatasetVersionDAO {

  /**
   * Paginated endpoint to get all dataset versions logged under a dataset with id matching the id
   * in request.
   *
   * @param datasetId : dataset.id
   * @param pageNumber : page number
   * @param pageLimit : limit of records per page
   * @param isAscending : asc=true / desc=false
   * @param sortKey : sory key like dataset.name, dataset.attribute etc.
   * @param currentLoginUser: current login user
   * @return {@link DatasetVersionDTO} : DatasetVersionDTO
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  DatasetVersionDTO getDatasetVersions(
      DatasetDAO datasetDAO,
      String datasetId,
      int pageNumber,
      int pageLimit,
      boolean isAscending,
      String sortKey,
      UserInfo currentLoginUser)
      throws InvalidProtocolBufferException;

  /**
   * Delete all datasetVersions with dataset ids matching the ids in request.
   *
   * @param datasetIds : list of dataset.id
   * @return {@link Boolean} : status
   */
  Boolean deleteDatasetVersionsByDatasetIDs(List<String> datasetIds);

  /**
   * Fetch the datasetVersion with id matching the id in request
   *
   * @param datasetVersionId : datasetVersion.id
   * @return {@link DatasetVersion} : datasetVersion object
   */
  DatasetVersion getDatasetVersion(String datasetVersionId) throws InvalidProtocolBufferException;

  /**
   * Get a path for generating a pre-signed URL for a datasetVersion of type {@link RawDatasetInfo}
   *
   * @param datasetVersionId : datasetVersion.id
   * @param method :
   * @return {@link String} : datasetVersion url
   */
  String getUrlForDatasetVersion(String datasetVersionId, String method)
      throws InvalidProtocolBufferException;

  /**
   * @param datasetVersionIds : list of datasetVersion ids
   * @return : datasetVersion list
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  List<DatasetVersion> getDatasetVersionsByBatchIds(List<String> datasetVersionIds)
      throws InvalidProtocolBufferException;

  /**
   * Return list of datasetVersions based on FindDatasetVersions queryParameters
   *
   * @param datasetDAO : dataset entity dao
   * @param queryParameters : queryParameters --> query parameters for filtering datasetVersions
   * @param userInfo : userInfo
   * @return {@link DatasetVersionDTO} : datasetVersionDTO contains the list of datasetVersions
   *     based on filter queryParameters & total_pages count
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  DatasetVersionDTO findDatasetVersions(
      DatasetDAO datasetDAO, FindDatasetVersions queryParameters, UserInfo userInfo)
      throws InvalidProtocolBufferException;

  /**
   * Update DatasetVersion description in database using datasetVersionId.
   *
   * @param datasetVersionId : datasetVersion.id
   * @param datasetVersionDescription : Updated datasetVersion description
   * @return {@link DatasetVersion} : DatasetVersion updated DatasetVersion entity
   */
  DatasetVersion updateDatasetVersionDescription(
      String datasetVersionId, String datasetVersionDescription)
      throws InvalidProtocolBufferException;

  /**
   * Update DatasetVersion Tags in database using datasetVersionId.
   *
   * @param datasetVersionId : datasetVersion.id
   * @param tagsList : List<String> new added tags
   * @return {@link DatasetVersion} DatasetVersion : updated DatasetVersion entity
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  DatasetVersion addDatasetVersionTags(String datasetVersionId, List<String> tagsList)
      throws InvalidProtocolBufferException;

  /**
   * Delete DatasetVersion Tags in database using datasetVersionId.
   *
   * @param datasetVersionTagList : tag list for deletion
   * @param deleteAll : flag for identification of delete all tag
   * @param datasetVersionId : datasetVersion.id
   * @return DatasetVersion : datasetVersion
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  DatasetVersion deleteDatasetVersionTags(
      String datasetVersionId, List<String> datasetVersionTagList, Boolean deleteAll)
      throws InvalidProtocolBufferException;

  /**
   * Add attributes in database using datasetVersionId
   *
   * @param datasetVersionId : datasetVersion.id
   * @param attributesList : new attribute list
   * @return {@link DatasetVersion} updatedDatasetVersion : updated DatasetVersion entity
   * @throws InvalidProtocolBufferException invalidProtocolBufferException
   */
  DatasetVersion addDatasetVersionAttributes(String datasetVersionId, List<KeyValue> attributesList)
      throws InvalidProtocolBufferException;

  /**
   * Update DatasetVersion Attributes in database using datasetVersionId.
   *
   * <p>updatedCount success updated response from database. if there is no any object update then
   * its return zero. If updated new data is same as old data then it also return zero.
   *
   * @param datasetVersionId : datasetVersion.id
   * @param attribute : attribute for update
   * @return {@link DatasetVersion} updatedDatasetVersion : updated DatasetVersion entity
   * @throws InvalidProtocolBufferException invalidProtocolBufferException
   */
  DatasetVersion updateDatasetVersionAttributes(String datasetVersionId, KeyValue attribute)
      throws InvalidProtocolBufferException;

  /**
   * Fetch DatasetVersion Attributes from database using datasetVersionId.
   *
   * @param datasetVersionId : datasetVersion.id
   * @param attributeKeyList : attribute keys
   * @param getAll : flag
   * @return {@link List<KeyValue>} datasetVersionAttributes
   * @throws InvalidProtocolBufferException invalidProtocolBufferException
   */
  List<KeyValue> getDatasetVersionAttributes(
      String datasetVersionId, List<String> attributeKeyList, Boolean getAll)
      throws InvalidProtocolBufferException;

  /**
   * Delete DatasetVersion Attributes in database using datasetVersionId.
   *
   * @param datasetVersionId : datasetVersion.id
   * @param attributeKeyList : attribute keys
   * @param deleteAll : flag
   * @return {@link DatasetVersion} updatedDatasetVersion
   * @throws InvalidProtocolBufferException invalidProtocolBufferException
   */
  DatasetVersion deleteDatasetVersionAttributes(
      String datasetVersionId, List<String> attributeKeyList, Boolean deleteAll)
      throws InvalidProtocolBufferException;

  /**
   * Set/Update datasetVersion visibility
   *
   * @param datasetVersionId : datasetVersion.id
   * @param datasetVersionVisibility : DatasetVisibility.PUBLIC, DatasetVisibility.PRIVATE
   * @return {@link DatasetVersion} : updated DatasetVersion
   * @throws InvalidProtocolBufferException invalidProtocolBufferException
   */
  DatasetVersion setDatasetVersionVisibility(
      String datasetVersionId, DatasetVisibility datasetVersionVisibility)
      throws InvalidProtocolBufferException;

  default DatasetVersion getDatasetVersionFromRequest(
      AuthService authService, CreateDatasetVersion request, UserInfo userInfo)
      throws ModelDBException {
    DatasetVersion.Builder datasetVersionBuilder =
        DatasetVersion.newBuilder()
            .setDatasetId(request.getDatasetId())
            .setDescription(request.getDescription())
            .addAllTags(request.getTagsList())
            .setDatasetVersionVisibility(request.getDatasetVersionVisibility())
            .addAllAttributes(request.getAttributesList());

    if (App.getInstance().getStoreClientCreationTimestamp() && request.getTimeCreated() != 0L) {
      datasetVersionBuilder.setTimeLogged(request.getTimeCreated());
      datasetVersionBuilder.setTimeUpdated(request.getTimeCreated());
    } else {
      datasetVersionBuilder.setTimeLogged(Calendar.getInstance().getTimeInMillis());
      datasetVersionBuilder.setTimeUpdated(Calendar.getInstance().getTimeInMillis());
    }

    if (!request.getParentId().isEmpty()) {
      datasetVersionBuilder.setParentId(request.getParentId());
    }

    if (userInfo != null) {
      datasetVersionBuilder.setOwner(authService.getVertaIdFromUserInfo(userInfo));
    }

    if (!request.hasPathDatasetVersionInfo()) {
      throw new ModelDBException("Not supported", Status.Code.UNIMPLEMENTED);
    }
    datasetVersionBuilder.setPathDatasetVersionInfo(request.getPathDatasetVersionInfo());

    return datasetVersionBuilder.build();
  }

  boolean isDatasetVersionExists(Session session, String id);
}
