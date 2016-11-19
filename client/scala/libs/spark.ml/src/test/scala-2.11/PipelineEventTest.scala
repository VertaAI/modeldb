import edu.mit.csail.db.ml.modeldb.client.event._
import modeldb.{FitEventResponse, PipelineEventResponse, TransformEventResponse}
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.feature.{HashingTF, OneHotEncoder, Tokenizer}
import org.apache.spark.ml.regression.LinearRegression
import org.apache.spark.sql.DataFrame
import org.scalatest.{BeforeAndAfter, FunSuite}
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._

class PipelineEventTest extends FunSuite with BeforeAndAfter {
  before {
    TestBase.reset()
  }

  private def makePipeline: Pipeline = {
    val tokenizer = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("words")
    val hashingTF = new HashingTF()
      .setInputCol(tokenizer.getOutputCol)
      .setOutputCol("features")
    val lr = new LogisticRegression()
      .setMaxIter(10)
    val pipeline = new Pipeline()
      .setStages(Array(tokenizer, hashingTF, lr))
    pipeline
  }

  private def dfCopy: DataFrame = TestBase.trainingData.toDF()

  private def makeOhe: OneHotEncoder = new OneHotEncoder().setInputCol("inputCol").setOutputCol("outputCol")
  private def makeLr: LinearRegression = new LinearRegression()
    .setLabelCol("labelCol")
    .setFeaturesCol("featuresCol")
    .setPredictionCol("predictionCol")

  test("pipeline fit logs PipelineEvent") {
    val syncer = TestBase.makeSyncer
    val originalCount = syncer.numEvents
    val pipeline = makePipeline
    val pipelineModel = pipeline.fitSync(TestBase.trainingData)

    assert(syncer.numEvents - originalCount === 1)
    assert(syncer.hasEvent(originalCount) {
      case x: PipelineEvent =>
        x.pipeline === pipeline &&
          x.pipelineModel === pipelineModel &&
          x.inputDataFrame === TestBase.trainingData &&
          x.stages.length === 3 &&
          x.stages.head.isInstanceOf[TransformerPipelineStageEvent] &&
          x.stages(1).isInstanceOf[TransformerPipelineStageEvent] &&
          x.stages(2).isInstanceOf[FitPipelineStageEvent]
      case _ => false
    })
  }

  test("makePipelineFit for PipelineEvent") {
    val syncer = TestBase.makeSyncer
    val pipeline = makePipeline
    val pipelineModel = pipeline.fit(TestBase.trainingData)
    val pipelineEvent = PipelineEvent(pipeline, pipelineModel, TestBase.trainingData, Seq.empty)
    val pipelineFit = pipelineEvent.makePipelineFit
    assert(pipelineFit.model === pipelineModel)
    assert(pipelineFit.dataframe === TestBase.trainingData)
    assert(pipelineFit.estimator === pipeline)
  }

  test("makeStages for PipelineEvent") {
    val syncer = TestBase.makeSyncer
    val pipeline = makePipeline
    val pipelineModel = pipeline.fit(TestBase.trainingData)

    val lr = makeLr
    val oheFit = makeOhe
    val dfInFit = dfCopy
    val dfOutFit = dfCopy

    val oheT1 = makeOhe
    val dfInT1 = dfCopy
    val dfOutT1 = dfCopy

    val oheT2 = makeOhe
    val dfInT2 = dfCopy
    val dfOutT2 = dfCopy

    val pipelineEvent = PipelineEvent(
      pipeline,
      pipelineModel,
      TestBase.trainingData,
      Seq(
        TransformerPipelineStageEvent(dfInT1, dfOutT1, oheT1),
        FitPipelineStageEvent(dfInFit, dfOutFit, lr, oheFit),
        TransformerPipelineStageEvent(dfInT2, dfOutT2, oheT2)
      )
    )

    val (transformStages, fitStages, labelCols, featureCols, predictionCols) = pipelineEvent.makeStages(syncer)

    // Verify counts of columns. Note that these are set according to the expected results of oheFit.
    assert(labelCols.length === 0)
    assert(featureCols.length === 1)
    assert(predictionCols.length === 1)

    // Verify contents of columns.
    assert(featureCols.contains("inputCol"))
    assert(predictionCols.contains("outputCol"))

    // Verify lengths of stages.
    assert(fitStages.length === 1)
    assert(transformStages.length === 3)

    // Verify fit stages.
    assert(fitStages.head._1 === 1)
    assert(fitStages.head._2.estimator === lr)
    assert(fitStages.head._2.model === oheFit)
    assert(fitStages.head._2.dataframe === dfInFit)

    // Verify transform stages.
    assert(transformStages.head._1 === 0)
    assert(transformStages.head._2.inputDataframe === dfInT1)
    assert(transformStages.head._2.outputDataframe === dfOutT1)
    assert(transformStages.head._2.transformer === oheT1)

    assert(transformStages(1)._1 === 1)
    assert(transformStages(1)._2.inputDataframe === dfInFit)
    assert(transformStages(1)._2.outputDataframe === dfOutFit)
    assert(transformStages(1)._2.transformer === oheFit)

    assert(transformStages(2)._1 === 2)
    assert(transformStages(2)._2.inputDataframe === dfInT2)
    assert(transformStages(2)._2.outputDataframe === dfOutT2)
    assert(transformStages(2)._2.transformer === oheT2)
  }

  test("associate for PipelineEvent") {
    val syncer = TestBase.makeSyncer
    val pipeline = makePipeline
    val pipelineModel = pipeline.fit(TestBase.trainingData)

    val lr = makeLr
    val oheFit = makeOhe
    val dfInFit = dfCopy
    val dfOutFit = dfCopy

    val oheT1 = makeOhe
    val dfInT1 = dfCopy
    val dfOutT1 = dfCopy

    val oheT2 = makeOhe
    val dfInT2 = dfCopy
    val dfOutT2 = dfCopy

    val transformStages = Seq(
      (0, TransformEvent(oheT1, dfInT1, dfOutT1)),
      (1, TransformEvent(oheFit, dfInFit, dfOutFit)),
      (2, TransformEvent(oheT2, dfInT2, dfOutT2))
    )
    val fitStages = Seq((1, FitEvent(lr, dfInFit, oheFit)))

    val pipelineFit = FitEvent(pipeline, TestBase.trainingData, pipelineModel)

    val pipelineEvent = PipelineEvent(pipeline, pipelineModel, TestBase.trainingData, Seq.empty)

    val inDfId = 5
    val stage1Id = 11
    val stage2Id = 15
    val fitTransformId = 16
    pipelineEvent.associate(
      PipelineEventResponse(
        FitEventResponse(inDfId, 6, 7, 8, 9),
        Seq(
          TransformEventResponse(inDfId, stage1Id, 12, 13, ""),
          TransformEventResponse(stage1Id, stage2Id, fitTransformId, 17, ""),
          TransformEventResponse(stage2Id, 19, 20, 21, "")
        ),
        Seq(
          FitEventResponse(stage1Id, 23, fitTransformId, 25, 26)
        )
      ),
      pipelineFit,
      transformStages,
      fitStages,
      syncer,
      None
    )

    // Verify the Pipeline fit.
    assert(syncer.id(pipelineFit.dataframe).get === inDfId)
    assert(syncer.id(pipelineFit.model).get === 7)
    assert(syncer.id(pipelineFit.estimator).get === 6)

    // Verify the first transform stage.
    assert(syncer.id(oheT1).get === 12)
    assert(syncer.id(dfInT1).get === inDfId)
    assert(syncer.id(dfOutT1).get === stage1Id)

    // Verify the fit stage.
    assert(syncer.id(lr).get === 23)
    assert(syncer.id(oheFit).get === fitTransformId)
    assert(syncer.id(dfInFit).get === stage1Id)

    // Verify the second transform stage.
    assert(syncer.id(oheFit).get === fitTransformId)
    assert(syncer.id(dfInFit).get === stage1Id)
    assert(syncer.id(dfOutFit).get === stage2Id)

    // Verify the last transform stage
    assert(syncer.id(oheT2).get === 20)
    assert(syncer.id(dfInT2).get === stage2Id)
    assert(syncer.id(dfOutT2).get === 19)
  }
}
