// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model;

import ai.verta.modeldb.versioning.*;
import ai.verta.modeldb.versioning.blob.diff.*;
import com.pholser.junit.quickcheck.generator.*;
import com.pholser.junit.quickcheck.generator.java.util.*;
import com.pholser.junit.quickcheck.random.*;
import java.util.*;

public class AutogenDatasetBlobGen extends Generator<AutogenDatasetBlob> {
  public AutogenDatasetBlobGen() {
    super(AutogenDatasetBlob.class);
  }

  @Override
  public AutogenDatasetBlob generate(SourceOfRandomness r, GenerationStatus status) {
    // if (r.nextBoolean())
    //     return null;

    AutogenDatasetBlob obj = new AutogenDatasetBlob();
    if (r.nextBoolean()) {
      obj.setPath(Utils.removeEmpty(gen().type(AutogenPathDatasetBlob.class).generate(r, status)));
    }
    if (r.nextBoolean()) {
      obj.setS3(Utils.removeEmpty(gen().type(AutogenS3DatasetBlob.class).generate(r, status)));
    }
    return obj;
  }
}
