package ai.verta.modeldb.versioning.blob.container;

import static ai.verta.modeldb.versioning.blob.factory.BlobFactory.CONFIG_BLOB;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.config.ConfigBlobEntity;
import ai.verta.modeldb.entities.config.HyperparameterElementConfigBlobEntity;
import ai.verta.modeldb.entities.config.HyperparameterSetConfigBlobEntity;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.ConfigBlob;
import ai.verta.modeldb.versioning.ContinuousHyperparameterSetConfigBlob;
import ai.verta.modeldb.versioning.DiscreteHyperparameterSetConfigBlob;
import ai.verta.modeldb.versioning.FileHasher;
import ai.verta.modeldb.versioning.HyperparameterConfigBlob;
import ai.verta.modeldb.versioning.HyperparameterSetConfigBlob;
import ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob;
import ai.verta.modeldb.versioning.HyperparameterValuesConfigBlob.ValueCase;
import ai.verta.modeldb.versioning.TreeElem;
import io.grpc.Status.Code;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;

public class ConfigContainer extends BlobContainer {

  private final ConfigBlob config;

  public ConfigContainer(BlobExpanded blobExpanded) {
    super(blobExpanded);
    config = blobExpanded.getBlob().getConfig();
  }

  @Override
  public void validate() throws ModelDBException {
    for (HyperparameterConfigBlob hyperparameterConfigBlob : config.getHyperparametersList()) {
      final String name = hyperparameterConfigBlob.getName();
      if (name.isEmpty()) {
        throw new ModelDBException("Hyperparameter name is empty", Code.INVALID_ARGUMENT);
      }
      final HyperparameterValuesConfigBlob value = hyperparameterConfigBlob.getValue();
      validate(name, value);
    }
    for (HyperparameterSetConfigBlob hyperparameterSetConfigBlob :
        config.getHyperparameterSetList()) {
      final String name = hyperparameterSetConfigBlob.getName();
      if (name.isEmpty()) {
        throw new ModelDBException("Hyperparameter set name is empty", Code.INVALID_ARGUMENT);
      }
      switch (hyperparameterSetConfigBlob.getValueCase()) {
        case CONTINUOUS:
          final ContinuousHyperparameterSetConfigBlob continuous =
              hyperparameterSetConfigBlob.getContinuous();
          if (!continuous.hasIntervalBegin()) {
            throw new ModelDBException(
                "Hyperparameter set " + name + " doesn't have interval begin",
                Code.INVALID_ARGUMENT);
          }
          if (!continuous.hasIntervalEnd()) {
            throw new ModelDBException(
                "Hyperparameter set " + name + " doesn't have interval end", Code.INVALID_ARGUMENT);
          }
          if (!continuous.hasIntervalStep()) {
            throw new ModelDBException(
                "Hyperparameter set " + name + " doesn't have interval step",
                Code.INVALID_ARGUMENT);
          }

          HyperparameterValuesConfigBlob beginSetConfigBlob = continuous.getIntervalBegin();
          HyperparameterValuesConfigBlob endSetConfigBlob = continuous.getIntervalEnd();
          HyperparameterValuesConfigBlob stepSetConfigBlob = continuous.getIntervalStep();

          if (beginSetConfigBlob.getValueCase().equals(ValueCase.VALUE_NOT_SET)
              || endSetConfigBlob.getValueCase().equals(ValueCase.VALUE_NOT_SET)
              || stepSetConfigBlob.getValueCase().equals(ValueCase.VALUE_NOT_SET)) {
            throw new ModelDBException(
                "Hyperparameter continuous set doesn't have one of the INT_VALUE, FLOAT_VALUE, STRING_VALUE",
                Code.INVALID_ARGUMENT);
          }

          if (beginSetConfigBlob.getValueCase().equals(ValueCase.STRING_VALUE)) {
            try {
              Double.parseDouble(beginSetConfigBlob.getStringValue());
            } catch (Exception ex) {
              throw new ModelDBException(
                  "beginSetConfigBlob has a STRING_VALUE which is not in a valid numeric notation");
            }
          }
          if (endSetConfigBlob.getValueCase().equals(ValueCase.STRING_VALUE)) {
            try {
              Double.parseDouble(endSetConfigBlob.getStringValue());
            } catch (Exception ex) {
              throw new ModelDBException(
                  "endSetConfigBlob has a STRING_VALUE which is not in a valid numeric notation");
            }
          }
          if (stepSetConfigBlob.getValueCase().equals(ValueCase.STRING_VALUE)) {
            try {
              Double.parseDouble(stepSetConfigBlob.getStringValue());
            } catch (Exception ex) {
              throw new ModelDBException(
                  "stepSetConfigBlob has a STRING_VALUE which is not in a valid numeric notation");
            }
          }

          validate(name, continuous.getIntervalBegin());
          validate(name, continuous.getIntervalEnd());
          validate(name, continuous.getIntervalStep());
          break;
        case DISCRETE:
          DiscreteHyperparameterSetConfigBlob discrete = hyperparameterSetConfigBlob.getDiscrete();
          if (discrete.getValuesCount() == 0) {
            throw new ModelDBException("No values for set " + name + " found");
          }
          for (HyperparameterValuesConfigBlob value : discrete.getValuesList()) {
            validate(name, value);
          }
          break;
        case VALUE_NOT_SET:
        default:
          throw new ModelDBException(
              "Hyperparameter set " + name + " value has unknown type", Code.INVALID_ARGUMENT);
      }
    }
  }

  void validate(String name, HyperparameterValuesConfigBlob value) throws ModelDBException {
    switch (value.getValueCase()) {
      case INT_VALUE:
      case FLOAT_VALUE:
      case STRING_VALUE:
        break;
      case VALUE_NOT_SET:
      default:
        throw new ModelDBException("Hyperparameter " + name + " value has unknown type");
    }
  }

  @Override
  public void process(Session session, TreeElem rootTree, FileHasher fileHasher)
      throws NoSuchAlgorithmException, ModelDBException {
    List<ConfigBlobEntity> hyperparameterBlobEntities = new LinkedList<>();
    List<ConfigBlobEntity> hyperparameterSetBlobEntities = new LinkedList<>();
    Map<String, HyperparameterSetConfigBlobEntity> setConfigEntities = new HashMap<>();
    Map<String, HyperparameterElementConfigBlobEntity> valueEntities = new HashMap<>();
    int index = 0;
    for (HyperparameterConfigBlob hyperparameterConfigBlob : config.getHyperparametersList()) {
      final String name = hyperparameterConfigBlob.getName();
      final HyperparameterValuesConfigBlob value = hyperparameterConfigBlob.getValue();
      final HyperparameterElementConfigBlobEntity hyperparameterElementConfigBlobEntity =
          getValueBlob(session, name, value, valueEntities);
      final String valueBlobHash = hyperparameterElementConfigBlobEntity.getBlobHash();
      ConfigBlobEntity configBlobEntity =
          new ConfigBlobEntity(valueBlobHash, index++, hyperparameterElementConfigBlobEntity);
      hyperparameterBlobEntities.add(configBlobEntity);
    }
    for (HyperparameterSetConfigBlob hyperparameterSetConfigBlob :
        config.getHyperparameterSetList()) {
      final String name = hyperparameterSetConfigBlob.getName();
      final String hyperparameterSetBlobHash;
      HyperparameterSetConfigBlobEntity hyperparameterSetConfigBlobEntity;
      switch (hyperparameterSetConfigBlob.getValueCase()) {
        case CONTINUOUS:
          final ContinuousHyperparameterSetConfigBlob continuous =
              hyperparameterSetConfigBlob.getContinuous();
          HyperparameterElementConfigBlobEntity hyperparameterElementConfigBlobEntityBegin =
              getValueBlob(session, "", continuous.getIntervalBegin(), valueEntities);
          HyperparameterElementConfigBlobEntity hyperparameterElementConfigBlobEntityEnd =
              getValueBlob(session, "", continuous.getIntervalEnd(), valueEntities);
          HyperparameterElementConfigBlobEntity hyperparameterElementConfigBlobEntityStep =
              getValueBlob(session, "", continuous.getIntervalStep(), valueEntities);
          hyperparameterSetBlobHash =
              computeContinuousSHA(
                  name,
                  hyperparameterElementConfigBlobEntityBegin,
                  hyperparameterElementConfigBlobEntityEnd,
                  hyperparameterElementConfigBlobEntityStep);
          hyperparameterSetConfigBlobEntity =
              getHyperparameterSetConfigBlobEntity(
                  session,
                  setConfigEntities,
                  hyperparameterSetConfigBlob,
                  hyperparameterSetBlobHash);
          hyperparameterSetConfigBlobEntity.setInterval_begin_hash(
              hyperparameterElementConfigBlobEntityBegin);
          hyperparameterSetConfigBlobEntity.setInterval_end_hash(
              hyperparameterElementConfigBlobEntityEnd);
          hyperparameterSetConfigBlobEntity.setInterval_step_hash(
              hyperparameterElementConfigBlobEntityStep);

          break;
        case DISCRETE:
          DiscreteHyperparameterSetConfigBlob discrete = hyperparameterSetConfigBlob.getDiscrete();
          Map<String, HyperparameterElementConfigBlobEntity>
              hyperparameterElementConfigBlobEntitySet = new HashMap<>();
          for (HyperparameterValuesConfigBlob hyperparameterValuesConfigBlob :
              discrete.getValuesList()) {
            HyperparameterElementConfigBlobEntity valueBlobEntity =
                getValueBlob(session, "", hyperparameterValuesConfigBlob, valueEntities);
            hyperparameterElementConfigBlobEntitySet.put(
                valueBlobEntity.getBlobHash(), valueBlobEntity);
          }
          hyperparameterSetBlobHash =
              computeContinuousSHA(name, hyperparameterElementConfigBlobEntitySet);
          hyperparameterSetConfigBlobEntity =
              getHyperparameterSetConfigBlobEntity(
                  session,
                  setConfigEntities,
                  hyperparameterSetConfigBlob,
                  hyperparameterSetBlobHash);

          hyperparameterSetConfigBlobEntity.setHyperparameterSetConfigElementMapping(
              hyperparameterElementConfigBlobEntitySet.values());
          hyperparameterElementConfigBlobEntitySet.values().stream()
              .map(
                  hyperparameterElementConfigBlobEntity ->
                      "value:" + hyperparameterElementConfigBlobEntity.getBlobHash())
              .reduce((s, s2) -> s + ":" + s2)
              .orElseThrow(() -> new ModelDBException("Empty set found"));
          break;
        default:
          throw new ModelDBException(
              "Hyperparameter set " + name + " value has unknown type", Code.INTERNAL);
      }
      ConfigBlobEntity configBlobEntity =
          new ConfigBlobEntity(
              hyperparameterSetBlobHash, index++, hyperparameterSetConfigBlobEntity);
      hyperparameterSetBlobEntities.add(configBlobEntity);
    }
    String result =
        FileHasher.getSha(
            "config:elemenets:"
                + toString(hyperparameterBlobEntities)
                + ":element_sets:"
                + toString(hyperparameterSetBlobEntities));
    hyperparameterBlobEntities.forEach(
        configBlobEntity -> {
          configBlobEntity.setBlobHash(result);
          session.saveOrUpdate(configBlobEntity);
        });
    hyperparameterSetBlobEntities.forEach(
        configBlobEntity -> {
          configBlobEntity.setBlobHash(result);
          session.saveOrUpdate(configBlobEntity);
        });

    rootTree.push(getLocationList(), result, CONFIG_BLOB);
  }

  private String computeContinuousSHA(
      String name,
      Map<String, HyperparameterElementConfigBlobEntity> hyperparameterElementConfigBlobEntityMap)
      throws NoSuchAlgorithmException {
    StringBuilder sb = new StringBuilder("name:" + name);
    for (HyperparameterElementConfigBlobEntity hyperparameterElementConfigBlobEntity :
        hyperparameterElementConfigBlobEntityMap.values()) {
      sb.append(":entry:").append(hyperparameterElementConfigBlobEntity.getBlobHash());
    }
    return FileHasher.getSha(sb.toString());
  }

  HyperparameterSetConfigBlobEntity getHyperparameterSetConfigBlobEntity(
      Session session,
      Map<String, HyperparameterSetConfigBlobEntity> setConfigEntities,
      HyperparameterSetConfigBlob hyperparameterSetConfigBlob,
      String hyperparameterSetBlobHash)
      throws ModelDBException {
    HyperparameterSetConfigBlobEntity hyperparameterSetConfigBlobEntity =
        setConfigEntities.get(hyperparameterSetBlobHash);
    if (hyperparameterSetConfigBlobEntity == null) {
      hyperparameterSetConfigBlobEntity =
          new HyperparameterSetConfigBlobEntity(
              hyperparameterSetBlobHash, hyperparameterSetConfigBlob);
      setConfigEntities.put(hyperparameterSetBlobHash, hyperparameterSetConfigBlobEntity);
      session.saveOrUpdate(hyperparameterSetConfigBlobEntity);
    }
    return hyperparameterSetConfigBlobEntity;
  }

  private String toString(List<ConfigBlobEntity> hyperparameterBlobEntities) {
    return hyperparameterBlobEntities.stream()
        .map(
            configBlobEntity ->
                "seq_number:"
                    + configBlobEntity.getConfigSeqNumber().toString()
                    + ":value:"
                    + configBlobEntity.getComponentBlobHash())
        .reduce((s, s2) -> s + ":" + s2)
        .orElse("");
  }

  private String computeContinuousSHA(
      String name,
      HyperparameterElementConfigBlobEntity hyperparameterElementConfigBlobEntityBegin,
      HyperparameterElementConfigBlobEntity hyperparameterElementConfigBlobEntityEnd,
      HyperparameterElementConfigBlobEntity hyperparameterElementConfigBlobEntityStep)
      throws NoSuchAlgorithmException {
    final String payload =
        "name:"
            + name
            + ":begin:"
            + hyperparameterElementConfigBlobEntityBegin.getBlobHash()
            + ":end:"
            + hyperparameterElementConfigBlobEntityEnd.getBlobHash()
            + ":step:"
            + hyperparameterElementConfigBlobEntityStep.getBlobHash();
    return FileHasher.getSha(payload);
  }

  private HyperparameterElementConfigBlobEntity getValueBlob(
      Session session,
      String name,
      HyperparameterValuesConfigBlob hyperparameterValuesConfigBlob,
      Map<String, HyperparameterElementConfigBlobEntity> valueEntities)
      throws NoSuchAlgorithmException, ModelDBException {
    final String blobHash =
        FileHasher.getSha("name:" + name + ":value:" + computeSHA(hyperparameterValuesConfigBlob));
    HyperparameterElementConfigBlobEntity entity = valueEntities.get(blobHash);
    if (entity == null) {
      entity =
          new HyperparameterElementConfigBlobEntity(
              blobHash, null, name, hyperparameterValuesConfigBlob);
      valueEntities.put(blobHash, entity);
      session.saveOrUpdate(entity);
    }
    return entity;
  }

  private String computeSHA(HyperparameterValuesConfigBlob hyperparameterValuesConfigBlob)
      throws NoSuchAlgorithmException, ModelDBException {
    switch (hyperparameterValuesConfigBlob.getValueCase()) {
      case INT_VALUE:
        return FileHasher.getSha(String.valueOf(hyperparameterValuesConfigBlob.getIntValue()));
      case FLOAT_VALUE:
        return FileHasher.getSha(String.valueOf(hyperparameterValuesConfigBlob.getFloatValue()));
      case STRING_VALUE:
        return FileHasher.getSha(hyperparameterValuesConfigBlob.getStringValue());
      default:
        throw new ModelDBException("Unexpected wrong type", Code.INTERNAL);
    }
  }
}
