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

public class AutogenContinuousHyperparameterSetConfigBlob implements ProtoType {
  private AutogenHyperparameterValuesConfigBlob IntervalBegin;
  private AutogenHyperparameterValuesConfigBlob IntervalEnd;
  private AutogenHyperparameterValuesConfigBlob IntervalStep;

  public AutogenContinuousHyperparameterSetConfigBlob() {
    this.IntervalBegin = null;
    this.IntervalEnd = null;
    this.IntervalStep = null;
  }

  public Boolean isEmpty() {
    if (this.IntervalBegin != null && !this.IntervalBegin.equals(null)) {
      return false;
    }
    if (this.IntervalEnd != null && !this.IntervalEnd.equals(null)) {
      return false;
    }
    if (this.IntervalStep != null && !this.IntervalStep.equals(null)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"class\": \"AutogenContinuousHyperparameterSetConfigBlob\", \"fields\": {");
    boolean first = true;
    if (this.IntervalBegin != null && !this.IntervalBegin.equals(null)) {
      if (!first) sb.append(", ");
      sb.append("\"IntervalBegin\": " + IntervalBegin);
      first = false;
    }
    if (this.IntervalEnd != null && !this.IntervalEnd.equals(null)) {
      if (!first) sb.append(", ");
      sb.append("\"IntervalEnd\": " + IntervalEnd);
      first = false;
    }
    if (this.IntervalStep != null && !this.IntervalStep.equals(null)) {
      if (!first) sb.append(", ");
      sb.append("\"IntervalStep\": " + IntervalStep);
      first = false;
    }
    sb.append("}}");
    return sb.toString();
  }

  // TODO: actually hash
  public String getSHA() {
    StringBuilder sb = new StringBuilder();
    sb.append("AutogenContinuousHyperparameterSetConfigBlob");
    if (this.IntervalBegin != null && !this.IntervalBegin.equals(null)) {
      sb.append("::IntervalBegin::").append(IntervalBegin);
    }
    if (this.IntervalEnd != null && !this.IntervalEnd.equals(null)) {
      sb.append("::IntervalEnd::").append(IntervalEnd);
    }
    if (this.IntervalStep != null && !this.IntervalStep.equals(null)) {
      sb.append("::IntervalStep::").append(IntervalStep);
    }

    return sb.toString();
  }

  // TODO: not consider order on lists
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (!(o instanceof AutogenContinuousHyperparameterSetConfigBlob)) return false;
    AutogenContinuousHyperparameterSetConfigBlob other =
        (AutogenContinuousHyperparameterSetConfigBlob) o;

    {
      Function3<
              AutogenHyperparameterValuesConfigBlob, AutogenHyperparameterValuesConfigBlob, Boolean>
          f = (x, y) -> x.equals(y);
      if (this.IntervalBegin != null || other.IntervalBegin != null) {
        if (this.IntervalBegin == null && other.IntervalBegin != null) return false;
        if (this.IntervalBegin != null && other.IntervalBegin == null) return false;
        if (!f.apply(this.IntervalBegin, other.IntervalBegin)) return false;
      }
    }
    {
      Function3<
              AutogenHyperparameterValuesConfigBlob, AutogenHyperparameterValuesConfigBlob, Boolean>
          f = (x, y) -> x.equals(y);
      if (this.IntervalEnd != null || other.IntervalEnd != null) {
        if (this.IntervalEnd == null && other.IntervalEnd != null) return false;
        if (this.IntervalEnd != null && other.IntervalEnd == null) return false;
        if (!f.apply(this.IntervalEnd, other.IntervalEnd)) return false;
      }
    }
    {
      Function3<
              AutogenHyperparameterValuesConfigBlob, AutogenHyperparameterValuesConfigBlob, Boolean>
          f = (x, y) -> x.equals(y);
      if (this.IntervalStep != null || other.IntervalStep != null) {
        if (this.IntervalStep == null && other.IntervalStep != null) return false;
        if (this.IntervalStep != null && other.IntervalStep == null) return false;
        if (!f.apply(this.IntervalStep, other.IntervalStep)) return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.IntervalBegin, this.IntervalEnd, this.IntervalStep);
  }

  public AutogenContinuousHyperparameterSetConfigBlob setIntervalBegin(
      AutogenHyperparameterValuesConfigBlob value) {
    this.IntervalBegin = Utils.removeEmpty(value);
    return this;
  }

  public AutogenHyperparameterValuesConfigBlob getIntervalBegin() {
    return this.IntervalBegin;
  }

  public AutogenContinuousHyperparameterSetConfigBlob setIntervalEnd(
      AutogenHyperparameterValuesConfigBlob value) {
    this.IntervalEnd = Utils.removeEmpty(value);
    return this;
  }

  public AutogenHyperparameterValuesConfigBlob getIntervalEnd() {
    return this.IntervalEnd;
  }

  public AutogenContinuousHyperparameterSetConfigBlob setIntervalStep(
      AutogenHyperparameterValuesConfigBlob value) {
    this.IntervalStep = Utils.removeEmpty(value);
    return this;
  }

  public AutogenHyperparameterValuesConfigBlob getIntervalStep() {
    return this.IntervalStep;
  }

  public static AutogenContinuousHyperparameterSetConfigBlob fromProto(
      ai.verta.modeldb.versioning.ContinuousHyperparameterSetConfigBlob blob) {
    if (blob == null) {
      return null;
    }

    AutogenContinuousHyperparameterSetConfigBlob obj =
        new AutogenContinuousHyperparameterSetConfigBlob();
    {
      Function<
              ai.verta.modeldb.versioning.ContinuousHyperparameterSetConfigBlob,
              AutogenHyperparameterValuesConfigBlob>
          f = x -> AutogenHyperparameterValuesConfigBlob.fromProto(blob.getIntervalBegin());
      obj.setIntervalBegin(f.apply(blob));
    }
    {
      Function<
              ai.verta.modeldb.versioning.ContinuousHyperparameterSetConfigBlob,
              AutogenHyperparameterValuesConfigBlob>
          f = x -> AutogenHyperparameterValuesConfigBlob.fromProto(blob.getIntervalEnd());
      obj.setIntervalEnd(f.apply(blob));
    }
    {
      Function<
              ai.verta.modeldb.versioning.ContinuousHyperparameterSetConfigBlob,
              AutogenHyperparameterValuesConfigBlob>
          f = x -> AutogenHyperparameterValuesConfigBlob.fromProto(blob.getIntervalStep());
      obj.setIntervalStep(f.apply(blob));
    }
    return obj;
  }

  public ai.verta.modeldb.versioning.ContinuousHyperparameterSetConfigBlob.Builder toProto() {
    ai.verta.modeldb.versioning.ContinuousHyperparameterSetConfigBlob.Builder builder =
        ai.verta.modeldb.versioning.ContinuousHyperparameterSetConfigBlob.newBuilder();
    {
      if (this.IntervalBegin != null && !this.IntervalBegin.equals(null)) {
        Function<ai.verta.modeldb.versioning.ContinuousHyperparameterSetConfigBlob.Builder, Void>
            f =
                x -> {
                  builder.setIntervalBegin(this.IntervalBegin.toProto());
                  return null;
                };
        f.apply(builder);
      }
    }
    {
      if (this.IntervalEnd != null && !this.IntervalEnd.equals(null)) {
        Function<ai.verta.modeldb.versioning.ContinuousHyperparameterSetConfigBlob.Builder, Void>
            f =
                x -> {
                  builder.setIntervalEnd(this.IntervalEnd.toProto());
                  return null;
                };
        f.apply(builder);
      }
    }
    {
      if (this.IntervalStep != null && !this.IntervalStep.equals(null)) {
        Function<ai.verta.modeldb.versioning.ContinuousHyperparameterSetConfigBlob.Builder, Void>
            f =
                x -> {
                  builder.setIntervalStep(this.IntervalStep.toProto());
                  return null;
                };
        f.apply(builder);
      }
    }
    return builder;
  }

  public void preVisitShallow(Visitor visitor) throws ModelDBException {
    visitor.preVisitAutogenContinuousHyperparameterSetConfigBlob(this);
  }

  public void preVisitDeep(Visitor visitor) throws ModelDBException {
    this.preVisitShallow(visitor);
    visitor.preVisitDeepAutogenHyperparameterValuesConfigBlob(this.IntervalBegin);
    visitor.preVisitDeepAutogenHyperparameterValuesConfigBlob(this.IntervalEnd);
    visitor.preVisitDeepAutogenHyperparameterValuesConfigBlob(this.IntervalStep);
  }

  public AutogenContinuousHyperparameterSetConfigBlob postVisitShallow(Visitor visitor)
      throws ModelDBException {
    return visitor.postVisitAutogenContinuousHyperparameterSetConfigBlob(this);
  }

  public AutogenContinuousHyperparameterSetConfigBlob postVisitDeep(Visitor visitor)
      throws ModelDBException {
    this.setIntervalBegin(
        visitor.postVisitDeepAutogenHyperparameterValuesConfigBlob(this.IntervalBegin));
    this.setIntervalEnd(
        visitor.postVisitDeepAutogenHyperparameterValuesConfigBlob(this.IntervalEnd));
    this.setIntervalStep(
        visitor.postVisitDeepAutogenHyperparameterValuesConfigBlob(this.IntervalStep));
    return this.postVisitShallow(visitor);
  }
}
