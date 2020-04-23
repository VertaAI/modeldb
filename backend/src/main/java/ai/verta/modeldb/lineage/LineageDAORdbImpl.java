package ai.verta.modeldb.lineage;

import static ai.verta.modeldb.entities.lineage.ConnectionEntity.CONNECTION_TYPE_ANY;
import static ai.verta.modeldb.entities.lineage.ConnectionEntity.CONNECTION_TYPE_INPUT;
import static ai.verta.modeldb.entities.lineage.ConnectionEntity.CONNECTION_TYPE_OUTPUT;
import static ai.verta.modeldb.entities.lineage.ConnectionEntity.ENTITY_TYPE_EXPERIMENT_RUN;
import static ai.verta.modeldb.entities.lineage.ConnectionEntity.ENTITY_TYPE_VERSIONING_BLOB;

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
import ai.verta.modeldb.entities.lineage.LineageElementEntity;
import ai.verta.modeldb.entities.lineage.LineageExperimentRunEntity;
import ai.verta.modeldb.entities.lineage.LineageVersioningBlobEntity;
import ai.verta.modeldb.entities.versioning.InternalFolderElementEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import com.google.protobuf.InvalidProtocolBufferException;
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
      AddLineage addLineage,
      ResourceExistsCheckConsumer resourceExistsCheckConsumer,
      CommitHashToBlobHashFunction commitHashToBlobHashFunction)
      throws ModelDBException, InvalidProtocolBufferException, NoSuchAlgorithmException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      Long id;
      LineageElementEntity lineageElementEntity;
      if (addLineage.getId() != 0) {
        id = addLineage.getId();
        lineageElementEntity = session.get(LineageElementEntity.class, addLineage.getId());
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
        addLineage(session, input, id, CONNECTION_TYPE_INPUT, commitHashToBlobHashFunction);
      }
      for (LineageEntry output : addLineage.getOutputList()) {
        addLineage(session, output, id, CONNECTION_TYPE_OUTPUT, commitHashToBlobHashFunction);
      }
      session.getTransaction().commit();
      return AddLineage.Response.newBuilder().setId(lineageElementEntity.getId()).build();
    }
  }

  @Override
  public DeleteLineage.Response deleteLineage(
      DeleteLineage deleteLineage, CommitHashToBlobHashFunction commitHashToBlobHashFunction)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      validate(deleteLineage.getInputList(), deleteLineage.getOutputList());
      long id = deleteLineage.getId();
      if (id == 0) {
        throw new ModelDBException("Id not specified", Code.INVALID_ARGUMENT);
      }
      List connectionEntitiesById = getConnectionEntitiesById(session, id);
      Map.Entry<
              Map<LineageEntryContainer, ConnectionEntity>,
              Map<LineageEntryContainer, ConnectionEntity>>
          inputOutputs = getInputOutputs(session, connectionEntitiesById);
      Map<LineageEntryContainer, ConnectionEntity> inputInDatabase = inputOutputs.getKey();
      Map<LineageEntryContainer, ConnectionEntity> outputInDatabase = inputOutputs.getValue();
      for (LineageEntry input : deleteLineage.getInputList()) {
        deleteLineage(session, input, inputInDatabase, commitHashToBlobHashFunction);
      }
      for (LineageEntry output : deleteLineage.getOutputList()) {
        deleteLineage(session, output, outputInDatabase, commitHashToBlobHashFunction);
      }
      /* noDataProvided is a flag that there are no entries provided in the request.
      That means that we should delete all entries with the specified id.
      Also when there are no input or output in the database left we should delete
      the remaining ones because they are not connected with anything. */
      boolean noDataProvided =
          deleteLineage.getInputCount() == 0 && deleteLineage.getOutputCount() == 0;
      if (noDataProvided || inputInDatabase.isEmpty() || outputInDatabase.isEmpty()) {
        Set<ConnectionEntity> values = new HashSet<>(inputInDatabase.values());
        values.addAll(outputInDatabase.values());
        values.forEach(connectionEntity -> deleteConnectionEntity(session, connectionEntity));
        LineageElementEntity lineageElementEntity = session.get(LineageElementEntity.class, id);
        if (lineageElementEntity != null) {
          session.remove(lineageElementEntity);
        }
      }
      session.getTransaction().commit();
    }
    return DeleteLineage.Response.newBuilder().setStatus(true).build();
  }

  private Entry<
          Map<LineageEntryContainer, ConnectionEntity>,
          Map<LineageEntryContainer, ConnectionEntity>>
      getInputOutputs(Session session, List result) {
    Map<LineageEntryContainer, ConnectionEntity> inputInDatabase = new HashMap<>();
    Map<LineageEntryContainer, ConnectionEntity> outputInDatabase = new HashMap<>();
    for (Object entity : result) {
      ConnectionEntity connectionEntity = (ConnectionEntity) entity;
      if (connectionEntity.getConnectionType() == CONNECTION_TYPE_INPUT) {
        inputInDatabase.put(connectionEntity.getLineageElement(session), connectionEntity);
      } else {
        outputInDatabase.put(connectionEntity.getLineageElement(session), connectionEntity);
      }
    }
    return new AbstractMap.SimpleEntry<>(inputInDatabase, outputInDatabase);
  }

  private Map<LineageEntryContainer, ConnectionEntity> getInputOrOutput(
      Session session, List<ConnectionEntity> result) {
    return result.stream()
        .collect(Collectors.toMap(connectionEntity -> connectionEntity.getLineageElement(session),
            connectionEntity -> connectionEntity));
  }

  private List<ConnectionEntity> getConnectionEntitiesById(Session session, long id) {
    return getConnectionEntitiesByIdAndConnectionType(session, id, CONNECTION_TYPE_ANY);
  }

  private List<ConnectionEntity> getConnectionEntitiesByIdAndConnectionType(
      Session session, long id, int connectionType) {
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<ConnectionEntity> criteriaQuery =
        builder.createQuery(ConnectionEntity.class);
    Root<ConnectionEntity> root = criteriaQuery.from(ConnectionEntity.class);
    final Predicate idPredicate = root.get("id").in(id);
    final Predicate finalPredicate;
    if (connectionType != CONNECTION_TYPE_ANY) {
      finalPredicate = builder.and(idPredicate, root.get("connectionType").in(connectionType));
    } else {
      finalPredicate = idPredicate;
    }
    criteriaQuery.select(root);
    criteriaQuery.where(finalPredicate);
    Query<ConnectionEntity> query = session.createQuery(criteriaQuery);
    return query.list();
  }

  private Map<Long, List<LineageEntry>> getConnectionEntitiesByEntryAndConnectionType(
      Session session,
      LineageEntry lineageEntry,
      int connectionType,
      CommitHashToBlobHashFunction commitHashToBlobHashFunction,
      BlobHashToCommitHashFunction blobHashToCommitHashFunction)
      throws ModelDBException {
    int entityType;
    Long entityId;
    Map<Long, List<LineageEntry>> result = new HashMap<>();
    switch (lineageEntry.getDescriptionCase()) {
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
        try {
          InternalFolderElementEntity blobHashInCommit =
              commitHashToBlobHashFunction.apply(session, lineageEntry.getBlob());
          lineageVersioningBlobEntity =
              getLineageVersioningBlobEntity(
                  session,
                  lineageEntry.getBlob().getRepositoryId(),
                  blobHashInCommit.getElement_sha(),
                  blobHashInCommit.getElement_type());
        } catch (ModelDBException e) {
          LOGGER.warn("commit not found: {}", e.getMessage());
          lineageVersioningBlobEntity = null;
        }
        if (lineageVersioningBlobEntity == null) {
          return result;
        }
        entityId = lineageVersioningBlobEntity.getId();
        entityType = ENTITY_TYPE_VERSIONING_BLOB;
        break;
      default:
        throw new ModelDBException("Unknown entry type");
    }
    List list = getConnectionEntities(session, invert(connectionType), entityId, entityType);
    for (Object entity : list) {
      ConnectionEntity connectionEntity = (ConnectionEntity) entity;
      List<ConnectionEntity> connectionEntitiesById = getConnectionEntitiesByIdAndConnectionType(
          session, connectionEntity.getId(), connectionType);
      Map<LineageEntryContainer, ConnectionEntity> inputOrOutputInDatabase =
          getInputOrOutput(session, connectionEntitiesById);
      result.put(
          connectionEntity.getId(),
          inputOrOutputInDatabase.keySet().stream()
              .map(lineageElement -> lineageElement.toProto(session, blobHashToCommitHashFunction))
              .collect(Collectors.toList()));
    }
    return result;
  }

  private List getConnectionEntities(
      Session session, int connectionType, Long entityId, int entityType) {
    String queryString;
    queryString =
        "from "
            + ConnectionEntity.class.getSimpleName()
            + " where entityId = "
            + entityId
            + " and entity_type = "
            + entityType;
    if (connectionType != CONNECTION_TYPE_ANY) {
      queryString += " and connectionType = " + connectionType;
    }
    Query query = session.createQuery(queryString);
    return query.list();
  }

  private LineageVersioningBlobEntity getLineageVersioningBlobEntity(
      Session session, long repositoryId, String blobSha, String blobType) {
    String queryString;
    Object entity;
    queryString =
        "from "
            + LineageVersioningBlobEntity.class.getSimpleName()
            + " where repositoryId = '"
            + repositoryId
            + "' and blobSha = '"
            + blobSha
            + "' and blobType = '"
            + blobType
            + "'";
    entity = session.createQuery(queryString).uniqueResult();
    return (LineageVersioningBlobEntity) entity;
  }

  private LineageExperimentRunEntity getLineageExperimentRunEntity(
      Session session, LineageEntry lineageEntry) {
    String queryString;
    queryString =
        "from "
            + LineageExperimentRunEntity.class.getSimpleName()
            + " where experimentRunId = '"
            + lineageEntry.getExperimentRun()
            + "'";
    Query query = session.createQuery(queryString);
    Object entity = query.uniqueResult();
    return (LineageExperimentRunEntity) entity;
  }

  private int invert(int connectionType) throws ModelDBException {
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
      LOGGER.warn(message);
      throw new ModelDBException(message, Code.INVALID_ARGUMENT);
    }
  }

  private void validate(LineageEntry lineageEntry) throws ModelDBException {
    final String message;
    switch (lineageEntry.getDescriptionCase()) {
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
        if (lineageEntry.getExperimentRun().isEmpty()) {
          message = "Experiment run id is empty";
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
      CommitHashToBlobHashFunction commitHashToBlobHashFunction,
      BlobHashToCommitHashFunction blobHashToCommitHashFunction)
      throws ModelDBException {
    FindAllInputs.Response.Builder response = FindAllInputs.Response.newBuilder();
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      List<LineageEntryBatchRequest> itemList = findAllInputs.getItemsList();
      for (LineageEntryBatchRequest output : itemList) {
        validate(output);
        response.addInputs(
            LineageEntryBatchResponse.newBuilder()
                .addAllItems(
                    getInputsByOutput(
                        session,
                        output,
                        commitHashToBlobHashFunction,
                        blobHashToCommitHashFunction))
                .build());
      }
    }
    return response.build();
  }

  @Override
  public FindAllOutputs.Response findAllOutputs(
      FindAllOutputs findAllOutputs,
      CommitHashToBlobHashFunction commitHashToBlobHashFunction,
      BlobHashToCommitHashFunction blobHashToCommitHashFunction)
      throws ModelDBException {
    FindAllOutputs.Response.Builder response = FindAllOutputs.Response.newBuilder();
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      List<LineageEntryBatchRequest> itemList = findAllOutputs.getItemsList();
      for (LineageEntryBatchRequest input : itemList) {
        validate(input);
        response.addOutputs(
            LineageEntryBatchResponse.newBuilder()
                .addAllItems(
                    getOutputsByInput(
                        session, input, commitHashToBlobHashFunction, blobHashToCommitHashFunction))
                .build());
      }
    }
    return response.build();
  }

  @Override
  public FindAllInputsOutputs.Response findAllInputsOutputs(
      FindAllInputsOutputs findAllInputsOutputs,
      CommitHashToBlobHashFunction commitHashToBlobHashFunction,
      BlobHashToCommitHashFunction blobHashToCommitHashFunction)
      throws ModelDBException {
    FindAllInputsOutputs.Response.Builder response = FindAllInputsOutputs.Response.newBuilder();
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      List<LineageEntryBatchRequest> itemList = findAllInputsOutputs.getItemsList();
      for (LineageEntryBatchRequest inputoutput : itemList) {
        validate(inputoutput);
        response
            .addInputs(
                LineageEntryBatchResponse.newBuilder()
                    .addAllItems(
                        getInputsByOutput(
                            session,
                            inputoutput,
                            commitHashToBlobHashFunction,
                            blobHashToCommitHashFunction))
                    .build())
            .addOutputs(
                LineageEntryBatchResponse.newBuilder()
                    .addAllItems(
                        getOutputsByInput(
                            session,
                            inputoutput,
                            commitHashToBlobHashFunction,
                            blobHashToCommitHashFunction))
                    .build());
      }
    }
    return response.build();
  }

  private void deleteLineage(
      Session session,
      LineageEntry lineageEntry,
      Map<LineageEntryContainer, ConnectionEntity> entityIds,
      CommitHashToBlobHashFunction commitHashToBlobHashFunction)
      throws ModelDBException {
    LineageEntryContainer key =
        LineageEntryContainer.fromProto(session, lineageEntry, commitHashToBlobHashFunction);
    ConnectionEntity connectionEntity = entityIds.get(key);
    if (connectionEntity != null) {
      deleteConnectionEntity(session, connectionEntity);
    }
    entityIds.keySet().remove(key);
  }

  private void deleteConnectionEntity(Session session, ConnectionEntity connectionEntity) {
    List connectionEntities =
        getConnectionEntities(
            session,
            CONNECTION_TYPE_ANY,
            connectionEntity.getEntityId(),
            connectionEntity.getEntityType());
    if (connectionEntities.size() < 2) {
      switch (connectionEntity.getEntityType()) {
        case ENTITY_TYPE_EXPERIMENT_RUN:
          session.delete(
              session.get(LineageExperimentRunEntity.class, connectionEntity.getEntityId()));
          break;
        case ENTITY_TYPE_VERSIONING_BLOB:
          session.delete(
              session.get(LineageVersioningBlobEntity.class, connectionEntity.getEntityId()));
          break;
      }
    }
    session.delete(connectionEntity);
  }

  private void addLineage(
      Session session,
      LineageEntry lineageEntry,
      Long id,
      int connectionType,
      CommitHashToBlobHashFunction commitHashToBlobHashFunction)
      throws ModelDBException {
    final Long entityId;
    final int entityType;
    final boolean connectionExists;

    switch (lineageEntry.getDescriptionCase()) {
      case EXPERIMENT_RUN:
        LineageExperimentRunEntity experimentRun =
            getLineageExperimentRunEntity(session, lineageEntry);
        if (experimentRun == null) {
          experimentRun = new LineageExperimentRunEntity(lineageEntry.getExperimentRun());
          session.save(experimentRun);
          connectionExists = false;
        } else {
          connectionExists = true;
        }
        entityId = experimentRun.getId();
        entityType = ENTITY_TYPE_EXPERIMENT_RUN;
        break;
      case BLOB:
        VersioningLineageEntry blob = lineageEntry.getBlob();
        InternalFolderElementEntity result = commitHashToBlobHashFunction.apply(session, blob);
        LineageVersioningBlobEntity lineageVersioningBlobEntity =
            getLineageVersioningBlobEntity(
                session, blob.getRepositoryId(), result.getElement_sha(), result.getElement_type());
        if (lineageVersioningBlobEntity == null) {
          lineageVersioningBlobEntity =
              new LineageVersioningBlobEntity(
                  blob.getRepositoryId(), result.getElement_sha(), result.getElement_type());
          session.save(lineageVersioningBlobEntity);
          connectionExists = false;
        } else {
          connectionExists = true;
        }
        entityId = lineageVersioningBlobEntity.getId();
        entityType = ENTITY_TYPE_VERSIONING_BLOB;
        break;
      default:
        throw new ModelDBException("Unknown lineage type");
    }

    List connectionEntities =
        getConnectionEntities(session, invert(connectionType), entityId, entityType);
    if (connectionExists) {
      if (connectionType == CONNECTION_TYPE_OUTPUT
          && !((ConnectionEntity) connectionEntities.get(0)).getId().equals(id)) {
        throw new ModelDBException(
            "Specified lineage entry already has an output connection", Code.INVALID_ARGUMENT);
      }
    }
    for (Object entity : connectionEntities) {
      ConnectionEntity connectionEntity = (ConnectionEntity) entity;
      if (connectionEntity.getId().equals(id)) {
        return;
      }
    }
    session.save(new ConnectionEntity(id, entityId, connectionType, entityType));
  }

  private List<LineageEntryBatchResponseSingle> getOutputsByInput(
      Session session,
      LineageEntryBatchRequest input,
      CommitHashToBlobHashFunction commitHashToBlobHashFunction,
      BlobHashToCommitHashFunction blobHashToCommitHashFunction)
      throws ModelDBException {
    return getInputOrOutput(
        session,
        input,
        CONNECTION_TYPE_OUTPUT,
        commitHashToBlobHashFunction,
        blobHashToCommitHashFunction);
  }

  private List<LineageEntryBatchResponseSingle> getInputsByOutput(
      Session session,
      LineageEntryBatchRequest output,
      CommitHashToBlobHashFunction commitHashToBlobHashFunction,
      BlobHashToCommitHashFunction blobHashToCommitHashFunction)
      throws ModelDBException {
    return getInputOrOutput(
        session,
        output,
        CONNECTION_TYPE_INPUT,
        commitHashToBlobHashFunction,
        blobHashToCommitHashFunction);
  }

  private List<LineageEntryBatchResponseSingle> getInputOrOutput(
      Session session,
      LineageEntryBatchRequest sideB,
      int connectionType,
      CommitHashToBlobHashFunction commitHashToBlobHashFunction,
      BlobHashToCommitHashFunction blobHashToCommitHashFunction)
      throws ModelDBException {
    Map<Long, List<LineageEntry>> sideAInDatabase;
    switch (sideB.getIdentifierCase()) {
      case ID:
        List<ConnectionEntity> connectionEntitiesById = getConnectionEntitiesByIdAndConnectionType(
            session, sideB.getId(), connectionType);
        sideAInDatabase =
            Collections.singletonMap(
                sideB.getId(),
                getInputOrOutput(session, connectionEntitiesById).keySet().stream()
                    .map(
                        lineageElement ->
                            lineageElement.toProto(session, blobHashToCommitHashFunction))
                    .collect(Collectors.toList()));
        break;
      case ENTRY:
        sideAInDatabase =
            getConnectionEntitiesByEntryAndConnectionType(
                session,
                sideB.getEntry(),
                connectionType,
                commitHashToBlobHashFunction,
                blobHashToCommitHashFunction);
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
