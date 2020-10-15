package ai.verta.modeldb.lineage;

import static ai.verta.modeldb.entities.lineage.ConnectionEntity.ConnectionType.CONNECTION_TYPE_ANY;
import static ai.verta.modeldb.entities.lineage.ConnectionEntity.ConnectionType.CONNECTION_TYPE_INPUT;
import static ai.verta.modeldb.entities.lineage.ConnectionEntity.ConnectionType.CONNECTION_TYPE_OUTPUT;
import static ai.verta.modeldb.entities.lineage.ConnectionEntity.ENTITY_TYPE_EXPERIMENT_RUN;
import static ai.verta.modeldb.entities.lineage.ConnectionEntity.ENTITY_TYPE_VERSIONING_BLOB;
import static ai.verta.modeldb.entities.lineage.LineageVersioningBlobEntity.toLocationString;

import ai.verta.modeldb.AddLineage;
import ai.verta.modeldb.AddLineage.Response;
import ai.verta.modeldb.DeleteLineage;
import ai.verta.modeldb.FindAllInputs;
import ai.verta.modeldb.FindAllInputsOutputs;
import ai.verta.modeldb.FindAllOutputs;
import ai.verta.modeldb.LineageEntry;
import ai.verta.modeldb.LineageEntryBatchRequest;
import ai.verta.modeldb.LineageEntryBatchResponse;
import ai.verta.modeldb.LineageEntryBatchResponseSingle;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.VersioningLineageEntry;
import ai.verta.modeldb.entities.lineage.ConnectionEntity;
import ai.verta.modeldb.entities.lineage.ConnectionEntity.ConnectionType;
import ai.verta.modeldb.entities.lineage.LineageElementEntity;
import ai.verta.modeldb.entities.lineage.LineageExperimentRunEntity;
import ai.verta.modeldb.entities.lineage.LineageVersioningBlobEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.blob.diff.Function3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ProtocolStringList;
import io.grpc.Status.Code;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class LineageDAORdbImpl implements LineageDAO {

  private static final Logger LOGGER = LogManager.getLogger(LineageDAORdbImpl.class);

  public LineageDAORdbImpl() {}

  @Override
  public Response addLineage(
      AddLineage addLineage, ResourceExistsCheckConsumer resourceExistsCheckConsumer)
      throws ModelDBException, InvalidProtocolBufferException, NoSuchAlgorithmException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      Long id;
      LineageElementEntity lineageElementEntity;
      if (addLineage.getId() != 0) {
        id = addLineage.getId();
        lineageElementEntity = session.get(LineageElementEntity.class, id);
        if (lineageElementEntity == null) {
          throw new ModelDBException("Can't find a lineage with the specified id", Code.NOT_FOUND);
        }
      } else {
        id = null;
        lineageElementEntity = null;
      }
      if (lineageElementEntity == null) {
        lineageElementEntity = new LineageElementEntity(id);
        session.save(lineageElementEntity);
        id = lineageElementEntity.getId();
      }
      validate(addLineage.getInputList(), addLineage.getOutputList());
      List<LineageEntry> lineageEntries = new LinkedList<>(addLineage.getInputList());
      lineageEntries.addAll(addLineage.getOutputList());
      resourceExistsCheckConsumer.accept(session, lineageEntries);
      for (LineageEntry input : addLineage.getInputList()) {
        addLineage(session, input, id, CONNECTION_TYPE_INPUT);
      }
      for (LineageEntry output : addLineage.getOutputList()) {
        addLineage(session, output, id, CONNECTION_TYPE_OUTPUT);
      }
      session.getTransaction().commit();
      return AddLineage.Response.newBuilder().setId(lineageElementEntity.getId()).build();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return addLineage(addLineage, resourceExistsCheckConsumer);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public DeleteLineage.Response deleteLineage(
      DeleteLineage deleteLineage, ResourceExistsCheckConsumer resourceExistsCheckConsumer)
      throws ModelDBException, InvalidProtocolBufferException, NoSuchAlgorithmException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      validate(deleteLineage.getInputList(), deleteLineage.getOutputList());
      long id = deleteLineage.getId();
      if (id == 0) {
        throw new ModelDBException("Id not specified", Code.INVALID_ARGUMENT);
      }
      List<ConnectionEntity> connectionEntitiesById = getConnectionEntitiesById(session, id);
      Map.Entry<
              Map<LineageEntryContainer, ConnectionEntity>,
              Map<LineageEntryContainer, ConnectionEntity>>
          inputOutputs = getInputOutputs(session, connectionEntitiesById);
      Map<LineageEntryContainer, ConnectionEntity> inputInDatabase = inputOutputs.getKey();
      Map<LineageEntryContainer, ConnectionEntity> outputInDatabase = inputOutputs.getValue();
      /* noDataProvided is a flag that there are no entries provided in the request.
      That means that we should delete all entries with the specified id.*/
      boolean noDataProvided =
          deleteLineage.getInputCount() == 0 && deleteLineage.getOutputCount() == 0;
      List<LineageEntry> lineageEntries = new LinkedList<>(deleteLineage.getInputList());
      lineageEntries.addAll(deleteLineage.getOutputList());
      if (noDataProvided) {
        lineageEntries.addAll(
            inputInDatabase.keySet().stream()
                .map(LineageEntryContainer::toProto)
                .collect(Collectors.toList()));
        lineageEntries.addAll(
            outputInDatabase.keySet().stream()
                .map(LineageEntryContainer::toProto)
                .collect(Collectors.toList()));
      }
      resourceExistsCheckConsumer.accept(session, lineageEntries);
      session.beginTransaction();
      for (LineageEntry input : deleteLineage.getInputList()) {
        deleteLineage(session, input, inputInDatabase);
      }
      for (LineageEntry output : deleteLineage.getOutputList()) {
        deleteLineage(session, output, outputInDatabase);
      }
      if (noDataProvided) {
        Set<ConnectionEntity> values = new HashSet<>(inputInDatabase.values());
        values.addAll(outputInDatabase.values());
        values.forEach(connectionEntity -> deleteConnectionEntity(session, connectionEntity));
      }
      if (noDataProvided || inputInDatabase.size() == 0 && outputInDatabase.size() == 0) {
        LineageElementEntity lineageElementEntity = session.get(LineageElementEntity.class, id);
        if (lineageElementEntity != null) {
          session.remove(lineageElementEntity);
        }
      }
      session.getTransaction().commit();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteLineage(deleteLineage, resourceExistsCheckConsumer);
      } else {
        throw ex;
      }
    }
    return DeleteLineage.Response.newBuilder().setStatus(true).build();
  }

  private Entry<
          Map<LineageEntryContainer, ConnectionEntity>,
          Map<LineageEntryContainer, ConnectionEntity>>
      getInputOutputs(Session session, List<ConnectionEntity> result) {
    Map<LineageEntryContainer, ConnectionEntity> inputInDatabase = new HashMap<>();
    Map<LineageEntryContainer, ConnectionEntity> outputInDatabase = new HashMap<>();
    for (ConnectionEntity connectionEntity : result) {
      if (connectionEntity.getConnectionType() == CONNECTION_TYPE_INPUT) {
        inputInDatabase.put(connectionEntity.getLineageElement(session), connectionEntity);
      } else {
        outputInDatabase.put(connectionEntity.getLineageElement(session), connectionEntity);
      }
    }
    return new AbstractMap.SimpleEntry<>(inputInDatabase, outputInDatabase);
  }

  private Map<LineageEntryContainer, ConnectionEntity>
      getLineageEntryContainerToConnectionEntityMap(
          Session session, List<ConnectionEntity> result) {
    return result.stream()
        .collect(
            Collectors.toMap(
                connectionEntity -> connectionEntity.getLineageElement(session),
                connectionEntity -> connectionEntity));
  }

  private List<ConnectionEntity> getConnectionEntitiesById(Session session, long id) {
    return getConnectionEntitiesByIdAndConnectionType(session, id, CONNECTION_TYPE_ANY);
  }

  private List<ConnectionEntity> getConnectionEntitiesByIdAndConnectionType(
      Session session, long id, ConnectionType connectionType) {
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<ConnectionEntity> criteriaQuery = builder.createQuery(ConnectionEntity.class);
    Root<ConnectionEntity> root = criteriaQuery.from(ConnectionEntity.class);
    final Predicate idPredicate = root.get(ConnectionEntity.ID).in(id);
    final Predicate finalPredicate;
    if (connectionType != CONNECTION_TYPE_ANY) {
      finalPredicate =
          builder.and(
              idPredicate,
              root.get(ConnectionEntity.CONNECTION_TYPE).in(connectionType.getValue()));
    } else {
      finalPredicate = idPredicate;
    }
    criteriaQuery.select(root);
    criteriaQuery.where(finalPredicate);
    Query<ConnectionEntity> query = session.createQuery(criteriaQuery);
    return query.list();
  }

  private List<ConnectionEntity> getConnectionEntities(
      Session session, ConnectionType connectionType, Long entityId, int entityType) {
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<ConnectionEntity> criteriaQuery = builder.createQuery(ConnectionEntity.class);
    Root<ConnectionEntity> root = criteriaQuery.from(ConnectionEntity.class);
    final Predicate entityIdPredicate = root.get(ConnectionEntity.ENTITY_ID).in(entityId);
    final Predicate entityTypePredicate = root.get(ConnectionEntity.ENTITY_TYPE).in(entityType);
    final Predicate combinedPredicate = builder.and(entityIdPredicate, entityTypePredicate);
    final Predicate finalPredicate;
    if (connectionType != CONNECTION_TYPE_ANY) {
      finalPredicate =
          builder.and(
              combinedPredicate,
              root.get(ConnectionEntity.CONNECTION_TYPE).in(connectionType.getValue()));
    } else {
      finalPredicate = combinedPredicate;
    }
    criteriaQuery.select(root);
    criteriaQuery.where(finalPredicate);
    Query<ConnectionEntity> query = session.createQuery(criteriaQuery);
    return query.list();
  }

  /**
   * Get id -> entries of input or output. A -- what we want to receive (can be input or output). B
   * -- what we already have (input or output).
   *
   * @param session current session
   * @param lineageEntry sideB
   * @param connectionType input or output
   * @return id -> entries
   * @throws ModelDBException unexpected error
   * @throws InvalidProtocolBufferException unexpected error
   */
  private Map<Long, List<LineageEntry>> getSideAEntries(
      Session session, LineageEntry lineageEntry, ConnectionEntity.ConnectionType connectionType)
      throws ModelDBException, InvalidProtocolBufferException {
    int entityType;
    Long entityId;
    Map<Long, List<LineageEntry>> result = new HashMap<>();
    switch (lineageEntry.getType()) {
      case EXPERIMENT_RUN:
        LineageExperimentRunEntity lineageExperimentRunEntity =
            getLineageExperimentRunEntity(session, lineageEntry);
        if (lineageExperimentRunEntity == null) {
          return result;
        }
        entityId = lineageExperimentRunEntity.getId();
        entityType = ENTITY_TYPE_EXPERIMENT_RUN;
        break;
      case BLOB:
        LineageVersioningBlobEntity lineageVersioningBlobEntity;
        VersioningLineageEntry blob = lineageEntry.getBlob();
        lineageVersioningBlobEntity =
            getLineageVersioningBlobEntity(
                session, blob.getRepositoryId(), blob.getCommitSha(), blob.getLocationList());
        if (lineageVersioningBlobEntity == null) {
          return result;
        }
        entityId = lineageVersioningBlobEntity.getId();
        entityType = ENTITY_TYPE_VERSIONING_BLOB;
        break;
      default:
        throw new ModelDBException("Unknown entry type");
    }
    List<ConnectionEntity> connectionEntities =
        getConnectionEntities(session, invert(connectionType), entityId, entityType);
    for (ConnectionEntity connectionEntity : connectionEntities) {
      List<ConnectionEntity> connectionEntitiesById =
          getConnectionEntitiesByIdAndConnectionType(
              session, connectionEntity.getId(), connectionType);
      Map<LineageEntryContainer, ConnectionEntity> inputOrOutputInDatabase =
          getLineageEntryContainerToConnectionEntityMap(session, connectionEntitiesById);
      result.put(
          connectionEntity.getId(),
          inputOrOutputInDatabase.keySet().stream()
              .map(LineageEntryContainer::toProto)
              .collect(Collectors.toList()));
    }
    return result;
  }

  private LineageVersioningBlobEntity getLineageVersioningBlobEntity(
      Session session, long repositoryId, String commitSha, ProtocolStringList location)
      throws InvalidProtocolBufferException {
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<LineageVersioningBlobEntity> criteriaQuery =
        builder.createQuery(LineageVersioningBlobEntity.class);
    Root<LineageVersioningBlobEntity> root = criteriaQuery.from(LineageVersioningBlobEntity.class);
    final Predicate repositoryIdPredicate = root.get("repositoryId").in(repositoryId);
    final Predicate commitShaPredicate = root.get("commitSha").in(commitSha);
    final Predicate locationPredicate = root.get("location").in(toLocationString(location));
    final Predicate finalPredicate =
        builder.and(repositoryIdPredicate, commitShaPredicate, locationPredicate);
    criteriaQuery.select(root);
    criteriaQuery.where(finalPredicate);
    Query<LineageVersioningBlobEntity> query = session.createQuery(criteriaQuery);
    return query.uniqueResult();
  }

  private LineageExperimentRunEntity getLineageExperimentRunEntity(
      Session session, LineageEntry lineageEntry) {
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<LineageExperimentRunEntity> criteriaQuery =
        builder.createQuery(LineageExperimentRunEntity.class);
    Root<LineageExperimentRunEntity> root = criteriaQuery.from(LineageExperimentRunEntity.class);
    final Predicate experimentRunIdPredicate =
        root.get("experimentRunId").in(lineageEntry.getExternalId());
    criteriaQuery.select(root);
    criteriaQuery.where(experimentRunIdPredicate);
    Query<LineageExperimentRunEntity> query = session.createQuery(criteriaQuery);
    return query.uniqueResult();
  }

  private ConnectionEntity.ConnectionType invert(ConnectionEntity.ConnectionType connectionType)
      throws ModelDBException {
    switch (connectionType) {
      case CONNECTION_TYPE_INPUT:
        return CONNECTION_TYPE_OUTPUT;
      case CONNECTION_TYPE_OUTPUT:
        return CONNECTION_TYPE_INPUT;
      default:
        throw new ModelDBException("Unknown connection type");
    }
  }

  private void validate(List<LineageEntry> inputList, List<LineageEntry> outputList)
      throws ModelDBException {
    validate(inputList);
    validate(outputList);
  }

  private void validate(List<LineageEntry> list) throws ModelDBException {
    Set<LineageEntry> ids = new HashSet<>();
    for (LineageEntry input : list) {
      ids.add(input);
      validate(input);
    }
    if (ids.size() != list.size()) {
      throw new ModelDBException("Non-unique resource ids in a requests", Code.INVALID_ARGUMENT);
    }
  }

  private void validate(LineageEntryBatchRequest lineageEntryBatchRequest) throws ModelDBException {
    final String message;
    switch (lineageEntryBatchRequest.getIdentifierCase()) {
      case ENTRY:
        LineageEntry lineageEntry = lineageEntryBatchRequest.getEntry();
        validate(lineageEntry);
        message = null;
        break;
      case ID:
        message = null;
        break;
      default:
        message = "Unknown request type";
    }
    if (message != null) {
      LOGGER.info(message);
      throw new ModelDBException(message, Code.INVALID_ARGUMENT);
    }
  }

  private void validate(LineageEntry lineageEntry) throws ModelDBException {
    final String message;
    switch (lineageEntry.getType()) {
      case BLOB:
        VersioningLineageEntry blob = lineageEntry.getBlob();
        if (blob.getCommitSha().isEmpty()) {
          message = "Commit sha is empty";
        } else if (blob.getLocationCount() == 0) {
          message = "Location is empty";
        } else {
          message = null;
        }
        break;
      case EXPERIMENT_RUN:
        if (lineageEntry.getExternalId().isEmpty()) {
          message = "Experiment run id is empty";
        } else {
          message = null;
        }
        break;
      case DATASET_VERSION:
        if (lineageEntry.getExternalId().isEmpty()) {
          message = "Dataset version id is empty";
        } else {
          message = null;
        }
        break;
      default:
        message = "Unknown lineage type";
    }
    if (message != null) {
      LOGGER.warn(message);
      throw new ModelDBException(message, Code.INVALID_ARGUMENT);
    }
  }

  @Override
  public FindAllInputs.Response findAllInputs(
      FindAllInputs findAllInputs,
      ResourceExistsCheckConsumer resourceExistsCheckConsumer,
      Function3<Session, FindAllInputs.Response, FindAllInputs.Response> filter)
      throws ModelDBException, InvalidProtocolBufferException, NoSuchAlgorithmException {
    FindAllInputs.Response.Builder response = FindAllInputs.Response.newBuilder();
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      List<LineageEntryBatchRequest> itemList = findAllInputs.getItemsList();
      List<LineageEntry> lineageEntries = new LinkedList<>();
      for (LineageEntryBatchRequest output : itemList) {
        validate(output);
        lineageEntries.add(output.getEntry());
        response.addInputs(
            LineageEntryBatchResponse.newBuilder()
                .addAllItems(getInputsByOutput(session, output))
                .build());
      }
      resourceExistsCheckConsumer.accept(session, lineageEntries);
      return filter.apply(session, response.build());
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return findAllInputs(findAllInputs, resourceExistsCheckConsumer, filter);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public FindAllOutputs.Response findAllOutputs(
      FindAllOutputs findAllOutputs,
      ResourceExistsCheckConsumer resourceExistsCheckConsumer,
      Function3<Session, FindAllOutputs.Response, FindAllOutputs.Response> filter)
      throws ModelDBException, InvalidProtocolBufferException, NoSuchAlgorithmException {
    FindAllOutputs.Response.Builder response = FindAllOutputs.Response.newBuilder();
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      List<LineageEntryBatchRequest> itemList = findAllOutputs.getItemsList();
      List<LineageEntry> lineageEntries = new LinkedList<>();
      for (LineageEntryBatchRequest input : itemList) {
        validate(input);
        lineageEntries.add(input.getEntry());
        response.addOutputs(
            LineageEntryBatchResponse.newBuilder()
                .addAllItems(getOutputsByInput(session, input))
                .build());
      }
      resourceExistsCheckConsumer.accept(session, lineageEntries);
      return filter.apply(session, response.build());
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return findAllOutputs(findAllOutputs, resourceExistsCheckConsumer, filter);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public FindAllInputsOutputs.Response findAllInputsOutputs(
      FindAllInputsOutputs findAllInputsOutputs,
      ResourceExistsCheckConsumer resourceExistsCheckConsumer,
      Function3<Session, FindAllInputsOutputs.Response, FindAllInputsOutputs.Response> filter)
      throws ModelDBException, InvalidProtocolBufferException, NoSuchAlgorithmException {
    FindAllInputsOutputs.Response.Builder response = FindAllInputsOutputs.Response.newBuilder();
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      List<LineageEntryBatchRequest> itemList = findAllInputsOutputs.getItemsList();
      List<LineageEntry> lineageEntries = new LinkedList<>();
      for (LineageEntryBatchRequest inputoutput : itemList) {
        validate(inputoutput);
        lineageEntries.add(inputoutput.getEntry());
        response
            .addInputs(
                LineageEntryBatchResponse.newBuilder()
                    .addAllItems(getInputsByOutput(session, inputoutput))
                    .build())
            .addOutputs(
                LineageEntryBatchResponse.newBuilder()
                    .addAllItems(getOutputsByInput(session, inputoutput))
                    .build());
      }
      resourceExistsCheckConsumer.accept(session, lineageEntries);
      return filter.apply(session, response.build());
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return findAllInputsOutputs(findAllInputsOutputs, resourceExistsCheckConsumer, filter);
      } else {
        throw ex;
      }
    }
  }

  private void deleteLineage(
      Session session,
      LineageEntry lineageEntry,
      Map<LineageEntryContainer, ConnectionEntity> entityIds)
      throws ModelDBException, InvalidProtocolBufferException {
    LineageEntryContainer key = LineageEntryContainer.fromProto(lineageEntry);
    ConnectionEntity connectionEntity = entityIds.get(key);
    if (connectionEntity != null) {
      deleteConnectionEntity(session, connectionEntity);
    }
    entityIds.keySet().remove(key);
  }

  private void deleteConnectionEntity(Session session, ConnectionEntity connectionEntity) {
    session.delete(connectionEntity);
  }

  private void addLineage(
      Session session, LineageEntry lineageEntry, Long id, ConnectionType connectionType)
      throws ModelDBException, InvalidProtocolBufferException {
    final Long entityId;
    final int entityType;

    switch (lineageEntry.getType()) {
      case EXPERIMENT_RUN:
        LineageExperimentRunEntity experimentRun =
            getLineageExperimentRunEntity(session, lineageEntry);
        if (experimentRun == null) {
          experimentRun = new LineageExperimentRunEntity(lineageEntry.getExternalId());
          session.save(experimentRun);
        }
        entityId = experimentRun.getId();
        entityType = ENTITY_TYPE_EXPERIMENT_RUN;
        break;
      case BLOB:
        VersioningLineageEntry blob = lineageEntry.getBlob();
        LineageVersioningBlobEntity lineageVersioningBlobEntity =
            getLineageVersioningBlobEntity(
                session, blob.getRepositoryId(), blob.getCommitSha(), blob.getLocationList());
        if (lineageVersioningBlobEntity == null) {
          lineageVersioningBlobEntity =
              new LineageVersioningBlobEntity(
                  blob.getRepositoryId(), blob.getCommitSha(), blob.getLocationList());
          session.save(lineageVersioningBlobEntity);
        }
        entityId = lineageVersioningBlobEntity.getId();
        entityType = ENTITY_TYPE_VERSIONING_BLOB;
        break;
        // TODO: dataset
      default:
        throw new ModelDBException("Unknown lineage type");
    }

    List<ConnectionEntity> connectionEntitiesForTheEntityId =
        getConnectionEntities(session, connectionType, entityId, entityType);
    if (connectionType == CONNECTION_TYPE_OUTPUT
        && !connectionEntitiesForTheEntityId.isEmpty()
        && !connectionEntitiesForTheEntityId.get(0).getId().equals(id)) {
      throw new ModelDBException(
          "Specified lineage entry already has an output connection", Code.INVALID_ARGUMENT);
    }
    for (ConnectionEntity connectionEntity : connectionEntitiesForTheEntityId) {
      if (connectionEntity.getId().equals(id)) {
        return;
      }
    }
    session.save(new ConnectionEntity(id, entityId, connectionType.getValue(), entityType));
  }

  private List<LineageEntryBatchResponseSingle> getOutputsByInput(
      Session session, LineageEntryBatchRequest input)
      throws ModelDBException, InvalidProtocolBufferException {
    return getLineageEntryContainerToConnectionEntityMap(session, input, CONNECTION_TYPE_OUTPUT);
  }

  private List<LineageEntryBatchResponseSingle> getInputsByOutput(
      Session session, LineageEntryBatchRequest output)
      throws ModelDBException, InvalidProtocolBufferException {
    return getLineageEntryContainerToConnectionEntityMap(session, output, CONNECTION_TYPE_INPUT);
  }

  /**
   * A -- what we want to receive (can be input or output). B -- what we already have (input or
   * output).
   *
   * @param session current session
   * @param sideB information about entries
   * @param connectionTypeSideA connection type of entries which we want to receive
   * @return side A information
   * @throws ModelDBException validation errors, internal errors
   */
  private List<LineageEntryBatchResponseSingle> getLineageEntryContainerToConnectionEntityMap(
      Session session, LineageEntryBatchRequest sideB, ConnectionType connectionTypeSideA)
      throws ModelDBException, InvalidProtocolBufferException {
    Map<Long, List<LineageEntry>> sideAInDatabase;
    switch (sideB.getIdentifierCase()) {
      case ID:
        List<ConnectionEntity> connectionEntitiesById =
            getConnectionEntitiesByIdAndConnectionType(session, sideB.getId(), connectionTypeSideA);
        sideAInDatabase =
            Collections.singletonMap(
                sideB.getId(),
                getLineageEntryContainerToConnectionEntityMap(session, connectionEntitiesById)
                    .keySet().stream()
                    .map(LineageEntryContainer::toProto)
                    .collect(Collectors.toList()));
        break;
      case ENTRY:
        sideAInDatabase = getSideAEntries(session, sideB.getEntry(), connectionTypeSideA);
        break;
      default:
        throw new ModelDBException("Unknown id type");
    }
    List<LineageEntryBatchResponseSingle> result = new LinkedList<>();
    for (Map.Entry<Long, List<LineageEntry>> entry : sideAInDatabase.entrySet()) {
      result.add(
          LineageEntryBatchResponseSingle.newBuilder()
              .setId(entry.getKey())
              .addAllItems(entry.getValue())
              .build());
    }
    return result;
  }
}
