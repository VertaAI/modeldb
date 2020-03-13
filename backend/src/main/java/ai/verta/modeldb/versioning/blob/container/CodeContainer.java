package ai.verta.modeldb.versioning.blob.container;

import static ai.verta.modeldb.versioning.blob.factory.BlobFactory.GIT_CODE_BLOB;
import static ai.verta.modeldb.versioning.blob.factory.BlobFactory.NOTEBOOK_CODE_BLOB;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.entities.code.GitCodeBlobEntity;
import ai.verta.modeldb.entities.code.NotebookCodeBlobEntity;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.CodeBlob;
import ai.verta.modeldb.versioning.FileHasher;
import ai.verta.modeldb.versioning.GitCodeBlob;
import ai.verta.modeldb.versioning.NotebookCodeBlob;
import ai.verta.modeldb.versioning.TreeElem;
import io.grpc.Status.Code;
import java.security.NoSuchAlgorithmException;
import org.hibernate.Session;

public class CodeContainer extends BlobContainer {

  private final CodeBlob code;

  public CodeContainer(BlobExpanded blobExpanded) {
    super(blobExpanded);
    code = blobExpanded.getBlob().getCode();
  }

  @Override
  public void validate() throws ModelDBException {
    switch (code.getContentCase()) {
      case GIT:
        validate(code.getGit());
        break;
      case NOTEBOOK:
        final NotebookCodeBlob notebook = code.getNotebook();
        validate(notebook.getGitRepo());
        validate(notebook.getPath());
        break;
      default:
        throw new ModelDBException("Blob unknown type", Code.INVALID_ARGUMENT);
    }
  }

  private void validate(GitCodeBlob gitRepo) throws ModelDBException {
    if (gitRepo.getRepo().isEmpty()) {
      throw new ModelDBException("Code repository path should not be empty", Code.INVALID_ARGUMENT);
    }
  }

  @Override
  public void process(Session session, TreeElem rootTree, FileHasher fileHasher)
      throws NoSuchAlgorithmException, ModelDBException {
    String blobType;
    final String blobHash;
    switch (code.getContentCase()) {
      case GIT:
        blobType = GIT_CODE_BLOB;
        GitCodeBlob gitCodeBlob = code.getGit();
        blobHash = saveBlob(session, gitCodeBlob).getBlobHash();
        break;
      case NOTEBOOK:
        blobType = NOTEBOOK_CODE_BLOB;
        NotebookCodeBlob notebook = code.getNotebook();
        GitCodeBlobEntity gitCodeBlobEntity = saveBlob(session, notebook.getGitRepo());
        String pathBlobSha = DatasetContainer.saveBlob(session, notebook.getPath());
        blobHash = FileHasher.getSha(gitCodeBlobEntity.getBlobHash() + ":" + pathBlobSha);
        session.saveOrUpdate(new NotebookCodeBlobEntity(blobHash, gitCodeBlobEntity, pathBlobSha));
        break;
      default:
        throw new ModelDBException("Blob unknown type", Code.INTERNAL);
    }
    rootTree.push(getLocationList(), blobHash, blobType);
  }

  private GitCodeBlobEntity saveBlob(Session session, GitCodeBlob gitCodeBlob)
      throws NoSuchAlgorithmException {
    GitCodeBlobEntity gitCodeBlobEntity =
        new GitCodeBlobEntity(computeSHA(gitCodeBlob), gitCodeBlob);
    session.saveOrUpdate(gitCodeBlobEntity);
    return gitCodeBlobEntity;
  }

  private String computeSHA(GitCodeBlob gitCodeBlob) throws NoSuchAlgorithmException {
    StringBuilder sb = new StringBuilder();
    sb.append("git:repo:")
        .append(gitCodeBlob.getRepo())
        .append(":tag:")
        .append(gitCodeBlob.getTag())
        .append(":branch:")
        .append(gitCodeBlob.getBranch())
        .append(":hash:")
        .append(gitCodeBlob.getHash())
        .append(":is_dirty:")
        .append(gitCodeBlob.getIsDirty());

    return FileHasher.getSha(sb.toString());
  }
}
