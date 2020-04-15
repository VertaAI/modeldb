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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Hex;

public class AutogenConfigDiff implements ProtoType {
    private List<AutogenHyperparameterSetConfigDiff
>
 HyperparameterSet;
    private List<AutogenHyperparameterConfigDiff
>
 Hyperparameters;

    public AutogenConfigDiff() {
        this.HyperparameterSet = null;
        this.Hyperparameters = null;
    }

    public Boolean isEmpty() {
        if (this.HyperparameterSet != null && !this.HyperparameterSet.equals(null)  && !this.HyperparameterSet.isEmpty()) {
            return false;
        }
        if (this.Hyperparameters != null && !this.Hyperparameters.equals(null)  && !this.Hyperparameters.isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"class\": \"AutogenConfigDiff\", \"fields\": {");
        boolean first = true;
        if (this.HyperparameterSet != null && !this.HyperparameterSet.equals(null)  && !this.HyperparameterSet.isEmpty()) {
            if (!first) sb.append(", ");
            sb.append("\"HyperparameterSet\": " + HyperparameterSet);
            first = false;
        }
        if (this.Hyperparameters != null && !this.Hyperparameters.equals(null)  && !this.Hyperparameters.isEmpty()) {
            if (!first) sb.append(", ");
            sb.append("\"Hyperparameters\": " + Hyperparameters);
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
        if (!(o instanceof AutogenConfigDiff)) return false;
        AutogenConfigDiff other = (AutogenConfigDiff) o;

        {
            Function3<List<AutogenHyperparameterSetConfigDiff
>
,List<AutogenHyperparameterSetConfigDiff
>
,Boolean> f = (x2, y2) -> IntStream.range(0, Math.min(x2.size(), y2.size())).mapToObj(i -> { Function3<AutogenHyperparameterSetConfigDiff
,AutogenHyperparameterSetConfigDiff
,Boolean> f2 = (x, y) -> x.equals(y); return f2.apply(x2.get(i), y2.get(i));}).filter(x -> x.equals(false)).collect(Collectors.toList()).isEmpty();
            if (this.HyperparameterSet != null || other.HyperparameterSet != null) {
                if (this.HyperparameterSet == null && other.HyperparameterSet != null)
                    return false;
                if (this.HyperparameterSet != null && other.HyperparameterSet == null)
                    return false;
                if (!f.apply(this.HyperparameterSet, other.HyperparameterSet))
                    return false;
            }
        }
        {
            Function3<List<AutogenHyperparameterConfigDiff
>
,List<AutogenHyperparameterConfigDiff
>
,Boolean> f = (x2, y2) -> IntStream.range(0, Math.min(x2.size(), y2.size())).mapToObj(i -> { Function3<AutogenHyperparameterConfigDiff
,AutogenHyperparameterConfigDiff
,Boolean> f2 = (x, y) -> x.equals(y); return f2.apply(x2.get(i), y2.get(i));}).filter(x -> x.equals(false)).collect(Collectors.toList()).isEmpty();
            if (this.Hyperparameters != null || other.Hyperparameters != null) {
                if (this.Hyperparameters == null && other.Hyperparameters != null)
                    return false;
                if (this.Hyperparameters != null && other.Hyperparameters == null)
                    return false;
                if (!f.apply(this.Hyperparameters, other.Hyperparameters))
                    return false;
            }
        }
        return true;
    }

    public AutogenConfigDiff setHyperparameterSet(List<AutogenHyperparameterSetConfigDiff
>
 value) {
        this.HyperparameterSet = Utils.removeEmpty(value);
        if (this.HyperparameterSet != null) {
            this.HyperparameterSet.sort(Comparator.comparingInt(AutogenHyperparameterSetConfigDiff
::hashCode));
        }
        return this;
    }
    public List<AutogenHyperparameterSetConfigDiff
>
 getHyperparameterSet() {
        return this.HyperparameterSet;
    }
    public AutogenConfigDiff setHyperparameters(List<AutogenHyperparameterConfigDiff
>
 value) {
        this.Hyperparameters = Utils.removeEmpty(value);
        if (this.Hyperparameters != null) {
            this.Hyperparameters.sort(Comparator.comparingInt(AutogenHyperparameterConfigDiff
::hashCode));
        }
        return this;
    }
    public List<AutogenHyperparameterConfigDiff
>
 getHyperparameters() {
        return this.Hyperparameters;
    }

    static public AutogenConfigDiff fromProto(ai.verta.modeldb.versioning.ConfigDiff blob) {
        if (blob == null) {
            return null;
        }

        AutogenConfigDiff obj = new AutogenConfigDiff();
        {
            Function<ai.verta.modeldb.versioning.ConfigDiff,List<AutogenHyperparameterSetConfigDiff
>
> f = x -> blob.getHyperparameterSetList().stream().map(AutogenHyperparameterSetConfigDiff::fromProto).collect(Collectors.toList());
            obj.setHyperparameterSet(f.apply(blob));
        }
        {
            Function<ai.verta.modeldb.versioning.ConfigDiff,List<AutogenHyperparameterConfigDiff
>
> f = x -> blob.getHyperparametersList().stream().map(AutogenHyperparameterConfigDiff::fromProto).collect(Collectors.toList());
            obj.setHyperparameters(f.apply(blob));
        }
        return obj;
    }

    public ai.verta.modeldb.versioning.ConfigDiff.Builder toProto() {
        ai.verta.modeldb.versioning.ConfigDiff.Builder builder = ai.verta.modeldb.versioning.ConfigDiff.newBuilder();
        {
            if (this.HyperparameterSet != null && !this.HyperparameterSet.equals(null)  && !this.HyperparameterSet.isEmpty()) {
                Function<ai.verta.modeldb.versioning.ConfigDiff.Builder,Void> f = x -> { builder.addAllHyperparameterSet(this.HyperparameterSet.stream().map(y -> y.toProto().build()).collect(Collectors.toList())); return null; };
                f.apply(builder);
            }
        }
        {
            if (this.Hyperparameters != null && !this.Hyperparameters.equals(null)  && !this.Hyperparameters.isEmpty()) {
                Function<ai.verta.modeldb.versioning.ConfigDiff.Builder,Void> f = x -> { builder.addAllHyperparameters(this.Hyperparameters.stream().map(y -> y.toProto().build()).collect(Collectors.toList())); return null; };
                f.apply(builder);
            }
        }
        return builder;
    }

    public void preVisitShallow(Visitor visitor) throws ModelDBException {
        visitor.preVisitAutogenConfigDiff(this);
    }

    public void preVisitDeep(Visitor visitor) throws ModelDBException {
        this.preVisitShallow(visitor);
        visitor.preVisitDeepListOfAutogenHyperparameterSetConfigDiff

(this.HyperparameterSet);
        visitor.preVisitDeepListOfAutogenHyperparameterConfigDiff

(this.Hyperparameters);
    }

    public AutogenConfigDiff postVisitShallow(Visitor visitor) throws ModelDBException {
        return visitor.postVisitAutogenConfigDiff(this);
    }

    public AutogenConfigDiff postVisitDeep(Visitor visitor) throws ModelDBException {
        this.setHyperparameterSet(visitor.postVisitDeepListOfAutogenHyperparameterSetConfigDiff

(this.HyperparameterSet));
        this.setHyperparameters(visitor.postVisitDeepListOfAutogenHyperparameterConfigDiff

(this.Hyperparameters));
        return this.postVisitShallow(visitor);
    }
}
