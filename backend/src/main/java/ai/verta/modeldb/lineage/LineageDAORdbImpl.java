package ai.verta.modeldb.lineage;

import ai.verta.modeldb.AddLineage;
import ai.verta.modeldb.AddLineage.Response;
import ai.verta.modeldb.DeleteLineage;
import ai.verta.modeldb.FindAllInputs;
import ai.verta.modeldb.FindAllInputsOutputs;
import ai.verta.modeldb.FindAllOutputs;
import ai.verta.modeldb.LineageEntry;
import ai.verta.modeldb.LineageEntryBatch;
import ai.verta.modeldb.LineageEntryEnum.LineageEntryType;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.entities.LineageEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import io.grpc.Status.Code;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;

public class LineageDAORdbImpl implements LineageDAO {

  private static final Logger LOGGER = LogManager.getLogger(LineageDAORdbImpl.class);
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();

  @Override
  public Response addLineage(AddLineage addLineage, IsExistsPredicate isExistsPredicate)
      throws ModelDBException {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      validate(addLineage.getInputList(), addLineage.getOutputList());
      validateExistence(
          addLineage.getInputList(), addLineage.getOutputList(), isExistsPredicate, session);
      session.beginTransaction();
      for (LineageEntry input : addLineage.getInputList()) {
        for (LineageEntry output : addLineage.getOutputList()) {
          addLineage(session, input, output);
        }
      }
      session.getTransaction().commit();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return addLineage(addLineage, isExistsPredicate);
      } else {
        throw ex;
      }
    }
    return AddLineage.Response.newBuilder().setStatus(true).build();
  }

  @Override
  public DeleteLineage.Response deleteLineage(DeleteLineage deleteLineage) throws ModelDBException {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      validate(deleteLineage.getInputList(), deleteLineage.getOutputList());
      session.beginTransaction();
      for (LineageEntry input : deleteLineage.getInputList()) {
        for (LineageEntry output : deleteLineage.getOutputList()) {
          deleteLineage(session, input, output);
        }
      }
      session.getTransaction().commit();
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteLineage(deleteLineage);
      } else {
        throw ex;
      }
    }
    return DeleteLineage.Response.newBuilder().setStatus(true).build();
  }

  private void validate(List<LineageEntry> inputList, List<LineageEntry> outputList)
      throws ModelDBException {
    validate(inputList);
    validate(outputList);
  }

  private void validate(List<LineageEntry> list) throws ModelDBException {
    Set<String> ids = new HashSet<>();
    for (LineageEntry input : list) {
      ids.add(input.getExternalId());
      validate(input);
    }
    if (ids.size() != list.size()) {
      throw new ModelDBException("Non-unique resource ids in a requests", Code.INVALID_ARGUMENT);
    }
  }

  private void validate(LineageEntry lineageEntry) throws ModelDBException {
    String message;
    if (lineageEntry.getType() == LineageEntryType.UNKNOWN) {
      message = "Unknown lineage type";
    } else if (lineageEntry.getExternalId().isEmpty()) {
      message = "External id is empty";
    } else {
      message = null;
    }
    if (message != null) {
      LOGGER.info(message);
      throw new ModelDBException(message, Code.INVALID_ARGUMENT);
    }
  }

  private void validateExistence(
      List<LineageEntry> inputList,
      List<LineageEntry> outputList,
      IsExistsPredicate isExistsPredicate,
      Session session)
      throws ModelDBException {
    validateExistence(inputList, isExistsPredicate, session);
    validateExistence(outputList, isExistsPredicate, session);
  }

  private void validateExistence(
      List<LineageEntry> list, IsExistsPredicate isExistsPredicate, Session session)
      throws ModelDBException {
    for (LineageEntry input : list) {
      validateExistence(input, isExistsPredicate, session);
    }
  }

  private void validateExistence(
      LineageEntry lineageEntry, IsExistsPredicate isExistsResourcePredicate, Session session)
      throws ModelDBException {
    if (!isExistsResourcePredicate.test(
        session, lineageEntry.getExternalId(), lineageEntry.getType())) {
      final var message = "External resource with a specified id does not exists";
      LOGGER.info(message);
      throw new ModelDBException(message, Code.INVALID_ARGUMENT);
    }
  }

  @Override
  public FindAllInputs.Response findAllInputs(FindAllInputs findAllInputs) throws ModelDBException {
    var response = FindAllInputs.Response.newBuilder();
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      for (LineageEntry output : findAllInputs.getItemsList()) {
        validate(output);
        response.addInputs(
            LineageEntryBatch.newBuilder().addAllItems(getInputsByOutput(session, output)).build());
      }
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return findAllInputs(findAllInputs);
      } else {
        throw ex;
      }
    }
    return response.build();
  }

  @Override
  public FindAllOutputs.Response findAllOutputs(FindAllOutputs findAllOutputs)
      throws ModelDBException {
    var response = FindAllOutputs.Response.newBuilder();
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      for (LineageEntry input : findAllOutputs.getItemsList()) {
        validate(input);
        response.addOutputs(
            LineageEntryBatch.newBuilder().addAllItems(getOutputsByInput(session, input)).build());
      }
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return findAllOutputs(findAllOutputs);
      } else {
        throw ex;
      }
    }
    return response.build();
  }

  @Override
  public FindAllInputsOutputs.Response findAllInputsOutputs(
      FindAllInputsOutputs findAllInputsOutputs) throws ModelDBException {
    var response = FindAllInputsOutputs.Response.newBuilder();
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      for (LineageEntry inputoutput : findAllInputsOutputs.getItemsList()) {
        validate(inputoutput);
        final List<LineageEntry> inputs = getInputsByOutput(session, inputoutput);
        final List<LineageEntry> outputs = getOutputsByInput(session, inputoutput);
        response.addInputs(LineageEntryBatch.newBuilder().addAllItems(inputs));
        response.addOutputs(LineageEntryBatch.newBuilder().addAllItems(outputs).build());
      }
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return findAllInputsOutputs(findAllInputsOutputs);
      } else {
        throw ex;
      }
    }
    return response.build();
  }

  private void deleteLineage(Session session, LineageEntry input, LineageEntry output) {
    getExisting(session, input, output)
        .ifPresent(
            lineageEntity -> {
              session.lock(lineageEntity, LockMode.PESSIMISTIC_WRITE);
              session.delete(lineageEntity);
            });
  }

  private void addLineage(Session session, LineageEntry input, LineageEntry output) {
    saveOrUpdate(session, input, output);
  }

  private void saveOrUpdate(Session session, LineageEntry input, LineageEntry output) {
    var lineageEntity = new LineageEntity(input, output);
    session.buildLockRequest(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
    session.saveOrUpdate(lineageEntity);
  }

  private Optional<LineageEntity> getExisting(
      Session session, LineageEntry input, LineageEntry output) {
    var query =
        session.createQuery(
            "from LineageEntity where inputExternalId = :inputExternalId "
                + " and inputType = :inputType "
                + " and outputExternalId = :outputExternalId "
                + " and outputType = :outputType ");
    query.setParameter("inputExternalId", input.getExternalId());
    query.setParameter("inputType", input.getTypeValue());
    query.setParameter("outputExternalId", output.getExternalId());
    query.setParameter("outputType", output.getTypeValue());
    return Optional.ofNullable((LineageEntity) query.uniqueResult());
  }

  private List<LineageEntry> getOutputsByInput(Session session, LineageEntry input) {
    var query =
        session.createQuery(
            "from LineageEntity where inputExternalId = :inputExternalId and inputType = :inputType ");
    query.setParameter("inputExternalId", input.getExternalId());
    query.setParameter("inputType", input.getTypeValue());
    List<LineageEntry> result = new LinkedList<>();
    for (Object r : query.getResultList()) {
      var lineageEntity = (LineageEntity) r;
      result.add(
          LineageEntry.newBuilder()
              .setTypeValue(lineageEntity.getOutputType())
              .setExternalId(lineageEntity.getOutputExternalId())
              .build());
    }
    return result;
  }

  private List<LineageEntry> getInputsByOutput(Session session, LineageEntry output) {
    var query =
        session.createQuery(
            "from LineageEntity where outputExternalId = :outputExternalId and outputType = :outputType");
    query.setParameter("outputExternalId", output.getExternalId());
    query.setParameter("outputType", output.getTypeValue());
    List<LineageEntry> result = new LinkedList<>();
    for (Object r : query.getResultList()) {
      var lineageEntity = (LineageEntity) r;
      result.add(
          LineageEntry.newBuilder()
              .setTypeValue(lineageEntity.getInputType())
              .setExternalId(lineageEntity.getInputExternalId())
              .build());
    }
    return result;
  }
}
