package ai.verta.modeldb.versioning.blob.diffFactory;

import static ai.verta.modeldb.versioning.blob.diffFactory.ConfigBlobDiffFactory.removeCommon;

import ai.verta.modeldb.versioning.BlobDiff;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.DatasetBlob;
import ai.verta.modeldb.versioning.DatasetDiff;
import ai.verta.modeldb.versioning.PathDatasetComponentBlob;
import ai.verta.modeldb.versioning.PathDatasetDiff;
import ai.verta.modeldb.versioning.S3DatasetComponentBlob;
import ai.verta.modeldb.versioning.S3DatasetDiff;
import java.util.HashSet;
import java.util.Set;

public class DatasetBlobDiffFactory extends BlobDiffFactory {

  public DatasetBlobDiffFactory(BlobExpanded blobExpanded) {
    super(blobExpanded);
  }

  @Override
  protected boolean subtypeEqual(BlobDiffFactory blobDiffFactory) {
    return blobDiffFactory
        .getBlobExpanded()
        .getBlob()
        .getDataset()
        .getContentCase()
        .equals(getBlobExpanded().getBlob().getDataset().getContentCase());
  }

  @Override
  protected void add(BlobDiff.Builder blobDiffBuilder) {
    modify(blobDiffBuilder, true);
  }

  @Override
  protected void delete(BlobDiff.Builder blobDiffBuilder) {
    modify(blobDiffBuilder, false);
  }

  private void modify(BlobDiff.Builder blobDiffBuilder, boolean add) {
    final DatasetDiff.Builder datasetBuilder = DatasetDiff.newBuilder();
    final DatasetBlob dataset = getBlobExpanded().getBlob().getDataset();
    switch (dataset.getContentCase()) {
      case PATH:
        PathDatasetDiff.Builder pathBuilder;
        if (blobDiffBuilder.hasDataset()) {
          pathBuilder = blobDiffBuilder.getDataset().getPath().toBuilder();
        } else {
          pathBuilder = PathDatasetDiff.newBuilder();
        }
        if (add) {
          if (pathBuilder.getACount() != 0) {
            HashSet<PathDatasetComponentBlob> pathDatasetComponentBlobsA =
                new HashSet<>(pathBuilder.getAList());
            Set<PathDatasetComponentBlob> pathDatasetComponentBlobsB =
                new HashSet<>(dataset.getPath().getComponentsList());
            removeCommon(pathDatasetComponentBlobsA, pathDatasetComponentBlobsB);
            pathBuilder.clear();
            pathBuilder.addAllA(pathDatasetComponentBlobsA);
            pathBuilder.addAllB(pathDatasetComponentBlobsB);
          } else {
            pathBuilder.addAllB(dataset.getPath().getComponentsList());
          }
        } else {
          pathBuilder.addAllA(dataset.getPath().getComponentsList());
        }

        datasetBuilder.setPath(pathBuilder).build();
        break;
      case S3:
        S3DatasetDiff.Builder s3Builder;
        if (blobDiffBuilder.hasDataset()) {
          s3Builder = blobDiffBuilder.getDataset().getS3().toBuilder();
        } else {
          s3Builder = S3DatasetDiff.newBuilder();
        }
        if (add) {
          if (s3Builder.getACount() != 0) {
            HashSet<S3DatasetComponentBlob> s3DatasetComponentBlobsA =
                new HashSet<>(s3Builder.getAList());
            Set<S3DatasetComponentBlob> s3DatasetComponentBlobsB =
                new HashSet<>(dataset.getS3().getComponentsList());
            removeCommon(s3DatasetComponentBlobsA, s3DatasetComponentBlobsB);
            s3Builder.clear();
            s3Builder.addAllA(s3DatasetComponentBlobsA);
            s3Builder.addAllB(s3DatasetComponentBlobsB);
          } else {
            s3Builder.addAllB(dataset.getS3().getComponentsList());
          }
        } else {
          s3Builder.addAllA(dataset.getS3().getComponentsList());
        }

        datasetBuilder.setS3(s3Builder).build();
        break;
    }
    blobDiffBuilder.setDataset(datasetBuilder.build());
  }
}
