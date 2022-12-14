package ai.verta.modeldb.dataset;

import ai.verta.common.KeyValue;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.OperatorEnum;
import ai.verta.common.ValueTypeEnum;
import ai.verta.common.WorkspaceTypeEnum.WorkspaceType;
import ai.verta.modeldb.Dataset;
import ai.verta.modeldb.FindDatasets;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.collaborator.CollaboratorUser;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.exceptions.PermissionDeniedException;
import ai.verta.modeldb.dto.DatasetPaginationDTO;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.DatasetEntity;
import ai.verta.modeldb.entities.TagsMapping;
import ai.verta.modeldb.telemetry.TelemetryUtils;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.uac.*;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.criteria.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class DatasetDAORdbImpl implements DatasetDAO {

  private static final Logger LOGGER = LogManager.getLogger(DatasetDAORdbImpl.class);
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private static final String DATASET_ID_POST_QUERY_PARAM = " = :datasetId";
  private final AuthService authService;
  private final MDBRoleService mdbRoleService;

  // Queries
  private static final String GET_DATASET_BY_IDS_QUERY =
      "From DatasetEntity ds where ds.id IN (:ids) AND ds."
          + ModelDBConstants.DELETED
          + " = false ";
  private static final String GET_DATASET_ATTRIBUTES_QUERY =
      new StringBuilder("From AttributeEntity attr where attr.")
          .append(ModelDBConstants.KEY)
          .append(" in (:keys) AND attr.datasetEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :datasetId AND attr.field_type = :fieldType")
          .toString();
  private static final String CHECK_DATASE_QUERY_PREFIX =
      new StringBuilder("Select count(*) From DatasetEntity ds where ")
          .append(" ds." + ModelDBConstants.NAME + " = :datasetName ")
          .toString();
  private static final String DELETED_STATUS_DATASET_QUERY_STRING =
      new StringBuilder("UPDATE ")
          .append(DatasetEntity.class.getSimpleName())
          .append(" dt ")
          .append("SET dt.")
          .append(ModelDBConstants.DELETED)
          .append(" = :deleted ")
          .append(" WHERE dt.")
          .append(ModelDBConstants.ID)
          .append(" IN (:datasetIds)")
          .toString();
  private static final String COUNT_DATASET_BY_ID_HQL =
      "Select Count(id) From DatasetEntity d where d.deleted = false AND d.id = :datasetId";
  private static final String NON_DELETED_DATASET_IDS =
      "select id  From DatasetEntity d where d.deleted = false";
  private static final String NON_DELETED_DATASET_IDS_BY_IDS =
      NON_DELETED_DATASET_IDS + " AND d.id in (:" + ModelDBConstants.DATASET_IDS + ")";

  public DatasetDAORdbImpl(AuthService authService, MDBRoleService mdbRoleService) {
    this.authService = authService;
    this.mdbRoleService = mdbRoleService;
  }

  private void checkDatasetAlreadyExist(Dataset dataset) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      // TODO: this entire DAO will removed after merging of the PR:
      // https://github.com/VertaAI/modeldb/pull/1846
      /*modelDBHibernateUtil.checkIfEntityAlreadyExists(
      session,
      "ds",
      CHECK_DATASE_QUERY_PREFIX,
      "Dataset",
      "datasetName",
      dataset.getName(),
      ModelDBConstants.WORKSPACE,
      dataset.getWorkspaceId(),
      dataset.getWorkspaceType(),
      LOGGER);*/
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        checkDatasetAlreadyExist(dataset);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Dataset createDataset(Dataset dataset, UserInfo userInfo) {
    // Check entity already exists
    checkDatasetAlreadyExist(dataset);

    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var datasetEntity = RdbmsUtils.generateDatasetEntity(dataset);
      var transaction = session.beginTransaction();
      session.save(datasetEntity);
      transaction.commit();
      LOGGER.debug("Dataset created successfully");

      var resourceVisibility = dataset.getVisibility();
      if (dataset.getVisibility().equals(ResourceVisibility.UNKNOWN)) {
        resourceVisibility =
            ModelDBUtils.getResourceVisibility(Optional.empty(), dataset.getDatasetVisibility());
      }
      mdbRoleService.createWorkspacePermissions(
          Optional.of(dataset.getWorkspaceServiceId()),
          Optional.empty(),
          dataset.getId(),
          datasetEntity.getName(),
          Optional.empty(),
          ModelDBServiceResourceTypes.DATASET,
          dataset.getCustomPermission(),
          resourceVisibility,
          false);
      LOGGER.debug("Dataset role bindings created successfully");
      TelemetryUtils.insertModelDBDeploymentInfo();
      return datasetEntity.getProtoObject(mdbRoleService);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return createDataset(dataset, userInfo);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<Dataset> getDatasetByIds(List<String> sharedDatasetIds) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      List<DatasetEntity> datasetEntities = getDatasetEntityList(session, sharedDatasetIds);
      LOGGER.debug("Got Dataset by Ids successfully");
      return RdbmsUtils.convertDatasetsFromDatasetEntityList(mdbRoleService, datasetEntities);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getDatasetByIds(sharedDatasetIds);
      } else {
        throw ex;
      }
    }
  }

  @SuppressWarnings("unchecked")
  private List<DatasetEntity> getDatasetEntityList(Session session, List<String> datasetIds) {
    var query = session.createQuery(GET_DATASET_BY_IDS_QUERY);
    query.setParameterList("ids", datasetIds);
    return query.list();
  }

  @Override
  public DatasetPaginationDTO getDatasets(
      UserInfo userInfo,
      Integer pageNumber,
      Integer pageLimit,
      Boolean order,
      String sortKey,
      ResourceVisibility datasetVisibility) {
    var findDatasets =
        FindDatasets.newBuilder()
            .setPageNumber(pageNumber)
            .setPageLimit(pageLimit)
            .setAscending(order)
            .setSortKey(sortKey)
            .build();
    return findDatasets(findDatasets, userInfo, datasetVisibility);
  }

  @Override
  public Boolean deleteDatasets(List<String> datasetIds) {
    // Get self allowed resources id where user has delete permission
    Collection<String> allowedDatasetIds =
        mdbRoleService.getAccessibleResourceIdsByActions(
            ModelDBServiceResourceTypes.DATASET,
            ModelDBActionEnum.ModelDBServiceActions.DELETE,
            datasetIds);
    if (allowedDatasetIds.isEmpty()) {
      throw new PermissionDeniedException("Access Denied for given dataset Ids : " + datasetIds);
    }

    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();
      var deletedDatasetsQuery =
          session
              .createQuery(DELETED_STATUS_DATASET_QUERY_STRING)
              .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
      deletedDatasetsQuery.setParameter("deleted", true);
      deletedDatasetsQuery.setParameter("datasetIds", allowedDatasetIds);
      int updatedCount = deletedDatasetsQuery.executeUpdate();
      LOGGER.debug("Mark Datasets as deleted : {}, count : {}", allowedDatasetIds, updatedCount);
      transaction.commit();
      LOGGER.debug("Dataset deleted successfully");
      return true;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteDatasets(datasetIds);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Dataset getDatasetById(String datasetId) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var datasetObj = getDatasetEntity(session, datasetId);
      return datasetObj.getProtoObject(mdbRoleService);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getDatasetById(datasetId);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public DatasetEntity getDatasetEntity(Session session, String datasetId) {
    DatasetEntity datasetObj = session.get(DatasetEntity.class, datasetId);
    if (datasetObj == null) {
      throw new NotFoundException("dataset with input id not found");
    }
    LOGGER.debug(ModelDBMessages.DATASET_UPDATE_SUCCESSFULLY_MSG);
    return datasetObj;
  }

  @Override
  public DatasetPaginationDTO findDatasets(
      FindDatasets queryParameters,
      UserInfo currentLoginUserInfo,
      ResourceVisibility datasetVisibility) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {

      Collection<String> accessibleDatasetIds =
          mdbRoleService.getAccessibleResourceIds(
              null,
              new CollaboratorUser(authService, currentLoginUserInfo),
              ModelDBServiceResourceTypes.DATASET,
              queryParameters.getDatasetIdsList());

      if (accessibleDatasetIds.isEmpty() && mdbRoleService.IsImplemented()) {
        LOGGER.debug("Accessible Dataset Ids not found, size 0");
        var datasetPaginationDTO = new DatasetPaginationDTO();
        datasetPaginationDTO.setDatasets(Collections.emptyList());
        datasetPaginationDTO.setTotalRecords(0L);
        return datasetPaginationDTO;
      }

      var builder = session.getCriteriaBuilder();
      // Using FROM and JOIN
      CriteriaQuery<DatasetEntity> criteriaQuery = builder.createQuery(DatasetEntity.class);
      Root<DatasetEntity> datasetRoot = criteriaQuery.from(DatasetEntity.class);
      datasetRoot.alias("ds");
      List<Predicate> finalPredicatesList = new ArrayList<>();

      List<KeyValueQuery> predicates = new ArrayList<>(queryParameters.getPredicatesList());
      for (KeyValueQuery predicate : predicates) {
        // Validate if current user has access to the entity or not where predicate key has an id
        RdbmsUtils.validatePredicates(
            ModelDBConstants.DATASETS,
            accessibleDatasetIds,
            predicate,
            mdbRoleService.IsImplemented());
      }

      String workspaceName = queryParameters.getWorkspaceName();

      if (workspaceName != null
          && !workspaceName.isEmpty()
          && workspaceName.equals(authService.getUsernameFromUserInfo(currentLoginUserInfo))) {
        accessibleDatasetIds =
            mdbRoleService.getSelfDirectlyAllowedResources(
                ModelDBServiceResourceTypes.DATASET, ModelDBActionEnum.ModelDBServiceActions.READ);
        if (queryParameters.getDatasetIdsList() != null
            && !queryParameters.getDatasetIdsList().isEmpty()) {
          accessibleDatasetIds.retainAll(queryParameters.getDatasetIdsList());
        }
        // user is in his workspace and has no datasets, return empty
        if (accessibleDatasetIds.isEmpty()) {
          var datasetPaginationDTO = new DatasetPaginationDTO();
          datasetPaginationDTO.setDatasets(Collections.emptyList());
          datasetPaginationDTO.setTotalRecords(0L);
          return datasetPaginationDTO;
        }

        List<String> orgIds =
            mdbRoleService.listMyOrganizations().stream()
                .map(Organization::getId)
                .collect(Collectors.toList());
        if (!orgIds.isEmpty()) {
          finalPredicatesList.add(
              builder.not(
                  builder.and(
                      datasetRoot.get(ModelDBConstants.WORKSPACE).in(orgIds),
                      builder.equal(
                          datasetRoot.get(ModelDBConstants.WORKSPACE_TYPE),
                          WorkspaceType.ORGANIZATION_VALUE))));
        }
      } else {
        if (datasetVisibility.equals(ResourceVisibility.PRIVATE)) {
          List<KeyValueQuery> workspacePredicates =
              ModelDBUtils.getKeyValueQueriesByWorkspace(
                  mdbRoleService, currentLoginUserInfo, workspaceName);
          if (workspacePredicates.size() > 0) {
            var privateWorkspacePredicate =
                builder.equal(
                    datasetRoot.get(ModelDBConstants.WORKSPACE),
                    workspacePredicates.get(0).getValue().getStringValue());
            var privateWorkspaceTypePredicate =
                builder.equal(
                    datasetRoot.get(ModelDBConstants.WORKSPACE_TYPE),
                    workspacePredicates.get(1).getValue().getNumberValue());
            var privatePredicate =
                builder.and(privateWorkspacePredicate, privateWorkspaceTypePredicate);

            finalPredicatesList.add(privatePredicate);
          }
        }
      }

      if (!accessibleDatasetIds.isEmpty()) {
        Expression<String> exp = datasetRoot.get(ModelDBConstants.ID);
        var predicate2 = exp.in(accessibleDatasetIds);
        finalPredicatesList.add(predicate2);
      }

      var entityName = "datasetEntity";
      try {
        List<Predicate> queryPredicatesList =
            RdbmsUtils.getQueryPredicatesFromPredicateList(
                entityName,
                predicates,
                builder,
                criteriaQuery,
                datasetRoot,
                authService,
                mdbRoleService,
                ModelDBServiceResourceTypes.DATASET);
        if (!queryPredicatesList.isEmpty()) {
          finalPredicatesList.addAll(queryPredicatesList);
        }
      } catch (ModelDBException ex) {
        if (ex.getCode().ordinal() == Code.FAILED_PRECONDITION_VALUE
            && ModelDBConstants.INTERNAL_MSG_USERS_NOT_FOUND.equals(ex.getMessage())) {
          LOGGER.info(ex.getMessage());
          var datasetPaginationDTO = new DatasetPaginationDTO();
          datasetPaginationDTO.setDatasets(Collections.emptyList());
          datasetPaginationDTO.setTotalRecords(0L);
          return datasetPaginationDTO;
        }
        throw ex;
      }

      finalPredicatesList.add(builder.equal(datasetRoot.get(ModelDBConstants.DELETED), false));

      String sortBy = queryParameters.getSortKey();
      if (sortBy == null || sortBy.isEmpty()) {
        sortBy = ModelDBConstants.TIME_UPDATED;
      }

      var orderBy =
          RdbmsUtils.getOrderBasedOnSortKey(
              sortBy, queryParameters.getAscending(), builder, datasetRoot, entityName);

      var predicateArr = new Predicate[finalPredicatesList.size()];
      for (var index = 0; index < finalPredicatesList.size(); index++) {
        predicateArr[index] = finalPredicatesList.get(index);
      }

      var predicateWhereCause = builder.and(predicateArr);
      criteriaQuery.select(datasetRoot);
      criteriaQuery.where(predicateWhereCause);
      criteriaQuery.orderBy(orderBy);

      var query = session.createQuery(criteriaQuery);
      LOGGER.debug("Datasets final query : {}", query.getQueryString());
      if (queryParameters.getPageNumber() != 0 && queryParameters.getPageLimit() != 0) {
        // Calculate number of documents to skip
        int skips = queryParameters.getPageLimit() * (queryParameters.getPageNumber() - 1);
        query.setFirstResult(skips);
        query.setMaxResults(queryParameters.getPageLimit());
      }

      List<Dataset> datasetList = new ArrayList<>();
      List<DatasetEntity> datasetEntities = query.list();
      LOGGER.debug("Datasets result count : {}", datasetEntities.size());
      if (!datasetEntities.isEmpty()) {
        datasetList =
            RdbmsUtils.convertDatasetsFromDatasetEntityList(mdbRoleService, datasetEntities);
      }

      Set<String> datasetIdsSet = new HashSet<>();
      List<Dataset> datasets = new ArrayList<>();
      for (Dataset dataset : datasetList) {
        if (!datasetIdsSet.contains(dataset.getId())) {
          datasetIdsSet.add(dataset.getId());
          if (queryParameters.getIdsOnly()) {
            dataset = Dataset.newBuilder().setId(dataset.getId()).build();
            datasets.add(dataset);
          } else {
            datasets.add(dataset);
          }
        }
      }

      long totalRecords = RdbmsUtils.count(session, datasetRoot, criteriaQuery);
      LOGGER.debug("Datasets total records count : {}", totalRecords);

      var datasetPaginationDTO = new DatasetPaginationDTO();
      datasetPaginationDTO.setDatasets(datasets);
      datasetPaginationDTO.setTotalRecords(totalRecords);
      return datasetPaginationDTO;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return findDatasets(queryParameters, currentLoginUserInfo, datasetVisibility);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<Dataset> getDatasets(String key, String value, UserInfo userInfo) {
    var findDatasets =
        FindDatasets.newBuilder()
            .addPredicates(
                KeyValueQuery.newBuilder()
                    .setKey(key)
                    .setValue(Value.newBuilder().setStringValue(value).build())
                    .setOperator(OperatorEnum.Operator.EQ)
                    .setValueType(ValueTypeEnum.ValueType.STRING)
                    .build())
            .build();
    var datasetPaginationDTO = findDatasets(findDatasets, userInfo, ResourceVisibility.PRIVATE);
    LOGGER.debug("Datasets size is {}", datasetPaginationDTO.getDatasets().size());
    return datasetPaginationDTO.getDatasets();
  }

  @Override
  public Dataset updateDatasetName(String datasetId, String datasetName) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetEntity datasetObj =
          session.load(DatasetEntity.class, datasetId, LockMode.PESSIMISTIC_WRITE);

      var dataset =
          Dataset.newBuilder()
              .setName(datasetName)
              .setWorkspaceId(datasetObj.getWorkspace())
              .setWorkspaceTypeValue(datasetObj.getWorkspace_type())
              .build();
      // Check entity already exists
      checkDatasetAlreadyExist(dataset);

      datasetObj.setName(datasetName);
      datasetObj.setTime_updated(Calendar.getInstance().getTimeInMillis());
      var transaction = session.beginTransaction();
      session.update(datasetObj);
      transaction.commit();
      LOGGER.debug(ModelDBMessages.DATASET_UPDATE_SUCCESSFULLY_MSG);
      return datasetObj.getProtoObject(mdbRoleService);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return updateDatasetName(datasetId, datasetName);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Dataset updateDatasetDescription(String datasetId, String datasetDescription) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetEntity datasetObj =
          session.load(DatasetEntity.class, datasetId, LockMode.PESSIMISTIC_WRITE);
      datasetObj.setDescription(datasetDescription);
      datasetObj.setTime_updated(Calendar.getInstance().getTimeInMillis());
      var transaction = session.beginTransaction();
      session.update(datasetObj);
      transaction.commit();
      LOGGER.debug(ModelDBMessages.DATASET_UPDATE_SUCCESSFULLY_MSG);
      return datasetObj.getProtoObject(mdbRoleService);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return updateDatasetDescription(datasetId, datasetDescription);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Dataset addDatasetTags(String datasetId, List<String> tagsList) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetEntity datasetObj =
          session.get(DatasetEntity.class, datasetId, LockMode.PESSIMISTIC_WRITE);
      if (datasetObj == null) {
        var errorMessage = "Dataset not found for given ID";
        throw new NotFoundException(errorMessage);
      }
      List<String> newTags = new ArrayList<>();
      var existingProtoDatasetObj = datasetObj.getProtoObject(mdbRoleService);
      for (String tag : tagsList) {
        if (!existingProtoDatasetObj.getTagsList().contains(tag)) {
          newTags.add(tag);
        }
      }
      if (!newTags.isEmpty()) {
        List<TagsMapping> newTagMappings =
            RdbmsUtils.convertTagListFromTagMappingList(datasetObj, newTags);
        datasetObj.getTags().addAll(newTagMappings);
        datasetObj.setTime_updated(Calendar.getInstance().getTimeInMillis());
        var transaction = session.beginTransaction();
        session.saveOrUpdate(datasetObj);
        transaction.commit();
      }
      LOGGER.debug("Dataset tags added successfully");
      return datasetObj.getProtoObject(mdbRoleService);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return addDatasetTags(datasetId, tagsList);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<String> getDatasetTags(String datasetId) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetEntity datasetObj = session.get(DatasetEntity.class, datasetId);
      LOGGER.debug("Got Dataset");
      return datasetObj.getProtoObject(mdbRoleService).getTagsList();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getDatasetTags(datasetId);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Dataset deleteDatasetTags(
      String datasetId, List<String> datasetTagList, Boolean deleteAll) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();
      var stringQueryBuilder = new StringBuilder("delete from TagsMapping tm WHERE ");
      if (deleteAll) {
        stringQueryBuilder.append(
            " tm.datasetEntity." + ModelDBConstants.ID + DATASET_ID_POST_QUERY_PARAM);
        var query =
            session
                .createQuery(stringQueryBuilder.toString())
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter(ModelDBConstants.DATASET_ID_STR, datasetId);
        query.executeUpdate();
      } else {
        stringQueryBuilder.append(" tm." + ModelDBConstants.TAGS + " in (:tags)");
        stringQueryBuilder.append(
            " AND tm.datasetEntity." + ModelDBConstants.ID + DATASET_ID_POST_QUERY_PARAM);
        var query =
            session
                .createQuery(stringQueryBuilder.toString())
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter("tags", datasetTagList);
        query.setParameter(ModelDBConstants.DATASET_ID_STR, datasetId);
        query.executeUpdate();
      }

      DatasetEntity datasetObj = session.get(DatasetEntity.class, datasetId);
      datasetObj.setTime_updated(Calendar.getInstance().getTimeInMillis());
      session.update(datasetObj);
      transaction.commit();
      LOGGER.debug("Dataset tags deleted successfully");
      return datasetObj.getProtoObject(mdbRoleService);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteDatasetTags(datasetId, datasetTagList, deleteAll);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Dataset addDatasetAttributes(String datasetId, List<KeyValue> attributesList) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetEntity datasetObj =
          session.get(DatasetEntity.class, datasetId, LockMode.PESSIMISTIC_WRITE);
      datasetObj.setAttributeMapping(
          RdbmsUtils.convertAttributesFromAttributeEntityList(
              datasetObj, ModelDBConstants.ATTRIBUTES, attributesList));
      datasetObj.setTime_updated(Calendar.getInstance().getTimeInMillis());
      var transaction = session.beginTransaction();
      session.saveOrUpdate(datasetObj);
      transaction.commit();
      LOGGER.debug("Dataset attributes added successfully");
      return datasetObj.getProtoObject(mdbRoleService);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return addDatasetAttributes(datasetId, attributesList);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Dataset updateDatasetAttributes(String datasetId, KeyValue attribute) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetEntity datasetObj =
          session.get(DatasetEntity.class, datasetId, LockMode.PESSIMISTIC_WRITE);
      if (datasetObj == null) {
        var errorMessage = "Dataset not found for given ID";
        throw new NotFoundException(errorMessage);
      }

      var updatedAttributeObj =
          RdbmsUtils.generateAttributeEntity(datasetObj, ModelDBConstants.ATTRIBUTES, attribute);

      List<AttributeEntity> existingAttributes = datasetObj.getAttributeMapping();
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
          datasetObj.setAttributeMapping(Collections.singletonList(updatedAttributeObj));
        }
      } else {
        datasetObj.setAttributeMapping(Collections.singletonList(updatedAttributeObj));
      }
      datasetObj.setTime_updated(Calendar.getInstance().getTimeInMillis());
      var transaction = session.beginTransaction();
      session.saveOrUpdate(datasetObj);
      transaction.commit();
      return datasetObj.getProtoObject(mdbRoleService);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return updateDatasetAttributes(datasetId, attribute);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<KeyValue> getDatasetAttributes(
      String datasetId, List<String> attributeKeyList, Boolean getAll) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      if (getAll) {
        DatasetEntity datasetObj = session.get(DatasetEntity.class, datasetId);
        return datasetObj.getProtoObject(mdbRoleService).getAttributesList();
      } else {
        var query = session.createQuery(GET_DATASET_ATTRIBUTES_QUERY);
        query.setParameterList("keys", attributeKeyList);
        query.setParameter(ModelDBConstants.DATASET_ID_STR, datasetId);
        query.setParameter("fieldType", ModelDBConstants.ATTRIBUTES);

        @SuppressWarnings("unchecked")
        List<AttributeEntity> attributeEntities = query.list();
        return RdbmsUtils.convertAttributeEntityListFromAttributes(attributeEntities);
      }
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getDatasetAttributes(datasetId, attributeKeyList, getAll);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Dataset deleteDatasetAttributes(
      String datasetId, List<String> attributeKeyList, Boolean deleteAll) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();

      var stringQueryBuilder = new StringBuilder("delete from AttributeEntity attr WHERE ");
      if (deleteAll) {
        stringQueryBuilder.append(
            " attr.datasetEntity." + ModelDBConstants.ID + DATASET_ID_POST_QUERY_PARAM);
        var query =
            session
                .createQuery(stringQueryBuilder.toString())
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter(ModelDBConstants.DATASET_ID_STR, datasetId);
        query.executeUpdate();
      } else {
        stringQueryBuilder.append(" attr." + ModelDBConstants.KEY + " in (:keys)");
        stringQueryBuilder.append(
            " AND attr.datasetEntity." + ModelDBConstants.ID + DATASET_ID_POST_QUERY_PARAM);
        var query =
            session
                .createQuery(stringQueryBuilder.toString())
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter("keys", attributeKeyList);
        query.setParameter(ModelDBConstants.DATASET_ID_STR, datasetId);
        query.executeUpdate();
      }
      DatasetEntity datasetObj = session.get(DatasetEntity.class, datasetId);
      datasetObj.setTime_updated(Calendar.getInstance().getTimeInMillis());
      session.update(datasetObj);
      transaction.commit();
      return datasetObj.getProtoObject(mdbRoleService);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteDatasetAttributes(datasetId, attributeKeyList, deleteAll);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Map<String, String> getOwnersByDatasetIds(List<String> datasetIds) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var query = session.createQuery(GET_DATASET_BY_IDS_QUERY);
      query.setParameterList("ids", datasetIds);

      @SuppressWarnings("unchecked")
      List<DatasetEntity> datasetEntities = query.list();
      LOGGER.debug("Got Dataset by Id");
      Map<String, String> datasetOwnersMap = new HashMap<>();
      for (DatasetEntity datasetEntity : datasetEntities) {
        datasetOwnersMap.put(datasetEntity.getId(), datasetEntity.getOwner());
      }
      return datasetOwnersMap;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getOwnersByDatasetIds(datasetIds);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<String> getWorkspaceDatasetIDs(String workspaceName, UserInfo currentLoginUserInfo) {
    if (!mdbRoleService.IsImplemented()) {
      try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
        return session.createQuery(NON_DELETED_DATASET_IDS).list();
      }
    } else {

      // get list of accessible datasets
      @SuppressWarnings("unchecked")
      Collection<String> accessibleDatasetIds =
          mdbRoleService.getAccessibleResourceIds(
              null,
              new CollaboratorUser(authService, currentLoginUserInfo),
              ModelDBServiceResourceTypes.DATASET,
              Collections.EMPTY_LIST);

      Set<String> accessibleResourceIds = new HashSet<>(accessibleDatasetIds);
      // in personal workspace show datasets directly shared
      if (workspaceName != null
          && !workspaceName.isEmpty()
          && workspaceName.equals(authService.getUsernameFromUserInfo(currentLoginUserInfo))) {
        LOGGER.debug("Workspace and current login user match");
        List<GetResourcesResponseItem> accessibleAllWorkspaceItems =
            mdbRoleService.getResourceItems(
                null, Collections.emptySet(), ModelDBServiceResourceTypes.DATASET, false);
        accessibleResourceIds.addAll(
            accessibleAllWorkspaceItems.stream()
                .map(GetResourcesResponseItem::getResourceId)
                .collect(Collectors.toSet()));
      } else if (workspaceName != null && !workspaceName.isEmpty()) {
        // get list of accessible datasets
        accessibleResourceIds =
            ModelDBUtils.filterWorkspaceOnlyAccessibleIds(
                mdbRoleService,
                accessibleResourceIds,
                workspaceName,
                currentLoginUserInfo,
                ModelDBServiceResourceTypes.DATASET);
      }

      LOGGER.debug("accessibleAllWorkspaceDatasetIds : {}", accessibleResourceIds);

      try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
        @SuppressWarnings("unchecked")
        Query<String> query = session.createQuery(NON_DELETED_DATASET_IDS_BY_IDS);
        query.setParameterList(ModelDBConstants.DATASET_IDS, accessibleDatasetIds);
        List<String> resultDatasets = query.list();
        LOGGER.debug(
            "Total accessible project Ids in function getWorkspaceDatasetIDs : {}", resultDatasets);
        return resultDatasets;
      }
    }
  }

  @Override
  public boolean datasetExistsInDB(String datasetId) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var query = session.createQuery(COUNT_DATASET_BY_ID_HQL);
      query.setParameter("datasetId", datasetId);
      Long projectCount = (Long) query.getSingleResult();
      return projectCount == 1L;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return datasetExistsInDB(datasetId);
      } else {
        throw ex;
      }
    }
  }
}
