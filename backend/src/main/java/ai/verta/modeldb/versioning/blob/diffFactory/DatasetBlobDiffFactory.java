package ai.verta.modeldb.versioning.blob.diffFactory;

import ai.verta.modeldb.versioning.*;

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
    /*final DatasetDiff.Builder datasetBuilder = DatasetDiff.newBuilder();
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
          if (pathBuilder.getComponentsCount() != 0) {
            HashMap<String, PathDatasetComponentBlob> pathDatasetComponentBlobsA =
                new HashMap<>(pathBuilder.getComponentsCount());
            pathBuilder.getComponentsList().stream()
                .map(c -> c.getA())
                .forEach(c -> pathDatasetComponentBlobsA.put(c.getPath(), c));
            HashMap<String, PathDatasetComponentBlob> pathDatasetComponentBlobsB =
                new HashMap<>(dataset.getPath().getComponentsCount());
            dataset.getPath().getComponentsList().stream()
                .forEach(c -> pathDatasetComponentBlobsB.put(c.getPath(), c));

            Set<String> keys = pathDatasetComponentBlobsA.keySet();
            keys.addAll(pathDatasetComponentBlobsB.keySet());

            pathBuilder.clear();
            pathBuilder.addAllComponents(
                keys.stream()
                        .map(
                            key -> {
                              PathDatasetComponentDiff.Builder builder =
                                  PathDatasetComponentDiff.newBuilder();
                              if (!pathDatasetComponentBlobsB.containsKey(key)) {
                                builder
                                    .setA(pathDatasetComponentBlobsA.get(key))
                                    .setStatus(DiffStatusEnum.DiffStatus.DELETED);
                              } else if (!pathDatasetComponentBlobsA.containsKey(key)) {
                                builder
                                    .setB(pathDatasetComponentBlobsB.get(key))
                                    .setStatus(DiffStatusEnum.DiffStatus.ADDED);
                              } else {
                                builder
                                    .setA(pathDatasetComponentBlobsA.get(key))
                                    .setB(pathDatasetComponentBlobsB.get(key))
                                    .setStatus(DiffStatusEnum.DiffStatus.MODIFIED);
                              }
                              return builder.build();
                            })
                    ::iterator);
          } else {
            pathBuilder.addAllComponents(
                dataset.getPath().getComponentsList().stream()
                        .map(
                            c ->
                                PathDatasetComponentDiff.newBuilder()
                                    .setB(c)
                                    .setStatus(DiffStatusEnum.DiffStatus.ADDED)
                                    .build())
                    ::iterator);
          }
        } else {
          pathBuilder.addAllComponents(
              dataset.getPath().getComponentsList().stream()
                      .map(
                          c ->
                              PathDatasetComponentDiff.newBuilder()
                                  .setA(c)
                                  .setStatus(DiffStatusEnum.DiffStatus.DELETED)
                                  .build())
                  ::iterator);
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
    blobDiffBuilder.setDataset(datasetBuilder.build());*/
  }
}
