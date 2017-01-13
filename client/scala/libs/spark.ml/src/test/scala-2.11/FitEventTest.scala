import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
import edu.mit.csail.db.ml.modeldb.client.event.FitEvent
import modeldb.{FitEventResponse, ProblemType}
import org.apache.spark.ml.feature.StringIndexer
import org.apache.spark.ml.param.ParamMap
import org.scalatest.{BeforeAndAfter, FunSuite}

class FitEventTest extends FunSuite with BeforeAndAfter {
  before {
    TestBase.reset()
  }

  test("Fitting logs FitEvent") {
    val syncer = TestBase.makeSyncer
    val originalCount = syncer.numEvents
    val indexer = new StringIndexer()
      .setInputCol("text")
      .setOutputCol("textIndexed")
    val indexerModel = indexer.fitSync(TestBase.trainingData)

    assert(syncer.numEvents - originalCount === 1)
    assert(syncer.hasEvent(originalCount) {
      case x: FitEvent =>
        x.model === indexerModel &&
        x.estimator === indexer &&
        x.dataframe === TestBase.trainingData
      case _ => false
    })
  }

  test("Fit with ParamMap") {
    val syncer = TestBase.makeSyncer
    val originalCount = syncer.numEvents
    val indexer = new StringIndexer()
    val pm = ParamMap()
      .put(indexer.inputCol -> "text")
      .put(indexer.outputCol -> "textIndexed")
    val pm2 = ParamMap()
      .put(indexer.inputCol -> "text")
      .put(indexer.outputCol -> "textIndexed2")
    indexer.fitSync(TestBase.trainingData, Array(pm, pm2))

    assert(syncer.numEvents - originalCount === 2)
    assert(syncer.hasEvent(originalCount) {
      case x: FitEvent =>
        x.estimator.asInstanceOf[StringIndexer].getOutputCol === "textIndexed"
      case _ => false
    })
    assert(syncer.hasEvent(originalCount + 1) {
      case x: FitEvent =>
        x.estimator.asInstanceOf[StringIndexer].getOutputCol === "textIndexed2"
      case _ => false
    })
  }

  test("makeEvent of a FitEvent") {
    val syncer = TestBase.makeSyncer
    val indexer = new StringIndexer()
      .setInputCol("text")
      .setOutputCol("textIndexed")
    val indexerModel = indexer.fit(TestBase.trainingData)

    val fe = FitEvent(indexer, TestBase.trainingData, indexerModel)
    val fEvent = fe.makeEvent(syncer)
    assert(fEvent.featureColumns.size === 1)
    assert(fEvent.featureColumns.head === "text")
    assert(fEvent.labelColumns.isEmpty)
    assert(fEvent.predictionColumns.size === 1)
    assert(fEvent.predictionColumns.head === "textIndexed")
    assert(fEvent.problemType === ProblemType.Undefined)
    assert(fEvent.df.numRows === 12)
    assert(fEvent.model.transformerType === "StringIndexerModel")
    assert(fEvent.spec.transformerType === "StringIndexer")
  }

  test("associate of a FitEvent") {
    val syncer = TestBase.makeSyncer
    val indexer = new StringIndexer()
      .setInputCol("text")
      .setOutputCol("textIndexed")
    val indexerModel = indexer.fit(TestBase.trainingData)

    val fe = FitEvent(indexer, TestBase.trainingData, indexerModel)
    fe.associate(FitEventResponse(5, 6, 7, 8, 9), syncer)
    assert(syncer.id(TestBase.trainingData).get === 5)
    assert(syncer.id(indexer).get === 6)
    assert(syncer.id(indexerModel).get === 7)
    assert(syncer.id(fe).get === 8)
  }
}
