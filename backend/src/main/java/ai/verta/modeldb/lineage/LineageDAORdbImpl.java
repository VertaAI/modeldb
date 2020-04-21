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
import ai.verta.modeldb.Location;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.VersioningLineageEntry;
import ai.verta.modeldb.entities.lineage.ConnectionEntity;
import ai.verta.modeldb.entities.lineage.ElementEntity;
import ai.verta.modeldb.entities.lineage.ExperimentRunEntity;
import ai.verta.modeldb.entities.lineage.VersioningBlobEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class LineageDAORdbImpl implements LineageDAO {

  private static final Logger LOGGER = LogManager.getLogger(LineageDAORdbImpl.class);

  public LineageDAORdbImpl() {}

  @Override
  public Response addLineage(AddLineage addLineage, ExistsCheckConsumer existsCheckConsumer)
      throws ModelDBException, InvalidProtocolBufferException, NoSuchAlgorithmException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      Long id;
      ElementEntity elementEntity;
      if (addLineage.getId() != 0) {
        id = addLineage.getId();
        elementEntity = session.get(ElementEntity.class, addLineage.getId());
      } else {
        id = null;
        elementEntity = null;
      }
      if (elementEntity == null) {
        elementEntity = new ElementEntity(id);
        session.save(elementEntity);
      }
      validate(addLineage.getInputList(), addLineage.getOutputList());
      List<LineageEntry> lineageEntries = new LinkedList<>(addLineage.getInputList());
      lineageEntries.addAll(addLineage.getOutputList());
      existsCheckConsumer.test(session, lineageEntries);
      for (LineageEntry input : addLineage.getInputList()) {
        for (LineageEntry output : addLineage.getOutputList()) {
          addLineage(session, input, output, id);
        }
      }
      session.getTransaction().commit();
      return AddLineage.Response.newBuilder().setId(elementEntity.getId()).build();
    }
  }

  @Override
  public DeleteLineage.Response deleteLineage(DeleteLineage deleteLineage)
      throws ModelDBException, InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      validate(deleteLineage.getInputList(), deleteLineage.getOutputList());
      long id = deleteLineage.getId();
      List connectionEntitiesById = getConnectionEntitiesById(session, id);
      Map.Entry<Map<LineageElement, ConnectionEntity>, Map<LineageElement, ConnectionEntity>>
          inputOutputs = getInputOutputs(session, connectionEntitiesById);
      Map<LineageElement, ConnectionEntity> inputInDatabase = inputOutputs.getKey();
      Map<LineageElement, ConnectionEntity> outputInDatabase = inputOutputs.getValue();
      for (LineageEntry input : deleteLineage.getInputList()) {
        deleteLineage(session, input, inputInDatabase);
      }
      for (LineageEntry output : deleteLineage.getOutputList()) {
        deleteLineage(session, output, outputInDatabase);
      }
      if (inputInDatabase.isEmpty() || outputInDatabase.isEmpty()) {
        ElementEntity elementEntity = session.get(ElementEntity.class, id);
        session.remove(elementEntity);
      }
      session.getTransaction().commit();
    }
    return DeleteLineage.Response.newBuilder().setStatus(true).build();
  }

  private Entry<Map<LineageElement, ConnectionEntity>, Map<LineageElement, ConnectionEntity>>
      getInputOutputs(Session session, List result) throws ModelDBException {
    Map<LineageElement, ConnectionEntity> inputInDatabase = new HashMap<>();
    Map<LineageElement, ConnectionEntity> outputInDatabase = new HashMap<>();
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

  private Map<LineageElement, ConnectionEntity> getInputOrOutput(Session session, List result)
      throws ModelDBException {
    Map<LineageElement, ConnectionEntity> inputOrOutputInDatabase = new HashMap<>();
    for (Object entity : result) {
      ConnectionEntity connectionEntity = (ConnectionEntity) entity;
      inputOrOutputInDatabase.put(connectionEntity.getLineageElement(session), connectionEntity);
    }
    return inputOrOutputInDatabase;
  }

  private List getConnectionEntitiesById(Session session, long id) {
    return getConnectionEntitiesByIdAndConnectionType(session, id, CONNECTION_TYPE_ANY);
  }

  private List getConnectionEntitiesByIdAndConnectionType(
      Session session, long id, int connectionType) {
    String queryString = "from " + ConnectionEntity.class.getSimpleName() + " where id = " + id;
    if (connectionType != CONNECTION_TYPE_ANY) {
      queryString += " and connectionType = " + connectionType;
    }
    Query query = session.createQuery(queryString);
    return query.list();
  }

  private Map<Long, List<LineageEntry>> getConnectionEntitiesByEntryAndConnectionType(
      Session session, LineageEntry lineageEntry, int connectionType)
      throws ModelDBException, InvalidProtocolBufferException {
    String queryString;
    List<Long> elements = new LinkedList<>();
    int entityType;
    switch (lineageEntry.getDescriptionCase()) {
      case EXPERIMENT_RUN:
        queryString =
            "from "
                + ExperimentRunEntity.class.getSimpleName()
                + " where experimentRunId = '"
                + lineageEntry.getExperimentRun()
                + "'";
        List experimentRunEntityList = session.createQuery(queryString).list();
        for (Object entity : experimentRunEntityList) {
          ExperimentRunEntity experimentRunEntity = (ExperimentRunEntity) entity;
          elements.add(experimentRunEntity.getId());
        }
        entityType = ENTITY_TYPE_EXPERIMENT_RUN;
        break;
      case BLOB:
        VersioningLineageEntry blob = lineageEntry.getBlob();
        queryString =
            "from "
                + VersioningBlobEntity.class.getSimpleName()
                + " where repositoryId = '"
                + blob.getRepositoryId()
                + "' and commitSha = '"
                + blob.getCommitSha()
                + "' and location = '"
                + ModelDBUtils.getStringFromProtoObject(
                    Location.newBuilder().addAllLocation(blob.getLocationList()))
                + "'";
        List versioningBlobEntityList = session.createQuery(queryString).list();
        for (Object entity : versioningBlobEntityList) {
          VersioningBlobEntity versioningBlobEntity = (VersioningBlobEntity) entity;
          elements.add(versioningBlobEntity.getId());
        }
        entityType = ENTITY_TYPE_VERSIONING_BLOB;
        break;
      default:
        throw new ModelDBException("Unknown entry type");
    }
    Map<Long, List<LineageEntry>> result = new HashMap<>();
    for (Long entry : elements) {
      queryString =
          "from "
              + ConnectionEntity.class.getSimpleName()
              + " where entityId = "
              + entry
              + " and entity_type = "
              + entityType
              + " and connectionType = "
              + invert(connectionType);
      Query query = session.createQuery(queryString);
      List list = query.list();
      for (Object entity : list) {
        ConnectionEntity connectionEntity = (ConnectionEntity) entity;
        List connectionEntitiesById =
            getConnectionEntitiesByIdAndConnectionType(
                session, connectionEntity.getId(), connectionType);
        Map<LineageElement, ConnectionEntity> inputOrOutputInDatabase =
            getInputOrOutput(session, connectionEntitiesById);
        result.put(
            connectionEntity.getId(),
            inputOrOutputInDatabase.keySet().stream()
                .map(LineageElement::toProto)
                .collect(Collectors.toList()));
      }
    }
    return result;
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
  public FindAllInputs.Response findAllInputs(FindAllInputs findAllInputs)
      throws ModelDBException, InvalidProtocolBufferException {
    FindAllInputs.Response.Builder response = FindAllInputs.Response.newBuilder();
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      List<LineageEntryBatchRequest> itemList = findAllInputs.getItemsList();
      for (LineageEntryBatchRequest output : itemList) {
        validate(output);
        response.addInputs(
            LineageEntryBatchResponse.newBuilder()
                .addAllItems(getInputsByOutput(session, output))
                .build());
      }
    }
    return response.build();
  }

  @Override
  public FindAllOutputs.Response findAllOutputs(FindAllOutputs findAllOutputs)
      throws ModelDBException, InvalidProtocolBufferException {
    FindAllOutputs.Response.Builder response = FindAllOutputs.Response.newBuilder();
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      List<LineageEntryBatchRequest> itemList = findAllOutputs.getItemsList();
      for (LineageEntryBatchRequest input : itemList) {
        validate(input);
        response.addOutputs(
            LineageEntryBatchResponse.newBuilder()
                .addAllItems(getOutputsByInput(session, input))
                .build());
      }
    }
    return response.build();
  }

  @Override
  public FindAllInputsOutputs.Response findAllInputsOutputs(
      FindAllInputsOutputs findAllInputsOutputs)
      throws ModelDBException, InvalidProtocolBufferException {
    FindAllInputsOutputs.Response.Builder response = FindAllInputsOutputs.Response.newBuilder();
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      List<LineageEntryBatchRequest> itemList = findAllInputsOutputs.getItemsList();
      for (LineageEntry inputoutput : findAllInputsOutputs.getItemsList()) {
        validate(inputoutput);
        final List<LineageEntry> inputs = getInputsByOutput(session, inputoutput);
        final List<LineageEntry> outputs = getOutputsByInput(session, inputoutput);
        response.addInputs(LineageEntryBatch.newBuilder().addAllItems(inputs));
        response.addOutputs(LineageEntryBatch.newBuilder().addAllItems(outputs).build());
      }
    }*/
    return response.build();
  }

  private void deleteLineage(
      Session session, LineageEntry lineageEntry, Map<LineageElement, ConnectionEntity> entityIds)
      throws InvalidProtocolBufferException, ModelDBException {
    LineageElement key = LineageElement.fromProto(lineageEntry);
    ConnectionEntity connectionEntity = entityIds.get(key);
    if (connectionEntity != null) {
      switch (connectionEntity.getEntityType()) {
        case ENTITY_TYPE_EXPERIMENT_RUN:
          session.delete(session.get(ExperimentRunEntity.class, connectionEntity.getEntityId()));
          break;
        case ENTITY_TYPE_VERSIONING_BLOB:
          session.delete(session.get(VersioningBlobEntity.class, connectionEntity.getEntityId()));
          break;
        default:
          throw new ModelDBException("Unknown connection type");
      }
      session.delete(connectionEntity);
      entityIds.entrySet().remove(key);
    }
  }

  private void addLineage(Session session, LineageEntry input, LineageEntry output, Long id)
      throws InvalidProtocolBufferException, ModelDBException {
    saveOrUpdate(session, input, id, CONNECTION_TYPE_INPUT);
    saveOrUpdate(session, output, id, CONNECTION_TYPE_OUTPUT);
  }

  private void saveOrUpdate(Session session, LineageEntry lineageEntry, Long id, int connectionType)
      throws ModelDBException, InvalidProtocolBufferException {
    Long entityId;
    int entityType;
    switch (lineageEntry.getDescriptionCase()) {
      case EXPERIMENT_RUN:
        ExperimentRunEntity experimentRun =
            new ExperimentRunEntity(lineageEntry.getExperimentRun());
        session.saveOrUpdate(experimentRun);
        entityId = experimentRun.getId();
        entityType = ENTITY_TYPE_EXPERIMENT_RUN;
        break;
      case BLOB:
        VersioningLineageEntry blob = lineageEntry.getBlob();
        VersioningBlobEntity versioningBlobEntity =
            new VersioningBlobEntity(
                blob.getRepositoryId(),
                blob.getCommitSha(),
                ModelDBUtils.getStringFromProtoObject(
                    Location.newBuilder().addAllLocation(blob.getLocationList())));
        session.saveOrUpdate(versioningBlobEntity);
        entityId = versioningBlobEntity.getId();
        entityType = ENTITY_TYPE_VERSIONING_BLOB;
        break;
      default:
        throw new ModelDBException("Unknown lineage type");
    }
    session.saveOrUpdate(new ConnectionEntity(id, entityId, connectionType, entityType));
  }

  private String formQuery(LineageEntry lineageEntry, int type)
      throws InvalidProtocolBufferException, ModelDBException {
    String result = "connectionType = " + type + " and ";
    switch (lineageEntry.getDescriptionCase()) {
      case EXPERIMENT_RUN:
        return result + type + "ExperimentId = '" + lineageEntry.getExperimentRun() + "'";
      case BLOB:
        VersioningLineageEntry blob = lineageEntry.getBlob();
        return result
            + type
            + "RepositoryId = '"
            + blob.getRepositoryId()
            + "' and "
            + type
            + "CommitSha = '"
            + blob.getCommitSha()
            + "' and "
            + type
            + "Location = '"
            + ModelDBUtils.getStringFromProtoObject(
                Location.newBuilder().addAllLocation(blob.getLocationList()));
    }
    throw new ModelDBException("Unknown lineage type", Code.INTERNAL);
  }

  private List<LineageEntryBatchResponseSingle> getOutputsByInput(
      Session session, LineageEntryBatchRequest input)
      throws InvalidProtocolBufferException, ModelDBException {
    return getInputOrOutput(session, input, CONNECTION_TYPE_OUTPUT);
  }

  private List<LineageEntryBatchResponseSingle> getInputsByOutput(
      Session session, LineageEntryBatchRequest output)
      throws InvalidProtocolBufferException, ModelDBException {
    return getInputOrOutput(session, output, CONNECTION_TYPE_INPUT);
  }
  
  private List<LineageEntryBatchResponseSingle> getInputOrOutput(
      Session session, LineageEntryBatchRequest sideB, int connectionType)
      throws InvalidProtocolBufferException, ModelDBException {
    Map<Long, List<LineageEntry>> sideAInDatabase;
    switch (sideB.getIdentifierCase()) {
      case ID:
        List connectionEntitiesById =
            getConnectionEntitiesByIdAndConnectionType(
                session, sideB.getId(), connectionType);
        sideAInDatabase =
            Collections.singletonMap(
                sideB.getId(),
                getInputOrOutput(session, connectionEntitiesById).keySet().stream()
                    .map(LineageElement::toProto)
                    .collect(Collectors.toList()));
        break;
      case ENTRY:
        sideAInDatabase =
            getConnectionEntitiesByEntryAndConnectionType(
                session, sideB.getEntry(), CONNECTION_TYPE_INPUT);
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
