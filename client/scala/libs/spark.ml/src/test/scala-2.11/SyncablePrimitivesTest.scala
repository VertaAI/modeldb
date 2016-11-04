import edu.mit.csail.db.ml.modeldb.client.{ModelDbSyncer, SyncableDataFrame}
import org.scalatest.{BeforeAndAfter, FunSuite}

class SyncablePrimitivesTest extends FunSuite with BeforeAndAfter {
  before {
    TestBase.reset()
    TestBase.getSyncer
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
    var trainingData = TestBase.trainingData
    ModelDbSyncer.syncer.get.associateObjectAndId(trainingData, 99)
    val df = SyncableDataFrame(trainingData)
    assert(df.id === 99)
  }
}
