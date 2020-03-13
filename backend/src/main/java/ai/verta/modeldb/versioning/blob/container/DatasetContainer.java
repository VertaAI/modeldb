package ai.verta.modeldb.versioning.blob.container;

import static ai.verta.modeldb.versioning.blob.factory.BlobFactory.PATH_DATASET_BLOB;
import static ai.verta.modeldb.versioning.blob.factory.BlobFactory.S_3_DATASET_BLOB;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.dataset.PathDatasetComponentBlobEntity;
import ai.verta.modeldb.entities.dataset.S3DatasetComponentBlobEntity;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.DatasetBlob;
import ai.verta.modeldb.versioning.FileHasher;
import ai.verta.modeldb.versioning.PathDatasetBlob;
import ai.verta.modeldb.versioning.PathDatasetComponentBlob;
import ai.verta.modeldb.versioning.S3DatasetBlob;
import ai.verta.modeldb.versioning.S3DatasetComponentBlob;
import ai.verta.modeldb.versioning.TreeElem;
import io.grpc.Status.Code;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;

public class DatasetContainer extends BlobContainer {

  private final DatasetBlob dataset;

  public DatasetContainer(BlobExpanded blobExpanded) {
    super(blobExpanded);
    dataset = blobExpanded.getBlob().getDataset();
  }

  @Override
  public void validate() throws ModelDBException {
    switch (dataset.getContentCase()) {
      case S3:
        if (dataset.getS3().getComponentsCount() == 0) {
          throw new ModelDBException("Blob should not be empty", Code.INVALID_ARGUMENT);
        }
        for (S3DatasetComponentBlob component : dataset.getS3().getComponentsList()) {
          if (!component.hasPath()) {
            throw new ModelDBException("Blob path should not be empty", Code.INVALID_ARGUMENT);
          }
          validate(component.getPath());
        }
        break;
      case PATH:
        if (dataset.getPath().getComponentsCount() == 0) {
          throw new ModelDBException("Blob should not be empty", Code.INVALID_ARGUMENT);
        }
        validate(dataset.getPath());
        break;
      default:
        throw new ModelDBException("Blob unknown type", Code.INVALID_ARGUMENT);
    }
  }

  @Override
  public void process(Session session, TreeElem rootTree, FileHasher fileHasher)
      throws NoSuchAlgorithmException, ModelDBException {
    final List<String> locationList = getLocationList();
    String blobType = getBlobType();

    String blobHash;
    switch (dataset.getContentCase()) {
      case S3:
        final S3DatasetBlob s3 = dataset.getS3();

        // sorted
        Map<String, List<S3DatasetComponentBlob>> componentHashes = new LinkedHashMap<>();
        for (S3DatasetComponentBlob componentBlob : s3.getComponentsList()) {
          final String componentHash = computeSHA(componentBlob);
          componentHashes.putIfAbsent(componentHash, new LinkedList<>());
          componentHashes.get(componentHash).add(componentBlob);
        }
        blobHash = computeSHAS3Dataset(componentHashes);
        for (Map.Entry<String, List<S3DatasetComponentBlob>> component :
            componentHashes.entrySet()) {
          S3DatasetComponentBlobEntity s3DatasetComponentBlobEntity =
              new S3DatasetComponentBlobEntity(
                  component.getKey(), blobHash, component.getValue().get(0));
          session.saveOrUpdate(s3DatasetComponentBlobEntity);
        }
        break;
      case PATH:
        blobHash = saveBlob(session, dataset.getPath());
        break;
      default:
        throw new ModelDBException("Unknown blob type", Code.INTERNAL);
    }
    rootTree.push(locationList, blobHash, blobType);
  }

  static String saveBlob(Session session, PathDatasetBlob path) throws NoSuchAlgorithmException {
    // sorted
    Map<String, List<PathDatasetComponentBlob>> componentHashes = new LinkedHashMap<>();
    for (PathDatasetComponentBlob componentBlob : path.getComponentsList()) {
      final String componentHash = computeSHA(componentBlob);
      componentHashes.putIfAbsent(componentHash, new LinkedList<>());
      componentHashes.get(componentHash).add(componentBlob);
    }
    String blobHash = computeSHAPathDataset(componentHashes);
    for (Map.Entry<String, List<PathDatasetComponentBlob>> component : componentHashes.entrySet()) {
      PathDatasetComponentBlobEntity pathDatasetComponentBlobEntity =
          new PathDatasetComponentBlobEntity(
              component.getKey(), blobHash, component.getValue().get(0));
      session.saveOrUpdate(pathDatasetComponentBlobEntity);
    }
    return blobHash;
  }

  private String getBlobType() {
    switch (dataset.getContentCase()) {
      case PATH:
        return PATH_DATASET_BLOB;
      case S3:
      default:
        return S_3_DATASET_BLOB;
    }
  }

  static String computeSHA(PathDatasetComponentBlob path) throws NoSuchAlgorithmException {
    StringBuilder sb = new StringBuilder();
    sb.append("path:")
        .append(path.getPath())
        .append(":size:")
        .append(path.getSize())
        .append(":last_modified:")
        .append(path.getLastModifiedAtSource())
        .append(":sha256:")
        .append(path.getSha256())
        .append(":md5:")
        .append(path.getMd5());
    return FileHasher.getSha(sb.toString());
  }

  private String computeSHA(S3DatasetComponentBlob s3componentBlob)
      throws NoSuchAlgorithmException {
    StringBuilder sb = new StringBuilder();
    sb.append(":s3:").append(computeSHA(s3componentBlob.getPath()));
    return FileHasher.getSha(sb.toString());
  }

  private String computeSHAS3Dataset(Map<String, List<S3DatasetComponentBlob>> componentHashes)
      throws NoSuchAlgorithmException {
    StringBuilder sb = new StringBuilder();
    sb.append("s3");
    for (Map.Entry<String, List<S3DatasetComponentBlob>> component : componentHashes.entrySet()) {
      for (int i = 0; i < component.getValue().size(); ++i) {
        sb.append(":component:").append(component.getKey());
      }
    }
    return FileHasher.getSha(sb.toString());
  }

  static String computeSHAPathDataset(Map<String, List<PathDatasetComponentBlob>> componentHashes)
      throws NoSuchAlgorithmException {
    StringBuilder sb = new StringBuilder();
    sb.append("path");
    for (Map.Entry<String, List<PathDatasetComponentBlob>> component : componentHashes.entrySet()) {
      for (int i = 0; i < component.getValue().size(); ++i) {
        sb.append(":component:").append(component.getKey());
      }
    }
    return FileHasher.getSha(sb.toString());
  }
}
