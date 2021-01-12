// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.versioning.blob.diff.*;
import ai.verta.modeldb.versioning.blob.diff.Function3;
import ai.verta.modeldb.versioning.blob.visitors.Visitor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.codec.binary.Hex;

public class AutogenEnvironmentBlob implements ProtoType {
  private List<String> CommandLine;
  private AutogenDockerEnvironmentBlob Docker;
  private List<AutogenEnvironmentVariablesBlob> EnvironmentVariables;
  private AutogenPythonEnvironmentBlob Python;

  public AutogenEnvironmentBlob() {
    this.CommandLine = null;
    this.Docker = null;
    this.EnvironmentVariables = null;
    this.Python = null;
  }

  public Boolean isEmpty() {
    if (this.CommandLine != null && !this.CommandLine.equals(null) && !this.CommandLine.isEmpty()) {
      return false;
    }
    if (this.Docker != null && !this.Docker.equals(null)) {
      return false;
    }
    if (this.EnvironmentVariables != null
        && !this.EnvironmentVariables.equals(null)
        && !this.EnvironmentVariables.isEmpty()) {
      return false;
    }
    if (this.Python != null && !this.Python.equals(null)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"class\": \"AutogenEnvironmentBlob\", \"fields\": {");
    boolean first = true;
    if (this.CommandLine != null && !this.CommandLine.equals(null) && !this.CommandLine.isEmpty()) {
      if (!first) sb.append(", ");
      sb.append("\"CommandLine\": " + CommandLine);
      first = false;
    }
    if (this.Docker != null && !this.Docker.equals(null)) {
      if (!first) sb.append(", ");
      sb.append("\"Docker\": " + Docker);
      first = false;
    }
    if (this.EnvironmentVariables != null
        && !this.EnvironmentVariables.equals(null)
        && !this.EnvironmentVariables.isEmpty()) {
      if (!first) sb.append(", ");
      sb.append("\"EnvironmentVariables\": " + EnvironmentVariables);
      first = false;
    }
    if (this.Python != null && !this.Python.equals(null)) {
      if (!first) sb.append(", ");
      sb.append("\"Python\": " + Python);
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
    if (!(o instanceof AutogenEnvironmentBlob)) return false;
    AutogenEnvironmentBlob other = (AutogenEnvironmentBlob) o;

    {
      Function3<List<String>, List<String>, Boolean> f =
          (x2, y2) ->
              IntStream.range(0, Math.min(x2.size(), y2.size()))
                  .mapToObj(
                      i -> {
                        Function3<String, String, Boolean> f2 = (x, y) -> x.equals(y);
                        return f2.apply(x2.get(i), y2.get(i));
                      })
                  .filter(x -> x.equals(false))
                  .collect(Collectors.toList())
                  .isEmpty();
      if (this.CommandLine != null || other.CommandLine != null) {
        if (this.CommandLine == null && other.CommandLine != null) return false;
        if (this.CommandLine != null && other.CommandLine == null) return false;
        if (!f.apply(this.CommandLine, other.CommandLine)) return false;
      }
    }
    {
      Function3<AutogenDockerEnvironmentBlob, AutogenDockerEnvironmentBlob, Boolean> f =
          (x, y) -> x.equals(y);
      if (this.Docker != null || other.Docker != null) {
        if (this.Docker == null && other.Docker != null) return false;
        if (this.Docker != null && other.Docker == null) return false;
        if (!f.apply(this.Docker, other.Docker)) return false;
      }
    }
    {
      Function3<
              List<AutogenEnvironmentVariablesBlob>, List<AutogenEnvironmentVariablesBlob>, Boolean>
          f =
              (x2, y2) ->
                  IntStream.range(0, Math.min(x2.size(), y2.size()))
                      .mapToObj(
                          i -> {
                            Function3<
                                    AutogenEnvironmentVariablesBlob,
                                    AutogenEnvironmentVariablesBlob,
                                    Boolean>
                                f2 = (x, y) -> x.equals(y);
                            return f2.apply(x2.get(i), y2.get(i));
                          })
                      .filter(x -> x.equals(false))
                      .collect(Collectors.toList())
                      .isEmpty();
      if (this.EnvironmentVariables != null || other.EnvironmentVariables != null) {
        if (this.EnvironmentVariables == null && other.EnvironmentVariables != null) return false;
        if (this.EnvironmentVariables != null && other.EnvironmentVariables == null) return false;
        if (!f.apply(this.EnvironmentVariables, other.EnvironmentVariables)) return false;
      }
    }
    {
      Function3<AutogenPythonEnvironmentBlob, AutogenPythonEnvironmentBlob, Boolean> f =
          (x, y) -> x.equals(y);
      if (this.Python != null || other.Python != null) {
        if (this.Python == null && other.Python != null) return false;
        if (this.Python != null && other.Python == null) return false;
        if (!f.apply(this.Python, other.Python)) return false;
      }
    }
    return true;
  }

  public AutogenEnvironmentBlob setCommandLine(List<String> value) {
    this.CommandLine = Utils.removeEmpty(value);
    return this;
  }

  public List<String> getCommandLine() {
    return this.CommandLine;
  }

  public AutogenEnvironmentBlob setDocker(AutogenDockerEnvironmentBlob value) {
    this.Docker = Utils.removeEmpty(value);
    return this;
  }

  public AutogenDockerEnvironmentBlob getDocker() {
    return this.Docker;
  }

  public AutogenEnvironmentBlob setEnvironmentVariables(
      List<AutogenEnvironmentVariablesBlob> value) {
    this.EnvironmentVariables = Utils.removeEmpty(value);
    if (this.EnvironmentVariables != null) {
      this.EnvironmentVariables.sort(
          Comparator.comparingInt(AutogenEnvironmentVariablesBlob::hashCode));
    }
    return this;
  }

  public List<AutogenEnvironmentVariablesBlob> getEnvironmentVariables() {
    return this.EnvironmentVariables;
  }

  public AutogenEnvironmentBlob setPython(AutogenPythonEnvironmentBlob value) {
    this.Python = Utils.removeEmpty(value);
    return this;
  }

  public AutogenPythonEnvironmentBlob getPython() {
    return this.Python;
  }

  public static AutogenEnvironmentBlob fromProto(ai.verta.modeldb.versioning.EnvironmentBlob blob) {
    if (blob == null) {
      return null;
    }

    AutogenEnvironmentBlob obj = new AutogenEnvironmentBlob();
    {
      Function<ai.verta.modeldb.versioning.EnvironmentBlob, List<String>> f =
          x -> blob.getCommandLineList();
      obj.setCommandLine(f.apply(blob));
    }
    {
      Function<ai.verta.modeldb.versioning.EnvironmentBlob, AutogenDockerEnvironmentBlob> f =
          x -> AutogenDockerEnvironmentBlob.fromProto(blob.getDocker());
      obj.setDocker(f.apply(blob));
    }
    {
      Function<ai.verta.modeldb.versioning.EnvironmentBlob, List<AutogenEnvironmentVariablesBlob>>
          f =
              x ->
                  blob.getEnvironmentVariablesList().stream()
                      .map(AutogenEnvironmentVariablesBlob::fromProto)
                      .collect(Collectors.toList());
      obj.setEnvironmentVariables(f.apply(blob));
    }
    {
      Function<ai.verta.modeldb.versioning.EnvironmentBlob, AutogenPythonEnvironmentBlob> f =
          x -> AutogenPythonEnvironmentBlob.fromProto(blob.getPython());
      obj.setPython(f.apply(blob));
    }
    return obj;
  }

  public ai.verta.modeldb.versioning.EnvironmentBlob.Builder toProto() {
    ai.verta.modeldb.versioning.EnvironmentBlob.Builder builder =
        ai.verta.modeldb.versioning.EnvironmentBlob.newBuilder();
    {
      if (this.CommandLine != null
          && !this.CommandLine.equals(null)
          && !this.CommandLine.isEmpty()) {
        Function<ai.verta.modeldb.versioning.EnvironmentBlob.Builder, Void> f =
            x -> {
              builder.addAllCommandLine(this.CommandLine);
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Docker != null && !this.Docker.equals(null)) {
        Function<ai.verta.modeldb.versioning.EnvironmentBlob.Builder, Void> f =
            x -> {
              builder.setDocker(this.Docker.toProto());
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.EnvironmentVariables != null
          && !this.EnvironmentVariables.equals(null)
          && !this.EnvironmentVariables.isEmpty()) {
        Function<ai.verta.modeldb.versioning.EnvironmentBlob.Builder, Void> f =
            x -> {
              builder.addAllEnvironmentVariables(
                  this.EnvironmentVariables.stream()
                      .map(y -> y.toProto().build())
                      .collect(Collectors.toList()));
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Python != null && !this.Python.equals(null)) {
        Function<ai.verta.modeldb.versioning.EnvironmentBlob.Builder, Void> f =
            x -> {
              builder.setPython(this.Python.toProto());
              return null;
            };
        f.apply(builder);
      }
    }
    return builder;
  }

  public void preVisitShallow(Visitor visitor) throws ModelDBException {
    visitor.preVisitAutogenEnvironmentBlob(this);
  }

  public void preVisitDeep(Visitor visitor) throws ModelDBException {
    this.preVisitShallow(visitor);
    visitor.preVisitDeepListOfString(this.CommandLine);

    visitor.preVisitDeepAutogenDockerEnvironmentBlob(this.Docker);
    visitor.preVisitDeepListOfAutogenEnvironmentVariablesBlob(this.EnvironmentVariables);

    visitor.preVisitDeepAutogenPythonEnvironmentBlob(this.Python);
  }

  public AutogenEnvironmentBlob postVisitShallow(Visitor visitor) throws ModelDBException {
    return visitor.postVisitAutogenEnvironmentBlob(this);
  }

  public AutogenEnvironmentBlob postVisitDeep(Visitor visitor) throws ModelDBException {
    this.setCommandLine(visitor.postVisitDeepListOfString(this.CommandLine));

    this.setDocker(visitor.postVisitDeepAutogenDockerEnvironmentBlob(this.Docker));
    this.setEnvironmentVariables(
        visitor.postVisitDeepListOfAutogenEnvironmentVariablesBlob(this.EnvironmentVariables));

    this.setPython(visitor.postVisitDeepAutogenPythonEnvironmentBlob(this.Python));
    return this.postVisitShallow(visitor);
  }
}
