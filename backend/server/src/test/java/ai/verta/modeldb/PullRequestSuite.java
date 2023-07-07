package ai.verta.modeldb;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
// @ExcludeClassNamePatterns({"ai.verta.modeldb.metadata.MetadataTest"})
@SelectClasses({ai.verta.modeldb.ExperimentRunTest.class})
// @SelectPackages({"ai.verta.modeldb"})
public class PullRequestSuite {}
