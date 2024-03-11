package ai.verta.modeldb.common.artifactStore.storageservice.nfs;

import ai.verta.modeldb.common.artifactStore.storageservice.ArtifactStoreService;
import ai.verta.modeldb.common.config.ArtifactStoreConfig;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.amazonaws.services.s3.model.PartETag;
import com.google.rpc.Code;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StringUtils;

public class NFSService implements ArtifactStoreService {

  private static final Logger LOGGER = LogManager.getLogger(NFSService.class);
  private final Path fileStorageLocation;
  private final ArtifactStoreConfig artifactStoreConfig;

  /**
   * Create NFS service bean by springBoot and create root folder if not exists
   *
   * @param fileStorageProperties : file root path properties
   * @param artifactStoreConfig: artifact store config
   * @throws ModelDBException ModelDBException
   */
  public NFSService(
      FileStorageProperties fileStorageProperties, ArtifactStoreConfig artifactStoreConfig)
      throws ModelDBException {
    this.artifactStoreConfig = artifactStoreConfig;
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
      var errorMessage = "Could not create the directory where the uploaded files will be stored.";
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
  String storeFile(
      String artifactPath, InputStream uploadedFileInputStream, HttpServletRequest request)
      throws ModelDBException {
    LOGGER.trace("NFSService - storeFile called");

    try {
      var cleanArtifactPath = StringUtils.cleanPath(Objects.requireNonNull(artifactPath));
      String[] folders = cleanArtifactPath.split("/");

      var folderPath = new StringBuilder();
      for (var i = 0; i < folders.length - 1; i++) {
        folderPath.append(folders[i]);
        folderPath.append(File.separator);
      }
      LOGGER.trace("NFSService - storeFile - folder path : {}", folderPath.toString());

      // Copy file to the target location (Replacing existing file with the same name)
      var foldersExists =
          new File(this.fileStorageLocation + File.separator + folderPath.toString());
      if (!foldersExists.exists()) {
        boolean folderCreatingStatus = foldersExists.mkdirs();
        LOGGER.trace(
            "NFSService - storeFile - folders created : {}, Path: {}",
            folderCreatingStatus,
            foldersExists.getAbsolutePath());
      }
      LOGGER.trace("NFSService - storeFile -  folders found : {}", foldersExists.getAbsolutePath());

      var destinationFile = new File(this.fileStorageLocation + File.separator + cleanArtifactPath);
      if (!destinationFile.exists()) {
        boolean destFileCreatingStatus = destinationFile.createNewFile();
        LOGGER.trace(
            "NFSService - storeFile - file created : {}, Path: {}",
            destFileCreatingStatus,
            destinationFile.getAbsolutePath());
      }
      LOGGER.trace("NFSService - storeFile -  file found : {}", foldersExists.getAbsolutePath());
      var fileOutputStream = new FileOutputStream(destinationFile);
      uploadedFileInputStream.transferTo(fileOutputStream);
      fileOutputStream.close();
      uploadedFileInputStream.close();
      LOGGER.trace(
          "NFSService - storeFile - file stored successfully, target location : {}",
          destinationFile.getAbsolutePath());
      LOGGER.trace("NFSService - storeFile returned");
      return destinationFile.getName();
    } catch (IOException ex) {
      var errorMessage = "Could not store file. Please try again!";
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
      var filePath = this.fileStorageLocation.resolve(artifactPath).normalize();
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
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("artifact_path", artifactPath);

    if (method.equalsIgnoreCase("put")) {
      LOGGER.trace("NFSService - generatePresignedUrl - put url returned");
      return getUploadUrl(
          parameters,
          artifactStoreConfig.getProtocol(),
          artifactStoreConfig.getArtifactEndpoint().getStoreArtifact(),
          artifactStoreConfig.isPickArtifactStoreHostFromConfig(),
          artifactStoreConfig.getHost());
    } else if (method.equalsIgnoreCase("get")) {
      LOGGER.trace("NFSService - generatePresignedUrl - get url returned");
      var filename = artifactPath.substring(artifactPath.lastIndexOf("/"));
      parameters.put("FileName", filename);
      return getDownloadUrl(
          parameters,
          artifactStoreConfig.getProtocol(),
          artifactStoreConfig.getArtifactEndpoint().getGetArtifact(),
          artifactStoreConfig.isPickArtifactStoreHostFromConfig(),
          artifactStoreConfig.getHost());
    } else {
      var errorMessage = "Unsupported HTTP Method for NFS Presigned URL";
      throw new InvalidArgumentException(errorMessage);
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

  @Override
  public InputStream downloadFileFromStorage(String artifactPath) throws ModelDBException {

    try {
      var filePath = this.fileStorageLocation.resolve(artifactPath).normalize();
      Resource resource = new UrlResource(filePath.toUri());
      if (resource.exists()) {
        LOGGER.info("file exist in NFS storage");
        var fileInputStream = resource.getInputStream();
        LOGGER.info("file fetched successfully from storage");
        return fileInputStream;
      }
    } catch (IOException e) {
      throw new ModelDBException(e.getMessage(), Code.INTERNAL, e);
    }

    String errorMessage = "artifact file not found in NFS storage for given key : " + artifactPath;
    LOGGER.info(errorMessage);
    throw new ModelDBException(errorMessage, Code.NOT_FOUND);
  }
}
