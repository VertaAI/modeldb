import edu.mit.csail.db.ml.modeldb.client.{ModelDbSyncer, SyncableDataFrame, SyncableDataFramePaths, SyncableTransformer}
import org.apache.spark.ml.{Pipeline, SyncableEstimator}
import org.apache.spark.ml.feature.OneHotEncoder
import org.apache.spark.ml.regression.LinearRegression
import org.scalatest.{BeforeAndAfter, FunSuite}

class SyncablePrimitivesTest extends FunSuite with BeforeAndAfter {
  before {
    TestBase.reset()
    TestBase.makeSyncer
  }

  test("Syncable DataFrame") {
    val df = SyncableDataFrame(TestBase.trainingData)
    assert(df.id === -1)
    assert(df.numRows === 12)
    assert(df.filepath.isEmpty)
    assert(df.tag === "")
    assert(df.schema.size === 3)
    assert(df.schema.head.name === "id")
    assert(df.schema.head.`type` === "bigint")
    assert(df.schema(1).name === "text")
    assert(df.schema(1).`type` === "string")
    assert(df.schema(2).name === "label")
    assert(df.schema(2).`type` === "double")
  }

  test("Syncable DataFrame with ID") {
    ModelDbSyncer.syncer.get.associateObjectAndId(TestBase.trainingData, 99)
    assert(SyncableDataFrame(TestBase.trainingData).id === 99)
  }

  test("Syncable DataFrame with tag") {
    ModelDbSyncer.syncer.get.associateObjectAndTag(TestBase.trainingData, "tagged")
    assert(SyncableDataFrame(TestBase.trainingData).tag === "tagged")
  }

  test("Syncable DataFrame with path") {
    SyncableDataFramePaths.setPath(TestBase.trainingData, "filepath")
    assert(SyncableDataFrame(TestBase.trainingData).filepath.get === "filepath")
  }

  test("Syncable Transformer") {
    val ohe = SyncableTransformer(new OneHotEncoder())
    assert(ohe.id === -1)
    assert(ohe.transformerType === "OneHotEncoder")
    assert(ohe.tag === "")
    assert(ohe.filepath.isEmpty)
  }

  test("Syncable Transformer with ID") {
    val ohe = new OneHotEncoder()
    ModelDbSyncer.syncer.get.associateObjectAndId(ohe, 99)
    assert(SyncableTransformer(ohe).id === 99)
  }

  test("Syncable Transformer with tag") {
    val ohe = new OneHotEncoder()
    ModelDbSyncer.syncer.get.associateObjectAndTag(ohe, "tagged")
    assert(SyncableTransformer(ohe).tag === "tagged")
  }

  test("Syncable Estimator") {
    val lr = new LinearRegression()
    val lrs = SyncableEstimator(lr)
    assert(lrs.id === -1)
    assert(lrs.transformerType === "LinearRegression")
    assert(lrs.tag === "")
    assert(lrs.hyperparameters.exists(hp => hp.name === "regParam" && hp.`type` === "Double"))
  }

  test("Syncable Estimator with ID") {
    val lr = new LinearRegression()
    ModelDbSyncer.syncer.get.associateObjectAndId(lr, 99)
    assert(SyncableEstimator(lr).id === 99)
  }

  test("Syncable Estimator with tag") {
    val lr = new LinearRegression()
    ModelDbSyncer.syncer.get.associateObjectAndTag(lr, "tagged")
    assert(SyncableEstimator(lr).tag === "tagged")
  }

  test("Syncable Estimator for Pipeline") {
    assert(SyncableEstimator(new Pipeline()).hyperparameters.isEmpty)
  }
}
