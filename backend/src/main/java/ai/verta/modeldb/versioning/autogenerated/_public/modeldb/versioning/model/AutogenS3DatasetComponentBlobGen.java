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

public class AutogenS3DatasetComponentBlobGen extends Generator<AutogenS3DatasetComponentBlob> {
  public AutogenS3DatasetComponentBlobGen() {
    super(AutogenS3DatasetComponentBlob.class);
  }

  @Override
  public AutogenS3DatasetComponentBlob generate(SourceOfRandomness r, GenerationStatus status) {
    // if (r.nextBoolean())
    //     return null;

    AutogenS3DatasetComponentBlob obj = new AutogenS3DatasetComponentBlob();
    if (r.nextBoolean()) {
      obj.setPath(
          Utils.removeEmpty(gen().type(AutogenPathDatasetComponentBlob.class).generate(r, status)));
    }
    return obj;
  }
}
