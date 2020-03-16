package ai.verta.modeldb;

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
  FindProjectEntitiesTest.class,
  FindDatasetEntitiesTest.class
  //  ArtifactStoreTest.class
})
public class TestSequenceSuite {}
