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

public class AutogenHyperparameterConfigBlob implements ProtoType {
  private String Name;
  private AutogenHyperparameterValuesConfigBlob Value;

  public AutogenHyperparameterConfigBlob() {
    this.Name = "";
    this.Value = null;
  }

  public Boolean isEmpty() {
    if (this.Name != null && !this.Name.equals("")) {
      return false;
    }
    if (this.Value != null && !this.Value.equals(null)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"class\": \"AutogenHyperparameterConfigBlob\", \"fields\": {");
    boolean first = true;
    if (this.Name != null && !this.Name.equals("")) {
      if (!first) sb.append(", ");
      sb.append("\"Name\": " + "\"" + Name + "\"");
      first = false;
    }
    if (this.Value != null && !this.Value.equals(null)) {
      if (!first) sb.append(", ");
      sb.append("\"Value\": " + Value);
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
    if (!(o instanceof AutogenHyperparameterConfigBlob)) return false;
    AutogenHyperparameterConfigBlob other = (AutogenHyperparameterConfigBlob) o;

    {
      Function3<String, String, Boolean> f = (x, y) -> x.equals(y);
      if (this.Name != null || other.Name != null) {
        if (this.Name == null && other.Name != null) return false;
        if (this.Name != null && other.Name == null) return false;
        if (!f.apply(this.Name, other.Name)) return false;
      }
    }
    {
      Function3<
              AutogenHyperparameterValuesConfigBlob, AutogenHyperparameterValuesConfigBlob, Boolean>
          f = (x, y) -> x.equals(y);
      if (this.Value != null || other.Value != null) {
        if (this.Value == null && other.Value != null) return false;
        if (this.Value != null && other.Value == null) return false;
        if (!f.apply(this.Value, other.Value)) return false;
      }
    }
    return true;
  }

  public AutogenHyperparameterConfigBlob setName(String value) {
    this.Name = Utils.removeEmpty(value);
    return this;
  }

  public String getName() {
    return this.Name;
  }

  public AutogenHyperparameterConfigBlob setValue(AutogenHyperparameterValuesConfigBlob value) {
    this.Value = Utils.removeEmpty(value);
    return this;
  }

  public AutogenHyperparameterValuesConfigBlob getValue() {
    return this.Value;
  }

  public static AutogenHyperparameterConfigBlob fromProto(
      ai.verta.modeldb.versioning.HyperparameterConfigBlob blob) {
    if (blob == null) {
      return null;
    }

    AutogenHyperparameterConfigBlob obj = new AutogenHyperparameterConfigBlob();
    {
      Function<ai.verta.modeldb.versioning.HyperparameterConfigBlob, String> f =
          x -> (blob.getName());
      obj.setName(f.apply(blob));
    }
    {
      Function<
              ai.verta.modeldb.versioning.HyperparameterConfigBlob,
              AutogenHyperparameterValuesConfigBlob>
          f = x -> AutogenHyperparameterValuesConfigBlob.fromProto(blob.getValue());
      obj.setValue(f.apply(blob));
    }
    return obj;
  }

  public ai.verta.modeldb.versioning.HyperparameterConfigBlob.Builder toProto() {
    ai.verta.modeldb.versioning.HyperparameterConfigBlob.Builder builder =
        ai.verta.modeldb.versioning.HyperparameterConfigBlob.newBuilder();
    {
      if (this.Name != null && !this.Name.equals("")) {
        Function<ai.verta.modeldb.versioning.HyperparameterConfigBlob.Builder, Void> f =
            x -> {
              builder.setName(this.Name);
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Value != null && !this.Value.equals(null)) {
        Function<ai.verta.modeldb.versioning.HyperparameterConfigBlob.Builder, Void> f =
            x -> {
              builder.setValue(this.Value.toProto());
              return null;
            };
        f.apply(builder);
      }
    }
    return builder;
  }

  public void preVisitShallow(Visitor visitor) throws ModelDBException {
    visitor.preVisitAutogenHyperparameterConfigBlob(this);
  }

  public void preVisitDeep(Visitor visitor) throws ModelDBException {
    this.preVisitShallow(visitor);
    visitor.preVisitDeepString(this.Name);
    visitor.preVisitDeepAutogenHyperparameterValuesConfigBlob(this.Value);
  }

  public AutogenHyperparameterConfigBlob postVisitShallow(Visitor visitor) throws ModelDBException {
    return visitor.postVisitAutogenHyperparameterConfigBlob(this);
  }

  public AutogenHyperparameterConfigBlob postVisitDeep(Visitor visitor) throws ModelDBException {
    this.setName(visitor.postVisitDeepString(this.Name));
    this.setValue(visitor.postVisitDeepAutogenHyperparameterValuesConfigBlob(this.Value));
    return this.postVisitShallow(visitor);
  }
}
