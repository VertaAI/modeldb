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

public class AutogenDatasetDiffGen extends Generator<AutogenDatasetDiff> {
    public AutogenDatasetDiffGen() {
        super(AutogenDatasetDiff.class);
    }

    @Override public AutogenDatasetDiff generate(
            SourceOfRandomness r,
            GenerationStatus status) {
                // if (r.nextBoolean())
                //     return null;

                AutogenDatasetDiff obj = new AutogenDatasetDiff();
                if (r.nextBoolean()) {
                    obj.setPath(Utils.removeEmpty(gen().type(AutogenPathDatasetDiff
.class).generate(r, status)));
                }
                if (r.nextBoolean()) {
                    obj.setS3(Utils.removeEmpty(gen().type(AutogenS3DatasetDiff
.class).generate(r, status)));
                }
                return obj;
    }
}
