package ai.verta.modeldb.versioning.blob.diffFactory;

import ai.verta.modeldb.versioning.BlobDiff;
import ai.verta.modeldb.versioning.BlobExpanded;
import ai.verta.modeldb.versioning.CodeBlob;
import ai.verta.modeldb.versioning.CodeDiff;
import ai.verta.modeldb.versioning.GitCodeDiff;
import ai.verta.modeldb.versioning.NotebookCodeDiff;

public class CodeBlobDiffFactory extends BlobDiffFactory {

  public CodeBlobDiffFactory(BlobExpanded blobExpanded) {
    super(blobExpanded);
  }

  @Override
  protected boolean subtypeEqual(BlobDiffFactory blobDiffFactory) {
    return blobDiffFactory
        .getBlobExpanded()
        .getBlob()
        .getCode()
        .getContentCase()
        .equals(getBlobExpanded().getBlob().getCode().getContentCase());
  }

  @Override
  protected void add(BlobDiff.Builder blobDiffBuilder) {
    modify(blobDiffBuilder, true);
  }

  @Override
  protected void delete(BlobDiff.Builder blobDiffBuilder) {
    modify(blobDiffBuilder, false);
  }

  private void modify(BlobDiff.Builder blobDiffBuilder, boolean add) {
    final CodeDiff.Builder codeBuilder = CodeDiff.newBuilder();
    final CodeBlob code = getBlobExpanded().getBlob().getCode();
    switch (code.getContentCase()) {
      case GIT:
        GitCodeDiff.Builder gitBuilder;
        if (blobDiffBuilder.hasCode()) {
          gitBuilder = blobDiffBuilder.getCode().getGit().toBuilder();
        } else {
          gitBuilder = GitCodeDiff.newBuilder();
        }
        if (add) {
          gitBuilder.setB(code.getGit());
        } else {
          gitBuilder.setA(code.getGit());
        }

        codeBuilder.setGit(gitBuilder).build();
        break;
      case NOTEBOOK:
        NotebookCodeDiff.Builder notebookBuilder;
        if (blobDiffBuilder.hasCode()) {
          notebookBuilder = blobDiffBuilder.getCode().getNotebook().toBuilder();
        } else {
          notebookBuilder = NotebookCodeDiff.newBuilder();
        }
        if (add) {
          notebookBuilder.setB(code.getNotebook());
        } else {
          notebookBuilder.setA(code.getNotebook());
        }

        codeBuilder.setNotebook(notebookBuilder).build();
        break;
    }
    blobDiffBuilder.setCode(codeBuilder.build());
  }
}
