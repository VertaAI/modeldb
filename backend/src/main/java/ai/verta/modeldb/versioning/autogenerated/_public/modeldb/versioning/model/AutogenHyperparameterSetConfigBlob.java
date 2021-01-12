// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model;

import ai.verta.modeldb.common.ModelDBException;
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

public class AutogenHyperparameterSetConfigBlob implements ProtoType {
  private AutogenContinuousHyperparameterSetConfigBlob Continuous;
  private AutogenDiscreteHyperparameterSetConfigBlob Discrete;
  private String Name;

  public AutogenHyperparameterSetConfigBlob() {
    this.Continuous = null;
    this.Discrete = null;
    this.Name = "";
  }

  public Boolean isEmpty() {
    if (this.Continuous != null && !this.Continuous.equals(null)) {
      return false;
    }
    if (this.Discrete != null && !this.Discrete.equals(null)) {
      return false;
    }
    if (this.Name != null && !this.Name.equals("")) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"class\": \"AutogenHyperparameterSetConfigBlob\", \"fields\": {");
    boolean first = true;
    if (this.Continuous != null && !this.Continuous.equals(null)) {
      if (!first) sb.append(", ");
      sb.append("\"Continuous\": " + Continuous);
      first = false;
    }
    if (this.Discrete != null && !this.Discrete.equals(null)) {
      if (!first) sb.append(", ");
      sb.append("\"Discrete\": " + Discrete);
      first = false;
    }
    if (this.Name != null && !this.Name.equals("")) {
      if (!first) sb.append(", ");
      sb.append("\"Name\": " + "\"" + Name + "\"");
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
    if (!(o instanceof AutogenHyperparameterSetConfigBlob)) return false;
    AutogenHyperparameterSetConfigBlob other = (AutogenHyperparameterSetConfigBlob) o;

    {
      Function3<
              AutogenContinuousHyperparameterSetConfigBlob,
              AutogenContinuousHyperparameterSetConfigBlob,
              Boolean>
          f = (x, y) -> x.equals(y);
      if (this.Continuous != null || other.Continuous != null) {
        if (this.Continuous == null && other.Continuous != null) return false;
        if (this.Continuous != null && other.Continuous == null) return false;
        if (!f.apply(this.Continuous, other.Continuous)) return false;
      }
    }
    {
      Function3<
              AutogenDiscreteHyperparameterSetConfigBlob,
              AutogenDiscreteHyperparameterSetConfigBlob,
              Boolean>
          f = (x, y) -> x.equals(y);
      if (this.Discrete != null || other.Discrete != null) {
        if (this.Discrete == null && other.Discrete != null) return false;
        if (this.Discrete != null && other.Discrete == null) return false;
        if (!f.apply(this.Discrete, other.Discrete)) return false;
      }
    }
    {
      Function3<String, String, Boolean> f = (x, y) -> x.equals(y);
      if (this.Name != null || other.Name != null) {
        if (this.Name == null && other.Name != null) return false;
        if (this.Name != null && other.Name == null) return false;
        if (!f.apply(this.Name, other.Name)) return false;
      }
    }
    return true;
  }

  public AutogenHyperparameterSetConfigBlob setContinuous(
      AutogenContinuousHyperparameterSetConfigBlob value) {
    this.Continuous = Utils.removeEmpty(value);
    return this;
  }

  public AutogenContinuousHyperparameterSetConfigBlob getContinuous() {
    return this.Continuous;
  }

  public AutogenHyperparameterSetConfigBlob setDiscrete(
      AutogenDiscreteHyperparameterSetConfigBlob value) {
    this.Discrete = Utils.removeEmpty(value);
    return this;
  }

  public AutogenDiscreteHyperparameterSetConfigBlob getDiscrete() {
    return this.Discrete;
  }

  public AutogenHyperparameterSetConfigBlob setName(String value) {
    this.Name = Utils.removeEmpty(value);
    return this;
  }

  public String getName() {
    return this.Name;
  }

  public static AutogenHyperparameterSetConfigBlob fromProto(
      ai.verta.modeldb.versioning.HyperparameterSetConfigBlob blob) {
    if (blob == null) {
      return null;
    }

    AutogenHyperparameterSetConfigBlob obj = new AutogenHyperparameterSetConfigBlob();
    {
      Function<
              ai.verta.modeldb.versioning.HyperparameterSetConfigBlob,
              AutogenContinuousHyperparameterSetConfigBlob>
          f = x -> AutogenContinuousHyperparameterSetConfigBlob.fromProto(blob.getContinuous());
      obj.setContinuous(f.apply(blob));
    }
    {
      Function<
              ai.verta.modeldb.versioning.HyperparameterSetConfigBlob,
              AutogenDiscreteHyperparameterSetConfigBlob>
          f = x -> AutogenDiscreteHyperparameterSetConfigBlob.fromProto(blob.getDiscrete());
      obj.setDiscrete(f.apply(blob));
    }
    {
      Function<ai.verta.modeldb.versioning.HyperparameterSetConfigBlob, String> f =
          x -> (blob.getName());
      obj.setName(f.apply(blob));
    }
    return obj;
  }

  public ai.verta.modeldb.versioning.HyperparameterSetConfigBlob.Builder toProto() {
    ai.verta.modeldb.versioning.HyperparameterSetConfigBlob.Builder builder =
        ai.verta.modeldb.versioning.HyperparameterSetConfigBlob.newBuilder();
    {
      if (this.Continuous != null && !this.Continuous.equals(null)) {
        Function<ai.verta.modeldb.versioning.HyperparameterSetConfigBlob.Builder, Void> f =
            x -> {
              builder.setContinuous(this.Continuous.toProto());
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Discrete != null && !this.Discrete.equals(null)) {
        Function<ai.verta.modeldb.versioning.HyperparameterSetConfigBlob.Builder, Void> f =
            x -> {
              builder.setDiscrete(this.Discrete.toProto());
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Name != null && !this.Name.equals("")) {
        Function<ai.verta.modeldb.versioning.HyperparameterSetConfigBlob.Builder, Void> f =
            x -> {
              builder.setName(this.Name);
              return null;
            };
        f.apply(builder);
      }
    }
    return builder;
  }

  public void preVisitShallow(Visitor visitor) throws ModelDBException {
    visitor.preVisitAutogenHyperparameterSetConfigBlob(this);
  }

  public void preVisitDeep(Visitor visitor) throws ModelDBException {
    this.preVisitShallow(visitor);
    visitor.preVisitDeepAutogenContinuousHyperparameterSetConfigBlob(this.Continuous);
    visitor.preVisitDeepAutogenDiscreteHyperparameterSetConfigBlob(this.Discrete);
    visitor.preVisitDeepString(this.Name);
  }

  public AutogenHyperparameterSetConfigBlob postVisitShallow(Visitor visitor)
      throws ModelDBException {
    return visitor.postVisitAutogenHyperparameterSetConfigBlob(this);
  }

  public AutogenHyperparameterSetConfigBlob postVisitDeep(Visitor visitor) throws ModelDBException {
    this.setContinuous(
        visitor.postVisitDeepAutogenContinuousHyperparameterSetConfigBlob(this.Continuous));
    this.setDiscrete(
        visitor.postVisitDeepAutogenDiscreteHyperparameterSetConfigBlob(this.Discrete));
    this.setName(visitor.postVisitDeepString(this.Name));
    return this.postVisitShallow(visitor);
  }
}
