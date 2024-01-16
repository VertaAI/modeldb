package ai.verta.modeldb.experimentRun.subtypes;

import ai.verta.common.CodeVersion;
import ai.verta.common.GitSnapshot;
import ai.verta.modeldb.Location;
import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.entities.code.GitCodeBlobEntity;
import ai.verta.modeldb.entities.code.NotebookCodeBlobEntity;
import ai.verta.modeldb.entities.dataset.PathDatasetComponentBlobEntity;
import ai.verta.modeldb.utils.InternalFuture;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.utils.ModelDBUtils;
import ai.verta.modeldb.versioning.Blob;
import ai.verta.modeldb.versioning.GitCodeBlob;
import ai.verta.modeldb.versioning.PathDatasetComponentBlob;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CodeVersionFromBlobHandler {
  private static final Logger LOGGER = LogManager.getLogger(CodeVersionFromBlobHandler.class);
  private final ModelDBHibernateUtil modelDBHibernateUtil = ModelDBHibernateUtil.getInstance();

  /**
   * @param expRunIds : ExperimentRun ids
   * @return {@link Map <String, Map<String, CodeBlob >>} : Map from experimentRunID to Map of
   *     LocationString to CodeVersion
   */
  public InternalFuture<Map<String, Map<String, CodeVersion>>> getExperimentRunCodeVersionMap(
      Set<String> expRunIds,
      Collection<String> selfAllowedRepositoryIds,
      boolean allowedAllRepositories) {
    if (!allowedAllRepositories) {
      // If all repositories are not allowed and some one send empty selfAllowedRepositoryIds list
      // then this will return empty list from here for security
      if (selfAllowedRepositoryIds == null || selfAllowedRepositoryIds.isEmpty()) {
        return InternalFuture.completedInternalFuture(new HashMap<>());
      }
    }

    List<Object[]> codeBlobEntities;
    try (var session = modelDBHibernateUtil.getSessionFactory().openSession()) {
      String queryBuilder =
          "SELECT vme.experimentRunEntity.id, vme.versioning_location, gcb, ncb, pdcb "
              + " From VersioningModeldbEntityMapping vme LEFT JOIN GitCodeBlobEntity gcb ON vme.blob_hash = gcb.blob_hash "
              + " LEFT JOIN NotebookCodeBlobEntity ncb ON vme.blob_hash = ncb.blob_hash "
              + " LEFT JOIN PathDatasetComponentBlobEntity pdcb ON ncb.path_dataset_blob_hash = pdcb.id.path_dataset_blob_id "
              + " WHERE vme.versioning_blob_type = :versioningBlobType AND vme.experimentRunEntity.id IN (:expRunIds) ";

      if (!allowedAllRepositories) {
        queryBuilder = queryBuilder + " AND vme.repository_id IN (:repoIds)";
      }

      var query = session.createQuery(queryBuilder);
      query.setParameter("versioningBlobType", Blob.ContentCase.CODE.getNumber());
      query.setParameterList("expRunIds", expRunIds);
      if (!allowedAllRepositories) {
        query.setParameterList(
            "repoIds",
            selfAllowedRepositoryIds.stream().map(Long::parseLong).collect(Collectors.toList()));
      }

      LOGGER.trace(
          "Final experimentRuns code config blob final query : {}", query.getQueryString());
      codeBlobEntities = query.list();
      LOGGER.trace("Final experimentRuns code config list size : {}", codeBlobEntities.size());
    }

    // Map<experimentRunID, Map<LocationString, CodeVersion>> : Map from experimentRunID to Map of
    // LocationString to CodeVersion
    Map<String, Map<String, CodeVersion>> expRunCodeBlobMap = new LinkedHashMap<>();
    if (!codeBlobEntities.isEmpty()) {
      for (Object[] objects : codeBlobEntities) {
        String expRunId = (String) objects[0];
        String versioningLocation = (String) objects[1];
        GitCodeBlobEntity gitBlobEntity = (GitCodeBlobEntity) objects[2];
        var notebookCodeBlobEntity = (NotebookCodeBlobEntity) objects[3];
        var pathDatasetComponentBlobEntity = (PathDatasetComponentBlobEntity) objects[4];

        var codeVersionBuilder = CodeVersion.newBuilder();
        LOGGER.trace("notebookCodeBlobEntity {}", notebookCodeBlobEntity);
        LOGGER.trace("pathDatasetComponentBlobEntity {}", pathDatasetComponentBlobEntity);
        LOGGER.trace("gitBlobEntity {}", gitBlobEntity);
        if (notebookCodeBlobEntity != null) {
          if (pathDatasetComponentBlobEntity != null) {
            convertGitBlobToGitSnapshot(
                codeVersionBuilder,
                notebookCodeBlobEntity.getGitCodeBlobEntity().toProto(),
                pathDatasetComponentBlobEntity.toProto());
          } else {
            convertGitBlobToGitSnapshot(
                codeVersionBuilder, notebookCodeBlobEntity.getGitCodeBlobEntity().toProto(), null);
          }
        } else if (gitBlobEntity != null) {
          convertGitBlobToGitSnapshot(codeVersionBuilder, gitBlobEntity.toProto(), null);
        }
        Map<String, CodeVersion> codeBlobMap = expRunCodeBlobMap.get(expRunId);
        if (codeBlobMap == null) {
          codeBlobMap = new LinkedHashMap<>();
        }
        var locationBuilder = Location.newBuilder();
        CommonUtils.getProtoObjectFromString(versioningLocation, locationBuilder);
        codeBlobMap.put(
            ModelDBUtils.getLocationWithSlashOperator(locationBuilder.getLocationList()),
            codeVersionBuilder.build());
        expRunCodeBlobMap.put(expRunId, codeBlobMap);
      }
    }
    return InternalFuture.completedInternalFuture(expRunCodeBlobMap);
  }

  private void convertGitBlobToGitSnapshot(
      CodeVersion.Builder codeVersionBuilder,
      GitCodeBlob codeBlob,
      PathDatasetComponentBlob pathComponentBlob) {
    var gitSnapShot = GitSnapshot.newBuilder();
    if (codeBlob != null) {
      gitSnapShot
          .setRepo(codeBlob.getRepo())
          .setHash(codeBlob.getHash())
          .setIsDirtyValue(codeBlob.getIsDirty() ? 1 : 2)
          .build();
    }
    if (pathComponentBlob != null) {
      gitSnapShot.addFilepaths(pathComponentBlob.getPath());
    }
    codeVersionBuilder.setGitSnapshot(gitSnapShot);
  }
}
