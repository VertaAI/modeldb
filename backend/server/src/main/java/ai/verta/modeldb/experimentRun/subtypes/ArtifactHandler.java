package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.ArtifactTypeEnum;
import ai.verta.modeldb.*;
import ai.verta.modeldb.common.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.common.config.ArtifactStoreConfig;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.FutureExecutor;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.config.MDBConfig;
import ai.verta.modeldb.entities.ArtifactEntity;
import ai.verta.modeldb.entities.ArtifactPartEntity;
import ai.verta.modeldb.experimentRun.S3KeyFunction;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.versioning.VersioningUtils;
import io.grpc.Status.Code;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.Session;

public class ArtifactHandler extends ArtifactHandlerBase {
  private static Logger LOGGER = LogManager.getLogger(ArtifactHandler.class);
  private static final String KEY_S_NOT_LOGGED_ERROR = "Key %s not logged";

  private final CodeVersionHandler codeVersionHandler;
  private final DatasetHandler datasetHandler;
  private final MDBConfig mdbConfig = App.getInstance().mdbConfig;

  private final ArtifactStoreDAO artifactStoreDAO;
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();
  private final int artifactEntityType;

  public ArtifactHandler(
      FutureExecutor executor,
      FutureJdbi jdbi,
      String entityName,
      CodeVersionHandler codeVersionHandler,
      DatasetHandler datasetHandler,
      ArtifactStoreDAO artifactStoreDAO,
      MDBConfig mdbConfig) {
    super(executor, jdbi, "artifacts", entityName, mdbConfig.getArtifactStoreConfig());
    this.codeVersionHandler = codeVersionHandler;
    this.datasetHandler = datasetHandler;
    this.artifactStoreDAO = artifactStoreDAO;

    if (entityName.equals("ProjectEntity")) {
      this.artifactEntityType = ArtifactPartEntity.PROJECT_ARTIFACT;
    } else if (entityName.equals("ExperimentEntity")) {
      this.artifactEntityType = ArtifactPartEntity.EXPERIMENT_ARTIFACT;
    } else if (entityName.equals("ExperimentRunEntity")) {
      this.artifactEntityType = ArtifactPartEntity.EXP_RUN_ARTIFACT;
    } else {
      throw new ModelDBException("Invalid entity type for ArtifactPart", Code.INTERNAL);
    }
  }

  public InternalFuture<GetUrlForArtifact.Response> getUrlForArtifact(GetUrlForArtifact request) {
    return InternalFuture.runAsync(
            () -> {
              String errorMessage = null;
              if (request.getKey().isEmpty()) {
                errorMessage = "Artifact Key not found in GetUrlForArtifact request";
              } else if (request.getMethod().isEmpty()) {
                errorMessage = "Method is not found in GetUrlForArtifact request";
              }

              if (errorMessage != null) {
                throw new InvalidArgumentException(errorMessage);
              }
            },
            executor)
        .thenCompose(
            unused -> {
              final InternalFuture<Map.Entry<String, String>> urlInfo;
              String errorMessage;

              /*Process code*/
              if (request.getArtifactType() == ArtifactTypeEnum.ArtifactType.CODE) {
                errorMessage =
                    String.format("Code versioning artifact not found at %s level", entityName);
                urlInfo =
                    getUrlForCode(request)
                        .thenApply(s3Key -> new AbstractMap.SimpleEntry<>(s3Key, null), executor);
              } else if (request.getArtifactType() == ArtifactTypeEnum.ArtifactType.DATA) {
                errorMessage = "Data versioning artifact not found";
                urlInfo = getUrlForData(request);
              } else {
                errorMessage =
                    String.format(
                        "%s ID "
                            + request.getId()
                            + " does not have the artifact "
                            + request.getKey(),
                        entityName);

                urlInfo =
                    getEntityArtifactS3PathAndMultipartUploadID(
                        request.getId(),
                        request.getKey(),
                        request.getPartNumber(),
                        artifactStoreDAO::initializeMultipart);
              }

              String finalErrorMessage = errorMessage;
              return urlInfo.thenApply(
                  info -> {
                    final var s3Key = info.getKey();
                    final var uploadId = info.getValue();
                    if (s3Key == null) {
                      throw new NotFoundException(finalErrorMessage);
                    }

                    return artifactStoreDAO.getUrlForArtifactMultipart(
                        s3Key, request.getMethod(), request.getPartNumber(), uploadId);
                  },
                  executor);
            },
            executor);
  }

  private InternalFuture<Map.Entry<String, String>> getEntityArtifactS3PathAndMultipartUploadID(
      String entityId, String key, long partNumber, S3KeyFunction initializeMultipart) {
    return getArtifactId(entityId, key)
        .thenApply(
            maybeId -> {
              final var id =
                  maybeId.orElseThrow(
                      () ->
                          new InvalidArgumentException(String.format(KEY_S_NOT_LOGGED_ERROR, key)));
              try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
                final var artifactEntity =
                    session.get(ArtifactEntity.class, id, LockMode.PESSIMISTIC_WRITE);
                return getS3PathAndMultipartUploadId(
                    session, artifactEntity, partNumber != 0, initializeMultipart);
              }
            },
            executor);
  }

  private AbstractMap.SimpleEntry<String, String> getS3PathAndMultipartUploadId(
      Session session,
      ArtifactEntity artifactEntity,
      boolean partNumberSpecified,
      S3KeyFunction initializeMultipart) {
    String uploadId;
    if (partNumberSpecified
        && mdbConfig
            .getArtifactStoreConfig()
            .getArtifactStoreType()
            .equals(ArtifactStoreConfig.S3_TYPE_STORE)) {
      uploadId = artifactEntity.getUploadId();
      String message = null;
      if (uploadId == null || artifactEntity.isUploadCompleted()) {
        if (initializeMultipart == null) {
          message = "Multipart wasn't initialized";
        } else {
          uploadId = initializeMultipart.apply(artifactEntity.getPath()).orElse(null);
        }
      }
      if (message != null) {
        LOGGER.info(message);
        throw new ModelDBException(message, Code.FAILED_PRECONDITION);
      }
      if (!Objects.equals(uploadId, artifactEntity.getUploadId())
          || artifactEntity.isUploadCompleted()) {
        session.beginTransaction();
        VersioningUtils.getArtifactPartEntities(
                session, String.valueOf(artifactEntity.getId()), this.artifactEntityType)
            .forEach(session::delete);
        artifactEntity.setUploadId(uploadId);
        artifactEntity.setUploadCompleted(false);
        session.getTransaction().commit();
      }
    } else {
      uploadId = null;
    }
    return new AbstractMap.SimpleEntry<>(artifactEntity.getPath(), uploadId);
  }

  private InternalFuture<Map.Entry<String, String>> getUrlForData(GetUrlForArtifact request) {
    return InternalFuture.runAsync(
            () -> {
              if (request.getKey().isEmpty()) {
                throw new InvalidArgumentException("Key must be provided");
              }
            },
            executor)
        .thenCompose(
            unused ->
                datasetHandler
                    .getArtifacts(request.getId(), Optional.of(request.getKey()))
                    .thenApply(
                        artifacts -> {
                          if (artifacts.isEmpty()) {
                            throw new InvalidArgumentException(
                                String.format(KEY_S_NOT_LOGGED_ERROR, request.getKey()));
                          }
                          throw new InvalidArgumentException("Not supported yet");
                        },
                        executor),
            executor);
  }

  private InternalFuture<String> getUrlForCode(GetUrlForArtifact request) {
    return codeVersionHandler
        .getCodeVersion(request.getId())
        .thenApply(
            maybeCodeVersion ->
                maybeCodeVersion
                    .map(codeVersion -> codeVersion.getCodeArchive().getPath())
                    .orElseThrow(
                        () -> new InvalidArgumentException("Code version has not been logged")),
            executor);
  }

  public InternalFuture<Void> commitArtifactPart(CommitArtifactPart request) {
    return getArtifactId(request.getId(), request.getKey())
        .thenAccept(
            maybeId -> {
              final var id =
                  maybeId.orElseThrow(
                      () ->
                          new InvalidArgumentException(
                              String.format(KEY_S_NOT_LOGGED_ERROR, request.getKey())));
              try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
                final var artifactEntity =
                    session.get(ArtifactEntity.class, id, LockMode.PESSIMISTIC_WRITE);
                VersioningUtils.saveArtifactPartEntity(
                    request.getArtifactPart(),
                    session,
                    String.valueOf(artifactEntity.getId()),
                    this.artifactEntityType);
              }
            },
            executor);
  }

  public InternalFuture<GetCommittedArtifactParts.Response> getCommittedArtifactParts(
      GetCommittedArtifactParts request) {
    return getArtifactId(request.getId(), request.getKey())
        .thenApply(
            maybeId -> {
              final var id =
                  maybeId.orElseThrow(
                      () ->
                          new InvalidArgumentException(
                              String.format(KEY_S_NOT_LOGGED_ERROR, request.getKey())));
              try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
                final var artifactEntity =
                    session.get(ArtifactEntity.class, id, LockMode.PESSIMISTIC_WRITE);
                Set<ArtifactPartEntity> artifactPartEntities =
                    VersioningUtils.getArtifactPartEntities(
                        session, String.valueOf(artifactEntity.getId()), this.artifactEntityType);

                var response = GetCommittedArtifactParts.Response.newBuilder();
                artifactPartEntities.forEach(
                    artifactPartEntity -> response.addArtifactParts(artifactPartEntity.toProto()));
                return response.build();
              }
            },
            executor);
  }

  public InternalFuture<Void> commitMultipartArtifact(CommitMultipartArtifact request) {
    return getArtifactId(request.getId(), request.getKey())
        .thenAccept(
            maybeId -> {
              final var id =
                  maybeId.orElseThrow(
                      () ->
                          new InvalidArgumentException(
                              String.format(KEY_S_NOT_LOGGED_ERROR, request.getKey())));
              try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
                final var artifactEntity =
                    session.get(ArtifactEntity.class, id, LockMode.PESSIMISTIC_WRITE);
                if (artifactEntity.getUploadId() == null) {
                  var message =
                      "Multipart wasn't initialized OR Multipart artifact already committed";
                  throw new InvalidArgumentException(message);
                }
                Set<ArtifactPartEntity> artifactPartEntities =
                    VersioningUtils.getArtifactPartEntities(
                        session, String.valueOf(artifactEntity.getId()), this.artifactEntityType);
                final var partETags =
                    artifactPartEntities.stream()
                        .map(ArtifactPartEntity::toPartETag)
                        .collect(Collectors.toList());
                artifactStoreDAO.commitMultipart(
                    artifactEntity.getPath(), artifactEntity.getUploadId(), partETags);
                session.beginTransaction();
                artifactEntity.setUploadCompleted(true);
                artifactEntity.setUploadId(null);
                session.getTransaction().commit();
              }
            },
            executor);
  }
}
