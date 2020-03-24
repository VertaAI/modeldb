// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model;

import ai.verta.modeldb.ModelDBException;
import ai.verta.modeldb.versioning.*;
import ai.verta.modeldb.versioning.blob.diff.*;
import ai.verta.modeldb.versioning.blob.diff.Function3;
import ai.verta.modeldb.versioning.blob.visitors.Visitor;
import com.pholser.junit.quickcheck.generator.*;
import com.pholser.junit.quickcheck.random.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EnvironmentDiff implements ProtoType {
  private CommandLineEnvironmentDiff CommandLine;
  private DockerEnvironmentDiff Docker;
  private List<EnvironmentVariablesDiff> EnvironmentVariables;
  private PythonEnvironmentDiff Python;

  public EnvironmentDiff() {
    this.CommandLine = null;
    this.Docker = null;
    this.EnvironmentVariables = null;
    this.Python = null;
  }

  public Boolean isEmpty() {
    if (this.CommandLine != null && !this.CommandLine.equals(null)) {
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
    sb.append("{\"class\": \"EnvironmentDiff\", \"fields\": {");
    boolean first = true;
    if (this.CommandLine != null && !this.CommandLine.equals(null)) {
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
  public String getSHA() {
    StringBuilder sb = new StringBuilder();
    sb.append("EnvironmentDiff");
    if (this.CommandLine != null && !this.CommandLine.equals(null)) {
      sb.append("::CommandLine::").append(CommandLine);
    }
    if (this.Docker != null && !this.Docker.equals(null)) {
      sb.append("::Docker::").append(Docker);
    }
    if (this.EnvironmentVariables != null
        && !this.EnvironmentVariables.equals(null)
        && !this.EnvironmentVariables.isEmpty()) {
      sb.append("::EnvironmentVariables::").append(EnvironmentVariables);
    }
    if (this.Python != null && !this.Python.equals(null)) {
      sb.append("::Python::").append(Python);
    }

    return sb.toString();
  }

  // TODO: not consider order on lists
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (!(o instanceof EnvironmentDiff)) return false;
    EnvironmentDiff other = (EnvironmentDiff) o;

    {
      Function3<CommandLineEnvironmentDiff, CommandLineEnvironmentDiff, Boolean> f =
          (x, y) -> x.equals(y);
      if (this.CommandLine != null || other.CommandLine != null) {
        if (this.CommandLine == null && other.CommandLine != null) return false;
        if (this.CommandLine != null && other.CommandLine == null) return false;
        if (!f.apply(this.CommandLine, other.CommandLine)) return false;
      }
    }
    {
      Function3<DockerEnvironmentDiff, DockerEnvironmentDiff, Boolean> f = (x, y) -> x.equals(y);
      if (this.Docker != null || other.Docker != null) {
        if (this.Docker == null && other.Docker != null) return false;
        if (this.Docker != null && other.Docker == null) return false;
        if (!f.apply(this.Docker, other.Docker)) return false;
      }
    }
    {
      Function3<List<EnvironmentVariablesDiff>, List<EnvironmentVariablesDiff>, Boolean> f =
          (x2, y2) ->
              IntStream.range(0, Math.min(x2.size(), y2.size()))
                  .mapToObj(
                      i -> {
                        Function3<EnvironmentVariablesDiff, EnvironmentVariablesDiff, Boolean> f2 =
                            (x, y) -> x.equals(y);
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
      Function3<PythonEnvironmentDiff, PythonEnvironmentDiff, Boolean> f = (x, y) -> x.equals(y);
      if (this.Python != null || other.Python != null) {
        if (this.Python == null && other.Python != null) return false;
        if (this.Python != null && other.Python == null) return false;
        if (!f.apply(this.Python, other.Python)) return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.CommandLine, this.Docker, this.EnvironmentVariables, this.Python);
  }

  public EnvironmentDiff setCommandLine(CommandLineEnvironmentDiff value) {
    this.CommandLine = Utils.removeEmpty(value);
    return this;
  }

  public CommandLineEnvironmentDiff getCommandLine() {
    return this.CommandLine;
  }

  public EnvironmentDiff setDocker(DockerEnvironmentDiff value) {
    this.Docker = Utils.removeEmpty(value);
    return this;
  }

  public DockerEnvironmentDiff getDocker() {
    return this.Docker;
  }

  public EnvironmentDiff setEnvironmentVariables(List<EnvironmentVariablesDiff> value) {
    this.EnvironmentVariables = Utils.removeEmpty(value);
    if (this.EnvironmentVariables != null) {
      this.EnvironmentVariables.sort(Comparator.comparingInt(EnvironmentVariablesDiff::hashCode));
    }
    return this;
  }

  public List<EnvironmentVariablesDiff> getEnvironmentVariables() {
    return this.EnvironmentVariables;
  }

  public EnvironmentDiff setPython(PythonEnvironmentDiff value) {
    this.Python = Utils.removeEmpty(value);
    return this;
  }

  public PythonEnvironmentDiff getPython() {
    return this.Python;
  }

  public static EnvironmentDiff fromProto(ai.verta.modeldb.versioning.EnvironmentDiff blob) {
    if (blob == null) {
      return null;
    }

    EnvironmentDiff obj = new EnvironmentDiff();
    {
      Function<ai.verta.modeldb.versioning.EnvironmentDiff, CommandLineEnvironmentDiff> f =
          x -> CommandLineEnvironmentDiff.fromProto(blob.getCommandLine());
      obj.CommandLine = Utils.removeEmpty(f.apply(blob));
    }
    {
      Function<ai.verta.modeldb.versioning.EnvironmentDiff, DockerEnvironmentDiff> f =
          x -> DockerEnvironmentDiff.fromProto(blob.getDocker());
      obj.Docker = Utils.removeEmpty(f.apply(blob));
    }
    {
      Function<ai.verta.modeldb.versioning.EnvironmentDiff, List<EnvironmentVariablesDiff>> f =
          x ->
              blob.getEnvironmentVariablesList().stream()
                  .map(EnvironmentVariablesDiff::fromProto)
                  .collect(Collectors.toList());
      obj.EnvironmentVariables = Utils.removeEmpty(f.apply(blob));
    }
    {
      Function<ai.verta.modeldb.versioning.EnvironmentDiff, PythonEnvironmentDiff> f =
          x -> PythonEnvironmentDiff.fromProto(blob.getPython());
      obj.Python = Utils.removeEmpty(f.apply(blob));
    }
    return obj;
  }

  public ai.verta.modeldb.versioning.EnvironmentDiff.Builder toProto() {
    ai.verta.modeldb.versioning.EnvironmentDiff.Builder builder =
        ai.verta.modeldb.versioning.EnvironmentDiff.newBuilder();
    {
      if (this.CommandLine != null && !this.CommandLine.equals(null)) {
        Function<ai.verta.modeldb.versioning.EnvironmentDiff.Builder, Void> f =
            x -> {
              builder.setCommandLine(this.CommandLine.toProto());
              return null;
            };
        f.apply(builder);
      }
    }
    {
      if (this.Docker != null && !this.Docker.equals(null)) {
        Function<ai.verta.modeldb.versioning.EnvironmentDiff.Builder, Void> f =
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
        Function<ai.verta.modeldb.versioning.EnvironmentDiff.Builder, Void> f =
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
        Function<ai.verta.modeldb.versioning.EnvironmentDiff.Builder, Void> f =
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
    visitor.preVisitEnvironmentDiff(this);
  }

  public void preVisitDeep(Visitor visitor) throws ModelDBException {
    this.preVisitShallow(visitor);
    visitor.preVisitDeepCommandLineEnvironmentDiff(this.CommandLine);
    visitor.preVisitDeepDockerEnvironmentDiff(this.Docker);
    visitor.preVisitDeepListOfEnvironmentVariablesDiff(this.EnvironmentVariables);
    visitor.preVisitDeepPythonEnvironmentDiff(this.Python);
  }

  public EnvironmentDiff postVisitShallow(Visitor visitor) throws ModelDBException {
    return visitor.postVisitEnvironmentDiff(this);
  }

  public EnvironmentDiff postVisitDeep(Visitor visitor) throws ModelDBException {
    this.setCommandLine(visitor.postVisitDeepCommandLineEnvironmentDiff(this.CommandLine));
    this.setDocker(visitor.postVisitDeepDockerEnvironmentDiff(this.Docker));
    this.setEnvironmentVariables(
        visitor.postVisitDeepListOfEnvironmentVariablesDiff(this.EnvironmentVariables));
    this.setPython(visitor.postVisitDeepPythonEnvironmentDiff(this.Python));
    return this.postVisitShallow(visitor);
  }
}
