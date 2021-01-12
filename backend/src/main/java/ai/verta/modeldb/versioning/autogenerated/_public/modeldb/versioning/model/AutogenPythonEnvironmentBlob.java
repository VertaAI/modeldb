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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.codec.binary.Hex;

public class AutogenPythonEnvironmentBlob implements ProtoType {
  private List<AutogenPythonRequirementEnvironmentBlob> Constraints;
  private List<AutogenPythonRequirementEnvironmentBlob> Requirements;
  private AutogenVersionEnvironmentBlob Version;

  public AutogenPythonEnvironmentBlob() {
    this.Constraints = null;
    this.Requirements = null;
    this.Version = null;
  }

  public Boolean isEmpty() {
    if (this.Constraints != null && !this.Constraints.equals(null) && !this.Constraints.isEmpty()) {
      return false;
    }
    if (this.Requirements != null
        && !this.Requirements.equals(null)
        && !this.Requirements.isEmpty()) {
      return false;
    }
    if (this.Version != null && !this.Version.equals(null)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"class\": \"AutogenPythonEnvironmentBlob\", \"fields\": {");
    boolean first = true;
    if (this.Constraints != null && !this.Constraints.equals(null) && !this.Constraints.isEmpty()) {
      if (!first) sb.append(", ");
      sb.append("\"Constraints\": " + Constraints);
      first = false;
    }
    if (this.Requirements != null
        && !this.Requirements.equals(null)
        && !this.Requirements.isEmpty()) {
      if (!first) sb.append(", ");
      sb.append("\"Requirements\": " + Requirements);
      first = false;
    }
    if (this.Version != null && !this.Version.equals(null)) {
      if (!first) sb.append(", ");
      sb.append("\"Version\": " + Version);
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
    if (!(o instanceof AutogenPythonEnvironmentBlob)) return false;
    AutogenPythonEnvironmentBlob other = (AutogenPythonEnvironmentBlob) o;

    {
      Function3<
              List<AutogenPythonRequirementEnvironmentBlob>,
              List<AutogenPythonRequirementEnvironmentBlob>,
              Boolean>
          f =
              (x2, y2) ->
                  IntStream.range(0, Math.min(x2.size(), y2.size()))
                      .mapToObj(
                          i -> {
                            Function3<
                                    AutogenPythonRequirementEnvironmentBlob,
                                    AutogenPythonRequirementEnvironmentBlob,
                                    Boolean>
                                f2 = (x, y) -> x.equals(y);
                            return f2.apply(x2.get(i), y2.get(i));
                          })
                      .filter(x -> x.equals(false))
                      .collect(Collectors.toList())
                      .isEmpty();
      if (this.Constraints != null || other.Constraints != null) {
        if (this.Constraints == null && other.Constraints != null) return false;
        if (this.Constraints != null && other.Constraints == null) return false;
        if (!f.apply(this.Constraints, other.Constraints)) return false;
      }
    }
    {
      Function3<
              List<AutogenPythonRequirementEnvironmentBlob>,
              List<AutogenPythonRequirementEnvironmentBlob>,
              Boolean>
          f =
              (x2, y2) ->
                  IntStream.range(0, Math.min(x2.size(), y2.size()))
                      .mapToObj(
                          i -> {
                            Function3<
                                    AutogenPythonRequirementEnvironmentBlob,
                                    AutogenPythonRequirementEnvironmentBlob,
                                    Boolean>
                                f2 = (x, y) -> x.equals(y);
                            return f2.apply(x2.get(i), y2.get(i));
                          })
                      .filter(x -> x.equals(false))
                      .collect(Collectors.toList())
                      .isEmpty();
      if (this.Requirements != null || other.Requirements != null) {
        if (this.Requirements == null && other.Requirements != null) return false;
        if (this.Requirements != null && other.Requirements == null) return false;
        if (!f.apply(this.Requirements, other.Requirements)) return false;
      }
    }
    {
      Function3<AutogenVersionEnvironmentBlob, AutogenVersionEnvironmentBlob, Boolean> f =
          (x, y) -> x.equals(y);
      if (this.Version != null || other.Version != null) {
        if (this.Version == null && other.Version != null) return false;
        if (this.Version != null && other.Version == null) return false;
        if (!f.apply(this.Version, other.Version)) return false;
      }
    }
    return true;
  }

  public AutogenPythonEnvironmentBlob setConstraints(
      List<AutogenPythonRequirementEnvironmentBlob> value) {
    this.Constraints = Utils.removeEmpty(value);
    if (this.Constraints != null) {
      this.Constraints.sort(
          Comparator.comparingInt(AutogenPythonRequirementEnvironmentBlob::hashCode));
    }
    return this;
  }

  public List<AutogenPythonRequirementEnvironmentBlob> getConstraints() {
    return this.Constraints;
  }

  public AutogenPythonEnvironmentBlob setRequirements(
      List<AutogenPythonRequirementEnvironmentBlob> value) {
    this.Requirements = Utils.removeEmpty(value);
    if (this.Requirements != null) {
      this.Requirements.sort(
          Comparator.comparingInt(AutogenPythonRequirementEnvironmentBlob::hashCode));
    }
    return this;
  }

  public List<AutogenPythonRequirementEnvironmentBlob> getRequirements() {
    return this.Requirements;
  }

  public AutogenPythonEnvironmentBlob setVersion(AutogenVersionEnvironmentBlob value) {
    this.Version = Utils.removeEmpty(value);
    return this;
  }

  public AutogenVersionEnvironmentBlob getVersion() {
    return this.Version;
  }

  public static AutogenPythonEnvironmentBlob fromProto(
      ai.verta.modeldb.versioning.PythonEnvironmentBlob blob) {
    if (blob == null) {
      return null;
    }

    AutogenPythonEnvironmentBlob obj = new AutogenPythonEnvironmentBlob();
    {
      Function<
              ai.verta.modeldb.versioning.PythonEnvironmentBlob,
              List<AutogenPythonRequirementEnvironmentBlob>>
          f =
              x ->
                  blob.getConstraintsList().stream()
                      .map(AutogenPythonRequirementEnvironmentBlob::fromProto)
                      .collect(Collectors.toList());
      obj.setConstraints(f.apply(blob));
    }
    {
      Function<
              ai.verta.modeldb.versioning.PythonEnvironmentBlob,
              List<AutogenPythonRequirementEnvironmentBlob>>
          f =
              x ->
                  blob.getRequirementsList().stream()
                      .map(AutogenPythonRequirementEnvironmentBlob::fromProto)
                      .collect(Collectors.toList());
      obj.setRequirements(f.apply(blob));
    }
    {
      Function<ai.verta.modeldb.versioning.PythonEnvironmentBlob, AutogenVersionEnvironmentBlob> f =
          x -> AutogenVersionEnvironmentBlob.fromProto(blob.getVersion());
      obj.setVersion(f.apply(blob));
    }
    return obj;
  }

  public ai.verta.modeldb.versioning.PythonEnvironmentBlob.Builder toProto() {
    ai.verta.modeldb.versioning.PythonEnvironmentBlob.Builder builder =
        ai.verta.modeldb.versioning.PythonEnvironmentBlob.newBuilder();
    {
      if (this.Constraints != null
          && !this.Constraints.equals(null)
          && !this.Constraints.isEmpty()) {
        Function<ai.verta.modeldb.versioning.PythonEnvironmentBlob.Builder, Void> f =
            x -> {
              builder.addAllConstraints(
                  this.Constraints.stream()
                      .map(y -> y.toProto().build())
                      .collect(Collectors.toList()));
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Requirements != null
          && !this.Requirements.equals(null)
          && !this.Requirements.isEmpty()) {
        Function<ai.verta.modeldb.versioning.PythonEnvironmentBlob.Builder, Void> f =
            x -> {
              builder.addAllRequirements(
                  this.Requirements.stream()
                      .map(y -> y.toProto().build())
                      .collect(Collectors.toList()));
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Version != null && !this.Version.equals(null)) {
        Function<ai.verta.modeldb.versioning.PythonEnvironmentBlob.Builder, Void> f =
            x -> {
              builder.setVersion(this.Version.toProto());
              return null;
            };
        f.apply(builder);
      }
    }
    return builder;
  }

  public void preVisitShallow(Visitor visitor) throws ModelDBException {
    visitor.preVisitAutogenPythonEnvironmentBlob(this);
  }

  public void preVisitDeep(Visitor visitor) throws ModelDBException {
    this.preVisitShallow(visitor);
    visitor.preVisitDeepListOfAutogenPythonRequirementEnvironmentBlob(this.Constraints);

    visitor.preVisitDeepListOfAutogenPythonRequirementEnvironmentBlob(this.Requirements);

    visitor.preVisitDeepAutogenVersionEnvironmentBlob(this.Version);
  }

  public AutogenPythonEnvironmentBlob postVisitShallow(Visitor visitor) throws ModelDBException {
    return visitor.postVisitAutogenPythonEnvironmentBlob(this);
  }

  public AutogenPythonEnvironmentBlob postVisitDeep(Visitor visitor) throws ModelDBException {
    this.setConstraints(
        visitor.postVisitDeepListOfAutogenPythonRequirementEnvironmentBlob(this.Constraints));

    this.setRequirements(
        visitor.postVisitDeepListOfAutogenPythonRequirementEnvironmentBlob(this.Requirements));

    this.setVersion(visitor.postVisitDeepAutogenVersionEnvironmentBlob(this.Version));
    return this.postVisitShallow(visitor);
  }
}
