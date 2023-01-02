package ai.verta.modeldb;

import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@ExcludeClassNamePatterns({
  "ai.verta.modeldb.metadata.MetadataTest",
  "ai.verta.modeldb.BranchTest",
  ".*TestSequenceSuite",
})
@SelectPackages({"ai.verta.modeldb"})
public class PullRequestSuite {}
