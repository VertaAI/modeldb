package ai.verta.modeldb.experiment;

import ai.verta.common.Artifact;
import ai.verta.common.KeyValue;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.CodeVersion;
import ai.verta.modeldb.DeleteExperiments;
import ai.verta.modeldb.Experiment;
import ai.verta.modeldb.FindExperiments;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.collaborator.CollaboratorUser;
import ai.verta.modeldb.dto.ExperimentPaginationDTO;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.CodeVersionEntity;
import ai.verta.modeldb.entities.ExperimentEntity;
import ai.verta.modeldb.entities.ProjectEntity;
import ai.verta.modeldb.entities.TagsMapping;
import ai.verta.modeldb.project.ProjectDAO;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.uac.ModelDBActionEnum;
import ai.verta.uac.Role;
import ai.verta.uac.UserInfo;
import com.google.protobuf.Any;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
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

public class ExperimentDAORdbImpl implements ExperimentDAO {

  private static final Logger LOGGER = LogManager.getLogger(ExperimentDAORdbImpl.class.getName());
  private final AuthService authService;
  private final RoleService roleService;

  private static final String CHECK_ENTITY_PREFIX =
      "Select count(*) From ExperimentEntity ee where ee.";
  private static final String CHECK_ENTITY_BY_PROJ_ID_AND_NAME_QUERY =
      new StringBuilder(CHECK_ENTITY_PREFIX)
          .append(ModelDBConstants.NAME)
          .append(" = :experimentName AND ee.")
          .append(ModelDBConstants.PROJECT_ID)
          .append(" = :projectId ")
          .append(" AND ee." + ModelDBConstants.DELETED + " = false ")
          .toString();
  private static final String CHECK_ENTITY_BY_ID =
      new StringBuilder(CHECK_ENTITY_PREFIX)
          .append(ModelDBConstants.ID)
          .append(" = :experimentId ")
          .append(" AND ee." + ModelDBConstants.DELETED + " = false ")
          .toString();
  private static final String GET_KEY_VALUE_EXPERIMENT_QUERY =
      new StringBuilder("From AttributeEntity attr where attr.")
          .append(ModelDBConstants.KEY)
          .append(" in (:keys) AND attr.experimentEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :experimentId AND attr.field_type = :fieldType")
          .toString();
  private static final String EXPERIMENT_BY_BATCH_IDS_QUERY =
      "From ExperimentEntity ex where ex.id IN (:ids) AND ex."
          + ModelDBConstants.DELETED
          + " = false";
  private static final String DELETE_TAGS_PREFIX_QUERY =
      new StringBuffer("delete from TagsMapping tm WHERE tm.experimentEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :experimentId")
          .toString();
  private static final String DELETE_ALL_ATTRIBUTE_QUERY =
      new StringBuffer("delete from AttributeEntity attr WHERE attr.experimentEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :experimentId  AND attr.field_type = :fieldType ")
          .toString();
  private static final String DELETE_SELECTED_ATTRIBUTE_BY_KEYS_QUERY =
      new StringBuffer("delete from AttributeEntity attr WHERE attr.experimentEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :experimentId  AND attr.field_type = :fieldType ")
          .append(" AND attr.")
          .append(ModelDBConstants.KEY)
          .append(" in (:keys) ")
          .toString();
  private static final String DELETE_ARTIFACT_QUERY =
      new StringBuffer("delete from ArtifactEntity ar WHERE ar.experimentEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :experimentId  AND ar.")
          .append(ModelDBConstants.KEY)
          .append(" in (:keys)")
          .toString();
  private static final String GET_EXPERIMENT_BY_ID_QUERY =
      "From ExperimentEntity exp where exp.id = :id";

  private static final String PROJ_IDS_BY_EXP_IDS_HQL =
      new StringBuffer("From ExperimentEntity ere where ere.")
          .append(ModelDBConstants.ID)
          .append(" IN (:experimentIds) ")
          .append(" AND ere.")
          .append(ModelDBConstants.DELETED)
          .append(" = :deleted ")
          .toString();
  private static final String DELETED_STATUS_EXPERIMENT_QUERY_STRING =
      new StringBuilder("UPDATE ")
          .append(ExperimentEntity.class.getSimpleName())
          .append(" expr ")
          .append("SET expr.")
          .append(ModelDBConstants.DELETED)
          .append(" = :deleted ")
          .append(" WHERE expr.")
          .append(ModelDBConstants.ID)
          .append(" IN (:experimentIds)")
          .toString();

  /**
   * For getting experiments that user has access to (either as owner or a collaborator), fetch all
   * experiments of the requested experimentIds then iterate that list and check if experiment is
   * accessible or not. The list of accessible experimentIDs is built and returned by this method.
   *
   * @param requestedExperimentIds : experiment Ids
   * @return List<String> : list of accessible Experiment Id
   */
  public List<String> getAccessibleExperimentIDs(
      List<String> requestedExperimentIds,
      ModelDBActionEnum.ModelDBServiceActions modelDBServiceActions) {
    Map<String, String> projectIdExperimentIdMap =
        getProjectIdsByExperimentIds(requestedExperimentIds);

    Set<String> projectIdSet = new HashSet<>(projectIdExperimentIdMap.values());

    List<String> accessibleExperimentIds = new ArrayList<>();
    List<String> allowedProjectIds;
    // Validate if current user has access to the entity or not
    if (projectIdSet.size() == 1) {
      roleService.isSelfAllowed(
          ModelDBServiceResourceTypes.PROJECT,
          modelDBServiceActions,
          new ArrayList<>(projectIdSet).get(0));
      accessibleExperimentIds.addAll(requestedExperimentIds);
    } else {
      allowedProjectIds =
          roleService.getSelfAllowedResources(
              ModelDBServiceResourceTypes.PROJECT, modelDBServiceActions);
      // Validate if current user has access to the entity or not
      allowedProjectIds.retainAll(projectIdSet);
      for (Map.Entry<String, String> entry : projectIdExperimentIdMap.entrySet()) {
        if (allowedProjectIds.contains(entry.getValue())) {
          accessibleExperimentIds.add(entry.getKey());
        }
      }
    }
    return accessibleExperimentIds;
  }

  public ExperimentDAORdbImpl(AuthService authService, RoleService roleService) {
    this.authService = authService;
    this.roleService = roleService;
  }

  private void checkIfEntityAlreadyExists(Experiment experiment, Boolean isInsert) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {

      String queryStr = "";
      if (isInsert) {
        queryStr = CHECK_ENTITY_BY_PROJ_ID_AND_NAME_QUERY;
      } else {
        queryStr = CHECK_ENTITY_BY_ID;
      }

      Query query = session.createQuery(queryStr);
      if (isInsert) {
        query.setParameter("experimentName", experiment.getName());
        query.setParameter("projectId", experiment.getProjectId());
      } else {
        query.setParameter(ModelDBConstants.EXPERIMENT_ID_STR, experiment.getId());
      }
      Long count = (Long) query.uniqueResult();
      boolean existStatus = false;
      if (count > 0) {
        existStatus = true;
      }

      // Throw error if it is an insert request and Experiment with same name already exists
      if (existStatus && isInsert) {
        Status status =
            Status.newBuilder()
                .setCode(Code.ALREADY_EXISTS_VALUE)
                .setMessage("Experiment already exists in database")
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      } else if (!existStatus && !isInsert) {
        // Throw error if it is an update request and Experiment with given name does not exist
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage("Experiment does not exist in database")
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        checkIfEntityAlreadyExists(experiment, isInsert);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Experiment insertExperiment(Experiment experiment, UserInfo userInfo)
      throws InvalidProtocolBufferException {
    checkIfEntityAlreadyExists(experiment, true);
    createRoleBindingsForExperiment(experiment, userInfo);
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      session.save(RdbmsUtils.generateExperimentEntity(experiment));
      transaction.commit();
      LOGGER.debug("Experiment created successfully");
      return experiment;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return insertExperiment(experiment, userInfo);
      } else {
        throw ex;
      }
    }
  }

  private void createRoleBindingsForExperiment(Experiment experiment, UserInfo userInfo) {
    Role ownerRole = roleService.getRoleByName(ModelDBConstants.ROLE_EXPERIMENT_OWNER, null);
    roleService.createRoleBinding(
        ownerRole,
        new CollaboratorUser(authService, userInfo),
        experiment.getId(),
        ModelDBServiceResourceTypes.EXPERIMENT);
  }

  @Override
  public Experiment updateExperimentName(String experimentId, String experimentName)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentEntity experimentEntity = session.load(ExperimentEntity.class, experimentId);
      experimentEntity.setName(experimentName);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentEntity.setDate_updated(currentTimestamp);
      Transaction transaction = session.beginTransaction();
      session.update(experimentEntity);
      transaction.commit();
      LOGGER.debug("Experiment name updated successfully");
      return experimentEntity.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return updateExperimentName(experimentId, experimentName);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Experiment updateExperimentDescription(String experimentId, String experimentDescription)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentEntity experimentEntity = session.load(ExperimentEntity.class, experimentId);
      experimentEntity.setDescription(experimentDescription);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentEntity.setDate_updated(currentTimestamp);
      Transaction transaction = session.beginTransaction();
      session.update(experimentEntity);
      transaction.commit();
      LOGGER.debug("Experiment description updated successfully");
      return experimentEntity.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return updateExperimentDescription(experimentId, experimentDescription);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Experiment getExperiment(String experimentId) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentEntity experimentObj = session.get(ExperimentEntity.class, experimentId);
      if (experimentObj != null && !experimentObj.getDeleted()) {
        LOGGER.debug("Experiment getting successfully");
        return experimentObj.getProtoObject();
      } else {
        String errorMessage = ModelDBMessages.EXPERIMENT_NOT_FOUND_ERROR_MSG + experimentId;
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperiment(experimentId);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<Experiment> getExperimentsByBatchIds(List<String> experimentIds)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query query = session.createQuery(EXPERIMENT_BY_BATCH_IDS_QUERY);
      query.setParameterList("ids", experimentIds);

      @SuppressWarnings("unchecked")
      List<ExperimentEntity> experimentEntities = query.list();
      LOGGER.debug("Experiment by Ids getting successfully");
      return RdbmsUtils.convertExperimentsFromExperimentEntityList(experimentEntities);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperimentsByBatchIds(experimentIds);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public ExperimentPaginationDTO getExperimentsInProject(
      ProjectDAO projectDAO,
      String projectId,
      Integer pageNumber,
      Integer pageLimit,
      Boolean order,
      String sortKey)
      throws InvalidProtocolBufferException {

    UserInfo userInfo = authService.getCurrentLoginUserInfo();
    FindExperiments findExperiments =
        FindExperiments.newBuilder()
            .setProjectId(projectId)
            .setPageNumber(pageNumber)
            .setPageLimit(pageLimit)
            .setAscending(order)
            .setSortKey(sortKey)
            .build();
    return findExperiments(projectDAO, userInfo, findExperiments);
  }

  @Override
  public Experiment addExperimentTags(String experimentId, List<String> tagsList)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentEntity experimentObj = session.load(ExperimentEntity.class, experimentId);
      if (experimentObj == null) {
        String errorMessage = ModelDBMessages.EXPERIMENT_NOT_FOUND_ERROR_MSG + experimentId;
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      List<String> newTags = new ArrayList<>();
      Experiment existingProtoExperimentObj = experimentObj.getProtoObject();
      for (String tag : tagsList) {
        if (!existingProtoExperimentObj.getTagsList().contains(tag)) {
          newTags.add(tag);
        }
      }
      if (!newTags.isEmpty()) {
        List<TagsMapping> newTagMappings =
            RdbmsUtils.convertTagListFromTagMappingList(experimentObj, newTags);
        experimentObj.getTags().addAll(newTagMappings);
        experimentObj.setDate_updated(Calendar.getInstance().getTimeInMillis());
        Transaction transaction = session.beginTransaction();
        session.saveOrUpdate(experimentObj);
        transaction.commit();
      }
      LOGGER.debug("Experiment tags added successfully");
      return experimentObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return addExperimentTags(experimentId, tagsList);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<String> getExperimentTags(String experimentId) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentEntity experimentObj = session.get(ExperimentEntity.class, experimentId);
      LOGGER.debug("Experiment Tags getting successfully");
      return experimentObj.getProtoObject().getTagsList();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperimentTags(experimentId);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Experiment deleteExperimentTags(
      String experimentId, List<String> experimentTagList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      if (deleteAll) {
        Query query = session.createQuery(DELETE_TAGS_PREFIX_QUERY);
        query.setParameter(ModelDBConstants.EXPERIMENT_ID_STR, experimentId);
        query.executeUpdate();
      } else {
        StringBuilder stringQueryBuilder =
            new StringBuilder(DELETE_TAGS_PREFIX_QUERY)
                .append(" AND tm.")
                .append(ModelDBConstants.TAGS)
                .append(" in (:tags)");
        Query query = session.createQuery(stringQueryBuilder.toString());
        query.setParameter("tags", experimentTagList);
        query.setParameter(ModelDBConstants.EXPERIMENT_ID_STR, experimentId);
        query.executeUpdate();
      }
      ExperimentEntity experimentObj = session.get(ExperimentEntity.class, experimentId);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentObj.setDate_updated(currentTimestamp);
      session.update(experimentObj);
      transaction.commit();
      LOGGER.debug("Experiment tags deleted successfully");
      return experimentObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteExperimentTags(experimentId, experimentTagList, deleteAll);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Experiment addExperimentAttributes(String experimentId, List<KeyValue> attributes)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentEntity experimentObj = session.get(ExperimentEntity.class, experimentId);
      if (experimentObj == null) {
        String errorMessage = ModelDBMessages.EXPERIMENT_NOT_FOUND_ERROR_MSG + experimentId;
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      experimentObj.setAttributeMapping(
          RdbmsUtils.convertAttributesFromAttributeEntityList(
              experimentObj, ModelDBConstants.ATTRIBUTES, attributes));
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentObj.setDate_updated(currentTimestamp);
      Transaction transaction = session.beginTransaction();
      session.saveOrUpdate(experimentObj);
      transaction.commit();
      LOGGER.debug("Experiment attributes added successfully");
      return experimentObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return addExperimentAttributes(experimentId, attributes);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<KeyValue> getExperimentAttributes(
      String experimentId, List<String> attributeKeyList, Boolean getAll)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentEntity experimentObj = session.get(ExperimentEntity.class, experimentId);
      if (experimentObj == null) {
        String errorMessage = ModelDBMessages.EXPERIMENT_NOT_FOUND_ERROR_MSG + experimentId;
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      if (getAll) {
        return experimentObj.getProtoObject().getAttributesList();
      } else {
        Query query = session.createQuery(GET_KEY_VALUE_EXPERIMENT_QUERY);
        query.setParameterList("keys", attributeKeyList);
        query.setParameter(ModelDBConstants.EXPERIMENT_ID_STR, experimentId);
        query.setParameter(ModelDBConstants.FIELD_TYPE_STR, ModelDBConstants.ATTRIBUTES);
        List<AttributeEntity> attributeEntities = query.list();
        return RdbmsUtils.convertAttributeEntityListFromAttributes(attributeEntities);
      }
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperimentAttributes(experimentId, attributeKeyList, getAll);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Experiment deleteExperimentAttributes(
      String experimentId, List<String> attributeKeyList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      if (deleteAll) {
        Query query = session.createQuery(DELETE_ALL_ATTRIBUTE_QUERY);
        query.setParameter(ModelDBConstants.EXPERIMENT_ID_STR, experimentId);
        query.setParameter(ModelDBConstants.FIELD_TYPE_STR, ModelDBConstants.ATTRIBUTES);
        query.executeUpdate();
      } else {
        Query query = session.createQuery(DELETE_SELECTED_ATTRIBUTE_BY_KEYS_QUERY);
        query.setParameter("keys", attributeKeyList);
        query.setParameter(ModelDBConstants.EXPERIMENT_ID_STR, experimentId);
        query.setParameter(ModelDBConstants.FIELD_TYPE_STR, ModelDBConstants.ATTRIBUTES);
        query.executeUpdate();
      }
      ExperimentEntity experimentObj = session.get(ExperimentEntity.class, experimentId);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentObj.setDate_updated(currentTimestamp);
      session.update(experimentObj);
      transaction.commit();
      return experimentObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteExperimentAttributes(experimentId, attributeKeyList, deleteAll);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Boolean deleteExperiments(List<String> experimentIds) {
    List<String> accessibleExperimentIds =
        getAccessibleExperimentIDs(experimentIds, ModelDBActionEnum.ModelDBServiceActions.UPDATE);

    if (accessibleExperimentIds.isEmpty()) {
      String errorMessage =
          "Access is denied. User is unauthorized for given Experiment IDs : "
              + accessibleExperimentIds;
      ModelDBUtils.logAndThrowError(
          errorMessage,
          Code.PERMISSION_DENIED_VALUE,
          Any.pack(DeleteExperiments.getDefaultInstance()));
    }

    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      Query deletedExperimentQuery = session.createQuery(DELETED_STATUS_EXPERIMENT_QUERY_STRING);
      deletedExperimentQuery.setParameter("deleted", true);
      deletedExperimentQuery.setParameter("experimentIds", accessibleExperimentIds);
      int updatedCount = deletedExperimentQuery.executeUpdate();
      LOGGER.debug(
          "Mark Experiments as deleted : {}, count : {}", accessibleExperimentIds, updatedCount);
      transaction.commit();
      LOGGER.debug("Experiment deleted successfully");
      return true;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteExperiments(experimentIds);
      } else {
        throw ex;
      }
    }
  }

  @Override
  @Deprecated
  public Experiment getExperiment(List<KeyValue> keyValues) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      StringBuilder stringQueryBuilder = new StringBuilder("From ExperimentEntity ee where ");
      stringQueryBuilder.append("ee.").append(ModelDBConstants.DELETED).append(" = :deleted AND ");
      Map<String, Object> paramMap = new HashMap<>();
      for (int index = 0; index < keyValues.size(); index++) {
        KeyValue keyValue = keyValues.get(index);
        Value value = keyValue.getValue();
        String key = keyValue.getKey();

        switch (value.getKindCase()) {
          case NUMBER_VALUE:
            paramMap.put(key, value.getNumberValue());
            break;
          case STRING_VALUE:
            paramMap.put(key, value.getStringValue());
            break;
          case BOOL_VALUE:
            paramMap.put(key, value.getBoolValue());
            break;
          default:
            Status invalidValueTypeError =
                Status.newBuilder()
                    .setCode(Code.UNIMPLEMENTED_VALUE)
                    .setMessage(
                        "Unknown 'Value' type recognized, valid 'Value' type are NUMBER_VALUE, STRING_VALUE, BOOL_VALUE")
                    .build();
            throw StatusProto.toStatusRuntimeException(invalidValueTypeError);
        }
        stringQueryBuilder.append(" ee." + key + " = :" + key);
        if (index < keyValues.size() - 1) {
          stringQueryBuilder.append(" AND ");
        }
      }
      Query query = session.createQuery(stringQueryBuilder.toString());
      query.setParameter("deleted", false);
      for (Entry<String, Object> paramEntry : paramMap.entrySet()) {
        query.setParameter(paramEntry.getKey(), paramEntry.getValue());
      }
      ExperimentEntity experimentObj = (ExperimentEntity) query.uniqueResult();
      if (experimentObj == null) {
        String errorMessage = "Experiment not found";
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      return experimentObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperiment(keyValues);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<Experiment> getExperiments(List<KeyValue> keyValues)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      StringBuilder stringQueryBuilder = new StringBuilder("From ExperimentEntity ee where ");
      Map<String, Object> paramMap = new HashMap<>();
      for (int index = 0; index < keyValues.size(); index++) {
        KeyValue keyValue = keyValues.get(index);
        Value value = keyValue.getValue();
        String key = keyValue.getKey();

        switch (value.getKindCase()) {
          case NUMBER_VALUE:
            paramMap.put(key, value.getNumberValue());
            break;
          case STRING_VALUE:
            paramMap.put(key, value.getStringValue());
            break;
          case BOOL_VALUE:
            paramMap.put(key, value.getBoolValue());
            break;
          default:
            Status invalidValueTypeError =
                Status.newBuilder()
                    .setCode(Code.UNIMPLEMENTED_VALUE)
                    .setMessage(
                        "Unknown 'Value' type recognized, valid 'Value' type are NUMBER_VALUE, STRING_VALUE, BOOL_VALUE")
                    .build();
            throw StatusProto.toStatusRuntimeException(invalidValueTypeError);
        }
        stringQueryBuilder.append(" ee." + key + " = :" + key);
        if (index < keyValues.size() - 1) {
          stringQueryBuilder.append(" AND ");
        }
      }
      Query query =
          session.createQuery(
              stringQueryBuilder.toString() + " AND ee." + ModelDBConstants.DELETED + " = false ");
      for (Entry<String, Object> paramEntry : paramMap.entrySet()) {
        query.setParameter(paramEntry.getKey(), paramEntry.getValue());
      }
      List<ExperimentEntity> experimentObjList = query.list();
      return RdbmsUtils.convertExperimentsFromExperimentEntityList(experimentObjList);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperiments(keyValues);
      } else {
        throw ex;
      }
    }
  }

  private Experiment copyExperimentAndUpdateDetails(
      Experiment srcExperiment, Project newProject, UserInfo newOwner) {
    Experiment.Builder experimentBuilder =
        Experiment.newBuilder(srcExperiment).setId(UUID.randomUUID().toString());

    if (newOwner != null) {
      experimentBuilder.setOwner(authService.getVertaIdFromUserInfo(newOwner));
    }
    if (newProject != null) {
      experimentBuilder.setProjectId(newProject.getId());
    }
    return experimentBuilder.build();
  }

  @Override
  public Experiment deepCopyExperimentForUser(
      Experiment srcExperiment, Project newProject, UserInfo newOwner)
      throws InvalidProtocolBufferException {
    checkIfEntityAlreadyExists(srcExperiment, false);

    if (newOwner == null || newProject == null) {
      Status status =
          Status.newBuilder()
              .setCode(Code.INVALID_ARGUMENT_VALUE)
              .setMessage("New owner or new project not passed for cloning Experiment.")
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }

    Experiment copyExperiment = copyExperimentAndUpdateDetails(srcExperiment, newProject, newOwner);
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentEntity experimentObj = RdbmsUtils.generateExperimentEntity(copyExperiment);
      Transaction transaction = session.beginTransaction();
      session.saveOrUpdate(experimentObj);
      transaction.commit();
      LOGGER.debug("Experiment copied successfully");
      return experimentObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deepCopyExperimentForUser(srcExperiment, newProject, newOwner);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Experiment logExperimentCodeVersion(String experimentId, CodeVersion updatedCodeVersion)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentEntity experimentEntity = session.get(ExperimentEntity.class, experimentId);

      CodeVersionEntity existingCodeVersionEntity = experimentEntity.getCode_version_snapshot();
      if (existingCodeVersionEntity == null) {
        experimentEntity.setCode_version_snapshot(
            RdbmsUtils.generateCodeVersionEntity(
                ModelDBConstants.CODE_VERSION, updatedCodeVersion));
      } else {
        existingCodeVersionEntity.setDate_logged(updatedCodeVersion.getDateLogged());
        if (updatedCodeVersion.hasGitSnapshot()) {
          existingCodeVersionEntity.setGit_snapshot(
              RdbmsUtils.generateGitSnapshotEntity(
                  ModelDBConstants.GIT_SNAPSHOT, updatedCodeVersion.getGitSnapshot()));
          existingCodeVersionEntity.setCode_archive(null);
        } else if (updatedCodeVersion.hasCodeArchive()) {
          existingCodeVersionEntity.setCode_archive(
              RdbmsUtils.generateArtifactEntity(
                  experimentEntity,
                  ModelDBConstants.CODE_ARCHIVE,
                  updatedCodeVersion.getCodeArchive()));
          existingCodeVersionEntity.setGit_snapshot(null);
        }
      }
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentEntity.setDate_updated(currentTimestamp);
      Transaction transaction = session.beginTransaction();
      session.update(experimentEntity);
      transaction.commit();
      LOGGER.debug("Experiment code version snapshot updated successfully");
      return experimentEntity.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return logExperimentCodeVersion(experimentId, updatedCodeVersion);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public ExperimentPaginationDTO findExperiments(
      ProjectDAO projectDAO, UserInfo currentLoginUserInfo, FindExperiments queryParameters)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {

      List<String> accessibleExperimentIds = new ArrayList<>();
      if (!queryParameters.getExperimentIdsList().isEmpty()) {
        accessibleExperimentIds.addAll(
            getAccessibleExperimentIDs(
                queryParameters.getExperimentIdsList(),
                ModelDBActionEnum.ModelDBServiceActions.READ));
        if (accessibleExperimentIds.isEmpty()) {
          String errorMessage =
              "Access is denied. User is unauthorized for given Experiment IDs : "
                  + accessibleExperimentIds;
          ModelDBUtils.logAndThrowError(
              errorMessage,
              Code.PERMISSION_DENIED_VALUE,
              Any.pack(FindExperiments.getDefaultInstance()));
        }
      }

      List<KeyValueQuery> predicates = new ArrayList<>(queryParameters.getPredicatesList());
      for (KeyValueQuery predicate : predicates) {
        if (predicate.getKey().equals(ModelDBConstants.ID)) {
          List<String> accessibleExperimentId =
              getAccessibleExperimentIDs(
                  Collections.singletonList(predicate.getValue().getStringValue()),
                  ModelDBActionEnum.ModelDBServiceActions.READ);
          accessibleExperimentIds.addAll(accessibleExperimentId);
          // Validate if current user has access to the entity or not where predicate key has an id
          RdbmsUtils.validatePredicates(
              ModelDBConstants.EXPERIMENTS, accessibleExperimentIds, predicate, roleService);
        }
      }

      CriteriaBuilder builder = session.getCriteriaBuilder();
      // Using FROM and JOIN
      CriteriaQuery<ExperimentEntity> criteriaQuery = builder.createQuery(ExperimentEntity.class);
      Root<ExperimentEntity> experimentRoot = criteriaQuery.from(ExperimentEntity.class);
      experimentRoot.alias("exp");

      Root<ProjectEntity> projectEntityRoot = criteriaQuery.from(ProjectEntity.class);
      projectEntityRoot.alias("pr");

      List<Predicate> finalPredicatesList = new ArrayList<>();
      finalPredicatesList.add(
          builder.equal(
              experimentRoot.get(ModelDBConstants.PROJECT_ID),
              projectEntityRoot.get(ModelDBConstants.ID)));

      List<String> projectIds = new ArrayList<>();
      if (!queryParameters.getProjectId().isEmpty()) {
        projectIds.add(queryParameters.getProjectId());
      } else if (accessibleExperimentIds.isEmpty()) {
        List<String> workspaceProjectIDs =
            projectDAO.getWorkspaceProjectIDs(
                queryParameters.getWorkspaceName(), currentLoginUserInfo);
        if (workspaceProjectIDs == null || workspaceProjectIDs.isEmpty()) {
          LOGGER.info(
              "accessible project for the experiments not found for given workspace : {}",
              queryParameters.getWorkspaceName());
          ExperimentPaginationDTO experimentPaginationDTO = new ExperimentPaginationDTO();
          experimentPaginationDTO.setExperiments(Collections.emptyList());
          experimentPaginationDTO.setTotalRecords(0L);
          return experimentPaginationDTO;
        }
        projectIds.addAll(workspaceProjectIDs);
      }

      if (accessibleExperimentIds.isEmpty() && projectIds.isEmpty()) {
        String errorMessage =
            "Access is denied. Accessible projects not found for given Experiment IDs : "
                + accessibleExperimentIds;
        ModelDBUtils.logAndThrowError(
            errorMessage,
            Code.PERMISSION_DENIED_VALUE,
            Any.pack(FindExperiments.getDefaultInstance()));
      }

      if (!projectIds.isEmpty()) {
        Expression<String> projectExpression = experimentRoot.get(ModelDBConstants.PROJECT_ID);
        Predicate projectsPredicate = projectExpression.in(projectIds);
        finalPredicatesList.add(projectsPredicate);
      }

      if (!accessibleExperimentIds.isEmpty()) {
        Expression<String> exp = experimentRoot.get(ModelDBConstants.ID);
        Predicate predicate2 = exp.in(accessibleExperimentIds);
        finalPredicatesList.add(predicate2);
      }

      String entityName = "experimentEntity";
      try {
        List<Predicate> queryPredicatesList =
            RdbmsUtils.getQueryPredicatesFromPredicateList(
                entityName, predicates, builder, criteriaQuery, experimentRoot, authService);
        if (!queryPredicatesList.isEmpty()) {
          finalPredicatesList.addAll(queryPredicatesList);
        }
      } catch (ModelDBException ex) {
        if (ex.getCode().ordinal() == Code.FAILED_PRECONDITION_VALUE
            && ModelDBConstants.INTERNAL_MSG_USERS_NOT_FOUND.equals(ex.getMessage())) {
          LOGGER.info(ex.getMessage());
          ExperimentPaginationDTO experimentPaginationDTO = new ExperimentPaginationDTO();
          experimentPaginationDTO.setExperiments(Collections.emptyList());
          experimentPaginationDTO.setTotalRecords(0L);
          return experimentPaginationDTO;
        }
      }

      finalPredicatesList.add(builder.equal(experimentRoot.get(ModelDBConstants.DELETED), false));
      finalPredicatesList.add(
          builder.equal(projectEntityRoot.get(ModelDBConstants.DELETED), false));

      Order orderBy =
          RdbmsUtils.getOrderBasedOnSortKey(
              queryParameters.getSortKey(),
              queryParameters.getAscending(),
              builder,
              experimentRoot,
              entityName);

      Predicate[] predicateArr = new Predicate[finalPredicatesList.size()];
      for (int index = 0; index < finalPredicatesList.size(); index++) {
        predicateArr[index] = finalPredicatesList.get(index);
      }

      Predicate predicateWhereCause = builder.and(predicateArr);
      criteriaQuery.select(experimentRoot);
      criteriaQuery.where(predicateWhereCause);
      criteriaQuery.orderBy(orderBy);

      Query query = session.createQuery(criteriaQuery);
      LOGGER.debug("Final experiments final query : {}", query.getQueryString());
      if (queryParameters.getPageNumber() != 0 && queryParameters.getPageLimit() != 0) {
        // Calculate number of documents to skip
        int skips = queryParameters.getPageLimit() * (queryParameters.getPageNumber() - 1);
        query.setFirstResult(skips);
        query.setMaxResults(queryParameters.getPageLimit());
      }

      List<Experiment> experimentList = new ArrayList<>();
      List<ExperimentEntity> experimentEntities = query.list();
      LOGGER.debug("Final experiments list size : {}", experimentEntities.size());
      if (!experimentEntities.isEmpty()) {
        experimentList = RdbmsUtils.convertExperimentsFromExperimentEntityList(experimentEntities);
      }

      Set<String> experimentIdsSet = new HashSet<>();
      List<Experiment> experiments = new ArrayList<>();
      for (Experiment experiment : experimentList) {
        if (!experimentIdsSet.contains(experiment.getId())) {
          experimentIdsSet.add(experiment.getId());
          if (queryParameters.getIdsOnly()) {
            experiment = Experiment.newBuilder().setId(experiment.getId()).build();
            experiments.add(experiment);
          } else {
            experiments.add(experiment);
          }
        }
      }

      long totalRecords = RdbmsUtils.count(session, experimentRoot, criteriaQuery);
      LOGGER.debug("Experiments Total record count : {}", totalRecords);

      ExperimentPaginationDTO experimentPaginationDTO = new ExperimentPaginationDTO();
      experimentPaginationDTO.setExperiments(experiments);
      experimentPaginationDTO.setTotalRecords(totalRecords);
      return experimentPaginationDTO;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return findExperiments(projectDAO, currentLoginUserInfo, queryParameters);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Experiment logArtifacts(String experimentId, List<Artifact> newArtifacts)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query query = session.createQuery(GET_EXPERIMENT_BY_ID_QUERY);
      query.setParameter("id", experimentId);
      ExperimentEntity experimentEntity = (ExperimentEntity) query.uniqueResult();
      String errorMessage = null;
      if (experimentEntity == null) {
        errorMessage = ModelDBMessages.EXPERIMENT_NOT_FOUND_ERROR_MSG + experimentId;
      }

      if (errorMessage != null) {
        ModelDBUtils.logAndThrowError(errorMessage, Code.INTERNAL_VALUE, Any.newBuilder().build());
      }

      assert experimentEntity != null;
      List<Artifact> existingArtifacts = experimentEntity.getProtoObject().getArtifactsList();
      for (Artifact existingArtifact : existingArtifacts) {
        for (Artifact newArtifact : newArtifacts) {
          if (existingArtifact.getKey().equals(newArtifact.getKey())) {
            Status status =
                Status.newBuilder()
                    .setCode(Code.ALREADY_EXISTS_VALUE)
                    .setMessage(
                        "Artifact being logged already exists. existing artifact key : "
                            + newArtifact.getKey())
                    .build();
            throw StatusProto.toStatusRuntimeException(status);
          }
        }
      }

      experimentEntity.setArtifactMapping(
          RdbmsUtils.convertArtifactsFromArtifactEntityList(
              experimentEntity, ModelDBConstants.ARTIFACTS, newArtifacts));
      experimentEntity.setDate_updated(Calendar.getInstance().getTimeInMillis());
      Transaction transaction = session.beginTransaction();
      session.update(experimentEntity);
      transaction.commit();
      LOGGER.debug("Experiment log artifact successfully");
      return experimentEntity.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return logArtifacts(experimentId, newArtifacts);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<Artifact> getExperimentArtifacts(String experimentId)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query query = session.createQuery(GET_EXPERIMENT_BY_ID_QUERY);
      query.setParameter("id", experimentId);
      ExperimentEntity experimentEntity = (ExperimentEntity) query.uniqueResult();
      Experiment experiment = experimentEntity.getProtoObject();
      if (experiment.getArtifactsList() != null && !experiment.getArtifactsList().isEmpty()) {
        LOGGER.debug("Experiment Artifacts getting successfully");
        return experiment.getArtifactsList();
      } else {
        String errorMessage = "Artifacts not found in the Experiment";
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperimentArtifacts(experimentId);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Experiment deleteArtifacts(String experimentId, String artifactKey)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();

      Query query = session.createQuery(DELETE_ARTIFACT_QUERY);
      query.setParameter("keys", Collections.singletonList(artifactKey));
      query.setParameter(ModelDBConstants.EXPERIMENT_ID_STR, experimentId);
      query.executeUpdate();
      ExperimentEntity experimentObj = session.get(ExperimentEntity.class, experimentId);
      experimentObj.setDate_updated(Calendar.getInstance().getTimeInMillis());
      session.update(experimentObj);
      transaction.commit();
      return experimentObj.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteArtifacts(experimentId, artifactKey);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Map<String, String> getProjectIdsByExperimentIds(List<String> experimentIds) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query experimentQuery = session.createQuery(PROJ_IDS_BY_EXP_IDS_HQL);
      experimentQuery.setParameterList("experimentIds", experimentIds);
      experimentQuery.setParameter("deleted", false);
      List<ExperimentEntity> experimentEntities = experimentQuery.list();

      Map<String, String> projectIdFromExperimentMap = new HashMap<>();
      for (ExperimentEntity experimentEntity : experimentEntities) {
        projectIdFromExperimentMap.put(experimentEntity.getId(), experimentEntity.getProject_id());
      }
      return projectIdFromExperimentMap;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getProjectIdsByExperimentIds(experimentIds);
      } else {
        throw ex;
      }
    }
  }
}
