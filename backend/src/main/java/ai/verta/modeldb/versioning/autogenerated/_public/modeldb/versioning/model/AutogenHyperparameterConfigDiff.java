// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.versioning.*;
import ai.verta.modeldb.versioning.blob.diff.*;
import ai.verta.modeldb.versioning.blob.diff.Function3;
import ai.verta.modeldb.versioning.blob.visitors.Visitor;
import com.pholser.junit.quickcheck.generator.*;
import com.pholser.junit.quickcheck.random.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import org.apache.commons.codec.binary.Hex;

public class AutogenHyperparameterConfigDiff implements ProtoType {
  private AutogenHyperparameterConfigBlob A;
  private AutogenHyperparameterConfigBlob B;
  private AutogenDiffStatusEnumDiffStatus Status;

  public AutogenHyperparameterConfigDiff() {
    this.A = null;
    this.B = null;
    this.Status = null;
  }

  public Boolean isEmpty() {
    if (this.A != null && !this.A.equals(null)) {
      return false;
    }
    if (this.B != null && !this.B.equals(null)) {
      return false;
    }
    if (this.Status != null && !this.Status.equals(null)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"class\": \"AutogenHyperparameterConfigDiff\", \"fields\": {");
    boolean first = true;
    if (this.A != null && !this.A.equals(null)) {
      if (!first) sb.append(", ");
      sb.append("\"A\": " + A);
      first = false;
    }
    if (this.B != null && !this.B.equals(null)) {
      if (!first) sb.append(", ");
      sb.append("\"B\": " + B);
      first = false;
    }
    if (this.Status != null && !this.Status.equals(null)) {
      if (!first) sb.append(", ");
      sb.append("\"Status\": " + Status);
      first = false;
    }
    sb.append("}}");
    return sb.toString();
  }

  // TODO: actually hash
  public String getSHA() throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(this.toString().getBytes(StandardCharsets.UTF_8));
    return new String(new Hex().encode(hash));
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.toString());
  }

  // TODO: not consider order on lists
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (!(o instanceof AutogenHyperparameterConfigDiff)) return false;
    AutogenHyperparameterConfigDiff other = (AutogenHyperparameterConfigDiff) o;

    {
      Function3<AutogenHyperparameterConfigBlob, AutogenHyperparameterConfigBlob, Boolean> f =
          (x, y) -> x.equals(y);
      if (this.A != null || other.A != null) {
        if (this.A == null && other.A != null) return false;
        if (this.A != null && other.A == null) return false;
        if (!f.apply(this.A, other.A)) return false;
      }
    }
    {
      Function3<AutogenHyperparameterConfigBlob, AutogenHyperparameterConfigBlob, Boolean> f =
          (x, y) -> x.equals(y);
      if (this.B != null || other.B != null) {
        if (this.B == null && other.B != null) return false;
        if (this.B != null && other.B == null) return false;
        if (!f.apply(this.B, other.B)) return false;
      }
    }
    {
      Function3<AutogenDiffStatusEnumDiffStatus, AutogenDiffStatusEnumDiffStatus, Boolean> f =
          (x, y) -> x.equals(y);
      if (this.Status != null || other.Status != null) {
        if (this.Status == null && other.Status != null) return false;
        if (this.Status != null && other.Status == null) return false;
        if (!f.apply(this.Status, other.Status)) return false;
      }
    }
    return true;
  }

  public AutogenHyperparameterConfigDiff setA(AutogenHyperparameterConfigBlob value) {
    this.A = Utils.removeEmpty(value);
    return this;
  }

  public AutogenHyperparameterConfigBlob getA() {
    return this.A;
  }

  public AutogenHyperparameterConfigDiff setB(AutogenHyperparameterConfigBlob value) {
    this.B = Utils.removeEmpty(value);
    return this;
  }

  public AutogenHyperparameterConfigBlob getB() {
    return this.B;
  }

  public AutogenHyperparameterConfigDiff setStatus(AutogenDiffStatusEnumDiffStatus value) {
    this.Status = Utils.removeEmpty(value);
    return this;
  }

  public AutogenDiffStatusEnumDiffStatus getStatus() {
    return this.Status;
  }

  public static AutogenHyperparameterConfigDiff fromProto(
      ai.verta.modeldb.versioning.HyperparameterConfigDiff blob) {
    if (blob == null) {
      return null;
    }

    AutogenHyperparameterConfigDiff obj = new AutogenHyperparameterConfigDiff();
    {
      Function<
              ai.verta.modeldb.versioning.HyperparameterConfigDiff, AutogenHyperparameterConfigBlob>
          f = x -> AutogenHyperparameterConfigBlob.fromProto(blob.getA());
      obj.setA(f.apply(blob));
    }
    {
      Function<
              ai.verta.modeldb.versioning.HyperparameterConfigDiff, AutogenHyperparameterConfigBlob>
          f = x -> AutogenHyperparameterConfigBlob.fromProto(blob.getB());
      obj.setB(f.apply(blob));
    }
    {
      Function<
              ai.verta.modeldb.versioning.HyperparameterConfigDiff, AutogenDiffStatusEnumDiffStatus>
          f = x -> AutogenDiffStatusEnumDiffStatus.fromProto(blob.getStatus());
      obj.setStatus(f.apply(blob));
    }
    return obj;
  }

  public ai.verta.modeldb.versioning.HyperparameterConfigDiff.Builder toProto() {
    ai.verta.modeldb.versioning.HyperparameterConfigDiff.Builder builder =
        ai.verta.modeldb.versioning.HyperparameterConfigDiff.newBuilder();
    {
      if (this.A != null && !this.A.equals(null)) {
        Function<ai.verta.modeldb.versioning.HyperparameterConfigDiff.Builder, Void> f =
            x -> {
              builder.setA(this.A.toProto());
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.B != null && !this.B.equals(null)) {
        Function<ai.verta.modeldb.versioning.HyperparameterConfigDiff.Builder, Void> f =
            x -> {
              builder.setB(this.B.toProto());
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Status != null && !this.Status.equals(null)) {
        Function<ai.verta.modeldb.versioning.HyperparameterConfigDiff.Builder, Void> f =
            x -> {
              builder.setStatus(this.Status.toProto());
              return null;
            };
        f.apply(builder);
      }
    }
    return builder;
  }

  public void preVisitShallow(Visitor visitor) throws ModelDBException {
    visitor.preVisitAutogenHyperparameterConfigDiff(this);
  }

  public void preVisitDeep(Visitor visitor) throws ModelDBException {
    this.preVisitShallow(visitor);
    visitor.preVisitDeepAutogenHyperparameterConfigBlob(this.A);
    visitor.preVisitDeepAutogenHyperparameterConfigBlob(this.B);
    visitor.preVisitDeepAutogenDiffStatusEnumDiffStatus(this.Status);
  }

  public AutogenHyperparameterConfigDiff postVisitShallow(Visitor visitor) throws ModelDBException {
    return visitor.postVisitAutogenHyperparameterConfigDiff(this);
  }

  public AutogenHyperparameterConfigDiff postVisitDeep(Visitor visitor) throws ModelDBException {
    this.setA(visitor.postVisitDeepAutogenHyperparameterConfigBlob(this.A));
    this.setB(visitor.postVisitDeepAutogenHyperparameterConfigBlob(this.B));
    this.setStatus(visitor.postVisitDeepAutogenDiffStatusEnumDiffStatus(this.Status));
    return this.postVisitShallow(visitor);
  }
}
