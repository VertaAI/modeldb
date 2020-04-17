package ai.verta.modeldb.lineage;

import ai.verta.modeldb.AddLineage;
import ai.verta.modeldb.AddLineage.Response;
import ai.verta.modeldb.DeleteLineage;
import ai.verta.modeldb.FindAllInputs;
import ai.verta.modeldb.FindAllInputsOutputs;
import ai.verta.modeldb.FindAllOutputs;
import ai.verta.modeldb.LineageEntry;
import ai.verta.modeldb.LineageEntryBatch;
import ai.verta.modeldb.Location;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.VersioningLineageEntry;
import ai.verta.modeldb.entities.LineageEntity;
import ai.verta.modeldb.entities.LineageIdEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Status.Code;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
      LineageIdEntity lineageIdEntity;
      if (addLineage.getId() != 0) {
        id = addLineage.getId();
        lineageIdEntity = session.get(LineageIdEntity.class, addLineage.getId());
      } else {
        id = null;
        lineageIdEntity = null;
      }
      if (lineageIdEntity == null) {
        lineageIdEntity = new LineageIdEntity(id);
        session.save(lineageIdEntity);
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
      return AddLineage.Response.newBuilder().setId(lineageIdEntity.getId()).build();
    }
  }

  @Override
  public DeleteLineage.Response deleteLineage(DeleteLineage deleteLineage)
      throws ModelDBException, InvalidProtocolBufferException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      validate(deleteLineage.getInputList(), deleteLineage.getOutputList());
      for (LineageEntry input : deleteLineage.getInputList()) {
        for (LineageEntry output : deleteLineage.getOutputList()) {
          deleteLineage(session, input, output, deleteLineage.getId());
        }
      }
      session.getTransaction().commit();
    }
    return DeleteLineage.Response.newBuilder().setStatus(true).build();
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
      for (LineageEntry output : findAllInputs.getItemsList()) {
        validate(output);
        response.addInputs(
            LineageEntryBatch.newBuilder().addAllItems(getInputsByOutput(session, output)).build());
      }
    }
    return response.build();
  }

  @Override
  public FindAllOutputs.Response findAllOutputs(FindAllOutputs findAllOutputs)
      throws ModelDBException, InvalidProtocolBufferException {
    FindAllOutputs.Response.Builder response = FindAllOutputs.Response.newBuilder();
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      for (LineageEntry input : findAllOutputs.getItemsList()) {
        validate(input);
        response.addOutputs(
            LineageEntryBatch.newBuilder().addAllItems(getOutputsByInput(session, input)).build());
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
      for (LineageEntry inputoutput : findAllInputsOutputs.getItemsList()) {
        validate(inputoutput);
        final List<LineageEntry> inputs = getInputsByOutput(session, inputoutput);
        final List<LineageEntry> outputs = getOutputsByInput(session, inputoutput);
        response.addInputs(LineageEntryBatch.newBuilder().addAllItems(inputs));
        response.addOutputs(LineageEntryBatch.newBuilder().addAllItems(outputs).build());
      }
    }
    return response.build();
  }

  private void deleteLineage(Session session, LineageEntry input, LineageEntry output, long id)
      throws InvalidProtocolBufferException, ModelDBException {
    getExisting(session, input, output).ifPresent(session::delete);
  }

  private void addLineage(Session session, LineageEntry input, LineageEntry output,
      Long id) throws InvalidProtocolBufferException {
    saveOrUpdate(session, input, output, id);
  }

  private void saveOrUpdate(Session session, LineageEntry input, LineageEntry output,
      Long id) throws InvalidProtocolBufferException {
    session.saveOrUpdate(new LineageEntity(id, input, output));
  }

  private Optional<LineageEntity> getExisting(
      Session session, LineageEntry input, LineageEntry output)
      throws InvalidProtocolBufferException, ModelDBException {
    Query query =
        session.createQuery(
            "from LineageEntity where " + formQuery(input, "input") + " and " + formQuery(output, "output"));
    return Optional.ofNullable((LineageEntity) query.uniqueResult());
  }

  private String formQuery(LineageEntry lineageEntry, String type)
      throws InvalidProtocolBufferException, ModelDBException {
    String result = type + "Type = '" + lineageEntry.getDescriptionCase().getNumber() + "' and ";
    switch (lineageEntry.getDescriptionCase()) {
      case EXPERIMENT_RUN:
        return result + type + "ExperimentId = '" + lineageEntry.getExperimentRun() + "'";
      case BLOB:
        VersioningLineageEntry blob = lineageEntry.getBlob();
        return result + type + "RepositoryId = '" + blob.getRepositoryId() +
            "' and " + type + "CommitSha = '" + blob.getCommitSha() +
            "' and " + type + "Location = '" + ModelDBUtils
            .getStringFromProtoObject(Location.newBuilder().addAllLocation(blob.getLocationList()));
    }
    throw new ModelDBException("Unknown lineage type", Code.INTERNAL);
  }

  private List<LineageEntry> getOutputsByInput(Session session, LineageEntry input)
      throws InvalidProtocolBufferException, ModelDBException {
    Query query =
        session.createQuery(
            "from LineageEntity where " + formQuery(input, "input"));
    List<LineageEntry> result = new LinkedList<>();
    for (Object r : query.getResultList()) {
      LineageEntity lineageEntity = (LineageEntity) r;
      result.add(
          LineageEntry.newBuilder()
              .setExperimentRun(lineageEntity.getOutputExperimentId())
              .setBlob(lineageEntity.getOutputBlob())
              .build());
    }
    return result;
  }

  private List<LineageEntry> getInputsByOutput(Session session, LineageEntry output)
      throws InvalidProtocolBufferException, ModelDBException {
    Query query =
        session.createQuery(
            "from LineageEntity where " + formQuery(output, "output"));
    List<LineageEntry> result = new LinkedList<>();
    for (Object r : query.getResultList()) {
      LineageEntity lineageEntity = (LineageEntity) r;
      result.add(
          LineageEntry.newBuilder()
              .setExperimentRun(lineageEntity.getInputExperimentId())
              .setBlob(lineageEntity.getInputBlob())
              .build());
    }
    return result;
  }
}
