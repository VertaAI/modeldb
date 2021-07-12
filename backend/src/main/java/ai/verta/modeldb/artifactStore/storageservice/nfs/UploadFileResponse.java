package ai.verta.modeldb.artifactStore.storageservice.nfs;

public class UploadFileResponse {
  private String fileName;
  private String fileDownloadUri;
  private String fileType;
  private long size;
  private String ETag;

  public UploadFileResponse(
      String fileName, String fileDownloadUri, String fileType, long size, String ETag) {
    this.fileName = fileName;
    this.fileDownloadUri = fileDownloadUri;
    this.fileType = fileType;
    this.size = size;
    this.ETag = ETag;
  }

  public String getFileName() {
    return fileName;
  }

  public String getFileDownloadUri() {
    return fileDownloadUri;
  }

  public String getFileType() {
    return fileType;
  }

  public long getSize() {
    return size;
  }

  public String getETag() {
    return ETag;
  }
}
