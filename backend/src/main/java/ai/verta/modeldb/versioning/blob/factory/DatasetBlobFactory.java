package ai.verta.modeldb.versioning.blob.factory;

import ai.verta.common.KeyValue;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.dataset.PathDatasetComponentBlobEntity;
import ai.verta.modeldb.entities.dataset.S3DatasetComponentBlobEntity;
import ai.verta.modeldb.entities.versioning.InternalFolderElementEntity;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.modeldb.versioning.Blob;
import ai.verta.modeldb.versioning.DatasetBlob;
import ai.verta.modeldb.versioning.PathDatasetBlob;
import ai.verta.modeldb.versioning.PathDatasetComponentBlob;
import ai.verta.modeldb.versioning.S3DatasetBlob;
import ai.verta.modeldb.versioning.S3DatasetComponentBlob;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Status;
import io.grpc.Status.Code;
import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class DatasetBlobFactory extends BlobFactory {

  DatasetBlobFactory(InternalFolderElementEntity internalFolderElementEntity) {
    super(
        internalFolderElementEntity.getElement_type(),
        internalFolderElementEntity.getElement_sha());
  }

  @Override
  public Blob getBlob(Session session) throws ModelDBException {
    DatasetBlob.Builder datasetBlobBuilder = DatasetBlob.newBuilder();
    switch (getElementType()) {
      case S_3_DATASET_BLOB:
        datasetBlobBuilder.setS3(getS3Blob(session, getElementSha()));
        break;
      case PATH_DATASET_BLOB:
        final PathDatasetBlob pathBlob = getPathBlob(session, getElementSha());
        if (pathBlob == null) {
          throw new ModelDBException("Path blob not found", Code.INTERNAL);
        }
        datasetBlobBuilder.setPath(pathBlob);
        break;
    }
    Blob.Builder blobBuilder = Blob.newBuilder().setDataset(datasetBlobBuilder);
    try {
      blobBuilder.addAllAttributes(getAttributes(session));
    } catch (InvalidProtocolBufferException e) {
      throw new ModelDBException(e);
    }
    return blobBuilder.build();
  }

  private static S3DatasetBlob getS3Blob(Session session, String blobHash) throws ModelDBException {
    String s3ComponentQueryHQL =
        "From "
            + S3DatasetComponentBlobEntity.class.getSimpleName()
            + " s3 WHERE s3.id.s3_dataset_blob_id = :blobShas";

    Query<S3DatasetComponentBlobEntity> s3ComponentQuery = session.createQuery(s3ComponentQueryHQL);
    s3ComponentQuery.setParameter("blobShas", blobHash);
    List<S3DatasetComponentBlobEntity> datasetComponentBlobEntities = s3ComponentQuery.list();

    if (datasetComponentBlobEntities != null && datasetComponentBlobEntities.size() > 0) {
      List<S3DatasetComponentBlob> componentBlobs =
          datasetComponentBlobEntities.stream()
              .map(S3DatasetComponentBlobEntity::toProto)
              .collect(Collectors.toList());
      return S3DatasetBlob.newBuilder().addAllComponents(componentBlobs).build();
    } else {
      throw new ModelDBException("S3 dataset Blob not found", Status.Code.NOT_FOUND);
    }
  }

  static PathDatasetBlob getPathBlob(Session session, String blobHash) {
    String pathComponentQueryHQL =
        "From "
            + PathDatasetComponentBlobEntity.class.getSimpleName()
            + " p WHERE p.id.path_dataset_blob_id = :blobShas";

    Query<PathDatasetComponentBlobEntity> pathComponentQuery =
        session.createQuery(pathComponentQueryHQL);
    pathComponentQuery.setParameter("blobShas", blobHash);
    List<PathDatasetComponentBlobEntity> pathDatasetComponentBlobEntities =
        pathComponentQuery.list();

    if (pathDatasetComponentBlobEntities != null && pathDatasetComponentBlobEntities.size() > 0) {
      List<PathDatasetComponentBlob> componentBlobs =
          pathDatasetComponentBlobEntities.stream()
              .map(PathDatasetComponentBlobEntity::toProto)
              .collect(Collectors.toList());
      return PathDatasetBlob.newBuilder().addAllComponents(componentBlobs).build();
    } else {
      return null;
    }
  }

  private List<KeyValue> getAttributes(Session session) throws InvalidProtocolBufferException {
    String getAttributesHQL =
        "From AttributeEntity kv where kv.entity_hash = :entityHash "
            + " AND kv.entity_name = :entityName AND kv.field_type = :fieldType";
    Query getQuery = session.createQuery(getAttributesHQL);
    getQuery.setParameter("entityName", ModelDBConstants.BLOB);
    getQuery.setParameter("entityHash", getElementSha());
    getQuery.setParameter("fieldType", ModelDBConstants.ATTRIBUTES);
    List<AttributeEntity> attributeEntities = getQuery.list();
    return RdbmsUtils.convertAttributeEntityListFromAttributes(attributeEntities);
  }
}
