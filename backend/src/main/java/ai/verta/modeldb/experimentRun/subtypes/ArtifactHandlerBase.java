package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.Artifact;
import ai.verta.common.ArtifactTypeEnum;
import ai.verta.modeldb.GetUrlForArtifact;
import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.artifactStore.ArtifactStoreDAO;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.NotFoundException;
import ai.verta.modeldb.common.futures.FutureJdbi;
import ai.verta.modeldb.common.futures.InternalFuture;
import ai.verta.modeldb.config.Config;
import ai.verta.modeldb.datasetVersion.DatasetVersionDAO;
import ai.verta.modeldb.entities.ArtifactEntity;
import ai.verta.modeldb.entities.ArtifactPartEntity;
import ai.verta.modeldb.exceptions.AlreadyExistsException;
import ai.verta.modeldb.exceptions.InvalidArgumentException;
import ai.verta.modeldb.experimentRun.S3KeyFunction;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.versioning.VersioningUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.LockMode;
import org.hibernate.Session;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class ArtifactHandlerBase {
  private static Logger LOGGER = LogManager.getLogger(ArtifactHandlerBase.class);

  private final Executor executor;
  private final FutureJdbi jdbi;
  private final String fieldType;
  private final String entityName;
  private final String entityIdReferenceColumn;
  private final CodeVersionHandler codeVersionHandler;
  private final DatasetHandler datasetHandler;
  private final Config config = Config.getInstance();

  private final ArtifactStoreDAO artifactStoreDAO;
  private final DatasetVersionDAO datasetVersionDAO;
  private static final ModelDBHibernateUtil modelDBHibernateUtil =
      ModelDBHibernateUtil.getInstance();

  protected String getTableName() {
    return "artifact";
  }

  public ArtifactHandlerBase(
      Executor executor,
      FutureJdbi jdbi,
      String fieldType,
      String entityName,
      CodeVersionHandler codeVersionHandler,
      DatasetHandler datasetHandler,
      ArtifactStoreDAO artifactStoreDAO,
      DatasetVersionDAO datasetVersionDAO) {
    this.executor = executor;
    this.jdbi = jdbi;
    this.fieldType = fieldType;
    this.entityName = entityName;
    this.codeVersionHandler = codeVersionHandler;
    this.datasetHandler = datasetHandler;
    this.artifactStoreDAO = artifactStoreDAO;
    this.datasetVersionDAO = datasetVersionDAO;

    switch (entityName) {
      case "ProjectEntity":
        this.entityIdReferenceColumn = "project_id";
        break;
      case "ExperimentRunEntity":
        this.entityIdReferenceColumn = "experiment_run_id";
        break;
      default:
        throw new InternalErrorException("Invalid entity name: " + entityName);
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

  private InternalFuture<Optional<Long>> getArtifactId(String entityId, String key) {
    return InternalFuture.runAsync(
            () -> {
              if (key.isEmpty()) {
                throw new InvalidArgumentException("Key must be provided");
              }
            },
            executor)
        .thenCompose(
            unused ->
                jdbi.withHandle(
                    handle ->
                        handle
                            .createQuery(
                                "select id from "
                                    + getTableName()
                                    + " where entity_name=:entity_name and field_type=:field_type and "
                                    + entityIdReferenceColumn
                                    + "=:entity_id and ar_key=:ar_key")
                            .bind("entity_id", entityId)
                            .bind("field_type", fieldType)
                            .bind("entity_name", entityName)
                            .bind("ar_key", key)
                            .mapTo(Long.class)
                            .findOne()),
            executor);
  }

  public InternalFuture<List<Artifact>> getArtifacts(String entityId, Optional<String> maybeKey) {
    var currentFuture =
        InternalFuture.runAsync(
            () -> {
              if (entityId == null || entityId.isEmpty()) {
                throw new InvalidArgumentException("Entity id is empty");
              }
            },
            executor);
    return currentFuture.thenCompose(
        unused ->
            jdbi.withHandle(
                handle -> {
                  var queryStr =
                      "select ar_key as k, ar_path as p, artifact_type as at, path_only as po, linked_artifact_id as lai, filename_extension as fe from "
                          + getTableName()
                          + " where entity_name=:entity_name and field_type=:field_type and "
                          + entityIdReferenceColumn
                          + "=:entity_id";

                  if (!maybeKey.isPresent()) {
                    queryStr = queryStr + " AND ar_key=:ar_key ";
                  }

                  var query =
                      handle
                          .createQuery(queryStr)
                          .bind("entity_id", entityId)
                          .bind("field_type", fieldType)
                          .bind("entity_name", entityName);
                  if (!maybeKey.isPresent()) {
                    query.bind("ar_key", maybeKey.get());
                  }
                  List<Artifact> artifacts =
                      query
                          .map(
                              (rs, ctx) ->
                                  Artifact.newBuilder()
                                      .setKey(rs.getString("k"))
                                      .setPath(rs.getString("p"))
                                      .setArtifactTypeValue(rs.getInt("at"))
                                      .setPathOnly(rs.getBoolean("po"))
                                      .setLinkedArtifactId(rs.getString("lai"))
                                      .setFilenameExtension(rs.getString("fe"))
                                      .build())
                          .list();
                  return artifacts;
                }),
        executor);
  }

  public InternalFuture<Void> logArtifacts(
      String entityId, List<Artifact> artifacts, boolean overwrite) {
    // Validate input
    return InternalFuture.runAsync(
            () -> {
              if (entityId == null || entityId.isEmpty()) {
                throw new InvalidArgumentException("Entity id is empty");
              }
              for (final var artifact : artifacts) {
                String errorMessage = null;
                if (artifact.getKey().isEmpty()
                    && (artifact.getPathOnly() && artifact.getPath().isEmpty())) {
                  errorMessage = "Artifact key and Artifact path not found in request";
                } else if (artifact.getKey().isEmpty()) {
                  errorMessage = "Artifact key not found in request";
                } else if (artifact.getPathOnly() && artifact.getPath().isEmpty()) {
                  errorMessage = "Artifact path not found in request";
                }

                if (errorMessage != null) {
                  throw new InvalidArgumentException(errorMessage);
                }
              }
            },
            executor)
        .thenCompose(
            unused ->
                // Check for conflicts
                jdbi.useHandle(
                    handle -> {
                      if (overwrite) {
                        handle
                            .createUpdate(
                                "delete from "
                                    + getTableName()
                                    + " where entity_name=:entity_name and field_type=:field_type and ar_key in (<keys>) and "
                                    + entityIdReferenceColumn
                                    + "=:entity_id")
                            .bindList(
                                "keys",
                                artifacts.stream()
                                    .map(Artifact::getKey)
                                    .collect(Collectors.toList()))
                            .bind("field_type", fieldType)
                            .bind("entity_name", entityName)
                            .bind("entity_id", entityId);
                      } else {
                        for (final var artifact : artifacts) {
                          handle
                              .createQuery(
                                  "select id from "
                                      + getTableName()
                                      + " where entity_name=:entity_name and field_type=:field_type and ar_key=:key and "
                                      + entityIdReferenceColumn
                                      + "=:entity_id")
                              .bind("key", artifact.getKey())
                              .bind("field_type", fieldType)
                              .bind("entity_name", entityName)
                              .bind("entity_id", entityId)
                              .mapTo(Long.class)
                              .findOne()
                              .ifPresent(
                                  present -> {
                                    throw new AlreadyExistsException(
                                        "Key '" + artifact.getKey() + "' already exists");
                                  });
                        }
                      }
                    }),
            executor)
        .thenCompose(
            unused ->
                // Log
                jdbi.useHandle(
                    handle -> {
                      for (final var artifact : artifacts) {
                        var storeTypePath =
                            !artifact.getPathOnly()
                                ? Config.getInstance().artifactStoreConfig.storeTypePathPrefix()
                                    + artifact.getPath()
                                : "";
                        handle
                            .createUpdate(
                                "insert into "
                                    + getTableName()
                                    + " (entity_name, field_type, ar_key, ar_path, artifact_type, path_only, linked_artifact_id, filename_extension, store_type_path,"
                                    + entityIdReferenceColumn
                                    + ") "
                                    + "values (:entity_name, :field_type, :key, :path, :type,:path_only,:linked_artifact_id,:filename_extension,:store_type_path, :entity_id)")
                            .bind("key", artifact.getKey())
                            .bind("path", artifact.getPath())
                            .bind("type", artifact.getArtifactTypeValue())
                            .bind("path_only", artifact.getPathOnly())
                            .bind("linked_artifact_id", artifact.getLinkedArtifactId())
                            .bind("filename_extension", artifact.getFilenameExtension())
                            .bind("store_type_path", storeTypePath)
                            .bind("entity_id", entityId)
                            .bind("field_type", fieldType)
                            .bind("entity_name", entityName)
                            .execute();
                      }
                    }),
            executor);
  }

  public InternalFuture<Void> deleteArtifacts(String entityId, Optional<List<String>> maybeKeys) {
    var currentFuture =
        InternalFuture.runAsync(
            () -> {
              if (entityId == null || entityId.isEmpty()) {
                throw new InvalidArgumentException("Entity id is empty");
              }
            },
            executor);
    return currentFuture.thenCompose(
        unused ->
            jdbi.useHandle(
                handle -> {
                  var sql =
                      "delete from "
                          + getTableName()
                          + " where entity_name=:entity_name and field_type=:field_type and "
                          + entityIdReferenceColumn
                          + "=:entity_id";

                  if (maybeKeys.isPresent()) {
                    sql += " and ar_key in (<keys>)";
                  }

                  var query =
                      handle
                          .createUpdate(sql)
                          .bind("entity_id", entityId)
                          .bind("field_type", fieldType)
                          .bind("entity_name", entityName);

                  if (maybeKeys.isPresent()) {
                    query = query.bindList("keys", maybeKeys.get());
                  }

                  query.execute();
                }),
        executor);
  }
}
