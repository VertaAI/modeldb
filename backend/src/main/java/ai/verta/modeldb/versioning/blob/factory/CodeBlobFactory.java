package ai.verta.modeldb.versioning.blob.factory;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.code.GitCodeBlobEntity;
import ai.verta.modeldb.entities.code.NotebookCodeBlobEntity;
import ai.verta.modeldb.entities.versioning.InternalFolderElementEntity;
import ai.verta.modeldb.versioning.Blob;
import ai.verta.modeldb.versioning.CodeBlob;
import ai.verta.modeldb.versioning.CodeBlob.Builder;
import ai.verta.modeldb.versioning.NotebookCodeBlob;
import ai.verta.modeldb.versioning.PathDatasetBlob;
import org.hibernate.Session;

public class CodeBlobFactory extends BlobFactory {

  CodeBlobFactory(InternalFolderElementEntity internalFolderElementEntity) {
    super(
        internalFolderElementEntity.getElement_type(),
        internalFolderElementEntity.getElement_sha());
  }

  @Override
  public Blob getBlob(Session session) throws ModelDBException {
    Builder codeBlobBuilder = CodeBlob.newBuilder();
    switch (getElementType()) {
      case GIT_CODE_BLOB:
        codeBlobBuilder.setGit(session.get(GitCodeBlobEntity.class, getElementSha()).toProto());
        break;
      case NOTEBOOK_CODE_BLOB:
        NotebookCodeBlobEntity notebookCodeBlobEntity =
            session.get(NotebookCodeBlobEntity.class, getElementSha());
        String datasetBlobHash = notebookCodeBlobEntity.getPath_dataset_blob_hash();
        final NotebookCodeBlob.Builder builder = NotebookCodeBlob.newBuilder();
        PathDatasetBlob pathBlob = DatasetBlobFactory.getPathBlob(session, datasetBlobHash);
        if (pathBlob != null) {
          builder.setPath(pathBlob);
        }
        codeBlobBuilder.setNotebook(
            builder.setGitRepo(notebookCodeBlobEntity.getGitCodeBlobEntity().toProto()).build());
        break;
    }
    return Blob.newBuilder().setCode(codeBlobBuilder).build();
  }
}
