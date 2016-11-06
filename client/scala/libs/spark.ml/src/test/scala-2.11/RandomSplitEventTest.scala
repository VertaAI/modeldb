import org.scalatest.{BeforeAndAfter, FunSuite}
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
import edu.mit.csail.db.ml.modeldb.client.event.RandomSplitEvent
import modeldb.RandomSplitEventResponse

class RandomSplitEventTest extends FunSuite with BeforeAndAfter {
  before {
    TestBase.reset()
  }

  test("Random splitting logs RandomSplitEvent") {
    val syncer = TestBase.makeSyncer
    val originalCount = syncer.numEvents
    val splits = TestBase.trainingData.randomSplitSync(Array(0.4, 0.6), 100)
    assert(syncer.numEvents - originalCount === 1)
    assert(syncer.hasEvent(originalCount) {
      case x: RandomSplitEvent =>
        x.weights.length === 2 &&
        x.weights.head === 0.4 &&
        x.weights(1) === 0.6 &&
        x.seed === 100 &&
        x.dataframe === TestBase.trainingData &&
        x.result.length === 2 &&
        x.result.head === splits(0)
        x.result(1) === splits(1)
      case _ => false
    })
  }

  test("makeEvent of a RandomSplitEvent") {
    val syncer = TestBase.makeSyncer
    val df1 = TestBase.trainingData.toDF()
    val df2 = TestBase.trainingData.toDF()
    val rse = RandomSplitEvent(TestBase.trainingData, Array(0.1, 0.9), 100, Array(df1, df2))
    val rsEvent = rse.makeEvent(syncer)
    assert(rsEvent.oldDataFrame.numRows === 12)
    assert(rsEvent.weights.length === 2)
    assert(rsEvent.weights.head === 0.1)
    assert(rsEvent.weights(1) === 0.9)
    assert(rsEvent.seed === 100)
    assert(rsEvent.splitDataFrames.length === 2)
    assert(rsEvent.splitDataFrames.head.numRows === 12)
    assert(rsEvent.splitDataFrames(1).numRows === 12)
  }

  test("associate of a RandomSplitEvent") {
    val syncer = TestBase.makeSyncer
    val df1 = TestBase.trainingData.toDF()
    val df2 = TestBase.trainingData.toDF()
    val rse = RandomSplitEvent(TestBase.trainingData, Array(0.5, 0.5), 100, Array(df1, df2))
    rse.associate(RandomSplitEventResponse(5, Seq(6, 7), 8), syncer)
    assert(syncer.id(TestBase.trainingData).get === 5)
    assert(syncer.id(df1).get === 6)
    assert(syncer.id(df2).get === 7)
    assert(syncer.id(rse).get === 8)
  }
}
