package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.ArtifactTypeEnum;
import ai.verta.modeldb.*;
import ai.verta.modeldb.common.ModelDBConstants;
import ai.verta.modeldb.common.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAO;
import ai.verta.modeldb.entities.ArtifactEntity;
import ai.verta.modeldb.entities.ArtifactPartEntity;
import ai.verta.modeldb.common.exceptions.InvalidArgumentException;
import ai.verta.modeldb.experimentRun.S3KeyFunction;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.versioning.VersioningUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.Session;

public class ArtifactHandler extends ArtifactHandlerBase {
  private static Logger LOGGER = LogManager.getLogger(ArtifactHandler.class);

  private final CodeVersionHandler codeVersionHandler;
  private final DatasetHandler datasetHandler;
  private final Config config = App.getInstance().config;

  private final ArtifactStoreDAO artifactStoreDAO;
  private final DatasetVersionDAO datasetVersionDAO;
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();

  public ArtifactHandler(
      Executor executor,
      FutureJdbi jdbi,
      String entityName,
      CodeVersionHandler codeVersionHandler,
      DatasetHandler datasetHandler,
      ArtifactStoreDAO artifactStoreDAO,
      DatasetVersionDAO datasetVersionDAO) {
    super(executor, jdbi, "artifacts", entityName);
    this.codeVersionHandler = codeVersionHandler;
    this.datasetHandler = datasetHandler;
    this.datasetVersionDAO = datasetVersionDAO;
    this.artifactStoreDAO = artifactStoreDAO;
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
              String errorMessage = null;

              /*Process code*/
              if (request.getArtifactType() == ArtifactTypeEnum.ArtifactType.CODE) {
                errorMessage =
                    "Code versioning artifact not found at experimentRun, experiment and project level";
                urlInfo =
                    getUrlForCode(request)
                        .thenApply(s3Key -> new AbstractMap.SimpleEntry<>(s3Key, null), executor);
              } else if (request.getArtifactType() == ArtifactTypeEnum.ArtifactType.DATA) {
                errorMessage = "Data versioning artifact not found";
                urlInfo = getUrlForData(request);
              } else {
                errorMessage =
                    "ExperimentRun ID "
                        + request.getId()
                        + " does not have the artifact "
                        + request.getKey();

                urlInfo =
                    getExperimentRunArtifactS3PathAndMultipartUploadID(
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

                    GetUrlForArtifact.Response response =
                        artifactStoreDAO.getUrlForArtifactMultipart(
                            s3Key, request.getMethod(), request.getPartNumber(), uploadId);

                    return response;
                  },
                  executor);
            },
            executor);
  }

  private InternalFuture<Map.Entry<String, String>>
      getExperimentRunArtifactS3PathAndMultipartUploadID(
          String experimentRunId, String key, long partNumber, S3KeyFunction initializeMultipart) {
    return getArtifactId(experimentRunId, key)
        .thenApply(
            maybeId -> {
              final var id =
                  maybeId.orElseThrow(
                      () -> new InvalidArgumentException("Key " + key + " not logged"));
              try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
                final ArtifactEntity artifactEntity =
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
        && config.artifactStoreConfig.artifactStoreType.equals(ModelDBConstants.S3)) {
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
        throw new ModelDBException(message, io.grpc.Status.Code.FAILED_PRECONDITION);
      }
      if (!Objects.equals(uploadId, artifactEntity.getUploadId())
          || artifactEntity.isUploadCompleted()) {
        session.beginTransaction();
        VersioningUtils.getArtifactPartEntities(
                session,
                String.valueOf(artifactEntity.getId()),
                ArtifactPartEntity.EXP_RUN_ARTIFACT)
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
                                "Key " + request.getKey() + " not logged");
                          }
                          try {
                            return new AbstractMap.SimpleEntry<>(
                                datasetVersionDAO.getUrlForDatasetVersion(
                                    artifacts.get(0).getLinkedArtifactId(), request.getMethod()),
                                null);
                          } catch (InvalidProtocolBufferException e) {
                            throw new ModelDBException(e);
                          }
                        },
                        executor),
            executor);
  }

  private InternalFuture<String> getUrlForCode(GetUrlForArtifact request) {
    return codeVersionHandler
        .getCodeVersion(request.getId())
        .thenApply(
            maybeExprRun ->
                maybeExprRun
                    .map(exprRun -> exprRun.getCodeArchive().getPath())
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
                          new InvalidArgumentException("Key " + request.getKey() + " not logged"));
              try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
                final ArtifactEntity artifactEntity =
                    session.get(ArtifactEntity.class, id, LockMode.PESSIMISTIC_WRITE);
                VersioningUtils.saveArtifactPartEntity(
                    request.getArtifactPart(),
                    session,
                    String.valueOf(artifactEntity.getId()),
                    ArtifactPartEntity.EXP_RUN_ARTIFACT);
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
                          new InvalidArgumentException("Key " + request.getKey() + " not logged"));
              try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
                final ArtifactEntity artifactEntity =
                    session.get(ArtifactEntity.class, id, LockMode.PESSIMISTIC_WRITE);
                Set<ArtifactPartEntity> artifactPartEntities =
                    VersioningUtils.getArtifactPartEntities(
                        session,
                        String.valueOf(artifactEntity.getId()),
                        ArtifactPartEntity.EXP_RUN_ARTIFACT);
                ;
                GetCommittedArtifactParts.Response.Builder response =
                    GetCommittedArtifactParts.Response.newBuilder();
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
                          new InvalidArgumentException("Key " + request.getKey() + " not logged"));
              try (Session session = modelDBHibernateUtil.getSessionFactory().openSession()) {
                final ArtifactEntity artifactEntity =
                    session.get(ArtifactEntity.class, id, LockMode.PESSIMISTIC_WRITE);
                if (artifactEntity.getUploadId() == null) {
                  String message =
                      "Multipart wasn't initialized OR Multipart artifact already committed";
                  throw new InvalidArgumentException(message);
                }
                Set<ArtifactPartEntity> artifactPartEntities =
                    VersioningUtils.getArtifactPartEntities(
                        session,
                        String.valueOf(artifactEntity.getId()),
                        ArtifactPartEntity.EXP_RUN_ARTIFACT);
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
