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

public class AutogenCodeBlob implements ProtoType {
  private AutogenGitCodeBlob Git;
  private AutogenNotebookCodeBlob Notebook;

  public AutogenCodeBlob() {
    this.Git = null;
    this.Notebook = null;
  }

  public Boolean isEmpty() {
    if (this.Git != null && !this.Git.equals(null)) {
      return false;
    }
    if (this.Notebook != null && !this.Notebook.equals(null)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"class\": \"AutogenCodeBlob\", \"fields\": {");
    boolean first = true;
    if (this.Git != null && !this.Git.equals(null)) {
      if (!first) sb.append(", ");
      sb.append("\"Git\": " + Git);
      first = false;
    }
    if (this.Notebook != null && !this.Notebook.equals(null)) {
      if (!first) sb.append(", ");
      sb.append("\"Notebook\": " + Notebook);
      first = false;
    }
    sb.append("}}");
    return sb.toString();
  }

  // TODO: actually hash
  public String getSHA() {
    StringBuilder sb = new StringBuilder();
    sb.append("AutogenCodeBlob");
    if (this.Git != null && !this.Git.equals(null)) {
      sb.append("::Git::").append(Git);
    }
    if (this.Notebook != null && !this.Notebook.equals(null)) {
      sb.append("::Notebook::").append(Notebook);
    }

    return sb.toString();
  }

  // TODO: not consider order on lists
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (!(o instanceof AutogenCodeBlob)) return false;
    AutogenCodeBlob other = (AutogenCodeBlob) o;

    {
      Function3<AutogenGitCodeBlob, AutogenGitCodeBlob, Boolean> f = (x, y) -> x.equals(y);
      if (this.Git != null || other.Git != null) {
        if (this.Git == null && other.Git != null) return false;
        if (this.Git != null && other.Git == null) return false;
        if (!f.apply(this.Git, other.Git)) return false;
      }
    }
    {
      Function3<AutogenNotebookCodeBlob, AutogenNotebookCodeBlob, Boolean> f =
          (x, y) -> x.equals(y);
      if (this.Notebook != null || other.Notebook != null) {
        if (this.Notebook == null && other.Notebook != null) return false;
        if (this.Notebook != null && other.Notebook == null) return false;
        if (!f.apply(this.Notebook, other.Notebook)) return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.Git, this.Notebook);
  }

  public AutogenCodeBlob setGit(AutogenGitCodeBlob value) {
    this.Git = Utils.removeEmpty(value);
    return this;
  }

  public AutogenGitCodeBlob getGit() {
    return this.Git;
  }

  public AutogenCodeBlob setNotebook(AutogenNotebookCodeBlob value) {
    this.Notebook = Utils.removeEmpty(value);
    return this;
  }

  public AutogenNotebookCodeBlob getNotebook() {
    return this.Notebook;
  }

  public static AutogenCodeBlob fromProto(ai.verta.modeldb.versioning.CodeBlob blob) {
    if (blob == null) {
      return null;
    }

    AutogenCodeBlob obj = new AutogenCodeBlob();
    {
      Function<ai.verta.modeldb.versioning.CodeBlob, AutogenGitCodeBlob> f =
          x -> AutogenGitCodeBlob.fromProto(blob.getGit());
      obj.setGit(f.apply(blob));
    }
    {
      Function<ai.verta.modeldb.versioning.CodeBlob, AutogenNotebookCodeBlob> f =
          x -> AutogenNotebookCodeBlob.fromProto(blob.getNotebook());
      obj.setNotebook(f.apply(blob));
    }
    return obj;
  }

  public ai.verta.modeldb.versioning.CodeBlob.Builder toProto() {
    ai.verta.modeldb.versioning.CodeBlob.Builder builder =
        ai.verta.modeldb.versioning.CodeBlob.newBuilder();
    {
      if (this.Git != null && !this.Git.equals(null)) {
        Function<ai.verta.modeldb.versioning.CodeBlob.Builder, Void> f =
            x -> {
              builder.setGit(this.Git.toProto());
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Notebook != null && !this.Notebook.equals(null)) {
        Function<ai.verta.modeldb.versioning.CodeBlob.Builder, Void> f =
            x -> {
              builder.setNotebook(this.Notebook.toProto());
              return null;
            };
        f.apply(builder);
      }
    }
    return builder;
  }

  public void preVisitShallow(Visitor visitor) throws ModelDBException {
    visitor.preVisitAutogenCodeBlob(this);
  }

  public void preVisitDeep(Visitor visitor) throws ModelDBException {
    this.preVisitShallow(visitor);
    visitor.preVisitDeepAutogenGitCodeBlob(this.Git);
    visitor.preVisitDeepAutogenNotebookCodeBlob(this.Notebook);
  }

  public AutogenCodeBlob postVisitShallow(Visitor visitor) throws ModelDBException {
    return visitor.postVisitAutogenCodeBlob(this);
  }

  public AutogenCodeBlob postVisitDeep(Visitor visitor) throws ModelDBException {
    this.setGit(visitor.postVisitDeepAutogenGitCodeBlob(this.Git));
    this.setNotebook(visitor.postVisitDeepAutogenNotebookCodeBlob(this.Notebook));
    return this.postVisitShallow(visitor);
  }
}
