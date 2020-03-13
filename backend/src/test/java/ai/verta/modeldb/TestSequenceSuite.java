package ai.verta.modeldb;

import ai.verta.modeldb.lineage.LineageServiceImplNegativeTest;
import ai.verta.modeldb.metadata.MetadataTest;
import ai.verta.modeldb.utils.ModelDBUtilsTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  JobTest.class,
  ProjectTest.class,
  ExperimentTest.class,
  ExperimentRunTest.class,
  CollaboratorTest.class,
  CommentTest.class,
  HydratedServiceTest.class,
  HydratedServiceOrgTeamTest.class,
  DatasetTest.class,
  DatasetVersionTest.class,
  ModelDBUtilsTest.class,
  LineageTest.class,
  LineageServiceImplNegativeTest.class,
  FindProjectEntitiesTest.class,
  FindDatasetEntitiesTest.class,
  RepositoryTest.class,
  CommitTest.class,
  MetadataTest.class,
  DiffTest.class
  //  ArtifactStoreTest.class
})
public class TestSequenceSuite {}
