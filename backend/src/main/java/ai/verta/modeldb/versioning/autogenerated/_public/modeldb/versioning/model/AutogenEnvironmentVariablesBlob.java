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

public class AutogenEnvironmentVariablesBlob implements ProtoType {
  private String Name;
  private String Value;

  public AutogenEnvironmentVariablesBlob() {
    this.Name = "";
    this.Value = "";
  }

  public Boolean isEmpty() {
    if (this.Name != null && !this.Name.equals("")) {
      return false;
    }
    if (this.Value != null && !this.Value.equals("")) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"class\": \"AutogenEnvironmentVariablesBlob\", \"fields\": {");
    boolean first = true;
    if (this.Name != null && !this.Name.equals("")) {
      if (!first) sb.append(", ");
      sb.append("\"Name\": " + "\"" + Name + "\"");
      first = false;
    }
    if (this.Value != null && !this.Value.equals("")) {
      if (!first) sb.append(", ");
      sb.append("\"Value\": " + "\"" + Value + "\"");
      first = false;
    }
    sb.append("}}");
    return sb.toString();
  }

  // TODO: actually hash
  public String getSHA() {
    StringBuilder sb = new StringBuilder();
    sb.append("AutogenEnvironmentVariablesBlob");
    if (this.Name != null && !this.Name.equals("")) {
      sb.append("::Name::").append(Name);
    }
    if (this.Value != null && !this.Value.equals("")) {
      sb.append("::Value::").append(Value);
    }

    return sb.toString();
  }

  // TODO: not consider order on lists
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (!(o instanceof AutogenEnvironmentVariablesBlob)) return false;
    AutogenEnvironmentVariablesBlob other = (AutogenEnvironmentVariablesBlob) o;

    {
      Function3<String, String, Boolean> f = (x, y) -> x.equals(y);
      if (this.Name != null || other.Name != null) {
        if (this.Name == null && other.Name != null) return false;
        if (this.Name != null && other.Name == null) return false;
        if (!f.apply(this.Name, other.Name)) return false;
      }
    }
    {
      Function3<String, String, Boolean> f = (x, y) -> x.equals(y);
      if (this.Value != null || other.Value != null) {
        if (this.Value == null && other.Value != null) return false;
        if (this.Value != null && other.Value == null) return false;
        if (!f.apply(this.Value, other.Value)) return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.Name, this.Value);
  }

  public AutogenEnvironmentVariablesBlob setName(String value) {
    this.Name = Utils.removeEmpty(value);
    return this;
  }

  public String getName() {
    return this.Name;
  }

  public AutogenEnvironmentVariablesBlob setValue(String value) {
    this.Value = Utils.removeEmpty(value);
    return this;
  }

  public String getValue() {
    return this.Value;
  }

  public static AutogenEnvironmentVariablesBlob fromProto(
      ai.verta.modeldb.versioning.EnvironmentVariablesBlob blob) {
    if (blob == null) {
      return null;
    }

    AutogenEnvironmentVariablesBlob obj = new AutogenEnvironmentVariablesBlob();
    {
      Function<ai.verta.modeldb.versioning.EnvironmentVariablesBlob, String> f =
          x -> (blob.getName());
      obj.setName(f.apply(blob));
    }
    {
      Function<ai.verta.modeldb.versioning.EnvironmentVariablesBlob, String> f =
          x -> (blob.getValue());
      obj.setValue(f.apply(blob));
    }
    return obj;
  }

  public ai.verta.modeldb.versioning.EnvironmentVariablesBlob.Builder toProto() {
    ai.verta.modeldb.versioning.EnvironmentVariablesBlob.Builder builder =
        ai.verta.modeldb.versioning.EnvironmentVariablesBlob.newBuilder();
    {
      if (this.Name != null && !this.Name.equals("")) {
        Function<ai.verta.modeldb.versioning.EnvironmentVariablesBlob.Builder, Void> f =
            x -> {
              builder.setName(this.Name);
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Value != null && !this.Value.equals("")) {
        Function<ai.verta.modeldb.versioning.EnvironmentVariablesBlob.Builder, Void> f =
            x -> {
              builder.setValue(this.Value);
              return null;
            };
        f.apply(builder);
      }
    }
    return builder;
  }

  public void preVisitShallow(Visitor visitor) throws ModelDBException {
    visitor.preVisitAutogenEnvironmentVariablesBlob(this);
  }

  public void preVisitDeep(Visitor visitor) throws ModelDBException {
    this.preVisitShallow(visitor);
    visitor.preVisitDeepString(this.Name);
    visitor.preVisitDeepString(this.Value);
  }

  public AutogenEnvironmentVariablesBlob postVisitShallow(Visitor visitor) throws ModelDBException {
    return visitor.postVisitAutogenEnvironmentVariablesBlob(this);
  }

  public AutogenEnvironmentVariablesBlob postVisitDeep(Visitor visitor) throws ModelDBException {
    this.setName(visitor.postVisitDeepString(this.Name));
    this.setValue(visitor.postVisitDeepString(this.Value));
    return this.postVisitShallow(visitor);
  }
}
