package ai.verta.modeldb.versioning;

import static java.util.stream.Collectors.toMap;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.versioning.CommitEntity;
import ai.verta.modeldb.entities.versioning.InternalFolderElementEntity;
import ai.verta.modeldb.entities.versioning.RepositoryEntity;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import ai.verta.modeldb.versioning.BlobDiff.ContentCase;
import ai.verta.modeldb.versioning.DiffStatusEnum.DiffStatus;
import ai.verta.modeldb.versioning.blob.container.BlobContainer;
import ai.verta.modeldb.versioning.blob.diffFactory.BlobDiffFactory;
import ai.verta.modeldb.versioning.blob.factory.BlobFactory;
import com.google.protobuf.ProtocolStringList;
import io.grpc.Status;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class BlobDAORdbImpl implements BlobDAO {

  private static final Logger LOGGER = LogManager.getLogger(BlobDAORdbImpl.class);

  public static final String TREE = "TREE";

  /**
   * Goes through each BlobExpanded creating TREE/BLOB node top down and computing SHA bottom up
   * there is a rootSHA which holds one TREE node of each BlobExpanded
   *
   * @throws ModelDBException
   */
  @Override
  public String setBlobs(List<BlobContainer> blobContainers, FileHasher fileHasher)
      throws NoSuchAlgorithmException, ModelDBException {
    TreeElem rootTree = new TreeElem();
    for (BlobContainer blobContainer : blobContainers) {
      // should save each blob during one session to avoid recurring entities ids
      try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
        session.beginTransaction();
        blobContainer.process(session, rootTree, fileHasher);
        session.getTransaction().commit();
      }
    }
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      final InternalFolderElement internalFolderElement = rootTree.saveFolders(session, fileHasher);
      session.getTransaction().commit();
      return internalFolderElement.getElementSha();
    }
  }

  private Blob getBlob(Session session, InternalFolderElementEntity folderElementEntity)
      throws ModelDBException {
    return BlobFactory.create(folderElementEntity).getBlob(session);
  }

  private Folder getFolder(Session session, String commitSha, String folderSha) {
    Optional result =
        session
            .createQuery(
                "From "
                    + InternalFolderElementEntity.class.getSimpleName()
                    + " where folder_hash = '"
                    + folderSha
                    + "'")
            .list().stream()
            .map(
                d -> {
                  InternalFolderElementEntity entity = (InternalFolderElementEntity) d;
                  Folder.Builder folder = Folder.newBuilder();
                  FolderElement.Builder folderElement =
                      FolderElement.newBuilder()
                          .setElementName(entity.getElement_name())
                          .setCreatedByCommit(commitSha);

                  if (entity.getElement_type().equals(TREE)) {
                    folder.addSubFolders(folderElement);
                  } else {
                    folder.addBlobs(folderElement);
                  }
                  return folder.build();
                })
            .reduce((a, b) -> ((Folder) a).toBuilder().mergeFrom((Folder) b).build());

    if (result.isPresent()) {
      return (Folder) result.get();
    } else {
      return null;
    }
  }

  // TODO : check if there is a way to optimize on the calls to data base.
  // We should fetch data  in a single query.
  @Override
  public GetCommitComponentRequest.Response getCommitComponent(
      RepositoryFunction repositoryFunction, String commitHash, ProtocolStringList locationList)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      RepositoryEntity repository = repositoryFunction.apply(session);
      CommitEntity commit = session.get(CommitEntity.class, commitHash);

      if (commit == null) {
        throw new ModelDBException("No such commit", Status.Code.NOT_FOUND);
      }

      if (!VersioningUtils.commitRepositoryMappingExists(session, commitHash, repository.getId())) {
        throw new ModelDBException("No such commit found in the repository", Status.Code.NOT_FOUND);
      }

      String folderHash = commit.getRootSha();
      if (locationList.isEmpty()) { // getting root
        Folder folder = getFolder(session, commit.getCommit_hash(), folderHash);
        session.getTransaction().commit();
        if (folder == null) { // root is empty
          return GetCommitComponentRequest.Response.newBuilder().build();
        }
        return GetCommitComponentRequest.Response.newBuilder().setFolder(folder).build();
      }
      for (int index = 0; index < locationList.size(); index++) {
        String folderLocation = locationList.get(index);
        String folderQueryHQL =
            "From "
                + InternalFolderElementEntity.class.getSimpleName()
                + " parentIfe WHERE parentIfe.element_name = :location AND parentIfe.folder_hash = :folderHash";
        Query<InternalFolderElementEntity> fetchTreeQuery = session.createQuery(folderQueryHQL);
        fetchTreeQuery.setParameter("location", folderLocation);
        fetchTreeQuery.setParameter("folderHash", folderHash);
        InternalFolderElementEntity elementEntity = fetchTreeQuery.uniqueResult();

        if (elementEntity == null) {
          LOGGER.warn(
              "No such folder found : {}. Failed at index {} looking for {}",
              folderLocation,
              index,
              folderLocation);
          throw new ModelDBException(
              "No such folder found : " + folderLocation, Status.Code.NOT_FOUND);
        }
        if (elementEntity.getElement_type().equals(TREE)) {
          folderHash = elementEntity.getElement_sha();
          if (index == locationList.size() - 1) {
            Folder folder = getFolder(session, commit.getCommit_hash(), folderHash);
            session.getTransaction().commit();
            if (folder == null) { // folder is empty
              return GetCommitComponentRequest.Response.newBuilder().build();
            }
            return GetCommitComponentRequest.Response.newBuilder().setFolder(folder).build();
          }
        } else {
          if (index == locationList.size() - 1) {
            Blob blob = getBlob(session, elementEntity);
            session.getTransaction().commit();
            return GetCommitComponentRequest.Response.newBuilder().setBlob(blob).build();
          } else {
            throw new ModelDBException(
                "No such folder found : " + locationList.get(index + 1), Status.Code.NOT_FOUND);
          }
        }
      }
    } catch (Throwable throwable) {
      if (throwable instanceof ModelDBException) {
        throw (ModelDBException) throwable;
      }
      LOGGER.warn(throwable);
      throw new ModelDBException("Unknown error", Status.Code.INTERNAL);
    }
    throw new ModelDBException(
        "Unexpected logic issue found when fetching blobs", Status.Code.UNKNOWN);
  }

  /**
   * get the Folder Element pointed to by the parentFolderHash and elementName
   *
   * @param session
   * @param parentFolderHash : folder hash of the parent
   * @param elementName : element name of the element to be fetched
   * @return {@link List<InternalFolderElementEntity>}
   */
  private List<InternalFolderElementEntity> getFolderElement(
      Session session, String parentFolderHash, String elementName) {
    StringBuilder folderQueryHQLBuilder =
        new StringBuilder("From ")
            .append(InternalFolderElementEntity.class.getSimpleName())
            .append(" parentIfe WHERE parentIfe.folder_hash = :folderHash ");

    if (elementName != null && !elementName.isEmpty()) {
      folderQueryHQLBuilder.append("AND parentIfe.element_name = :elementName");
    }

    Query<InternalFolderElementEntity> fetchTreeQuery =
        session.createQuery(folderQueryHQLBuilder.toString());
    fetchTreeQuery.setParameter("folderHash", parentFolderHash);
    if (elementName != null && !elementName.isEmpty()) {
      fetchTreeQuery.setParameter("elementName", elementName);
    }
    return fetchTreeQuery.list();
  }

  boolean childContains(Set<?> list, Set<?> sublist) {
    return Collections.indexOfSubList(new LinkedList<>(list), new LinkedList<>(sublist)) != -1;
  }

  private Map<String, Map.Entry<BlobExpanded, String>> getChildFolderBlobMap(
      Session session,
      List<String> requestedLocation,
      Set<String> parentLocation,
      String parentFolderHash)
      throws ModelDBException {
    String folderQueryHQL =
        "From "
            + InternalFolderElementEntity.class.getSimpleName()
            + " parentIfe WHERE parentIfe.folder_hash = :folderHash";
    Query<InternalFolderElementEntity> fetchTreeQuery = session.createQuery(folderQueryHQL);
    fetchTreeQuery.setParameter("folderHash", parentFolderHash);
    List<InternalFolderElementEntity> childElementFolders = fetchTreeQuery.list();

    Map<String, Map.Entry<BlobExpanded, String>> childBlobExpandedMap = new LinkedHashMap<>();
    for (InternalFolderElementEntity childElementFolder : childElementFolders) {
      if (childElementFolder.getElement_type().equals(TREE)) {
        Set<String> childLocation = new LinkedHashSet<>(parentLocation);
        childLocation.add(childElementFolder.getElement_name());
        if (childContains(new LinkedHashSet<>(requestedLocation), childLocation)
            || childLocation.containsAll(requestedLocation)) {
          childBlobExpandedMap.putAll(
              getChildFolderBlobMap(
                  session, requestedLocation, childLocation, childElementFolder.getElement_sha()));
        }
      } else {
        if (parentLocation.containsAll(requestedLocation)) {
          Blob blob = getBlob(session, childElementFolder);
          BlobExpanded blobExpanded =
              BlobExpanded.newBuilder()
                  .addAllLocation(parentLocation)
                  .addLocation(childElementFolder.getElement_name())
                  .setBlob(blob)
                  .build();
          childBlobExpandedMap.put(
              getStringFromLocationList(blobExpanded.getLocationList()),
              new AbstractMap.SimpleEntry<>(blobExpanded, childElementFolder.getElement_sha()));
        }
      }
    }
    return childBlobExpandedMap;
  }

  /**
   * Given a folderHash and a location list, collects all the blobs along the location list and
   * returns them with their location as set
   *
   * @param session
   * @param folderHash : the base folder to start the search for location list
   * @param locationList : list of trees and psossibly terminating with blob
   * @return
   * @throws ModelDBException
   */
  @Override
  public Map<String, BlobExpanded> getCommitBlobMap(
      Session session, String folderHash, List<String> locationList) throws ModelDBException {
    return convertToLocationBlobMap(getCommitBlobMapWithHash(session, folderHash, locationList));
  }

  private Map<String, BlobExpanded> convertToLocationBlobMap(
      Map<String, Map.Entry<BlobExpanded, String>> commitBlobMapWithHash) {
    return commitBlobMapWithHash.entrySet().stream()
        .collect(
            Collectors.toMap(
                Entry::getKey, stringEntryEntry -> stringEntryEntry.getValue().getKey()));
  }

  Map<String, Map.Entry<BlobExpanded, String>> getCommitBlobMapWithHash(
      Session session, String folderHash, List<String> locationList) throws ModelDBException {

    String parentLocation = locationList.size() == 0 ? null : locationList.get(0);
    List<InternalFolderElementEntity> parentFolderElementList =
        getFolderElement(session, folderHash, parentLocation);
    if (parentFolderElementList == null || parentFolderElementList.isEmpty()) {
      if (parentLocation
          != null) { // = null mainly is supporting the call on init commit which is an empty commit
        throw new ModelDBException(
            "No such folder found : " + parentLocation, Status.Code.NOT_FOUND);
      }
    }

    Map<String, Map.Entry<BlobExpanded, String>> finalLocationBlobMap = new LinkedHashMap<>();
    for (InternalFolderElementEntity parentFolderElement : parentFolderElementList) {
      if (!parentFolderElement.getElement_type().equals(TREE)) {
        Blob blob = getBlob(session, parentFolderElement);
        BlobExpanded blobExpanded =
            BlobExpanded.newBuilder()
                .addLocation(parentFolderElement.getElement_name())
                .setBlob(blob)
                .build();
        finalLocationBlobMap.put(
            getStringFromLocationList(blobExpanded.getLocationList()),
            new SimpleEntry<>(blobExpanded, parentFolderElement.getElement_sha()));
      } else {
        // if this is tree, search further
        Set<String> location = new LinkedHashSet<>();
        Map<String, Map.Entry<BlobExpanded, String>> locationBlobList =
            getChildFolderBlobMap(session, locationList, location, folderHash);
        finalLocationBlobMap.putAll(locationBlobList);
      }
    }

    Comparator<Map.Entry<String, Map.Entry<BlobExpanded, String>>> locationComparator =
        Comparator.comparing(
            (Map.Entry<String, Map.Entry<BlobExpanded, String>> o) ->
                o.getKey().replaceAll("#", ""));

    finalLocationBlobMap =
        finalLocationBlobMap.entrySet().stream()
            .sorted(locationComparator)
            .collect(
                toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));

    return finalLocationBlobMap;
  }

  @Override
  public ListCommitBlobsRequest.Response getCommitBlobsList(
      RepositoryFunction repositoryFunction, String commitHash, ProtocolStringList locationList)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();

      CommitEntity commit = session.get(CommitEntity.class, commitHash);
      if (commit == null) {
        throw new ModelDBException("No such commit", Status.Code.NOT_FOUND);
      }

      RepositoryEntity repository = repositoryFunction.apply(session);
      if (!VersioningUtils.commitRepositoryMappingExists(session, commitHash, repository.getId())) {
        throw new ModelDBException("No such commit found in the repository", Status.Code.NOT_FOUND);
      }
      Map<String, BlobExpanded> locationBlobMap =
          getCommitBlobMap(session, commit.getRootSha(), locationList);
      return ListCommitBlobsRequest.Response.newBuilder()
          .addAllBlobs(locationBlobMap.values())
          .build();
    } catch (Throwable throwable) {
      throwable.printStackTrace();
      if (throwable instanceof ModelDBException) {
        throw (ModelDBException) throwable;
      }
      throw new ModelDBException("Unknown error", Status.Code.INTERNAL);
    }
  }

  @Override
  public ComputeRepositoryDiffRequest.Response computeRepositoryDiff(
      RepositoryFunction repositoryFunction, ComputeRepositoryDiffRequest request)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      session.beginTransaction();
      RepositoryEntity repositoryEntity = repositoryFunction.apply(session);

      CommitEntity internalCommitA = session.get(CommitEntity.class, request.getCommitA());
      if (internalCommitA == null) {
        throw new ModelDBException(
            "No such commit found : " + request.getCommitA(), Status.Code.NOT_FOUND);
      }

      CommitEntity internalCommitB = session.get(CommitEntity.class, request.getCommitB());
      if (internalCommitB == null) {
        throw new ModelDBException(
            "No such commit found : " + request.getCommitB(), Status.Code.NOT_FOUND);
      }

      if (!VersioningUtils.commitRepositoryMappingExists(
          session, internalCommitA.getCommit_hash(), repositoryEntity.getId())) {
        throw new ModelDBException(
            "No such commit found in the repository : " + internalCommitA.getCommit_hash(),
            Status.Code.NOT_FOUND);
      }

      if (!VersioningUtils.commitRepositoryMappingExists(
          session, internalCommitB.getCommit_hash(), repositoryEntity.getId())) {
        throw new ModelDBException(
            "No such commit found in the repository : " + internalCommitB.getCommit_hash(),
            Status.Code.NOT_FOUND);
      }
      // get list of blob expanded in both commit and group them in a map based on location
      Map<String, Map.Entry<BlobExpanded, String>> locationBlobsMapCommitA =
          getCommitBlobMapWithHash(session, internalCommitA.getRootSha(), new ArrayList<>());

      Map<String, Map.Entry<BlobExpanded, String>> locationBlobsMapCommitB =
          getCommitBlobMapWithHash(session, internalCommitB.getRootSha(), new ArrayList<>());

      session.getTransaction().commit();

      // Added new blob location in the CommitB, locations in
      Set<String> addedLocations = new LinkedHashSet<>(locationBlobsMapCommitB.keySet());
      addedLocations.removeAll(locationBlobsMapCommitA.keySet());
      LOGGER.debug("Added location for Diff : {}", addedLocations);

      // deleted new blob location from the CommitA
      Set<String> deletedLocations = new LinkedHashSet<>(locationBlobsMapCommitA.keySet());
      deletedLocations.removeAll(locationBlobsMapCommitB.keySet());
      LOGGER.debug("Deleted location for Diff : {}", deletedLocations);

      // modified new blob location from the CommitA
      Set<String> modifiedLocations = new LinkedHashSet<>(locationBlobsMapCommitB.keySet());
      modifiedLocations.removeAll(addedLocations);
      Map<String, BlobExpanded> commonBlobs =
          locationBlobsMapCommitA.values().stream().collect(toMap(Entry::getValue, Entry::getKey));
      commonBlobs
          .keySet()
          .retainAll(
              locationBlobsMapCommitB.values().stream()
                  .map(Entry::getValue)
                  .collect(Collectors.toSet()));
      Map<String, BlobExpanded> locationBlobsCommon =
          getLocationWiseBlobExpandedMapFromCollection(commonBlobs.values());
      modifiedLocations.removeAll(locationBlobsCommon.keySet());
      LOGGER.debug("Modified location for Diff : {}", modifiedLocations);

      List<BlobDiff> addedBlobDiffList =
          getAddedBlobDiff(addedLocations, convertToLocationBlobMap(locationBlobsMapCommitB));
      List<BlobDiff> deletedBlobDiffList =
          getDeletedBlobDiff(deletedLocations, convertToLocationBlobMap(locationBlobsMapCommitA));
      List<BlobDiff> modifiedBlobDiffList =
          getModifiedBlobDiff(
              modifiedLocations,
              convertToLocationBlobMap(locationBlobsMapCommitA),
              convertToLocationBlobMap(locationBlobsMapCommitB));

      return ComputeRepositoryDiffRequest.Response.newBuilder()
          .addAllDiffs(addedBlobDiffList)
          .addAllDiffs(deletedBlobDiffList)
          .addAllDiffs(modifiedBlobDiffList)
          .build();
    }
  }

  List<BlobDiff> getAddedBlobDiff(
      Set<String> addedLocations, Map<String, BlobExpanded> locationBlobsMapCommitB) {
    return addedLocations.stream()
        .map(
            location -> {
              BlobExpanded blobExpanded = locationBlobsMapCommitB.get(location);
              BlobDiffFactory result = BlobDiffFactory.create(blobExpanded);
              return result.add(location);
            })
        .collect(Collectors.toList());
  }

  List<BlobDiff> getDeletedBlobDiff(
      Set<String> deletedLocations, Map<String, BlobExpanded> locationBlobsMapCommitA) {
    return deletedLocations.stream()
        .map(
            location -> {
              BlobExpanded blobExpanded = locationBlobsMapCommitA.get(location);
              BlobDiffFactory result = BlobDiffFactory.create(blobExpanded);
              return result.delete(location);
            })
        .collect(Collectors.toList());
  }

  List<BlobDiff> getModifiedBlobDiff(
      Set<String> modifiedLocations,
      Map<String, BlobExpanded> locationBlobsMapCommitA,
      Map<String, BlobExpanded> locationBlobsMapCommitB) {
    return modifiedLocations.stream()
        .flatMap(
            location -> {
              BlobExpanded blobExpandedCommitA = locationBlobsMapCommitA.get(location);
              BlobExpanded blobExpandedCommitB = locationBlobsMapCommitB.get(location);
              BlobDiffFactory a = BlobDiffFactory.create(blobExpandedCommitA);
              BlobDiffFactory b = BlobDiffFactory.create(blobExpandedCommitB);
              return a.compare(b, location).stream();
            })
        .collect(Collectors.toList());
  }

  private Map<String, BlobExpanded> getLocationWiseBlobExpandedMapFromCollection(
      Collection<BlobExpanded> blobExpandeds) {
    return blobExpandeds.stream()
        .collect(
            Collectors.toMap(
                // TODO: Here used the `#` for joining the locations but if folder locations contain
                // TODO: - the `#` then this functionality will break.
                blobExpanded -> getStringFromLocationList(blobExpanded.getLocationList()),
                blobExpanded -> blobExpanded));
  }

  private String getStringFromLocationList(List<String> locationList) {
    return String.join("#", locationList);
  }

  private List<String> getLocationListFromString(String locationString) {
    return Arrays.asList(locationString.split("#"));
  }

  @Override
  public List<BlobContainer> convertBlobDiffsToBlobs(
      CreateCommitRequest request,
      RepositoryFunction repositoryFunction,
      CommitFunction commitFunction)
      throws ModelDBException {
    try (Session session = ModelDBHibernateUtil.getSessionFactory().openSession()) {
      RepositoryEntity repositoryEntity = repositoryFunction.apply(session);
      CommitEntity commitEntity = commitFunction.apply(session, session1 -> repositoryEntity);
      Map<String, BlobExpanded> locationBlobsMap =
          getCommitBlobMap(session, commitEntity.getRootSha(), new ArrayList<>());
      Map<String, BlobExpanded> locationBlobsMapNew = new LinkedHashMap<>();
      for (BlobDiff blobDiff : request.getDiffsList()) {
        final ProtocolStringList locationList = blobDiff.getLocationList();
        if (locationList == null || locationList.isEmpty()) {
          throw new ModelDBException(
              "Location in BlobDiff should not be empty", Status.Code.INVALID_ARGUMENT);
        }
        // Apply the diffs on top of commit_base
        if (blobDiff.getStatus() == DiffStatus.ADDED) {
          // If a blob was added in the diff, add it on top of commit_base (doesn't matter
          // if it was present already or not)
          BlobExpanded blobExpanded = convertBlobDiffToBlobExpand(blobDiff);
          locationBlobsMapNew.put(
              getStringFromLocationList(blobExpanded.getLocationList()), blobExpanded);
        } else {
          if (blobDiff.getStatus() == DiffStatus.DELETED) {
            // If a blob was deleted, delete if from commit_base if present
            locationBlobsMap.remove(getStringFromLocationList(locationList));
          } else if (blobDiff.getStatus() == DiffStatus.MODIFIED) {
            // If a blob was modified, then:
            BlobExpanded blobExpanded =
                locationBlobsMap.get(getStringFromLocationList(locationList));
            if (blobExpanded == null) {
              throw new ModelDBException(
                  "Blob with path: " + locationList + " not found", Status.Code.NOT_FOUND);
            }
            // 1) check that the type of the diff is consistent with the type of the blob. If
            // they are different, raise an error saying so
            BlobExpanded blobDiffExpanded = convertBlobDiffToBlobExpand(blobDiff);
            checkType(blobDiffExpanded, blobExpanded);
            // 2) apply the diff to the blob as per the following logic:
            if (isAtomic(blobDiff.getContentCase())) {
              // 2a) if the field is atomic (e.g. python version, git repository), use the
              // newer version (B) from the diff and overwrite what the commit_base has
              blobExpanded = atomicResult(blobDiff);
              locationBlobsMapNew.put(
                  getStringFromLocationList(blobExpanded.getLocationList()), blobExpanded);
            } else {
              // 2b) if the field is not atomic (e.g. list of python requirements, dataset
              // components), merge the lists by a) copying new values, b) deleting removed
              // values, c) updating values that are already present based on some reasonable
              // key
              blobExpanded =
                  complexResult(blobDiff, blobExpanded).addAllLocation(locationList).build();
              locationBlobsMapNew.put(
                  getStringFromLocationList(blobExpanded.getLocationList()), blobExpanded);
            }
          } else {
            throw new ModelDBException("Invalid ModelDB diff type", Status.Code.INTERNAL);
          }
        }
      }
      locationBlobsMap.putAll(locationBlobsMapNew);
      List<BlobContainer> blobContainerList = new LinkedList<>();
      for (Map.Entry<String, BlobExpanded> blobExpandedEntry : locationBlobsMap.entrySet()) {
        blobContainerList.add(BlobContainer.create(blobExpandedEntry.getValue()));
      }
      return blobContainerList;
    }
  }

  private <T> Map<String, T> convertComponentsListToMap(
      List<T> configBlob, Function<T, String> getKey) {
    return configBlob.stream()
        .collect(
            Collectors.toMap(
                getKey,
                hyperparameterSetConfigBlob -> hyperparameterSetConfigBlob,
                (a, b) -> a,
                HashMap::new));
  }

  private BlobExpanded.Builder complexResult(BlobDiff blobDiff, BlobExpanded blobExpanded)
      throws ModelDBException {
    BlobExpanded.Builder blobExpandedNew = BlobExpanded.newBuilder();
    switch (blobDiff.getContentCase()) {
      case CONFIG:
        Map<String, HyperparameterSetConfigBlob> hyperparameterSetMap =
            convertComponentsListToMap(
                blobExpanded.getBlob().getConfig().getHyperparameterSetList(),
                HyperparameterSetConfigBlob::getName);

        blobDiff
            .getConfig()
            .getHyperparameterSet()
            .getAList()
            .forEach(
                hyperparameterSetConfigBlob ->
                    hyperparameterSetMap.remove(hyperparameterSetConfigBlob.getName()));
        hyperparameterSetMap.putAll(
            convertComponentsListToMap(
                blobDiff.getConfig().getHyperparameterSet().getBList(),
                HyperparameterSetConfigBlob::getName));

        Map<String, HyperparameterConfigBlob> hyperparameterMap =
            convertComponentsListToMap(
                blobExpanded.getBlob().getConfig().getHyperparametersList(),
                HyperparameterConfigBlob::getName);

        blobDiff
            .getConfig()
            .getHyperparameters()
            .getAList()
            .forEach(
                hyperparameterConfigBlob ->
                    hyperparameterMap.remove(hyperparameterConfigBlob.getName()));
        hyperparameterMap.putAll(
            convertComponentsListToMap(
                blobDiff.getConfig().getHyperparameters().getBList(),
                HyperparameterConfigBlob::getName));

        blobExpandedNew
            .setBlob(
                Blob.newBuilder()
                    .setConfig(
                        ConfigBlob.newBuilder()
                            .addAllHyperparameters(hyperparameterMap.values())
                            .addAllHyperparameterSet(hyperparameterSetMap.values())))
            .build();
        break;
      case DATASET:
        final DatasetBlob dataset = blobExpanded.getBlob().getDataset();
        switch (dataset.getContentCase()) {
          case PATH:
            Map<String, PathDatasetComponentBlob> pathMap =
                convertComponentsListToMap(
                    dataset.getPath().getComponentsList(), PathDatasetComponentBlob::getPath);

            blobDiff
                .getDataset()
                .getPath()
                .getAList()
                .forEach(
                    pathDatasetComponentBlob -> pathMap.remove(pathDatasetComponentBlob.getPath()));
            pathMap.putAll(
                convertComponentsListToMap(
                    blobDiff.getDataset().getPath().getBList(), PathDatasetComponentBlob::getPath));

            blobExpandedNew.setBlob(
                Blob.newBuilder()
                    .setDataset(
                        DatasetBlob.newBuilder()
                            .setPath(
                                PathDatasetBlob.newBuilder().addAllComponents(pathMap.values()))));
            break;
          case S3:
            Map<String, S3DatasetComponentBlob> s3Map =
                convertComponentsListToMap(
                    dataset.getS3().getComponentsList(),
                    s3DatasetComponentBlob -> s3DatasetComponentBlob.getPath().getPath());

            blobDiff
                .getDataset()
                .getS3()
                .getAList()
                .forEach(
                    s3DatasetComponentBlob ->
                        s3Map.remove(s3DatasetComponentBlob.getPath().getPath()));
            s3Map.putAll(
                convertComponentsListToMap(
                    blobDiff.getDataset().getS3().getBList(),
                    s3DatasetComponentBlob -> s3DatasetComponentBlob.getPath().getPath()));

            blobExpandedNew.setBlob(
                Blob.newBuilder()
                    .setDataset(
                        DatasetBlob.newBuilder()
                            .setS3(S3DatasetBlob.newBuilder().addAllComponents(s3Map.values()))));
            break;
          case CONTENT_NOT_SET:
          default:
            throw new ModelDBException("Unknown blob type", Status.Code.INVALID_ARGUMENT);
        }
        break;
      case ENVIRONMENT:
        EnvironmentDiff environmentDiff = blobDiff.getEnvironment();
        EnvironmentBlob.Builder environmentBlob = EnvironmentBlob.newBuilder();
        final EnvironmentBlob environment = blobExpanded.getBlob().getEnvironment();
        Map<String, EnvironmentVariablesBlob> environmentVariablesBlobMap =
            convertComponentsListToMap(
                environment.getEnvironmentVariablesList(), EnvironmentVariablesBlob::getName);
        environmentDiff
            .getEnvironmentVariablesAList()
            .forEach(
                environmentVariablesBlob ->
                    environmentVariablesBlobMap.remove(environmentVariablesBlob.getName()));
        environmentVariablesBlobMap.putAll(
            convertComponentsListToMap(
                environmentDiff.getEnvironmentVariablesBList(), EnvironmentVariablesBlob::getName));
        environmentBlob.addAllEnvironmentVariables(environmentVariablesBlobMap.values());
        environmentBlob.addAllCommandLine(environmentDiff.getCommandLineBList());
        switch (environmentDiff.getContentCase()) {
          case DOCKER:
            environmentBlob.setDocker(environmentDiff.getDocker().getB());
            break;
          case PYTHON:
            final PythonEnvironmentDiff pythonDiff = environmentDiff.getPython();
            Map<String, PythonRequirementEnvironmentBlob> pythonRequirementEnvironmentBlobMap =
                convertComponentsListToMap(
                    environment.getPython().getRequirementsList(),
                    PythonRequirementEnvironmentBlob::getLibrary);
            pythonDiff
                .getA()
                .getRequirementsList()
                .forEach(
                    pythonRequirementEnvironmentBlob ->
                        pythonRequirementEnvironmentBlobMap.remove(
                            pythonRequirementEnvironmentBlob.getLibrary()));
            pythonRequirementEnvironmentBlobMap.putAll(
                convertComponentsListToMap(
                    pythonDiff.getB().getRequirementsList(),
                    PythonRequirementEnvironmentBlob::getLibrary));
            Map<String, PythonRequirementEnvironmentBlob> pythonConstraintEnvironmentBlobMap =
                convertComponentsListToMap(
                    environment.getPython().getConstraintsList(),
                    PythonRequirementEnvironmentBlob::getLibrary);
            pythonDiff
                .getA()
                .getConstraintsList()
                .forEach(
                    pythonConstraintEnvironmentBlob ->
                        pythonConstraintEnvironmentBlobMap.remove(
                            pythonConstraintEnvironmentBlob.getLibrary()));
            pythonConstraintEnvironmentBlobMap.putAll(
                convertComponentsListToMap(
                    pythonDiff.getB().getConstraintsList(),
                    PythonRequirementEnvironmentBlob::getLibrary));
            environmentBlob.setPython(
                environmentDiff
                    .getPython()
                    .getB()
                    .toBuilder()
                    .clearRequirements()
                    .clearConstraints()
                    .addAllRequirements(pythonRequirementEnvironmentBlobMap.values())
                    .addAllConstraints(pythonConstraintEnvironmentBlobMap.values()));
            break;
          case CONTENT_NOT_SET:
          default:
            throw new ModelDBException("Unknown blob type", Status.Code.INVALID_ARGUMENT);
        }
        return blobExpandedNew.setBlob(
            Blob.newBuilder().setEnvironment(environmentBlob.build()).build());
      case CONTENT_NOT_SET:
      default:
        throw new ModelDBException("Unknown blob type", Status.Code.INVALID_ARGUMENT);
    }
    return blobExpandedNew;
  }

  private BlobExpanded atomicResult(BlobDiff blobDiff) throws ModelDBException {
    switch (blobDiff.getContentCase()) {
      case CODE:
        return convertBlobDiffToBlobExpand(blobDiff);
      case ENVIRONMENT:
      case DATASET:
      case CONFIG:
      case CONTENT_NOT_SET:
      default:
        throw new ModelDBException(
            "Invalid blob type found in BlobDiff", Status.Code.INVALID_ARGUMENT);
    }
  }

  private boolean isAtomic(ContentCase contentCase) {
    return contentCase == ContentCase.CODE;
  }

  /**
   * checks that the type of blobs are same till the inner most level.
   *
   * @param existingBlob : existing blob of the commit_base
   * @param blobDiffExpanded : new BlobDiff
   * @throws ModelDBException modelDBException
   */
  private void checkType(BlobExpanded blobDiffExpanded, BlobExpanded existingBlob)
      throws ModelDBException {
    if (!blobDiffExpanded
        .getBlob()
        .getContentCase()
        .equals(existingBlob.getBlob().getContentCase())) {
      throw new ModelDBException(
          "Modified blob type not matched with actual blob type", Status.Code.INVALID_ARGUMENT);
    } else {
      Blob newBlob = blobDiffExpanded.getBlob();
      switch (newBlob.getContentCase()) {
        case DATASET:
          if (!newBlob
              .getDataset()
              .getContentCase()
              .equals(existingBlob.getBlob().getDataset().getContentCase())) {
            throw new ModelDBException(
                "Modified dataset blob type not matched with actual blob type",
                Status.Code.INVALID_ARGUMENT);
          }
          break;
        case ENVIRONMENT:
          if (!newBlob
              .getEnvironment()
              .getContentCase()
              .equals(existingBlob.getBlob().getEnvironment().getContentCase())) {
            throw new ModelDBException(
                "Modified environment blob type not matched with actual blob type",
                Status.Code.INVALID_ARGUMENT);
          }
          break;
        case CODE:
          if (!newBlob
              .getCode()
              .getContentCase()
              .equals(existingBlob.getBlob().getCode().getContentCase())) {
            throw new ModelDBException(
                "Modified code blob type not matched with actual blob type",
                Status.Code.INVALID_ARGUMENT);
          }
        case CONFIG:
          // do nothing to check
          break;
        case CONTENT_NOT_SET:
        default:
          throw new ModelDBException("Unknown blob type found", Status.Code.INVALID_ARGUMENT);
      }
    }
  }

  private BlobExpanded convertBlobDiffToBlobExpand(BlobDiff blobDiff) throws ModelDBException {
    switch (blobDiff.getContentCase()) {
      case DATASET:
        DatasetBlob datasetBlob;
        switch (blobDiff.getDataset().getContentCase()) {
          case PATH:
            PathDatasetDiff pathDatasetDiff = blobDiff.getDataset().getPath();
            PathDatasetBlob pathDatasetBlob =
                PathDatasetBlob.newBuilder().addAllComponents(pathDatasetDiff.getBList()).build();
            datasetBlob = DatasetBlob.newBuilder().setPath(pathDatasetBlob).build();
            break;
          case S3:
            S3DatasetDiff s3DatasetDiff = blobDiff.getDataset().getS3();
            S3DatasetBlob s3DatasetBlob =
                S3DatasetBlob.newBuilder().addAllComponents(s3DatasetDiff.getBList()).build();
            datasetBlob = DatasetBlob.newBuilder().setS3(s3DatasetBlob).build();
            break;
          case CONTENT_NOT_SET:
          default:
            throw new ModelDBException("Unknown blob type", Status.Code.INVALID_ARGUMENT);
        }

        return BlobExpanded.newBuilder()
            .addAllLocation(blobDiff.getLocationList())
            .setBlob(Blob.newBuilder().setDataset(datasetBlob).build())
            .build();
      case ENVIRONMENT:
        EnvironmentDiff environmentDiff = blobDiff.getEnvironment();
        EnvironmentBlob.Builder environmentBlob =
            EnvironmentBlob.newBuilder()
                .addAllCommandLine(environmentDiff.getCommandLineBList())
                .addAllEnvironmentVariables(environmentDiff.getEnvironmentVariablesBList());
        switch (environmentDiff.getContentCase()) {
          case DOCKER:
            environmentBlob.setDocker(environmentDiff.getDocker().getB());
            break;
          case PYTHON:
            environmentBlob.setPython(environmentDiff.getPython().getB());
            break;
          case CONTENT_NOT_SET:
          default:
            throw new ModelDBException("Unknown blob type", Status.Code.INVALID_ARGUMENT);
        }
        return BlobExpanded.newBuilder()
            .addAllLocation(blobDiff.getLocationList())
            .setBlob(Blob.newBuilder().setEnvironment(environmentBlob.build()).build())
            .build();
      case CODE:
        CodeBlob.Builder codeBlobBuilder = CodeBlob.newBuilder();
        CodeDiff codeDiff = blobDiff.getCode();
        switch (codeDiff.getContentCase()) {
          case GIT:
            codeBlobBuilder.setGit(codeDiff.getGit().getB());
            break;
          case NOTEBOOK:
            codeBlobBuilder.setNotebook(codeDiff.getNotebook().getB());
            break;
          case CONTENT_NOT_SET:
          default:
            throw new ModelDBException("Unknown blob type", Status.Code.INVALID_ARGUMENT);
        }
        return BlobExpanded.newBuilder()
            .addAllLocation(blobDiff.getLocationList())
            .setBlob(Blob.newBuilder().setCode(codeBlobBuilder.build()).build())
            .build();
      case CONFIG:
        HyperparameterConfigDiff hyperparameterConfigDiff =
            blobDiff.getConfig().getHyperparameters();
        HyperparameterSetConfigDiff hyperparameterSetConfigDiff =
            blobDiff.getConfig().getHyperparameterSet();

        ConfigBlob configBlob =
            ConfigBlob.newBuilder()
                .addAllHyperparameters(hyperparameterConfigDiff.getBList())
                .addAllHyperparameterSet(hyperparameterSetConfigDiff.getBList())
                .build();
        return BlobExpanded.newBuilder()
            .addAllLocation(blobDiff.getLocationList())
            .setBlob(Blob.newBuilder().setConfig(configBlob).build())
            .build();
      case CONTENT_NOT_SET:
      default:
        throw new ModelDBException("Unknown blob type", Status.Code.INVALID_ARGUMENT);
    }
  }
}
