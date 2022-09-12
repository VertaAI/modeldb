package ai.verta.modeldb;

import ai.verta.modeldb.ArtifactStore.NFSArtifactStoreTest;
import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@ExcludeClassNamePatterns({
  "ai.verta.modeldb.metadata.MetadataTest",
  "ai.verta.modeldb.ArtifactStoreTest",
  "ai.verta.modeldb.BranchTest",
  "ai.verta.modeldb.CommentTest",
  "ai.verta.modeldb.CommitTest",
  "ai.verta.modeldb.DatasetTest",
  "ai.verta.modeldb.DatasetVersionTest",
  "ai.verta.modeldb.DiffTest",
  "ai.verta.modeldb.ExperimentRunTest",
  "ai.verta.modeldb.ExperimentTest",
  "ai.verta.modeldb.FindDatasetEntitiesTest",
  "ai.verta.modeldb.FindHydratedServiceTest",
  "ai.verta.modeldb.FindProjectEntitiesTest",
  "ai.verta.modeldb.GlobalSharingTest",
  "ai.verta.modeldb.HydratedServiceTest",
  "ai.verta.modeldb.IntegrationTest",
  "ai.verta.modeldb.LineageTest",
  "ai.verta.modeldb.MergeTest",
  "ai.verta.modeldb.ProjectTest",
  "ai.verta.modeldb.RepositoryTest",
  ".*TestSequenceSuite",
})
@SelectPackages({"ai.verta.modeldb"})
@SelectClasses({NFSArtifactStoreTest.class})
public class PullRequestSuite {}
