package ai.verta.modeldb.versioning.blob.factory;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.entities.code.GitCodeBlobEntity;
import ai.verta.modeldb.entities.code.NotebookCodeBlobEntity;
import ai.verta.modeldb.entities.versioning.InternalFolderElementEntity;
import ai.verta.modeldb.versioning.Blob;
import ai.verta.modeldb.versioning.CodeBlob;
import ai.verta.modeldb.versioning.NotebookCodeBlob;
import ai.verta.modeldb.versioning.PathDatasetBlob;
import io.grpc.Status.Code;
import org.hibernate.Session;

public class CodeBlobFactory extends BlobFactory {

  CodeBlobFactory(InternalFolderElementEntity internalFolderElementEntity) {
    super(
        internalFolderElementEntity.getElement_type(),
        internalFolderElementEntity.getElement_sha());
  }

  @Override
  public Blob getBlob(Session session) throws ModelDBException {
    var codeBlobBuilder = CodeBlob.newBuilder();
    switch (getElementType()) {
      case GIT_CODE_BLOB:
        codeBlobBuilder.setGit(session.get(GitCodeBlobEntity.class, getElementSha()).toProto());
        break;
      case NOTEBOOK_CODE_BLOB:
        var notebookCodeBlobEntity = session.get(NotebookCodeBlobEntity.class, getElementSha());
        String datasetBlobHash = notebookCodeBlobEntity.getPath_dataset_blob_hash();
        final var builder = NotebookCodeBlob.newBuilder();
        PathDatasetBlob pathBlob = DatasetBlobFactory.getPathBlob(session, datasetBlobHash);
        if (pathBlob != null) {
          if (pathBlob.getComponentsCount() == 1) {
            builder.setPath(pathBlob.getComponents(0));
          } else {
            throw new ModelDBException("Path should have only one component", Code.INTERNAL);
          }
        }
        codeBlobBuilder.setNotebook(
            builder.setGitRepo(notebookCodeBlobEntity.getGitCodeBlobEntity().toProto()).build());
        break;
      default:
        // Do nothing
        break;
    }
    return Blob.newBuilder().setCode(codeBlobBuilder).build();
  }
}
