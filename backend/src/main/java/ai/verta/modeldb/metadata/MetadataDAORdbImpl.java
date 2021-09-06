package ai.verta.modeldb.metadata;

import ai.verta.common.OperatorEnum;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.entities.metadata.KeyValuePropertyMappingEntity;
import ai.verta.modeldb.entities.metadata.LabelsMappingEntity;
import ai.verta.modeldb.entities.metadata.MetadataPropertyMappingEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.modeldb.versioning.VersioningUtils;
import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class MetadataDAORdbImpl implements MetadataDAO {
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();

  private static final String GET_LABELS_HQL =
      "From LabelsMappingEntity lm where lm.id.entity_hash = :entityHash AND lm.id.entity_type = :entityType ORDER BY lm.id.label";
  private static final String GET_LABEL_IDS_HQL = "From LabelsMappingEntity lm where ";
  private static final String GET_PROPERTY_HQL =
      "From MetadataPropertyMappingEntity pm where pm.id.repositoryId = :repositoryId AND pm.id.commitSha = :commitSha AND pm.id.location = :location AND pm.id.key = :key";
  private static final String GET_KEY_VALUE_PROPERTY_HQL =
      "From KeyValuePropertyMappingEntity kv where kv.id.entity_hash = :entity_hash AND kv.id.property_name = :property_name";

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
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();
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

  @Override
  public boolean addProperty(IdentificationType id, String key, String value) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();
      addProperty(session, id, key, value);
      transaction.commit();
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
    MetadataPropertyMappingEntity.LabelMappingId id0 =
        MetadataPropertyMappingEntity.createId(id, key);
    MetadataPropertyMappingEntity existingEntity =
        session.get(MetadataPropertyMappingEntity.class, id0, LockMode.PESSIMISTIC_WRITE);
    if (existingEntity == null) {
      session.saveOrUpdate(new MetadataPropertyMappingEntity(id0, value));
    } else {
      existingEntity.setValue(value);
      session.update(existingEntity);
    }
  }

  @Override
  public void addLabels(Session session, IdentificationType id, List<String> labels) {
    for (String label : labels) {
      LabelsMappingEntity.LabelMappingId id0 = LabelsMappingEntity.createId(id, label);
      var existingLabelsMappingEntity = session.get(LabelsMappingEntity.class, id0);
      if (existingLabelsMappingEntity == null) {
        session.save(new LabelsMappingEntity(id0));
      }
    }
  }

  @Override
  public boolean updateLabels(IdentificationType id, List<String> labels) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();
      deleteLabels(session, id, labels, true);
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
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
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
  public List<String> getLabels(Session session, IdentificationType id) {
    List<LabelsMappingEntity> labelsMappingEntities = getLabelsMappingEntities(session, id);
    return labelsMappingEntities.stream()
        .map(LabelsMappingEntity::getValue)
        .collect(Collectors.toList());
  }

  private List<LabelsMappingEntity> getLabelsMappingEntities(
      Session session, IdentificationType id) {
    Query<LabelsMappingEntity> query =
        session.createQuery(GET_LABELS_HQL, LabelsMappingEntity.class);
    query.setParameter("entityHash", getEntityHash(id));
    query.setParameter("entityType", id.getIdTypeValue());
    return query.list();
  }

  @Override
  public List<IdentificationType> getLabelIds(List<String> labels, OperatorEnum.Operator operator) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {

      Map<String, Object> parametersMap = new HashMap<>();
      var queryBuilder = new StringBuilder(GET_LABEL_IDS_HQL);
      for (var index = 0; index < labels.size(); index++) {
        if (operator.equals(OperatorEnum.Operator.CONTAIN)
            || operator.equals(OperatorEnum.Operator.NOT_CONTAIN)) {
          queryBuilder.append(" lower(lm.id.").append(ModelDBConstants.LABEL).append(") ");
        } else {
          queryBuilder.append(" lm.id.").append(ModelDBConstants.LABEL);
        }
        RdbmsUtils.setValueWithOperatorInQuery(
            index, queryBuilder, operator, labels.get(index), parametersMap);
        if (index < labels.size() - 1) {
          queryBuilder.append(" AND ");
        }
      }

      queryBuilder.append(" ORDER BY lm.id.label ");
      Query<LabelsMappingEntity> query =
          session.createQuery(queryBuilder.toString(), LabelsMappingEntity.class);
      RdbmsUtils.setParameterInQuery(query, parametersMap);

      List<LabelsMappingEntity> labelsMappingEntities = query.list();
      return labelsMappingEntities.stream()
          .map(
              labelsMappingEntity -> {
                var builder =
                    IdentificationType.newBuilder()
                        .setIdType(
                            IDTypeEnum.IDType.forNumber(
                                labelsMappingEntity.getId().getEntity_type()));
                switch (builder.getIdType()) {
                  case VERSIONING_REPOSITORY:
                    builder.setIntId(Long.parseLong(labelsMappingEntity.getId().getEntity_hash()));
                    break;
                  case VERSIONING_COMMIT:
                  default:
                    builder.setStringId(labelsMappingEntity.getId().getEntity_hash());
                }
                return builder.build();
              })
          .collect(Collectors.toList());
    } catch (Exception ex) {
      if (ModelDBUtils.needToRetry(ex)) {
        return getLabelIds(labels, operator);
      } else {
        throw ex;
      }
    }
  }

  @Override
  public String getProperty(IdentificationType id, String key) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
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
    String[] compositeIdArr =
        VersioningUtils.getDatasetVersionBlobCompositeIdString(id.getStringId());
    Long repositoryId = Long.parseLong(compositeIdArr[0]);
    String commitSha = compositeIdArr[1];
    String location =
        ModelDBUtils.getJoinedLocation(
            ModelDBUtils.getLocationWithSplitSlashOperator(compositeIdArr[2]));
    query.setParameter("repositoryId", repositoryId);
    query.setParameter("commitSha", commitSha);
    query.setParameter("location", location);
    query.setParameter("key", key);
    return query.uniqueResultOptional().map(MetadataPropertyMappingEntity::getValue).orElse(null);
  }

  @Override
  public boolean deleteLabels(IdentificationType id, List<String> labels, boolean deleteAll) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();
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
      Query<LabelsMappingEntity> query =
          session
              .createQuery(stringQueryBuilder.toString())
              .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
      query.setParameter(ModelDBConstants.ENTITY_HASH, getEntityHash(id));
      query.setParameter(ModelDBConstants.ENTITY_TYPE, id.getIdTypeValue());
      query.executeUpdate();
    } else {
      for (String label : labels) {
        LabelsMappingEntity.LabelMappingId id0 = LabelsMappingEntity.createId(id, label);
        var existingLabelsMappingEntity =
            session.get(LabelsMappingEntity.class, id0, LockMode.PESSIMISTIC_WRITE);
        if (existingLabelsMappingEntity != null) {
          session.delete(existingLabelsMappingEntity);
        }
      }
    }
  }

  @Override
  public boolean deleteProperty(IdentificationType id, String key) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();

      MetadataPropertyMappingEntity.LabelMappingId id0 =
          MetadataPropertyMappingEntity.createId(id, key);
      MetadataPropertyMappingEntity existingMetadataMappingEntity =
          session.get(MetadataPropertyMappingEntity.class, id0, LockMode.PESSIMISTIC_WRITE);
      if (existingMetadataMappingEntity != null) {
        session.delete(existingMetadataMappingEntity);
      } else {
        throw new NotFoundException("Label '" + key + "' not found in DB");
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

  @Override
  public void addKeyValueProperties(AddKeyValuePropertiesRequest request) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      var transaction = session.beginTransaction();
      for (KeyValueStringProperty keyValue : request.getKeyValuePropertyList()) {
        KeyValuePropertyMappingEntity.KeyValuePropertyMappingId id0 =
            KeyValuePropertyMappingEntity.createId(
                request.getId(), keyValue.getKey(), request.getPropertyName());
        KeyValuePropertyMappingEntity existingEntity =
            session.get(KeyValuePropertyMappingEntity.class, id0);
        if (existingEntity == null) {
          existingEntity = new KeyValuePropertyMappingEntity(id0, keyValue.getValue());
          session.saveOrUpdate(existingEntity);
        } else {
          existingEntity.setValue(keyValue.getValue());
          session.update(existingEntity);
        }
      }
      transaction.commit();
    }
  }

  @Override
  public List<KeyValueStringProperty> getKeyValueProperties(GetKeyValuePropertiesRequest request) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      List<KeyValueStringProperty> keyValues = new ArrayList<>();
      if (request.getGetAll()) {
        Query<KeyValuePropertyMappingEntity> query =
            session.createQuery(GET_KEY_VALUE_PROPERTY_HQL, KeyValuePropertyMappingEntity.class);
        query.setParameter(ModelDBConstants.ENTITY_HASH, getEntityHash(request.getId()));
        query.setParameter(ModelDBConstants.PROPERTY_NAME, request.getPropertyName());
        List<KeyValuePropertyMappingEntity> kvMappings = query.list();
        if (!kvMappings.isEmpty()) {
          kvMappings.forEach(
              mappingEntity -> {
                keyValues.add(
                    KeyValueStringProperty.newBuilder()
                        .setKey(mappingEntity.getId().getKey())
                        .setValue(mappingEntity.getValue())
                        .build());
              });
        }
      } else {
        for (String key : request.getKeysList()) {
          KeyValuePropertyMappingEntity.KeyValuePropertyMappingId id0 =
              KeyValuePropertyMappingEntity.createId(
                  request.getId(), key, request.getPropertyName());
          KeyValuePropertyMappingEntity kvEntity =
              session.get(KeyValuePropertyMappingEntity.class, id0);
          if (kvEntity != null) {
            keyValues.add(
                KeyValueStringProperty.newBuilder()
                    .setKey(key)
                    .setValue(kvEntity.getValue())
                    .build());
          }
        }
      }
      return keyValues;
    }
  }

  @Override
  public void deleteKeyValueProperties(DeleteKeyValuePropertiesRequest request) {
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      if (request.getDeleteAll()) {
        StringBuilder stringQueryBuilder =
            new StringBuilder("delete from ")
                .append(KeyValuePropertyMappingEntity.class.getSimpleName())
                .append(" kv WHERE ");
        stringQueryBuilder
            .append(" kv.")
            .append(ModelDBConstants.ID)
            .append(".")
            .append(ModelDBConstants.ENTITY_HASH)
            .append(" = :")
            .append(ModelDBConstants.ENTITY_HASH)
            .append(" AND kv.id.")
            .append(ModelDBConstants.PROPERTY_NAME)
            .append(" = :")
            .append(ModelDBConstants.PROPERTY_NAME);
        Query<LabelsMappingEntity> query =
            session
                .createQuery(stringQueryBuilder.toString())
                .setLockOptions(new LockOptions().setLockMode(LockMode.PESSIMISTIC_WRITE));
        query.setParameter(ModelDBConstants.ENTITY_HASH, getEntityHash(request.getId()));
        query.setParameter(ModelDBConstants.PROPERTY_NAME, request.getPropertyName());
        query.executeUpdate();
      } else {
        for (String key : request.getKeysList()) {
          KeyValuePropertyMappingEntity.KeyValuePropertyMappingId id0 =
              KeyValuePropertyMappingEntity.createId(
                  request.getId(), key, request.getPropertyName());
          KeyValuePropertyMappingEntity kvEntity =
              session.get(KeyValuePropertyMappingEntity.class, id0, LockMode.PESSIMISTIC_WRITE);
          if (kvEntity != null) {
            session.delete(kvEntity);
          }
        }
      }
      session.getTransaction().commit();
    }
  }
}
