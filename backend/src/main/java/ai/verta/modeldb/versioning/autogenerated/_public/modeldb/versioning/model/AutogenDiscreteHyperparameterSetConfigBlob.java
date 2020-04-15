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

public class AutogenDiscreteHyperparameterSetConfigBlob implements ProtoType {
    private List<AutogenHyperparameterValuesConfigBlob
>
 Values;

    public AutogenDiscreteHyperparameterSetConfigBlob() {
        this.Values = null;
    }

    public Boolean isEmpty() {
        if (this.Values != null && !this.Values.equals(null)  && !this.Values.isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"class\": \"AutogenDiscreteHyperparameterSetConfigBlob\", \"fields\": {");
        boolean first = true;
        if (this.Values != null && !this.Values.equals(null)  && !this.Values.isEmpty()) {
            if (!first) sb.append(", ");
            sb.append("\"Values\": " + Values);
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
        if (!(o instanceof AutogenDiscreteHyperparameterSetConfigBlob)) return false;
        AutogenDiscreteHyperparameterSetConfigBlob other = (AutogenDiscreteHyperparameterSetConfigBlob) o;

        {
            Function3<List<AutogenHyperparameterValuesConfigBlob
>
,List<AutogenHyperparameterValuesConfigBlob
>
,Boolean> f = (x2, y2) -> IntStream.range(0, Math.min(x2.size(), y2.size())).mapToObj(i -> { Function3<AutogenHyperparameterValuesConfigBlob
,AutogenHyperparameterValuesConfigBlob
,Boolean> f2 = (x, y) -> x.equals(y); return f2.apply(x2.get(i), y2.get(i));}).filter(x -> x.equals(false)).collect(Collectors.toList()).isEmpty();
            if (this.Values != null || other.Values != null) {
                if (this.Values == null && other.Values != null)
                    return false;
                if (this.Values != null && other.Values == null)
                    return false;
                if (!f.apply(this.Values, other.Values))
                    return false;
            }
        }
        return true;
    }

    public AutogenDiscreteHyperparameterSetConfigBlob setValues(List<AutogenHyperparameterValuesConfigBlob
>
 value) {
        this.Values = Utils.removeEmpty(value);
        if (this.Values != null) {
            this.Values.sort(Comparator.comparingInt(AutogenHyperparameterValuesConfigBlob
::hashCode));
        }
        return this;
    }
    public List<AutogenHyperparameterValuesConfigBlob
>
 getValues() {
        return this.Values;
    }

    static public AutogenDiscreteHyperparameterSetConfigBlob fromProto(ai.verta.modeldb.versioning.DiscreteHyperparameterSetConfigBlob blob) {
        if (blob == null) {
            return null;
        }

        AutogenDiscreteHyperparameterSetConfigBlob obj = new AutogenDiscreteHyperparameterSetConfigBlob();
        {
            Function<ai.verta.modeldb.versioning.DiscreteHyperparameterSetConfigBlob,List<AutogenHyperparameterValuesConfigBlob
>
> f = x -> blob.getValuesList().stream().map(AutogenHyperparameterValuesConfigBlob::fromProto).collect(Collectors.toList());
            obj.setValues(f.apply(blob));
        }
        return obj;
    }

    public ai.verta.modeldb.versioning.DiscreteHyperparameterSetConfigBlob.Builder toProto() {
        ai.verta.modeldb.versioning.DiscreteHyperparameterSetConfigBlob.Builder builder = ai.verta.modeldb.versioning.DiscreteHyperparameterSetConfigBlob.newBuilder();
        {
            if (this.Values != null && !this.Values.equals(null)  && !this.Values.isEmpty()) {
                Function<ai.verta.modeldb.versioning.DiscreteHyperparameterSetConfigBlob.Builder,Void> f = x -> { builder.addAllValues(this.Values.stream().map(y -> y.toProto().build()).collect(Collectors.toList())); return null; };
                f.apply(builder);
            }
        }
        return builder;
    }

    public void preVisitShallow(Visitor visitor) throws ModelDBException {
        visitor.preVisitAutogenDiscreteHyperparameterSetConfigBlob(this);
    }

    public void preVisitDeep(Visitor visitor) throws ModelDBException {
        this.preVisitShallow(visitor);
        visitor.preVisitDeepListOfAutogenHyperparameterValuesConfigBlob

(this.Values);
    }

    public AutogenDiscreteHyperparameterSetConfigBlob postVisitShallow(Visitor visitor) throws ModelDBException {
        return visitor.postVisitAutogenDiscreteHyperparameterSetConfigBlob(this);
    }

    public AutogenDiscreteHyperparameterSetConfigBlob postVisitDeep(Visitor visitor) throws ModelDBException {
        this.setValues(visitor.postVisitDeepListOfAutogenHyperparameterValuesConfigBlob

(this.Values));
        return this.postVisitShallow(visitor);
    }
}
