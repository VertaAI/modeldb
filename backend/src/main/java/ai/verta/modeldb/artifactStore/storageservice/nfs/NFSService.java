package ai.verta.modeldb.artifactStore.storageservice.nfs;

import ai.verta.modeldb.App;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.artifactStore.storageservice.ArtifactStoreService;
import com.amazonaws.services.s3.model.PartETag;
import com.google.api.client.util.IOUtils;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NFSService implements ArtifactStoreService {

  private static final Logger LOGGER = LogManager.getLogger(NFSService.class);
  private final Path fileStorageLocation;
  private App app = App.getInstance();

  /**
   * Create NFS service bean by springBoot and create root folder if not exists
   *
   * @param fileStorageProperties : file root path properties
   * @throws ModelDBException ModelDBException
   */
  @Autowired
  public NFSService(FileStorageProperties fileStorageProperties) throws ModelDBException {
    LOGGER.trace("NFSService constructor called");
    this.fileStorageLocation =
        Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
    LOGGER.trace("NFSService root directory : {}", this.fileStorageLocation);

    try {
      if (!this.fileStorageLocation.toFile().exists()) {
        Files.createDirectories(this.fileStorageLocation);
        LOGGER.trace("NFS root directory created successfully");
      } else {
        LOGGER.trace("NFS root directory already exists");
      }
    } catch (Exception ex) {
      String errorMessage =
          "Could not create the directory where the uploaded files will be stored.";
      LOGGER.warn(errorMessage, ex);
      throw new ModelDBException(errorMessage, ex);
    }
    LOGGER.trace("NFSService constructor returned");
  }

  /**
   * Upload multipart file into respected artifact path
   *
   * @param artifactPath : artifact path
   * @param uploadedFileInputStream : uploaded file input stream
   * @return {@link String} : upload filename
   * @throws ModelDBException ModelDBException
   */
  String storeFile(String artifactPath, InputStream uploadedFileInputStream)
      throws ModelDBException {
    LOGGER.trace("NFSService - storeFile called");

    try {
      String cleanArtifactPath = StringUtils.cleanPath(Objects.requireNonNull(artifactPath));
      String[] folders = cleanArtifactPath.split("/");

      StringBuilder folderPath = new StringBuilder();
      for (int i = 0; i < folders.length - 1; i++) {
        folderPath.append(folders[i]);
        folderPath.append(File.separator);
      }
      LOGGER.trace("NFSService - storeFile - folder path : {}", folderPath.toString());

      // Copy file to the target location (Replacing existing file with the same name)
      File foldersExists =
          new File(this.fileStorageLocation + File.separator + folderPath.toString());
      if (!foldersExists.exists()) {
        boolean folderCreatingStatus = foldersExists.mkdirs();
        LOGGER.trace(
            "NFSService - storeFile - folders created : {}, Path: {}",
            folderCreatingStatus,
            foldersExists.getAbsolutePath());
      }
      LOGGER.trace("NFSService - storeFile -  folders found : {}", foldersExists.getAbsolutePath());

      File destinationFile =
          new File(this.fileStorageLocation + File.separator + cleanArtifactPath);
      if (!destinationFile.exists()) {
        boolean destFileCreatingStatus = destinationFile.createNewFile();
        LOGGER.trace(
            "NFSService - storeFile - file created : {}, Path: {}",
            destFileCreatingStatus,
            destinationFile.getAbsolutePath());
      }
      LOGGER.trace("NFSService - storeFile -  file found : {}", foldersExists.getAbsolutePath());
      FileOutputStream fileOutputStream = new FileOutputStream(destinationFile);
      IOUtils.copy(uploadedFileInputStream, fileOutputStream);
      fileOutputStream.close();
      uploadedFileInputStream.close();
      LOGGER.trace(
          "NFSService - storeFile - file stored successfully, target location : {}",
          destinationFile.getAbsolutePath());
      LOGGER.trace("NFSService - storeFile returned");
      return destinationFile.getName();
    } catch (IOException ex) {
      String errorMessage = "Could not store file. Please try again!";
      LOGGER.warn(errorMessage, ex);
      throw new ModelDBException(errorMessage, ex);
    }
  }

  /**
   * Return File resource for getDownloadArtifact
   *
   * @param artifactPath : artifact path
   * @return {@link Resource} : File data as a resource for download
   * @throws ModelDBException ModelDBException
   */
  Resource loadFileAsResource(String artifactPath) throws ModelDBException {
    LOGGER.trace("NFSService - loadFileAsResource called");
    try {
      Path filePath = this.fileStorageLocation.resolve(artifactPath).normalize();
      Resource resource = new UrlResource(filePath.toUri());
      if (resource.exists()) {
        LOGGER.trace("NFSService - loadFileAsResource - resource exists");
        LOGGER.trace("NFSService - loadFileAsResource returned");
        return resource;
      } else {
        String errorMessage = "File not found " + artifactPath;
        LOGGER.warn(errorMessage);
        throw new ModelDBException(errorMessage);
      }
    } catch (MalformedURLException | ModelDBException ex) {
      String errorMessage = "File not found " + artifactPath;
      LOGGER.warn(errorMessage, ex);
      throw new ModelDBException(errorMessage, ex);
    }
  }

  private String generatePresignedUrl(String artifactPath, String method) {
    LOGGER.trace("NFSService - generatePresignedUrl called");
    if (method.equalsIgnoreCase(ModelDBConstants.PUT)) {
      LOGGER.trace("NFSService - generatePresignedUrl - put url returned");
      return getUploadUrl(
          Collections.singletonMap("artifact_path", artifactPath),
          app.getArtifactStoreUrlProtocol(),
          app.getStoreArtifactEndpoint(),
          app.getPickArtifactStoreHostFromConfig(),
          app.getArtifactStoreServerHost());
    } else if (method.equalsIgnoreCase(ModelDBConstants.GET)) {
      LOGGER.trace("NFSService - generatePresignedUrl - get url returned");
      return getDownloadUrl(
          Collections.singletonMap("artifact_path", artifactPath),
          app.getArtifactStoreUrlProtocol(),
          app.getGetArtifactEndpoint(),
          app.getPickArtifactStoreHostFromConfig(),
          app.getArtifactStoreServerHost());
    } else {
      String errorMessage = "Unsupported HTTP Method for NFS Presigned URL";
      Status status =
          Status.newBuilder().setCode(Code.NOT_FOUND_VALUE).setMessage(errorMessage).build();
      LOGGER.info(errorMessage);
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  @Override
  public Optional<String> initiateMultipart(String s3Key) {
    return Optional.empty();
  }

  @Override
  public String generatePresignedUrl(
      String artifactPath, String method, long partNumber, String uploadId) {
    return generatePresignedUrl(artifactPath, method);
  }

  @Override
  public void commitMultipart(String s3Path, String uploadId, List<PartETag> partETags)
      throws ModelDBException {
    throw new ModelDBException("Not supported by NFS", Code.FAILED_PRECONDITION);
  }
}
