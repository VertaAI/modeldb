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

public class AutogenVersionEnvironmentBlob implements ProtoType {
  private Integer Major;
  private Integer Minor;
  private Integer Patch;
  private String Suffix;

  public AutogenVersionEnvironmentBlob() {
    this.Major = 0;
    this.Minor = 0;
    this.Patch = 0;
    this.Suffix = "";
  }

  public Boolean isEmpty() {
    if (this.Major != null && !this.Major.equals(0)) {
      return false;
    }
    if (this.Minor != null && !this.Minor.equals(0)) {
      return false;
    }
    if (this.Patch != null && !this.Patch.equals(0)) {
      return false;
    }
    if (this.Suffix != null && !this.Suffix.equals("")) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"class\": \"AutogenVersionEnvironmentBlob\", \"fields\": {");
    boolean first = true;
    if (this.Major != null && !this.Major.equals(0)) {
      if (!first) sb.append(", ");
      sb.append("\"Major\": " + Major);
      first = false;
    }
    if (this.Minor != null && !this.Minor.equals(0)) {
      if (!first) sb.append(", ");
      sb.append("\"Minor\": " + Minor);
      first = false;
    }
    if (this.Patch != null && !this.Patch.equals(0)) {
      if (!first) sb.append(", ");
      sb.append("\"Patch\": " + Patch);
      first = false;
    }
    if (this.Suffix != null && !this.Suffix.equals("")) {
      if (!first) sb.append(", ");
      sb.append("\"Suffix\": " + "\"" + Suffix + "\"");
      first = false;
    }
    sb.append("}}");
    return sb.toString();
  }

  // TODO: actually hash
  public String getSHA() {
    StringBuilder sb = new StringBuilder();
    sb.append("AutogenVersionEnvironmentBlob");
    if (this.Major != null && !this.Major.equals(0)) {
      sb.append("::Major::").append(Major);
    }
    if (this.Minor != null && !this.Minor.equals(0)) {
      sb.append("::Minor::").append(Minor);
    }
    if (this.Patch != null && !this.Patch.equals(0)) {
      sb.append("::Patch::").append(Patch);
    }
    if (this.Suffix != null && !this.Suffix.equals("")) {
      sb.append("::Suffix::").append(Suffix);
    }

    return sb.toString();
  }

  // TODO: not consider order on lists
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (!(o instanceof AutogenVersionEnvironmentBlob)) return false;
    AutogenVersionEnvironmentBlob other = (AutogenVersionEnvironmentBlob) o;

    {
      Function3<Integer, Integer, Boolean> f = (x, y) -> x.equals(y);
      if (this.Major != null || other.Major != null) {
        if (this.Major == null && other.Major != null) return false;
        if (this.Major != null && other.Major == null) return false;
        if (!f.apply(this.Major, other.Major)) return false;
      }
    }
    {
      Function3<Integer, Integer, Boolean> f = (x, y) -> x.equals(y);
      if (this.Minor != null || other.Minor != null) {
        if (this.Minor == null && other.Minor != null) return false;
        if (this.Minor != null && other.Minor == null) return false;
        if (!f.apply(this.Minor, other.Minor)) return false;
      }
    }
    {
      Function3<Integer, Integer, Boolean> f = (x, y) -> x.equals(y);
      if (this.Patch != null || other.Patch != null) {
        if (this.Patch == null && other.Patch != null) return false;
        if (this.Patch != null && other.Patch == null) return false;
        if (!f.apply(this.Patch, other.Patch)) return false;
      }
    }
    {
      Function3<String, String, Boolean> f = (x, y) -> x.equals(y);
      if (this.Suffix != null || other.Suffix != null) {
        if (this.Suffix == null && other.Suffix != null) return false;
        if (this.Suffix != null && other.Suffix == null) return false;
        if (!f.apply(this.Suffix, other.Suffix)) return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.Major, this.Minor, this.Patch, this.Suffix);
  }

  public AutogenVersionEnvironmentBlob setMajor(Integer value) {
    this.Major = Utils.removeEmpty(value);
    return this;
  }

  public Integer getMajor() {
    return this.Major;
  }

  public AutogenVersionEnvironmentBlob setMinor(Integer value) {
    this.Minor = Utils.removeEmpty(value);
    return this;
  }

  public Integer getMinor() {
    return this.Minor;
  }

  public AutogenVersionEnvironmentBlob setPatch(Integer value) {
    this.Patch = Utils.removeEmpty(value);
    return this;
  }

  public Integer getPatch() {
    return this.Patch;
  }

  public AutogenVersionEnvironmentBlob setSuffix(String value) {
    this.Suffix = Utils.removeEmpty(value);
    return this;
  }

  public String getSuffix() {
    return this.Suffix;
  }

  public static AutogenVersionEnvironmentBlob fromProto(
      ai.verta.modeldb.versioning.VersionEnvironmentBlob blob) {
    if (blob == null) {
      return null;
    }

    AutogenVersionEnvironmentBlob obj = new AutogenVersionEnvironmentBlob();
    {
      Function<ai.verta.modeldb.versioning.VersionEnvironmentBlob, Integer> f =
          x -> (blob.getMajor());
      obj.setMajor(f.apply(blob));
    }
    {
      Function<ai.verta.modeldb.versioning.VersionEnvironmentBlob, Integer> f =
          x -> (blob.getMinor());
      obj.setMinor(f.apply(blob));
    }
    {
      Function<ai.verta.modeldb.versioning.VersionEnvironmentBlob, Integer> f =
          x -> (blob.getPatch());
      obj.setPatch(f.apply(blob));
    }
    {
      Function<ai.verta.modeldb.versioning.VersionEnvironmentBlob, String> f =
          x -> (blob.getSuffix());
      obj.setSuffix(f.apply(blob));
    }
    return obj;
  }

  public ai.verta.modeldb.versioning.VersionEnvironmentBlob.Builder toProto() {
    ai.verta.modeldb.versioning.VersionEnvironmentBlob.Builder builder =
        ai.verta.modeldb.versioning.VersionEnvironmentBlob.newBuilder();
    {
      if (this.Major != null && !this.Major.equals(0)) {
        Function<ai.verta.modeldb.versioning.VersionEnvironmentBlob.Builder, Void> f =
            x -> {
              builder.setMajor(this.Major);
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Minor != null && !this.Minor.equals(0)) {
        Function<ai.verta.modeldb.versioning.VersionEnvironmentBlob.Builder, Void> f =
            x -> {
              builder.setMinor(this.Minor);
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Patch != null && !this.Patch.equals(0)) {
        Function<ai.verta.modeldb.versioning.VersionEnvironmentBlob.Builder, Void> f =
            x -> {
              builder.setPatch(this.Patch);
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Suffix != null && !this.Suffix.equals("")) {
        Function<ai.verta.modeldb.versioning.VersionEnvironmentBlob.Builder, Void> f =
            x -> {
              builder.setSuffix(this.Suffix);
              return null;
            };
        f.apply(builder);
      }
    }
    return builder;
  }

  public void preVisitShallow(Visitor visitor) throws ModelDBException {
    visitor.preVisitAutogenVersionEnvironmentBlob(this);
  }

  public void preVisitDeep(Visitor visitor) throws ModelDBException {
    this.preVisitShallow(visitor);
    visitor.preVisitDeepInteger(this.Major);
    visitor.preVisitDeepInteger(this.Minor);
    visitor.preVisitDeepInteger(this.Patch);
    visitor.preVisitDeepString(this.Suffix);
  }

  public AutogenVersionEnvironmentBlob postVisitShallow(Visitor visitor) throws ModelDBException {
    return visitor.postVisitAutogenVersionEnvironmentBlob(this);
  }

  public AutogenVersionEnvironmentBlob postVisitDeep(Visitor visitor) throws ModelDBException {
    this.setMajor(visitor.postVisitDeepInteger(this.Major));
    this.setMinor(visitor.postVisitDeepInteger(this.Minor));
    this.setPatch(visitor.postVisitDeepInteger(this.Patch));
    this.setSuffix(visitor.postVisitDeepString(this.Suffix));
    return this.postVisitShallow(visitor);
  }
}
