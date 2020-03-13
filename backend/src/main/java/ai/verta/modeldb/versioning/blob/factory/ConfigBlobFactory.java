package ai.verta.modeldb.versioning.blob.factory;

import static ai.verta.modeldb.entities.config.ConfigBlobEntity.HYPERPARAMETER;
import static ai.verta.modeldb.entities.config.ConfigBlobEntity.HYPERPARAMETER_SET;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.config.ConfigBlobEntity;
import ai.verta.modeldb.entities.config.HyperparameterElementConfigBlobEntity;
import ai.verta.modeldb.entities.config.HyperparameterSetConfigBlobEntity;
import ai.verta.modeldb.entities.versioning.InternalFolderElementEntity;
import ai.verta.modeldb.versioning.Blob;
import ai.verta.modeldb.versioning.ConfigBlob;
import ai.verta.modeldb.versioning.HyperparameterConfigBlob;
import io.grpc.Status;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class ConfigBlobFactory extends BlobFactory {

  ConfigBlobFactory(InternalFolderElementEntity internalFolderElementEntity) {
    super(
        internalFolderElementEntity.getElement_type(),
        internalFolderElementEntity.getElement_sha());
  }

  @Override
  public Blob getBlob(Session session) throws ModelDBException {
    String configQuery =
        "From "
            + ConfigBlobEntity.class.getSimpleName()
            + " where blob_hash = :blobHash ORDER BY config_seq_number ASC";
    Query<ConfigBlobEntity> query = session.createQuery(configQuery);
    query.setParameter("blobHash", getElementSha());
    List<ConfigBlobEntity> configBlobEntities = query.list();
    ConfigBlob.Builder configBlobBuilder = ConfigBlob.newBuilder();
    for (ConfigBlobEntity configBlobEntity : configBlobEntities) {
      switch (getElementType()) {
        case CONFIG_BLOB:
          switch (configBlobEntity.getHyperparameter_type()) {
            case HYPERPARAMETER:
              HyperparameterElementConfigBlobEntity elementConfigBlobEntity =
                  session.get(
                      HyperparameterElementConfigBlobEntity.class,
                      configBlobEntity.getComponentBlobHash());
              HyperparameterConfigBlob hyperparameterConfigBlob =
                  HyperparameterConfigBlob.newBuilder()
                      .setName(elementConfigBlobEntity.getName())
                      .setValue(elementConfigBlobEntity.toProto())
                      .build();
              configBlobBuilder.addHyperparameters(hyperparameterConfigBlob);
              break;
            case HYPERPARAMETER_SET:
              HyperparameterSetConfigBlobEntity setConfigBlobEntity =
                  session.get(
                      HyperparameterSetConfigBlobEntity.class,
                      configBlobEntity.getComponentBlobHash());
              configBlobBuilder.addHyperparameterSet(setConfigBlobEntity.toProto());
              break;
            default:
              throw new ModelDBException("Unknown blob type found", Status.Code.INTERNAL);
          }
          break;
        default:
          throw new ModelDBException("Unknown blob type found", Status.Code.INTERNAL);
      }
    }
    return Blob.newBuilder().setConfig(configBlobBuilder.build()).build();
  }
}
