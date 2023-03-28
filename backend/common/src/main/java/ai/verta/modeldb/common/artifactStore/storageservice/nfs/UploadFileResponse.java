package ai.verta.modeldb.common.artifactStore.storageservice.nfs;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter(AccessLevel.NONE)
public class UploadFileResponse {
  private final String fileName;
  private final String fileDownloadUri;
  private final String fileType;
  private final long size;
  private final String eTag;

  public UploadFileResponse(
      String fileName, String fileDownloadUri, String fileType, long size, String eTag) {
    this.fileName = fileName;
    this.fileDownloadUri = fileDownloadUri;
    this.fileType = fileType;
    this.size = size;
    this.eTag = eTag;
  }
}
