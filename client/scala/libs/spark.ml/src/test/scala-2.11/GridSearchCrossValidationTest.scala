import edu.mit.csail.db.ml.modeldb.client.event.{CrossValidationFold, GridSearchCrossValidationEvent}
import modeldb.{CrossValidationEvent, ProblemType, TransformerSpec}
import org.apache.spark.ml.classification.{LogisticRegression, LogisticRegressionModel}
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.feature.{HashingTF, OneHotEncoder, Tokenizer}
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import org.apache.spark.ml.{Pipeline, PipelineStage}
import org.apache.spark.sql.DataFrame
import org.scalatest.{BeforeAndAfter, FunSuite}
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._

import scala.collection.mutable.ArrayBuffer

class GridSearchCrossValidationTest extends FunSuite with BeforeAndAfter {
  before {
    TestBase.reset()
  }

  private lazy val preprocessedData: DataFrame = {
    val tokenizer = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("words")
    val hashingTF = new HashingTF()
      .setInputCol(tokenizer.getOutputCol)
      .setOutputCol("features")
    val pipeline = new Pipeline()
      .setStages(Array(tokenizer, hashingTF))
    pipeline.fit(TestBase.trainingData).transform(TestBase.trainingData)
  }

  private def makeCv: (CrossValidator, BinaryClassificationEvaluator) = {
    val lr = new LogisticRegression()
      .setMaxIter(10)
    val paramGrid = new ParamGridBuilder()
      .addGrid(lr.regParam, Array(0.1, 0.3))
      .build()
    val eval = new BinaryClassificationEvaluator()
    val cv = new CrossValidator()
      .setEstimator(lr)
      .setEvaluator(eval)
      .setEstimatorParamMaps(paramGrid)
      .setNumFolds(2)
    (cv, eval)
  }

  private def dfCopy: DataFrame = TestBase.trainingData.toDF()

  private def makeOhe: OneHotEncoder = new OneHotEncoder().setInputCol("inputCol").setOutputCol("outputCol")

  test("makeCrossValidationEvents of GSCVE") {
    val syncer = TestBase.makeSyncer
    val (cv, eval) = makeCv
    val cvModel = cv.fit(preprocessedData)
    val bestModel = cvModel.bestModel.asInstanceOf[LogisticRegressionModel]

    val lr1: PipelineStage = new LogisticRegression()
    val lr2: PipelineStage = new LogisticRegression()
    val gscve = GridSearchCrossValidationEvent(
      preprocessedData,
      Map(
        lr1 -> ArrayBuffer(
          CrossValidationFold(makeOhe, dfCopy, dfCopy, 0.5),
          CrossValidationFold(makeOhe, dfCopy, dfCopy, 0.6)
        ),
        lr2 -> ArrayBuffer(
          CrossValidationFold(makeOhe, dfCopy, dfCopy, 0.7),
          CrossValidationFold(makeOhe, dfCopy, dfCopy, 0.8)
        )
      ),
      100,
      eval,
      bestModel,
      bestModel.parent,
      2
    )

    val cves = gscve.makeCrossValidationEvents(syncer)
    assert(cves.length === 2)
    assert(cves.head.folds.length === 2)
    assert(cves(1).folds.length === 2)
  }

  test("makeGscve of GSCVE") {
    val syncer = TestBase.makeSyncer
    val (cv, eval) = makeCv
    val cvModel = cv.fit(preprocessedData)
    val bestModel = cvModel.bestModel.asInstanceOf[LogisticRegressionModel]

    val lr1: PipelineStage = new LogisticRegression()
    val lr2: PipelineStage = new LogisticRegression()
    val gscve = GridSearchCrossValidationEvent(
      preprocessedData,
      Map.empty,
      100,
      eval,
      bestModel,
      bestModel.parent,
      1
    )

    val g = gscve.makeGscve(syncer, Seq(
      CrossValidationEvent(
        modeldb.DataFrame(numRows = 2),
        TransformerSpec(-1, "", Seq.empty, ""),
        10,
        "test",
        Seq.empty,
        Seq.empty,
        Seq.empty,
        Seq(
          modeldb.CrossValidationFold(
            modeldb.Transformer(-1, "t"),
            modeldb.DataFrame(numRows = 2),
            modeldb.DataFrame(numRows = 2),
            0.5
          )
        ),
        1
      )
    ))
    assert(g.numFolds === 1)
    assert(g.bestFit.problemType === ProblemType.BinaryClassification)
    assert(g.crossValidations.length === 1)
  }

  test("associate of GSCVE") {
    val syncer = TestBase.makeSyncer
    val (cv, eval) = makeCv
    val cvModel = cv.fit(preprocessedData)
    val bestModel = cvModel.bestModel.asInstanceOf[LogisticRegressionModel]

    val lr1: PipelineStage = new LogisticRegression()
    val lr2: PipelineStage = new LogisticRegression()
    val dfV1 = dfCopy
    val dfV2 = dfCopy
    val dfT1 = dfCopy
    val dfT2 = dfCopy
    val mod1 = makeOhe
    val mod2 = makeOhe
    val mod3 = makeOhe
    val mod4 = makeOhe
    val gscve = GridSearchCrossValidationEvent(
      preprocessedData,
      Map(
        lr1 -> ArrayBuffer(
          CrossValidationFold(mod1, dfT1, dfV1, 0.5),
          CrossValidationFold(mod2, dfT2, dfV2, 0.6)
        ),
        lr2 -> ArrayBuffer(
          CrossValidationFold(mod3, dfT1, dfV1, 0.7),
          CrossValidationFold(mod4, dfT2, dfV2, 0.8)
        )
      ),
      100,
      eval,
      bestModel,
      bestModel.parent,
      2
    )

    val dfId = 4
    val t1 = 13
    val t2 = 14
    val v1 = 15
    val v2 = 16
    val lr1Id =
    gscve.associate(
      syncer,
      modeldb.GridSearchCrossValidationEventResponse(
        2,
        3,
        modeldb.FitEventResponse(dfId, 5, 6, 7, 8),
        Seq(
          modeldb.CrossValidationEventResponse(
            dfId,
            9,
            10,
            Seq(
              modeldb.CrossValidationFoldResponse(17, v1, t1),
              modeldb.CrossValidationFoldResponse(18, v2, t2)
            ),
            11
          ),
          modeldb.CrossValidationEventResponse(
            dfId,
            19,
            20,
            Seq(
              modeldb.CrossValidationFoldResponse(22, v1, t1),
              modeldb.CrossValidationFoldResponse(23, v2, t2)
            ),
            21
          )
        )
      ),
      None
    )

    assert(syncer.id(preprocessedData).get === dfId)
    assert(syncer.id(gscve).get === 3)
    assert(syncer.id(bestModel).get === 6)
    assert(syncer.id(bestModel.parent).get === 5)
    assert(syncer.id(dfT1).get === t1)
    assert(syncer.id(dfT2).get === t2)
    assert(syncer.id(dfV1).get === v1)
    assert(syncer.id(dfV2).get === v2)
    assert(syncer.id(mod1).get === 17)
    assert(syncer.id(mod2).get === 18)
    assert(syncer.id(mod3).get === 22)
    assert(syncer.id(mod4).get === 23)
    assert(syncer.id(lr1).get === 9)
    assert(syncer.id(lr2).get === 19)
  }

  test("cross validation logs GSCVE") {
    val syncer = TestBase.makeSyncer
    val originalCount = syncer.numEvents
    val (cv, eval) = makeCv
    val cvModel = cv.fitSync(preprocessedData)
    assert(syncer.numEvents - originalCount === 1)
    assert(syncer.hasEvent(originalCount) {
      case x: GridSearchCrossValidationEvent =>
        x.inputDataFrame === preprocessedData &&
        x.evaluator === eval &&
        x.bestModel === cvModel.bestModel
      case _ => false
    })
  }
}
