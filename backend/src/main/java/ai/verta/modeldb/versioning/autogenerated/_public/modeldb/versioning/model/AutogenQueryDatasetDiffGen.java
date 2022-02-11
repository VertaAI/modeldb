// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model;

import ai.verta.modeldb.versioning.*;
import ai.verta.modeldb.versioning.blob.diff.*;
import com.pholser.junit.quickcheck.generator.*;
import com.pholser.junit.quickcheck.generator.java.util.*;
import com.pholser.junit.quickcheck.random.*;
import java.util.*;

public class AutogenQueryDatasetDiffGen extends Generator<AutogenQueryDatasetDiff> {

  public AutogenQueryDatasetDiffGen() {
    super(AutogenQueryDatasetDiff.class);
  }

  @Override
  public AutogenQueryDatasetDiff generate(SourceOfRandomness r, GenerationStatus status) {
    // if (r.nextBoolean())
    //     return null;

    AutogenQueryDatasetDiff obj = new AutogenQueryDatasetDiff();
    if (r.nextBoolean()) {
      int size = r.nextInt(0, 10);
      List<AutogenQueryDatasetComponentDiff> ret = new ArrayList(size);
      for (int i = 0; i < size; i++) {
        ret.add(gen().type(AutogenQueryDatasetComponentDiff.class).generate(r, status));
      }
      obj.setComponents(Utils.removeEmpty(ret));
    }
    return obj;
  }
}
