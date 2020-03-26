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

public class AutogenDockerEnvironmentBlob implements ProtoType {
  private String Repository;
  private String Sha;
  private String Tag;

  public AutogenDockerEnvironmentBlob() {
    this.Repository = "";
    this.Sha = "";
    this.Tag = "";
  }

  public Boolean isEmpty() {
    if (this.Repository != null && !this.Repository.equals("")) {
      return false;
    }
    if (this.Sha != null && !this.Sha.equals("")) {
      return false;
    }
    if (this.Tag != null && !this.Tag.equals("")) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"class\": \"AutogenDockerEnvironmentBlob\", \"fields\": {");
    boolean first = true;
    if (this.Repository != null && !this.Repository.equals("")) {
      if (!first) sb.append(", ");
      sb.append("\"Repository\": " + "\"" + Repository + "\"");
      first = false;
    }
    if (this.Sha != null && !this.Sha.equals("")) {
      if (!first) sb.append(", ");
      sb.append("\"Sha\": " + "\"" + Sha + "\"");
      first = false;
    }
    if (this.Tag != null && !this.Tag.equals("")) {
      if (!first) sb.append(", ");
      sb.append("\"Tag\": " + "\"" + Tag + "\"");
      first = false;
    }
    sb.append("}}");
    return sb.toString();
  }

  // TODO: actually hash
  public String getSHA() {
    StringBuilder sb = new StringBuilder();
    sb.append("AutogenDockerEnvironmentBlob");
    if (this.Repository != null && !this.Repository.equals("")) {
      sb.append("::Repository::").append(Repository);
    }
    if (this.Sha != null && !this.Sha.equals("")) {
      sb.append("::Sha::").append(Sha);
    }
    if (this.Tag != null && !this.Tag.equals("")) {
      sb.append("::Tag::").append(Tag);
    }

    return sb.toString();
  }

  // TODO: not consider order on lists
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (!(o instanceof AutogenDockerEnvironmentBlob)) return false;
    AutogenDockerEnvironmentBlob other = (AutogenDockerEnvironmentBlob) o;

    {
      Function3<String, String, Boolean> f = (x, y) -> x.equals(y);
      if (this.Repository != null || other.Repository != null) {
        if (this.Repository == null && other.Repository != null) return false;
        if (this.Repository != null && other.Repository == null) return false;
        if (!f.apply(this.Repository, other.Repository)) return false;
      }
    }
    {
      Function3<String, String, Boolean> f = (x, y) -> x.equals(y);
      if (this.Sha != null || other.Sha != null) {
        if (this.Sha == null && other.Sha != null) return false;
        if (this.Sha != null && other.Sha == null) return false;
        if (!f.apply(this.Sha, other.Sha)) return false;
      }
    }
    {
      Function3<String, String, Boolean> f = (x, y) -> x.equals(y);
      if (this.Tag != null || other.Tag != null) {
        if (this.Tag == null && other.Tag != null) return false;
        if (this.Tag != null && other.Tag == null) return false;
        if (!f.apply(this.Tag, other.Tag)) return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.Repository, this.Sha, this.Tag);
  }

  public AutogenDockerEnvironmentBlob setRepository(String value) {
    this.Repository = Utils.removeEmpty(value);
    return this;
  }

  public String getRepository() {
    return this.Repository;
  }

  public AutogenDockerEnvironmentBlob setSha(String value) {
    this.Sha = Utils.removeEmpty(value);
    return this;
  }

  public String getSha() {
    return this.Sha;
  }

  public AutogenDockerEnvironmentBlob setTag(String value) {
    this.Tag = Utils.removeEmpty(value);
    return this;
  }

  public String getTag() {
    return this.Tag;
  }

  public static AutogenDockerEnvironmentBlob fromProto(
      ai.verta.modeldb.versioning.DockerEnvironmentBlob blob) {
    if (blob == null) {
      return null;
    }

    AutogenDockerEnvironmentBlob obj = new AutogenDockerEnvironmentBlob();
    {
      Function<ai.verta.modeldb.versioning.DockerEnvironmentBlob, String> f =
          x -> (blob.getRepository());
      obj.setRepository(f.apply(blob));
    }
    {
      Function<ai.verta.modeldb.versioning.DockerEnvironmentBlob, String> f = x -> (blob.getSha());
      obj.setSha(f.apply(blob));
    }
    {
      Function<ai.verta.modeldb.versioning.DockerEnvironmentBlob, String> f = x -> (blob.getTag());
      obj.setTag(f.apply(blob));
    }
    return obj;
  }

  public ai.verta.modeldb.versioning.DockerEnvironmentBlob.Builder toProto() {
    ai.verta.modeldb.versioning.DockerEnvironmentBlob.Builder builder =
        ai.verta.modeldb.versioning.DockerEnvironmentBlob.newBuilder();
    {
      if (this.Repository != null && !this.Repository.equals("")) {
        Function<ai.verta.modeldb.versioning.DockerEnvironmentBlob.Builder, Void> f =
            x -> {
              builder.setRepository(this.Repository);
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Sha != null && !this.Sha.equals("")) {
        Function<ai.verta.modeldb.versioning.DockerEnvironmentBlob.Builder, Void> f =
            x -> {
              builder.setSha(this.Sha);
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Tag != null && !this.Tag.equals("")) {
        Function<ai.verta.modeldb.versioning.DockerEnvironmentBlob.Builder, Void> f =
            x -> {
              builder.setTag(this.Tag);
              return null;
            };
        f.apply(builder);
      }
    }
    return builder;
  }

  public void preVisitShallow(Visitor visitor) throws ModelDBException {
    visitor.preVisitAutogenDockerEnvironmentBlob(this);
  }

  public void preVisitDeep(Visitor visitor) throws ModelDBException {
    this.preVisitShallow(visitor);
    visitor.preVisitDeepString(this.Repository);
    visitor.preVisitDeepString(this.Sha);
    visitor.preVisitDeepString(this.Tag);
  }

  public AutogenDockerEnvironmentBlob postVisitShallow(Visitor visitor) throws ModelDBException {
    return visitor.postVisitAutogenDockerEnvironmentBlob(this);
  }

  public AutogenDockerEnvironmentBlob postVisitDeep(Visitor visitor) throws ModelDBException {
    this.setRepository(visitor.postVisitDeepString(this.Repository));
    this.setSha(visitor.postVisitDeepString(this.Sha));
    this.setTag(visitor.postVisitDeepString(this.Tag));
    return this.postVisitShallow(visitor);
  }
}
