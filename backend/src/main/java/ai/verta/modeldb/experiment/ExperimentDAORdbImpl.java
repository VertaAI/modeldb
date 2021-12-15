package ai.verta.modeldb.experiment;

import ai.verta.common.Artifact;
import ai.verta.common.CodeVersion;
import ai.verta.common.KeyValue;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.*;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.collaborator.CollaboratorUser;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.dto.ExperimentPaginationDTO;
import ai.verta.modeldb.entities.*;
import ai.verta.modeldb.exceptions.*;
import ai.verta.modeldb.project.ProjectDAO;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.uac.ModelDBActionEnum;
import ai.verta.uac.UserInfo;
import com.google.rpc.Code;
import java.util.*;
import java.util.Map.Entry;
import javax.persistence.criteria.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;

public class ExperimentDAORdbImpl implements ExperimentDAO {

  private static final Logger LOGGER = LogManager.getLogger(ExperimentDAORdbImpl.class.getName());
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private final AuthService authService;
  private final MDBRoleService mdbRoleService;

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
      mdbRoleService.isSelfAllowed(
          ModelDBServiceResourceTypes.PROJECT,
          modelDBServiceActions,
          new ArrayList<>(projectIdSet).get(0));
      accessibleExperimentIds.addAll(requestedExperimentIds);
    } else {
      allowedProjectIds =
          mdbRoleService.getSelfAllowedResources(
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

  public ExperimentDAORdbImpl(AuthService authService, MDBRoleService mdbRoleService) {
    this.authService = authService;
    this.mdbRoleService = mdbRoleService;
  }

  private void checkIfEntityAlreadyExists(Experiment experiment, Boolean isInsert) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {

      var queryStr = "";
      if (isInsert) {
        queryStr = CHECK_ENTITY_BY_PROJ_ID_AND_NAME_QUERY;
      } else {
        queryStr = CHECK_ENTITY_BY_ID;
      }

      var query = session.createQuery(queryStr);
      if (isInsert) {
        query.setParameter("experimentName", experiment.getName());
        query.setParameter("projectId", experiment.getProjectId());
      } else {
        query.setParameter(ModelDBConstants.EXPERIMENT_ID_STR, experiment.getId());
      }
      Long count = (Long) query.uniqueResult();
      var existStatus = false;
      if (count > 0) {
        existStatus = true;
      }

      // Throw error if it is an insert request and Experiment with same name already exists
      if (existStatus && isInsert) {
        throw new AlreadyExistsException("Experiment already exists in database");
      } else if (!existStatus && !isInsert) {
        // Throw error if it is an update request and Experiment with given name does not exist
        throw new NotFoundException("Experiment does not exist in database");
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
  public Experiment insertExperiment(Experiment experiment, UserInfo userInfo) {
    checkIfEntityAlreadyExists(experiment, true);
    createRoleBindingsForExperiment(experiment, userInfo);
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();
      var experimentEntity = RdbmsUtils.generateExperimentEntity(experiment);
      session.save(experimentEntity);
      transaction.commit();
      LOGGER.debug("Experiment created successfully");
      return experimentEntity.getProtoObject();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return insertExperiment(experiment, userInfo);
      } else {
        throw ex;
      }
    }
  }

  private void createRoleBindingsForExperiment(Experiment experiment, UserInfo userInfo) {
    mdbRoleService.createRoleBinding(
        ModelDBConstants.ROLE_EXPERIMENT_OWNER,
        new CollaboratorUser(authService, userInfo),
        experiment.getId(),
        ModelDBServiceResourceTypes.EXPERIMENT);
  }

  @Override
  public Experiment updateExperimentName(String experimentId, String experimentName) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentEntity =
          session.load(ExperimentEntity.class, experimentId, LockMode.PESSIMISTIC_WRITE);
      experimentEntity.setName(experimentName);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentEntity.setDate_updated(currentTimestamp);
      experimentEntity.increaseVersionNumber();
      var transaction = session.beginTransaction();
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
  public Experiment updateExperimentDescription(String experimentId, String experimentDescription) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentEntity =
          session.load(ExperimentEntity.class, experimentId, LockMode.PESSIMISTIC_WRITE);
      experimentEntity.setDescription(experimentDescription);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentEntity.setDate_updated(currentTimestamp);
      experimentEntity.increaseVersionNumber();
      var transaction = session.beginTransaction();
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
  public Experiment getExperiment(String experimentId) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentEntity experimentObj = session.get(ExperimentEntity.class, experimentId);
      if (experimentObj != null && !experimentObj.getDeleted()) {
        LOGGER.debug("Experiment getting successfully");
        return experimentObj.getProtoObject();
      } else {
        String errorMessage = ModelDBMessages.EXPERIMENT_NOT_FOUND_ERROR_MSG + experimentId;
        throw new NotFoundException(errorMessage);
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
  public List<Experiment> getExperimentsByBatchIds(List<String> experimentIds) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var query = session.createQuery(EXPERIMENT_BY_BATCH_IDS_QUERY);
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
      throws PermissionDeniedException {

    var userInfo = authService.getCurrentLoginUserInfo();
    var findExperiments =
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
  public Experiment addExperimentTags(String experimentId, List<String> tagsList) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentEntity experimentObj =
          session.load(ExperimentEntity.class, experimentId, LockMode.PESSIMISTIC_WRITE);
      if (experimentObj == null) {
        String errorMessage = ModelDBMessages.EXPERIMENT_NOT_FOUND_ERROR_MSG + experimentId;
        throw new NotFoundException(errorMessage);
      }
      List<String> newTags = new ArrayList<>();
      var existingProtoExperimentObj = experimentObj.getProtoObject();
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
        experimentObj.increaseVersionNumber();
        var transaction = session.beginTransaction();
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
  public List<String> getExperimentTags(String experimentId) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
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
      String experimentId, List<String> experimentTagList, Boolean deleteAll) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();
      if (deleteAll) {
        var query =
            session
                .createQuery(DELETE_TAGS_PREFIX_QUERY)
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter(ModelDBConstants.EXPERIMENT_ID_STR, experimentId);
        query.executeUpdate();
      } else {
        StringBuilder stringQueryBuilder =
            new StringBuilder(DELETE_TAGS_PREFIX_QUERY)
                .append(" AND tm.")
                .append(ModelDBConstants.TAGS)
                .append(" in (:tags)");
        var query =
            session
                .createQuery(stringQueryBuilder.toString())
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter("tags", experimentTagList);
        query.setParameter(ModelDBConstants.EXPERIMENT_ID_STR, experimentId);
        query.executeUpdate();
      }
      ExperimentEntity experimentObj = session.get(ExperimentEntity.class, experimentId);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentObj.setDate_updated(currentTimestamp);
      experimentObj.increaseVersionNumber();
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
  public Experiment addExperimentAttributes(String experimentId, List<KeyValue> attributes) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentEntity experimentObj =
          session.get(ExperimentEntity.class, experimentId, LockMode.PESSIMISTIC_WRITE);
      if (experimentObj == null) {
        String errorMessage = ModelDBMessages.EXPERIMENT_NOT_FOUND_ERROR_MSG + experimentId;
        throw new NotFoundException(errorMessage);
      }
      experimentObj.setAttributeMapping(
          RdbmsUtils.convertAttributesFromAttributeEntityList(
              experimentObj, ModelDBConstants.ATTRIBUTES, attributes));
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentObj.setDate_updated(currentTimestamp);
      experimentObj.increaseVersionNumber();
      var transaction = session.beginTransaction();
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
      String experimentId, List<String> attributeKeyList, Boolean getAll) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentEntity experimentObj = session.get(ExperimentEntity.class, experimentId);
      if (experimentObj == null) {
        String errorMessage = ModelDBMessages.EXPERIMENT_NOT_FOUND_ERROR_MSG + experimentId;
        throw new NotFoundException(errorMessage);
      }

      if (getAll) {
        return experimentObj.getProtoObject().getAttributesList();
      } else {
        var query = session.createQuery(GET_KEY_VALUE_EXPERIMENT_QUERY);
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
      String experimentId, List<String> attributeKeyList, Boolean deleteAll) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();
      if (deleteAll) {
        var query =
            session
                .createQuery(DELETE_ALL_ATTRIBUTE_QUERY)
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter(ModelDBConstants.EXPERIMENT_ID_STR, experimentId);
        query.setParameter(ModelDBConstants.FIELD_TYPE_STR, ModelDBConstants.ATTRIBUTES);
        query.executeUpdate();
      } else {
        var query =
            session
                .createQuery(DELETE_SELECTED_ATTRIBUTE_BY_KEYS_QUERY)
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter("keys", attributeKeyList);
        query.setParameter(ModelDBConstants.EXPERIMENT_ID_STR, experimentId);
        query.setParameter(ModelDBConstants.FIELD_TYPE_STR, ModelDBConstants.ATTRIBUTES);
        query.executeUpdate();
      }
      ExperimentEntity experimentObj = session.get(ExperimentEntity.class, experimentId);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentObj.setDate_updated(currentTimestamp);
      experimentObj.increaseVersionNumber();
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
  public List<String> deleteExperiments(List<String> experimentIds)
      throws PermissionDeniedException {
    List<String> accessibleExperimentIds =
        getAccessibleExperimentIDs(experimentIds, ModelDBActionEnum.ModelDBServiceActions.UPDATE);

    if (accessibleExperimentIds.isEmpty()) {
      throw new PermissionDeniedException(
          "Access is denied. User is unauthorized for given Experiment IDs : "
              + accessibleExperimentIds);
    }

    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();
      var deletedExperimentQuery =
          session
              .createQuery(DELETED_STATUS_EXPERIMENT_QUERY_STRING)
              .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
      deletedExperimentQuery.setParameter(ModelDBConstants.DELETED, true);
      deletedExperimentQuery.setParameter("experimentIds", accessibleExperimentIds);
      int updatedCount = deletedExperimentQuery.executeUpdate();
      LOGGER.debug(
          "Mark Experiments as deleted : {}, count : {}", accessibleExperimentIds, updatedCount);
      transaction.commit();
      LOGGER.debug("Experiment deleted successfully");
      return accessibleExperimentIds;
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
  public Experiment getExperiment(List<KeyValue> keyValues) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var stringQueryBuilder = new StringBuilder("From ExperimentEntity ee where ");
      stringQueryBuilder.append("ee.").append(ModelDBConstants.DELETED).append(" = :deleted AND ");
      Map<String, Object> paramMap = new HashMap<>();
      for (var index = 0; index < keyValues.size(); index++) {
        var keyValue = keyValues.get(index);
        var value = keyValue.getValue();
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
            throw new UnimplementedException(
                "Unknown 'Value' type recognized, valid 'Value' type are NUMBER_VALUE, STRING_VALUE, BOOL_VALUE");
        }
        stringQueryBuilder.append(" ee." + key + " = :" + key);
        if (index < keyValues.size() - 1) {
          stringQueryBuilder.append(" AND ");
        }
      }
      var query = session.createQuery(stringQueryBuilder.toString());
      query.setParameter(ModelDBConstants.DELETED, false);
      for (Entry<String, Object> paramEntry : paramMap.entrySet()) {
        query.setParameter(paramEntry.getKey(), paramEntry.getValue());
      }
      ExperimentEntity experimentObj = (ExperimentEntity) query.uniqueResult();
      if (experimentObj == null) {
        var errorMessage = "Experiment not found";
        throw new NotFoundException(errorMessage);
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
  public List<Experiment> getExperiments(List<KeyValue> keyValues) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var stringQueryBuilder = new StringBuilder("From ExperimentEntity ee where ");
      Map<String, Object> paramMap = new HashMap<>();
      for (var index = 0; index < keyValues.size(); index++) {
        var keyValue = keyValues.get(index);
        var value = keyValue.getValue();
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
            throw new UnimplementedException(
                "Unknown 'Value' type recognized, valid 'Value' type are NUMBER_VALUE, STRING_VALUE, BOOL_VALUE");
        }
        stringQueryBuilder.append(" ee." + key + " = :" + key);
        if (index < keyValues.size() - 1) {
          stringQueryBuilder.append(" AND ");
        }
      }
      stringQueryBuilder.append(" AND ee.deleted = false ");
      var query = session.createQuery(stringQueryBuilder.toString());
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
    var experimentBuilder =
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
      Experiment srcExperiment, Project newProject, UserInfo newOwner) {
    checkIfEntityAlreadyExists(srcExperiment, false);

    if (newOwner == null || newProject == null) {
      throw new InvalidArgumentException(
          "New owner or new project not passed for cloning Experiment.");
    }

    var copyExperiment = copyExperimentAndUpdateDetails(srcExperiment, newProject, newOwner);
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentObj = RdbmsUtils.generateExperimentEntity(copyExperiment);
      var transaction = session.beginTransaction();
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
  public Experiment logExperimentCodeVersion(String experimentId, CodeVersion updatedCodeVersion) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentEntity =
          session.get(ExperimentEntity.class, experimentId, LockMode.PESSIMISTIC_WRITE);

      var existingCodeVersionEntity = experimentEntity.getCode_version_snapshot();
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
      experimentEntity.increaseVersionNumber();
      var transaction = session.beginTransaction();
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
      throws PermissionDeniedException {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {

      List<String> accessibleExperimentIds = new ArrayList<>();
      if (!queryParameters.getExperimentIdsList().isEmpty()) {
        accessibleExperimentIds.addAll(
            getAccessibleExperimentIDs(
                queryParameters.getExperimentIdsList(),
                ModelDBActionEnum.ModelDBServiceActions.READ));
        if (accessibleExperimentIds.isEmpty()) {
          throw new PermissionDeniedException(
              "Access is denied. User is unauthorized for given Experiment IDs : "
                  + accessibleExperimentIds);
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
              ModelDBConstants.EXPERIMENTS, accessibleExperimentIds, predicate, mdbRoleService);
        }
      }

      var builder = session.getCriteriaBuilder();
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
          var experimentPaginationDTO = new ExperimentPaginationDTO();
          experimentPaginationDTO.setExperiments(Collections.emptyList());
          experimentPaginationDTO.setTotalRecords(0L);
          return experimentPaginationDTO;
        }
        projectIds.addAll(workspaceProjectIDs);
      }

      if (accessibleExperimentIds.isEmpty() && projectIds.isEmpty()) {
        throw new PermissionDeniedException(
            "Access is denied. Accessible projects not found for given Experiment IDs : "
                + accessibleExperimentIds);
      }

      if (!projectIds.isEmpty()) {
        Expression<String> projectExpression = experimentRoot.get(ModelDBConstants.PROJECT_ID);
        var projectsPredicate = projectExpression.in(projectIds);
        finalPredicatesList.add(projectsPredicate);
      }

      if (!accessibleExperimentIds.isEmpty()) {
        Expression<String> exp = experimentRoot.get(ModelDBConstants.ID);
        var predicate2 = exp.in(accessibleExperimentIds);
        finalPredicatesList.add(predicate2);
      }

      var entityName = "experimentEntity";
      try {
        List<Predicate> queryPredicatesList =
            RdbmsUtils.getQueryPredicatesFromPredicateList(
                entityName,
                predicates,
                builder,
                criteriaQuery,
                experimentRoot,
                authService,
                mdbRoleService,
                ModelDBServiceResourceTypes.EXPERIMENT);
        if (!queryPredicatesList.isEmpty()) {
          finalPredicatesList.addAll(queryPredicatesList);
        }
      } catch (ModelDBException ex) {
        if (ex.getCode().ordinal() == Code.FAILED_PRECONDITION_VALUE
            && ModelDBConstants.INTERNAL_MSG_USERS_NOT_FOUND.equals(ex.getMessage())) {
          LOGGER.info(ex.getMessage());
          var experimentPaginationDTO = new ExperimentPaginationDTO();
          experimentPaginationDTO.setExperiments(Collections.emptyList());
          experimentPaginationDTO.setTotalRecords(0L);
          return experimentPaginationDTO;
        }
        throw ex;
      }

      finalPredicatesList.add(builder.equal(experimentRoot.get(ModelDBConstants.DELETED), false));
      finalPredicatesList.add(
          builder.equal(projectEntityRoot.get(ModelDBConstants.DELETED), false));

      var orderBy =
          RdbmsUtils.getOrderBasedOnSortKey(
              queryParameters.getSortKey(),
              queryParameters.getAscending(),
              builder,
              experimentRoot,
              entityName);

      var predicateArr = new Predicate[finalPredicatesList.size()];
      for (var index = 0; index < finalPredicatesList.size(); index++) {
        predicateArr[index] = finalPredicatesList.get(index);
      }

      var predicateWhereCause = builder.and(predicateArr);
      criteriaQuery.select(experimentRoot);
      criteriaQuery.where(predicateWhereCause);
      criteriaQuery.orderBy(orderBy);

      var query = session.createQuery(criteriaQuery);
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

      var experimentPaginationDTO = new ExperimentPaginationDTO();
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
      throws NotFoundException {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();
      var query =
          session
              .createQuery(GET_EXPERIMENT_BY_ID_QUERY)
              .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
      query.setParameter("id", experimentId);
      var experimentEntity = (ExperimentEntity) query.uniqueResult();
      if (experimentEntity == null) {
        throw new NotFoundException(ModelDBMessages.EXPERIMENT_NOT_FOUND_ERROR_MSG + experimentId);
      }

      assert experimentEntity != null;
      List<Artifact> existingArtifacts = experimentEntity.getProtoObject().getArtifactsList();
      for (Artifact existingArtifact : existingArtifacts) {
        for (Artifact newArtifact : newArtifacts) {
          if (existingArtifact.getKey().equals(newArtifact.getKey())) {
            throw new AlreadyExistsException(
                "Artifact being logged already exists. existing artifact key : "
                    + newArtifact.getKey());
          }
        }
      }

      experimentEntity.setArtifactMapping(
          RdbmsUtils.convertArtifactsFromArtifactEntityList(
              experimentEntity, ModelDBConstants.ARTIFACTS, newArtifacts));
      experimentEntity.setDate_updated(Calendar.getInstance().getTimeInMillis());
      experimentEntity.increaseVersionNumber();
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
  public List<Artifact> getExperimentArtifacts(String experimentId) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var query = session.createQuery(GET_EXPERIMENT_BY_ID_QUERY);
      query.setParameter("id", experimentId);
      var experimentEntity = (ExperimentEntity) query.uniqueResult();
      var experiment = experimentEntity.getProtoObject();
      if (experiment.getArtifactsList() != null && !experiment.getArtifactsList().isEmpty()) {
        LOGGER.debug("Experiment Artifacts getting successfully");
        return experiment.getArtifactsList();
      } else {
        var errorMessage = "Artifacts not found in the Experiment";
        throw new NotFoundException(errorMessage);
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
  public Experiment deleteArtifacts(String experimentId, String artifactKey) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();

      var query =
          session
              .createQuery(DELETE_ARTIFACT_QUERY)
              .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
      query.setParameter("keys", Collections.singletonList(artifactKey));
      query.setParameter(ModelDBConstants.EXPERIMENT_ID_STR, experimentId);
      query.executeUpdate();
      ExperimentEntity experimentObj = session.get(ExperimentEntity.class, experimentId);
      experimentObj.setDate_updated(Calendar.getInstance().getTimeInMillis());
      experimentObj.increaseVersionNumber();
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
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentQuery = session.createQuery(PROJ_IDS_BY_EXP_IDS_HQL);
      experimentQuery.setParameterList("experimentIds", experimentIds);
      experimentQuery.setParameter(ModelDBConstants.DELETED, false);
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
