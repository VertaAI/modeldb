package ai.verta.modeldb;

import ai.verta.modeldb.lineage.LineageServiceImplNegativeTest;
import ai.verta.modeldb.metadata.MetadataTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  JobTest.class,
  ProjectTest.class,
  ExperimentTest.class,
  ExperimentRunTest.class,
  CommentTest.class,
  DatasetTest.class,
  DatasetVersionTest.class,
  HydratedServiceTest.class,
  LineageTest.class,
  LineageServiceImplNegativeTest.class,
  FindProjectEntitiesTest.class,
  FindDatasetEntitiesTest.class,
  RepositoryTest.class,
  CommitTest.class,
  DiffTest.class,
  MetadataTest.class
})
public class PublicTestSequenceSuite {}
