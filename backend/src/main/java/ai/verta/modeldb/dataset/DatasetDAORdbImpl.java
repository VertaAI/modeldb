package ai.verta.modeldb.dataset;

import ai.verta.common.KeyValue;
import ai.verta.common.ValueTypeEnum;
import ai.verta.modeldb.Dataset;
import ai.verta.modeldb.DatasetVisibilityEnum.DatasetVisibility;
import ai.verta.modeldb.FindDatasets;
import ai.verta.modeldb.KeyValueQuery;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.OperatorEnum;
import ai.verta.modeldb.WorkspaceTypeEnum.WorkspaceType;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.collaborator.CollaboratorOrg;
import ai.verta.modeldb.collaborator.CollaboratorUser;
import ai.verta.modeldb.dto.DatasetPaginationDTO;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.DatasetEntity;
import ai.verta.modeldb.entities.DatasetVersionEntity;
import ai.verta.modeldb.entities.TagsMapping;
import ai.verta.modeldb.telemetry.TelemetryUtils;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.uac.ModelDBActionEnum;
import ai.verta.uac.ModelResourceEnum.ModelDBServiceResourceTypes;
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
  private final AuthService authService;
  private final RoleService roleService;

  // Queries
  private static final String GET_DATASET_BY_IDS_QUERY =
      "From DatasetEntity ds where ds.id IN (:ids)";
  private static final String UPDATE_TIME_QUERY =
      new StringBuilder("UPDATE DatasetEntity ds SET ds.")
          .append(ModelDBConstants.TIME_UPDATED)
          .append(" = :timestamp where ds.")
          .append(ModelDBConstants.ID)
          .append(" IN (:ids) ")
          .toString();
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
  private static final String DATASET_VERSION_BY_DATA_SET_IDS_QUERY =
      "From DatasetVersionEntity ds where ds.dataset_id IN (:datasetIds) ";

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
    }
  }

  @Override
  public Dataset createDataset(Dataset dataset, UserInfo userInfo)
      throws InvalidProtocolBufferException {
    // Check entity already exists
    checkDatasetAlreadyExist(dataset);

    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      DatasetEntity datasetEntity = RdbmsUtils.generateDatasetEntity(dataset);
      session.save(datasetEntity);

      Role ownerRole = roleService.getRoleByName(ModelDBConstants.ROLE_DATASET_OWNER, null);
      roleService.createRoleBinding(
          ownerRole,
          new CollaboratorUser(authService, userInfo),
          dataset.getId(),
          ModelDBServiceResourceTypes.DATASET);
      if (dataset.getDatasetVisibility().equals(DatasetVisibility.PUBLIC)) {
        Role publicReadRole =
            roleService.getRoleByName(ModelDBConstants.ROLE_DATASET_PUBLIC_READ, null);
        UserInfo unsignedUser = authService.getUnsignedUser();
        roleService.createRoleBinding(
            publicReadRole,
            new CollaboratorUser(authService, unsignedUser),
            dataset.getId(),
            ModelDBServiceResourceTypes.DATASET);
      }

      createWorkspaceRoleBinding(
          dataset.getWorkspaceId(),
          dataset.getWorkspaceType(),
          dataset.getId(),
          dataset.getDatasetVisibility());

      transaction.commit();
      LOGGER.debug("Dataset created successfully");
      TelemetryUtils.insertModelDBDeploymentInfo();
      return datasetEntity.getProtoObject();
    }
  }

  private void createWorkspaceRoleBinding(
      String workspaceId,
      WorkspaceType workspaceType,
      String datasetId,
      DatasetVisibility datasetVisibility) {
    if (workspaceId != null && !workspaceId.isEmpty()) {
      Role datasetAdmin = roleService.getRoleByName(ModelDBConstants.ROLE_DATASET_ADMIN, null);
      Role datasetRead = roleService.getRoleByName(ModelDBConstants.ROLE_DATASET_READ_ONLY, null);
      switch (workspaceType) {
        case ORGANIZATION:
          Organization org = (Organization) roleService.getOrgById(workspaceId);
          roleService.createRoleBinding(
              datasetAdmin,
              new CollaboratorUser(authService, org.getOwnerId()),
              datasetId,
              ModelDBServiceResourceTypes.DATASET);
          if (datasetVisibility.equals(DatasetVisibility.ORG_SCOPED_PUBLIC)) {
            roleService.createRoleBinding(
                datasetRead,
                new CollaboratorOrg(org.getId()),
                datasetId,
                ModelDBServiceResourceTypes.DATASET);
          }
          break;
        case USER:
          roleService.createRoleBinding(
              datasetAdmin,
              new CollaboratorUser(authService, workspaceId),
              datasetId,
              ModelDBServiceResourceTypes.DATASET);
          break;
        default:
          break;
      }
    }
  }

  @Override
  public List<Dataset> getDatasetByIds(List<String> sharedDatasetIds)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      Query query = session.createQuery(GET_DATASET_BY_IDS_QUERY);
      query.setParameterList("ids", sharedDatasetIds);

      @SuppressWarnings("unchecked")
      List<DatasetEntity> datasetEntities = query.list();
      transaction.commit();
      LOGGER.debug("Got Dataset by Ids successfully");
      return RdbmsUtils.convertDatasetsFromDatasetEntityList(datasetEntities);
    }
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

  private void deleteRoleBindingsOfAccessibleDatasets(List<String> allowedDatasetIds)
      throws InvalidProtocolBufferException {
    List<Dataset> allowedDatasets = getDatasetByIds(allowedDatasetIds);
    UserInfo unsignedUser = authService.getUnsignedUser();
    for (Dataset dataset : allowedDatasets) {
      String datasetId = dataset.getId();
      String ownerRoleBindingName =
          roleService.buildRoleBindingName(
              ModelDBConstants.ROLE_DATASET_OWNER,
              datasetId,
              dataset.getOwner(),
              ModelDBServiceResourceTypes.DATASET.name());
      RoleBinding roleBinding = roleService.getRoleBindingByName(ownerRoleBindingName);
      if (roleBinding != null && !roleBinding.getId().isEmpty()) {
        roleService.deleteRoleBinding(roleBinding.getId());
      }

      if (dataset.getDatasetVisibility().equals(DatasetVisibility.PUBLIC)) {
        String publicReadRoleBindingName =
            roleService.buildRoleBindingName(
                ModelDBConstants.ROLE_DATASET_PUBLIC_READ,
                datasetId,
                authService.getVertaIdFromUserInfo(unsignedUser),
                ModelDBServiceResourceTypes.DATASET.name());
        RoleBinding publicReadRoleBinding =
            roleService.getRoleBindingByName(publicReadRoleBindingName);
        if (publicReadRoleBinding != null && !publicReadRoleBinding.getId().isEmpty()) {
          roleService.deleteRoleBinding(publicReadRoleBinding.getId());
        }
      }

      // Remove all dataset collaborators
      roleService.removeResourceRoleBindings(
          datasetId, dataset.getOwner(), ModelDBServiceResourceTypes.DATASET);

      // Delete workspace based roleBindings
      deleteWorkspaceRoleBindings(
          dataset.getWorkspaceId(),
          dataset.getWorkspaceType(),
          dataset.getId(),
          dataset.getDatasetVisibility());
    }
  }

  private void deleteWorkspaceRoleBindings(
      String workspaceId,
      WorkspaceType workspaceType,
      String datasetId,
      DatasetVisibility datasetVisibility) {
    if (workspaceId != null && !workspaceId.isEmpty()) {
      switch (workspaceType) {
        case ORGANIZATION:
          Organization org = (Organization) roleService.getOrgById(workspaceId);
          String datasetAdminRoleBindingName =
              roleService.buildRoleBindingName(
                  ModelDBConstants.ROLE_DATASET_ADMIN,
                  datasetId,
                  new CollaboratorUser(authService, org.getOwnerId()),
                  ModelDBServiceResourceTypes.DATASET.name());
          RoleBinding datasetAdminRoleBinding =
              roleService.getRoleBindingByName(datasetAdminRoleBindingName);
          if (datasetAdminRoleBinding != null && !datasetAdminRoleBinding.getId().isEmpty()) {
            roleService.deleteRoleBinding(datasetAdminRoleBinding.getId());
          }
          if (datasetVisibility.equals(DatasetVisibility.ORG_SCOPED_PUBLIC)) {
            String orgDatasetReadRoleBindingName =
                roleService.buildRoleBindingName(
                    ModelDBConstants.ROLE_DATASET_READ_ONLY,
                    datasetId,
                    new CollaboratorOrg(org.getId()),
                    ModelDBServiceResourceTypes.DATASET.name());
            RoleBinding orgDatasetReadRoleBinding =
                roleService.getRoleBindingByName(orgDatasetReadRoleBindingName);
            if (orgDatasetReadRoleBinding != null && !orgDatasetReadRoleBinding.getId().isEmpty()) {
              roleService.deleteRoleBinding(orgDatasetReadRoleBinding.getId());
            }
          }
          break;
        case USER:
          String datasetRoleBindingName =
              roleService.buildRoleBindingName(
                  ModelDBConstants.ROLE_DATASET_ADMIN,
                  datasetId,
                  new CollaboratorUser(authService, workspaceId),
                  ModelDBServiceResourceTypes.DATASET.name());
          RoleBinding datasetRoleBinding = roleService.getRoleBindingByName(datasetRoleBindingName);
          if (datasetRoleBinding != null && !datasetRoleBinding.getId().isEmpty()) {
            roleService.deleteRoleBinding(datasetRoleBinding.getId());
          }
          break;
        default:
          break;
      }
    }
  }

  public void deleteDatasetVersionsByDatasetIDs(Session session, List<String> datasetIds) {
    Transaction transaction = session.beginTransaction();
    Query query = session.createQuery(DATASET_VERSION_BY_DATA_SET_IDS_QUERY);
    query.setParameterList("datasetIds", datasetIds);
    List<DatasetVersionEntity> datasetVersionEntities = query.list();
    for (DatasetVersionEntity datasetVersionEntity : datasetVersionEntities) {
      session.delete(datasetVersionEntity);
    }
    transaction.commit();
    LOGGER.debug("DatasetVersion deleted successfully");
  }

  @Override
  public Boolean deleteDatasets(List<String> datasetIds) throws InvalidProtocolBufferException {
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

    // Remove roleBindings by accessible datasets
    deleteRoleBindingsOfAccessibleDatasets(allowedDatasetIds);

    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      deleteDatasetVersionsByDatasetIDs(session, allowedDatasetIds);

      Transaction transaction = session.beginTransaction();
      // Remove dataset collaborator mappings
      for (String datasetId : allowedDatasetIds) {
        DatasetEntity datasetObj = session.load(DatasetEntity.class, datasetId);
        session.delete(datasetObj);
      }
      transaction.commit();
      LOGGER.debug("Dataset deleted successfully");
      return true;
    }
  }

  @Override
  public Boolean setUpdateTime(List<String> datasetIds, long timestamp) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      Query query = session.createQuery(UPDATE_TIME_QUERY);
      query.setParameter("timestamp", timestamp);
      query.setParameterList("ids", datasetIds);
      int result = query.executeUpdate();
      transaction.commit();
      LOGGER.debug(ModelDBMessages.DATASET_UPDATE_SUCCESSFULLY_MSG);
      return result > 0;
    }
  }

  @Override
  public Dataset getDatasetById(String datasetId) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
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
      return datasetObj.getProtoObject();
    }
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
        // Validate if current user has access to the entity or not where predicate key has a
        // datasetId
        if (predicate.getKey().equals(ModelDBConstants.ID)) {
          if (!predicate.getOperator().equals(OperatorEnum.Operator.EQ)) {
            Status statusMessage =
                Status.newBuilder()
                    .setCode(Code.INVALID_ARGUMENT_VALUE)
                    .setMessage("Unknown 'Operator' type recognized, valid 'Operator' type is EQ")
                    .build();
            throw StatusProto.toStatusRuntimeException(statusMessage);
          }
          String datasetId = predicate.getValue().getStringValue();
          if (accessibleDatasetIds.isEmpty() || !accessibleDatasetIds.contains(datasetId)) {
            Status statusMessage =
                Status.newBuilder()
                    .setCode(Code.PERMISSION_DENIED_VALUE)
                    .setMessage(
                        "Access is denied. User is unauthorized for given Dataset entity ID : "
                            + datasetId)
                    .build();
            throw StatusProto.toStatusRuntimeException(statusMessage);
          }
        }

        if (predicate.getKey().equalsIgnoreCase(ModelDBConstants.WORKSPACE)
            || predicate.getKey().equalsIgnoreCase(ModelDBConstants.WORKSPACE_NAME)
            || predicate.getKey().equalsIgnoreCase(ModelDBConstants.WORKSPACE_TYPE)) {
          Status statusMessage =
              Status.newBuilder()
                  .setCode(Code.INVALID_ARGUMENT_VALUE)
                  .setMessage("Workspace name OR type not supported as predicate")
                  .build();
          throw StatusProto.toStatusRuntimeException(statusMessage);
        }
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
        finalPredicatesList.add(
            builder.or(
                predicate2,
                builder.equal(
                    datasetRoot.get(ModelDBConstants.DATASET_VISIBILITY),
                    DatasetVisibility.PUBLIC.getNumber())));
      }

      String entityName = "datasetEntity";
      List<Predicate> queryPredicatesList =
          RdbmsUtils.getQueryPredicatesFromPredicateList(
              entityName, predicates, builder, criteriaQuery, datasetRoot);
      if (!queryPredicatesList.isEmpty()) {
        finalPredicatesList.addAll(queryPredicatesList);
      }

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
      Transaction transaction = session.beginTransaction();
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
      session.update(datasetObj);
      transaction.commit();
      LOGGER.debug(ModelDBMessages.DATASET_UPDATE_SUCCESSFULLY_MSG);
      return datasetObj.getProtoObject();
    }
  }

  @Override
  public Dataset updateDatasetDescription(String datasetId, String datasetDescription)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      DatasetEntity datasetObj = session.load(DatasetEntity.class, datasetId);
      datasetObj.setDescription(datasetDescription);
      datasetObj.setTime_updated(Calendar.getInstance().getTimeInMillis());
      session.update(datasetObj);
      transaction.commit();
      LOGGER.debug(ModelDBMessages.DATASET_UPDATE_SUCCESSFULLY_MSG);
      return datasetObj.getProtoObject();
    }
  }

  @Override
  public Dataset addDatasetTags(String datasetId, List<String> tagsList)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      DatasetEntity datasetObj = session.get(DatasetEntity.class, datasetId);
      if (datasetObj == null) {
        String errorMessage = "Dataset not found for given ID";
        LOGGER.warn(errorMessage);
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
        session.saveOrUpdate(datasetObj);
      }
      transaction.commit();
      LOGGER.debug("Dataset tags added successfully");
      return datasetObj.getProtoObject();
    }
  }

  @Override
  public List<String> getDatasetTags(String datasetId) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      DatasetEntity datasetObj = session.get(DatasetEntity.class, datasetId);
      LOGGER.debug("Got Dataset");
      return datasetObj.getProtoObject().getTagsList();
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
    }
  }

  @Override
  public Dataset addDatasetAttributes(String datasetId, List<KeyValue> attributesList)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      DatasetEntity datasetObj = session.get(DatasetEntity.class, datasetId);
      datasetObj.setAttributeMapping(
          RdbmsUtils.convertAttributesFromAttributeEntityList(
              datasetObj, ModelDBConstants.ATTRIBUTES, attributesList));
      datasetObj.setTime_updated(Calendar.getInstance().getTimeInMillis());
      session.saveOrUpdate(datasetObj);
      transaction.commit();
      LOGGER.debug("Dataset attributes added successfully");
      return datasetObj.getProtoObject();
    }
  }

  @Override
  public Dataset updateDatasetAttributes(String datasetId, KeyValue attribute)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      DatasetEntity datasetObj = session.get(DatasetEntity.class, datasetId);
      if (datasetObj == null) {
        String errorMessage = "Dataset not found for given ID";
        LOGGER.warn(errorMessage);
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
      session.saveOrUpdate(datasetObj);
      transaction.commit();
      return datasetObj.getProtoObject();
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
    }
  }

  @Override
  public Dataset setDatasetVisibility(String datasetId, DatasetVisibility datasetVisibility)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      DatasetEntity datasetEntity = session.load(DatasetEntity.class, datasetId);

      Integer oldVisibilityInt = datasetEntity.getDataset_visibility();
      DatasetVisibility oldVisibility = DatasetVisibility.PRIVATE;
      if (oldVisibilityInt != null) {
        oldVisibility = DatasetVisibility.forNumber(oldVisibilityInt);
      }
      if (!oldVisibility.equals(datasetVisibility)) {
        datasetEntity.setDataset_visibility(datasetVisibility.ordinal());
        datasetEntity.setTime_updated(Calendar.getInstance().getTimeInMillis());
        session.update(datasetEntity);
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

      transaction.commit();
      LOGGER.debug("Dataset by Id getting successfully");
      return datasetEntity.getProtoObject();
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
        Role publicReadRole =
            roleService.getRoleByName(ModelDBConstants.ROLE_DATASET_PUBLIC_READ, null);
        roleService.createRoleBinding(
            publicReadRole,
            new CollaboratorUser(authService, authService.getUnsignedUser()),
            datasetId,
            ModelDBServiceResourceTypes.DATASET);
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
            roleService.buildRoleBindingName(
                ModelDBConstants.ROLE_DATASET_PUBLIC_READ,
                datasetId,
                authService.getVertaIdFromUserInfo(authService.getUnsignedUser()),
                ModelDBServiceResourceTypes.DATASET.name());
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
    }
  }

  @Override
  public Dataset setDatasetWorkspace(String datasetId, WorkspaceDTO workspaceDTO)
      throws InvalidProtocolBufferException {

    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      DatasetEntity datasetEntity = session.load(DatasetEntity.class, datasetId);
      deleteWorkspaceRoleBindings(
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
      session.update(datasetEntity);
      LOGGER.debug("Dataset workspace updated successfully");
      transaction.commit();
      return datasetEntity.getProtoObject();
    }
  }
}
