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

public class AutogenHyperparameterConfigDiffGen extends Generator<AutogenHyperparameterConfigDiff> {
  public AutogenHyperparameterConfigDiffGen() {
    super(AutogenHyperparameterConfigDiff.class);
  }

  @Override
  public AutogenHyperparameterConfigDiff generate(SourceOfRandomness r, GenerationStatus status) {
    // if (r.nextBoolean())
    //     return null;

    AutogenHyperparameterConfigDiff obj = new AutogenHyperparameterConfigDiff();
    if (r.nextBoolean()) {
      obj.setA(
          Utils.removeEmpty(gen().type(AutogenHyperparameterConfigBlob.class).generate(r, status)));
    }
    if (r.nextBoolean()) {
      obj.setB(
          Utils.removeEmpty(gen().type(AutogenHyperparameterConfigBlob.class).generate(r, status)));
    }
    if (r.nextBoolean()) {
      obj.setStatus(
          Utils.removeEmpty(gen().type(AutogenDiffStatusEnumDiffStatus.class).generate(r, status)));
    }
    return obj;
  }
}
