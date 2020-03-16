package ai.verta.modeldb.datasetVersion;

import ai.verta.modeldb.App;
import ai.verta.modeldb.CreateDatasetVersion;
import ai.verta.modeldb.Dataset;
import ai.verta.modeldb.DatasetPartInfo;
import ai.verta.modeldb.DatasetVersion;
import ai.verta.modeldb.DatasetVisibilityEnum.DatasetVisibility;
import ai.verta.modeldb.FindDatasetVersions;
import ai.verta.modeldb.KeyValue;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.PathDatasetVersionInfo;
import ai.verta.modeldb.QueryDatasetVersionInfo;
import ai.verta.modeldb.RawDatasetVersionInfo;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.dto.DatasetVersionDTO;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.uac.UserInfo;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public interface DatasetVersionDAO {

  final Logger LOGGER = LogManager.getLogger(DatasetVersionDAO.class);

  /**
   * Create and log a dataset version.
   *
   * @param datasetVersion : new datasetVersion
   * @return {@link DatasetVersion} : DatasetVersion
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  DatasetVersion createDatasetVersion(
      CreateDatasetVersion request, Dataset dataset, UserInfo userInfo)
      throws InvalidProtocolBufferException;

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
      String datasetId,
      int pageNumber,
      int pageLimit,
      boolean isAscending,
      String sortKey,
      UserInfo currentLoginUser)
      throws InvalidProtocolBufferException;

  /**
   * Delete datasetVersion with ids matching the ids in request
   *
   * @param datasetVersionIds : list of datasetVersion.id
   * @return {@link Boolean} : status
   */
  Boolean deleteDatasetVersions(List<String> datasetVersionIds, Boolean parentExists);

  /**
   * Delete all datasetVersions with dataset ids matching the ids in request.
   *
   * @param datasetIds : list of dataset.id
   * @param parentExists : If datasetVersion of parent dataset exist then set true
   * @return {@link Boolean} : status
   */
  Boolean deleteDatasetVersionsByDatasetIDs(List<String> datasetIds, Boolean parentExists);

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
   * @param queryParameters : queryParameters --> query parameters for filtering datasetVersions
   * @param userInfo : userInfo
   * @return {@link DatasetVersionDTO} : datasetVersionDTO contains the list of datasetVersions
   *     based on filter queryParameters & total_pages count
   * @throws InvalidProtocolBufferException InvalidProtocolBufferException
   */
  DatasetVersionDTO findDatasetVersions(FindDatasetVersions queryParameters, UserInfo userInfo)
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
   * Get datasetVersion tags from database
   *
   * @param datasetVersionId : datasetVersion.id
   * @return {@link List<String>} datasetVersion.tags
   * @throws InvalidProtocolBufferException invalidProtocolBufferException
   */
  List<String> getDatasetVersionTags(String datasetVersionId) throws InvalidProtocolBufferException;

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

  default List<DatasetVersion> getDatasetVersionFromRequest(
      AuthService authService,
      CreateDatasetVersion request,
      UserInfo userInfo,
      DatasetVersion existingDatasetVersion)
      throws InvalidProtocolBufferException {

    long version = 0L;
    DatasetVersion parentDatasetVersion = null;
    if (request.getParentId().isEmpty() && existingDatasetVersion != null) {
      parentDatasetVersion = existingDatasetVersion;
    } else if (!request.getParentId().isEmpty()) {
      parentDatasetVersion = getDatasetVersion(request.getParentId());
    }

    if (parentDatasetVersion != null) {
      version = parentDatasetVersion.getVersion();
      switch (request.getDatasetType()) {
        case PATH:
          PathDatasetVersionInfo requestedPathDatasetVersionInfo =
              request.getPathDatasetVersionInfo();
          PathDatasetVersionInfo parentPathDatasetVersionInfo =
              parentDatasetVersion.getPathDatasetVersionInfo();
          // ERROR: if the location_type is different from the parent
          if (!requestedPathDatasetVersionInfo
              .getLocationType()
              .equals(parentPathDatasetVersionInfo.getLocationType())) {
            ModelDBUtils.logAndThrowError(
                ModelDBMessages.LOCATION_TYPE_NOT_MATCH_OF_PATH_DATASET_VERSION_INFO,
                Code.INVALID_ARGUMENT_VALUE,
                Any.pack(CreateDatasetVersion.Response.getDefaultInstance()));
          }

          // base_path is the same?
          // Length of dataset_path_infos is the same?
          if (requestedPathDatasetVersionInfo
                  .getBasePath()
                  .equals(parentPathDatasetVersionInfo.getBasePath())
              && requestedPathDatasetVersionInfo.getDatasetPartInfosCount()
                  == parentPathDatasetVersionInfo.getDatasetPartInfosCount()) {
            List<DatasetPartInfo> matchedDatasetPartInfo = new ArrayList<>();
            for (DatasetPartInfo datasetPartInfo :
                requestedPathDatasetVersionInfo.getDatasetPartInfosList()) {
              for (DatasetPartInfo parentDatasetPartInfo :
                  parentPathDatasetVersionInfo.getDatasetPartInfosList()) {
                // Contents of dataset_path_infos are the same?, order not important
                if (datasetPartInfo.getPath().equals(parentDatasetPartInfo.getPath())
                    && datasetPartInfo.getSize() == parentDatasetPartInfo.getSize()
                    && datasetPartInfo.getChecksum().equals(parentDatasetPartInfo.getChecksum())) {
                  matchedDatasetPartInfo.add(datasetPartInfo);
                }
              }
            }
            if (matchedDatasetPartInfo.size()
                == requestedPathDatasetVersionInfo.getDatasetPartInfosCount()) { // All match
              return Collections.singletonList(parentDatasetVersion);
            }
          }
          break;
        case QUERY:
          QueryDatasetVersionInfo requestedQueryDatasetVersionInfo =
              request.getQueryDatasetVersionInfo();
          QueryDatasetVersionInfo parentQueryDatasetVersionInfo =
              parentDatasetVersion.getQueryDatasetVersionInfo();
          if (requestedQueryDatasetVersionInfo
                  .getQuery()
                  .equals(parentQueryDatasetVersionInfo.getQuery())
              && requestedQueryDatasetVersionInfo.getExecutionTimestamp()
                  == parentQueryDatasetVersionInfo.getExecutionTimestamp()
              && requestedQueryDatasetVersionInfo
                  .getDataSourceUri()
                  .equals(parentQueryDatasetVersionInfo.getDataSourceUri())
              && requestedQueryDatasetVersionInfo.getNumRecords()
                  == parentQueryDatasetVersionInfo.getNumRecords()) {
            return Collections.singletonList(parentDatasetVersion);
          }
          break;
        case RAW:
          RawDatasetVersionInfo requestedRawDatasetVersionInfo = request.getRawDatasetVersionInfo();
          RawDatasetVersionInfo parentRawDatasetVersionInfo =
              parentDatasetVersion.getRawDatasetVersionInfo();
          if (requestedRawDatasetVersionInfo.equals(parentRawDatasetVersionInfo)) {
            return Collections.singletonList(parentDatasetVersion);
          }
          break;
        case UNRECOGNIZED:
        default:
          LOGGER.warn(ModelDBMessages.INVALID_DATSET_TYPE);
          Status status =
              Status.newBuilder()
                  .setCode(Code.INVALID_ARGUMENT_VALUE)
                  .setMessage(ModelDBMessages.INVALID_DATSET_TYPE)
                  .build();
          throw StatusProto.toStatusRuntimeException(status);
      }
    }

    DatasetVersion.Builder datasetVersionBuilder =
        DatasetVersion.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setDatasetId(request.getDatasetId())
            .setDescription(request.getDescription())
            .addAllTags(request.getTagsList())
            .setDatasetVersionVisibility(request.getDatasetVersionVisibility())
            .addAllAttributes(request.getAttributesList())
            .setVersion(version + 1L)
            .setDatasetType(request.getDatasetType());

    if (App.getInstance().getStoreClientCreationTimestamp() && request.getTimeCreated() != 0L) {
      datasetVersionBuilder
          .setTimeLogged(request.getTimeCreated())
          .setTimeUpdated(request.getTimeCreated());
    } else {
      datasetVersionBuilder
          .setTimeLogged(Calendar.getInstance().getTimeInMillis())
          .setTimeUpdated(Calendar.getInstance().getTimeInMillis());
    }

    if (parentDatasetVersion != null) {
      datasetVersionBuilder.setParentId(parentDatasetVersion.getId());
    }

    if (userInfo != null) {
      datasetVersionBuilder.setOwner(authService.getVertaIdFromUserInfo(userInfo));
    }

    switch (request.getDatasetType()) {
      case PATH:
        datasetVersionBuilder.setPathDatasetVersionInfo(request.getPathDatasetVersionInfo());
        break;
      case QUERY:
        datasetVersionBuilder.setQueryDatasetVersionInfo(request.getQueryDatasetVersionInfo());
        break;
      case RAW:
        // TODO: need to process this data?
        datasetVersionBuilder.setRawDatasetVersionInfo(request.getRawDatasetVersionInfo());
        break;
      case UNRECOGNIZED:
      default:
        LOGGER.warn(ModelDBMessages.INVALID_DATSET_TYPE);
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(ModelDBMessages.INVALID_DATSET_TYPE)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
    }

    List<DatasetVersion> datasetVersionList = new ArrayList<>();
    datasetVersionList.add(parentDatasetVersion);
    datasetVersionList.add(datasetVersionBuilder.build());
    return datasetVersionList;
  }
}
