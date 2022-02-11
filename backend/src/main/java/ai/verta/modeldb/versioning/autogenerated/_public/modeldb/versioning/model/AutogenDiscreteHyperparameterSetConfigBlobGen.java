// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model;

import ai.verta.modeldb.versioning.*;
import ai.verta.modeldb.versioning.blob.diff.*;
import com.pholser.junit.quickcheck.generator.*;
import com.pholser.junit.quickcheck.generator.java.util.*;
import com.pholser.junit.quickcheck.random.*;
import java.util.*;

public class AutogenDiscreteHyperparameterSetConfigBlobGen
    extends Generator<AutogenDiscreteHyperparameterSetConfigBlob> {

  public AutogenDiscreteHyperparameterSetConfigBlobGen() {
    super(AutogenDiscreteHyperparameterSetConfigBlob.class);
  }

  @Override
  public AutogenDiscreteHyperparameterSetConfigBlob generate(
      SourceOfRandomness r, GenerationStatus status) {
    // if (r.nextBoolean())
    //     return null;

    AutogenDiscreteHyperparameterSetConfigBlob obj =
        new AutogenDiscreteHyperparameterSetConfigBlob();
    if (r.nextBoolean()) {
      int size = r.nextInt(0, 10);
      List<AutogenHyperparameterValuesConfigBlob> ret = new ArrayList(size);
      for (int i = 0; i < size; i++) {
        ret.add(gen().type(AutogenHyperparameterValuesConfigBlob.class).generate(r, status));
      }
      obj.setValues(Utils.removeEmpty(ret));
    }
    return obj;
  }
}
