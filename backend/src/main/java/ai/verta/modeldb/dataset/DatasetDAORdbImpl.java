package ai.verta.modeldb.dataset;

import ai.verta.common.KeyValue;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.OperatorEnum;
import ai.verta.common.ValueTypeEnum;
import ai.verta.common.WorkspaceTypeEnum.WorkspaceType;
import ai.verta.modeldb.Dataset;
import ai.verta.modeldb.DatasetVisibilityEnum.DatasetVisibility;
import ai.verta.modeldb.FindDatasets;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.collaborator.CollaboratorOrg;
import ai.verta.modeldb.collaborator.CollaboratorUser;
import ai.verta.modeldb.dto.DatasetPaginationDTO;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.DatasetEntity;
import ai.verta.modeldb.entities.TagsMapping;
import ai.verta.modeldb.telemetry.TelemetryUtils;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.uac.ModelDBActionEnum;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import ai.verta.uac.Organization;
import ai.verta.uac.Role;
import ai.verta.uac.RoleBinding;
import ai.verta.uac.UserInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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

public class DatasetDAORdbImpl implements DatasetDAO {

  private static final Logger LOGGER = LogManager.getLogger(DatasetDAORdbImpl.class);
  public static final String GLOBAL_SHARING = "_DATASET_GLOBAL_SHARING";
  private final AuthService authService;
  private final RoleService roleService;

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
  private static final String IDS_FILTERED_BY_WORKSPACE =
      NON_DELETED_DATASET_IDS_BY_IDS
          + " AND d."
          + ModelDBConstants.WORKSPACE
          + " = :"
          + ModelDBConstants.WORKSPACE
          + " AND d."
          + ModelDBConstants.WORKSPACE_TYPE
          + " = :"
          + ModelDBConstants.WORKSPACE_TYPE;

  public DatasetDAORdbImpl(AuthService authService, RoleService roleService) {
    this.authService = authService;
    this.roleService = roleService;
  }

  private void checkDatasetAlreadyExist(Dataset dataset) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ModelDBHibernateUtil.checkIfEntityAlreadyExists(
          session,
          "ds",
          CHECK_DATASE_QUERY_PREFIX,
          "Dataset",
          "datasetName",
          dataset.getName(),
          ModelDBConstants.WORKSPACE,
          dataset.getWorkspaceId(),
          dataset.getWorkspaceType(),
          LOGGER);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        checkDatasetAlreadyExist(dataset);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Dataset createDataset(Dataset dataset, UserInfo userInfo)
      throws InvalidProtocolBufferException {
    // Check entity already exists
    checkDatasetAlreadyExist(dataset);
    createRoleBindingsForDataset(dataset, userInfo);

    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetEntity datasetEntity = RdbmsUtils.generateDatasetEntity(dataset);
      Transaction transaction = session.beginTransaction();
      session.save(datasetEntity);
      transaction.commit();
      LOGGER.debug("Dataset created successfully");
      TelemetryUtils.insertModelDBDeploymentInfo();
      return datasetEntity.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return createDataset(dataset, userInfo);
      } else {
        throw ex;
      }
    }
  }

  private void createRoleBindingsForDataset(Dataset dataset, UserInfo userInfo) {
    Role ownerRole = roleService.getRoleByName(ModelDBConstants.ROLE_DATASET_OWNER, null);
    roleService.createRoleBinding(
        ownerRole,
        new CollaboratorUser(authService, userInfo),
        dataset.getId(),
        ModelDBServiceResourceTypes.DATASET);

    if (dataset.getDatasetVisibility().equals(DatasetVisibility.PUBLIC)) {
      roleService.createPublicRoleBinding(dataset.getId(), ModelDBServiceResourceTypes.DATASET);
    }

    createWorkspaceRoleBinding(
        dataset.getWorkspaceId(),
        dataset.getWorkspaceType(),
        dataset.getId(),
        dataset.getDatasetVisibility());
  }

  private void createWorkspaceRoleBinding(
      String workspaceId,
      WorkspaceType workspaceType,
      String datasetId,
      DatasetVisibility datasetVisibility) {
    if (workspaceId != null && !workspaceId.isEmpty()) {
      roleService.createWorkspaceRoleBinding(
          workspaceId,
          workspaceType,
          datasetId,
          ModelDBConstants.ROLE_DATASET_ADMIN,
          ModelDBServiceResourceTypes.DATASET,
          datasetVisibility.equals(DatasetVisibility.ORG_SCOPED_PUBLIC),
          GLOBAL_SHARING);
      switch (workspaceType) {
        case ORGANIZATION:
          if (datasetVisibility.equals(DatasetVisibility.ORG_SCOPED_PUBLIC)) {
            Role datasetRead =
                roleService.getRoleByName(ModelDBConstants.ROLE_DATASET_READ_ONLY, null);
            roleService.createRoleBinding(
                datasetRead,
                new CollaboratorOrg(workspaceId),
                datasetId,
                ModelDBServiceResourceTypes.DATASET);
          }
          break;
        case USER:
        default:
          break;
      }
    }
  }

  @Override
  public List<Dataset> getDatasetByIds(List<String> sharedDatasetIds)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      List<DatasetEntity> datasetEntities = getDatasetEntityList(session, sharedDatasetIds);
      LOGGER.debug("Got Dataset by Ids successfully");
      return RdbmsUtils.convertDatasetsFromDatasetEntityList(datasetEntities);
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
    Query query = session.createQuery(GET_DATASET_BY_IDS_QUERY);
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
      DatasetVisibility datasetVisibility)
      throws InvalidProtocolBufferException {
    FindDatasets findDatasets =
        FindDatasets.newBuilder()
            .setPageNumber(pageNumber)
            .setPageLimit(pageLimit)
            .setAscending(order)
            .setSortKey(sortKey)
            .build();
    return findDatasets(findDatasets, userInfo, datasetVisibility);
  }

  private List<String> getWorkspaceRoleBindings(
      String workspaceId,
      WorkspaceType workspaceType,
      String datasetId,
      DatasetVisibility datasetVisibility) {
    List<String> workspaceRoleBindings = new ArrayList<>();
    if (workspaceId != null && !workspaceId.isEmpty()) {
      switch (workspaceType) {
        case ORGANIZATION:
          if (datasetVisibility.equals(DatasetVisibility.ORG_SCOPED_PUBLIC)) {
            String orgDatasetReadRoleBindingName =
                roleService.buildRoleBindingName(
                    ModelDBConstants.ROLE_DATASET_READ_ONLY,
                    datasetId,
                    new CollaboratorOrg(workspaceId),
                    ModelDBServiceResourceTypes.DATASET.name());
            if (orgDatasetReadRoleBindingName != null && !orgDatasetReadRoleBindingName.isEmpty()) {
              workspaceRoleBindings.add(orgDatasetReadRoleBindingName);
            }
          }
          break;
        case USER:
        default:
          break;
      }
    }
    List<String> orgWorkspaceRoleBindings =
        roleService.getWorkspaceRoleBindings(
            workspaceId,
            workspaceType,
            datasetId,
            ModelDBConstants.ROLE_DATASET_ADMIN,
            ModelDBServiceResourceTypes.DATASET,
            datasetVisibility.equals(DatasetVisibility.ORG_SCOPED_PUBLIC),
            GLOBAL_SHARING);

    if (orgWorkspaceRoleBindings != null && !orgWorkspaceRoleBindings.isEmpty()) {
      workspaceRoleBindings.addAll(orgWorkspaceRoleBindings);
    }
    return workspaceRoleBindings;
  }

  @Override
  public Boolean deleteDatasets(List<String> datasetIds) {
    // Get self allowed resources id where user has delete permission
    List<String> allowedDatasetIds =
        roleService.getAccessibleResourceIdsByActions(
            ModelDBServiceResourceTypes.DATASET,
            ModelDBActionEnum.ModelDBServiceActions.DELETE,
            datasetIds);
    if (allowedDatasetIds.isEmpty()) {
      Status status =
          Status.newBuilder()
              .setCode(Code.PERMISSION_DENIED_VALUE)
              .setMessage("Access Denied for given dataset Ids : " + datasetIds)
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }

    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      Query deletedDatasetsQuery = session.createQuery(DELETED_STATUS_DATASET_QUERY_STRING);
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
  public Dataset getDatasetById(String datasetId) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetEntity datasetObj = getDatasetEntity(session, datasetId);
      return datasetObj.getProtoObject();
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
      Status status =
          Status.newBuilder()
              .setCode(Code.NOT_FOUND_VALUE)
              .setMessage("dataset with input id not found")
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
    LOGGER.debug(ModelDBMessages.DATASET_UPDATE_SUCCESSFULLY_MSG);
    return datasetObj;
  }

  @Override
  public DatasetPaginationDTO findDatasets(
      FindDatasets queryParameters,
      UserInfo currentLoginUserInfo,
      DatasetVisibility datasetVisibility)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {

      List<String> accessibleDatasetIds =
          roleService.getAccessibleResourceIds(
              null,
              new CollaboratorUser(authService, currentLoginUserInfo),
              datasetVisibility,
              ModelDBServiceResourceTypes.DATASET,
              queryParameters.getDatasetIdsList());

      if (accessibleDatasetIds.isEmpty() && roleService.IsImplemented()) {
        LOGGER.debug("Accessible Dataset Ids not found, size 0");
        DatasetPaginationDTO datasetPaginationDTO = new DatasetPaginationDTO();
        datasetPaginationDTO.setDatasets(Collections.emptyList());
        datasetPaginationDTO.setTotalRecords(0L);
        return datasetPaginationDTO;
      }

      CriteriaBuilder builder = session.getCriteriaBuilder();
      // Using FROM and JOIN
      CriteriaQuery<DatasetEntity> criteriaQuery = builder.createQuery(DatasetEntity.class);
      Root<DatasetEntity> datasetRoot = criteriaQuery.from(DatasetEntity.class);
      datasetRoot.alias("ds");
      List<Predicate> finalPredicatesList = new ArrayList<>();

      List<KeyValueQuery> predicates = new ArrayList<>(queryParameters.getPredicatesList());
      for (KeyValueQuery predicate : predicates) {
        // Validate if current user has access to the entity or not where predicate key has an id
        RdbmsUtils.validatePredicates(
            ModelDBConstants.DATASETS, accessibleDatasetIds, predicate, roleService);
      }

      String workspaceName = queryParameters.getWorkspaceName();

      if (workspaceName != null
          && !workspaceName.isEmpty()
          && workspaceName.equals(authService.getUsernameFromUserInfo(currentLoginUserInfo))) {
        accessibleDatasetIds =
            roleService.getSelfDirectlyAllowedResources(
                ModelDBServiceResourceTypes.DATASET, ModelDBActionEnum.ModelDBServiceActions.READ);
        if (queryParameters.getDatasetIdsList() != null
            && !queryParameters.getDatasetIdsList().isEmpty()) {
          accessibleDatasetIds.retainAll(queryParameters.getDatasetIdsList());
        }
        // user is in his workspace and has no datasets, return empty
        if (accessibleDatasetIds.isEmpty()) {
          DatasetPaginationDTO datasetPaginationDTO = new DatasetPaginationDTO();
          datasetPaginationDTO.setDatasets(Collections.emptyList());
          datasetPaginationDTO.setTotalRecords(0L);
          return datasetPaginationDTO;
        }

        List<String> orgIds =
            roleService.listMyOrganizations().stream()
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
        if (datasetVisibility.equals(DatasetVisibility.PRIVATE)) {
          List<KeyValueQuery> workspacePredicates =
              ModelDBUtils.getKeyValueQueriesByWorkspace(
                  roleService, currentLoginUserInfo, workspaceName);
          if (workspacePredicates.size() > 0) {
            Predicate privateWorkspacePredicate =
                builder.equal(
                    datasetRoot.get(ModelDBConstants.WORKSPACE),
                    workspacePredicates.get(0).getValue().getStringValue());
            Predicate privateWorkspaceTypePredicate =
                builder.equal(
                    datasetRoot.get(ModelDBConstants.WORKSPACE_TYPE),
                    workspacePredicates.get(1).getValue().getNumberValue());
            Predicate privatePredicate =
                builder.and(privateWorkspacePredicate, privateWorkspaceTypePredicate);

            finalPredicatesList.add(privatePredicate);
          }
        }
      }

      if (!accessibleDatasetIds.isEmpty()) {
        Expression<String> exp = datasetRoot.get(ModelDBConstants.ID);
        Predicate predicate2 = exp.in(accessibleDatasetIds);
        finalPredicatesList.add(predicate2);
      }

      String entityName = "datasetEntity";
      try {
        List<Predicate> queryPredicatesList =
            RdbmsUtils.getQueryPredicatesFromPredicateList(
                entityName, predicates, builder, criteriaQuery, datasetRoot, authService);
        if (!queryPredicatesList.isEmpty()) {
          finalPredicatesList.addAll(queryPredicatesList);
        }
      } catch (ModelDBException ex) {
        if (ex.getCode().ordinal() == Code.FAILED_PRECONDITION_VALUE
            && ModelDBConstants.INTERNAL_MSG_USERS_NOT_FOUND.equals(ex.getMessage())) {
          LOGGER.info(ex.getMessage());
          DatasetPaginationDTO datasetPaginationDTO = new DatasetPaginationDTO();
          datasetPaginationDTO.setDatasets(Collections.emptyList());
          datasetPaginationDTO.setTotalRecords(0L);
          return datasetPaginationDTO;
        }
      }

      finalPredicatesList.add(builder.equal(datasetRoot.get(ModelDBConstants.DELETED), false));

      String sortBy = queryParameters.getSortKey();
      if (sortBy == null || sortBy.isEmpty()) {
        sortBy = ModelDBConstants.TIME_UPDATED;
      }

      Order orderBy =
          RdbmsUtils.getOrderBasedOnSortKey(
              sortBy, queryParameters.getAscending(), builder, datasetRoot, entityName);

      Predicate[] predicateArr = new Predicate[finalPredicatesList.size()];
      for (int index = 0; index < finalPredicatesList.size(); index++) {
        predicateArr[index] = finalPredicatesList.get(index);
      }

      Predicate predicateWhereCause = builder.and(predicateArr);
      criteriaQuery.select(datasetRoot);
      criteriaQuery.where(predicateWhereCause);
      criteriaQuery.orderBy(orderBy);

      Query query = session.createQuery(criteriaQuery);
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
        datasetList = RdbmsUtils.convertDatasetsFromDatasetEntityList(datasetEntities);
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

      DatasetPaginationDTO datasetPaginationDTO = new DatasetPaginationDTO();
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
  public List<Dataset> getDatasets(String key, String value, UserInfo userInfo)
      throws InvalidProtocolBufferException {
    FindDatasets findDatasets =
        FindDatasets.newBuilder()
            .addPredicates(
                KeyValueQuery.newBuilder()
                    .setKey(key)
                    .setValue(Value.newBuilder().setStringValue(value).build())
                    .setOperator(OperatorEnum.Operator.EQ)
                    .setValueType(ValueTypeEnum.ValueType.STRING)
                    .build())
            .build();
    DatasetPaginationDTO datasetPaginationDTO =
        findDatasets(findDatasets, userInfo, DatasetVisibility.PRIVATE);
    LOGGER.debug("Datasets size is {}", datasetPaginationDTO.getDatasets().size());
    return datasetPaginationDTO.getDatasets();
  }

  @Override
  public Dataset updateDatasetName(String datasetId, String datasetName)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetEntity datasetObj = session.load(DatasetEntity.class, datasetId);

      Dataset dataset =
          Dataset.newBuilder()
              .setName(datasetName)
              .setWorkspaceId(datasetObj.getWorkspace())
              .setWorkspaceTypeValue(datasetObj.getWorkspace_type())
              .build();
      // Check entity already exists
      checkDatasetAlreadyExist(dataset);

      datasetObj.setName(datasetName);
      datasetObj.setTime_updated(Calendar.getInstance().getTimeInMillis());
      Transaction transaction = session.beginTransaction();
      session.update(datasetObj);
      transaction.commit();
      LOGGER.debug(ModelDBMessages.DATASET_UPDATE_SUCCESSFULLY_MSG);
      return datasetObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return updateDatasetName(datasetId, datasetName);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Dataset updateDatasetDescription(String datasetId, String datasetDescription)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetEntity datasetObj = session.load(DatasetEntity.class, datasetId);
      datasetObj.setDescription(datasetDescription);
      datasetObj.setTime_updated(Calendar.getInstance().getTimeInMillis());
      Transaction transaction = session.beginTransaction();
      session.update(datasetObj);
      transaction.commit();
      LOGGER.debug(ModelDBMessages.DATASET_UPDATE_SUCCESSFULLY_MSG);
      return datasetObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return updateDatasetDescription(datasetId, datasetDescription);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Dataset addDatasetTags(String datasetId, List<String> tagsList)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetEntity datasetObj = session.get(DatasetEntity.class, datasetId);
      if (datasetObj == null) {
        String errorMessage = "Dataset not found for given ID";
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      List<String> newTags = new ArrayList<>();
      Dataset existingProtoDatasetObj = datasetObj.getProtoObject();
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
        Transaction transaction = session.beginTransaction();
        session.saveOrUpdate(datasetObj);
        transaction.commit();
      }
      LOGGER.debug("Dataset tags added successfully");
      return datasetObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return addDatasetTags(datasetId, tagsList);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<String> getDatasetTags(String datasetId) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetEntity datasetObj = session.get(DatasetEntity.class, datasetId);
      LOGGER.debug("Got Dataset");
      return datasetObj.getProtoObject().getTagsList();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getDatasetTags(datasetId);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Dataset deleteDatasetTags(String datasetId, List<String> datasetTagList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      StringBuilder stringQueryBuilder = new StringBuilder("delete from TagsMapping tm WHERE ");
      if (deleteAll) {
        stringQueryBuilder.append(" tm.datasetEntity." + ModelDBConstants.ID + " = :datasetId");
        Query query = session.createQuery(stringQueryBuilder.toString());
        query.setParameter(ModelDBConstants.DATASET_ID_STR, datasetId);
        query.executeUpdate();
      } else {
        stringQueryBuilder.append(" tm." + ModelDBConstants.TAGS + " in (:tags)");
        stringQueryBuilder.append(" AND tm.datasetEntity." + ModelDBConstants.ID + " = :datasetId");
        Query query = session.createQuery(stringQueryBuilder.toString());
        query.setParameter("tags", datasetTagList);
        query.setParameter(ModelDBConstants.DATASET_ID_STR, datasetId);
        query.executeUpdate();
      }

      DatasetEntity datasetObj = session.get(DatasetEntity.class, datasetId);
      datasetObj.setTime_updated(Calendar.getInstance().getTimeInMillis());
      session.update(datasetObj);
      transaction.commit();
      LOGGER.debug("Dataset tags deleted successfully");
      return datasetObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteDatasetTags(datasetId, datasetTagList, deleteAll);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Dataset addDatasetAttributes(String datasetId, List<KeyValue> attributesList)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetEntity datasetObj = session.get(DatasetEntity.class, datasetId);
      datasetObj.setAttributeMapping(
          RdbmsUtils.convertAttributesFromAttributeEntityList(
              datasetObj, ModelDBConstants.ATTRIBUTES, attributesList));
      datasetObj.setTime_updated(Calendar.getInstance().getTimeInMillis());
      Transaction transaction = session.beginTransaction();
      session.saveOrUpdate(datasetObj);
      transaction.commit();
      LOGGER.debug("Dataset attributes added successfully");
      return datasetObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return addDatasetAttributes(datasetId, attributesList);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Dataset updateDatasetAttributes(String datasetId, KeyValue attribute)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetEntity datasetObj = session.get(DatasetEntity.class, datasetId);
      if (datasetObj == null) {
        String errorMessage = "Dataset not found for given ID";
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      AttributeEntity updatedAttributeObj =
          RdbmsUtils.generateAttributeEntity(datasetObj, ModelDBConstants.ATTRIBUTES, attribute);

      List<AttributeEntity> existingAttributes = datasetObj.getAttributeMapping();
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
          datasetObj.setAttributeMapping(Collections.singletonList(updatedAttributeObj));
        }
      } else {
        datasetObj.setAttributeMapping(Collections.singletonList(updatedAttributeObj));
      }
      datasetObj.setTime_updated(Calendar.getInstance().getTimeInMillis());
      Transaction transaction = session.beginTransaction();
      session.saveOrUpdate(datasetObj);
      transaction.commit();
      return datasetObj.getProtoObject();
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
      String datasetId, List<String> attributeKeyList, Boolean getAll)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      if (getAll) {
        DatasetEntity datasetObj = session.get(DatasetEntity.class, datasetId);
        return datasetObj.getProtoObject().getAttributesList();
      } else {
        Query query = session.createQuery(GET_DATASET_ATTRIBUTES_QUERY);
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
      String datasetId, List<String> attributeKeyList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();

      StringBuilder stringQueryBuilder =
          new StringBuilder("delete from AttributeEntity attr WHERE ");
      if (deleteAll) {
        stringQueryBuilder.append(" attr.datasetEntity." + ModelDBConstants.ID + " = :datasetId");
        Query query = session.createQuery(stringQueryBuilder.toString());
        query.setParameter(ModelDBConstants.DATASET_ID_STR, datasetId);
        query.executeUpdate();
      } else {
        stringQueryBuilder.append(" attr." + ModelDBConstants.KEY + " in (:keys)");
        stringQueryBuilder.append(
            " AND attr.datasetEntity." + ModelDBConstants.ID + " = :datasetId");
        Query query = session.createQuery(stringQueryBuilder.toString());
        query.setParameter("keys", attributeKeyList);
        query.setParameter(ModelDBConstants.DATASET_ID_STR, datasetId);
        query.executeUpdate();
      }
      DatasetEntity datasetObj = session.get(DatasetEntity.class, datasetId);
      datasetObj.setTime_updated(Calendar.getInstance().getTimeInMillis());
      session.update(datasetObj);
      transaction.commit();
      return datasetObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteDatasetAttributes(datasetId, attributeKeyList, deleteAll);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Dataset setDatasetVisibility(String datasetId, DatasetVisibility datasetVisibility)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetEntity datasetEntity = session.load(DatasetEntity.class, datasetId);

      Integer oldVisibilityInt = datasetEntity.getDataset_visibility();
      DatasetVisibility oldVisibility = DatasetVisibility.PRIVATE;
      if (oldVisibilityInt != null) {
        oldVisibility = DatasetVisibility.forNumber(oldVisibilityInt);
      }
      if (!oldVisibility.equals(datasetVisibility)) {
        datasetEntity.setDataset_visibility(datasetVisibility.ordinal());
        datasetEntity.setTime_updated(Calendar.getInstance().getTimeInMillis());
        Transaction transaction = session.beginTransaction();
        session.update(datasetEntity);
        transaction.commit();
        // FIXME: RoleBinding modification is outside Transaction and can lead to consistency
        deleteOldVisibilityBasedBinding(
            oldVisibility,
            datasetId,
            datasetEntity.getWorkspace_type(),
            datasetEntity.getWorkspace());
        createNewVisibilityBasedBinding(
            datasetVisibility,
            datasetId,
            datasetEntity.getWorkspace_type(),
            datasetEntity.getWorkspace());
      }

      LOGGER.debug("Dataset by Id getting successfully");
      return datasetEntity.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return setDatasetVisibility(datasetId, datasetVisibility);
      } else {
        throw ex;
      }
    }
  }

  private void createNewVisibilityBasedBinding(
      DatasetVisibility newVisibility,
      String datasetId,
      int datasetWorkspaceType,
      String workspaceId) {
    switch (newVisibility) {
      case ORG_SCOPED_PUBLIC:
        if (datasetWorkspaceType == WorkspaceType.ORGANIZATION_VALUE) {
          Role datasetRead =
              roleService.getRoleByName(ModelDBConstants.ROLE_DATASET_READ_ONLY, null);
          roleService.createRoleBinding(
              datasetRead,
              new CollaboratorOrg(workspaceId),
              datasetId,
              ModelDBServiceResourceTypes.DATASET);
        }
        break;
      case PUBLIC:
        roleService.createPublicRoleBinding(datasetId, ModelDBServiceResourceTypes.DATASET);
        break;
      case PRIVATE:
      case UNRECOGNIZED:
        break;
    }
  }

  private void deleteOldVisibilityBasedBinding(
      DatasetVisibility oldVisibility,
      String datasetId,
      int datasetWorkspaceType,
      String workspaceId) {
    switch (oldVisibility) {
      case ORG_SCOPED_PUBLIC:
        if (datasetWorkspaceType == WorkspaceType.ORGANIZATION_VALUE) {
          String roleBindingName =
              roleService.buildReadOnlyRoleBindingName(
                  datasetId, new CollaboratorOrg(workspaceId), ModelDBServiceResourceTypes.DATASET);
          RoleBinding roleBinding = roleService.getRoleBindingByName(roleBindingName);
          if (roleBinding != null && !roleBinding.getId().isEmpty()) {
            roleService.deleteRoleBinding(roleBinding.getId());
          }
        }
        break;
      case PUBLIC:
        String roleBindingName =
            roleService.buildPublicRoleBindingName(datasetId, ModelDBServiceResourceTypes.DATASET);
        RoleBinding publicReadRoleBinding = roleService.getRoleBindingByName(roleBindingName);
        if (publicReadRoleBinding != null && !publicReadRoleBinding.getId().isEmpty()) {
          roleService.deleteRoleBinding(publicReadRoleBinding.getId());
        }
        break;
      case PRIVATE:
      case UNRECOGNIZED:
        break;
    }
  }

  @Override
  public Map<String, String> getOwnersByDatasetIds(List<String> datasetIds) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query query = session.createQuery(GET_DATASET_BY_IDS_QUERY);
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
  public Dataset setDatasetWorkspace(String datasetId, WorkspaceDTO workspaceDTO)
      throws InvalidProtocolBufferException {

    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetEntity datasetEntity = session.load(DatasetEntity.class, datasetId);
      getWorkspaceRoleBindings(
          datasetEntity.getWorkspace(),
          WorkspaceType.forNumber(datasetEntity.getWorkspace_type()),
          datasetId,
          DatasetVisibility.forNumber(datasetEntity.getDataset_visibility()));
      createWorkspaceRoleBinding(
          workspaceDTO.getWorkspaceId(),
          workspaceDTO.getWorkspaceType(),
          datasetId,
          DatasetVisibility.forNumber(datasetEntity.getDataset_visibility()));
      datasetEntity.setWorkspace(workspaceDTO.getWorkspaceId());
      datasetEntity.setWorkspace_type(workspaceDTO.getWorkspaceType().getNumber());
      datasetEntity.setTime_updated(Calendar.getInstance().getTimeInMillis());
      Transaction transaction = session.beginTransaction();
      session.update(datasetEntity);
      transaction.commit();
      LOGGER.debug("Dataset workspace updated successfully");
      return datasetEntity.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return setDatasetWorkspace(datasetId, workspaceDTO);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<String> getWorkspaceDatasetIDs(String workspaceName, UserInfo currentLoginUserInfo)
      throws InvalidProtocolBufferException {
    if (!roleService.IsImplemented()) {
      try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
        return session.createQuery(NON_DELETED_DATASET_IDS).list();
      }
    } else {

      // get list of accessible datasets
      @SuppressWarnings("unchecked")
      List<String> accessibleDatasetIds =
          roleService.getAccessibleResourceIds(
              null,
              new CollaboratorUser(authService, currentLoginUserInfo),
              DatasetVisibility.PRIVATE,
              ModelDBServiceResourceTypes.DATASET,
              Collections.EMPTY_LIST);

      // resolve workspace
      WorkspaceDTO workspaceDTO =
          roleService.getWorkspaceDTOByWorkspaceName(currentLoginUserInfo, workspaceName);

      List<String> resultDatasets = new LinkedList<String>();
      try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
        @SuppressWarnings("unchecked")
        Query<String> query = session.createQuery(IDS_FILTERED_BY_WORKSPACE);
        query.setParameterList(ModelDBConstants.DATASET_IDS, accessibleDatasetIds);
        query.setParameter(ModelDBConstants.WORKSPACE, workspaceDTO.getWorkspaceId());
        query.setParameter(
            ModelDBConstants.WORKSPACE_TYPE, workspaceDTO.getWorkspaceType().getNumber());
        resultDatasets = query.list();

        // in personal workspace show datasets directly shared
        if (workspaceName.equals(authService.getUsernameFromUserInfo(currentLoginUserInfo))) {
          List<String> directlySharedDatasets =
              roleService.getSelfDirectlyAllowedResources(
                  ModelDBServiceResourceTypes.DATASET, ModelDBServiceActions.READ);
          query = session.createQuery(NON_DELETED_DATASET_IDS_BY_IDS);
          query.setParameterList(ModelDBConstants.DATASET_IDS, directlySharedDatasets);
          resultDatasets.addAll(query.list());
        }
      }
      return resultDatasets;
    }
  }

  @Override
  public boolean datasetExistsInDB(String datasetId) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query query = session.createQuery(COUNT_DATASET_BY_ID_HQL);
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
