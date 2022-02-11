// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model;

import ai.verta.modeldb.versioning.*;
import ai.verta.modeldb.versioning.blob.diff.*;
import com.pholser.junit.quickcheck.generator.*;
import com.pholser.junit.quickcheck.generator.java.util.*;
import com.pholser.junit.quickcheck.random.*;
import java.util.*;

public class AutogenNotebookCodeBlobGen extends Generator<AutogenNotebookCodeBlob> {

  public AutogenNotebookCodeBlobGen() {
    super(AutogenNotebookCodeBlob.class);
  }

  @Override
  public AutogenNotebookCodeBlob generate(SourceOfRandomness r, GenerationStatus status) {
    // if (r.nextBoolean())
    //     return null;

    AutogenNotebookCodeBlob obj = new AutogenNotebookCodeBlob();
    if (r.nextBoolean()) {
      obj.setGitRepo(Utils.removeEmpty(gen().type(AutogenGitCodeBlob.class).generate(r, status)));
    }
    if (r.nextBoolean()) {
      obj.setPath(
          Utils.removeEmpty(gen().type(AutogenPathDatasetComponentBlob.class).generate(r, status)));
    }
    return obj;
  }
}
