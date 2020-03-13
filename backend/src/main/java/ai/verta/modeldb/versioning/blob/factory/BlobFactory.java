package ai.verta.modeldb.versioning.blob.factory;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.versioning.InternalFolderElementEntity;
import ai.verta.modeldb.versioning.Blob;
import io.grpc.Status.Code;
import org.hibernate.Session;

/** constructs proto object from it's database implementation */
public abstract class BlobFactory {

  public static final String S_3_DATASET_BLOB = "S3DatasetBlob";
  public static final String PATH_DATASET_BLOB = "PathDatasetBlob";
  public static final String PYTHON_ENVIRONMENT_BLOB = "PythonEnvironmentBlob";
  public static final String DOCKER_ENVIRONMENT_BLOB = "DockerEnvironmentBlob";
  public static final String GIT_CODE_BLOB = "GitCodeBlob";
  public static final String NOTEBOOK_CODE_BLOB = "NotebookCodeBlob";
  public static final String CONFIG_BLOB = "ConfigBlob";
  private final String elementType;
  private final String elementSha;

  BlobFactory(String elementType, String elementSha) {
    this.elementType = elementType;
    this.elementSha = elementSha;
  }

  public static BlobFactory create(InternalFolderElementEntity folderElementEntity)
      throws ModelDBException {
    switch (folderElementEntity.getElement_type()) {
      case S_3_DATASET_BLOB:
      case PATH_DATASET_BLOB:
        return new DatasetBlobFactory(folderElementEntity);
      case PYTHON_ENVIRONMENT_BLOB:
      case DOCKER_ENVIRONMENT_BLOB:
        return new EnvironmentBlobFactory(folderElementEntity);
      case GIT_CODE_BLOB:
      case NOTEBOOK_CODE_BLOB:
        return new CodeBlobFactory(folderElementEntity);
      case CONFIG_BLOB:
        return new ConfigBlobFactory(folderElementEntity);
      default:
        throw new ModelDBException("Unknown blob type found " + folderElementEntity, Code.INTERNAL);
    }
  }

  public abstract Blob getBlob(Session session) throws ModelDBException;

  String getElementType() {
    return elementType;
  }

  String getElementSha() {
    return elementSha;
  }
}
