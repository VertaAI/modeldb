// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model;

import ai.verta.modeldb.versioning.*;
import ai.verta.modeldb.versioning.blob.diff.*;
import com.pholser.junit.quickcheck.generator.*;
import com.pholser.junit.quickcheck.generator.java.util.*;
import com.pholser.junit.quickcheck.random.*;
import java.util.*;

public class AutogenDiffStatusEnumDiffStatusGen extends Generator<AutogenDiffStatusEnumDiffStatus> {

  public AutogenDiffStatusEnumDiffStatusGen() {
    super(AutogenDiffStatusEnumDiffStatus.class);
  }

  @Override
  public AutogenDiffStatusEnumDiffStatus generate(SourceOfRandomness r, GenerationStatus status) {
    return new AutogenDiffStatusEnumDiffStatus(
        r.choose(
            new DiffStatusEnum.DiffStatus[]{
                DiffStatusEnum.DiffStatus.ADDED,
                DiffStatusEnum.DiffStatus.DELETED,
                DiffStatusEnum.DiffStatus.MODIFIED
            }));
  }
}
