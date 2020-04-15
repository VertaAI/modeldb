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
import com.pholser.junit.quickcheck.generator.java.util.*;
import com.pholser.junit.quickcheck.random.*;

public class AutogenBlobDiffGen extends Generator<AutogenBlobDiff> {
    public AutogenBlobDiffGen() {
        super(AutogenBlobDiff.class);
    }

    @Override public AutogenBlobDiff generate(
            SourceOfRandomness r,
            GenerationStatus status) {
                // if (r.nextBoolean())
                //     return null;

                AutogenBlobDiff obj = new AutogenBlobDiff();
                if (r.nextBoolean()) {
                    obj.setCode(Utils.removeEmpty(gen().type(AutogenCodeDiff
.class).generate(r, status)));
                }
                if (r.nextBoolean()) {
                    obj.setConfig(Utils.removeEmpty(gen().type(AutogenConfigDiff
.class).generate(r, status)));
                }
                if (r.nextBoolean()) {
                    obj.setDataset(Utils.removeEmpty(gen().type(AutogenDatasetDiff
.class).generate(r, status)));
                }
                if (r.nextBoolean()) {
                    obj.setEnvironment(Utils.removeEmpty(gen().type(AutogenEnvironmentDiff
.class).generate(r, status)));
                }
                if (r.nextBoolean()) {
                    int size = r.nextInt(0, 10);
                    List<String
> ret = new ArrayList(size);
                    for (int i = 0; i < size; i++) {
                        ret.add(new StringGenerator().generate(r, status));
                    }
                    obj.setLocation(Utils.removeEmpty(ret));
                }
                if (r.nextBoolean()) {
                    obj.setStatus(Utils.removeEmpty(gen().type(AutogenDiffStatusEnumDiffStatus
.class).generate(r, status)));
                }
                return obj;
    }
}
