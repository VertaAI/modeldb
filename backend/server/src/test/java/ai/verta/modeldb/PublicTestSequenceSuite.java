package ai.verta.modeldb;

import ai.verta.modeldb.blobs.BlobEquality;
import ai.verta.modeldb.blobs.BlobProtoEquality;
import ai.verta.modeldb.blobs.DiffAndMerge;
import ai.verta.modeldb.lineage.LineageServiceImplNegativeTest;
import ai.verta.modeldb.metadata.MetadataTest;
import ai.verta.modeldb.versioning.blob.visitors.ValidatorBlobDiffTest;
import ai.verta.modeldb.versioning.blob.visitors.ValidatorBlobTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  ProjectTest.class,
  ExperimentTest.class,
  ExperimentRunTest.class,
  CommentTest.class,
  DatasetTest.class,
  DatasetVersionTest.class,
  LineageTest.class,
  LineageServiceImplNegativeTest.class,
  FindProjectEntitiesTest.class,
  FindDatasetEntitiesTest.class,
  RepositoryTest.class,
  CommitTest.class,
  DiffTest.class,
  MetadataTest.class,
  BlobEquality.class,
  BlobProtoEquality.class,
  DiffAndMerge.class,
  ValidatorBlobTest.class,
  ValidatorBlobDiffTest.class
})
public class PublicTestSequenceSuite {}
