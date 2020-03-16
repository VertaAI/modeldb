package ai.verta.modeldb.datasetVersion;

import ai.verta.modeldb.App;
import ai.verta.modeldb.CreateDatasetVersion;
import ai.verta.modeldb.Dataset;
import ai.verta.modeldb.DatasetTypeEnum;
import ai.verta.modeldb.DatasetVersion;
import ai.verta.modeldb.DatasetVisibilityEnum;
import ai.verta.modeldb.FindDatasetVersions;
import ai.verta.modeldb.KeyValue;
import ai.verta.modeldb.KeyValueQuery;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.OperatorEnum;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.dto.DatasetVersionDTO;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.DatasetVersionEntity;
import ai.verta.modeldb.entities.TagsMapping;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.ModelResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.uac.UserInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class DatasetVersionDAORdbImpl implements DatasetVersionDAO {

  private static final Logger LOGGER = LogManager.getLogger(DatasetVersionDAORdbImpl.class);
  private final AuthService authService;
  private final RoleService roleService;
  private final ReentrantLock createDatasetVersionLock = new ReentrantLock();

  private static final String DATASET_VERSION_EXISTS_QUERY =
      new StringBuilder("Select count(*) From DatasetVersionEntity dsv where dsv.")
          .append(ModelDBConstants.DATASET_ID)
          .append(" = :datasetId AND dsv.")
          .append(ModelDBConstants.VERSION)
          .append(" =:version")
          .toString();
  private static final String DATASET_VERSION_BY_DATA_SET_IDS_QUERY =
      "From DatasetVersionEntity ds where ds.dataset_id IN (:datasetIds) ";
  private static final String DATASET_VERSION_BY_IDS_QUERY =
      "From DatasetVersionEntity ds where ds.id IN (:ids)";
  private static final String DELETE_DATASET_VERSION_QUERY_PREFIX =
      new StringBuilder("delete from TagsMapping tm WHERE ")
          .append(" tm.datasetVersionEntity." + ModelDBConstants.ID + " = :datasetVersionId ")
          .toString();
  private static final String GET_KEY_VALUE_DATASET_VERSION_QUERY =
      new StringBuilder("From AttributeEntity attr where attr.")
          .append(ModelDBConstants.KEY)
          .append(" in (:keys) AND attr.datasetVersionEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :datasetVersionId AND attr.field_type = :fieldType")
          .toString();

  private static final String DELETE_KEY_VALUE_DATASET_VERSION_QUERY_PREFIX =
      new StringBuilder("delete from AttributeEntity attr WHERE attr.datasetVersionEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :datasetVersionId")
          .toString();
  private static final String DATASET_UPDATE_TIME_QUERY =
      new StringBuilder("UPDATE DatasetEntity ds SET ds.")
          .append(ModelDBConstants.TIME_UPDATED)
          .append(" = :timestamp where ds.")
          .append(ModelDBConstants.ID)
          .append(" IN (:ids) ")
          .toString();

  public DatasetVersionDAORdbImpl(AuthService authService, RoleService roleService) {
    this.authService = authService;
    this.roleService = roleService;
  }

  private boolean checkDatasetVersionAlreadyExist(
      Session session, DatasetVersion newDatasetVersion) {
    Query query = session.createQuery(DATASET_VERSION_EXISTS_QUERY);
    query.setParameter("datasetId", newDatasetVersion.getDatasetId());
    query.setParameter("version", newDatasetVersion.getVersion());
    Long count = (Long) query.uniqueResult();
    return count > 0;
  }

  public void setDatasetUpdateTime(Session session, List<String> datasetIds) {
    Query query = session.createQuery(DATASET_UPDATE_TIME_QUERY);
    query.setParameter("timestamp", Calendar.getInstance().getTimeInMillis());
    query.setParameterList("ids", datasetIds);
    int result = query.executeUpdate();
  }

  @Override
  public DatasetVersion createDatasetVersion(
      CreateDatasetVersion request, Dataset dataset, UserInfo userInfo)
      throws InvalidProtocolBufferException {

    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      createDatasetVersionLock.lock();
      Transaction transaction = session.beginTransaction();

      String lastDatasetVersionQueryStr =
          DATASET_VERSION_BY_DATA_SET_IDS_QUERY + " ORDER BY ds.version DESC";
      Query lastDatasetVersionQuery = session.createQuery(lastDatasetVersionQueryStr);
      lastDatasetVersionQuery.setParameterList(
          "datasetIds", Collections.singletonList(dataset.getId()));
      lastDatasetVersionQuery.setFirstResult(0);
      lastDatasetVersionQuery.setMaxResults(1);
      List<DatasetVersionEntity> datasetVersionEntities = lastDatasetVersionQuery.list();
      DatasetVersion existingDatasetVersion =
          datasetVersionEntities.size() > 0 ? datasetVersionEntities.get(0).getProtoObject() : null;

      List<DatasetVersion> datasetVersionList =
          getDatasetVersionFromRequest(authService, request, userInfo, existingDatasetVersion);

      if (datasetVersionList.size() == 1) {
        transaction.commit();
        return datasetVersionList.get(0);
      } else {
        DatasetVersion datasetVersion = datasetVersionList.get(1);
        if (checkDatasetVersionAlreadyExist(session, datasetVersion)) {
          Status status =
              Status.newBuilder()
                  .setCode(Code.ALREADY_EXISTS_VALUE)
                  .setMessage(
                      "Dataset Version being logged already exists. existing datasetVersion : "
                          + datasetVersion.getVersion()
                          + " in dataset "
                          + datasetVersion.getDatasetId())
                  .build();
          throw StatusProto.toStatusRuntimeException(status);
        }

        DatasetVersionEntity datasetVersionEntity =
            RdbmsUtils.generateDatasetVersionEntity(datasetVersion);
        session.save(datasetVersionEntity);
        setDatasetUpdateTime(session, Collections.singletonList(datasetVersion.getDatasetId()));
        transaction.commit();
        LOGGER.debug("DatasetVersion created successfully");
        return datasetVersionEntity.getProtoObject();
      }
    } finally {
      createDatasetVersionLock.unlock();
    }
  }

  @Override
  public DatasetVersionDTO getDatasetVersions(
      String datasetId,
      int pageNumber,
      int pageLimit,
      boolean isAscending,
      String sortKey,
      UserInfo currentLoginUser)
      throws InvalidProtocolBufferException {
    FindDatasetVersions findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .setDatasetId(datasetId)
            .setPageNumber(pageNumber)
            .setPageLimit(pageLimit)
            .setAscending(isAscending)
            .setSortKey(sortKey)
            .build();
    return findDatasetVersions(findDatasetVersions, currentLoginUser);
  }

  @Override
  public Boolean deleteDatasetVersions(List<String> datasetVersionIds, Boolean parentExists) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      List<String> datasetIds = new ArrayList<>();
      for (String datasetVersionId : datasetVersionIds) {
        DatasetVersionEntity datasetVersionObj =
            session.load(DatasetVersionEntity.class, datasetVersionId);
        datasetIds.add(datasetVersionObj.getDataset_id());
        session.delete(datasetVersionObj);
      }
      setDatasetUpdateTime(session, datasetIds);
      transaction.commit();
      LOGGER.debug("DatasetVersion deleted successfully");
      return true;
    }
  }

  @Override
  public Boolean deleteDatasetVersionsByDatasetIDs(List<String> datasetIds, Boolean parentExists) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      Query query = session.createQuery(DATASET_VERSION_BY_DATA_SET_IDS_QUERY);
      query.setParameterList("datasetIds", datasetIds);
      List<DatasetVersionEntity> datasetVersionEntities = query.list();
      for (DatasetVersionEntity datasetVersionEntity : datasetVersionEntities) {
        session.delete(datasetVersionEntity);
      }
      if (parentExists) {
        setDatasetUpdateTime(session, datasetIds);
      }
      transaction.commit();
      LOGGER.debug("DatasetVersion deleted successfully");
      return true;
    }
  }

  @Override
  public DatasetVersion getDatasetVersion(String datasetVersionId)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetVersionEntity datasetVersionObj =
          session.get(DatasetVersionEntity.class, datasetVersionId);
      if (datasetVersionObj == null) {
        LOGGER.warn(ModelDBMessages.DATA_VERSION_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.DATA_VERSION_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      LOGGER.debug("DatasetVersion getting successfully");
      return datasetVersionObj.getProtoObject();
    }
  }

  @Override
  public String getUrlForDatasetVersion(String datasetVersionId, String method)
      throws InvalidProtocolBufferException {
    DatasetVersion datasetVersion = getDatasetVersion(datasetVersionId);
    if (!datasetVersion.getDatasetType().equals(DatasetTypeEnum.DatasetType.RAW)) {
      Status status =
          Status.newBuilder()
              .setCode(Code.INVALID_ARGUMENT_VALUE)
              .setMessage("getUrl for dataset currently supported only for Raw dataset versions.")
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }

    return datasetVersion.getRawDatasetVersionInfo().getObjectPath();
  }

  @Override
  public List<DatasetVersion> getDatasetVersionsByBatchIds(List<String> datasetVersionIds)
      throws InvalidProtocolBufferException {
    if (datasetVersionIds == null || datasetVersionIds.isEmpty()) {
      LOGGER.info("Input datasetVersionIds is empty");
      return new ArrayList<>();
    }
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      Query query = session.createQuery(DATASET_VERSION_BY_IDS_QUERY);
      query.setParameterList("ids", datasetVersionIds);

      @SuppressWarnings("unchecked")
      List<DatasetVersionEntity> datasetEntities = query.list();
      transaction.commit();
      LOGGER.debug("DatasetVersion by Ids getting successfully");
      return RdbmsUtils.convertDatasetVersionsFromDatasetVersionEntityList(datasetEntities);
    }
  }

  @Override
  public DatasetVersionDTO findDatasetVersions(
      FindDatasetVersions queryParameters, UserInfo userInfo)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {

      CriteriaBuilder builder = session.getCriteriaBuilder();
      // Using FROM and JOIN
      CriteriaQuery<DatasetVersionEntity> criteriaQuery =
          builder.createQuery(DatasetVersionEntity.class);
      Root<DatasetVersionEntity> datasetVersionRoot =
          criteriaQuery.from(DatasetVersionEntity.class);
      datasetVersionRoot.alias("ds");
      List<Predicate> finalPredicatesList = new ArrayList<>();

      if (!queryParameters.getDatasetId().isEmpty()) {
        Expression<String> exp = datasetVersionRoot.get(ModelDBConstants.DATASET_ID);
        Predicate predicate1 = builder.equal(exp, queryParameters.getDatasetId());
        finalPredicatesList.add(predicate1);
      }

      if (!queryParameters.getDatasetVersionIdsList().isEmpty()) {
        Expression<String> exp = datasetVersionRoot.get(ModelDBConstants.ID);
        Predicate predicate2 = exp.in(queryParameters.getDatasetVersionIdsList());
        finalPredicatesList.add(predicate2);
      }

      List<KeyValueQuery> predicates = queryParameters.getPredicatesList();
      String entityName = "datasetVersionEntity";
      List<Predicate> queryPredicatesList =
          RdbmsUtils.getQueryPredicatesFromPredicateList(
              entityName, predicates, builder, criteriaQuery, datasetVersionRoot);
      if (!queryPredicatesList.isEmpty()) {
        finalPredicatesList.addAll(queryPredicatesList);
      }

      String sortBy = queryParameters.getSortKey();
      if (sortBy == null || sortBy.isEmpty()) {
        sortBy = ModelDBConstants.TIME_UPDATED;
      }

      Order orderBy =
          RdbmsUtils.getOrderBasedOnSortKey(
              sortBy, queryParameters.getAscending(), builder, datasetVersionRoot, entityName);

      Predicate[] predicateArr = new Predicate[finalPredicatesList.size()];
      for (int index = 0; index < finalPredicatesList.size(); index++) {
        predicateArr[index] = finalPredicatesList.get(index);
      }

      Predicate predicateWhereCause = builder.and(predicateArr);
      criteriaQuery.select(datasetVersionRoot);
      criteriaQuery.where(predicateWhereCause);
      criteriaQuery.orderBy(orderBy);

      Query query = session.createQuery(criteriaQuery);
      LOGGER.debug("Final datasetVersions final query : {}", query.getQueryString());
      if (queryParameters.getPageNumber() != 0 && queryParameters.getPageLimit() != 0) {
        // Calculate number of documents to skip
        int skips = queryParameters.getPageLimit() * (queryParameters.getPageNumber() - 1);
        query.setFirstResult(skips);
        query.setMaxResults(queryParameters.getPageLimit());
      }

      List<DatasetVersion> datasetVersionList = new ArrayList<>();
      List<DatasetVersionEntity> datasetVersionEntities = query.list();
      if (!datasetVersionEntities.isEmpty()) {
        datasetVersionList =
            RdbmsUtils.convertDatasetVersionsFromDatasetVersionEntityList(datasetVersionEntities);
      }

      // Validate if current user has access to the entity or not where predicate key has a
      // datasetId
      App app = App.getInstance();
      for (KeyValueQuery predicate : predicates) {
        if (predicate.getKey().equals(ModelDBConstants.ID)) {
          if (!predicate.getOperator().equals(OperatorEnum.Operator.EQ)) {
            Status statusMessage =
                Status.newBuilder()
                    .setCode(Code.INVALID_ARGUMENT_VALUE)
                    .setMessage("Unknown 'Operator' type recognized, valid 'Operator' type is EQ")
                    .build();
            throw StatusProto.toStatusRuntimeException(statusMessage);
          }
          if (datasetVersionList.isEmpty()) {
            Status statusMessage =
                Status.newBuilder()
                    .setCode(Code.PERMISSION_DENIED_VALUE)
                    .setMessage(
                        "Access is denied. User is unauthorized for given DatasetVersion entity ID")
                    .build();
            throw StatusProto.toStatusRuntimeException(statusMessage);
          } else {
            String datasetVersionId = predicate.getValue().getStringValue();
            for (DatasetVersion datasetVersion : datasetVersionList) {
              if (datasetVersion.getId().equals(datasetVersionId)) {
                // Validate if current user has access to the entity or not
                roleService.validateEntityUserWithUserInfo(
                    ModelDBServiceResourceTypes.DATASET,
                    datasetVersion.getDatasetId(),
                    ModelDBServiceActions.READ);
              }
            }
          }
        }
      }

      Set<String> datasetVersionIdsSet = new HashSet<>();
      List<DatasetVersion> datasetVersions = new ArrayList<>();
      for (DatasetVersion datasetVersion : datasetVersionList) {
        if (!datasetVersionIdsSet.contains(datasetVersion.getId())) {
          datasetVersionIdsSet.add(datasetVersion.getId());
          if (queryParameters.getIdsOnly()) {
            datasetVersion = DatasetVersion.newBuilder().setId(datasetVersion.getId()).build();
            datasetVersions.add(datasetVersion);
          } else {
            datasetVersions.add(datasetVersion);
          }
        }
      }

      long totalRecords = RdbmsUtils.count(session, datasetVersionRoot, criteriaQuery);

      DatasetVersionDTO datasetVersionPaginationDTO = new DatasetVersionDTO();
      datasetVersionPaginationDTO.setDatasetVersions(datasetVersions);
      datasetVersionPaginationDTO.setTotalRecords(totalRecords);
      return datasetVersionPaginationDTO;
    }
  }

  @Override
  public DatasetVersion updateDatasetVersionDescription(
      String datasetVersionId, String datasetVersionDescription)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      DatasetVersionEntity datasetVersionObj =
          session.get(DatasetVersionEntity.class, datasetVersionId);
      datasetVersionObj.setDescription(datasetVersionDescription);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      datasetVersionObj.setTime_updated(currentTimestamp);
      session.update(datasetVersionObj);
      setDatasetUpdateTime(session, Collections.singletonList(datasetVersionObj.getDataset_id()));
      transaction.commit();
      LOGGER.debug("DatasetVersion updated successfully");
      return datasetVersionObj.getProtoObject();
    }
  }

  @Override
  public DatasetVersion addDatasetVersionTags(String datasetVersionId, List<String> tagsList)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      DatasetVersionEntity datasetVersionObj =
          session.get(DatasetVersionEntity.class, datasetVersionId);
      if (datasetVersionObj == null) {
        LOGGER.warn(ModelDBMessages.DATA_VERSION_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.DATA_VERSION_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      List<String> newTags = new ArrayList<>();
      DatasetVersion existingProtoDatasetVersionObj = datasetVersionObj.getProtoObject();
      for (String tag : tagsList) {
        if (!existingProtoDatasetVersionObj.getTagsList().contains(tag)) {
          newTags.add(tag);
        }
      }
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      if (!newTags.isEmpty()) {
        List<TagsMapping> newTagMappings =
            RdbmsUtils.convertTagListFromTagMappingList(datasetVersionObj, newTags);
        datasetVersionObj.getTags().addAll(newTagMappings);
        datasetVersionObj.setTime_updated(currentTimestamp);
        session.saveOrUpdate(datasetVersionObj);
      }
      setDatasetUpdateTime(session, Collections.singletonList(datasetVersionObj.getDataset_id()));
      transaction.commit();
      LOGGER.debug("DatasetVersion tags added successfully");
      return datasetVersionObj.getProtoObject();
    }
  }

  @Override
  public List<String> getDatasetVersionTags(String datasetVersionId)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetVersionEntity datasetVersionObj =
          session.get(DatasetVersionEntity.class, datasetVersionId);
      LOGGER.debug("DatasetVersion getting successfully");
      return datasetVersionObj.getProtoObject().getTagsList();
    }
  }

  @Override
  public DatasetVersion deleteDatasetVersionTags(
      String datasetVersionId, List<String> datasetVersionTagList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();

      if (deleteAll) {
        Query query = session.createQuery(DELETE_DATASET_VERSION_QUERY_PREFIX);
        query.setParameter(ModelDBConstants.DATASET_VERSION_ID_STR, datasetVersionId);
        query.executeUpdate();
      } else {
        StringBuilder stringQueryBuilder =
            new StringBuilder(DELETE_DATASET_VERSION_QUERY_PREFIX)
                .append(" AND tm." + ModelDBConstants.TAGS + " in (:tags)");
        Query query = session.createQuery(stringQueryBuilder.toString());
        query.setParameter("tags", datasetVersionTagList);
        query.setParameter(ModelDBConstants.DATASET_VERSION_ID_STR, datasetVersionId);
        query.executeUpdate();
      }
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      DatasetVersionEntity datasetVersionObj =
          session.get(DatasetVersionEntity.class, datasetVersionId);
      datasetVersionObj.setTime_updated(currentTimestamp);
      session.update(datasetVersionObj);
      setDatasetUpdateTime(session, Collections.singletonList(datasetVersionObj.getDataset_id()));
      transaction.commit();
      LOGGER.debug("DatasetVersion tags deleted successfully");
      return datasetVersionObj.getProtoObject();
    }
  }

  @Override
  public DatasetVersion addDatasetVersionAttributes(
      String datasetVersionId, List<KeyValue> attributesList)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      DatasetVersionEntity datasetVersionObj =
          session.get(DatasetVersionEntity.class, datasetVersionId);
      datasetVersionObj.setAttributeMapping(
          RdbmsUtils.convertAttributesFromAttributeEntityList(
              datasetVersionObj, ModelDBConstants.ATTRIBUTES, attributesList));
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      datasetVersionObj.setTime_updated(currentTimestamp);
      session.saveOrUpdate(datasetVersionObj);
      setDatasetUpdateTime(session, Collections.singletonList(datasetVersionObj.getDataset_id()));
      transaction.commit();
      LOGGER.debug("DatasetVersion attributes added successfully");
      return datasetVersionObj.getProtoObject();
    }
  }

  @Override
  public DatasetVersion updateDatasetVersionAttributes(String datasetVersionId, KeyValue attribute)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      DatasetVersionEntity datasetVersionObj =
          session.get(DatasetVersionEntity.class, datasetVersionId);
      if (datasetVersionObj == null) {
        LOGGER.warn(ModelDBMessages.DATA_VERSION_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.DATA_VERSION_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      AttributeEntity updatedAttributeObj =
          RdbmsUtils.generateAttributeEntity(
              datasetVersionObj, ModelDBConstants.ATTRIBUTES, attribute);

      List<AttributeEntity> existingAttributes = datasetVersionObj.getAttributeMapping();
      if (!existingAttributes.isEmpty()) {
        boolean doesExist = false;
        for (AttributeEntity existingAttribute : existingAttributes) {
          if (existingAttribute.getKey().equals(attribute.getKey())) {
            existingAttribute.setKey(updatedAttributeObj.getKey());
            existingAttribute.setValue(updatedAttributeObj.getValue());
            existingAttribute.setValue_type(updatedAttributeObj.getValue_type());
            doesExist = true;
            break;
          }
        }
        if (!doesExist) {
          datasetVersionObj.setAttributeMapping(Collections.singletonList(updatedAttributeObj));
        }
      } else {
        datasetVersionObj.setAttributeMapping(Collections.singletonList(updatedAttributeObj));
      }
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      datasetVersionObj.setTime_updated(currentTimestamp);
      session.saveOrUpdate(datasetVersionObj);
      setDatasetUpdateTime(session, Collections.singletonList(datasetVersionObj.getDataset_id()));
      transaction.commit();
      return datasetVersionObj.getProtoObject();
    }
  }

  @Override
  public List<KeyValue> getDatasetVersionAttributes(
      String datasetVersionId, List<String> attributeKeyList, Boolean getAll)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      if (getAll) {
        DatasetVersionEntity datasetVersionObj =
            session.get(DatasetVersionEntity.class, datasetVersionId);
        return datasetVersionObj.getProtoObject().getAttributesList();
      } else {
        Query query = session.createQuery(GET_KEY_VALUE_DATASET_VERSION_QUERY);
        query.setParameterList("keys", attributeKeyList);
        query.setParameter(ModelDBConstants.DATASET_VERSION_ID_STR, datasetVersionId);
        query.setParameter("fieldType", ModelDBConstants.ATTRIBUTES);

        @SuppressWarnings("unchecked")
        List<AttributeEntity> attributeEntities = query.list();
        return RdbmsUtils.convertAttributeEntityListFromAttributes(attributeEntities);
      }
    }
  }

  @Override
  public DatasetVersion deleteDatasetVersionAttributes(
      String datasetVersionId, List<String> attributeKeyList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();

      if (deleteAll) {
        Query query = session.createQuery(DELETE_KEY_VALUE_DATASET_VERSION_QUERY_PREFIX);
        query.setParameter(ModelDBConstants.DATASET_VERSION_ID_STR, datasetVersionId);
        query.executeUpdate();
      } else {
        StringBuilder deleteKeyValueDatasetVersionQuery =
            new StringBuilder(DELETE_KEY_VALUE_DATASET_VERSION_QUERY_PREFIX)
                .append(" AND attr.")
                .append(ModelDBConstants.KEY)
                .append(" in (:keys)");
        Query query = session.createQuery(deleteKeyValueDatasetVersionQuery.toString());
        query.setParameter("keys", attributeKeyList);
        query.setParameter(ModelDBConstants.DATASET_VERSION_ID_STR, datasetVersionId);
        query.executeUpdate();
      }
      DatasetVersionEntity datasetVersionObj =
          session.get(DatasetVersionEntity.class, datasetVersionId);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      datasetVersionObj.setTime_updated(currentTimestamp);
      session.update(datasetVersionObj);
      setDatasetUpdateTime(session, Collections.singletonList(datasetVersionObj.getDataset_id()));
      transaction.commit();
      return datasetVersionObj.getProtoObject();
    }
  }

  @Override
  public DatasetVersion setDatasetVersionVisibility(
      String datasetVersionId, DatasetVisibilityEnum.DatasetVisibility datasetVersionVisibility)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      DatasetVersionEntity datasetVersionObj =
          session.get(DatasetVersionEntity.class, datasetVersionId);
      datasetVersionObj.setDataset_version_visibility(datasetVersionVisibility.ordinal());
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      datasetVersionObj.setTime_updated(currentTimestamp);
      session.update(datasetVersionObj);
      setDatasetUpdateTime(session, Collections.singletonList(datasetVersionObj.getDataset_id()));
      transaction.commit();
      LOGGER.debug("DatasetVersion updated successfully");
      return datasetVersionObj.getProtoObject();
    }
  }
}
