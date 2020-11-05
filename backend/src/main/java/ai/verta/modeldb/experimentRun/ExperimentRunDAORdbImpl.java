package ai.verta.modeldb.experimentRun;

import static ai.verta.modeldb.entities.config.ConfigBlobEntity.HYPERPARAMETER;

import ai.verta.common.Artifact;
import ai.verta.common.KeyValue;
import ai.verta.common.KeyValueQuery;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.common.OperatorEnum;
import ai.verta.common.ValueTypeEnum;
import ai.verta.modeldb.App;
import ai.verta.modeldb.CloneExperimentRun;
import ai.verta.modeldb.CodeVersion;
import ai.verta.modeldb.CommitArtifactPart;
import ai.verta.modeldb.CommitArtifactPart.Response;
import ai.verta.modeldb.CommitMultipartArtifact;
import ai.verta.modeldb.Experiment;
import ai.verta.modeldb.ExperimentRun;
import ai.verta.modeldb.FindExperimentRuns;
import ai.verta.modeldb.GetCommittedArtifactParts;
import ai.verta.modeldb.GetExperimentRunsByDatasetVersionId;
import ai.verta.modeldb.GetVersionedInput;
import ai.verta.modeldb.GitSnapshot;
import ai.verta.modeldb.ListBlobExperimentRunsRequest;
import ai.verta.modeldb.ListCommitExperimentRunsRequest;
import ai.verta.modeldb.Location;
import ai.verta.modeldb.LogVersionedInput;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.Observation;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.SortExperimentRuns;
import ai.verta.modeldb.TopExperimentRunsSelector;
import ai.verta.modeldb.VersioningEntry;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.authservice.RoleService;
import ai.verta.modeldb.collaborator.CollaboratorUser;
import ai.verta.modeldb.dto.ExperimentRunPaginationDTO;
import ai.verta.modeldb.dto.WorkspaceDTO;
import ai.verta.modeldb.entities.ArtifactEntity;
import ai.verta.modeldb.entities.ArtifactPartEntity;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.CodeVersionEntity;
import ai.verta.modeldb.entities.ExperimentEntity;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.entities.KeyValueEntity;
import ai.verta.modeldb.entities.ObservationEntity;
import ai.verta.modeldb.entities.ProjectEntity;
import ai.verta.modeldb.entities.TagsMapping;
import ai.verta.modeldb.entities.code.GitCodeBlobEntity;
import ai.verta.modeldb.entities.code.NotebookCodeBlobEntity;
import ai.verta.modeldb.entities.config.ConfigBlobEntity;
import ai.verta.modeldb.entities.config.HyperparameterElementConfigBlobEntity;
import ai.verta.modeldb.entities.config.HyperparameterElementMappingEntity;
import ai.verta.modeldb.entities.dataset.PathDatasetComponentBlobEntity;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.entities.versioning.VersioningModeldbEntityMapping;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.project.ProjectDAO;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.modeldb.versioning.Blob;
import ai.verta.modeldb.versioning.BlobDAO;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.CodeBlob;
import ai.verta.modeldb.versioning.CommitDAO;
import ai.verta.modeldb.versioning.CommitFunction;
import ai.verta.modeldb.versioning.GitCodeBlob;
import ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob;
import ai.verta.modeldb.versioning.PathDatasetComponentBlob;
import ai.verta.modeldb.versioning.RepositoryDAO;
import ai.verta.modeldb.versioning.RepositoryFunction;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.modeldb.versioning.VersioningUtils;
import ai.verta.uac.ModelDBActionEnum;
import ai.verta.uac.Role;
import ai.verta.uac.UserInfo;
import com.amazonaws.services.s3.model.PartETag;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
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

public class ExperimentRunDAORdbImpl implements ExperimentRunDAO {

  private static final Logger LOGGER =
      LogManager.getLogger(ExperimentRunDAORdbImpl.class.getName());
  private static final boolean OVERWRITE_VERSION_MAP = false;
  private App app = App.getInstance();
  private static final long CACHE_SIZE = 1000;
  private static final int DURATION = 10;
  private final AuthService authService;
  private final RoleService roleService;
  private final RepositoryDAO repositoryDAO;
  private final CommitDAO commitDAO;
  private final BlobDAO blobDAO;
  private final MetadataDAO metadataDAO;
  private static final String CHECK_EXP_RUN_EXISTS_AT_INSERT_HQL =
      new StringBuilder("Select count(*) From ExperimentRunEntity ere where ")
          .append(" ere." + ModelDBConstants.NAME + " = :experimentRunName ")
          .append(" AND ere." + ModelDBConstants.PROJECT_ID + " = :projectId ")
          .append(" AND ere." + ModelDBConstants.EXPERIMENT_ID + " = :experimentId ")
          .append(" AND ere." + ModelDBConstants.DELETED + " = false ")
          .toString();
  private static final String CHECK_EXP_RUN_EXISTS_AT_UPDATE_HQL =
      new StringBuilder("Select count(*) From ExperimentRunEntity ere where ")
          .append(" ere." + ModelDBConstants.ID + " = :experimentRunId ")
          .append(" AND ere." + ModelDBConstants.DELETED + " = false ")
          .toString();
  private static final String GET_EXP_RUN_BY_IDS_HQL =
      "From ExperimentRunEntity exr where exr.id IN (:ids) AND exr."
          + ModelDBConstants.DELETED
          + " = false ";
  private static final String DELETE_ALL_TAGS_HQL =
      new StringBuilder("delete from TagsMapping tm WHERE tm.experimentRunEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :experimentRunId")
          .toString();
  private static final String DELETE_SELECTED_TAGS_HQL =
      new StringBuilder("delete from TagsMapping tm WHERE tm.")
          .append(ModelDBConstants.TAGS)
          .append(" in (:tags) AND tm.experimentRunEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :experimentRunId")
          .toString();
  private static final String DELETE_ALL_ARTIFACTS_HQL =
      new StringBuilder("delete from ArtifactEntity ar WHERE ar.experimentRunEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :experimentRunId")
          .toString();
  private static final String DELETE_SELECTED_ARTIFACTS_HQL =
      new StringBuilder("delete from ArtifactEntity ar WHERE ar.")
          .append(ModelDBConstants.KEY)
          .append(" in (:keys) AND ar.experimentRunEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :experimentRunId ")
          .append(" AND ar.field_type = :field_type")
          .toString();
  private static final String GET_EXP_RUN_ATTRIBUTE_BY_KEYS_HQL =
      new StringBuilder("From AttributeEntity attr where attr.")
          .append(ModelDBConstants.KEY)
          .append(" in (:keys) AND attr.experimentRunEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :experimentRunId AND attr.field_type = :fieldType")
          .toString();
  private static final String DELETE_ALL_EXP_RUN_ATTRIBUTES_HQL =
      new StringBuilder("delete from AttributeEntity attr WHERE attr.experimentRunEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :experimentRunId AND attr.field_type = :fieldType")
          .toString();
  private static final String DELETE_SELECTED_EXP_RUN_ATTRIBUTES_HQL =
      new StringBuilder("delete from AttributeEntity attr WHERE attr.")
          .append(ModelDBConstants.KEY)
          .append(" in (:keys) AND attr.experimentRunEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :experimentRunId AND attr.field_type = :fieldType")
          .toString();
  private static final String GET_EXPERIMENT_RUN_BY_PROJECT_ID_HQL =
      new StringBuilder()
          .append("From ExperimentRunEntity ere where ere.")
          .append(ModelDBConstants.PROJECT_ID)
          .append(" IN (:projectIds) ")
          .append(" AND ere." + ModelDBConstants.DELETED + " = false ")
          .toString();
  private static final String GET_EXPERIMENT_RUN_BY_EXPERIMENT_ID_HQL =
      new StringBuilder()
          .append("From ExperimentRunEntity ere where ere.")
          .append(ModelDBConstants.EXPERIMENT_ID)
          .append(" IN (:experimentIds) ")
          .append(" AND ere." + ModelDBConstants.DELETED + " = false ")
          .toString();
  private static final String DELETED_STATUS_EXPERIMENT_RUN_QUERY_STRING =
      new StringBuilder("UPDATE ")
          .append(ExperimentRunEntity.class.getSimpleName())
          .append(" expr ")
          .append("SET expr.")
          .append(ModelDBConstants.DELETED)
          .append(" = :deleted ")
          .append(" WHERE expr.")
          .append(ModelDBConstants.ID)
          .append(" IN (:experimentRunIds)")
          .toString();
  private static final String DELETE_ALL_KEY_VALUES_HQL =
      new StringBuilder("delete from KeyValueEntity kv WHERE kv.experimentRunEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :experimentRunId")
          .append(" AND kv.field_type = :field_type")
          .toString();
  private static final String DELETE_SELECTED_KEY_VALUES_HQL =
      new StringBuilder("delete from KeyValueEntity kv WHERE kv.")
          .append(ModelDBConstants.KEY)
          .append(" in (:keys) AND kv.experimentRunEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :experimentRunId ")
          .append(" AND kv.field_type = :field_type")
          .toString();
  private static final String GET_ALL_OBSERVATIONS_HQL =
      new StringBuilder("FROM ObservationEntity oe WHERE oe.experimentRunEntity.")
          .append(ModelDBConstants.ID)
          .append(" = :experimentRunId")
          .append(" AND oe.field_type = :field_type")
          .toString();

  private LoadingCache<String, ReadWriteLock> locks =
      CacheBuilder.newBuilder()
          .maximumSize(CACHE_SIZE)
          .expireAfterWrite(DURATION, TimeUnit.MINUTES)
          .build(
              new CacheLoader<String, ReadWriteLock>() {
                public ReadWriteLock load(String lockKey) {
                  return new ReentrantReadWriteLock() {};
                }
              });

  protected AutoCloseable acquireReadLock(String lockKey) throws ExecutionException {
    LOGGER.debug("acquireReadLock for key: {}", lockKey);
    ReadWriteLock lock = locks.get(lockKey);
    Lock readLock = lock.readLock();
    readLock.lock();
    return readLock::unlock;
  }

  protected AutoCloseable acquireWriteLock(String lockKey) throws ExecutionException {
    LOGGER.debug("acquireWriteLock for key: {}", lockKey);
    ReadWriteLock lock = locks.get(lockKey);
    Lock writeLock = lock.writeLock();
    writeLock.lock();
    return writeLock::unlock;
  }

  public ExperimentRunDAORdbImpl(
      AuthService authService,
      RoleService roleService,
      RepositoryDAO repositoryDAO,
      CommitDAO commitDAO,
      BlobDAO blobDAO,
      MetadataDAO metadataDAO) {
    this.authService = authService;
    this.roleService = roleService;
    this.repositoryDAO = repositoryDAO;
    this.commitDAO = commitDAO;
    this.blobDAO = blobDAO;
    this.metadataDAO = metadataDAO;
  }

  private void checkIfEntityAlreadyExists(ExperimentRun experimentRun, Boolean isInsert) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query query = null;
      if (isInsert) {
        query = session.createQuery(CHECK_EXP_RUN_EXISTS_AT_INSERT_HQL);
      } else {
        query = session.createQuery(CHECK_EXP_RUN_EXISTS_AT_UPDATE_HQL);
      }

      if (isInsert) {
        query.setParameter("experimentRunName", experimentRun.getName());
        query.setParameter("projectId", experimentRun.getProjectId());
        query.setParameter("experimentId", experimentRun.getExperimentId());
      } else {
        query.setParameter(ModelDBConstants.EXPERIMENT_RUN_ID_STR, experimentRun.getId());
      }
      Long count = (Long) query.uniqueResult();
      boolean existStatus = false;
      if (count > 0) {
        existStatus = true;
      }

      // Throw error if it is an insert request and ExperimentRun with same name already exists
      if (existStatus && isInsert) {
        Status status =
            Status.newBuilder()
                .setCode(Code.ALREADY_EXISTS_VALUE)
                .setMessage("ExperimentRun already exists in database")
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      } else if (!existStatus && !isInsert) {
        // Throw error if it is an update request and ExperimentRun with given name does not exist
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage("ExperimentRun does not exist in database")
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        checkIfEntityAlreadyExists(experimentRun, isInsert);
      } else {
        throw ex;
      }
    }
  }

  /**
   * @param session : hibernate session
   * @param versioningEntry : versioningEntry
   * @return returns a map from location to an Entry of BlobExpanded and sha
   * @throws ModelDBException ModelDBException
   */
  private Map<String, Map.Entry<BlobExpanded, String>> validateVersioningEntity(
      Session session, VersioningEntry versioningEntry) throws ModelDBException {
    String errorMessage = null;
    if (versioningEntry.getRepositoryId() == 0L) {
      errorMessage = "Repository Id not found in VersioningEntry";
    } else if (versioningEntry.getCommit().isEmpty()) {
      errorMessage = "Commit hash not found in VersioningEntry";
    }

    if (errorMessage != null) {
      throw new ModelDBException(errorMessage, io.grpc.Status.Code.INVALID_ARGUMENT);
    }
    RepositoryIdentification repositoryIdentification =
        RepositoryIdentification.newBuilder().setRepoId(versioningEntry.getRepositoryId()).build();
    CommitEntity commitEntity =
        commitDAO.getCommitEntity(
            session,
            versioningEntry.getCommit(),
            (session1) -> repositoryDAO.getRepositoryById(session, repositoryIdentification));
    Map<String, Map.Entry<BlobExpanded, String>> requestedLocationBlobWithHashMap = new HashMap<>();
    if (!versioningEntry.getKeyLocationMapMap().isEmpty()) {
      Map<String, Map.Entry<BlobExpanded, String>> locationBlobWithHashMap =
          blobDAO.getCommitBlobMapWithHash(
              session, commitEntity.getRootSha(), new ArrayList<>(), Collections.emptyList());
      for (Map.Entry<String, Location> locationBlobKeyMap :
          versioningEntry.getKeyLocationMapMap().entrySet()) {
        String locationKey = String.join("#", locationBlobKeyMap.getValue().getLocationList());
        if (!locationBlobWithHashMap.containsKey(locationKey)) {
          throw new ModelDBException(
              "Blob Location '"
                  + locationBlobKeyMap.getValue().getLocationList()
                  + "' for key '"
                  + locationBlobKeyMap.getKey()
                  + "' not found in commit blobs",
              io.grpc.Status.Code.INVALID_ARGUMENT);
        }
        requestedLocationBlobWithHashMap.put(locationKey, locationBlobWithHashMap.get(locationKey));
      }
    }
    return requestedLocationBlobWithHashMap;
  }

  @Override
  public ExperimentRun insertExperimentRun(
      ProjectDAO projectDAO, ExperimentRun experimentRun, UserInfo userInfo)
      throws InvalidProtocolBufferException, ModelDBException {
    checkIfEntityAlreadyExists(experimentRun, true);
    createRoleBindingsForExperimentRun(experimentRun, userInfo);
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      validateExperimentRunPerWorkspaceForTrial(projectDAO, experimentRun.getProjectId(), userInfo);
      validateMaxArtifactsForTrial(experimentRun.getArtifactsCount(), 0);

      if (experimentRun.getDatasetsCount() > 0 && app.isPopulateConnectionsBasedOnPrivileges()) {
        experimentRun = checkDatasetVersionBasedOnPrivileges(experimentRun, true);
      }

      ExperimentRunEntity experimentRunObj = RdbmsUtils.generateExperimentRunEntity(experimentRun);
      if (experimentRun.getVersionedInputs() != null && experimentRun.hasVersionedInputs()) {
        Map<String, Map.Entry<BlobExpanded, String>> locationBlobWithHashMap =
            validateVersioningEntity(session, experimentRun.getVersionedInputs());
        List<VersioningModeldbEntityMapping> versioningModeldbEntityMappings =
            RdbmsUtils.getVersioningMappingFromVersioningInput(
                session,
                experimentRun.getVersionedInputs(),
                locationBlobWithHashMap,
                experimentRunObj);
        experimentRunObj.setVersioned_inputs(versioningModeldbEntityMappings);
        Set<HyperparameterElementMappingEntity> hyrParamMappings =
            prepareHyperparameterElemMappings(experimentRunObj, versioningModeldbEntityMappings);

        if (!hyrParamMappings.isEmpty()) {
          experimentRunObj.setHyperparameter_element_mappings(new ArrayList<>(hyrParamMappings));
        }
      }
      Transaction transaction = session.beginTransaction();
      session.saveOrUpdate(experimentRunObj);
      transaction.commit();
      LOGGER.debug("ExperimentRun created successfully");
      return experimentRun;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return insertExperimentRun(projectDAO, experimentRun, userInfo);
      } else {
        throw ex;
      }
    }
  }

  private void createRoleBindingsForExperimentRun(ExperimentRun experimentRun, UserInfo userInfo) {
    Role ownerRole = roleService.getRoleByName(ModelDBConstants.ROLE_EXPERIMENT_RUN_OWNER, null);
    roleService.createRoleBinding(
        ownerRole,
        new CollaboratorUser(authService, userInfo),
        experimentRun.getId(),
        ModelDBServiceResourceTypes.EXPERIMENT_RUN);
  }

  private Set<HyperparameterElementMappingEntity> prepareHyperparameterElemMappings(
      ExperimentRunEntity experimentRunObj,
      List<VersioningModeldbEntityMapping> versioningModeldbEntityMappings) {
    Set<HyperparameterElementMappingEntity> hyrParamMappings = new HashSet<>();
    versioningModeldbEntityMappings.forEach(
        versioningModeldbEntityMapping ->
            versioningModeldbEntityMapping
                .getConfig_blob_entities()
                .forEach(
                    configBlobEntity -> {
                      if (configBlobEntity.getHyperparameter_type().equals(HYPERPARAMETER)) {
                        HyperparameterElementConfigBlobEntity hyperparamElemConfBlobEntity =
                            configBlobEntity.getHyperparameterElementConfigBlobEntity();
                        try {
                          HyperparameterElementMappingEntity hyrParamMapping =
                              new HyperparameterElementMappingEntity(
                                  experimentRunObj,
                                  hyperparamElemConfBlobEntity.getName(),
                                  hyperparamElemConfBlobEntity.toProto());
                          hyrParamMappings.add(hyrParamMapping);
                        } catch (ModelDBException e) {
                          // This is never call because if something is wrong at this point then
                          // error will throw before this running 'for' loop
                          LOGGER.warn(e.getMessage());
                        }
                      }
                    }));
    return hyrParamMappings;
  }

  @Override
  public Boolean deleteExperimentRuns(List<String> experimentRunIds) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {

      List<String> accessibleExperimentRunIds =
          getAccessibleExperimentRunIDs(
              experimentRunIds, ModelDBActionEnum.ModelDBServiceActions.UPDATE);
      if (accessibleExperimentRunIds.isEmpty()) {
        Status statusMessage =
            Status.newBuilder()
                .setCode(Code.PERMISSION_DENIED_VALUE)
                .setMessage(
                    "Access is denied. User is unauthorized for given ExperimentRun entities : "
                        + accessibleExperimentRunIds)
                .build();
        throw StatusProto.toStatusRuntimeException(statusMessage);
      }
      Transaction transaction = session.beginTransaction();
      Query query = session.createQuery(DELETED_STATUS_EXPERIMENT_RUN_QUERY_STRING);
      query.setParameter("deleted", true);
      query.setParameter("experimentRunIds", accessibleExperimentRunIds);
      int updatedCount = query.executeUpdate();
      LOGGER.debug(
          "Mark ExperimentRun as deleted : {}, count : {}",
          accessibleExperimentRunIds,
          updatedCount);
      transaction.commit();
      LOGGER.debug("ExperimentRun deleted successfully");
      return true;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteExperimentRuns(experimentRunIds);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public ExperimentRunPaginationDTO getExperimentRunsFromEntity(
      ProjectDAO projectDAO,
      String entityKey,
      String entityValue,
      Integer pageNumber,
      Integer pageLimit,
      Boolean order,
      String sortKey)
      throws InvalidProtocolBufferException {

    KeyValueQuery entityKeyValuePredicate =
        KeyValueQuery.newBuilder()
            .setKey(entityKey)
            .setValue(Value.newBuilder().setStringValue(entityValue).build())
            .build();

    FindExperimentRuns findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .setPageNumber(pageNumber)
            .setPageLimit(pageLimit)
            .setAscending(order)
            .setSortKey(sortKey)
            .addPredicates(entityKeyValuePredicate)
            .build();
    UserInfo currentLoginUserInfo = authService.getCurrentLoginUserInfo();
    return findExperimentRuns(projectDAO, currentLoginUserInfo, findExperimentRuns);
  }

  @Override
  public List<ExperimentRun> getExperimentRuns(String key, String value, UserInfo userInfo)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      List<ExperimentRun> experimentRuns = new ArrayList<>();

      Map<String, Object[]> whereClauseParamMap = new HashMap<>();
      Object[] idValueArr = new Object[2];
      idValueArr[0] = RdbmsUtils.getRdbOperatorSymbol(OperatorEnum.Operator.EQ);
      idValueArr[1] = value;
      whereClauseParamMap.put(key, idValueArr);

      LOGGER.debug("Getting experimentRun for {} ", userInfo);
      if (userInfo != null) {
        Object[] ownerValueArr = new Object[2];
        ownerValueArr[0] = RdbmsUtils.getRdbOperatorSymbol(OperatorEnum.Operator.EQ);
        ownerValueArr[1] = authService.getVertaIdFromUserInfo(userInfo);
        whereClauseParamMap.put(ModelDBConstants.OWNER, ownerValueArr);
      }

      Map<String, Object> dataWithCountMap =
          RdbmsUtils.findListWithPagination(
              session,
              ExperimentRunEntity.class.getSimpleName(),
              null,
              whereClauseParamMap,
              null,
              null,
              false,
              null,
              false);
      @SuppressWarnings("unchecked")
      List<ExperimentRunEntity> experimentRunEntities =
          (List<ExperimentRunEntity>) dataWithCountMap.get(ModelDBConstants.DATA_LIST);
      LOGGER.debug("ExperimentRunEntity List size is {}", experimentRunEntities.size());

      if (!experimentRunEntities.isEmpty()) {
        experimentRuns =
            RdbmsUtils.convertExperimentRunsFromExperimentRunEntityList(experimentRunEntities);
      }
      LOGGER.debug("ExperimentRuns size is {}", experimentRuns.size());
      return experimentRuns;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperimentRuns(key, value, userInfo);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<ExperimentRun> getExperimentRunsByBatchIds(List<String> experimentRunIds)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query query = session.createQuery(GET_EXP_RUN_BY_IDS_HQL);
      query.setParameterList("ids", experimentRunIds);

      @SuppressWarnings("unchecked")
      List<ExperimentRunEntity> experimentRunEntities = query.list();
      LOGGER.debug("Got ExperimentRun by Ids");
      return RdbmsUtils.convertExperimentRunsFromExperimentRunEntityList(experimentRunEntities);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperimentRunsByBatchIds(experimentRunIds);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public ExperimentRun getExperimentRun(String experimentRunId)
      throws InvalidProtocolBufferException, ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunEntity =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunEntity == null) {
        LOGGER.info(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      LOGGER.debug("Got ExperimentRun successfully");
      ExperimentRun experimentRun = experimentRunEntity.getProtoObject();
      return populateFieldsBasedOnPrivileges(experimentRun);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperimentRun(experimentRunId);
      } else {
        throw ex;
      }
    }
  }

  private ExperimentRun populateFieldsBasedOnPrivileges(ExperimentRun experimentRun)
      throws ModelDBException {
    if (app.isPopulateConnectionsBasedOnPrivileges()) {
      if (experimentRun.getDatasetsCount() > 0) {
        experimentRun = checkDatasetVersionBasedOnPrivileges(experimentRun, false);
      }
      if (experimentRun.getVersionedInputs() != null
          && experimentRun.getVersionedInputs().getRepositoryId() != 0) {
        experimentRun =
            checkVersionInputBasedOnPrivileges(experimentRun, new HashSet<>(), new HashSet<>());
      }
    }
    return experimentRun;
  }

  /**
   * @param errorOut : Throw error while creation (true) otherwise we will keep it silent (false)
   */
  private ExperimentRun checkDatasetVersionBasedOnPrivileges(
      ExperimentRun experimentRun, boolean errorOut) throws ModelDBException {
    ExperimentRun.Builder experimentRunBuilder = experimentRun.toBuilder();
    List<Artifact> accessibleDatasetVersions =
        getPrivilegedDatasets(experimentRun.getDatasetsList(), errorOut);
    experimentRunBuilder.clearDatasets().addAllDatasets(accessibleDatasetVersions);
    return experimentRunBuilder.build();
  }

  /**
   * @param newDatasets : new datasets for privilege check
   * @param errorOut : Throw error while creation (true) otherwise we will keep it silent (false)
   * @return {@link List} : accessible datasets
   * @throws ModelDBException: modelDBException
   */
  private List<Artifact> getPrivilegedDatasets(List<Artifact> newDatasets, boolean errorOut)
      throws ModelDBException {
    List<Artifact> accessibleDatasets = new ArrayList<>();
    List<String> accessibleDatasetVersionIds = new ArrayList<>();
    for (Artifact dataset : newDatasets) {
      String datasetVersionId = dataset.getLinkedArtifactId();
      if (!datasetVersionId.isEmpty() && !accessibleDatasetVersionIds.contains(datasetVersionId)) {
        try {
          commitDAO.getDatasetVersionById(repositoryDAO, blobDAO, metadataDAO, datasetVersionId);
          accessibleDatasets.add(dataset);
          accessibleDatasetVersionIds.add(datasetVersionId);
        } catch (Exception ex) {
          LOGGER.debug(ex.getMessage());
          if (errorOut) {
            throw ex;
          }
        }
      } else {
        accessibleDatasets.add(dataset);
      }
    }
    return accessibleDatasets;
  }

  private ExperimentRun checkVersionInputBasedOnPrivileges(
      ExperimentRun experimentRun,
      Set<Long> accessibleRepoIdsSet,
      Set<Long> notAccessibleRepoIdIdsSet) {
    Long repoId = experimentRun.getVersionedInputs().getRepositoryId();
    if (accessibleRepoIdsSet.contains(repoId)) {
      accessibleRepoIdsSet.add(repoId);
    } else if (notAccessibleRepoIdIdsSet.contains(repoId)) {
      notAccessibleRepoIdIdsSet.add(repoId);
      experimentRun = experimentRun.toBuilder().clearVersionedInputs().build();
    } else {
      if (roleService.checkConnectionsBasedOnPrivileges(
          ModelDBServiceResourceTypes.REPOSITORY,
          ModelDBActionEnum.ModelDBServiceActions.READ,
          String.valueOf(repoId))) {
        accessibleRepoIdsSet.add(repoId);
      } else {
        experimentRun = experimentRun.toBuilder().clearVersionedInputs().build();
        notAccessibleRepoIdIdsSet.add(repoId);
      }
    }
    return experimentRun;
  }

  @Override
  public boolean isExperimentRunExists(Session session, String experimentRunId) {
    Query query = session.createQuery(CHECK_EXP_RUN_EXISTS_AT_UPDATE_HQL);
    query.setParameter("experimentRunId", experimentRunId);
    Long count = (Long) query.uniqueResult();
    return count > 0;
  }

  @Override
  public void updateExperimentRunName(String experimentRunId, String experimentRunName) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunEntity =
          session.load(ExperimentRunEntity.class, experimentRunId);
      experimentRunEntity.setName(experimentRunName);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntity.setDate_updated(currentTimestamp);
      Transaction transaction = session.beginTransaction();
      session.update(experimentRunEntity);
      transaction.commit();
      LOGGER.debug("ExperimentRun name updated successfully");
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        updateExperimentRunName(experimentRunId, experimentRunName);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public ExperimentRun updateExperimentRunDescription(
      String experimentRunId, String experimentRunDescription)
      throws InvalidProtocolBufferException, ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunEntity =
          session.load(ExperimentRunEntity.class, experimentRunId);
      experimentRunEntity.setDescription(experimentRunDescription);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntity.setDate_updated(currentTimestamp);
      Transaction transaction = session.beginTransaction();
      session.update(experimentRunEntity);
      transaction.commit();
      LOGGER.debug("ExperimentRun description updated successfully");
      ExperimentRun experimentRun = experimentRunEntity.getProtoObject();
      return populateFieldsBasedOnPrivileges(experimentRun);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return updateExperimentRunDescription(experimentRunId, experimentRunDescription);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public void logExperimentRunCodeVersion(String experimentRunId, CodeVersion updatedCodeVersion)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunEntity =
          session.get(ExperimentRunEntity.class, experimentRunId);

      CodeVersionEntity existingCodeVersionEntity = experimentRunEntity.getCode_version_snapshot();
      if (existingCodeVersionEntity == null) {
        experimentRunEntity.setCode_version_snapshot(
            RdbmsUtils.generateCodeVersionEntity(
                ModelDBConstants.CODE_VERSION, updatedCodeVersion));
      } else {
        session.delete(existingCodeVersionEntity);
        experimentRunEntity.setCode_version_snapshot(
            RdbmsUtils.generateCodeVersionEntity(
                ModelDBConstants.CODE_VERSION, updatedCodeVersion));
      }
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntity.setDate_updated(currentTimestamp);
      Transaction transaction = session.beginTransaction();
      session.update(experimentRunEntity);
      transaction.commit();
      LOGGER.debug("ExperimentRun code version snapshot updated successfully");
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        logExperimentRunCodeVersion(experimentRunId, updatedCodeVersion);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public void setParentExperimentRunId(String experimentRunId, String parentExperimentRunId) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunEntity =
          session.load(ExperimentRunEntity.class, experimentRunId);
      experimentRunEntity.setParent_id(parentExperimentRunId);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntity.setDate_updated(currentTimestamp);
      Transaction transaction = session.beginTransaction();
      session.update(experimentRunEntity);
      transaction.commit();
      LOGGER.debug("ExperimentRun parentId updated successfully");
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        setParentExperimentRunId(experimentRunId, parentExperimentRunId);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public ExperimentRun addExperimentRunTags(String experimentRunId, List<String> tagsList)
      throws InvalidProtocolBufferException, ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunObj == null) {
        LOGGER.info(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      List<String> newTags = new ArrayList<>();
      ExperimentRun existingProtoExperimentRunObj = experimentRunObj.getProtoObject();
      for (String tag : tagsList) {
        if (!existingProtoExperimentRunObj.getTagsList().contains(tag)) {
          newTags.add(tag);
        }
      }
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      if (!newTags.isEmpty()) {
        List<TagsMapping> newTagMappings =
            RdbmsUtils.convertTagListFromTagMappingList(experimentRunObj, newTags);
        experimentRunObj.getTags().addAll(newTagMappings);
        experimentRunObj.setDate_updated(currentTimestamp);
        Transaction transaction = session.beginTransaction();
        session.saveOrUpdate(experimentRunObj);
        transaction.commit();
      }
      LOGGER.debug("ExperimentRun tags added successfully");
      ExperimentRun experimentRun = experimentRunObj.getProtoObject();
      return populateFieldsBasedOnPrivileges(experimentRun);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return addExperimentRunTags(experimentRunId, tagsList);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public ExperimentRun deleteExperimentRunTags(
      String experimentRunId, List<String> experimentRunTagList, Boolean deleteAll)
      throws InvalidProtocolBufferException, ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      if (deleteAll) {
        Query query = session.createQuery(DELETE_ALL_TAGS_HQL);
        query.setParameter(ModelDBConstants.EXPERIMENT_RUN_ID_STR, experimentRunId);
        query.executeUpdate();
      } else {
        Query query = session.createQuery(DELETE_SELECTED_TAGS_HQL);
        query.setParameter("tags", experimentRunTagList);
        query.setParameter(ModelDBConstants.EXPERIMENT_RUN_ID_STR, experimentRunId);
        query.executeUpdate();
      }
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunObj.setDate_updated(currentTimestamp);
      session.update(experimentRunObj);
      transaction.commit();
      LOGGER.debug("ExperimentRun tags deleted successfully");
      ExperimentRun experimentRun = experimentRunObj.getProtoObject();
      return populateFieldsBasedOnPrivileges(experimentRun);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteExperimentRunTags(experimentRunId, experimentRunTagList, deleteAll);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public void logObservations(String experimentRunId, List<Observation> observations)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunEntityObj == null) {
        LOGGER.info(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      Transaction transaction = session.beginTransaction();
      List<ObservationEntity> newObservationList =
          RdbmsUtils.convertObservationsFromObservationEntityList(
              session,
              experimentRunEntityObj,
              ModelDBConstants.OBSERVATIONS,
              observations,
              ExperimentRunEntity.class.getSimpleName(),
              experimentRunEntityObj.getId());
      experimentRunEntityObj.setObservationMapping(newObservationList);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntityObj.setDate_updated(currentTimestamp);
      session.saveOrUpdate(experimentRunEntityObj);
      transaction.commit();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        logObservations(experimentRunId, observations);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<Observation> getObservationByKey(String experimentRunId, String observationKey)
      throws InvalidProtocolBufferException {

    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunEntityObj == null) {
        LOGGER.info(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      ExperimentRun experimentRun = experimentRunEntityObj.getProtoObject();
      List<Observation> observationEntities = new ArrayList<>();
      for (Observation observation : experimentRun.getObservationsList()) {
        if ((observation.hasArtifact() && observation.getArtifact().getKey().equals(observationKey))
            || (observation.hasAttribute()
                && observation.getAttribute().getKey().equals(observationKey))) {
          observationEntities.add(observation);
        }
      }
      return observationEntities;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getObservationByKey(experimentRunId, observationKey);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public void logMetrics(String experimentRunId, List<KeyValue> newMetrics)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunEntityObj == null) {
        LOGGER.info(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<KeyValue> existingMetrics = experimentRunEntityObj.getProtoObject().getMetricsList();
      for (KeyValue existingMetric : existingMetrics) {
        for (KeyValue newMetric : newMetrics) {
          if (existingMetric.getKey().equals(newMetric.getKey())) {
            Status status =
                Status.newBuilder()
                    .setCode(Code.ALREADY_EXISTS_VALUE)
                    .setMessage(
                        "Metric being logged already exists. existing metric Key : "
                            + newMetric.getKey())
                    .build();
            throw StatusProto.toStatusRuntimeException(status);
          }
        }
      }

      List<KeyValueEntity> newMetricList =
          RdbmsUtils.convertKeyValuesFromKeyValueEntityList(
              experimentRunEntityObj, ModelDBConstants.METRICS, newMetrics);
      experimentRunEntityObj.setKeyValueMapping(newMetricList);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntityObj.setDate_updated(currentTimestamp);
      Transaction transaction = session.beginTransaction();
      session.saveOrUpdate(experimentRunEntityObj);
      transaction.commit();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        logMetrics(experimentRunId, newMetrics);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<KeyValue> getExperimentRunMetrics(String experimentRunId)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunObj == null) {
        LOGGER.info(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      LOGGER.debug("Got ExperimentRun Metrics");
      return experimentRunObj.getProtoObject().getMetricsList();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperimentRunMetrics(experimentRunId);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<Artifact> getExperimentRunDatasets(String experimentRunId)
      throws InvalidProtocolBufferException, ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunObj == null) {
        LOGGER.info(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      LOGGER.debug("Got ExperimentRun Datasets");
      ExperimentRun experimentRun = experimentRunObj.getProtoObject();
      experimentRun = populateFieldsBasedOnPrivileges(experimentRun);
      return experimentRun.getDatasetsList();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperimentRunDatasets(experimentRunId);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public void logDatasets(String experimentRunId, List<Artifact> newDatasets, boolean overwrite)
      throws InvalidProtocolBufferException, ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunEntityObj == null) {
        LOGGER.info(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      ExperimentRun experimentRun = experimentRunEntityObj.getProtoObject();

      Transaction transaction = session.beginTransaction();
      if (overwrite) {
        List<String> datasetKeys = new ArrayList<>();
        for (Artifact dataset : newDatasets) {
          datasetKeys.add(dataset.getKey());
        }
        deleteArtifactEntities(session, experimentRunId, datasetKeys, ModelDBConstants.DATASETS);

      } else {
        List<Artifact> existingDatasets = experimentRun.getDatasetsList();
        for (Artifact existingDataset : existingDatasets) {
          for (Artifact newDataset : newDatasets) {
            if (existingDataset.getKey().equals(newDataset.getKey())) {
              Status status =
                  Status.newBuilder()
                      .setCode(Code.ALREADY_EXISTS_VALUE)
                      .setMessage(
                          "Dataset being logged already exists. existing dataSet key : "
                              + newDataset.getKey())
                      .build();
              throw StatusProto.toStatusRuntimeException(status);
            }
          }
        }
      }

      if (app.isPopulateConnectionsBasedOnPrivileges()) {
        newDatasets = getPrivilegedDatasets(newDatasets, true);
      }

      List<ArtifactEntity> newDatasetList =
          RdbmsUtils.convertArtifactsFromArtifactEntityList(
              experimentRunEntityObj, ModelDBConstants.DATASETS, newDatasets);
      experimentRunEntityObj.setArtifactMapping(newDatasetList);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntityObj.setDate_updated(currentTimestamp);
      session.saveOrUpdate(experimentRunEntityObj);
      transaction.commit();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        logDatasets(experimentRunId, newDatasets, overwrite);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public void logArtifacts(String experimentRunId, List<Artifact> newArtifacts)
      throws InvalidProtocolBufferException, ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunEntityObj == null) {
        LOGGER.info(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<Artifact> existingArtifacts = experimentRunEntityObj.getProtoObject().getArtifactsList();
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

      validateMaxArtifactsForTrial(newArtifacts.size(), existingArtifacts.size());

      List<ArtifactEntity> newArtifactList =
          RdbmsUtils.convertArtifactsFromArtifactEntityList(
              experimentRunEntityObj, ModelDBConstants.ARTIFACTS, newArtifacts);
      experimentRunEntityObj.setArtifactMapping(newArtifactList);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntityObj.setDate_updated(currentTimestamp);
      Transaction transaction = session.beginTransaction();
      session.saveOrUpdate(experimentRunEntityObj);
      transaction.commit();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        logArtifacts(experimentRunId, newArtifacts);
      } else {
        throw ex;
      }
    }
  }

  public void validateMaxArtifactsForTrial(int newArtifactsSize, int existingArtifactsSize)
      throws ModelDBException {
    if (app.getTrialEnabled()) {
      if (app.getMaxArtifactPerRun() != null
          && existingArtifactsSize + newArtifactsSize > app.getMaxArtifactPerRun()) {
        throw new ModelDBException(
            ModelDBConstants.LIMIT_RUN_ARTIFACT_NUMBER
                + "Maximum "
                + app.getMaxArtifactPerRun()
                + " artifacts are allow in the experimentRun",
            Code.RESOURCE_EXHAUSTED);
      }
    }
  }

  @Override
  public List<Artifact> getExperimentRunArtifacts(String experimentRunId)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunObj == null) {
        LOGGER.info(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      ExperimentRun experimentRun = experimentRunObj.getProtoObject();
      if (experimentRun.getArtifactsList() != null && !experimentRun.getArtifactsList().isEmpty()) {
        LOGGER.debug("Got ExperimentRun Artifacts");
        return experimentRun.getArtifactsList();
      } else {
        String errorMessage = "Artifacts not found in the ExperimentRun";
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperimentRunArtifacts(experimentRunId);
      } else {
        throw ex;
      }
    }
  }

  private void deleteArtifactEntities(
      Session session, String experimentRunId, List<String> keys, String fieldType) {
    Query query = session.createQuery(DELETE_SELECTED_ARTIFACTS_HQL);
    query.setParameterList("keys", keys);
    query.setParameter(ModelDBConstants.EXPERIMENT_RUN_ID_STR, experimentRunId);
    query.setParameter("field_type", fieldType);
    query.executeUpdate();
  }

  @Override
  public void deleteArtifacts(String experimentRunId, String artifactKey) {
    Transaction transaction = null;
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      transaction = session.beginTransaction();

      if (false) { // Change it with parameter for support to delete all artifacts
        Query query = session.createQuery(DELETE_ALL_ARTIFACTS_HQL);
        query.setParameter(ModelDBConstants.EXPERIMENT_RUN_ID_STR, experimentRunId);
        query.executeUpdate();
      } else {
        deleteArtifactEntities(
            session,
            experimentRunId,
            Collections.singletonList(artifactKey),
            ModelDBConstants.ARTIFACTS);
      }
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      experimentRunObj.setDate_updated(currentTimestamp);
      session.update(experimentRunObj);
      transaction.commit();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        deleteArtifacts(experimentRunId, artifactKey);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public void logHyperparameters(String experimentRunId, List<KeyValue> newHyperparameters)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunEntityObj == null) {
        LOGGER.info(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<KeyValue> existingHyperparameters =
          experimentRunEntityObj.getProtoObject().getHyperparametersList();
      for (KeyValue existingHyperparameter : existingHyperparameters) {
        for (KeyValue newHyperparameter : newHyperparameters) {
          if (existingHyperparameter.getKey().equals(newHyperparameter.getKey())) {
            Status status =
                Status.newBuilder()
                    .setCode(Code.ALREADY_EXISTS_VALUE)
                    .setMessage(
                        "Hyperparameter being logged already exists. existing hyperparameter Key : "
                            + newHyperparameter.getKey())
                    .build();
            throw StatusProto.toStatusRuntimeException(status);
          }
        }
      }

      List<KeyValueEntity> newHyperparameterList =
          RdbmsUtils.convertKeyValuesFromKeyValueEntityList(
              experimentRunEntityObj, ModelDBConstants.HYPERPARAMETERS, newHyperparameters);
      experimentRunEntityObj.setKeyValueMapping(newHyperparameterList);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntityObj.setDate_updated(currentTimestamp);
      Transaction transaction = session.beginTransaction();
      session.saveOrUpdate(experimentRunEntityObj);
      transaction.commit();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        logHyperparameters(experimentRunId, newHyperparameters);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<KeyValue> getExperimentRunHyperparameters(String experimentRunId)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunObj == null) {
        LOGGER.info(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      LOGGER.debug("Got ExperimentRun Hyperparameters");
      return experimentRunObj.getProtoObject().getHyperparametersList();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperimentRunHyperparameters(experimentRunId);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public void logAttributes(String experimentRunId, List<KeyValue> newAttributes)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunEntityObj == null) {
        LOGGER.info(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      List<KeyValue> existingAttributes =
          experimentRunEntityObj.getProtoObject().getAttributesList();
      for (KeyValue existingAttribute : existingAttributes) {
        for (KeyValue newAttribute : newAttributes) {
          if (existingAttribute.getKey().equals(newAttribute.getKey())) {
            Status status =
                Status.newBuilder()
                    .setCode(Code.ALREADY_EXISTS_VALUE)
                    .setMessage(
                        "Attribute being logged already exists. existing attribute Key : "
                            + newAttribute.getKey())
                    .build();
            throw StatusProto.toStatusRuntimeException(status);
          }
        }
      }

      List<AttributeEntity> newAttributeList =
          RdbmsUtils.convertAttributesFromAttributeEntityList(
              experimentRunEntityObj, ModelDBConstants.ATTRIBUTES, newAttributes);
      experimentRunEntityObj.setAttributeMapping(newAttributeList);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntityObj.setDate_updated(currentTimestamp);
      Transaction transaction = session.beginTransaction();
      session.saveOrUpdate(experimentRunEntityObj);
      transaction.commit();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        logAttributes(experimentRunId, newAttributes);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<KeyValue> getExperimentRunAttributes(
      String experimentRunId, List<String> attributeKeyList, Boolean getAll)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunObj == null) {
        String errorMessage = "Invalid ExperimentRun ID found";
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }

      if (getAll) {
        return experimentRunObj.getProtoObject().getAttributesList();
      } else {
        Query query = session.createQuery(GET_EXP_RUN_ATTRIBUTE_BY_KEYS_HQL);
        query.setParameterList("keys", attributeKeyList);
        query.setParameter(ModelDBConstants.EXPERIMENT_RUN_ID_STR, experimentRunId);
        query.setParameter(ModelDBConstants.FIELD_TYPE_STR, ModelDBConstants.ATTRIBUTES);
        List<AttributeEntity> attributeEntities = query.list();
        return RdbmsUtils.convertAttributeEntityListFromAttributes(attributeEntities);
      }
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperimentRunAttributes(experimentRunId, attributeKeyList, getAll);
      } else {
        throw ex;
      }
    }
  }

  /**
   * For getting experimentRuns that user has access to (either as the owner or a collaborator):
   * <br>
   *
   * <ol>
   *   <li>Iterate through all experimentRuns of the requested experimentRunIds
   *   <li>Get the project Id they belong to.
   *   <li>Check if project is accessible or not.
   * </ol>
   *
   * The list of accessible experimentRunIDs is built and returned by this method.
   *
   * @param requestedExperimentRunIds : experimentRun Ids
   * @return List<String> : list of accessible ExperimentRun Id
   */
  public List<String> getAccessibleExperimentRunIDs(
      List<String> requestedExperimentRunIds,
      ModelDBActionEnum.ModelDBServiceActions modelDBServiceActions) {
    List<String> accessibleExperimentRunIds = new ArrayList<>();

    Map<String, String> projectIdExperimentRunIdMap =
        getProjectIdsFromExperimentRunIds(requestedExperimentRunIds);
    if (projectIdExperimentRunIdMap.size() == 0) {
      Status status =
          Status.newBuilder()
              .setCode(Code.PERMISSION_DENIED_VALUE)
              .setMessage(
                  "Access is denied. ExperimentRun not found for given ids : "
                      + requestedExperimentRunIds)
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
    Set<String> projectIdSet = new HashSet<>(projectIdExperimentRunIdMap.values());

    List<String> allowedProjectIds;
    // Validate if current user has access to the entity or not
    if (projectIdSet.size() == 1) {
      roleService.isSelfAllowed(
          ModelDBServiceResourceTypes.PROJECT,
          modelDBServiceActions,
          new ArrayList<>(projectIdSet).get(0));
      accessibleExperimentRunIds.addAll(requestedExperimentRunIds);
    } else {
      allowedProjectIds =
          roleService.getSelfAllowedResources(
              ModelDBServiceResourceTypes.PROJECT, modelDBServiceActions);
      // Validate if current user has access to the entity or not
      allowedProjectIds.retainAll(projectIdSet);
      for (Map.Entry<String, String> entry : projectIdExperimentRunIdMap.entrySet()) {
        if (allowedProjectIds.contains(entry.getValue())) {
          accessibleExperimentRunIds.add(entry.getKey());
        }
      }
    }
    return accessibleExperimentRunIds;
  }

  @Override
  public ExperimentRunPaginationDTO findExperimentRuns(
      ProjectDAO projectDAO, UserInfo currentLoginUserInfo, FindExperimentRuns queryParameters)
      throws InvalidProtocolBufferException {

    LOGGER.trace("trying to open session");
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      LOGGER.trace("Starting to find experimentRuns");

      List<String> accessibleExperimentRunIds = new ArrayList<>();
      if (!queryParameters.getExperimentRunIdsList().isEmpty()) {
        accessibleExperimentRunIds.addAll(
            getAccessibleExperimentRunIDs(
                queryParameters.getExperimentRunIdsList(),
                ModelDBActionEnum.ModelDBServiceActions.READ));
        if (accessibleExperimentRunIds.isEmpty()) {
          String errorMessage =
              "Access is denied. User is unauthorized for given ExperimentRun IDs : "
                  + accessibleExperimentRunIds;
          ModelDBUtils.logAndThrowError(
              errorMessage,
              Code.PERMISSION_DENIED_VALUE,
              Any.pack(FindExperimentRuns.getDefaultInstance()));
        }
      }

      List<KeyValueQuery> predicates = new ArrayList<>(queryParameters.getPredicatesList());
      for (KeyValueQuery predicate : predicates) {
        if (predicate.getKey().equals(ModelDBConstants.ID)) {
          List<String> accessibleExperimentRunId =
              getAccessibleExperimentRunIDs(
                  Collections.singletonList(predicate.getValue().getStringValue()),
                  ModelDBActionEnum.ModelDBServiceActions.READ);
          accessibleExperimentRunIds.addAll(accessibleExperimentRunId);
          // Validate if current user has access to the entity or not where predicate key has an id
          RdbmsUtils.validatePredicates(
              ModelDBConstants.EXPERIMENT_RUNS, accessibleExperimentRunIds, predicate, roleService);
        }
      }

      CriteriaBuilder builder = session.getCriteriaBuilder();
      // Using FROM and JOIN
      CriteriaQuery<ExperimentRunEntity> criteriaQuery =
          builder.createQuery(ExperimentRunEntity.class);
      Root<ExperimentRunEntity> experimentRunRoot = criteriaQuery.from(ExperimentRunEntity.class);
      experimentRunRoot.alias("run");

      Root<ProjectEntity> projectEntityRoot = criteriaQuery.from(ProjectEntity.class);
      projectEntityRoot.alias("pr");

      Root<ExperimentEntity> experimentEntityRoot = criteriaQuery.from(ExperimentEntity.class);
      experimentEntityRoot.alias("ex");

      List<Predicate> finalPredicatesList = new ArrayList<>();
      finalPredicatesList.add(
          builder.equal(
              experimentRunRoot.get(ModelDBConstants.PROJECT_ID),
              projectEntityRoot.get(ModelDBConstants.ID)));
      finalPredicatesList.add(
          builder.equal(
              experimentRunRoot.get(ModelDBConstants.EXPERIMENT_ID),
              experimentEntityRoot.get(ModelDBConstants.ID)));

      List<String> projectIds = new ArrayList<>();
      if (!queryParameters.getProjectId().isEmpty()) {
        projectIds.add(queryParameters.getProjectId());
      } else if (accessibleExperimentRunIds.isEmpty()
          && queryParameters.getExperimentId().isEmpty()) {
        List<String> workspaceProjectIDs =
            projectDAO.getWorkspaceProjectIDs(
                queryParameters.getWorkspaceName(), currentLoginUserInfo);
        if (workspaceProjectIDs == null || workspaceProjectIDs.isEmpty()) {
          LOGGER.info(
              "accessible project for the experimentRuns not found for given workspace : {}",
              queryParameters.getWorkspaceName());
          ExperimentRunPaginationDTO experimentRunPaginationDTO = new ExperimentRunPaginationDTO();
          experimentRunPaginationDTO.setExperimentRuns(Collections.emptyList());
          experimentRunPaginationDTO.setTotalRecords(0L);
          return experimentRunPaginationDTO;
        }
        projectIds.addAll(workspaceProjectIDs);
      }

      if (accessibleExperimentRunIds.isEmpty()
          && projectIds.isEmpty()
          && queryParameters.getExperimentId().isEmpty()) {
        String errorMessage =
            "Access is denied. Accessible projects not found for given ExperimentRun IDs : "
                + accessibleExperimentRunIds;
        ModelDBUtils.logAndThrowError(
            errorMessage,
            Code.PERMISSION_DENIED_VALUE,
            Any.pack(FindExperimentRuns.getDefaultInstance()));
      }

      if (!projectIds.isEmpty()) {
        Expression<String> projectExpression = experimentRunRoot.get(ModelDBConstants.PROJECT_ID);
        Predicate projectsPredicate = projectExpression.in(projectIds);
        finalPredicatesList.add(projectsPredicate);
      }

      if (!queryParameters.getExperimentId().isEmpty()) {
        Expression<String> exp = experimentRunRoot.get(ModelDBConstants.EXPERIMENT_ID);
        Predicate predicate2 = builder.equal(exp, queryParameters.getExperimentId());
        finalPredicatesList.add(predicate2);
      }

      if (!queryParameters.getExperimentRunIdsList().isEmpty()) {
        Expression<String> exp = experimentRunRoot.get(ModelDBConstants.ID);
        Predicate predicate2 = exp.in(queryParameters.getExperimentRunIdsList());
        finalPredicatesList.add(predicate2);
      }

      LOGGER.trace("Added entity predicates");
      String entityName = "experimentRunEntity";
      try {
        List<Predicate> queryPredicatesList =
            RdbmsUtils.getQueryPredicatesFromPredicateList(
                entityName, predicates, builder, criteriaQuery, experimentRunRoot, authService);
        if (!queryPredicatesList.isEmpty()) {
          finalPredicatesList.addAll(queryPredicatesList);
        }
      } catch (ModelDBException ex) {
        if (ex.getCode().ordinal() == Code.FAILED_PRECONDITION_VALUE
            && ModelDBConstants.INTERNAL_MSG_USERS_NOT_FOUND.equals(ex.getMessage())) {
          LOGGER.info(ex.getMessage());
          ExperimentRunPaginationDTO experimentRunPaginationDTO = new ExperimentRunPaginationDTO();
          experimentRunPaginationDTO.setExperimentRuns(Collections.emptyList());
          experimentRunPaginationDTO.setTotalRecords(0L);
          return experimentRunPaginationDTO;
        }
      }

      finalPredicatesList.add(
          builder.equal(experimentRunRoot.get(ModelDBConstants.DELETED), false));
      finalPredicatesList.add(
          builder.equal(projectEntityRoot.get(ModelDBConstants.DELETED), false));
      finalPredicatesList.add(
          builder.equal(experimentEntityRoot.get(ModelDBConstants.DELETED), false));

      Order[] orderBy =
          RdbmsUtils.getOrderArrBasedOnSortKey(
              queryParameters.getSortKey(),
              queryParameters.getAscending(),
              builder,
              experimentRunRoot,
              entityName);

      Predicate[] predicateArr = new Predicate[finalPredicatesList.size()];
      for (int index = 0; index < finalPredicatesList.size(); index++) {
        predicateArr[index] = finalPredicatesList.get(index);
      }

      Predicate predicateWhereCause = builder.and(predicateArr);
      criteriaQuery.select(experimentRunRoot);
      criteriaQuery.where(predicateWhereCause);
      criteriaQuery.orderBy(orderBy);

      LOGGER.trace("Creating criteria query");
      Query query = session.createQuery(criteriaQuery);
      LOGGER.debug("Final experimentRuns final query : {}", query.getQueryString());
      if (queryParameters.getPageNumber() != 0 && queryParameters.getPageLimit() != 0) {
        // Calculate number of documents to skip
        int skips = queryParameters.getPageLimit() * (queryParameters.getPageNumber() - 1);
        query.setFirstResult(skips);
        query.setMaxResults(queryParameters.getPageLimit());
      }

      LOGGER.trace("Final query generated");
      List<ExperimentRunEntity> experimentRunEntities = query.list();
      LOGGER.debug("Final experimentRuns list size : {}", experimentRunEntities.size());
      List<ExperimentRun> experimentRuns = new ArrayList<>();
      if (!experimentRunEntities.isEmpty()) {

        LOGGER.trace("Converting from Hibernate to proto");
        List<ExperimentRun> experimentRunList =
            RdbmsUtils.convertExperimentRunsFromExperimentRunEntityList(experimentRunEntities);
        LOGGER.trace("experimentRunList {}", experimentRunList);
        LOGGER.trace("Converted from Hibernate to proto");

        List<String> selfAllowedRepositoryIds = new ArrayList<>();
        if (app.isPopulateConnectionsBasedOnPrivileges()) {
          selfAllowedRepositoryIds =
              roleService.getSelfAllowedResources(
                  ModelDBServiceResourceTypes.REPOSITORY,
                  ModelDBActionEnum.ModelDBServiceActions.READ);
        }

        List<String> expRunIds =
            experimentRunEntities.stream()
                .map(ExperimentRunEntity::getId)
                .collect(Collectors.toList());
        Map<String, List<KeyValue>> expRunHyperparameterConfigBlobMap =
            getExperimentRunHyperparameterConfigBlobMap(
                session, expRunIds, selfAllowedRepositoryIds);

        // Map<experimentRunID, Map<LocationString, CodeVersion>> : Map from experimentRunID to Map
        // of
        // LocationString to CodeBlob
        Map<String, Map<String, CodeVersion>> expRunCodeVersionMap =
            getExperimentRunCodeVersionMap(session, expRunIds, selfAllowedRepositoryIds);

        Set<String> experimentRunIdsSet = new HashSet<>();
        Set<String> accessibleDatasetVersionIdsSet = new HashSet<>();
        Set<String> notAccessibleDatasetVersionIdsSet = new HashSet<>();
        Set<Long> accessibleRepoIdsSet = new HashSet<>();
        Set<Long> notAccessibleRepoIdsSet = new HashSet<>();
        for (ExperimentRun experimentRun : experimentRunList) {
          if (!expRunHyperparameterConfigBlobMap.isEmpty()
              && expRunHyperparameterConfigBlobMap.containsKey(experimentRun.getId())) {
            experimentRun =
                experimentRun
                    .toBuilder()
                    .addAllHyperparameters(
                        expRunHyperparameterConfigBlobMap.get(experimentRun.getId()))
                    .build();
          }
          if (!expRunCodeVersionMap.isEmpty()
              && expRunCodeVersionMap.containsKey(experimentRun.getId())) {
            experimentRun =
                experimentRun
                    .toBuilder()
                    .putAllCodeVersionFromBlob(expRunCodeVersionMap.get(experimentRun.getId()))
                    .build();
          }
          if (!experimentRunIdsSet.contains(experimentRun.getId())) {
            experimentRunIdsSet.add(experimentRun.getId());
            if (queryParameters.getIdsOnly()) {
              experimentRun = ExperimentRun.newBuilder().setId(experimentRun.getId()).build();
              experimentRuns.add(experimentRun);
            } else {
              if (app.isPopulateConnectionsBasedOnPrivileges()) {
                if (experimentRun.getDatasetsCount() > 0) {
                  experimentRun =
                      filteredDatasetsBasedOnPrivileges(
                          accessibleDatasetVersionIdsSet,
                          notAccessibleDatasetVersionIdsSet,
                          experimentRun);
                }
                if (experimentRun.getVersionedInputs() != null
                    && experimentRun.getVersionedInputs().getRepositoryId() != 0) {
                  experimentRun =
                      checkVersionInputBasedOnPrivileges(
                          experimentRun, accessibleRepoIdsSet, notAccessibleRepoIdsSet);
                }
              }
              experimentRuns.add(experimentRun);
            }
          }
        }
      }

      long totalRecords = RdbmsUtils.count(session, experimentRunRoot, criteriaQuery);
      LOGGER.debug("ExperimentRuns Total record count : {}", totalRecords);

      ExperimentRunPaginationDTO experimentRunPaginationDTO = new ExperimentRunPaginationDTO();
      experimentRunPaginationDTO.setExperimentRuns(experimentRuns);
      experimentRunPaginationDTO.setTotalRecords(totalRecords);
      return experimentRunPaginationDTO;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return findExperimentRuns(projectDAO, currentLoginUserInfo, queryParameters);
      } else {
        throw ex;
      }
    }
  }

  private ExperimentRun filteredDatasetsBasedOnPrivileges(
      Set<String> accessibleDatasetVersionIdsSet,
      Set<String> notAccessibleDatasetVersionIdsSet,
      ExperimentRun experimentRun) {
    List<Artifact> accessibleDatasetVersions = new ArrayList<>();
    for (Artifact dataset : experimentRun.getDatasetsList()) {
      if (!dataset.getLinkedArtifactId().isEmpty()) {
        if (accessibleDatasetVersionIdsSet.contains(dataset.getLinkedArtifactId())) {
          accessibleDatasetVersions.add(dataset);
        } else if (notAccessibleDatasetVersionIdsSet.contains(dataset.getLinkedArtifactId())) {
          notAccessibleDatasetVersionIdsSet.add(dataset.getLinkedArtifactId());
        } else {
          try {
            commitDAO.getDatasetVersionById(
                repositoryDAO, blobDAO, metadataDAO, dataset.getLinkedArtifactId());
            accessibleDatasetVersionIdsSet.add(dataset.getLinkedArtifactId());
            accessibleDatasetVersions.add(dataset);
          } catch (Exception ex) {
            LOGGER.debug(ex.getMessage());
            notAccessibleDatasetVersionIdsSet.add(dataset.getLinkedArtifactId());
          }
        }
      } else {
        accessibleDatasetVersions.add(dataset);
      }
    }
    experimentRun =
        experimentRun.toBuilder().clearDatasets().addAllDatasets(accessibleDatasetVersions).build();
    return experimentRun;
  }

  private Map<String, List<KeyValue>> getExperimentRunHyperparameterConfigBlobMap(
      Session session, List<String> expRunIds, List<String> selfAllowedRepositoryIds) {

    String queryBuilder =
        "Select vme.experimentRunEntity.id, cb From ConfigBlobEntity cb INNER JOIN VersioningModeldbEntityMapping vme ON vme.blob_hash = cb.blob_hash WHERE cb.hyperparameter_type = :hyperparameterType AND vme.experimentRunEntity.id IN (:expRunIds) ";

    if (app.isPopulateConnectionsBasedOnPrivileges()) {
      if (selfAllowedRepositoryIds == null || selfAllowedRepositoryIds.isEmpty()) {
        return new HashMap<>();
      } else {
        queryBuilder = queryBuilder + " AND vme.repository_id IN (:repoIds)";
      }
    }

    Query query = session.createQuery(queryBuilder);
    query.setParameter("hyperparameterType", HYPERPARAMETER);
    query.setParameterList("expRunIds", expRunIds);
    if (app.isPopulateConnectionsBasedOnPrivileges()) {
      query.setParameterList(
          "repoIds",
          selfAllowedRepositoryIds.stream().map(Long::parseLong).collect(Collectors.toList()));
    }

    LOGGER.debug(
        "Final experimentRuns hyperparameter config blob final query : {}", query.getQueryString());
    List<Object[]> configBlobEntities = query.list();
    LOGGER.debug(
        "Final experimentRuns hyperparameter config list size : {}", configBlobEntities.size());
    Map<String, List<KeyValue>> hyperparametersMap = new LinkedHashMap<>();
    if (!configBlobEntities.isEmpty()) {
      configBlobEntities.forEach(
          objects -> {
            String expRunId = (String) objects[0];
            ConfigBlobEntity configBlobEntity = (ConfigBlobEntity) objects[1];
            if (configBlobEntity.getHyperparameter_type() == HYPERPARAMETER) {
              HyperparameterElementConfigBlobEntity hyperElementConfigBlobEntity =
                  configBlobEntity.getHyperparameterElementConfigBlobEntity();
              HyperparameterValuesConfigBlob valuesConfigBlob =
                  hyperElementConfigBlobEntity.toProto();
              Value.Builder valueBuilder = Value.newBuilder();
              switch (valuesConfigBlob.getValueCase()) {
                case INT_VALUE:
                  valueBuilder.setNumberValue(valuesConfigBlob.getIntValue());
                  break;
                case FLOAT_VALUE:
                  valueBuilder.setNumberValue(valuesConfigBlob.getFloatValue());
                  break;
                case STRING_VALUE:
                  valueBuilder.setStringValue(valuesConfigBlob.getStringValue());
                  break;
              }
              KeyValue hyperparameter =
                  KeyValue.newBuilder()
                      .setKey(hyperElementConfigBlobEntity.getName())
                      .setValue(valueBuilder.build())
                      .build();
              List<KeyValue> hyperparameterList = hyperparametersMap.get(expRunId);
              if (hyperparameterList == null) {
                hyperparameterList = new ArrayList<>();
              }
              hyperparameterList.add(hyperparameter);
              hyperparametersMap.put(expRunId, hyperparameterList);
            }
          });
    }
    return hyperparametersMap;
  }

  /**
   * @param session : session
   * @param expRunIds : ExperimentRun ids
   * @return {@link Map<String, Map<String, CodeBlob>>} : Map from experimentRunID to Map of
   *     LocationString to CodeVersion
   * @throws InvalidProtocolBufferException invalidProtocolBufferException
   */
  private Map<String, Map<String, CodeVersion>> getExperimentRunCodeVersionMap(
      Session session, List<String> expRunIds, List<String> selfAllowedRepositoryIds)
      throws InvalidProtocolBufferException {

    String queryBuilder =
        "SELECT vme.experimentRunEntity.id, vme.versioning_location, gcb, ncb, pdcb "
            + " From VersioningModeldbEntityMapping vme LEFT JOIN GitCodeBlobEntity gcb ON vme.blob_hash = gcb.blob_hash "
            + " LEFT JOIN NotebookCodeBlobEntity ncb ON vme.blob_hash = ncb.blob_hash "
            + " LEFT JOIN PathDatasetComponentBlobEntity pdcb ON ncb.path_dataset_blob_hash = pdcb.id.path_dataset_blob_id "
            + " WHERE vme.versioning_blob_type = :versioningBlobType AND vme.experimentRunEntity.id IN (:expRunIds) ";

    if (app.isPopulateConnectionsBasedOnPrivileges()) {
      if (selfAllowedRepositoryIds == null || selfAllowedRepositoryIds.isEmpty()) {
        return new HashMap<>();
      } else {
        queryBuilder = queryBuilder + " AND vme.repository_id IN (:repoIds)";
      }
    }

    Query query = session.createQuery(queryBuilder);
    query.setParameter("versioningBlobType", Blob.ContentCase.CODE.getNumber());
    query.setParameterList("expRunIds", expRunIds);
    if (app.isPopulateConnectionsBasedOnPrivileges()) {
      query.setParameterList(
          "repoIds",
          selfAllowedRepositoryIds.stream().map(Long::parseLong).collect(Collectors.toList()));
    }

    LOGGER.debug("Final experimentRuns code config blob final query : {}", query.getQueryString());
    List<Object[]> codeBlobEntities = query.list();
    LOGGER.debug("Final experimentRuns code config list size : {}", codeBlobEntities.size());

    // Map<experimentRunID, Map<LocationString, CodeVersion>> : Map from experimentRunID to Map of
    // LocationString to CodeVersion
    Map<String, Map<String, CodeVersion>> expRunCodeBlobMap = new LinkedHashMap<>();
    if (!codeBlobEntities.isEmpty()) {
      for (Object[] objects : codeBlobEntities) {
        String expRunId = (String) objects[0];
        String versioningLocation = (String) objects[1];
        GitCodeBlobEntity gitBlobEntity = (GitCodeBlobEntity) objects[2];
        NotebookCodeBlobEntity notebookCodeBlobEntity = (NotebookCodeBlobEntity) objects[3];
        PathDatasetComponentBlobEntity pathDatasetComponentBlobEntity =
            (PathDatasetComponentBlobEntity) objects[4];

        CodeVersion.Builder codeVersionBuilder = CodeVersion.newBuilder();
        LOGGER.debug("notebookCodeBlobEntity {}", notebookCodeBlobEntity);
        LOGGER.debug("pathDatasetComponentBlobEntity {}", pathDatasetComponentBlobEntity);
        LOGGER.debug("gitBlobEntity {}", gitBlobEntity);
        if (notebookCodeBlobEntity != null) {
          if (pathDatasetComponentBlobEntity != null) {
            convertGitBlobToGitSnapshot(
                codeVersionBuilder,
                notebookCodeBlobEntity.getGitCodeBlobEntity().toProto(),
                pathDatasetComponentBlobEntity.toProto());
          } else {
            convertGitBlobToGitSnapshot(
                codeVersionBuilder, notebookCodeBlobEntity.getGitCodeBlobEntity().toProto(), null);
          }
        } else if (gitBlobEntity != null) {
          convertGitBlobToGitSnapshot(codeVersionBuilder, gitBlobEntity.toProto(), null);
        }
        Map<String, CodeVersion> codeBlobMap = expRunCodeBlobMap.get(expRunId);
        if (codeBlobMap == null) {
          codeBlobMap = new LinkedHashMap<>();
        }
        Location.Builder locationBuilder = Location.newBuilder();
        ModelDBUtils.getProtoObjectFromString(versioningLocation, locationBuilder);
        codeBlobMap.put(
            ModelDBUtils.getLocationWithSlashOperator(locationBuilder.getLocationList()),
            codeVersionBuilder.build());
        expRunCodeBlobMap.put(expRunId, codeBlobMap);
      }
    }
    return expRunCodeBlobMap;
  }

  private void convertGitBlobToGitSnapshot(
      CodeVersion.Builder codeVersionBuilder,
      GitCodeBlob codeBlob,
      PathDatasetComponentBlob pathComponentBlob) {
    GitSnapshot.Builder gitSnapShot = GitSnapshot.newBuilder();
    if (codeBlob != null) {
      gitSnapShot
          .setRepo(codeBlob.getRepo())
          .setHash(codeBlob.getHash())
          .setIsDirtyValue(codeBlob.getIsDirty() ? 1 : 2)
          .build();
    }
    if (pathComponentBlob != null) {
      gitSnapShot.addFilepaths(pathComponentBlob.getPath());
    }
    codeVersionBuilder.setGitSnapshot(gitSnapShot);
  }

  @Override
  public ExperimentRunPaginationDTO sortExperimentRuns(
      ProjectDAO projectDAO, SortExperimentRuns queryParameters)
      throws InvalidProtocolBufferException {
    FindExperimentRuns findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .addAllExperimentRunIds(queryParameters.getExperimentRunIdsList())
            .setSortKey(queryParameters.getSortKey())
            .setAscending(queryParameters.getAscending())
            .setIdsOnly(queryParameters.getIdsOnly())
            .build();
    UserInfo currentLoginUserInfo = authService.getCurrentLoginUserInfo();
    return findExperimentRuns(projectDAO, currentLoginUserInfo, findExperimentRuns);
  }

  @Override
  public List<ExperimentRun> getTopExperimentRuns(
      ProjectDAO projectDAO, TopExperimentRunsSelector queryParameters)
      throws InvalidProtocolBufferException {
    FindExperimentRuns findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .setProjectId(queryParameters.getProjectId())
            .setExperimentId(queryParameters.getExperimentId())
            .addAllExperimentRunIds(queryParameters.getExperimentRunIdsList())
            .setSortKey(queryParameters.getSortKey())
            .setAscending(queryParameters.getAscending())
            .setIdsOnly(queryParameters.getIdsOnly())
            .setPageNumber(1)
            .setPageLimit(queryParameters.getTopK())
            .build();
    UserInfo currentLoginUserInfo = authService.getCurrentLoginUserInfo();
    return findExperimentRuns(projectDAO, currentLoginUserInfo, findExperimentRuns)
        .getExperimentRuns();
  }

  @Override
  public List<String> getExperimentRunTags(String experimentRunId)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      LOGGER.debug("Got ExperimentRun Tags");
      return experimentRunObj.getProtoObject().getTagsList();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperimentRunTags(experimentRunId);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public void addExperimentRunAttributes(String experimentRunId, List<KeyValue> attributesList)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      List<AttributeEntity> newAttributeList =
          RdbmsUtils.convertAttributesFromAttributeEntityList(
              experimentRunEntityObj, ModelDBConstants.ATTRIBUTES, attributesList);
      experimentRunEntityObj.setAttributeMapping(newAttributeList);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntityObj.setDate_updated(currentTimestamp);
      Transaction transaction = session.beginTransaction();
      session.saveOrUpdate(experimentRunEntityObj);
      transaction.commit();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        addExperimentRunAttributes(experimentRunId, attributesList);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public void deleteExperimentRunAttributes(
      String experimentRunId, List<String> attributeKeyList, Boolean deleteAll) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      if (deleteAll) {
        Query query = session.createQuery(DELETE_ALL_EXP_RUN_ATTRIBUTES_HQL);
        query.setParameter(ModelDBConstants.EXPERIMENT_RUN_ID_STR, experimentRunId);
        query.setParameter(ModelDBConstants.FIELD_TYPE_STR, ModelDBConstants.ATTRIBUTES);
        query.executeUpdate();
      } else {
        Query query = session.createQuery(DELETE_SELECTED_EXP_RUN_ATTRIBUTES_HQL);
        query.setParameter("keys", attributeKeyList);
        query.setParameter(ModelDBConstants.EXPERIMENT_RUN_ID_STR, experimentRunId);
        query.setParameter(ModelDBConstants.FIELD_TYPE_STR, ModelDBConstants.ATTRIBUTES);
        query.executeUpdate();
      }
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunObj.setDate_updated(currentTimestamp);
      session.update(experimentRunObj);
      transaction.commit();
      LOGGER.debug("ExperimentRun Attributes deleted successfully");
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        deleteExperimentRunAttributes(experimentRunId, attributeKeyList, deleteAll);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public void logJobId(String experimentRunId, String jobId) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      experimentRunEntityObj.setJob_id(jobId);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntityObj.setDate_updated(currentTimestamp);
      Transaction transaction = session.beginTransaction();
      session.saveOrUpdate(experimentRunEntityObj);
      transaction.commit();
      LOGGER.debug("ExperimentRun JobID added successfully");
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        logJobId(experimentRunId, jobId);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public String getJobId(String experimentRunId) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      LOGGER.debug("Got ExperimentRun JobID");
      return experimentRunEntityObj.getJob_id();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getJobId(experimentRunId);
      } else {
        throw ex;
      }
    }
  }

  private ExperimentRun copyExperimentRunAndUpdateDetails(
      ExperimentRun srcExperimentRun,
      Experiment newExperiment,
      Project newProject,
      UserInfo newOwner) {
    ExperimentRun.Builder experimentRunBuilder =
        ExperimentRun.newBuilder(srcExperimentRun).setId(UUID.randomUUID().toString());

    if (newOwner != null) {
      experimentRunBuilder.setOwner(authService.getVertaIdFromUserInfo(newOwner));
    }
    if (newProject != null) {
      experimentRunBuilder.setProjectId(newProject.getId());
    }
    if (newExperiment != null) {
      experimentRunBuilder.setExperimentId(newExperiment.getId());
    }
    return experimentRunBuilder.build();
  }

  @Override
  public List<ExperimentRun> getExperimentRuns(List<KeyValue> keyValues)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      StringBuilder stringQueryBuilder = new StringBuilder("From ExperimentRunEntity er where ");
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
        stringQueryBuilder.append(" er." + key + " = :" + key);
        if (index < keyValues.size() - 1) {
          stringQueryBuilder.append(" AND ");
        }
      }
      Query query =
          session.createQuery(
              stringQueryBuilder.toString() + " AND er." + ModelDBConstants.DELETED + " = false ");
      for (Map.Entry<String, Object> paramEntry : paramMap.entrySet()) {
        query.setParameter(paramEntry.getKey(), paramEntry.getValue());
      }
      List<ExperimentRunEntity> experimentRunObjList = query.list();
      return RdbmsUtils.convertExperimentRunsFromExperimentRunEntityList(experimentRunObjList);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperimentRuns(keyValues);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public String getProjectIdByExperimentRunId(String experimentRunId) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunObj != null) {
        LOGGER.debug("Got ProjectId by ExperimentRunId ");
        return experimentRunObj.getProject_id();
      } else {
        String errorMessage = "ExperimentRun not found for given ID : " + experimentRunId;
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getProjectIdByExperimentRunId(experimentRunId);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public Map<String, String> getProjectIdsFromExperimentRunIds(List<String> experimentRunIds) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query query = session.createQuery(GET_EXP_RUN_BY_IDS_HQL);
      query.setParameterList("ids", experimentRunIds);

      @SuppressWarnings("unchecked")
      List<ExperimentRunEntity> experimentRunEntities = query.list();
      LOGGER.debug("Got ExperimentRun by Ids. Size : {}", experimentRunEntities.size());
      Map<String, String> experimentRunIdToProjectIdMap = new HashMap<>();
      for (ExperimentRunEntity experimentRunEntity : experimentRunEntities) {
        experimentRunIdToProjectIdMap.put(
            experimentRunEntity.getId(), experimentRunEntity.getProject_id());
      }
      return experimentRunIdToProjectIdMap;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getProjectIdsFromExperimentRunIds(experimentRunIds);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<?> getSelectedFieldsByExperimentRunIds(
      List<String> experimentRunIds, List<String> selectedFields)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      String alias = "exr";
      StringBuilder queryBuilder = new StringBuilder("Select ");
      if (selectedFields != null && !selectedFields.isEmpty()) {
        int index = 1;
        for (String selectedField : selectedFields) {
          queryBuilder.append(alias).append(".");
          queryBuilder.append(selectedField);
          if (index < selectedFields.size()) {
            queryBuilder.append(", ");
            index++;
          }
        }
        queryBuilder.append(" ");
      }

      queryBuilder.append(GET_EXP_RUN_BY_IDS_HQL);
      Query experimentRunQuery = session.createQuery(queryBuilder.toString());
      experimentRunQuery.setParameterList("ids", experimentRunIds);
      return experimentRunQuery.list();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getSelectedFieldsByExperimentRunIds(experimentRunIds, selectedFields);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<String> getExperimentRunIdsByProjectIds(List<String> projectIds)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query experimentRunQuery = session.createQuery(GET_EXPERIMENT_RUN_BY_PROJECT_ID_HQL);
      experimentRunQuery.setParameterList("projectIds", projectIds);
      List<ExperimentRunEntity> experimentRunEntities = experimentRunQuery.list();

      List<String> experimentRunIds = new ArrayList<>();
      for (ExperimentRunEntity experimentRunEntity : experimentRunEntities) {
        experimentRunIds.add(experimentRunEntity.getId());
      }
      return experimentRunIds;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperimentRunIdsByProjectIds(projectIds);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<String> getExperimentRunIdsByExperimentIds(List<String> experimentIds)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query experimentRunQuery = session.createQuery(GET_EXPERIMENT_RUN_BY_EXPERIMENT_ID_HQL);
      experimentRunQuery.setParameterList("experimentIds", experimentIds);
      List<ExperimentRunEntity> experimentRunEntities = experimentRunQuery.list();

      List<String> experimentRunIds = new ArrayList<>();
      for (ExperimentRunEntity experimentRunEntity : experimentRunEntities) {
        experimentRunIds.add(experimentRunEntity.getId());
      }
      return experimentRunIds;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getExperimentRunIdsByExperimentIds(experimentIds);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public void logVersionedInput(LogVersionedInput request)
      throws InvalidProtocolBufferException, ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      VersioningEntry versioningEntry = request.getVersionedInputs();
      Map<String, Map.Entry<BlobExpanded, String>> locationBlobWithHashMap =
          validateVersioningEntity(session, versioningEntry);
      ExperimentRunEntity runEntity = session.get(ExperimentRunEntity.class, request.getId());
      List<VersioningModeldbEntityMapping> versioningModeldbEntityMappings =
          RdbmsUtils.getVersioningMappingFromVersioningInput(
              session, versioningEntry, locationBlobWithHashMap, runEntity);

      List<VersioningModeldbEntityMapping> existingMappings = runEntity.getVersioned_inputs();
      if (existingMappings.isEmpty()) {
        existingMappings.addAll(versioningModeldbEntityMappings);
      } else {
        if (!versioningModeldbEntityMappings.isEmpty()) {
          VersioningModeldbEntityMapping existingFirstEntityMapping = existingMappings.get(0);
          VersioningModeldbEntityMapping versioningModeldbFirstEntityMapping =
              versioningModeldbEntityMappings.get(0);
          if (!existingFirstEntityMapping
                  .getRepository_id()
                  .equals(versioningModeldbFirstEntityMapping.getRepository_id())
              || !existingFirstEntityMapping
                  .getCommit()
                  .equals(versioningModeldbFirstEntityMapping.getCommit())) {
            if (!OVERWRITE_VERSION_MAP) {
              throw new ModelDBException(
                  ModelDBConstants.DIFFERENT_REPOSITORY_OR_COMMIT_MESSAGE,
                  io.grpc.Status.Code.ALREADY_EXISTS);
            }
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaDelete<VersioningModeldbEntityMapping> delete =
                cb.createCriteriaDelete(VersioningModeldbEntityMapping.class);
            Root<VersioningModeldbEntityMapping> e =
                delete.from(VersioningModeldbEntityMapping.class);
            delete.where(cb.in(e.get("experimentRunEntity")).value(runEntity));
            Transaction transaction = session.beginTransaction();
            session.createQuery(delete).executeUpdate();
            transaction.commit();
            existingMappings.addAll(versioningModeldbEntityMappings);
          } else {
            List<VersioningModeldbEntityMapping> finalVersionList = new ArrayList<>();
            for (VersioningModeldbEntityMapping versioningModeldbEntityMapping :
                versioningModeldbEntityMappings) {
              boolean addNew = true;
              for (VersioningModeldbEntityMapping existsVerMapping : existingMappings) {
                if (versioningModeldbEntityMapping.equals(existsVerMapping)) {
                  addNew = false;
                  break;
                }
              }
              if (addNew) {
                finalVersionList.add(versioningModeldbEntityMapping);
              }
            }

            if (finalVersionList.isEmpty()) {
              return;
            }
            existingMappings.addAll(finalVersionList);
          }
        } else {
          return;
        }
      }

      Set<HyperparameterElementMappingEntity> hyrParamMappings =
          prepareHyperparameterElemMappings(runEntity, versioningModeldbEntityMappings);

      if (!hyrParamMappings.isEmpty()) {
        runEntity.setHyperparameter_element_mappings(new ArrayList<>(hyrParamMappings));
      }

      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      runEntity.setDate_updated(currentTimestamp);
      Transaction transaction = session.beginTransaction();
      session.saveOrUpdate(runEntity);
      transaction.commit();
      LOGGER.debug("ExperimentRun versioning added successfully");
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        logVersionedInput(request);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public void deleteLogVersionedInputs(Session session, Long repoId, String commitHash) {
    StringBuilder fetchAllExpRunLogVersionedInputsHqlBuilder =
        new StringBuilder(
            "DELETE FROM VersioningModeldbEntityMapping vm WHERE vm.repository_id = :repoId ");
    fetchAllExpRunLogVersionedInputsHqlBuilder
        .append(" AND vm.entity_type = '")
        .append(ExperimentRunEntity.class.getSimpleName())
        .append("' ");
    if (commitHash != null && !commitHash.isEmpty()) {
      fetchAllExpRunLogVersionedInputsHqlBuilder.append(" AND vm.commit = :commitHash");
    }
    Query query = session.createQuery(fetchAllExpRunLogVersionedInputsHqlBuilder.toString());
    query.setParameter("repoId", repoId);
    if (commitHash != null && !commitHash.isEmpty()) {
      query.setParameter("commitHash", commitHash);
    }
    query.executeUpdate();
    LOGGER.debug("ExperimentRun versioning deleted successfully");
  }

  @Override
  public void deleteLogVersionedInputs(Session session, List<Long> repoIds) {
    StringBuilder fetchAllExpRunLogVersionedInputsHqlBuilder =
        new StringBuilder(
            "DELETE FROM VersioningModeldbEntityMapping vm WHERE vm.repository_id IN (:repoIds) ");
    fetchAllExpRunLogVersionedInputsHqlBuilder
        .append(" AND vm.entity_type = '")
        .append(ExperimentRunEntity.class.getSimpleName())
        .append("'");
    Query query = session.createQuery(fetchAllExpRunLogVersionedInputsHqlBuilder.toString());
    query.setParameter("repoIds", repoIds);
    query.executeUpdate();
    LOGGER.debug("ExperimentRun versioning deleted successfully");
  }

  @Override
  public GetVersionedInput.Response getVersionedInputs(GetVersionedInput request)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, request.getId());
      if (experimentRunObj != null) {
        ExperimentRun experimentRun = experimentRunObj.getProtoObject();
        if (experimentRun.getVersionedInputs() != null
            && experimentRun.getVersionedInputs().getRepositoryId() != 0
            && app.isPopulateConnectionsBasedOnPrivileges()) {
          experimentRun =
              checkVersionInputBasedOnPrivileges(experimentRun, new HashSet<>(), new HashSet<>());
        }
        LOGGER.debug("ExperimentRun versioning fetch successfully");
        return GetVersionedInput.Response.newBuilder()
            .setVersionedInputs(experimentRun.getVersionedInputs())
            .build();
      } else {
        String errorMessage = "ExperimentRun not found for given ID : " + request.getId();
        LOGGER.info(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getVersionedInputs(request);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public ListCommitExperimentRunsRequest.Response listCommitExperimentRuns(
      ProjectDAO projectDAO,
      ListCommitExperimentRunsRequest request,
      RepositoryFunction repositoryFunction,
      CommitFunction commitFunction)
      throws ModelDBException, InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repositoryEntity = repositoryFunction.apply(session);
      CommitEntity commitEntity = commitFunction.apply(session, session1 -> repositoryEntity);

      KeyValueQuery repositoryIdPredicate =
          KeyValueQuery.newBuilder()
              .setKey(ModelDBConstants.VERSIONED_INPUTS + "." + ModelDBConstants.REPOSITORY_ID)
              .setValue(Value.newBuilder().setNumberValue(repositoryEntity.getId()).build())
              .setOperator(OperatorEnum.Operator.EQ)
              .setValueType(ValueTypeEnum.ValueType.NUMBER)
              .build();
      KeyValueQuery commitHashPredicate =
          KeyValueQuery.newBuilder()
              .setKey(ModelDBConstants.VERSIONED_INPUTS + "." + ModelDBConstants.COMMIT)
              .setValue(Value.newBuilder().setStringValue(commitEntity.getCommit_hash()).build())
              .setOperator(OperatorEnum.Operator.EQ)
              .setValueType(ValueTypeEnum.ValueType.STRING)
              .build();

      FindExperimentRuns.Builder findExperimentRuns =
          FindExperimentRuns.newBuilder()
              .setPageNumber(request.getPagination().getPageNumber())
              .setPageLimit(request.getPagination().getPageLimit())
              .setAscending(true)
              .setSortKey(ModelDBConstants.DATE_UPDATED)
              .addPredicates(repositoryIdPredicate)
              .addPredicates(commitHashPredicate);
      UserInfo currentLoginUserInfo = authService.getCurrentLoginUserInfo();
      if (request.getRepositoryId().hasNamedId()) {
        findExperimentRuns.setWorkspaceName(
            request.getRepositoryId().getNamedId().getWorkspaceName());
      } else {
        WorkspaceDTO workspaceDTO =
            roleService.getWorkspaceDTOByWorkspaceId(
                currentLoginUserInfo,
                repositoryEntity.getWorkspace_id(),
                repositoryEntity.getWorkspace_type());
        if (workspaceDTO != null && workspaceDTO.getWorkspaceName() != null) {
          findExperimentRuns.setWorkspaceName(workspaceDTO.getWorkspaceName());
        }
      }
      ExperimentRunPaginationDTO experimentRunPaginationDTO =
          findExperimentRuns(projectDAO, currentLoginUserInfo, findExperimentRuns.build());
      return ListCommitExperimentRunsRequest.Response.newBuilder()
          .addAllRuns(experimentRunPaginationDTO.getExperimentRuns())
          .setTotalRecords(experimentRunPaginationDTO.getTotalRecords())
          .build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return listCommitExperimentRuns(projectDAO, request, repositoryFunction, commitFunction);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public ListBlobExperimentRunsRequest.Response listBlobExperimentRuns(
      ProjectDAO projectDAO,
      ListBlobExperimentRunsRequest request,
      RepositoryFunction repositoryFunction,
      CommitFunction commitFunction)
      throws ModelDBException, InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repositoryEntity = repositoryFunction.apply(session);
      CommitEntity commitEntity = commitFunction.apply(session, session1 -> repositoryEntity);

      KeyValueQuery repositoryIdPredicate =
          KeyValueQuery.newBuilder()
              .setKey(ModelDBConstants.VERSIONED_INPUTS + "." + ModelDBConstants.REPOSITORY_ID)
              .setValue(Value.newBuilder().setNumberValue(repositoryEntity.getId()).build())
              .setOperator(OperatorEnum.Operator.EQ)
              .setValueType(ValueTypeEnum.ValueType.NUMBER)
              .build();
      KeyValueQuery commitHashPredicate =
          KeyValueQuery.newBuilder()
              .setKey(ModelDBConstants.VERSIONED_INPUTS + "." + ModelDBConstants.COMMIT)
              .setValue(Value.newBuilder().setStringValue(commitEntity.getCommit_hash()).build())
              .setOperator(OperatorEnum.Operator.EQ)
              .setValueType(ValueTypeEnum.ValueType.STRING)
              .build();

      Location location = Location.newBuilder().addAllLocation(request.getLocationList()).build();
      KeyValueQuery locationPredicate =
          KeyValueQuery.newBuilder()
              .setKey(
                  ModelDBConstants.VERSIONED_INPUTS + "." + ModelDBConstants.VERSIONING_LOCATION)
              .setValue(
                  Value.newBuilder()
                      .setStringValue(ModelDBUtils.getStringFromProtoObject(location)))
              .setOperator(OperatorEnum.Operator.EQ)
              .setValueType(ValueTypeEnum.ValueType.STRING)
              .build();

      FindExperimentRuns.Builder findExperimentRuns =
          FindExperimentRuns.newBuilder()
              .setPageNumber(request.getPagination().getPageNumber())
              .setPageLimit(request.getPagination().getPageLimit())
              .setAscending(true)
              .setSortKey(ModelDBConstants.DATE_UPDATED)
              .addPredicates(repositoryIdPredicate)
              .addPredicates(commitHashPredicate)
              .addPredicates(locationPredicate);
      UserInfo currentLoginUserInfo = authService.getCurrentLoginUserInfo();
      if (request.getRepositoryId().hasNamedId()) {
        findExperimentRuns.setWorkspaceName(
            request.getRepositoryId().getNamedId().getWorkspaceName());
      } else {
        WorkspaceDTO workspaceDTO =
            roleService.getWorkspaceDTOByWorkspaceId(
                currentLoginUserInfo,
                repositoryEntity.getWorkspace_id(),
                repositoryEntity.getWorkspace_type());
        if (workspaceDTO != null && workspaceDTO.getWorkspaceName() != null) {
          findExperimentRuns.setWorkspaceName(workspaceDTO.getWorkspaceName());
        }
      }

      ExperimentRunPaginationDTO experimentRunPaginationDTO =
          findExperimentRuns(
              projectDAO, authService.getCurrentLoginUserInfo(), findExperimentRuns.build());

      return ListBlobExperimentRunsRequest.Response.newBuilder()
          .addAllRuns(experimentRunPaginationDTO.getExperimentRuns())
          .setTotalRecords(experimentRunPaginationDTO.getTotalRecords())
          .build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return listBlobExperimentRuns(projectDAO, request, repositoryFunction, commitFunction);
      } else {
        throw ex;
      }
    }
  }

  private Optional<ArtifactEntity> getExperimentRunArtifact(
      Session session, String experimentRunId, String key) {
    ExperimentRunEntity experimentRunObj = session.get(ExperimentRunEntity.class, experimentRunId);
    if (experimentRunObj == null) {
      LOGGER.info(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
      Status status =
          Status.newBuilder()
              .setCode(Code.NOT_FOUND_VALUE)
              .setMessage(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG)
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }

    Map<String, List<ArtifactEntity>> artifactEntityMap = experimentRunObj.getArtifactEntityMap();

    List<ArtifactEntity> result =
        (artifactEntityMap != null && artifactEntityMap.containsKey(ModelDBConstants.ARTIFACTS))
            ? artifactEntityMap.get(ModelDBConstants.ARTIFACTS)
            : Collections.emptyList();
    return result.stream()
        .filter(artifactEntity -> artifactEntity.getKey().equals(key))
        .findFirst();
  }

  private String buildArtifactLockKey(String experimentRunId, String artifactKey) {
    return "expRun::" + experimentRunId + "::key::" + artifactKey;
  }

  @Override
  public Entry<String, String> getExperimentRunArtifactS3PathAndMultipartUploadID(
      String experimentRunId, String key, long partNumber, S3KeyFunction initializeMultipart)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ArtifactEntity artifactEntity = getArtifactEntity(session, experimentRunId, key);
      try (AutoCloseable ignored = acquireWriteLock(buildArtifactLockKey(experimentRunId, key))) {
        return getS3PathAndMultipartUploadId(
            session, artifactEntity, partNumber != 0, initializeMultipart);
      } catch (Exception e) {
        throwException(e);
        return null;
      }
    }
  }

  public ArtifactEntity getArtifactEntity(Session session, String experimentRunId, String key)
      throws ModelDBException {
    try (AutoCloseable ignored = acquireReadLock(buildArtifactLockKey(experimentRunId, key))) {
      Optional<ArtifactEntity> artifactEntityOptional =
          getExperimentRunArtifact(session, experimentRunId, key);
      return artifactEntityOptional.orElseThrow(
          () -> new ModelDBException("Can't find specified artifact", Code.NOT_FOUND));
    } catch (Exception e) {
      throwException(e);
      return null;
    }
  }

  private SimpleEntry<String, String> getS3PathAndMultipartUploadId(
      Session session,
      ArtifactEntity artifactEntity,
      boolean partNumberSpecified,
      S3KeyFunction initializeMultipart) {
    String uploadId;
    if (partNumberSpecified) {
      uploadId = artifactEntity.getUploadId();
      try {
        String message = null;
        if (uploadId == null || artifactEntity.isUploadCompleted()) {
          if (initializeMultipart == null) {
            message = "Multipart wasn't initialized";
          } else {
            uploadId = initializeMultipart.apply(artifactEntity.getPath()).orElse(null);
          }
        }
        if (message != null) {
          LOGGER.info(message);
          throw new ModelDBException(message, io.grpc.Status.Code.FAILED_PRECONDITION);
        }
      } catch (ModelDBException e) {
        Status status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(e.getMessage())
                .addDetails(Any.pack(CommitMultipartArtifact.Response.getDefaultInstance()))
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      if (!Objects.equals(uploadId, artifactEntity.getUploadId())
          || artifactEntity.isUploadCompleted()) {
        session.beginTransaction();
        VersioningUtils.getArtifactPartEntities(
                session,
                String.valueOf(artifactEntity.getId()),
                ArtifactPartEntity.EXP_RUN_ARTIFACT)
            .forEach(session::delete);
        artifactEntity.setUploadId(uploadId);
        artifactEntity.setUploadCompleted(false);
        session.getTransaction().commit();
      }
    } else {
      uploadId = null;
    }
    return new AbstractMap.SimpleEntry<>(artifactEntity.getPath(), uploadId);
  }

  private String buildArtifactPartLockKey(Long artifactId, Long partNumber) {
    return "artifactId::" + artifactId + "::partNumber::" + partNumber;
  }

  @Override
  public Response commitArtifactPart(CommitArtifactPart request) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ArtifactEntity artifactEntity = getArtifactEntity(session, request.getId(), request.getKey());
      try (AutoCloseable ignored =
          acquireWriteLock(
              buildArtifactPartLockKey(
                  artifactEntity.getId(), request.getArtifactPart().getPartNumber()))) {
        VersioningUtils.saveOrUpdateArtifactPartEntity(
            request.getArtifactPart(),
            session,
            String.valueOf(artifactEntity.getId()),
            ArtifactPartEntity.EXP_RUN_ARTIFACT);
        return Response.newBuilder().build();
      } catch (Exception e) {
        throwException(e);
        return null;
      }
    }
  }

  @Override
  public GetCommittedArtifactParts.Response getCommittedArtifactParts(
      GetCommittedArtifactParts request) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      try (AutoCloseable ignored =
          acquireReadLock(buildArtifactLockKey(request.getId(), request.getKey()))) {
        Set<ArtifactPartEntity> artifactPartEntities =
            getArtifactPartEntities(session, request.getId(), request.getKey());
        GetCommittedArtifactParts.Response.Builder response =
            GetCommittedArtifactParts.Response.newBuilder();
        artifactPartEntities.forEach(
            artifactPartEntity -> response.addArtifactParts(artifactPartEntity.toProto()));
        return response.build();
      } catch (Exception e) {
        throwException(e);
        return null;
      }
    }
  }

  public void throwException(Exception e) throws ModelDBException {
    if (e instanceof ModelDBException) {
      throw (ModelDBException) e;
    } else if (e instanceof StatusRuntimeException) {
      throw (StatusRuntimeException) e;
    } else {
      throw new ModelDBException(e);
    }
  }

  private Set<ArtifactPartEntity> getArtifactPartEntities(
      Session session, String experimentRunId, String key) throws ModelDBException {
    ArtifactEntity artifactEntity = getArtifactEntity(session, experimentRunId, key);
    return VersioningUtils.getArtifactPartEntities(
        session, String.valueOf(artifactEntity.getId()), ArtifactPartEntity.EXP_RUN_ARTIFACT);
  }

  @Override
  public CommitMultipartArtifact.Response commitMultipartArtifact(
      CommitMultipartArtifact request, CommitMultipartFunction commitMultipartFunction)
      throws ModelDBException {
    List<PartETag> partETags;
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      try (AutoCloseable ignored =
          acquireWriteLock(buildArtifactLockKey(request.getId(), request.getKey()))) {
        ArtifactEntity artifactEntity =
            getArtifactEntity(session, request.getId(), request.getKey());
        if (artifactEntity.getUploadId() == null) {
          String message = "Multipart wasn't initialized OR Multipart artifact already committed";
          LOGGER.info(message);
          throw new ModelDBException(message, Code.FAILED_PRECONDITION);
        }
        Set<ArtifactPartEntity> artifactPartEntities =
            VersioningUtils.getArtifactPartEntities(
                session,
                String.valueOf(artifactEntity.getId()),
                ArtifactPartEntity.EXP_RUN_ARTIFACT);
        partETags =
            artifactPartEntities.stream()
                .map(ArtifactPartEntity::toPartETag)
                .collect(Collectors.toList());
        commitMultipartFunction.apply(
            artifactEntity.getPath(), artifactEntity.getUploadId(), partETags);
        session.beginTransaction();
        artifactEntity.setUploadCompleted(true);
        artifactEntity.setUploadId(null);
        session.getTransaction().commit();
      } catch (Exception e) {
        throwException(e);
        return null;
      }
    }
    return CommitMultipartArtifact.Response.newBuilder().build();
  }

  private void deleteAllKeyValueEntities(
      Session session, String experimentRunId, String fieldType) {
    Query query = session.createQuery(DELETE_ALL_KEY_VALUES_HQL);
    query.setParameter(ModelDBConstants.EXPERIMENT_RUN_ID_STR, experimentRunId);
    query.setParameter("field_type", fieldType);
    query.executeUpdate();
  }

  private void deleteKeyValueEntities(
      Session session, String experimentRunId, List<String> keys, String fieldType) {
    Query query = session.createQuery(DELETE_SELECTED_KEY_VALUES_HQL);
    query.setParameterList("keys", keys);
    query.setParameter(ModelDBConstants.EXPERIMENT_RUN_ID_STR, experimentRunId);
    query.setParameter("field_type", fieldType);
    query.executeUpdate();
  }

  @Override
  public void deleteExperimentRunKeyValuesEntities(
      String experimentRunId,
      List<String> experimentRunKeyValuesKeys,
      Boolean deleteAll,
      String fieldType)
      throws InvalidProtocolBufferException {
    String projectId = getProjectIdByExperimentRunId(experimentRunId);
    // Validate if current user has access to the entity or not
    roleService.validateEntityUserWithUserInfo(
        ModelDBServiceResourceTypes.PROJECT,
        projectId,
        ModelDBActionEnum.ModelDBServiceActions.UPDATE);

    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      if (deleteAll) {
        deleteAllKeyValueEntities(session, experimentRunId, fieldType);
      } else {
        deleteKeyValueEntities(session, experimentRunId, experimentRunKeyValuesKeys, fieldType);
      }
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunObj.setDate_updated(currentTimestamp);
      session.update(experimentRunObj);
      transaction.commit();
      LOGGER.debug("ExperimentRun {} deleted successfully", fieldType);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        deleteExperimentRunKeyValuesEntities(
            experimentRunId, experimentRunKeyValuesKeys, deleteAll, fieldType);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public void deleteExperimentRunObservationsEntities(
      String experimentRunId, List<String> experimentRunObservationsKeys, Boolean deleteAll)
      throws InvalidProtocolBufferException {
    String projectId = getProjectIdByExperimentRunId(experimentRunId);
    // Validate if current user has access to the entity or not
    roleService.validateEntityUserWithUserInfo(
        ModelDBServiceResourceTypes.PROJECT,
        projectId,
        ModelDBActionEnum.ModelDBServiceActions.UPDATE);

    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      Query query = session.createQuery(GET_ALL_OBSERVATIONS_HQL);
      query.setParameter(ModelDBConstants.EXPERIMENT_RUN_ID_STR, experimentRunId);
      query.setParameter("field_type", ModelDBConstants.OBSERVATIONS);
      List<ObservationEntity> observationEntities = query.list();
      List<ObservationEntity> removedObservationEntities = new ArrayList<>();
      if (deleteAll) {
        observationEntities.forEach(session::delete);
        removedObservationEntities.addAll(observationEntities);
      } else {
        observationEntities.forEach(
            observationEntity -> {
              if ((observationEntity.getKeyValueMapping() != null
                      && experimentRunObservationsKeys.contains(
                          observationEntity.getKeyValueMapping().getKey()))
                  || (observationEntity.getArtifactMapping() != null
                      && experimentRunObservationsKeys.contains(
                          observationEntity.getArtifactMapping().getKey()))) {
                session.delete(observationEntity);
                removedObservationEntities.add(observationEntity);
              }
            });
      }
      ExperimentRunEntity experimentRunObj =
          session.load(ExperimentRunEntity.class, experimentRunId);
      experimentRunObj.getObservationMapping().removeAll(removedObservationEntities);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunObj.setDate_updated(currentTimestamp);
      session.update(experimentRunObj);
      transaction.commit();
      LOGGER.debug("ExperimentRun {} deleted successfully", ModelDBConstants.OBSERVATIONS);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        deleteExperimentRunObservationsEntities(
            experimentRunId, experimentRunObservationsKeys, deleteAll);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public ExperimentRunPaginationDTO getExperimentRunsByDatasetVersionId(
      ProjectDAO projectDAO, GetExperimentRunsByDatasetVersionId request)
      throws ModelDBException, InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      CommitEntity commitEntity = session.get(CommitEntity.class, request.getDatasetVersionId());
      if (commitEntity == null) {
        throw new ModelDBException("DatasetVersion not found", Code.NOT_FOUND);
      }

      List<RepositoryEntity> datasets = new ArrayList<>(commitEntity.getRepository());
      if (datasets.size() == 0) {
        throw new ModelDBException("DatasetVersion not attached with the dataset", Code.INTERNAL);
      } else if (datasets.size() > 1) {
        throw new ModelDBException(
            "DatasetVersion '"
                + commitEntity.getCommit_hash()
                + "' associated with multiple datasets",
            Code.INTERNAL);
      } else if (!datasets.get(0).isDataset()) {
        throw new ModelDBException("DatasetVersion not attached with the dataset", Code.NOT_FOUND);
      }

      KeyValueQuery entityKeyValuePredicate =
          KeyValueQuery.newBuilder()
              .setKey(ModelDBConstants.DATASETS + "." + ModelDBConstants.LINKED_ARTIFACT_ID)
              .setValue(Value.newBuilder().setStringValue(commitEntity.getCommit_hash()).build())
              .setOperator(OperatorEnum.Operator.EQ)
              .setValueType(ValueTypeEnum.ValueType.STRING)
              .build();

      FindExperimentRuns findExperimentRuns =
          FindExperimentRuns.newBuilder()
              .setPageNumber(request.getPageNumber())
              .setPageLimit(request.getPageLimit())
              .setAscending(request.getAscending())
              .setSortKey(request.getSortKey())
              .addPredicates(entityKeyValuePredicate)
              .build();
      UserInfo currentLoginUserInfo = authService.getCurrentLoginUserInfo();
      ExperimentRunPaginationDTO experimentRunPaginationDTO =
          findExperimentRuns(projectDAO, currentLoginUserInfo, findExperimentRuns);
      LOGGER.debug(
          "Final return ExperimentRun count : {}",
          experimentRunPaginationDTO.getExperimentRuns().size());
      LOGGER.debug(
          "Final return total record count : {}", experimentRunPaginationDTO.getTotalRecords());
      return experimentRunPaginationDTO;
    }
  }

  @Override
  public ExperimentRun cloneExperimentRun(
      ProjectDAO projectDAO, CloneExperimentRun cloneExperimentRun, UserInfo userInfo)
      throws InvalidProtocolBufferException, ModelDBException {
    ExperimentRun srcExperimentRun = getExperimentRun(cloneExperimentRun.getSrcExperimentRunId());

    // Validate if current user has access to the entity or not
    roleService.validateEntityUserWithUserInfo(
        ModelDBServiceResourceTypes.PROJECT,
        srcExperimentRun.getProjectId(),
        ModelDBActionEnum.ModelDBServiceActions.UPDATE);

    ExperimentRun.Builder desExperimentRunBuilder = srcExperimentRun.toBuilder().clone();
    desExperimentRunBuilder
        .setId(UUID.randomUUID().toString())
        .setDateCreated(Calendar.getInstance().getTimeInMillis())
        .setDateUpdated(Calendar.getInstance().getTimeInMillis())
        .setStartTime(Calendar.getInstance().getTimeInMillis())
        .setEndTime(Calendar.getInstance().getTimeInMillis());

    if (!cloneExperimentRun.getDestExperimentRunName().isEmpty()) {
      desExperimentRunBuilder.setName(cloneExperimentRun.getDestExperimentRunName());
    } else {
      desExperimentRunBuilder.setName(srcExperimentRun.getName() + " - " + new Date().getTime());
    }

    if (!cloneExperimentRun.getDestExperimentId().isEmpty()) {
      try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
        ExperimentEntity destExperimentEntity =
            session.get(ExperimentEntity.class, cloneExperimentRun.getDestExperimentId());
        if (destExperimentEntity == null) {
          throw new ModelDBException(
              "Destination experiment '" + cloneExperimentRun.getDestExperimentId() + "' not found",
              Code.NOT_FOUND);
        }

        // Validate if current user has access to the entity or not
        roleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.PROJECT,
            destExperimentEntity.getProject_id(),
            ModelDBActionEnum.ModelDBServiceActions.UPDATE);
        desExperimentRunBuilder.setProjectId(destExperimentEntity.getProject_id());
      }
      desExperimentRunBuilder.setExperimentId(cloneExperimentRun.getDestExperimentId());
    }

    desExperimentRunBuilder.clearOwner().setOwner(authService.getVertaIdFromUserInfo(userInfo));
    return insertExperimentRun(projectDAO, desExperimentRunBuilder.build(), userInfo);
  }

  private void validateExperimentRunPerWorkspaceForTrial(
      ProjectDAO projectDAO, String projectId, UserInfo userInfo)
      throws InvalidProtocolBufferException, ModelDBException {
    if (app.getTrialEnabled()) {
      Project project = projectDAO.getProjectByID(projectId);
      if (project.getWorkspaceId() != null && !project.getWorkspaceId().isEmpty()) {
        WorkspaceDTO workspaceDTO =
            roleService.getWorkspaceDTOByWorkspaceId(
                userInfo, project.getWorkspaceId(), project.getWorkspaceTypeValue());

        // TODO: We can be replaced by a count(*) query instead .setIdsOnly(true)
        FindExperimentRuns findExperimentRuns =
            FindExperimentRuns.newBuilder()
                .setIdsOnly(true)
                .setWorkspaceName(workspaceDTO.getWorkspaceName())
                .build();
        ExperimentRunPaginationDTO paginationDTO =
            findExperimentRuns(projectDAO, userInfo, findExperimentRuns);
        if (app.getMaxExperimentRunPerWorkspace() != null
            && paginationDTO.getTotalRecords() >= app.getMaxExperimentRunPerWorkspace()) {
          throw new ModelDBException(
              ModelDBConstants.LIMIT_RUN_NUMBER
                  + "Maximum "
                  + app.getMaxExperimentRunPerWorkspace()
                  + " experimentRuns are allow in the same workspace",
              Code.RESOURCE_EXHAUSTED);
        }
      }
    }
  }
}
