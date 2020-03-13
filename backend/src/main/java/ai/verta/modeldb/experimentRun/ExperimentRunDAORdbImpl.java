package ai.verta.modeldb.experimentRun;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.Artifact;
import ai.verta.modeldb.CodeVersion;
import ai.verta.modeldb.Experiment;
import ai.verta.modeldb.ExperimentRun;
import ai.verta.modeldb.FindExperimentRuns;
import ai.verta.modeldb.GetVersionedInput;
import ai.verta.modeldb.KeyValueQuery;
import ai.verta.modeldb.Location;
import ai.verta.modeldb.LogVersionedInput;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.ModelDBMessages;
import ai.verta.modeldb.Observation;
import ai.verta.modeldb.OperatorEnum;
import ai.verta.modeldb.Project;
import ai.verta.modeldb.SortExperimentRuns;
import ai.verta.modeldb.TopExperimentRunsSelector;
import ai.verta.modeldb.VersioningEntry;
import ai.verta.modeldb.authservice.AuthService;
import ai.verta.modeldb.dto.ExperimentRunPaginationDTO;
import ai.verta.modeldb.entities.ArtifactEntity;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.CodeVersionEntity;
import ai.verta.modeldb.entities.CommentEntity;
import ai.verta.modeldb.entities.ExperimentRunEntity;
import ai.verta.modeldb.entities.KeyValueEntity;
import ai.verta.modeldb.entities.ObservationEntity;
import ai.verta.modeldb.entities.TagsMapping;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.modeldb.versioning.BlobDAO;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.CommitDAO;
import ai.verta.modeldb.versioning.RepositoryDAO;
import ai.verta.modeldb.versioning.RepositoryIdentification;
import ai.verta.uac.UserInfo;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

public class ExperimentRunDAORdbImpl implements ExperimentRunDAO {

  private static final Logger LOGGER =
      LogManager.getLogger(ExperimentRunDAORdbImpl.class.getName());
  private final AuthService authService;
  private final RepositoryDAO repositoryDAO;
  private final CommitDAO commitDAO;
  private final BlobDAO blobDAO;
  private static final String UPDATE_PROJECT_HQL =
      new StringBuilder("UPDATE ProjectEntity p SET p.")
          .append(ModelDBConstants.DATE_UPDATED)
          .append(" = :timestamp where p.")
          .append(ModelDBConstants.ID)
          .append(" IN (:ids) ")
          .toString();
  private static final String UPDATE_EXP_TIMESTAMP_HQL =
      new StringBuilder()
          .append("UPDATE ExperimentEntity exp SET exp.")
          .append(ModelDBConstants.DATE_UPDATED)
          .append(" = :timestamp")
          .append(" where exp.")
          .append(ModelDBConstants.ID)
          .append(" IN (:ids) ")
          .toString();
  private static final String CHECK_EXP_RUN_EXISTS_AT_INSERT_HQL =
      new StringBuilder("Select count(*) From ExperimentRunEntity ere where ")
          .append(" ere." + ModelDBConstants.NAME + " = :experimentRunName ")
          .append(" AND ere." + ModelDBConstants.PROJECT_ID + " = :projectId ")
          .append(" AND ere." + ModelDBConstants.EXPERIMENT_ID + " = :experimentId ")
          .toString();
  private static final String CHECK_EXP_RUN_EXISTS_AT_UPDATE_HQL =
      new StringBuilder("Select count(*) From ExperimentRunEntity ere where ")
          .append(" ere." + ModelDBConstants.ID + " = :experimentRunId ")
          .toString();
  private static final String GET_EXP_RUN_BY_IDS_HQL =
      "From ExperimentRunEntity exr where exr.id IN (:ids)";
  private static final String COMMENT_DELETE_HQL =
      new StringBuilder()
          .append("From CommentEntity ce where ce.")
          .append(ModelDBConstants.ENTITY_ID)
          .append(" IN (:entityIds) AND ce.")
          .append(ModelDBConstants.ENTITY_NAME)
          .append(" =:entityName")
          .toString();
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
          .toString();
  private static final String GET_EXPERIMENT_RUN_BY_EXPERIMENT_ID_HQL =
      new StringBuilder()
          .append("From ExperimentRunEntity ere where ere.")
          .append(ModelDBConstants.EXPERIMENT_ID)
          .append(" IN (:experimentIds) ")
          .toString();

  public ExperimentRunDAORdbImpl(
      AuthService authService, RepositoryDAO repositoryDAO, CommitDAO commitDAO, BlobDAO blobDAO) {
    this.authService = authService;
    this.repositoryDAO = repositoryDAO;
    this.commitDAO = commitDAO;
    this.blobDAO = blobDAO;
  }

  private void updateParentEntitiesTimestamp(
      Session session, List<String> projectIds, List<String> experimentIds, long currentTimestamp) {
    if (projectIds != null && !projectIds.isEmpty()) {
      Query query = session.createQuery(UPDATE_PROJECT_HQL);
      query.setParameter("timestamp", currentTimestamp);
      query.setParameterList("ids", projectIds);
      query.executeUpdate();
    }
    if (experimentIds != null && !experimentIds.isEmpty()) {
      Query query = session.createQuery(UPDATE_EXP_TIMESTAMP_HQL);
      query.setParameter("timestamp", currentTimestamp);
      query.setParameterList("ids", experimentIds);
      query.executeUpdate();
    }
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
    }
  }

  private void validateVersioningEntity(Session session, VersioningEntry versioningEntry)
      throws ModelDBException {
    String errorMessage = null;
    if (versioningEntry.getRepositoryId() == 0L) {
      errorMessage = "Repository Id not found in VersioningEntry";
    } else if (versioningEntry.getCommit().isEmpty()) {
      errorMessage = "Commit hash not found in VersioningEntry";
    } else if (versioningEntry.getKeyLocationMapMap().isEmpty()) {
      errorMessage = "Location map should not be empty in VersioningEntry";
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
    Map<String, BlobExpanded> locationBlobMap =
        blobDAO.getCommitBlobMap(session, commitEntity.getRootSha(), new ArrayList<>());
    for (Map.Entry<String, Location> locationBlobKeyMap :
        versioningEntry.getKeyLocationMapMap().entrySet()) {
      if (!locationBlobMap.containsKey(
          String.join("#", locationBlobKeyMap.getValue().getLocationList()))) {
        throw new ModelDBException(
            "Location list for key '" + locationBlobKeyMap.getKey() + "' not found in commit blobs",
            io.grpc.Status.Code.INVALID_ARGUMENT);
      }
    }
  }

  @Override
  public ExperimentRun insertExperimentRun(ExperimentRun experimentRun)
      throws InvalidProtocolBufferException, ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      checkIfEntityAlreadyExists(experimentRun, true);
      Transaction transaction = session.beginTransaction();
      if (experimentRun.getVersionedInputs() != null && experimentRun.hasVersionedInputs()) {
        validateVersioningEntity(session, experimentRun.getVersionedInputs());
      }
      ExperimentRunEntity experimentRunObj = RdbmsUtils.generateExperimentRunEntity(experimentRun);
      session.saveOrUpdate(experimentRunObj);
      // Update parent entity timestamp
      updateParentEntitiesTimestamp(
          session,
          Collections.singletonList(experimentRun.getProjectId()),
          Collections.singletonList(experimentRun.getExperimentId()),
          Calendar.getInstance().getTimeInMillis());
      transaction.commit();
      LOGGER.debug("ExperimentRun created successfully");
      return experimentRun;
    }
  }

  @Override
  public Boolean deleteExperimentRun(String experimentRunId) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();

      // Delete the ExperimentRun comments
      removeEntityComments(
          session,
          Collections.singletonList(experimentRunId),
          ExperimentRunEntity.class.getSimpleName());

      // Delete the ExperimentEntity object
      ExperimentRunEntity experimentRunObj =
          session.load(ExperimentRunEntity.class, experimentRunId);
      String projectId = experimentRunObj.getProject_id();
      String experimentId = experimentRunObj.getExperiment_id();
      session.delete(experimentRunObj);

      // Update parent entity timestamp
      updateParentEntitiesTimestamp(
          session,
          Collections.singletonList(projectId),
          Collections.singletonList(experimentId),
          Calendar.getInstance().getTimeInMillis());
      transaction.commit();
      LOGGER.debug("ExperimentRun deleted successfully");
      return true;
    }
  }

  @Override
  public Boolean deleteExperimentRuns(List<String> experimentRunIds) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      // Delete the ExperimentRUn comments
      if (!experimentRunIds.isEmpty()) {
        removeEntityComments(session, experimentRunIds, ExperimentRunEntity.class.getSimpleName());
      }

      // Delete the ExperimentEntity object
      Query query = session.createQuery(GET_EXP_RUN_BY_IDS_HQL);
      query.setParameterList("ids", experimentRunIds);

      List<String> projectIds = new ArrayList<>();
      List<String> experimentIds = new ArrayList<>();
      @SuppressWarnings("unchecked")
      List<ExperimentRunEntity> experimentRunEntities = query.list();
      for (ExperimentRunEntity experimentRunEntity : experimentRunEntities) {
        projectIds.add(experimentRunEntity.getProject_id());
        experimentIds.add(experimentRunEntity.getExperiment_id());
        session.delete(experimentRunEntity);
      }

      // Update parent entity timestamp
      updateParentEntitiesTimestamp(
          session, projectIds, experimentIds, Calendar.getInstance().getTimeInMillis());
      transaction.commit();
      LOGGER.debug("ExperimentRun deleted successfully");
      return true;
    }
  }

  private void removeEntityComments(Session session, List<String> entityIds, String entityName) {
    Query commentDeleteQuery = session.createQuery(COMMENT_DELETE_HQL);
    commentDeleteQuery.setParameterList("entityIds", entityIds);
    commentDeleteQuery.setParameter("entityName", entityName);
    LOGGER.debug("Comments delete query : {}", commentDeleteQuery.getQueryString());
    List<CommentEntity> commentEntities = commentDeleteQuery.list();
    for (CommentEntity commentEntity : commentEntities) {
      session.delete(commentEntity);
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
    return findExperimentRuns(findExperimentRuns);
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
    }
  }

  @Override
  public ExperimentRun getExperimentRun(String experimentRunId)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunEntity =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunEntity == null) {
        LOGGER.warn(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      LOGGER.debug("Got ExperimentRun successfully");
      return experimentRunEntity.getProtoObject();
    }
  }

  @Override
  public boolean isExperimentRunExists(Session session, String experimentRunId) {
    ExperimentRunEntity experimentRunEntity =
        session.get(ExperimentRunEntity.class, experimentRunId);
    return experimentRunEntity != null;
  }

  @Override
  public ExperimentRun updateExperimentRunName(String experimentRunId, String experimentRunName)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ExperimentRunEntity experimentRunEntity =
          session.load(ExperimentRunEntity.class, experimentRunId);
      experimentRunEntity.setName(experimentRunName);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntity.setDate_updated(currentTimestamp);
      session.update(experimentRunEntity);
      LOGGER.debug("ExperimentRun name updated successfully");
      // Update parent entity timestamp
      updateParentEntitiesTimestamp(
          session,
          Collections.singletonList(experimentRunEntity.getProject_id()),
          Collections.singletonList(experimentRunEntity.getExperiment_id()),
          currentTimestamp);
      transaction.commit();
      return experimentRunEntity.getProtoObject();
    }
  }

  @Override
  public ExperimentRun updateExperimentRunDescription(
      String experimentRunId, String experimentRunDescription)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ExperimentRunEntity experimentRunEntity =
          session.load(ExperimentRunEntity.class, experimentRunId);
      experimentRunEntity.setDescription(experimentRunDescription);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntity.setDate_updated(currentTimestamp);
      session.update(experimentRunEntity);
      LOGGER.debug("ExperimentRun description updated successfully");
      // Update parent entity timestamp
      updateParentEntitiesTimestamp(
          session,
          Collections.singletonList(experimentRunEntity.getProject_id()),
          Collections.singletonList(experimentRunEntity.getExperiment_id()),
          currentTimestamp);
      transaction.commit();
      return experimentRunEntity.getProtoObject();
    }
  }

  @Override
  public ExperimentRun logExperimentRunCodeVersion(
      String experimentRunId, CodeVersion updatedCodeVersion)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
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
      session.update(experimentRunEntity);
      LOGGER.debug("ExperimentRun code version snapshot updated successfully");

      // Update parent entity timestamp
      updateParentEntitiesTimestamp(
          session,
          Collections.singletonList(experimentRunEntity.getProject_id()),
          Collections.singletonList(experimentRunEntity.getExperiment_id()),
          currentTimestamp);
      transaction.commit();
      return experimentRunEntity.getProtoObject();
    }
  }

  @Override
  public ExperimentRun setParentExperimentRunId(
      String experimentRunId, String parentExperimentRunId) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ExperimentRunEntity experimentRunEntity =
          session.load(ExperimentRunEntity.class, experimentRunId);
      experimentRunEntity.setParent_id(parentExperimentRunId);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntity.setDate_updated(currentTimestamp);
      session.update(experimentRunEntity);
      LOGGER.debug("ExperimentRun parentId updated successfully");
      // Update parent entity timestamp
      updateParentEntitiesTimestamp(
          session,
          Collections.singletonList(experimentRunEntity.getProject_id()),
          Collections.singletonList(experimentRunEntity.getExperiment_id()),
          currentTimestamp);
      transaction.commit();
      return experimentRunEntity.getProtoObject();
    }
  }

  @Override
  public ExperimentRun addExperimentRunTags(String experimentRunId, List<String> tagsList)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunObj == null) {
        LOGGER.warn(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
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
        session.saveOrUpdate(experimentRunObj);
      }
      // Update parent entity timestamp
      updateParentEntitiesTimestamp(
          session,
          Collections.singletonList(experimentRunObj.getProject_id()),
          Collections.singletonList(experimentRunObj.getExperiment_id()),
          currentTimestamp);
      transaction.commit();
      LOGGER.debug("ExperimentRun tags added successfully");
      return experimentRunObj.getProtoObject();
    }
  }

  @Override
  public ExperimentRun deleteExperimentRunTags(
      String experimentRunId, List<String> experimentRunTagList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
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
      // Update parent entity timestamp
      updateParentEntitiesTimestamp(
          session,
          Collections.singletonList(experimentRunObj.getProject_id()),
          Collections.singletonList(experimentRunObj.getExperiment_id()),
          currentTimestamp);
      transaction.commit();
      LOGGER.debug("ExperimentRun tags deleted successfully");
      return experimentRunObj.getProtoObject();
    }
  }

  @Override
  public ExperimentRun logObservations(String experimentRunId, List<Observation> observations)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunEntityObj == null) {
        LOGGER.warn(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      List<ObservationEntity> newObservationList =
          RdbmsUtils.convertObservationsFromObservationEntityList(
              experimentRunEntityObj, ModelDBConstants.OBSERVATIONS, observations);
      experimentRunEntityObj.setObservationMapping(newObservationList);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntityObj.setDate_updated(currentTimestamp);
      session.saveOrUpdate(experimentRunEntityObj);
      // Update parent entity timestamp
      updateParentEntitiesTimestamp(
          session,
          Collections.singletonList(experimentRunEntityObj.getProject_id()),
          Collections.singletonList(experimentRunEntityObj.getExperiment_id()),
          currentTimestamp);
      transaction.commit();
      return experimentRunEntityObj.getProtoObject();
    }
  }

  @Override
  public List<Observation> getObservationByKey(String experimentRunId, String observationKey)
      throws InvalidProtocolBufferException {

    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunEntityObj == null) {
        LOGGER.warn(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
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
    }
  }

  @Override
  public ExperimentRun logMetrics(String experimentRunId, List<KeyValue> newMetrics)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunEntityObj == null) {
        LOGGER.warn(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
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
      session.saveOrUpdate(experimentRunEntityObj);
      // Update parent entity timestamp
      updateParentEntitiesTimestamp(
          session,
          Collections.singletonList(experimentRunEntityObj.getProject_id()),
          Collections.singletonList(experimentRunEntityObj.getExperiment_id()),
          currentTimestamp);
      transaction.commit();
      return experimentRunEntityObj.getProtoObject();
    }
  }

  @Override
  public List<KeyValue> getExperimentRunMetrics(String experimentRunId)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunObj == null) {
        LOGGER.warn(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      LOGGER.debug("Got ExperimentRun Metrics");
      return experimentRunObj.getProtoObject().getMetricsList();
    }
  }

  @Override
  public List<Artifact> getExperimentRunDatasets(String experimentRunId)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunObj == null) {
        LOGGER.warn(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      LOGGER.debug("Got ExperimentRun Datasets");
      return experimentRunObj.getProtoObject().getDatasetsList();
    }
  }

  @Override
  public ExperimentRun logDatasets(
      String experimentRunId, List<Artifact> newDatasets, boolean overwrite)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunEntityObj == null) {
        LOGGER.warn(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      ExperimentRun experimentRun = experimentRunEntityObj.getProtoObject();

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

      List<ArtifactEntity> newDatasetList =
          RdbmsUtils.convertArtifactsFromArtifactEntityList(
              experimentRunEntityObj, ModelDBConstants.DATASETS, newDatasets);
      experimentRunEntityObj.setArtifactMapping(newDatasetList);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntityObj.setDate_updated(currentTimestamp);
      session.saveOrUpdate(experimentRunEntityObj);
      // Update parent entity timestamp
      updateParentEntitiesTimestamp(
          session,
          Collections.singletonList(experimentRunEntityObj.getProject_id()),
          Collections.singletonList(experimentRunEntityObj.getExperiment_id()),
          currentTimestamp);
      transaction.commit();
    }
    return getExperimentRun(experimentRunId);
  }

  @Override
  public ExperimentRun logArtifacts(String experimentRunId, List<Artifact> newArtifacts)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunEntityObj == null) {
        LOGGER.warn(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
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

      List<ArtifactEntity> newArtifactList =
          RdbmsUtils.convertArtifactsFromArtifactEntityList(
              experimentRunEntityObj, ModelDBConstants.ARTIFACTS, newArtifacts);
      experimentRunEntityObj.setArtifactMapping(newArtifactList);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntityObj.setDate_updated(currentTimestamp);
      session.saveOrUpdate(experimentRunEntityObj);
      // Update parent entity timestamp
      updateParentEntitiesTimestamp(
          session,
          Collections.singletonList(experimentRunEntityObj.getProject_id()),
          Collections.singletonList(experimentRunEntityObj.getExperiment_id()),
          currentTimestamp);
      transaction.commit();
      return experimentRunEntityObj.getProtoObject();
    }
  }

  @Override
  public List<Artifact> getExperimentRunArtifacts(String experimentRunId)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunObj == null) {
        LOGGER.warn(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
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
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
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
  public ExperimentRun deleteArtifacts(String experimentRunId, String artifactKey)
      throws InvalidProtocolBufferException {
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

      // Update parent entity timestamp
      updateParentEntitiesTimestamp(
          session,
          Collections.singletonList(experimentRunObj.getProject_id()),
          Collections.singletonList(experimentRunObj.getExperiment_id()),
          currentTimestamp);
      transaction.commit();
      return experimentRunObj.getProtoObject();
    } catch (StatusRuntimeException ex) {
      if (transaction != null) {
        transaction.rollback();
      }
      throw ex;
    }
  }

  @Override
  public ExperimentRun logHyperparameters(String experimentRunId, List<KeyValue> newHyperparameters)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunEntityObj == null) {
        LOGGER.warn(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
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
      session.saveOrUpdate(experimentRunEntityObj);
      // Update parent entity timestamp
      updateParentEntitiesTimestamp(
          session,
          Collections.singletonList(experimentRunEntityObj.getProject_id()),
          Collections.singletonList(experimentRunEntityObj.getExperiment_id()),
          currentTimestamp);
      transaction.commit();
      return experimentRunEntityObj.getProtoObject();
    }
  }

  @Override
  public List<KeyValue> getExperimentRunHyperparameters(String experimentRunId)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunObj == null) {
        LOGGER.warn(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG)
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      LOGGER.debug("Got ExperimentRun Hyperparameters");
      return experimentRunObj.getProtoObject().getHyperparametersList();
    }
  }

  @Override
  public ExperimentRun logAttributes(String experimentRunId, List<KeyValue> newAttributes)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      if (experimentRunEntityObj == null) {
        LOGGER.warn(ModelDBMessages.EXP_RUN_NOT_FOUND_ERROR_MSG);
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
      session.saveOrUpdate(experimentRunEntityObj);
      // Update parent entity timestamp
      updateParentEntitiesTimestamp(
          session,
          Collections.singletonList(experimentRunEntityObj.getProject_id()),
          Collections.singletonList(experimentRunEntityObj.getExperiment_id()),
          currentTimestamp);
      transaction.commit();
      return experimentRunEntityObj.getProtoObject();
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
        LOGGER.warn(errorMessage);
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
    }
  }

  @Override
  public ExperimentRunPaginationDTO findExperimentRuns(FindExperimentRuns queryParameters)
      throws InvalidProtocolBufferException {

    LOGGER.trace("trying to open session");
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      LOGGER.trace("Starting to find experiements");

      CriteriaBuilder builder = session.getCriteriaBuilder();
      // Using FROM and JOIN
      CriteriaQuery<ExperimentRunEntity> criteriaQuery =
          builder.createQuery(ExperimentRunEntity.class);
      Root<ExperimentRunEntity> experimentRunRoot = criteriaQuery.from(ExperimentRunEntity.class);
      experimentRunRoot.alias("exp");
      List<Predicate> finalPredicatesList = new ArrayList<>();

      if (!queryParameters.getProjectId().isEmpty()) {
        Expression<String> exp = experimentRunRoot.get(ModelDBConstants.PROJECT_ID);
        Predicate predicate2 = builder.equal(exp, queryParameters.getProjectId());
        finalPredicatesList.add(predicate2);
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
      List<KeyValueQuery> predicates = queryParameters.getPredicatesList();
      String entityName = "experimentRunEntity";
      List<Predicate> queryPredicatesList =
          RdbmsUtils.getQueryPredicatesFromPredicateList(
              entityName, predicates, builder, criteriaQuery, experimentRunRoot);
      if (!queryPredicatesList.isEmpty()) {
        finalPredicatesList.addAll(queryPredicatesList);
      }

      Order orderBy =
          RdbmsUtils.getOrderBasedOnSortKey(
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
      List<ExperimentRun> experimentRunList = new ArrayList<>();
      List<ExperimentRunEntity> experimentRunEntities = query.list();
      LOGGER.debug("Final experimentRuns list size : {}", experimentRunEntities.size());
      if (!experimentRunEntities.isEmpty()) {

        LOGGER.trace("Converting from Hibernate to proto");
        experimentRunList =
            RdbmsUtils.convertExperimentRunsFromExperimentRunEntityList(experimentRunEntities);
        LOGGER.trace("experimentRunList {}", experimentRunList);
        LOGGER.trace("Converted from Hibernate to proto");
      }

      Set<String> experimentRunIdsSet = new HashSet<>();
      List<ExperimentRun> experimentRuns = new ArrayList<>();
      for (ExperimentRun experimentRun : experimentRunList) {
        if (!experimentRunIdsSet.contains(experimentRun.getId())) {
          experimentRunIdsSet.add(experimentRun.getId());
          if (queryParameters.getIdsOnly()) {
            experimentRun = ExperimentRun.newBuilder().setId(experimentRun.getId()).build();
            experimentRuns.add(experimentRun);
          } else {
            experimentRuns.add(experimentRun);
          }
        }
      }

      long totalRecords = RdbmsUtils.count(session, experimentRunRoot, criteriaQuery);
      LOGGER.debug("ExperimentRuns Total record count : {}", totalRecords);

      ExperimentRunPaginationDTO experimentRunPaginationDTO = new ExperimentRunPaginationDTO();
      experimentRunPaginationDTO.setExperimentRuns(experimentRuns);
      experimentRunPaginationDTO.setTotalRecords(totalRecords);
      return experimentRunPaginationDTO;
    }
  }

  @Override
  public ExperimentRunPaginationDTO sortExperimentRuns(SortExperimentRuns queryParameters)
      throws InvalidProtocolBufferException {
    FindExperimentRuns findExperimentRuns =
        FindExperimentRuns.newBuilder()
            .addAllExperimentRunIds(queryParameters.getExperimentRunIdsList())
            .setSortKey(queryParameters.getSortKey())
            .setAscending(queryParameters.getAscending())
            .setIdsOnly(queryParameters.getIdsOnly())
            .build();
    return findExperimentRuns(findExperimentRuns);
  }

  @Override
  public List<ExperimentRun> getTopExperimentRuns(TopExperimentRunsSelector queryParameters)
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
    return findExperimentRuns(findExperimentRuns).getExperimentRuns();
  }

  @Override
  public List<String> getExperimentRunTags(String experimentRunId)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      LOGGER.debug("Got ExperimentRun Tags");
      return experimentRunObj.getProtoObject().getTagsList();
    }
  }

  @Override
  public ExperimentRun addExperimentRunAttributes(
      String experimentRunId, List<KeyValue> attributesList) throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      List<AttributeEntity> newAttributeList =
          RdbmsUtils.convertAttributesFromAttributeEntityList(
              experimentRunEntityObj, ModelDBConstants.ATTRIBUTES, attributesList);
      experimentRunEntityObj.setAttributeMapping(newAttributeList);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntityObj.setDate_updated(currentTimestamp);
      session.saveOrUpdate(experimentRunEntityObj);
      // Update parent entity timestamp
      updateParentEntitiesTimestamp(
          session,
          Collections.singletonList(experimentRunEntityObj.getProject_id()),
          Collections.singletonList(experimentRunEntityObj.getExperiment_id()),
          currentTimestamp);
      transaction.commit();
      return experimentRunEntityObj.getProtoObject();
    }
  }

  @Override
  public ExperimentRun deleteExperimentRunAttributes(
      String experimentRunId, List<String> attributeKeyList, Boolean deleteAll)
      throws InvalidProtocolBufferException {
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
      // Update parent entity timestamp
      updateParentEntitiesTimestamp(
          session,
          Collections.singletonList(experimentRunObj.getProject_id()),
          Collections.singletonList(experimentRunObj.getExperiment_id()),
          currentTimestamp);
      transaction.commit();
      LOGGER.debug("ExperimentRun Attributes deleted successfully");
      return experimentRunObj.getProtoObject();
    }
  }

  @Override
  public ExperimentRun logJobId(String experimentRunId, String jobId)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      experimentRunEntityObj.setJob_id(jobId);
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      experimentRunEntityObj.setDate_updated(currentTimestamp);
      session.saveOrUpdate(experimentRunEntityObj);
      // Update parent entity timestamp
      updateParentEntitiesTimestamp(
          session,
          Collections.singletonList(experimentRunEntityObj.getProject_id()),
          Collections.singletonList(experimentRunEntityObj.getExperiment_id()),
          currentTimestamp);
      transaction.commit();
      LOGGER.debug("ExperimentRun JobID added successfully");
      return experimentRunEntityObj.getProtoObject();
    }
  }

  @Override
  public String getJobId(String experimentRunId) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunEntityObj =
          session.get(ExperimentRunEntity.class, experimentRunId);
      LOGGER.debug("Got ExperimentRun JobID");
      return experimentRunEntityObj.getJob_id();
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
  public ExperimentRun deepCopyExperimentRunForUser(
      ExperimentRun srcExperimentRun,
      Experiment newExperiment,
      Project newProject,
      UserInfo newOwner)
      throws InvalidProtocolBufferException {
    checkIfEntityAlreadyExists(srcExperimentRun, false);

    if (newExperiment == null || newProject == null || newOwner == null) {
      Status status =
          Status.newBuilder()
              .setCode(Code.INVALID_ARGUMENT_VALUE)
              .setMessage(
                  "New owner, new project or new Experiment not passed for cloning ExperimentRun.")
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
    ExperimentRun copyExperimentRun =
        copyExperimentRunAndUpdateDetails(srcExperimentRun, newExperiment, newProject, newOwner);

    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      ExperimentRunEntity experimentRunObj =
          RdbmsUtils.generateExperimentRunEntity(copyExperimentRun);
      session.saveOrUpdate(experimentRunObj);
      transaction.commit();
      LOGGER.debug("ExperimentRun copied successfully");
      return experimentRunObj.getProtoObject();
    }
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
      Query query = session.createQuery(stringQueryBuilder.toString());
      for (Map.Entry<String, Object> paramEntry : paramMap.entrySet()) {
        query.setParameter(paramEntry.getKey(), paramEntry.getValue());
      }
      List<ExperimentRunEntity> experimentRunObjList = query.list();
      return RdbmsUtils.convertExperimentRunsFromExperimentRunEntityList(experimentRunObjList);
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
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
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
    }
  }

  @Override
  public LogVersionedInput.Response logVersionedInput(LogVersionedInput request)
      throws InvalidProtocolBufferException, ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      VersioningEntry versioningEntry = request.getVersionedInputs();
      validateVersioningEntity(session, versioningEntry);
      ExperimentRunEntity runEntity = session.get(ExperimentRunEntity.class, request.getId());
      runEntity.setVersioningModeldbEntityMappings(
          RdbmsUtils.getVersioningMappingFromVersioningInput(versioningEntry, runEntity));
      long currentTimestamp = Calendar.getInstance().getTimeInMillis();
      runEntity.setDate_updated(currentTimestamp);
      session.saveOrUpdate(runEntity);
      // Update parent entity timestamp
      updateParentEntitiesTimestamp(
          session,
          Collections.singletonList(runEntity.getProject_id()),
          Collections.singletonList(runEntity.getExperiment_id()),
          currentTimestamp);
      transaction.commit();
      LOGGER.debug("ExperimentRun versioning added successfully");
      return LogVersionedInput.Response.newBuilder()
          .setExperimentRun(runEntity.getProtoObject())
          .build();
    }
  }

  @Override
  public GetVersionedInput.Response getVersionedInputs(GetVersionedInput request)
      throws InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      ExperimentRunEntity experimentRunObj =
          session.get(ExperimentRunEntity.class, request.getId());
      if (experimentRunObj != null) {
        LOGGER.debug("ExperimentRun versioning fetch successfully");
        return GetVersionedInput.Response.newBuilder()
            .setVersionedInputs(
                RdbmsUtils.getVersioningEntryFromList(
                    experimentRunObj.getVersioningModeldbEntityMappings()))
            .build();
      } else {
        String errorMessage = "ExperimentRun not found for given ID : " + request.getId();
        LOGGER.warn(errorMessage);
        Status status =
            Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    }
  }
}
