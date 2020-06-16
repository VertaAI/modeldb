package ai.verta.modeldb.metadata;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.metadata.LabelsMappingEntity;
import ai.verta.modeldb.entities.metadata.MetadataPropertyMappingEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class MetadataDAORdbImpl implements MetadataDAO {
  private static final Logger LOGGER = LogManager.getLogger(MetadataDAORdbImpl.class);

  private static final String GET_LABELS_HQL =
      new StringBuilder("From LabelsMappingEntity lm where lm.id.")
          .append(ModelDBConstants.ENTITY_HASH)
          .append(" = :entityHash ")
          .append(" AND lm.id.")
          .append(ModelDBConstants.ENTITY_TYPE)
          .append(" = :entityType")
          .toString();
  private static final String GET_PROPERTY_HQL =
      new StringBuilder("From MetadataPropertyMappingEntity pm where pm.id.")
          .append("repositoryId")
          .append(" = :repositoryId")
          .append(" AND pm.id.")
          .append("commitSha")
          .append(" = :commitSha")
          .append(" AND pm.id.")
          .append("location")
          .append(" = :location")
          .append(" AND pm.id.")
          .append("key")
          .append(" = :key")
          .toString();
  private static final String DELETE_LABELS_HQL =
      new StringBuilder("DELETE From LabelsMappingEntity lm where lm.")
          .append(ModelDBConstants.ENTITY_HASH)
          .append(" = :entityHash ")
          .append(" AND lm.")
          .append(ModelDBConstants.ENTITY_TYPE)
          .append(" = :entityType AND lm.")
          .append(ModelDBConstants.LABEL)
          .append(" IN (:labels)")
          .toString();

  private String getEntityHash(IdentificationType id) throws ModelDBException {
    String entityHash;
    switch (id.getIdCase()) {
      case INT_ID:
        entityHash = String.valueOf(id.getIntId());
        break;
      case STRING_ID:
        entityHash = id.getStringId();
        break;
      case COMPOSITE_ID:
        entityHash =
            LabelsMappingEntity.getVersioningCompositeIdString(id.getCompositeId(), id.getIdType());
        break;
      default:
        throw new StatusRuntimeException(io.grpc.Status.INTERNAL);
    }
    return entityHash;
  }

  @Override
  public boolean addLabels(IdentificationType id, List<String> labels) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      addLabels(session, id, labels);
      return true;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return addLabels(id, labels);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public boolean addProperty(IdentificationType id, String key, String value) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      addProperty(session, id, key, value);
      return true;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return addProperty(id, key, value);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public void addProperty(Session session, IdentificationType id, String key, String value) {
    Transaction transaction = session.beginTransaction();
    MetadataPropertyMappingEntity.LabelMappingId id0 =
        MetadataPropertyMappingEntity.createId(id, key);
    session.saveOrUpdate(new MetadataPropertyMappingEntity(id0, value));
    transaction.commit();
  }

  @Override
  public void addLabels(Session session, IdentificationType id, List<String> labels)
      throws ModelDBException {
    Transaction transaction = session.beginTransaction();
    for (String label : labels) {
      LabelsMappingEntity.LabelMappingId id0 = LabelsMappingEntity.createId(id, label);
      LabelsMappingEntity existingLabelsMappingEntity = session.get(LabelsMappingEntity.class, id0);
      if (existingLabelsMappingEntity == null) {
        session.save(new LabelsMappingEntity(id0));
      } else {
        Status status =
            Status.newBuilder()
                .setCode(Code.ALREADY_EXISTS_VALUE)
                .setMessage("Label '" + label + "' already exists with given ID")
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    }
    transaction.commit();
  }

  @Override
  public List<String> getLabels(IdentificationType id) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      return getLabels(session, id);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getLabels(id);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<String> getLabels(Session session, IdentificationType id) throws ModelDBException {
    Query<LabelsMappingEntity> query =
        session.createQuery(GET_LABELS_HQL, LabelsMappingEntity.class);
    query.setParameter("entityHash", getEntityHash(id));
    query.setParameter("entityType", id.getIdTypeValue());
    return query.list().stream().map(LabelsMappingEntity::getValue).collect(Collectors.toList());
  }

  @Override
  public String getProperty(IdentificationType id, String key) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      return getProperty(session, id, key);
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getProperty(id, key);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public String getProperty(Session session, IdentificationType id, String key) {
    Query<MetadataPropertyMappingEntity> query =
        session.createQuery(GET_PROPERTY_HQL, MetadataPropertyMappingEntity.class);
    VersioningCompositeIdentifier compositeId = id.getCompositeId();
    query.setParameter("repositoryId", compositeId.getRepoId());
    query.setParameter("commitSha", compositeId.getCommitHash());
    query.setParameter("location", ModelDBUtils.getJoinedLocation(compositeId.getLocationList()));
    query.setParameter("key", key);
    return query.uniqueResultOptional().map(MetadataPropertyMappingEntity::getValue).orElse(null);
  }

  @Override
  public boolean deleteLabels(IdentificationType id, List<String> labels) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      deleteLabels(session, id, labels);
      return true;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteLabels(id, labels);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public void deleteLabels(Session session, IdentificationType id, List<String> labels) {
    Transaction transaction = session.beginTransaction();

    for (String label : labels) {
      LabelsMappingEntity.LabelMappingId id0 = LabelsMappingEntity.createId(id, label);
      LabelsMappingEntity existingLabelsMappingEntity = session.get(LabelsMappingEntity.class, id0);
      if (existingLabelsMappingEntity != null) {
        session.delete(existingLabelsMappingEntity);
      } else {
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage("Label '" + label + "' not found in DB")
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    }
    transaction.commit();
  }

  @Override
  public boolean deleteProperty(IdentificationType id, String key) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();

      MetadataPropertyMappingEntity.LabelMappingId id0 =
          MetadataPropertyMappingEntity.createId(id, key);
      MetadataPropertyMappingEntity existingMetadataMappingEntity =
          session.get(MetadataPropertyMappingEntity.class, id0);
      if (existingMetadataMappingEntity != null) {
        session.delete(existingMetadataMappingEntity);
      } else {
        Status status =
            Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage("Label '" + key + "' not found in DB")
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
      transaction.commit();
      return true;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteProperty(id, key);
      } else {
        throw ex;
      }
    }
  }
}
