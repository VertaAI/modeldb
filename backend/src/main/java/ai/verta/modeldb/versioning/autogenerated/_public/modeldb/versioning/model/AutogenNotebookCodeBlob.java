// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.versioning.*;
import ai.verta.modeldb.versioning.blob.diff.Function3;
import ai.verta.modeldb.versioning.blob.diff.*;
import ai.verta.modeldb.versioning.blob.visitors.Visitor;
import com.pholser.junit.quickcheck.generator.*;
import com.pholser.junit.quickcheck.random.*;

public class AutogenNotebookCodeBlob implements ProtoType {
  private AutogenGitCodeBlob GitRepo;
  private AutogenPathDatasetComponentBlob Path;

  public AutogenNotebookCodeBlob() {
    this.GitRepo = null;
    this.Path = null;
  }

  public Boolean isEmpty() {
    if (this.GitRepo != null && !this.GitRepo.equals(null)) {
      return false;
    }
    if (this.Path != null && !this.Path.equals(null)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"class\": \"AutogenNotebookCodeBlob\", \"fields\": {");
    boolean first = true;
    if (this.GitRepo != null && !this.GitRepo.equals(null)) {
      if (!first) sb.append(", ");
      sb.append("\"GitRepo\": " + GitRepo);
      first = false;
    }
    if (this.Path != null && !this.Path.equals(null)) {
      if (!first) sb.append(", ");
      sb.append("\"Path\": " + Path);
      first = false;
    }
    sb.append("}}");
    return sb.toString();
  }

  // TODO: actually hash
  public String getSHA() {
    StringBuilder sb = new StringBuilder();
    sb.append("AutogenNotebookCodeBlob");
    if (this.GitRepo != null && !this.GitRepo.equals(null)) {
      sb.append("::GitRepo::").append(GitRepo);
    }
    if (this.Path != null && !this.Path.equals(null)) {
      sb.append("::Path::").append(Path);
    }

    return sb.toString();
  }

  // TODO: not consider order on lists
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (!(o instanceof AutogenNotebookCodeBlob)) return false;
    AutogenNotebookCodeBlob other = (AutogenNotebookCodeBlob) o;

    {
      Function3<AutogenGitCodeBlob, AutogenGitCodeBlob, Boolean> f = (x, y) -> x.equals(y);
      if (this.GitRepo != null || other.GitRepo != null) {
        if (this.GitRepo == null && other.GitRepo != null) return false;
        if (this.GitRepo != null && other.GitRepo == null) return false;
        if (!f.apply(this.GitRepo, other.GitRepo)) return false;
      }
    }
    {
      Function3<AutogenPathDatasetComponentBlob, AutogenPathDatasetComponentBlob, Boolean> f =
          (x, y) -> x.equals(y);
      if (this.Path != null || other.Path != null) {
        if (this.Path == null && other.Path != null) return false;
        if (this.Path != null && other.Path == null) return false;
        if (!f.apply(this.Path, other.Path)) return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.GitRepo, this.Path);
  }

  public AutogenNotebookCodeBlob setGitRepo(AutogenGitCodeBlob value) {
    this.GitRepo = Utils.removeEmpty(value);
    return this;
  }

  public AutogenGitCodeBlob getGitRepo() {
    return this.GitRepo;
  }

  public AutogenNotebookCodeBlob setPath(AutogenPathDatasetComponentBlob value) {
    this.Path = Utils.removeEmpty(value);
    return this;
  }

  public AutogenPathDatasetComponentBlob getPath() {
    return this.Path;
  }

  public static AutogenNotebookCodeBlob fromProto(
      ai.verta.modeldb.versioning.NotebookCodeBlob blob) {
    if (blob == null) {
      return null;
    }

    AutogenNotebookCodeBlob obj = new AutogenNotebookCodeBlob();
    {
      Function<ai.verta.modeldb.versioning.NotebookCodeBlob, AutogenGitCodeBlob> f =
          x -> AutogenGitCodeBlob.fromProto(blob.getGitRepo());
      obj.setGitRepo(f.apply(blob));
    }
    {
      Function<ai.verta.modeldb.versioning.NotebookCodeBlob, AutogenPathDatasetComponentBlob> f =
          x -> AutogenPathDatasetComponentBlob.fromProto(blob.getPath());
      obj.setPath(f.apply(blob));
    }
    return obj;
  }

  public ai.verta.modeldb.versioning.NotebookCodeBlob.Builder toProto() {
    ai.verta.modeldb.versioning.NotebookCodeBlob.Builder builder =
        ai.verta.modeldb.versioning.NotebookCodeBlob.newBuilder();
    {
      if (this.GitRepo != null && !this.GitRepo.equals(null)) {
        Function<ai.verta.modeldb.versioning.NotebookCodeBlob.Builder, Void> f =
            x -> {
              builder.setGitRepo(this.GitRepo.toProto());
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Path != null && !this.Path.equals(null)) {
        Function<ai.verta.modeldb.versioning.NotebookCodeBlob.Builder, Void> f =
            x -> {
              builder.setPath(this.Path.toProto());
              return null;
            };
        f.apply(builder);
      }
    }
    return builder;
  }

  public void preVisitShallow(Visitor visitor) throws ModelDBException {
    visitor.preVisitAutogenNotebookCodeBlob(this);
  }

  public void preVisitDeep(Visitor visitor) throws ModelDBException {
    this.preVisitShallow(visitor);
    visitor.preVisitDeepAutogenGitCodeBlob(this.GitRepo);
    visitor.preVisitDeepAutogenPathDatasetComponentBlob(this.Path);
  }

  public AutogenNotebookCodeBlob postVisitShallow(Visitor visitor) throws ModelDBException {
    return visitor.postVisitAutogenNotebookCodeBlob(this);
  }

  public AutogenNotebookCodeBlob postVisitDeep(Visitor visitor) throws ModelDBException {
    this.setGitRepo(visitor.postVisitDeepAutogenGitCodeBlob(this.GitRepo));
    this.setPath(visitor.postVisitDeepAutogenPathDatasetComponentBlob(this.Path));
    return this.postVisitShallow(visitor);
  }
}
