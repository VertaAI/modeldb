package ai.verta.modeldb.versioning.blob.diffFactory;

import ai.verta.modeldb.versioning.Blob.ContentCase;
import ai.verta.modeldb.versioning.BlobDiff;
import ai.verta.modeldb.versioning.BlobDiff.Builder;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.DiffStatusEnum.DiffStatus;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** constructs proto blob diff object from proto blobs */
public abstract class BlobDiffFactory {

  private final BlobExpanded blobExpanded;

  BlobDiffFactory(BlobExpanded blobExpanded) {
    this.blobExpanded = blobExpanded;
  }

  private ContentCase getBlobType() {
    return blobExpanded.getBlob().getContentCase();
  }

  public BlobExpanded getBlobExpanded() {
    return blobExpanded;
  }

  public List<BlobDiff> compare(BlobDiffFactory blobDiffFactoryB, String location) {
    if (typeEqual(blobDiffFactoryB)) {
      // use modified blob diff
      // TODO: Here used the `#` for split the locations but if folder
      // TODO: - contain the `#` then this functionality will break.
      final Builder builder =
          BlobDiff.newBuilder().addAllLocation(Arrays.asList(location.split("#")));
      builder.setStatus(DiffStatus.MODIFIED);
      delete(builder);
      blobDiffFactoryB.add(builder);
      return Collections.singletonList(builder.build());
    } else {
      // use delete and add
      // TODO: Here used the `#` for split the locations but if folder
      // TODO: - contain the `#` then this functionality will break.
      final Builder oldBlobDiff =
          BlobDiff.newBuilder().addAllLocation(Arrays.asList(location.split("#")));
      final Builder newBlobDiff = oldBlobDiff.clone();
      oldBlobDiff.setStatus(DiffStatus.DELETED);
      delete(oldBlobDiff);
      newBlobDiff.setStatus(DiffStatus.ADDED);
      blobDiffFactoryB.add(newBlobDiff);
      return Stream.of(oldBlobDiff, newBlobDiff).map(Builder::build).collect(Collectors.toList());
    }
  }

  private boolean typeEqual(BlobDiffFactory blobDiffFactory) {
    return blobDiffFactory.getBlobType() == getBlobType() && subtypeEqual(blobDiffFactory);
  }

  protected abstract boolean subtypeEqual(BlobDiffFactory blobDiffFactory);

  protected abstract void add(Builder builder);

  protected abstract void delete(Builder builder);

  public BlobDiff add(String location) {
    // TODO: Here used the `#` for split the locations but if folder
    // TODO: - contain the `#` then this functionality will break.
    final Builder builder =
        BlobDiff.newBuilder().addAllLocation(Arrays.asList(location.split("#")));
    builder.setStatus(DiffStatus.ADDED);
    add(builder);
    return builder.build();
  }

  public BlobDiff delete(String location) {
    // TODO: Here used the `#` for split the locations but if folder
    // TODO: - contain the `#` then this functionality will break.
    final Builder builder =
        BlobDiff.newBuilder().addAllLocation(Arrays.asList(location.split("#")));
    builder.setStatus(DiffStatus.DELETED);
    delete(builder);
    return builder.build();
  }

  public static BlobDiffFactory create(BlobExpanded blobExpanded) {
    switch (blobExpanded.getBlob().getContentCase()) {
      case ENVIRONMENT:
        return new EnvironmentBlobDiffFactory(blobExpanded);
      case CODE:
        return new CodeBlobDiffFactory(blobExpanded);
      case CONFIG:
        return new ConfigBlobDiffFactory(blobExpanded);
      case DATASET:
        return new DatasetBlobDiffFactory(blobExpanded);
      default:
        Status status =
            Status.newBuilder()
                .setCode(Code.INTERNAL_VALUE)
                .setMessage("Invalid blob type found")
                .build();
        throw StatusProto.toStatusRuntimeException(status);
    }
  }
}
