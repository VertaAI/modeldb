package ai.verta.modeldb.artifactStore.storageservice;

import ai.verta.modeldb.App;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import com.amazonaws.services.s3.model.PartETag;
import com.google.auth.Credentials;
import com.google.auth.ServiceAccountSigner;
import com.google.cloud.storage.*;
import com.google.cloud.storage.BlobInfo;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GCSService implements ArtifactStoreService {

  private static final Logger LOGGER = LogManager.getLogger(GCSService.class);
  private Storage gcsClient;
  private Credentials credentials;
  private String bucketName;
  private Duration signedUrlValidity;

  public GCSService(String cloudBucketName) throws ModelDBException {
    try {
      App app = App.getInstance();
      this.gcsClient = StorageOptions.getDefaultInstance().getService();
      this.credentials = this.gcsClient.getOptions().getCredentials();
      this.bucketName = cloudBucketName;
      // Using the Java Duration parser to extract time. Value specified should follow ISO-8601
      // standard.
      // (https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/Duration.html#parse(java.lang.CharSequence))
      this.signedUrlValidity = app.getSignedUrlValidity();
    } catch (Exception ex) {
      String errorMessage = "Could not create GCSClient for ModelDB backend.";
      LOGGER.warn(errorMessage, ex);
      throw new ModelDBException(errorMessage, ex);
    }
  }

  private Boolean doesBucketExist(String bucketName) {
    return this.gcsClient.get(bucketName).exists();
  }

  private String getUploadUrl(BlobInfo blobInfo) throws ModelDBException {

    // Adding extension headers to generate signed URL
    // Resumable uploads are Google's way of performing uploads for large files
    // You can read more about resumable uploads here:
    // https://cloud.google.com/storage/docs/uploads-downloads
    Map<String, String> extensionHeaders = new HashMap<>();
    extensionHeaders.put("Content-Type", "application/octet-stream");
    extensionHeaders.put("x-goog-resumable", "start");

    // Get a signed URL that can be used for POST command to get resumable upload signed URL
    URL url =
        this.gcsClient.signUrl(
            blobInfo,
            this.signedUrlValidity.getSeconds(),
            TimeUnit.SECONDS,
            Storage.SignUrlOption.httpMethod(HttpMethod.POST),
            Storage.SignUrlOption.withExtHeaders(extensionHeaders),
            Storage.SignUrlOption.withV4Signature(),
            Storage.SignUrlOption.signWith((ServiceAccountSigner) this.credentials));

    // Opening Http connection to send POST request and fetch URL for performing resumable uploads
    // The default signed URL doesn't support resumable uploads.
    // The following implementation is based on documentation here:
    // https://cloud.google.com/storage/docs/performing-resumable-uploads#xml-api
    try {
      URL obj = new URL(url.toString());

      HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
      // Adding a try-finally block to ensure connection is always disconnected.
      try {
        // Adding header and setting connection output to be true
        connection.setRequestMethod("POST");
        connection.setRequestProperty("x-goog-resumable", "start");
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        connection.setDoOutput(true);

        // Connecting to URL and sending POST & headers
        try (DataOutputStream writer = new DataOutputStream(connection.getOutputStream())) {
          writer.writeBytes("");
          writer.flush();
        }

        if (connection.getResponseCode() == connection.HTTP_CREATED) {
          // If the post was successful, we should receive a signed URL that can be used for
          // resumable uploads.
          return connection.getHeaderField("Location");
        } else {
          // If instead of returning the URL we received any other error message from the server for our post request,
          // we can read it using getInputStream. Finally, we can thrown an error after reading the stream.
          try (BufferedReader reader =
              new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = reader.readLine()) != null) {
              response.append(inputLine);
            }

            String errorMessage = response.toString();
            LOGGER.error("Failed to send post request to GCS. Got the following error message: " + errorMessage);
            throw new ModelDBException(errorMessage);
          }
        }
      } finally {
        connection.disconnect();
      }
    } catch (Exception ex) {
      String errorMessage =
          "Error while trying to send post request for getting resumable uploads signed URL";
      LOGGER.error(errorMessage, ex);
      throw new ModelDBException(errorMessage, ex);
    }
  }

  private String generatePresignedUrl(String artifactPath, String method) throws ModelDBException {
    // Validate bucket
    Boolean exist = doesBucketExist(bucketName);
    if (!exist) {
      throw new ModelDBException("Bucket does not exists", io.grpc.Status.Code.INTERNAL);
    }

    LOGGER.trace("GCSService - generatePresignedUrl called");

    // Creating blob object to pass information about bucket and path to upload/download artifacts
    BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(this.bucketName, artifactPath)).build();

    if (method.equalsIgnoreCase(ModelDBConstants.PUT)) {
      LOGGER.trace("GCSService - generatePresignedUrl - Put function called");
      return getUploadUrl(blobInfo);
    } else if (method.equalsIgnoreCase(ModelDBConstants.GET)) {
      LOGGER.trace("GCSService - generatePresignedUrl - Get function called");
      URL url =
          this.gcsClient.signUrl(
              blobInfo,
              this.signedUrlValidity.getSeconds(),
              TimeUnit.SECONDS,
              Storage.SignUrlOption.withV4Signature(),
              Storage.SignUrlOption.signWith((ServiceAccountSigner) this.credentials));

      return url.toString();
    } else {
      String errorMessage = "Unsupported HTTP Method for GCS presigned URL";
      Status status =
          Status.newBuilder().setCode(Code.INVALID_ARGUMENT_VALUE).setMessage(errorMessage).build();
      LOGGER.error(errorMessage);
      throw StatusProto.toStatusRuntimeException(status);
    }
  }

  @Override
  public Optional<String> initiateMultipart(String s3Key) {
    // Returning empty would let the underlying caller know that this artifact store method doesn't
    // support multipart
    // uploads and will only call generatePresignedUrl once to get the upload URL.
    String errorMessage = "Multipart not supported by GCS";
    Status status =
            Status.newBuilder().setCode(Code.FAILED_PRECONDITION_VALUE).setMessage(errorMessage).build();
    LOGGER.error(errorMessage);
    throw StatusProto.toStatusRuntimeException(status);
  }

  @Override
  public String generatePresignedUrl(
      String artifactPath, String method, long partNumber, String uploadId)
      throws ModelDBException {
    return generatePresignedUrl(artifactPath, method);
  }

  @Override
  public void commitMultipart(String s3Path, String uploadId, List<PartETag> partETags)
      throws ModelDBException {
    throw new ModelDBException("Not supported by GCS", io.grpc.Status.Code.FAILED_PRECONDITION);
  }
}
