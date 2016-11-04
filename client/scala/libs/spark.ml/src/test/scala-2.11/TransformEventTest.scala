import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
import edu.mit.csail.db.ml.modeldb.client.event.TransformEvent
import modeldb.TransformEventResponse
import org.apache.spark.ml.feature.Tokenizer
import org.scalatest.{BeforeAndAfter, FunSuite}

class TransformEventTest extends FunSuite with BeforeAndAfter {
  before {
    TestBase.reset()
  }

  test("Transformation logs TransformEvent") {
    val syncer = TestBase.makeSyncer
    val originalCount = syncer.numEvents
    val tokenizer = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("words")
    val df = tokenizer.transformSync(TestBase.trainingData)

    assert(syncer.numEvents - originalCount === 1)
    assert(syncer.hasEvent(originalCount) {
      case x: TransformEvent =>
        x.inputDataframe === TestBase.trainingData &&
        x.outputDataframe === df &&
        x.transformer === tokenizer
      case _ => false
    })
  }

  test("makeEvent of a TransformEvent") {
    val syncer = TestBase.makeSyncer
    val tok = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("words")
    val te = TransformEvent(tok, TestBase.trainingData, TestBase.trainingData)
    val tEvent = te.makeEvent(syncer)
    assert(tEvent.inputColumns.nonEmpty)
    assert(tEvent.outputColumns.nonEmpty)
    assert(tEvent.inputColumns.head === "text")
    assert(tEvent.outputColumns.head === "words")
    assert(tEvent.transformer.transformerType === "Tokenizer")
    assert(tEvent.oldDataFrame.numRows === 12)
    assert(tEvent.newDataFrame.numRows === 12)
  }

  test("associate of a TransformEvent") {
    val syncer = TestBase.makeSyncer
    val tok = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("words")
    val df1 = TestBase.trainingData
    val df2 = TestBase.trainingData.toDF()
    val te = TransformEvent(tok, df1, df2)
    te.associate(TransformEventResponse(5, 6, 7, 8, ""), syncer)
    assert(syncer.id(df1).get === 5)
    assert(syncer.id(df2).get === 6)
    assert(syncer.id(tok).get === 7)
    assert(syncer.id(te).get === 8)
  }
}
