package ai.verta.modeldb.datasetVersion;

import ai.verta.common.KeyValue;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.OperatorEnum;
import ai.verta.modeldb.DatasetVersion;
import ai.verta.modeldb.FindDatasetVersions;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.authservice.RoleServiceUtils;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.exceptions.PermissionDeniedException;
import ai.verta.modeldb.dataset.DatasetDAO;
import ai.verta.modeldb.dto.DatasetVersionDTO;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.DatasetEntity;
import ai.verta.modeldb.entities.DatasetVersionEntity;
import ai.verta.modeldb.entities.TagsMapping;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.uac.ModelDBActionEnum;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.Resources;
import ai.verta.uac.UserInfo;
import com.google.rpc.Code;
import java.util.*;
import javax.persistence.criteria.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;

public class DatasetVersionDAORdbImpl implements DatasetVersionDAO {

  private static final Logger LOGGER = LogManager.getLogger(DatasetVersionDAORdbImpl.class);
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private final AuthService authService;
  private final MDBRoleService mdbRoleService;

  private static final String CHECK_DATASET_VERSION_EXISTS_BY_ID_HQL =
      new StringBuilder(
              "Select count(dsv." + ModelDBConstants.ID + ") From DatasetVersionEntity dsv where ")
          .append(" dsv." + ModelDBConstants.ID + " = :datasetVersionId ")
          .append(" AND dsv." + ModelDBConstants.DELETED + " = false ")
          .toString();
  private static final String DATASET_VERSION_BY_IDS_QUERY =
      "From DatasetVersionEntity ds where ds.id IN (:ids) AND ds."
          + ModelDBConstants.DELETED
          + " = false ";
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
  private static final String UPDATE_DELETED_STATUS_DATASET_VERSION_QUERY_STRING =
      new StringBuilder("UPDATE ")
          .append(DatasetVersionEntity.class.getSimpleName())
          .append(" dv ")
          .append("SET dv.")
          .append(ModelDBConstants.DELETED)
          .append(" = :deleted ")
          .append(" WHERE dv.")
          .append(ModelDBConstants.ID)
          .append(" IN (:datasetVersionIds)")
          .toString();
  private static final String DELETED_STATUS_DATASET_VERSION_BY_DATASET_QUERY_STRING =
      new StringBuilder("UPDATE ")
          .append(DatasetVersionEntity.class.getSimpleName())
          .append(" dv ")
          .append("SET dv.")
          .append(ModelDBConstants.DELETED)
          .append(" = :deleted ")
          .append(" WHERE dv.")
          .append(ModelDBConstants.DATASET_ID)
          .append(" IN (:datasetIds)")
          .toString();

  public DatasetVersionDAORdbImpl(AuthService authService, MDBRoleService mdbRoleService) {
    this.authService = authService;
    this.mdbRoleService = mdbRoleService;
  }

  @Override
  public DatasetVersionDTO getDatasetVersions(
      DatasetDAO datasetDAO,
      String datasetId,
      int pageNumber,
      int pageLimit,
      boolean isAscending,
      String sortKey,
      UserInfo currentLoginUser)
      throws PermissionDeniedException {
    var findDatasetVersions =
        FindDatasetVersions.newBuilder()
            .setDatasetId(datasetId)
            .setPageNumber(pageNumber)
            .setPageLimit(pageLimit)
            .setAscending(isAscending)
            .setSortKey(sortKey)
            .build();
    return findDatasetVersions(datasetDAO, findDatasetVersions, currentLoginUser);
  }

  @Override
  public Boolean deleteDatasetVersionsByDatasetIDs(List<String> datasetIds) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();
      var query =
          session
              .createQuery(DELETED_STATUS_DATASET_VERSION_BY_DATASET_QUERY_STRING)
              .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
      query.setParameter("deleted", true);
      query.setParameterList("datasetIds", datasetIds);
      int updatedCount = query.executeUpdate();
      transaction.commit();
      LOGGER.debug("DatasetVersion deleted successfully, updated Row: {}", updatedCount);
      return true;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteDatasetVersionsByDatasetIDs(datasetIds);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public DatasetVersion getDatasetVersion(String datasetVersionId) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetVersionEntity datasetVersionObj =
          session.get(DatasetVersionEntity.class, datasetVersionId);
      if (datasetVersionObj == null || datasetVersionObj.getDeleted()) {
        throw new NotFoundException(ModelDBMessages.DATA_VERSION_NOT_FOUND_ERROR_MSG);
      }
      LOGGER.debug("DatasetVersion getting successfully");
      return datasetVersionObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getDatasetVersion(datasetVersionId);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public String getUrlForDatasetVersion(String datasetVersionId, String method) {
    throw new InvalidArgumentException("Not supported yet");
  }

  @Override
  public List<DatasetVersion> getDatasetVersionsByBatchIds(List<String> datasetVersionIds) {
    if (datasetVersionIds == null || datasetVersionIds.isEmpty()) {
      LOGGER.info("Input datasetVersionIds is empty");
      return new ArrayList<>();
    }
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var query = session.createQuery(DATASET_VERSION_BY_IDS_QUERY);
      query.setParameterList("ids", datasetVersionIds);

      @SuppressWarnings("unchecked")
      List<DatasetVersionEntity> datasetEntities = query.list();
      LOGGER.debug("DatasetVersion by Ids getting successfully");
      return RdbmsUtils.convertDatasetVersionsFromDatasetVersionEntityList(datasetEntities);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getDatasetVersionsByBatchIds(datasetVersionIds);
      } else {
        throw ex;
      }
    }
  }

  /**
   * For getting datasetVersions that user has access to (either as the owner or a collaborator)
   * dataserVersion of dataset: <br>
   *
   * <ol>
   *   <li>Iterate through all datasetVersions of the requested datasetVersionIds
   *   <li>Get the dataset Id they belong to.
   *   <li>Check if dataset is accessible or not.
   * </ol>
   *
   * The list of accessible datasetVersionIDs is built and returned by this method.
   *
   * @param requestedDatasetVersionIds : datasetVersion Ids
   * @param modelDBServiceActions : modelDB action like READ, UPDATE
   * @return List<String> : list of accessible datasetVersion Id
   */
  public List<String> getAccessibleDatasetVersionIDs(
      List<String> requestedDatasetVersionIds, ModelDBServiceActions modelDBServiceActions) {
    List<DatasetVersion> datasetVersionList =
        getDatasetVersionsByBatchIds(requestedDatasetVersionIds);
    Map<String, String> datasetIdDatasetVersionIdMap = new HashMap<>();
    for (DatasetVersion datasetVersion : datasetVersionList) {
      datasetIdDatasetVersionIdMap.put(datasetVersion.getId(), datasetVersion.getDatasetId());
    }
    Set<String> datasetIdSet = new HashSet<>(datasetIdDatasetVersionIdMap.values());

    List<String> accessibleDatasetVersionIds = new ArrayList<>();
    List<Resources> allowedDatasets;
    // Validate if current user has access to the entity or not
    if (datasetIdSet.size() == 1) {
      mdbRoleService.isSelfAllowed(
          ModelDBServiceResourceTypes.DATASET,
          modelDBServiceActions,
          new ArrayList<>(datasetIdSet).get(0));
      accessibleDatasetVersionIds.addAll(requestedDatasetVersionIds);
    } else {
      allowedDatasets =
          mdbRoleService.getSelfAllowedResources(
              ModelDBServiceResourceTypes.DATASET, modelDBServiceActions);
      // Validate if current user has access to the entity or not
      Set<String> allowedDatasetIds =
          RoleServiceUtils.getAccessibleResourceIdsFromAllowedResources(
              datasetIdSet, allowedDatasets);
      for (Map.Entry<String, String> entry : datasetIdDatasetVersionIdMap.entrySet()) {
        if (allowedDatasetIds.contains(entry.getValue())) {
          accessibleDatasetVersionIds.add(entry.getKey());
        }
      }
    }
    return accessibleDatasetVersionIds;
  }

  @Override
  public DatasetVersionDTO findDatasetVersions(
      DatasetDAO datasetDAO, FindDatasetVersions queryParameters, UserInfo currentLoginUserInfo)
      throws PermissionDeniedException {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {

      List<String> accessibleDatasetVersionIds = new ArrayList<>();
      if (!queryParameters.getDatasetVersionIdsList().isEmpty()) {
        accessibleDatasetVersionIds.addAll(
            getAccessibleDatasetVersionIDs(
                queryParameters.getDatasetVersionIdsList(),
                ModelDBActionEnum.ModelDBServiceActions.READ));
        if (accessibleDatasetVersionIds.isEmpty()) {
          throw new PermissionDeniedException(
              "Access is denied. User is unauthorized for given DatasetVersion IDs : "
                  + accessibleDatasetVersionIds);
        }
      }

      List<KeyValueQuery> predicates = new ArrayList<>(queryParameters.getPredicatesList());
      for (KeyValueQuery predicate : predicates) {
        if (predicate.getKey().equals(ModelDBConstants.ID)) {
          List<String> accessibleDatasetVersionId =
              getAccessibleDatasetVersionIDs(
                  Collections.singletonList(predicate.getValue().getStringValue()),
                  ModelDBActionEnum.ModelDBServiceActions.READ);
          accessibleDatasetVersionIds.addAll(accessibleDatasetVersionId);
          RdbmsUtils.validatePredicates(
              ModelDBConstants.DATASETS_VERSIONS,
              accessibleDatasetVersionIds,
              predicate,
              mdbRoleService.IsImplemented());
        }
      }

      var builder = session.getCriteriaBuilder();
      // Using FROM and JOIN
      CriteriaQuery<DatasetVersionEntity> criteriaQuery =
          builder.createQuery(DatasetVersionEntity.class);
      Root<DatasetVersionEntity> datasetVersionRoot =
          criteriaQuery.from(DatasetVersionEntity.class);
      datasetVersionRoot.alias("ds");

      Root<DatasetEntity> datasetEntityRoot = criteriaQuery.from(DatasetEntity.class);
      datasetEntityRoot.alias("dt");

      List<Predicate> finalPredicatesList = new ArrayList<>();
      finalPredicatesList.add(
          builder.equal(
              datasetVersionRoot.get(ModelDBConstants.DATASET_ID),
              datasetEntityRoot.get(ModelDBConstants.ID)));

      List<String> datasetIds = new ArrayList<>();
      if (!queryParameters.getDatasetId().isEmpty()) {
        datasetIds.add(queryParameters.getDatasetId());
      } else if (accessibleDatasetVersionIds.isEmpty()) {
        List<String> workspaceDatasetIDs =
            datasetDAO.getWorkspaceDatasetIDs(
                queryParameters.getWorkspaceName(), currentLoginUserInfo);
        if (workspaceDatasetIDs == null || workspaceDatasetIDs.isEmpty()) {
          LOGGER.debug(
              "accessible dataset for the datasetVersions not found for given workspace : {}",
              queryParameters.getWorkspaceName());
          var datasetVersionPaginationDTO = new DatasetVersionDTO();
          datasetVersionPaginationDTO.setDatasetVersions(Collections.emptyList());
          datasetVersionPaginationDTO.setTotalRecords(0L);
          return datasetVersionPaginationDTO;
        }
        datasetIds.addAll(workspaceDatasetIDs);
      }

      if (accessibleDatasetVersionIds.isEmpty() && datasetIds.isEmpty()) {
        throw new PermissionDeniedException(
            "Access is denied. Accessible datasets not found for given DatasetVersion IDs : "
                + accessibleDatasetVersionIds);
      }

      if (!datasetIds.isEmpty()) {
        Expression<String> datasetExpression = datasetVersionRoot.get(ModelDBConstants.DATASET_ID);
        var datasetsPredicate = datasetExpression.in(datasetIds);
        finalPredicatesList.add(datasetsPredicate);
      }

      if (!accessibleDatasetVersionIds.isEmpty()) {
        Expression<String> exp = datasetVersionRoot.get(ModelDBConstants.ID);
        var predicate2 = exp.in(accessibleDatasetVersionIds);
        finalPredicatesList.add(predicate2);
      }

      var entityName = "datasetVersionEntity";
      try {
        List<Predicate> queryPredicatesList =
            RdbmsUtils.getQueryPredicatesFromPredicateList(
                entityName,
                predicates,
                builder,
                criteriaQuery,
                datasetVersionRoot,
                authService,
                mdbRoleService,
                ModelDBServiceResourceTypes.DATASET_VERSION);
        if (!queryPredicatesList.isEmpty()) {
          finalPredicatesList.addAll(queryPredicatesList);
        }
      } catch (ModelDBException ex) {
        if (ex.getCode().ordinal() == Code.FAILED_PRECONDITION_VALUE
            && ModelDBConstants.INTERNAL_MSG_USERS_NOT_FOUND.equals(ex.getMessage())) {
          LOGGER.info(ex.getMessage());
          var datasetVersionDTO = new DatasetVersionDTO();
          datasetVersionDTO.setDatasetVersions(Collections.emptyList());
          datasetVersionDTO.setTotalRecords(0L);
          return datasetVersionDTO;
        }
        throw ex;
      }

      finalPredicatesList.add(
          builder.equal(datasetVersionRoot.get(ModelDBConstants.DELETED), false));
      finalPredicatesList.add(
          builder.equal(datasetEntityRoot.get(ModelDBConstants.DELETED), false));

      String sortBy = queryParameters.getSortKey();
      if (sortBy.isEmpty()) {
        sortBy = ModelDBConstants.TIME_UPDATED;
      }

      var orderBy =
          RdbmsUtils.getOrderBasedOnSortKey(
              sortBy, queryParameters.getAscending(), builder, datasetVersionRoot, entityName);

      var predicateArr = new Predicate[finalPredicatesList.size()];
      for (var index = 0; index < finalPredicatesList.size(); index++) {
        predicateArr[index] = finalPredicatesList.get(index);
      }

      var predicateWhereCause = builder.and(predicateArr);
      criteriaQuery.select(datasetVersionRoot);
      criteriaQuery.where(predicateWhereCause);
      criteriaQuery.orderBy(orderBy);

      var query = session.createQuery(criteriaQuery);
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
      for (KeyValueQuery predicate : predicates) {
        if (predicate.getKey().equals(ModelDBConstants.ID)) {
          if (!predicate.getOperator().equals(OperatorEnum.Operator.EQ)) {
            throw new InvalidArgumentException(
                "Unknown 'Operator' type recognized, valid 'Operator' type is EQ");
          }
          if (datasetVersionList.isEmpty()) {
            throw new PermissionDeniedException(
                "Access is denied. User is unauthorized for given DatasetVersion entity ID");
          } else {
            var datasetVersionId = predicate.getValue().getStringValue();
            for (DatasetVersion datasetVersion : datasetVersionList) {
              if (datasetVersion.getId().equals(datasetVersionId)) {
                // Validate if current user has access to the entity or not
                mdbRoleService.validateEntityUserWithUserInfo(
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

      var datasetVersionPaginationDTO = new DatasetVersionDTO();
      datasetVersionPaginationDTO.setDatasetVersions(datasetVersions);
      datasetVersionPaginationDTO.setTotalRecords(totalRecords);
      return datasetVersionPaginationDTO;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return findDatasetVersions(datasetDAO, queryParameters, currentLoginUserInfo);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public DatasetVersion updateDatasetVersionDescription(
      String datasetVersionId, String datasetVersionDescription) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetVersionEntity datasetVersionObj =
          session.get(DatasetVersionEntity.class, datasetVersionId, LockMode.PESSIMISTIC_WRITE);
      datasetVersionObj.setDescription(datasetVersionDescription);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      datasetVersionObj.setTime_updated(currentTimestamp);
      var transaction = session.beginTransaction();
      session.update(datasetVersionObj);
      transaction.commit();
      LOGGER.debug("DatasetVersion updated successfully");
      return datasetVersionObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return updateDatasetVersionDescription(datasetVersionId, datasetVersionDescription);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public DatasetVersion addDatasetVersionTags(String datasetVersionId, List<String> tagsList) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetVersionEntity datasetVersionObj =
          session.get(DatasetVersionEntity.class, datasetVersionId, LockMode.PESSIMISTIC_WRITE);
      if (datasetVersionObj == null) {
        throw new NotFoundException(ModelDBMessages.DATA_VERSION_NOT_FOUND_ERROR_MSG);
      }
      List<String> newTags = new ArrayList<>();
      var existingProtoDatasetVersionObj = datasetVersionObj.getProtoObject();
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
        var transaction = session.beginTransaction();
        session.saveOrUpdate(datasetVersionObj);
        transaction.commit();
      }
      LOGGER.debug("DatasetVersion tags added successfully");
      return datasetVersionObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return addDatasetVersionTags(datasetVersionId, tagsList);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public DatasetVersion deleteDatasetVersionTags(
      String datasetVersionId, List<String> datasetVersionTagList, Boolean deleteAll) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();

      if (deleteAll) {
        var query =
            session
                .createQuery(DELETE_DATASET_VERSION_QUERY_PREFIX)
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter(ModelDBConstants.DATASET_VERSION_ID_STR, datasetVersionId);
        query.executeUpdate();
      } else {
        StringBuilder stringQueryBuilder =
            new StringBuilder(DELETE_DATASET_VERSION_QUERY_PREFIX)
                .append(" AND tm." + ModelDBConstants.TAGS + " in (:tags)");
        var query =
            session
                .createQuery(stringQueryBuilder.toString())
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter("tags", datasetVersionTagList);
        query.setParameter(ModelDBConstants.DATASET_VERSION_ID_STR, datasetVersionId);
        query.executeUpdate();
      }
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      DatasetVersionEntity datasetVersionObj =
          session.get(DatasetVersionEntity.class, datasetVersionId);
      datasetVersionObj.setTime_updated(currentTimestamp);
      session.update(datasetVersionObj);
      transaction.commit();
      LOGGER.debug("DatasetVersion tags deleted successfully");
      return datasetVersionObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteDatasetVersionTags(datasetVersionId, datasetVersionTagList, deleteAll);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public DatasetVersion addDatasetVersionAttributes(
      String datasetVersionId, List<KeyValue> attributesList) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetVersionEntity datasetVersionObj =
          session.get(DatasetVersionEntity.class, datasetVersionId, LockMode.PESSIMISTIC_WRITE);
      datasetVersionObj.setAttributeMapping(
          RdbmsUtils.convertAttributesFromAttributeEntityList(
              datasetVersionObj, ModelDBConstants.ATTRIBUTES, attributesList));
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      datasetVersionObj.setTime_updated(currentTimestamp);
      var transaction = session.beginTransaction();
      session.saveOrUpdate(datasetVersionObj);
      transaction.commit();
      LOGGER.debug("DatasetVersion attributes added successfully");
      return datasetVersionObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return addDatasetVersionAttributes(datasetVersionId, attributesList);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public DatasetVersion updateDatasetVersionAttributes(
      String datasetVersionId, KeyValue attribute) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetVersionEntity datasetVersionObj =
          session.get(DatasetVersionEntity.class, datasetVersionId, LockMode.PESSIMISTIC_WRITE);
      if (datasetVersionObj == null) {
        throw new NotFoundException(ModelDBMessages.DATA_VERSION_NOT_FOUND_ERROR_MSG);
      }

      var updatedAttributeObj =
          RdbmsUtils.generateAttributeEntity(
              datasetVersionObj, ModelDBConstants.ATTRIBUTES, attribute);

      List<AttributeEntity> existingAttributes = datasetVersionObj.getAttributeMapping();
      if (!existingAttributes.isEmpty()) {
        var doesExist = false;
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
      var transaction = session.beginTransaction();
      session.saveOrUpdate(datasetVersionObj);
      transaction.commit();
      return datasetVersionObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return updateDatasetVersionAttributes(datasetVersionId, attribute);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<KeyValue> getDatasetVersionAttributes(
      String datasetVersionId, List<String> attributeKeyList, Boolean getAll) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      if (getAll) {
        DatasetVersionEntity datasetVersionObj =
            session.get(DatasetVersionEntity.class, datasetVersionId);
        return datasetVersionObj.getProtoObject().getAttributesList();
      } else {
        var query = session.createQuery(GET_KEY_VALUE_DATASET_VERSION_QUERY);
        query.setParameterList("keys", attributeKeyList);
        query.setParameter(ModelDBConstants.DATASET_VERSION_ID_STR, datasetVersionId);
        query.setParameter("fieldType", ModelDBConstants.ATTRIBUTES);

        @SuppressWarnings("unchecked")
        List<AttributeEntity> attributeEntities = query.list();
        return RdbmsUtils.convertAttributeEntityListFromAttributes(attributeEntities);
      }
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getDatasetVersionAttributes(datasetVersionId, attributeKeyList, getAll);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public DatasetVersion deleteDatasetVersionAttributes(
      String datasetVersionId, List<String> attributeKeyList, Boolean deleteAll) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();

      if (deleteAll) {
        var query =
            session
                .createQuery(DELETE_KEY_VALUE_DATASET_VERSION_QUERY_PREFIX)
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter(ModelDBConstants.DATASET_VERSION_ID_STR, datasetVersionId);
        query.executeUpdate();
      } else {
        StringBuilder deleteKeyValueDatasetVersionQuery =
            new StringBuilder(DELETE_KEY_VALUE_DATASET_VERSION_QUERY_PREFIX)
                .append(" AND attr.")
                .append(ModelDBConstants.KEY)
                .append(" in (:keys)");
        var query =
            session
                .createQuery(deleteKeyValueDatasetVersionQuery.toString())
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter("keys", attributeKeyList);
        query.setParameter(ModelDBConstants.DATASET_VERSION_ID_STR, datasetVersionId);
        query.executeUpdate();
      }
      DatasetVersionEntity datasetVersionObj =
          session.get(DatasetVersionEntity.class, datasetVersionId);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      datasetVersionObj.setTime_updated(currentTimestamp);
      session.update(datasetVersionObj);
      transaction.commit();
      return datasetVersionObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteDatasetVersionAttributes(datasetVersionId, attributeKeyList, deleteAll);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public boolean isDatasetVersionExists(Session session, String datasetVersionId) {
    var query = session.createQuery(CHECK_DATASET_VERSION_EXISTS_BY_ID_HQL);
    query.setParameter("datasetVersionId", datasetVersionId);
    Long count = (Long) query.uniqueResult();
    return count > 0;
  }
}
