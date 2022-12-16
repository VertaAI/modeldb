package ai.verta.modeldb.versioning.blob.factory;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.entities.dataset.PathDatasetComponentBlobEntity;
import ai.verta.modeldb.entities.dataset.QueryDatasetComponentBlobEntity;
import ai.verta.modeldb.entities.dataset.S3DatasetComponentBlobEntity;
import ai.verta.modeldb.entities.versioning.InternalFolderElementEntity;
import ai.verta.modeldb.versioning.Blob;
import ai.verta.modeldb.versioning.DatasetBlob;
import ai.verta.modeldb.versioning.PathDatasetBlob;
import ai.verta.modeldb.versioning.PathDatasetComponentBlob;
import ai.verta.modeldb.versioning.QueryDatasetBlob;
import ai.verta.modeldb.versioning.QueryDatasetComponentBlob;
import ai.verta.modeldb.versioning.S3DatasetBlob;
import ai.verta.modeldb.versioning.S3DatasetComponentBlob;
import io.grpc.Status.Code;
import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class DatasetBlobFactory extends BlobFactory {

  private static final String BLOB_SHAS_QUERY_PARAM = "blobShas";

  DatasetBlobFactory(InternalFolderElementEntity internalFolderElementEntity) {
    super(
        internalFolderElementEntity.getElement_type(),
        internalFolderElementEntity.getElement_sha());
  }

  @Override
  public Blob getBlob(Session session) throws ModelDBException {
    var datasetBlobBuilder = DatasetBlob.newBuilder();
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
      case QUERY_DATASET_BLOB:
        final QueryDatasetBlob queryBlob = getQueryBlob(session, getElementSha());
        if (queryBlob == null) {
          throw new ModelDBException("Query blob not found", Code.INTERNAL);
        }
        datasetBlobBuilder.setQuery(queryBlob);
        break;
      default:
        // Do nothing
        break;
    }
    return Blob.newBuilder().setDataset(datasetBlobBuilder).build();
  }

  private static S3DatasetBlob getS3Blob(Session session, String blobHash) throws ModelDBException {
    var s3ComponentQueryHQL =
        "From S3DatasetComponentBlobEntity s3 WHERE s3.id.s3_dataset_blob_id = :blobShas";

    Query<S3DatasetComponentBlobEntity> s3ComponentQuery = session.createQuery(s3ComponentQueryHQL);
    s3ComponentQuery.setParameter(BLOB_SHAS_QUERY_PARAM, blobHash);
    List<S3DatasetComponentBlobEntity> datasetComponentBlobEntities = s3ComponentQuery.list();

    if (datasetComponentBlobEntities != null && datasetComponentBlobEntities.size() > 0) {
      List<S3DatasetComponentBlob> componentBlobs =
          datasetComponentBlobEntities.stream()
              .map(S3DatasetComponentBlobEntity::toProto)
              .collect(Collectors.toList());
      return S3DatasetBlob.newBuilder().addAllComponents(componentBlobs).build();
    } else {
      throw new ModelDBException("S3 dataset Blob not found", Code.NOT_FOUND);
    }
  }

  static PathDatasetBlob getPathBlob(Session session, String blobHash) {
    var pathComponentQueryHQL =
        "From PathDatasetComponentBlobEntity p WHERE p.id.path_dataset_blob_id = :blobShas";

    Query<PathDatasetComponentBlobEntity> pathComponentQuery =
        session.createQuery(pathComponentQueryHQL);
    pathComponentQuery.setParameter(BLOB_SHAS_QUERY_PARAM, blobHash);
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

  static QueryDatasetBlob getQueryBlob(Session session, String blobHash) {
    var pathComponentQueryHQL =
        "From QueryDatasetComponentBlobEntity q WHERE q.id.query_dataset_blob_id = :blobShas";

    Query<QueryDatasetComponentBlobEntity> queryComponentQuery =
        session.createQuery(pathComponentQueryHQL);
    queryComponentQuery.setParameter(BLOB_SHAS_QUERY_PARAM, blobHash);
    List<QueryDatasetComponentBlobEntity> queryDatasetComponentBlobEntities =
        queryComponentQuery.list();

    if (queryDatasetComponentBlobEntities != null && queryDatasetComponentBlobEntities.size() > 0) {
      List<QueryDatasetComponentBlob> componentBlobs =
          queryDatasetComponentBlobEntities.stream()
              .map(QueryDatasetComponentBlobEntity::toProto)
              .collect(Collectors.toList());
      return QueryDatasetBlob.newBuilder().addAllComponents(componentBlobs).build();
    } else {
      return null;
    }
  }
}
