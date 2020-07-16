package ai.verta.modeldb.metadata;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.metadata.LabelsMappingEntity;
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
  private static final String GET_LABEL_IDS_HQL =
      new StringBuilder("From LabelsMappingEntity lm where lm.id.")
          .append(ModelDBConstants.LABEL)
          .append(" IN (:labels) ")
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

  private String getEntityHash(IdentificationType id) {
    String entityHash;
    switch (id.getIdCase()) {
      case INT_ID:
        entityHash = String.valueOf(id.getIntId());
        break;
      case STRING_ID:
        entityHash = id.getStringId();
        break;
      default:
        throw new StatusRuntimeException(io.grpc.Status.INTERNAL);
    }
    return entityHash;
  }

  @Override
  public boolean addLabels(IdentificationType id, List<String> labels) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      addLabels(session, id, labels);
      transaction.commit();
      return true;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return addLabels(id, labels);
      } else {
        throw ex;
      }
    }
  }

  private void addLabels(Session session, IdentificationType id, List<String> labels) {
    for (String label : labels) {
      LabelsMappingEntity labelsMappingEntity = new LabelsMappingEntity(id, label);
      LabelsMappingEntity existingLabelsMappingEntity =
          session.get(LabelsMappingEntity.class, labelsMappingEntity.getId());
      if (existingLabelsMappingEntity == null) {
        session.save(labelsMappingEntity);
      } else {
        Status status =
            Status.newBuilder()
                .setCode(Code.ALREADY_EXISTS_VALUE)
                .setMessage("Label '" + label + "' already exists with given ID")
                .build();
        throw StatusProto.toStatusRuntimeException(status);
      }
    }
  }

  @Override
  public boolean updateLabels(IdentificationType id, List<String> labels) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      deleteLabels(session, id, labels, false);
      addLabels(session, id, labels);
      transaction.commit();
      return true;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return updateLabels(id, labels);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<String> getLabels(IdentificationType id) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query query = session.createQuery(GET_LABELS_HQL);
      query.setParameter("entityHash", getEntityHash(id));
      query.setParameter("entityType", id.getIdTypeValue());
      List<LabelsMappingEntity> labelsMappingEntities = query.list();
      return labelsMappingEntities.stream()
          .map(labelsMappingEntity -> labelsMappingEntity.getId().getLabel())
          .collect(Collectors.toList());
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getLabels(id);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public List<IdentificationType> getLabelIds(List<String> labels) throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Query<LabelsMappingEntity> query =
          session.createQuery(
              GET_LABEL_IDS_HQL + " ORDER BY lm.id.label", LabelsMappingEntity.class);
      query.setParameterList("labels", labels);
      List<LabelsMappingEntity> labelsMappingEntities = query.list();
      return labelsMappingEntities.stream()
          .map(
              labelsMappingEntity -> {
                IdentificationType.Builder builder =
                    IdentificationType.newBuilder()
                        .setIdType(
                            IDTypeEnum.IDType.forNumber(
                                labelsMappingEntity.getId().getEntity_type()));
                switch (builder.getIdType()) {
                  case VERSIONING_REPOSITORY:
                    builder.setIntId(Long.parseLong(labelsMappingEntity.getId().getEntity_hash()));
                  case VERSIONING_COMMIT:
                  default:
                    builder.setStringId(labelsMappingEntity.getId().getEntity_hash());
                }
                return builder.build();
              })
          .collect(Collectors.toList());
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getLabelIds(labels);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public boolean deleteLabels(IdentificationType id, List<String> labels, boolean deleteAll) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();
      deleteLabels(session, id, labels, deleteAll);
      transaction.commit();
      return true;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteLabels(id, labels, deleteAll);
      } else {
        throw ex;
      }
    }
  }

  private void deleteLabels(
      Session session, IdentificationType id, List<String> labels, boolean deleteAll) {
    if (deleteAll) {
      StringBuilder stringQueryBuilder =
          new StringBuilder("delete from ")
              .append(LabelsMappingEntity.class.getSimpleName())
              .append(" lm WHERE ");
      stringQueryBuilder
          .append(" lm.")
          .append(ModelDBConstants.ID)
          .append(".")
          .append(ModelDBConstants.ENTITY_HASH)
          .append(" = :")
          .append(ModelDBConstants.ENTITY_HASH)
          .append(" AND lm.id.")
          .append(ModelDBConstants.ENTITY_TYPE)
          .append(" = :")
          .append(ModelDBConstants.ENTITY_TYPE);
      Query<LabelsMappingEntity> query = session.createQuery(stringQueryBuilder.toString());
      query.setParameter(ModelDBConstants.ENTITY_HASH, getEntityHash(id));
      query.setParameter(ModelDBConstants.ENTITY_TYPE, id.getIdTypeValue());
      query.executeUpdate();
    } else {
      for (String label : labels) {
        LabelsMappingEntity labelsMappingEntity = new LabelsMappingEntity(id, label);
        LabelsMappingEntity existingLabelsMappingEntity =
            session.get(LabelsMappingEntity.class, labelsMappingEntity.getId());
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
    }
  }
}
