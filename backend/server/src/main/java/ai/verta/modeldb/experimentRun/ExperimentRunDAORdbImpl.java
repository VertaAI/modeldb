package ai.verta.modeldb.experimentRun;

import static ai.verta.modeldb.entities.config.ConfigBlobEntity.HYPERPARAMETER;

import ai.verta.common.*;
import ai.verta.common.ModelDBResourceEnum.ModelDBServiceResourceTypes;
import ai.verta.modeldb.*;
import ai.verta.modeldb.CommitArtifactPart;
import ai.verta.modeldb.CommitArtifactPart.Response;
import ai.verta.modeldb.CommitMultipartArtifact;
import ai.verta.modeldb.GetCommittedArtifactParts;
import ai.verta.modeldb.authservice.MDBRoleService;
import ai.verta.modeldb.common.CommonConstants;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.authservice.AuthService;
import ai.verta.modeldb.common.authservice.RoleServiceUtils;
import ai.verta.modeldb.common.collaborator.CollaboratorUser;
import ai.verta.modeldb.common.exceptions.AlreadyExistsException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.exceptions.PermissionDeniedException;
import ai.verta.modeldb.common.exceptions.UnimplementedException;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.dto.ExperimentRunPaginationDTO;
import ai.verta.modeldb.entities.*;
import ai.verta.modeldb.entities.code.GitCodeBlobEntity;
import ai.verta.modeldb.entities.code.NotebookCodeBlobEntity;
import ai.verta.modeldb.entities.config.ConfigBlobEntity;
import ai.verta.modeldb.entities.config.HyperparameterElementMappingEntity;
import ai.verta.modeldb.entities.dataset.PathDatasetComponentBlobEntity;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.entities.versioning.VersioningModeldbEntityMapping;
import ai.verta.modeldb.metadata.MetadataDAO;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.modeldb.versioning.*;
import ai.verta.uac.*;
import ai.verta.uac.ModelDBActionEnum.ModelDBServiceActions;
import com.amazonaws.services.s3.model.PartETag;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import io.grpc.StatusRuntimeException;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import javax.persistence.criteria.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class ExperimentRunDAORdbImpl implements ExperimentRunDAO {

  private static final Logger LOGGER =
      LogManager.getLogger(ExperimentRunDAORdbImpl.class.getName());
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private static final boolean OVERWRITE_VERSION_MAP = false;
  private static final String FIELD_TYPE_QUERY_PARAM = "field_type";
  private static final String REPO_IDS_QUERY_PARAM = "repoIds";
  private final MDBConfig mdbConfig;
  private static final long CACHE_SIZE = 1000;
  private static final int DURATION = 10;
  private final AuthService authService;
  private final MDBRoleService mdbRoleService;
  private final RepositoryDAO repositoryDAO;
  private final CommitDAO commitDAO;
  private final BlobDAO blobDAO;
  private final MetadataDAO metadataDAO;
  private static final String CHECK_EXP_RUN_EXISTS_AT_INSERT_HQL =
      "Select count(*) From ExperimentRunEntity ere where ere.name = :experimentRunName AND ere.project_id = :projectId AND ere.experiment_id = :experimentId AND ere.deleted = false";
  private static final String CHECK_EXP_RUN_EXISTS_AT_UPDATE_HQL =
      "Select count(*) From ExperimentRunEntity ere where ere.id = :experimentRunId AND ere.deleted = false";
  private static final String GET_EXP_RUN_BY_IDS_HQL =
      "From ExperimentRunEntity exr where exr.id IN (:ids) AND exr.deleted = false ";
  private static final String DELETE_ALL_TAGS_HQL =
      "delete from TagsMapping tm WHERE tm.experimentRunEntity.id = :experimentRunId";
  private static final String DELETE_SELECTED_TAGS_HQL =
      "delete from TagsMapping tm WHERE tm.tags in (:tags) AND tm.experimentRunEntity.id = :experimentRunId";
  private static final String DELETE_ALL_ARTIFACTS_HQL =
      "delete from ArtifactEntity ar WHERE ar.experimentRunEntity.id = :experimentRunId";
  private static final String DELETE_SELECTED_ARTIFACTS_HQL =
      "delete from ArtifactEntity ar WHERE ar.key in (:keys) AND ar.experimentRunEntity.id = :experimentRunId AND ar.field_type = :field_type";
  private static final String GET_EXP_RUN_ATTRIBUTE_BY_KEYS_HQL =
      "From AttributeEntity attr where attr.key in (:keys) AND attr.experimentRunEntity.id = :experimentRunId AND attr.field_type = :fieldType";
  private static final String DELETE_ALL_EXP_RUN_ATTRIBUTES_HQL =
      "delete from AttributeEntity attr WHERE attr.experimentRunEntity.id = :experimentRunId AND attr.field_type = :fieldType";
  private static final String DELETE_SELECTED_EXP_RUN_ATTRIBUTES_HQL =
      "delete from AttributeEntity attr WHERE attr.key in (:keys) AND attr.experimentRunEntity.id = :experimentRunId AND attr.field_type = :fieldType";
  private static final String GET_EXPERIMENT_RUN_BY_PROJECT_ID_HQL =
      "From ExperimentRunEntity ere where ere.project_id IN (:projectIds) AND ere.deleted = false";
  private static final String GET_EXPERIMENT_RUN_BY_EXPERIMENT_ID_HQL =
      "From ExperimentRunEntity ere where ere.experiment_id IN (:experimentIds) AND ere.deleted = false";
  private static final String DELETED_STATUS_EXPERIMENT_RUN_QUERY_STRING =
      "UPDATE ExperimentRunEntity expr SET expr.deleted = :deleted WHERE expr.id IN (:experimentRunIds)";
  private static final String DELETE_ALL_KEY_VALUES_HQL =
      "delete from KeyValueEntity kv WHERE kv.experimentRunEntity.id = :experimentRunId AND kv.field_type = :field_type";
  private static final String DELETE_SELECTED_KEY_VALUES_HQL =
      "delete from KeyValueEntity kv WHERE kv.key in (:keys) AND kv.experimentRunEntity.id = :experimentRunId AND kv.field_type = :field_type";
  private static final String GET_ALL_OBSERVATIONS_HQL =
      "FROM ObservationEntity oe WHERE oe.experimentRunEntity.id = :experimentRunId AND oe.field_type = :field_type";

  private final LoadingCache<String, ReadWriteLock> locks =
      CacheBuilder.newBuilder()
          .maximumSize(CACHE_SIZE)
          .expireAfterWrite(DURATION, TimeUnit.MINUTES)
          .build(
              new CacheLoader<String, ReadWriteLock>() {
                public ReadWriteLock load(String lockKey) {
                  return new ReentrantReadWriteLock() {};
                }
              });

  @SuppressWarnings({"squid:S2222"})
  protected AutoCloseable acquireReadLock(String lockKey) throws ExecutionException {
    LOGGER.debug("acquireReadLock for key: {}", lockKey);
    ReadWriteLock lock = locks.get(lockKey);
    var readLock = lock.readLock();
    readLock.lock();
    return readLock::unlock;
  }

  @SuppressWarnings({"squid:S2222"})
  protected AutoCloseable acquireWriteLock(String lockKey) throws ExecutionException {
    LOGGER.debug("acquireWriteLock for key: {}", lockKey);
    ReadWriteLock lock = locks.get(lockKey);
    var writeLock = lock.writeLock();
    writeLock.lock();
    return writeLock::unlock;
  }

  public ExperimentRunDAORdbImpl(
      MDBConfig mdbConfig,
      AuthService authService,
      MDBRoleService mdbRoleService,
      RepositoryDAO repositoryDAO,
      CommitDAO commitDAO,
      BlobDAO blobDAO,
      MetadataDAO metadataDAO) {
    this.mdbConfig = mdbConfig;
    this.authService = authService;
    this.mdbRoleService = mdbRoleService;
    this.repositoryDAO = repositoryDAO;
    this.commitDAO = commitDAO;
    this.blobDAO = blobDAO;
    this.metadataDAO = metadataDAO;
  }

  private void checkIfEntityAlreadyExists(ExperimentRun experimentRun, Boolean isInsert) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
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
      var existStatus = false;
      if (count > 0) {
        existStatus = true;
      }

      // Throw error if it is an insert request and ExperimentRun with same name already exists
      if (existStatus && isInsert) {
        throw new AlreadyExistsException("ExperimentRun already exists in database");
      } else if (!existStatus && !isInsert) {
        // Throw error if it is an update request and ExperimentRun with given name does not exist
        throw new NotFoundException("ExperimentRun does not exist in database");
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
      throw new ModelDBException(errorMessage, Code.INVALID_ARGUMENT);
    }
    var repositoryIdentification =
        RepositoryIdentification.newBuilder().setRepoId(versioningEntry.getRepositoryId()).build();
    var commitEntity =
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
        var locationKey = String.join("#", locationBlobKeyMap.getValue().getLocationList());
        if (!locationBlobWithHashMap.containsKey(locationKey)) {
          throw new ModelDBException(
              "Blob Location '"
                  + locationBlobKeyMap.getValue().getLocationList()
                  + "' for key '"
                  + locationBlobKeyMap.getKey()
                  + "' not found in commit blobs",
              Code.INVALID_ARGUMENT);
        }
        requestedLocationBlobWithHashMap.put(locationKey, locationBlobWithHashMap.get(locationKey));
      }
    }
    return requestedLocationBlobWithHashMap;
  }

  @Override
  public ExperimentRun insertExperimentRun(ExperimentRun experimentRun, UserInfo userInfo)
      throws ModelDBException {
    checkIfEntityAlreadyExists(experimentRun, true);
    createRoleBindingsForExperimentRun(experimentRun, userInfo);
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {

      if (experimentRun.getDatasetsCount() > 0
          && mdbConfig.isPopulateConnectionsBasedOnPrivileges()) {
        experimentRun = checkDatasetVersionBasedOnPrivileges(experimentRun, true);
      }

      var experimentRunObj = RdbmsUtils.generateExperimentRunEntity(experimentRun);
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
      var transaction = session.beginTransaction();
      experimentRunObj.setCreated(true);
      session.saveOrUpdate(experimentRunObj);
      transaction.commit();
      LOGGER.debug("ExperimentRun created successfully");
      return experimentRun;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return insertExperimentRun(experimentRun, userInfo);
      } else {
        throw ex;
      }
    }
  }

  private void createRoleBindingsForExperimentRun(ExperimentRun experimentRun, UserInfo userInfo) {
    mdbRoleService.createRoleBinding(
        ModelDBConstants.ROLE_EXPERIMENT_RUN_OWNER,
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
                        var hyperparamElemConfBlobEntity =
                            configBlobEntity.getHyperparameterElementConfigBlobEntity();
                        try {
                          var hyrParamMapping =
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
  public List<String> deleteExperimentRuns(List<String> experimentRunIds) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      List<Resources> allowedProjectIds =
          mdbRoleService.getSelfAllowedResources(
              ModelDBServiceResourceTypes.PROJECT, ModelDBServiceActions.UPDATE);

      List<String> accessibleExperimentRunIds =
          getAccessibleExperimentRunIDs(experimentRunIds, allowedProjectIds);
      if (accessibleExperimentRunIds.isEmpty()) {
        throw new PermissionDeniedException(
            "Access is denied. User is unauthorized for given ExperimentRun entities : "
                + accessibleExperimentRunIds);
      }
      var transaction = session.beginTransaction();
      var query =
          session
              .createQuery(DELETED_STATUS_EXPERIMENT_RUN_QUERY_STRING)
              .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
      query.setParameter("deleted", true);
      query.setParameter("experimentRunIds", accessibleExperimentRunIds);
      int updatedCount = query.executeUpdate();
      LOGGER.debug(
          "Mark ExperimentRun as deleted : {}, count : {}",
          accessibleExperimentRunIds,
          updatedCount);
      transaction.commit();
      LOGGER.debug("ExperimentRun deleted successfully");
      return accessibleExperimentRunIds;
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
      String entityKey,
      String entityValue,
      Integer pageNumber,
      Integer pageLimit,
      Boolean order,
      String sortKey)
      throws PermissionDeniedException {

    KeyValueQuery entityKeyValuePredicate =
        KeyValueQuery.newBuilder()
            .setKey(entityKey)
            .setValue(Value.newBuilder().setStringValue(entityValue).build())
            .build();

    var findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .setPageNumber(pageNumber)
            .setPageLimit(pageLimit)
            .setAscending(order)
            .setSortKey(sortKey)
            .addPredicates(entityKeyValuePredicate)
            .build();
    var currentLoginUserInfo = authService.getCurrentLoginUserInfo();
    return findExperimentRuns(currentLoginUserInfo, findExperimentRuns);
  }

  @Override
  public List<ExperimentRun> getExperimentRuns(String key, String value, UserInfo userInfo) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      List<ExperimentRun> experimentRuns = new ArrayList<>();

      Map<String, Object[]> whereClauseParamMap = new HashMap<>();
      var idValueArr = new Object[2];
      idValueArr[0] = RdbmsUtils.getRdbOperatorSymbol(OperatorEnum.Operator.EQ);
      idValueArr[1] = value;
      whereClauseParamMap.put(key, idValueArr);

      LOGGER.debug("Getting experimentRun for {} ", userInfo);
      if (userInfo != null) {
        var ownerValueArr = new Object[2];
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
  public List<ExperimentRun> getExperimentRunsByBatchIds(List<String> experimentRunIds) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var query = session.createQuery(GET_EXP_RUN_BY_IDS_HQL);
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
  public ExperimentRun getExperimentRun(String experimentRunId) throws ModelDBException {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentRunEntity = session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunEntity == null) {
        throw new NotFoundException(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
      }
      LOGGER.debug("Got ExperimentRun successfully");
      var experimentRun = experimentRunEntity.getProtoObject();
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
    if (mdbConfig.isPopulateConnectionsBasedOnPrivileges()) {
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
    var experimentRunBuilder = experimentRun.toBuilder();
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
      if (mdbRoleService.checkConnectionsBasedOnPrivileges(
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
    var query = session.createQuery(CHECK_EXP_RUN_EXISTS_AT_UPDATE_HQL);
    query.setParameter("experimentRunId", experimentRunId);
    Long count = (Long) query.uniqueResult();
    return count > 0;
  }

  @Override
  public void updateExperimentRunName(String experimentRunId, String experimentRunName) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentRunEntity =
          session.load(ExperimentRunEntity.class, experimentRunId, LockMode.PESSIMISTIC_WRITE);
      experimentRunEntity.setName(experimentRunName);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntity.setDate_updated(currentTimestamp);
      experimentRunEntity.increaseVersionNumber();
      var transaction = session.beginTransaction();
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
      String experimentRunId, String experimentRunDescription) throws ModelDBException {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentRunEntity =
          session.load(ExperimentRunEntity.class, experimentRunId, LockMode.PESSIMISTIC_WRITE);
      experimentRunEntity.setDescription(experimentRunDescription);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntity.setDate_updated(currentTimestamp);
      experimentRunEntity.increaseVersionNumber();
      var transaction = session.beginTransaction();
      session.update(experimentRunEntity);
      transaction.commit();
      LOGGER.debug("ExperimentRun description updated successfully");
      var experimentRun = experimentRunEntity.getProtoObject();
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
  public void logExperimentRunCodeVersion(String experimentRunId, CodeVersion updatedCodeVersion) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentRunEntity =
          session.get(ExperimentRunEntity.class, experimentRunId, LockMode.PESSIMISTIC_WRITE);

      var existingCodeVersionEntity = experimentRunEntity.getCode_version_snapshot();
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
      experimentRunEntity.increaseVersionNumber();
      var transaction = session.beginTransaction();
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
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentRunEntity =
          session.load(ExperimentRunEntity.class, experimentRunId, LockMode.PESSIMISTIC_WRITE);
      experimentRunEntity.setParent_id(parentExperimentRunId);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntity.setDate_updated(currentTimestamp);
      experimentRunEntity.increaseVersionNumber();
      var transaction = session.beginTransaction();
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
      throws ModelDBException {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId, LockMode.PESSIMISTIC_WRITE);
      if (experimentRunObj == null) {
        throw new NotFoundException(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
      }
      List<String> newTags = new ArrayList<>();
      var existingProtoExperimentRunObj = experimentRunObj.getProtoObject();
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
        experimentRunObj.increaseVersionNumber();
        var transaction = session.beginTransaction();
        session.saveOrUpdate(experimentRunObj);
        transaction.commit();
      }
      LOGGER.debug("ExperimentRun tags added successfully");
      var experimentRun = experimentRunObj.getProtoObject();
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
      throws ModelDBException {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();
      if (deleteAll) {
        var query =
            session
                .createQuery(DELETE_ALL_TAGS_HQL)
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter(ModelDBConstants.EXPERIMENT_RUN_ID_STR, experimentRunId);
        query.executeUpdate();
      } else {
        var query =
            session
                .createQuery(DELETE_SELECTED_TAGS_HQL)
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter("tags", experimentRunTagList);
        query.setParameter(ModelDBConstants.EXPERIMENT_RUN_ID_STR, experimentRunId);
        query.executeUpdate();
      }
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunObj.setDate_updated(currentTimestamp);
      experimentRunObj.increaseVersionNumber();
      session.update(experimentRunObj);
      transaction.commit();
      LOGGER.debug("ExperimentRun tags deleted successfully");
      var experimentRun = experimentRunObj.getProtoObject();
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
  public void logObservations(String experimentRunId, List<Observation> observations) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();
      var experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId, LockMode.PESSIMISTIC_WRITE);
      if (experimentRunEntityObj == null) {
        throw new NotFoundException(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
      }

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
      experimentRunEntityObj.increaseVersionNumber();
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
  public List<Observation> getObservationByKey(String experimentRunId, String observationKey) {

    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentRunEntityObj = session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunEntityObj == null) {
        throw new NotFoundException(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
      }
      var experimentRun = experimentRunEntityObj.getProtoObject();
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
  public void logMetrics(String experimentRunId, List<KeyValue> newMetrics) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId, LockMode.PESSIMISTIC_WRITE);
      if (experimentRunEntityObj == null) {
        throw new NotFoundException(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
      }

      List<KeyValue> existingMetrics = experimentRunEntityObj.getProtoObject().getMetricsList();
      for (KeyValue existingMetric : existingMetrics) {
        for (KeyValue newMetric : newMetrics) {
          if (existingMetric.getKey().equals(newMetric.getKey())) {
            throw new AlreadyExistsException(
                "Metric being logged already exists. existing metric Key : " + newMetric.getKey());
          }
        }
      }

      List<KeyValueEntity> newMetricList =
          RdbmsUtils.convertKeyValuesFromKeyValueEntityList(
              experimentRunEntityObj, ModelDBConstants.METRICS, newMetrics);
      experimentRunEntityObj.setKeyValueMapping(newMetricList);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntityObj.setDate_updated(currentTimestamp);
      experimentRunEntityObj.increaseVersionNumber();
      var transaction = session.beginTransaction();
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
  public List<KeyValue> getExperimentRunMetrics(String experimentRunId) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunObj == null) {
        throw new NotFoundException(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
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
  public List<Artifact> getExperimentRunDatasets(String experimentRunId) throws ModelDBException {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunObj == null) {
        throw new NotFoundException(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
      }
      LOGGER.debug("Got ExperimentRun Datasets");
      var experimentRun = experimentRunObj.getProtoObject();
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
      throws ModelDBException {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId, LockMode.PESSIMISTIC_WRITE);
      if (experimentRunEntityObj == null) {
        throw new NotFoundException(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
      }
      var experimentRun = experimentRunEntityObj.getProtoObject();

      var transaction = session.beginTransaction();
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
              throw new AlreadyExistsException(
                  "Dataset being logged already exists. existing dataSet key : "
                      + newDataset.getKey());
            }
          }
        }
      }

      if (mdbConfig.isPopulateConnectionsBasedOnPrivileges()) {
        newDatasets = getPrivilegedDatasets(newDatasets, true);
      }

      List<ArtifactEntity> newDatasetList =
          RdbmsUtils.convertArtifactsFromArtifactEntityList(
              experimentRunEntityObj,
              ModelDBConstants.DATASETS,
              newDatasets,
              ExperimentRunEntity.class.getSimpleName(),
              experimentRunEntityObj.getId());
      experimentRunEntityObj.setArtifactMapping(newDatasetList);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntityObj.setDate_updated(currentTimestamp);
      experimentRunEntityObj.increaseVersionNumber();
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
      throws ModelDBException {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId, LockMode.PESSIMISTIC_WRITE);
      if (experimentRunEntityObj == null) {
        throw new NotFoundException(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
      }

      List<Artifact> existingArtifacts = experimentRunEntityObj.getProtoObject().getArtifactsList();
      for (Artifact existingArtifact : existingArtifacts) {
        for (Artifact newArtifact : newArtifacts) {
          if (existingArtifact.getKey().equals(newArtifact.getKey())) {
            throw new AlreadyExistsException(
                "Artifact being logged already exists. existing artifact key : "
                    + newArtifact.getKey());
          }
        }
      }

      List<ArtifactEntity> newArtifactList =
          RdbmsUtils.convertArtifactsFromArtifactEntityList(
              experimentRunEntityObj,
              ModelDBConstants.ARTIFACTS,
              newArtifacts,
              ExperimentRunEntity.class.getSimpleName(),
              experimentRunEntityObj.getId());
      experimentRunEntityObj.setArtifactMapping(newArtifactList);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntityObj.setDate_updated(currentTimestamp);
      experimentRunEntityObj.increaseVersionNumber();
      var transaction = session.beginTransaction();
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

  @Override
  public List<Artifact> getExperimentRunArtifacts(String experimentRunId) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunObj == null) {
        throw new NotFoundException(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
      }
      var experimentRun = experimentRunObj.getProtoObject();
      if (experimentRun.getArtifactsList() != null && !experimentRun.getArtifactsList().isEmpty()) {
        LOGGER.debug("Got ExperimentRun Artifacts");
        return experimentRun.getArtifactsList();
      } else {
        var errorMessage = "Artifacts not found in the ExperimentRun";
        throw new NotFoundException(errorMessage);
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
    var query =
        session
            .createQuery(DELETE_SELECTED_ARTIFACTS_HQL)
            .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
    query.setParameterList("keys", keys);
    query.setParameter(ModelDBConstants.EXPERIMENT_RUN_ID_STR, experimentRunId);
    query.setParameter(FIELD_TYPE_QUERY_PARAM, fieldType);
    query.executeUpdate();
  }

  @Override
  public void deleteArtifacts(String experimentRunId, String artifactKey) {
    Transaction transaction = null;
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      transaction = session.beginTransaction();

      if (false) { // Change it with parameter for support to delete all artifacts
        var query =
            session
                .createQuery(DELETE_ALL_ARTIFACTS_HQL)
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
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
      experimentRunObj.increaseVersionNumber();
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
  public void logHyperparameters(String experimentRunId, List<KeyValue> newHyperparameters) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId, LockMode.PESSIMISTIC_WRITE);
      if (experimentRunEntityObj == null) {
        throw new NotFoundException(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
      }

      List<KeyValue> existingHyperparameters =
          experimentRunEntityObj.getProtoObject().getHyperparametersList();
      for (KeyValue existingHyperparameter : existingHyperparameters) {
        for (KeyValue newHyperparameter : newHyperparameters) {
          if (existingHyperparameter.getKey().equals(newHyperparameter.getKey())) {
            throw new AlreadyExistsException(
                "Hyperparameter being logged already exists. existing hyperparameter Key : "
                    + newHyperparameter.getKey());
          }
        }
      }

      List<KeyValueEntity> newHyperparameterList =
          RdbmsUtils.convertKeyValuesFromKeyValueEntityList(
              experimentRunEntityObj, ModelDBConstants.HYPERPARAMETERS, newHyperparameters);
      experimentRunEntityObj.setKeyValueMapping(newHyperparameterList);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntityObj.setDate_updated(currentTimestamp);
      experimentRunEntityObj.increaseVersionNumber();
      var transaction = session.beginTransaction();
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
  public List<KeyValue> getExperimentRunHyperparameters(String experimentRunId) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunObj == null) {
        throw new NotFoundException(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
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
  public void logAttributes(String experimentRunId, List<KeyValue> newAttributes) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId, LockMode.PESSIMISTIC_WRITE);
      if (experimentRunEntityObj == null) {
        throw new NotFoundException(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
      }

      List<KeyValue> existingAttributes =
          experimentRunEntityObj.getProtoObject().getAttributesList();
      for (KeyValue existingAttribute : existingAttributes) {
        for (KeyValue newAttribute : newAttributes) {
          if (existingAttribute.getKey().equals(newAttribute.getKey())) {
            throw new AlreadyExistsException(
                "Attribute being logged already exists. existing attribute Key : "
                    + newAttribute.getKey());
          }
        }
      }

      List<AttributeEntity> newAttributeList =
          RdbmsUtils.convertAttributesFromAttributeEntityList(
              experimentRunEntityObj, ModelDBConstants.ATTRIBUTES, newAttributes);
      experimentRunEntityObj.setAttributeMapping(newAttributeList);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntityObj.setDate_updated(currentTimestamp);
      experimentRunEntityObj.increaseVersionNumber();
      var transaction = session.beginTransaction();
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
      String experimentRunId, List<String> attributeKeyList, Boolean getAll) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunObj == null) {
        var errorMessage = "Invalid ExperimentRun ID found";
        throw new NotFoundException(errorMessage);
      }

      if (getAll) {
        return experimentRunObj.getProtoObject().getAttributesList();
      } else {
        var query = session.createQuery(GET_EXP_RUN_ATTRIBUTE_BY_KEYS_HQL);
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
      List<String> requestedExperimentRunIds, Collection<Resources> allowedProjects) {
    List<String> accessibleExperimentRunIds = new ArrayList<>();

    Map<String, String> projectIdExperimentRunIdMap =
        getProjectIdsFromExperimentRunIds(requestedExperimentRunIds);
    if (projectIdExperimentRunIdMap.size() == 0) {
      throw new PermissionDeniedException(
          "Access is denied. ExperimentRun not found for given ids : " + requestedExperimentRunIds);
    }
    Set<String> projectIdSet = new HashSet<>(projectIdExperimentRunIdMap.values());
    Set<String> allowedProjectIds =
        RoleServiceUtils.getAccessibleResourceIdsFromAllowedResources(
            projectIdSet, allowedProjects);
    for (Map.Entry<String, String> entry : projectIdExperimentRunIdMap.entrySet()) {
      if (allowedProjectIds.contains(entry.getValue())) {
        accessibleExperimentRunIds.add(entry.getKey());
      }
    }
    return accessibleExperimentRunIds;
  }

  @Override
  public ExperimentRunPaginationDTO findExperimentRuns(
      UserInfo currentLoginUserInfo, FindExperimentRuns queryParameters)
      throws PermissionDeniedException {

    LOGGER.trace("trying to open session");
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      LOGGER.trace("Starting to find experimentRuns");

      Workspace workspace = null;
      if (!queryParameters.getWorkspaceName().isEmpty()) {
        workspace =
            mdbRoleService.getWorkspaceByWorkspaceName(
                currentLoginUserInfo, queryParameters.getWorkspaceName());
      }
      List<GetResourcesResponseItem> accessibleProjectResourceByWorkspace =
          mdbRoleService.getResourceItems(
              workspace,
              !queryParameters.getProjectId().isEmpty()
                  ? new HashSet<>(Collections.singletonList(queryParameters.getProjectId()))
                  : Collections.emptySet(),
              ModelDBServiceResourceTypes.PROJECT,
              false);
      Set<String> accessibleProjectIds =
          accessibleProjectResourceByWorkspace.stream()
              .map(GetResourcesResponseItem::getResourceId)
              .collect(Collectors.toSet());

      List<String> accessibleExperimentRunIds = new ArrayList<>();
      List<Resources> accessibleProjects =
          Collections.singletonList(
              Resources.newBuilder()
                  .addAllResourceIds(accessibleProjectIds)
                  .setResourceType(
                      ResourceType.newBuilder()
                          .setModeldbServiceResourceType(ModelDBServiceResourceTypes.PROJECT))
                  .build());
      if (!queryParameters.getExperimentRunIdsList().isEmpty()) {
        accessibleExperimentRunIds.addAll(
            getAccessibleExperimentRunIDs(
                queryParameters.getExperimentRunIdsList(), accessibleProjects));
        if (accessibleExperimentRunIds.isEmpty()) {
          throw new PermissionDeniedException(
              "Access is denied. User is unauthorized for given ExperimentRun IDs : "
                  + accessibleExperimentRunIds);
        }
      }

      List<KeyValueQuery> predicates = new ArrayList<>(queryParameters.getPredicatesList());
      for (KeyValueQuery predicate : predicates) {
        if (predicate.getKey().equals(ModelDBConstants.ID)) {
          List<String> accessibleExperimentRunId =
              getAccessibleExperimentRunIDs(
                  Collections.singletonList(predicate.getValue().getStringValue()),
                  accessibleProjects);
          accessibleExperimentRunIds.addAll(accessibleExperimentRunId);
          // Validate if current user has access to the entity or not where predicate key has an id
          RdbmsUtils.validatePredicates(
              ModelDBConstants.EXPERIMENT_RUNS,
              accessibleExperimentRunIds,
              predicate,
              mdbRoleService.IsImplemented());
        }
      }

      var builder = session.getCriteriaBuilder();
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
      if (!queryParameters.getProjectId().isEmpty()
          && accessibleProjectIds.contains(queryParameters.getProjectId())) {
        projectIds.add(queryParameters.getProjectId());
      } else if (accessibleExperimentRunIds.isEmpty()
          && queryParameters.getExperimentId().isEmpty()) {
        projectIds.addAll(accessibleProjectIds);
      }

      if (accessibleExperimentRunIds.isEmpty()
          && projectIds.isEmpty()
          && queryParameters.getExperimentId().isEmpty()) {
        throw new PermissionDeniedException(
            "Access is denied. Accessible projects not found for given ExperimentRun IDs : "
                + accessibleExperimentRunIds);
      }

      if (!projectIds.isEmpty()) {
        Expression<String> projectExpression = experimentRunRoot.get(ModelDBConstants.PROJECT_ID);
        var projectsPredicate = projectExpression.in(projectIds);
        finalPredicatesList.add(projectsPredicate);
      }

      if (!queryParameters.getExperimentId().isEmpty()) {
        Expression<String> exp = experimentRunRoot.get(ModelDBConstants.EXPERIMENT_ID);
        var predicate2 = builder.equal(exp, queryParameters.getExperimentId());
        finalPredicatesList.add(predicate2);
      }

      if (!queryParameters.getExperimentRunIdsList().isEmpty()) {
        Expression<String> exp = experimentRunRoot.get(ModelDBConstants.ID);
        var predicate2 = exp.in(queryParameters.getExperimentRunIdsList());
        finalPredicatesList.add(predicate2);
      }

      LOGGER.trace("Added entity predicates");
      var entityName = "experimentRunEntity";
      try {
        List<Predicate> queryPredicatesList =
            RdbmsUtils.getQueryPredicatesFromPredicateList(
                entityName,
                predicates,
                builder,
                criteriaQuery,
                experimentRunRoot,
                authService,
                mdbRoleService,
                ModelDBServiceResourceTypes.EXPERIMENT_RUN);
        if (!queryPredicatesList.isEmpty()) {
          finalPredicatesList.addAll(queryPredicatesList);
        }
      } catch (ModelDBException ex) {
        if (ex.getCode().ordinal() == Code.FAILED_PRECONDITION_VALUE
            && ModelDBConstants.INTERNAL_MSG_USERS_NOT_FOUND.equals(ex.getMessage())) {
          LOGGER.info(ex.getMessage());
          var experimentRunPaginationDTO = new ExperimentRunPaginationDTO();
          experimentRunPaginationDTO.setExperimentRuns(Collections.emptyList());
          experimentRunPaginationDTO.setTotalRecords(0L);
          return experimentRunPaginationDTO;
        }
        throw ex;
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

      var predicateArr = new Predicate[finalPredicatesList.size()];
      for (var index = 0; index < finalPredicatesList.size(); index++) {
        predicateArr[index] = finalPredicatesList.get(index);
      }

      var predicateWhereCause = builder.and(predicateArr);
      criteriaQuery.select(experimentRunRoot);
      criteriaQuery.where(predicateWhereCause);
      criteriaQuery.orderBy(orderBy);

      LOGGER.trace("Creating criteria query");
      var query = session.createQuery(criteriaQuery);
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

        List<Resources> selfAllowedRepositories = new ArrayList<>();
        if (mdbConfig.isPopulateConnectionsBasedOnPrivileges()) {
          selfAllowedRepositories =
              mdbRoleService.getSelfAllowedResources(
                  ModelDBServiceResourceTypes.REPOSITORY,
                  ModelDBActionEnum.ModelDBServiceActions.READ);
        }

        List<String> expRunIds =
            experimentRunEntities.stream()
                .map(ExperimentRunEntity::getId)
                .collect(Collectors.toList());
        Map<String, List<KeyValue>> expRunHyperparameterConfigBlobMap =
            getExperimentRunHyperparameterConfigBlobMap(
                session, expRunIds, selfAllowedRepositories);

        // Map<experimentRunID, Map<LocationString, CodeVersion>> : Map from experimentRunID to Map
        // of
        // LocationString to CodeBlob
        Map<String, Map<String, CodeVersion>> expRunCodeVersionMap =
            getExperimentRunCodeVersionMap(session, expRunIds, selfAllowedRepositories);

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
              if (mdbConfig.isPopulateConnectionsBasedOnPrivileges()) {
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

      var experimentRunPaginationDTO = new ExperimentRunPaginationDTO();
      experimentRunPaginationDTO.setExperimentRuns(experimentRuns);
      experimentRunPaginationDTO.setTotalRecords(totalRecords);
      return experimentRunPaginationDTO;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return findExperimentRuns(currentLoginUserInfo, queryParameters);
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
      Session session, List<String> expRunIds, List<Resources> selfAllowedRepositories) {

    var queryBuilder =
        "Select vme.experimentRunEntity.id, cb From ConfigBlobEntity cb INNER JOIN VersioningModeldbEntityMapping vme ON vme.blob_hash = cb.blob_hash WHERE cb.hyperparameter_type = :hyperparameterType AND vme.experimentRunEntity.id IN (:expRunIds) ";

    Set<String> accessibleResourceIds = Collections.emptySet();
    if (mdbConfig.isPopulateConnectionsBasedOnPrivileges()) {
      boolean allowedAllResources =
          RoleServiceUtils.checkAllResourceAllowed(selfAllowedRepositories);
      if (allowedAllResources) {
        return new HashMap<>();
      }
      accessibleResourceIds = RoleServiceUtils.getResourceIds(selfAllowedRepositories);
      queryBuilder = queryBuilder + " AND vme.repository_id IN (:repoIds)";
    }

    var query = session.createQuery(queryBuilder);
    query.setParameter("hyperparameterType", HYPERPARAMETER);
    query.setParameterList("expRunIds", expRunIds);
    if (mdbConfig.isPopulateConnectionsBasedOnPrivileges()) {
      query.setParameterList(
          REPO_IDS_QUERY_PARAM,
          accessibleResourceIds.stream().map(Long::parseLong).collect(Collectors.toList()));
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
            var configBlobEntity = (ConfigBlobEntity) objects[1];
            if (configBlobEntity.getHyperparameter_type() == HYPERPARAMETER) {
              var hyperElementConfigBlobEntity =
                  configBlobEntity.getHyperparameterElementConfigBlobEntity();
              HyperparameterValuesConfigBlob valuesConfigBlob =
                  hyperElementConfigBlobEntity.toProto();
              var valueBuilder = Value.newBuilder();
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
                default:
                  // Do nothing
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
   */
  private Map<String, Map<String, CodeVersion>> getExperimentRunCodeVersionMap(
      Session session, List<String> expRunIds, List<Resources> selfAllowedRepositories) {

    String queryBuilder =
        "SELECT vme.experimentRunEntity.id, vme.versioning_location, gcb, ncb, pdcb "
            + " From VersioningModeldbEntityMapping vme LEFT JOIN GitCodeBlobEntity gcb ON vme.blob_hash = gcb.blob_hash "
            + " LEFT JOIN NotebookCodeBlobEntity ncb ON vme.blob_hash = ncb.blob_hash "
            + " LEFT JOIN PathDatasetComponentBlobEntity pdcb ON ncb.path_dataset_blob_hash = pdcb.id.path_dataset_blob_id "
            + " WHERE vme.versioning_blob_type = :versioningBlobType AND vme.experimentRunEntity.id IN (:expRunIds) ";

    Set<String> accessibleRepositoryIds = Collections.emptySet();
    if (mdbConfig.isPopulateConnectionsBasedOnPrivileges()) {
      boolean allowedAllResources =
          RoleServiceUtils.checkAllResourceAllowed(selfAllowedRepositories);
      if (allowedAllResources) {
        return new HashMap<>();
      }
      accessibleRepositoryIds = RoleServiceUtils.getResourceIds(selfAllowedRepositories);
      queryBuilder = queryBuilder + " AND vme.repository_id IN (:repoIds)";
    }

    var query = session.createQuery(queryBuilder);
    query.setParameter("versioningBlobType", Blob.ContentCase.CODE.getNumber());
    query.setParameterList("expRunIds", expRunIds);
    if (mdbConfig.isPopulateConnectionsBasedOnPrivileges()) {
      query.setParameterList(
          REPO_IDS_QUERY_PARAM,
          accessibleRepositoryIds.stream().map(Long::parseLong).collect(Collectors.toList()));
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
        var notebookCodeBlobEntity = (NotebookCodeBlobEntity) objects[3];
        var pathDatasetComponentBlobEntity = (PathDatasetComponentBlobEntity) objects[4];

        var codeVersionBuilder = CodeVersion.newBuilder();
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
        var locationBuilder = Location.newBuilder();
        CommonUtils.getProtoObjectFromString(versioningLocation, locationBuilder);
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
    var gitSnapShot = GitSnapshot.newBuilder();
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
  public ExperimentRunPaginationDTO sortExperimentRuns(SortExperimentRuns queryParameters)
      throws PermissionDeniedException {
    var findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .addAllExperimentRunIds(queryParameters.getExperimentRunIdsList())
            .setSortKey(queryParameters.getSortKey())
            .setAscending(queryParameters.getAscending())
            .setIdsOnly(queryParameters.getIdsOnly())
            .build();
    var currentLoginUserInfo = authService.getCurrentLoginUserInfo();
    return findExperimentRuns(currentLoginUserInfo, findExperimentRuns);
  }

  @Override
  public List<ExperimentRun> getTopExperimentRuns(TopExperimentRunsSelector queryParameters)
      throws PermissionDeniedException {
    var findExperimentRuns =
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
    var currentLoginUserInfo = authService.getCurrentLoginUserInfo();
    return findExperimentRuns(currentLoginUserInfo, findExperimentRuns).getExperimentRuns();
  }

  @Override
  public List<String> getExperimentRunTags(String experimentRunId) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
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
  public void addExperimentRunAttributes(String experimentRunId, List<KeyValue> attributesList) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId, LockMode.PESSIMISTIC_WRITE);
      List<AttributeEntity> newAttributeList =
          RdbmsUtils.convertAttributesFromAttributeEntityList(
              experimentRunEntityObj, ModelDBConstants.ATTRIBUTES, attributesList);
      experimentRunEntityObj.setAttributeMapping(newAttributeList);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntityObj.setDate_updated(currentTimestamp);
      experimentRunEntityObj.increaseVersionNumber();
      var transaction = session.beginTransaction();
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
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();
      if (deleteAll) {
        var query =
            session
                .createQuery(DELETE_ALL_EXP_RUN_ATTRIBUTES_HQL)
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter(ModelDBConstants.EXPERIMENT_RUN_ID_STR, experimentRunId);
        query.setParameter(ModelDBConstants.FIELD_TYPE_STR, ModelDBConstants.ATTRIBUTES);
        query.executeUpdate();
      } else {
        var query =
            session
                .createQuery(DELETE_SELECTED_EXP_RUN_ATTRIBUTES_HQL)
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter("keys", attributeKeyList);
        query.setParameter(ModelDBConstants.EXPERIMENT_RUN_ID_STR, experimentRunId);
        query.setParameter(ModelDBConstants.FIELD_TYPE_STR, ModelDBConstants.ATTRIBUTES);
        query.executeUpdate();
      }
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunObj.setDate_updated(currentTimestamp);
      experimentRunObj.increaseVersionNumber();
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
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId, LockMode.PESSIMISTIC_WRITE);
      experimentRunEntityObj.setJob_id(jobId);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntityObj.setDate_updated(currentTimestamp);
      experimentRunEntityObj.increaseVersionNumber();
      var transaction = session.beginTransaction();
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
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentRunEntityObj = session.get(ExperimentRunEntity.class, experimentRunId);
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
    var experimentRunBuilder =
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
  public List<ExperimentRun> getExperimentRuns(List<KeyValue> keyValues) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var stringQueryBuilder = new StringBuilder("From ExperimentRunEntity er where ");
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
        stringQueryBuilder.append(" er." + key + " = :" + key);
        if (index < keyValues.size() - 1) {
          stringQueryBuilder.append(" AND ");
        }
      }
      stringQueryBuilder.append(" AND er.deleted = false ");
      var query = session.createQuery(stringQueryBuilder.toString());
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
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunObj != null) {
        LOGGER.debug("Got ProjectId by ExperimentRunId ");
        return experimentRunObj.getProject_id();
      } else {
        String errorMessage = "ExperimentRun not found for given ID : " + experimentRunId;
        throw new NotFoundException(errorMessage);
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
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var query =
          session.createQuery(
              "Select exr.id, exr.project_id From ExperimentRunEntity exr where exr.id IN (:ids) AND exr.deleted = false ");
      query.setParameterList("ids", experimentRunIds);

      @SuppressWarnings("unchecked")
      List<Object[]> selectedFieldsFromQuery = query.list();
      LOGGER.debug("Got ExperimentRun by Ids. Size : {}", selectedFieldsFromQuery.size());
      Map<String, String> experimentRunIdToProjectIdMap = new HashMap<>();
      for (Object[] selectedFields : selectedFieldsFromQuery) {
        experimentRunIdToProjectIdMap.put(
            String.valueOf(selectedFields[0]), String.valueOf(selectedFields[1]));
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
      List<String> experimentRunIds, List<String> selectedFields) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var alias = "exr";
      var queryBuilder = new StringBuilder("Select ");
      if (selectedFields != null && !selectedFields.isEmpty()) {
        var index = 1;
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
      var experimentRunQuery = session.createQuery(queryBuilder.toString());
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
  public List<String> getExperimentRunIdsByProjectIds(List<String> projectIds) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentRunQuery = session.createQuery(GET_EXPERIMENT_RUN_BY_PROJECT_ID_HQL);
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
  public List<String> getExperimentRunIdsByExperimentIds(List<String> experimentIds) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var experimentRunQuery = session.createQuery(GET_EXPERIMENT_RUN_BY_EXPERIMENT_ID_HQL);
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
  public void logVersionedInput(LogVersionedInput request) throws ModelDBException {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var versioningEntry = request.getVersionedInputs();
      Map<String, Map.Entry<BlobExpanded, String>> locationBlobWithHashMap =
          validateVersioningEntity(session, versioningEntry);
      ExperimentRunEntity runEntity =
          session.get(ExperimentRunEntity.class, request.getId(), LockMode.PESSIMISTIC_WRITE);
      List<VersioningModeldbEntityMapping> versioningModeldbEntityMappings =
          RdbmsUtils.getVersioningMappingFromVersioningInput(
              session, versioningEntry, locationBlobWithHashMap, runEntity);

      List<VersioningModeldbEntityMapping> existingMappings = runEntity.getVersioned_inputs();
      if (existingMappings.isEmpty()) {
        existingMappings.addAll(versioningModeldbEntityMappings);
      } else {
        if (!versioningModeldbEntityMappings.isEmpty()) {
          VersioningModeldbEntityMapping existingFirstEntityMapping = existingMappings.get(0);
          var versioningModeldbFirstEntityMapping = versioningModeldbEntityMappings.get(0);
          if (!existingFirstEntityMapping
                  .getRepository_id()
                  .equals(versioningModeldbFirstEntityMapping.getRepository_id())
              || !existingFirstEntityMapping
                  .getCommit()
                  .equals(versioningModeldbFirstEntityMapping.getCommit())) {
            if (!OVERWRITE_VERSION_MAP) {
              throw new ModelDBException(
                  ModelDBConstants.DIFFERENT_REPOSITORY_OR_COMMIT_MESSAGE, Code.ALREADY_EXISTS);
            }
            var cb = session.getCriteriaBuilder();
            CriteriaDelete<VersioningModeldbEntityMapping> delete =
                cb.createCriteriaDelete(VersioningModeldbEntityMapping.class);
            Root<VersioningModeldbEntityMapping> e =
                delete.from(VersioningModeldbEntityMapping.class);
            delete.where(cb.in(e.get("experimentRunEntity")).value(runEntity));
            var transaction = session.beginTransaction();
            session
                .createQuery(delete)
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE))
                .executeUpdate();
            transaction.commit();
            existingMappings.addAll(versioningModeldbEntityMappings);
          } else {
            List<VersioningModeldbEntityMapping> finalVersionList = new ArrayList<>();
            for (VersioningModeldbEntityMapping versioningModeldbEntityMapping :
                versioningModeldbEntityMappings) {
              var addNew = true;
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
      runEntity.increaseVersionNumber();
      var transaction = session.beginTransaction();
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
    var fetchAllExpRunLogVersionedInputsHqlBuilder =
        new StringBuilder(
            "DELETE FROM VersioningModeldbEntityMapping vm WHERE vm.repository_id = :repoId ");
    fetchAllExpRunLogVersionedInputsHqlBuilder
        .append(" AND vm.entity_type = '")
        .append(ExperimentRunEntity.class.getSimpleName())
        .append("' ");
    if (commitHash != null && !commitHash.isEmpty()) {
      fetchAllExpRunLogVersionedInputsHqlBuilder.append(" AND vm.commit = :commitHash");
    }
    var query =
        session
            .createQuery(fetchAllExpRunLogVersionedInputsHqlBuilder.toString())
            .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
    query.setParameter("repoId", repoId);
    if (commitHash != null && !commitHash.isEmpty()) {
      query.setParameter("commitHash", commitHash);
    }
    query.executeUpdate();
    LOGGER.debug("ExperimentRun versioning deleted successfully");
  }

  @Override
  public void deleteLogVersionedInputs(Session session, List<Long> repoIds) {
    var fetchAllExpRunLogVersionedInputsHqlBuilder =
        new StringBuilder(
            "DELETE FROM VersioningModeldbEntityMapping vm WHERE vm.repository_id IN (:repoIds) ");
    fetchAllExpRunLogVersionedInputsHqlBuilder
        .append(" AND vm.entity_type = '")
        .append(ExperimentRunEntity.class.getSimpleName())
        .append("'");
    var query =
        session
            .createQuery(fetchAllExpRunLogVersionedInputsHqlBuilder.toString())
            .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
    query.setParameter(REPO_IDS_QUERY_PARAM, repoIds);
    query.executeUpdate();
    LOGGER.debug("ExperimentRun versioning deleted successfully");
  }

  @Override
  public GetVersionedInput.Response getVersionedInputs(GetVersionedInput request) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, request.getId());
      if (experimentRunObj != null) {
        var experimentRun = experimentRunObj.getProtoObject();
        if (experimentRun.getVersionedInputs() != null
            && experimentRun.getVersionedInputs().getRepositoryId() != 0
            && mdbConfig.isPopulateConnectionsBasedOnPrivileges()) {
          experimentRun =
              checkVersionInputBasedOnPrivileges(experimentRun, new HashSet<>(), new HashSet<>());
        }
        LOGGER.debug("ExperimentRun versioning fetch successfully");
        return GetVersionedInput.Response.newBuilder()
            .setVersionedInputs(experimentRun.getVersionedInputs())
            .build();
      } else {
        String errorMessage = "ExperimentRun not found for given ID : " + request.getId();
        throw new NotFoundException(errorMessage);
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
      ListCommitExperimentRunsRequest request,
      RepositoryFunction repositoryFunction,
      CommitFunction commitFunction)
      throws ModelDBException {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var repositoryEntity = repositoryFunction.apply(session);
      var commitEntity = commitFunction.apply(session, session1 -> repositoryEntity);

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
      var currentLoginUserInfo = authService.getCurrentLoginUserInfo();
      if (request.getRepositoryId().hasNamedId()) {
        findExperimentRuns.setWorkspaceName(
            request.getRepositoryId().getNamedId().getWorkspaceName());
      } else {
        GetResourcesResponseItem entityResource =
            mdbRoleService.getEntityResource(
                Optional.of(String.valueOf(request.getRepositoryId().getRepoId())),
                Optional.empty(),
                ModelDBServiceResourceTypes.REPOSITORY);
        var workspace = authService.workspaceById(true, entityResource.getWorkspaceId());
        if (workspace != null) {
          findExperimentRuns.setWorkspaceName(
              workspace.getInternalIdCase() == Workspace.InternalIdCase.ORG_ID
                  ? workspace.getOrgName()
                  : workspace.getUsername());
        }
      }
      var experimentRunPaginationDTO =
          findExperimentRuns(currentLoginUserInfo, findExperimentRuns.build());
      return ListCommitExperimentRunsRequest.Response.newBuilder()
          .addAllRuns(experimentRunPaginationDTO.getExperimentRuns())
          .setTotalRecords(experimentRunPaginationDTO.getTotalRecords())
          .build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return listCommitExperimentRuns(request, repositoryFunction, commitFunction);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public ListBlobExperimentRunsRequest.Response listBlobExperimentRuns(
      ListBlobExperimentRunsRequest request,
      RepositoryFunction repositoryFunction,
      CommitFunction commitFunction)
      throws ModelDBException {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var repositoryEntity = repositoryFunction.apply(session);
      var commitEntity = commitFunction.apply(session, session1 -> repositoryEntity);

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

      var location = Location.newBuilder().addAllLocation(request.getLocationList()).build();
      KeyValueQuery locationPredicate =
          KeyValueQuery.newBuilder()
              .setKey(
                  ModelDBConstants.VERSIONED_INPUTS + "." + ModelDBConstants.VERSIONING_LOCATION)
              .setValue(
                  Value.newBuilder().setStringValue(CommonUtils.getStringFromProtoObject(location)))
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
      var currentLoginUserInfo = authService.getCurrentLoginUserInfo();
      if (request.getRepositoryId().hasNamedId()) {
        findExperimentRuns.setWorkspaceName(
            request.getRepositoryId().getNamedId().getWorkspaceName());
      } else {
        GetResourcesResponseItem entityResource =
            mdbRoleService.getEntityResource(
                Optional.of(String.valueOf(request.getRepositoryId().getRepoId())),
                Optional.empty(),
                ModelDBServiceResourceTypes.REPOSITORY);
        var workspace = authService.workspaceById(true, entityResource.getWorkspaceId());
        if (workspace != null) {
          findExperimentRuns.setWorkspaceName(
              workspace.getInternalIdCase() == Workspace.InternalIdCase.ORG_ID
                  ? workspace.getOrgName()
                  : workspace.getUsername());
        }
      }

      var experimentRunPaginationDTO =
          findExperimentRuns(currentLoginUserInfo, findExperimentRuns.build());

      return ListBlobExperimentRunsRequest.Response.newBuilder()
          .addAllRuns(experimentRunPaginationDTO.getExperimentRuns())
          .setTotalRecords(experimentRunPaginationDTO.getTotalRecords())
          .build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return listBlobExperimentRuns(request, repositoryFunction, commitFunction);
      } else {
        throw ex;
      }
    }
  }

  private Optional<ArtifactEntity> getExperimentRunArtifact(
      Session session, String experimentRunId, String key) {
    ExperimentRunEntity experimentRunObj =
        session.get(ExperimentRunEntity.class, experimentRunId, LockMode.PESSIMISTIC_WRITE);
    if (experimentRunObj == null) {
      throw new NotFoundException(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
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
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var artifactEntity = getArtifactEntity(session, experimentRunId, key);
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
    if (partNumberSpecified
        && mdbConfig.getArtifactStoreConfig().getArtifactStoreType().equals(CommonConstants.S3)) {
      uploadId = artifactEntity.getUploadId();
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
        throw new ModelDBException(message, Code.FAILED_PRECONDITION);
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
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var artifactEntity = getArtifactEntity(session, request.getId(), request.getKey());
      try (AutoCloseable ignored =
          acquireWriteLock(
              buildArtifactPartLockKey(
                  artifactEntity.getId(), request.getArtifactPart().getPartNumber()))) {
        VersioningUtils.saveArtifactPartEntity(
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
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      try (AutoCloseable ignored =
          acquireReadLock(buildArtifactLockKey(request.getId(), request.getKey()))) {
        Set<ArtifactPartEntity> artifactPartEntities =
            getArtifactPartEntities(session, request.getId(), request.getKey());
        var response = GetCommittedArtifactParts.Response.newBuilder();
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
    var artifactEntity = getArtifactEntity(session, experimentRunId, key);
    return VersioningUtils.getArtifactPartEntities(
        session, String.valueOf(artifactEntity.getId()), ArtifactPartEntity.EXP_RUN_ARTIFACT);
  }

  @Override
  public CommitMultipartArtifact.Response commitMultipartArtifact(
      CommitMultipartArtifact request, CommitMultipartFunction commitMultipartFunction)
      throws ModelDBException {
    List<PartETag> partETags;
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      try (AutoCloseable ignored =
          acquireWriteLock(buildArtifactLockKey(request.getId(), request.getKey()))) {
        var artifactEntity = getArtifactEntity(session, request.getId(), request.getKey());
        if (artifactEntity.getUploadId() == null) {
          var message = "Multipart wasn't initialized OR Multipart artifact already committed";
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
    var query =
        session
            .createQuery(DELETE_ALL_KEY_VALUES_HQL)
            .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
    query.setParameter(ModelDBConstants.EXPERIMENT_RUN_ID_STR, experimentRunId);
    query.setParameter(FIELD_TYPE_QUERY_PARAM, fieldType);
    query.executeUpdate();
  }

  private void deleteKeyValueEntities(
      Session session, String experimentRunId, List<String> keys, String fieldType) {
    var query =
        session
            .createQuery(DELETE_SELECTED_KEY_VALUES_HQL)
            .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
    query.setParameterList("keys", keys);
    query.setParameter(ModelDBConstants.EXPERIMENT_RUN_ID_STR, experimentRunId);
    query.setParameter(FIELD_TYPE_QUERY_PARAM, fieldType);
    query.executeUpdate();
  }

  @Override
  public void deleteExperimentRunKeyValuesEntities(
      String experimentRunId,
      List<String> experimentRunKeyValuesKeys,
      Boolean deleteAll,
      String fieldType) {
    String projectId = getProjectIdByExperimentRunId(experimentRunId);
    // Validate if current user has access to the entity or not
    mdbRoleService.validateEntityUserWithUserInfo(
        ModelDBServiceResourceTypes.PROJECT,
        projectId,
        ModelDBActionEnum.ModelDBServiceActions.UPDATE);

    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();
      if (deleteAll) {
        deleteAllKeyValueEntities(session, experimentRunId, fieldType);
      } else {
        deleteKeyValueEntities(session, experimentRunId, experimentRunKeyValuesKeys, fieldType);
      }
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunObj.setDate_updated(currentTimestamp);
      experimentRunObj.increaseVersionNumber();
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
      String experimentRunId, List<String> experimentRunObservationsKeys, Boolean deleteAll) {
    String projectId = getProjectIdByExperimentRunId(experimentRunId);
    // Validate if current user has access to the entity or not
    mdbRoleService.validateEntityUserWithUserInfo(
        ModelDBServiceResourceTypes.PROJECT,
        projectId,
        ModelDBActionEnum.ModelDBServiceActions.UPDATE);

    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();
      var query =
          session
              .createQuery(GET_ALL_OBSERVATIONS_HQL)
              .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
      query.setParameter(ModelDBConstants.EXPERIMENT_RUN_ID_STR, experimentRunId);
      query.setParameter(FIELD_TYPE_QUERY_PARAM, ModelDBConstants.OBSERVATIONS);
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
      experimentRunObj.increaseVersionNumber();
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
      GetExperimentRunsByDatasetVersionId request) throws ModelDBException {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var commitEntity = session.get(CommitEntity.class, request.getDatasetVersionId());
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

      var findExperimentRuns =
          FindExperimentRuns.newBuilder()
              .setPageNumber(request.getPageNumber())
              .setPageLimit(request.getPageLimit())
              .setAscending(request.getAscending())
              .setSortKey(request.getSortKey())
              .addPredicates(entityKeyValuePredicate)
              .build();
      var currentLoginUserInfo = authService.getCurrentLoginUserInfo();
      var experimentRunPaginationDTO = findExperimentRuns(currentLoginUserInfo, findExperimentRuns);
      LOGGER.debug(
          "Final return ExperimentRun count : {}",
          experimentRunPaginationDTO.getExperimentRuns().size());
      LOGGER.debug(
          "Final return total record count : {}", experimentRunPaginationDTO.getTotalRecords());
      return experimentRunPaginationDTO;
    }
  }

  @Override
  public ExperimentRun cloneExperimentRun(CloneExperimentRun cloneExperimentRun, UserInfo userInfo)
      throws ModelDBException {
    var srcExperimentRun = getExperimentRun(cloneExperimentRun.getSrcExperimentRunId());

    // Validate if current user has access to the entity or not
    mdbRoleService.validateEntityUserWithUserInfo(
        ModelDBServiceResourceTypes.PROJECT,
        srcExperimentRun.getProjectId(),
        ModelDBActionEnum.ModelDBServiceActions.UPDATE);

    var desExperimentRunBuilder = srcExperimentRun.toBuilder().clone();
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
      try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
        var destExperimentEntity =
            session.get(ExperimentEntity.class, cloneExperimentRun.getDestExperimentId());
        if (destExperimentEntity == null) {
          throw new ModelDBException(
              "Destination experiment '" + cloneExperimentRun.getDestExperimentId() + "' not found",
              Code.NOT_FOUND);
        }

        // Validate if current user has access to the entity or not
        mdbRoleService.validateEntityUserWithUserInfo(
            ModelDBServiceResourceTypes.PROJECT,
            destExperimentEntity.getProject_id(),
            ModelDBActionEnum.ModelDBServiceActions.UPDATE);
        desExperimentRunBuilder.setProjectId(destExperimentEntity.getProject_id());
      }
      desExperimentRunBuilder.setExperimentId(cloneExperimentRun.getDestExperimentId());
    }

    desExperimentRunBuilder.clearOwner().setOwner(authService.getVertaIdFromUserInfo(userInfo));
    return insertExperimentRun(desExperimentRunBuilder.build(), userInfo);
  }

  @Override
  public void logEnvironment(String experimentRunId, EnvironmentBlob environmentBlob) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();
      var experimentRunEntity = session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunEntity == null) {
        throw new NotFoundException(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
      }
      experimentRunEntity.setEnvironment(CommonUtils.getStringFromProtoObject(environmentBlob));
      session.update(experimentRunEntity);
      transaction.commit();
      LOGGER.debug("EnvironmentBlob logged successfully");
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        logEnvironment(experimentRunId, environmentBlob);
      } else {
        throw ex;
      }
    }
  }
}
