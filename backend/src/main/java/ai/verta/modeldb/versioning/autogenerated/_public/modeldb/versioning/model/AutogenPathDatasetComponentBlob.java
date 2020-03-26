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

public class AutogenPathDatasetComponentBlob implements ProtoType {
  private Long LastModifiedAtSource;
  private String Md5;
  private String Path;
  private String Sha256;
  private Long Size;

  public AutogenPathDatasetComponentBlob() {
    this.LastModifiedAtSource = 0l;
    this.Md5 = "";
    this.Path = "";
    this.Sha256 = "";
    this.Size = 0l;
  }

  public Boolean isEmpty() {
    if (this.LastModifiedAtSource != null && !this.LastModifiedAtSource.equals(0l)) {
      return false;
    }
    if (this.Md5 != null && !this.Md5.equals("")) {
      return false;
    }
    if (this.Path != null && !this.Path.equals("")) {
      return false;
    }
    if (this.Sha256 != null && !this.Sha256.equals("")) {
      return false;
    }
    if (this.Size != null && !this.Size.equals(0l)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"class\": \"AutogenPathDatasetComponentBlob\", \"fields\": {");
    boolean first = true;
    if (this.LastModifiedAtSource != null && !this.LastModifiedAtSource.equals(0l)) {
      if (!first) sb.append(", ");
      sb.append("\"LastModifiedAtSource\": " + LastModifiedAtSource);
      first = false;
    }
    if (this.Md5 != null && !this.Md5.equals("")) {
      if (!first) sb.append(", ");
      sb.append("\"Md5\": " + "\"" + Md5 + "\"");
      first = false;
    }
    if (this.Path != null && !this.Path.equals("")) {
      if (!first) sb.append(", ");
      sb.append("\"Path\": " + "\"" + Path + "\"");
      first = false;
    }
    if (this.Sha256 != null && !this.Sha256.equals("")) {
      if (!first) sb.append(", ");
      sb.append("\"Sha256\": " + "\"" + Sha256 + "\"");
      first = false;
    }
    if (this.Size != null && !this.Size.equals(0l)) {
      if (!first) sb.append(", ");
      sb.append("\"Size\": " + Size);
      first = false;
    }
    sb.append("}}");
    return sb.toString();
  }

  // TODO: actually hash
  public String getSHA() {
    StringBuilder sb = new StringBuilder();
    sb.append("AutogenPathDatasetComponentBlob");
    if (this.LastModifiedAtSource != null && !this.LastModifiedAtSource.equals(0l)) {
      sb.append("::LastModifiedAtSource::").append(LastModifiedAtSource);
    }
    if (this.Md5 != null && !this.Md5.equals("")) {
      sb.append("::Md5::").append(Md5);
    }
    if (this.Path != null && !this.Path.equals("")) {
      sb.append("::Path::").append(Path);
    }
    if (this.Sha256 != null && !this.Sha256.equals("")) {
      sb.append("::Sha256::").append(Sha256);
    }
    if (this.Size != null && !this.Size.equals(0l)) {
      sb.append("::Size::").append(Size);
    }

    return sb.toString();
  }

  // TODO: not consider order on lists
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (!(o instanceof AutogenPathDatasetComponentBlob)) return false;
    AutogenPathDatasetComponentBlob other = (AutogenPathDatasetComponentBlob) o;

    {
      Function3<Long, Long, Boolean> f = (x, y) -> x.equals(y);
      if (this.LastModifiedAtSource != null || other.LastModifiedAtSource != null) {
        if (this.LastModifiedAtSource == null && other.LastModifiedAtSource != null) return false;
        if (this.LastModifiedAtSource != null && other.LastModifiedAtSource == null) return false;
        if (!f.apply(this.LastModifiedAtSource, other.LastModifiedAtSource)) return false;
      }
    }
    {
      Function3<String, String, Boolean> f = (x, y) -> x.equals(y);
      if (this.Md5 != null || other.Md5 != null) {
        if (this.Md5 == null && other.Md5 != null) return false;
        if (this.Md5 != null && other.Md5 == null) return false;
        if (!f.apply(this.Md5, other.Md5)) return false;
      }
    }
    {
      Function3<String, String, Boolean> f = (x, y) -> x.equals(y);
      if (this.Path != null || other.Path != null) {
        if (this.Path == null && other.Path != null) return false;
        if (this.Path != null && other.Path == null) return false;
        if (!f.apply(this.Path, other.Path)) return false;
      }
    }
    {
      Function3<String, String, Boolean> f = (x, y) -> x.equals(y);
      if (this.Sha256 != null || other.Sha256 != null) {
        if (this.Sha256 == null && other.Sha256 != null) return false;
        if (this.Sha256 != null && other.Sha256 == null) return false;
        if (!f.apply(this.Sha256, other.Sha256)) return false;
      }
    }
    {
      Function3<Long, Long, Boolean> f = (x, y) -> x.equals(y);
      if (this.Size != null || other.Size != null) {
        if (this.Size == null && other.Size != null) return false;
        if (this.Size != null && other.Size == null) return false;
        if (!f.apply(this.Size, other.Size)) return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.LastModifiedAtSource, this.Md5, this.Path, this.Sha256, this.Size);
  }

  public AutogenPathDatasetComponentBlob setLastModifiedAtSource(Long value) {
    this.LastModifiedAtSource = Utils.removeEmpty(value);
    return this;
  }

  public Long getLastModifiedAtSource() {
    return this.LastModifiedAtSource;
  }

  public AutogenPathDatasetComponentBlob setMd5(String value) {
    this.Md5 = Utils.removeEmpty(value);
    return this;
  }

  public String getMd5() {
    return this.Md5;
  }

  public AutogenPathDatasetComponentBlob setPath(String value) {
    this.Path = Utils.removeEmpty(value);
    return this;
  }

  public String getPath() {
    return this.Path;
  }

  public AutogenPathDatasetComponentBlob setSha256(String value) {
    this.Sha256 = Utils.removeEmpty(value);
    return this;
  }

  public String getSha256() {
    return this.Sha256;
  }

  public AutogenPathDatasetComponentBlob setSize(Long value) {
    this.Size = Utils.removeEmpty(value);
    return this;
  }

  public Long getSize() {
    return this.Size;
  }

  public static AutogenPathDatasetComponentBlob fromProto(
      ai.verta.modeldb.versioning.PathDatasetComponentBlob blob) {
    if (blob == null) {
      return null;
    }

    AutogenPathDatasetComponentBlob obj = new AutogenPathDatasetComponentBlob();
    {
      Function<ai.verta.modeldb.versioning.PathDatasetComponentBlob, Long> f =
          x -> (blob.getLastModifiedAtSource());
      obj.setLastModifiedAtSource(f.apply(blob));
    }
    {
      Function<ai.verta.modeldb.versioning.PathDatasetComponentBlob, String> f =
          x -> (blob.getMd5());
      obj.setMd5(f.apply(blob));
    }
    {
      Function<ai.verta.modeldb.versioning.PathDatasetComponentBlob, String> f =
          x -> (blob.getPath());
      obj.setPath(f.apply(blob));
    }
    {
      Function<ai.verta.modeldb.versioning.PathDatasetComponentBlob, String> f =
          x -> (blob.getSha256());
      obj.setSha256(f.apply(blob));
    }
    {
      Function<ai.verta.modeldb.versioning.PathDatasetComponentBlob, Long> f =
          x -> (blob.getSize());
      obj.setSize(f.apply(blob));
    }
    return obj;
  }

  public ai.verta.modeldb.versioning.PathDatasetComponentBlob.Builder toProto() {
    ai.verta.modeldb.versioning.PathDatasetComponentBlob.Builder builder =
        ai.verta.modeldb.versioning.PathDatasetComponentBlob.newBuilder();
    {
      if (this.LastModifiedAtSource != null && !this.LastModifiedAtSource.equals(0l)) {
        Function<ai.verta.modeldb.versioning.PathDatasetComponentBlob.Builder, Void> f =
            x -> {
              builder.setLastModifiedAtSource(this.LastModifiedAtSource);
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Md5 != null && !this.Md5.equals("")) {
        Function<ai.verta.modeldb.versioning.PathDatasetComponentBlob.Builder, Void> f =
            x -> {
              builder.setMd5(this.Md5);
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Path != null && !this.Path.equals("")) {
        Function<ai.verta.modeldb.versioning.PathDatasetComponentBlob.Builder, Void> f =
            x -> {
              builder.setPath(this.Path);
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Sha256 != null && !this.Sha256.equals("")) {
        Function<ai.verta.modeldb.versioning.PathDatasetComponentBlob.Builder, Void> f =
            x -> {
              builder.setSha256(this.Sha256);
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Size != null && !this.Size.equals(0l)) {
        Function<ai.verta.modeldb.versioning.PathDatasetComponentBlob.Builder, Void> f =
            x -> {
              builder.setSize(this.Size);
              return null;
            };
        f.apply(builder);
      }
    }
    return builder;
  }

  public void preVisitShallow(Visitor visitor) throws ModelDBException {
    visitor.preVisitAutogenPathDatasetComponentBlob(this);
  }

  public void preVisitDeep(Visitor visitor) throws ModelDBException {
    this.preVisitShallow(visitor);
    visitor.preVisitDeepLong(this.LastModifiedAtSource);
    visitor.preVisitDeepString(this.Md5);
    visitor.preVisitDeepString(this.Path);
    visitor.preVisitDeepString(this.Sha256);
    visitor.preVisitDeepLong(this.Size);
  }

  public AutogenPathDatasetComponentBlob postVisitShallow(Visitor visitor) throws ModelDBException {
    return visitor.postVisitAutogenPathDatasetComponentBlob(this);
  }

  public AutogenPathDatasetComponentBlob postVisitDeep(Visitor visitor) throws ModelDBException {
    this.setLastModifiedAtSource(visitor.postVisitDeepLong(this.LastModifiedAtSource));
    this.setMd5(visitor.postVisitDeepString(this.Md5));
    this.setPath(visitor.postVisitDeepString(this.Path));
    this.setSha256(visitor.postVisitDeepString(this.Sha256));
    this.setSize(visitor.postVisitDeepLong(this.Size));
    return this.postVisitShallow(visitor);
  }
}
