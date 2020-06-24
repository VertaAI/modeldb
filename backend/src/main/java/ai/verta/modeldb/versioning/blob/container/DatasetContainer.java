package ai.verta.modeldb.versioning.blob.container;

import static ai.verta.modeldb.versioning.blob.factory.BlobFactory.PATH_DATASET_BLOB;
import static ai.verta.modeldb.versioning.blob.factory.BlobFactory.S_3_DATASET_BLOB;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.AttributeEntity;
import ai.verta.modeldb.entities.dataset.PathDatasetComponentBlobEntity;
import ai.verta.modeldb.entities.dataset.S3DatasetComponentBlobEntity;
import ai.verta.modeldb.utils.RdbmsUtils;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.DatasetBlob;
import ai.verta.modeldb.versioning.FileHasher;
import ai.verta.modeldb.versioning.PathDatasetBlob;
import ai.verta.modeldb.versioning.S3DatasetBlob;
import ai.verta.modeldb.versioning.TreeElem;
import ai.verta.modeldb.versioning.VersioningUtils;
import ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model.AutogenPathDatasetBlob;
import ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model.AutogenPathDatasetComponentBlob;
import ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model.AutogenS3DatasetBlob;
import ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model.AutogenS3DatasetComponentBlob;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.Status.Code;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.Session;

public class DatasetContainer extends BlobContainer {

  private final DatasetBlob dataset;

  public DatasetContainer(BlobExpanded blobExpanded) {
    super(blobExpanded);
    dataset = blobExpanded.getBlob().getDataset();
  }

  @Override
  public void process(
      Session session, TreeElem rootTree, FileHasher fileHasher, Set<String> blobHashes)
      throws NoSuchAlgorithmException, ModelDBException {
    final List<String> locationList = getLocationList();
    String blobType = getBlobType();

    String blobHash;
    switch (dataset.getContentCase()) {
      case S3:
        final S3DatasetBlob s3 = dataset.getS3();

        // sorted
        Map<String, AutogenS3DatasetComponentBlob> componentHashes = new LinkedHashMap<>();
        AutogenS3DatasetBlob autogenS3DatasetBlob = AutogenS3DatasetBlob.fromProto(s3);
        if (autogenS3DatasetBlob != null && autogenS3DatasetBlob.getComponents() != null) {
          for (AutogenS3DatasetComponentBlob componentBlob : autogenS3DatasetBlob.getComponents()) {
            final String componentHash = computeSHA(componentBlob);
            componentHashes.put(componentHash, componentBlob);
          }
          blobHash = computeSHAS3Dataset(componentHashes);
          if (!blobHashes.contains(blobHash)) {
            blobHashes.add(blobHash);
            for (Map.Entry<String, AutogenS3DatasetComponentBlob> component :
                componentHashes.entrySet()) {
              if (!blobHashes.contains(component.getKey())) {
                blobHashes.add(component.getKey());
                S3DatasetComponentBlobEntity s3DatasetComponentBlobEntity =
                    new S3DatasetComponentBlobEntity(
                        component.getKey(), blobHash, component.getValue().toProto().build());
                session.saveOrUpdate(s3DatasetComponentBlobEntity);
              }
            }
          }
        } else {
          blobHash = null;
        }
        break;
      case PATH:
        blobHash = saveBlob(session, dataset.getPath(), blobHashes);
        break;
      default:
        throw new ModelDBException("Unknown blob type", Code.INTERNAL);
    }
    if (blobHash != null) {
      rootTree.push(locationList, blobHash, blobType);
    }
  }

  static String saveBlob(Session session, PathDatasetBlob path, Set<String> blobHashes)
      throws NoSuchAlgorithmException {
    // sorted
    Map<String, AutogenPathDatasetComponentBlob> componentHashes = new LinkedHashMap<>();
    AutogenPathDatasetBlob autogenPathDatasetBlob = AutogenPathDatasetBlob.fromProto(path);
    if (autogenPathDatasetBlob != null && autogenPathDatasetBlob.getComponents() != null) {
      for (AutogenPathDatasetComponentBlob componentBlob : autogenPathDatasetBlob.getComponents()) {
        final String componentHash = computeSHA(componentBlob);
        componentHashes.put(componentHash, componentBlob);
      }
      String blobHash = computeSHAPathDataset(componentHashes);
      if (!blobHashes.contains(blobHash)) {
        blobHashes.add(blobHash);
        for (Map.Entry<String, AutogenPathDatasetComponentBlob> component :
            componentHashes.entrySet()) {
          if (!blobHashes.contains(component.getKey())) {
            blobHashes.add(component.getKey());
            PathDatasetComponentBlobEntity pathDatasetComponentBlobEntity =
                new PathDatasetComponentBlobEntity(
                    component.getKey(), blobHash, component.getValue().toProto().build());
            session.saveOrUpdate(pathDatasetComponentBlobEntity);
          }
        }
      }
      return blobHash;
    }
    return null;
  }

  private String getBlobType() {
    switch (dataset.getContentCase()) {
      case PATH:
        return PATH_DATASET_BLOB;
      case S3:
      default:
        return S_3_DATASET_BLOB;
    }
  }

  public static String computeSHA(AutogenPathDatasetComponentBlob path)
      throws NoSuchAlgorithmException {
    StringBuilder sb = new StringBuilder();
    sb.append("path:")
        .append(path.getPath())
        .append(":size:")
        .append(path.getSize())
        .append(":last_modified:")
        .append(path.getLastModifiedAtSource())
        .append(":sha256:")
        .append(path.getSha256())
        .append(":md5:")
        .append(path.getMd5());
    return FileHasher.getSha(sb.toString());
  }

  public static String computeSHA(AutogenS3DatasetComponentBlob s3componentBlob)
      throws NoSuchAlgorithmException {
    StringBuilder sb = new StringBuilder();
    sb.append(":s3:")
        .append(computeSHA(s3componentBlob.getPath()))
        .append(":s3_version_id:")
        .append(s3componentBlob.getS3VersionId());
    return FileHasher.getSha(sb.toString());
  }

  private String computeSHAS3Dataset(Map<String, AutogenS3DatasetComponentBlob> componentHashes)
      throws NoSuchAlgorithmException {
    StringBuilder sb = new StringBuilder();
    sb.append("s3");
    for (Map.Entry<String, AutogenS3DatasetComponentBlob> component : componentHashes.entrySet()) {
      sb.append(":component:").append(component.getKey());
    }
    return FileHasher.getSha(sb.toString());
  }

  static String computeSHAPathDataset(Map<String, AutogenPathDatasetComponentBlob> componentHashes)
      throws NoSuchAlgorithmException {
    StringBuilder sb = new StringBuilder();
    sb.append("path");
    for (Map.Entry<String, AutogenPathDatasetComponentBlob> component :
        componentHashes.entrySet()) {
      sb.append(":component:").append(component.getKey());
    }
    return FileHasher.getSha(sb.toString());
  }

  @Override
  public void processAttribute(
      Session session, Long repoId, String commitHash, boolean addAttribute)
      throws ModelDBException {
    BlobExpanded blobExpanded = super.getBlobExpanded();
    try {
      List<AttributeEntity> newOrUpdatedAttributeEntities =
          RdbmsUtils.convertAttributesFromAttributeEntityList(
              this, ModelDBConstants.ATTRIBUTES, blobExpanded.getAttributesList());
      List<AttributeEntity> newAttributeEntities = new ArrayList<>();
      if (addAttribute) {
        newAttributeEntities.addAll(newOrUpdatedAttributeEntities);
      } else {
        List<AttributeEntity> existingAttributes =
            VersioningUtils.getAttributeEntities(
                session, repoId, commitHash, blobExpanded.getLocationList(), null);
        if (!existingAttributes.isEmpty()) {
          for (AttributeEntity updatedAttributeObj : newOrUpdatedAttributeEntities) {
            boolean doesExist = false;
            for (AttributeEntity existingAttribute : existingAttributes) {
              if (existingAttribute.getKey().equals(updatedAttributeObj.getKey())) {
                existingAttribute.setKey(updatedAttributeObj.getKey());
                existingAttribute.setValue(updatedAttributeObj.getValue());
                existingAttribute.setValue_type(updatedAttributeObj.getValue_type());
                doesExist = true;
                session.saveOrUpdate(existingAttribute);
                break;
              }
            }
            if (!doesExist) {
              newAttributeEntities.add(updatedAttributeObj);
            }
          }
        } else {
          newAttributeEntities.addAll(newOrUpdatedAttributeEntities);
        }
      }
      newAttributeEntities.forEach(
          attributeEntity -> {
            attributeEntity.setEntity_hash(
                VersioningUtils.getVersioningCompositeId(
                    repoId, commitHash, blobExpanded.getLocationList()));
            session.saveOrUpdate(attributeEntity);
          });
    } catch (InvalidProtocolBufferException e) {
      throw new ModelDBException(e);
    }
  }
}
