package ai.verta.modeldb.metadata;

import static ai.verta.modeldb.metadata.IDTypeEnum.IDType.VERSIONING_REPO_COMMIT_BLOB_DESCRIPTION;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.entities.metadata.LabelsMappingDescriptionEntity;
import ai.verta.modeldb.entities.metadata.LabelsMappingEntity;
import ai.verta.modeldb.entities.metadata.LabelsMappingEntityBase;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.blob.diff.Function4;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import java.util.List;
import java.util.function.Supplier;
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
  private static final String GET_LABELS_DESCRIPTION_HQL =
      new StringBuilder("From LabelsMappingDescriptionEntity lm where lm.id.")
          .append("repositoryId")
          .append(" = :repositoryId")
          .append(" AND lm.id.")
          .append("commitSha")
          .append(" = :commitSha")
          .append(" AND lm.id.")
          .append("location")
          .append(" = :location")
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
      processLabels(
          session,
          id,
          labels,
          (entity, existingEntity, label) -> {
            saveLabel(session, entity, existingEntity, label);
            return null;
          });
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

  private void processLabels(
      Session session,
      IdentificationType id,
      List<String> labels,
      Function4<Supplier<Object>, Object, String, Object> processLabel) {
    if (id.getIdType() == VERSIONING_REPO_COMMIT_BLOB_DESCRIPTION) {
      String label = labels.get(0);
      LabelsMappingDescriptionEntity.LabelMappingId id0 = LabelsMappingDescriptionEntity.createId(id);
      LabelsMappingEntity existingLabelsMappingEntity =
              session.get(LabelsMappingEntity.class, id0);
      processLabel.apply(() -> new LabelsMappingDescriptionEntity(id0, label), existingLabelsMappingEntity, label);
    } else {
      for (String label : labels) {
        LabelsMappingEntity.LabelMappingId id0 = LabelsMappingEntity.createId(id, label);
        LabelsMappingEntity existingLabelsMappingEntity =
            session.get(LabelsMappingEntity.class, id0);
        processLabel.apply(() -> new LabelsMappingEntity(id0), existingLabelsMappingEntity, label);
      }
    }
  }

  private <T> void saveLabel(
      Session session, Supplier<T> labelsMappingEntity, T existingLabelsMappingEntity, String label) {
    if (existingLabelsMappingEntity == null) {
      session.save(labelsMappingEntity.get());
    } else {
      Status status =
          Status.newBuilder()
              .setCode(Code.ALREADY_EXISTS_VALUE)
              .setMessage("Label '" + label + "' already exists with given ID")
              .build();
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  private <T> void deleteLabels(
      Session session, Supplier<T> labelsMappingEntity, T existingLabelsMappingEntity, String label) {
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

  @Override
  public List<String> getLabels(IdentificationType id) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      List<LabelsMappingEntityBase> labelsMappingEntities;
      if (id.getIdType() == VERSIONING_REPO_COMMIT_BLOB_DESCRIPTION) {
        Query<LabelsMappingDescriptionEntity> query =
            session.createQuery(GET_LABELS_DESCRIPTION_HQL, LabelsMappingDescriptionEntity.class);
        VersioningCompositeIdentifier compositeId = id.getCompositeId();
        query.setParameter("repositoryId", compositeId.getRepoId());
        query.setParameter("commitSha", compositeId.getCommitHash());
        query.setParameter(
            "location", ModelDBUtils.getJoinedLocation(compositeId.getLocationList()));
        labelsMappingEntities =
            query.list().stream()
                .map(e -> (LabelsMappingEntityBase) e)
                .collect(Collectors.toList());
      } else {
        Query<LabelsMappingEntity> query =
            session.createQuery(GET_LABELS_HQL, LabelsMappingEntity.class);
        query.setParameter("entityHash", getEntityHash(id));
        query.setParameter("entityType", id.getIdTypeValue());
        labelsMappingEntities =
            query.list().stream()
                .map(e -> (LabelsMappingEntityBase) e)
                .collect(Collectors.toList());
      }
      return labelsMappingEntities.stream()
          .map(LabelsMappingEntityBase::getLabel)
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
  public boolean deleteLabels(IdentificationType id, List<String> labels) {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      Transaction transaction = session.beginTransaction();

      processLabels(
          session,
          id,
          labels,
          (entity, existingEntity, label) -> {
            deleteLabels(session, entity, existingEntity, label);
            return null;
          });
      transaction.commit();
      return true;
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return deleteLabels(id, labels);
      } else {
        throw ex;
      }
    }
  }
}
