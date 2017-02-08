package edu.mit.csail.db.ml.modeldb.client.event

import com.twitter.util.Await
import edu.mit.csail.db.ml.modeldb.client._
import modeldb.ModelDBService.FutureIface
import modeldb.{CrossValidationEvent, GridSearchCrossValidationEventResponse}
import org.apache.spark.ml.evaluation.Evaluator
import org.apache.spark.ml.{PipelineStage, SyncableEstimator, Transformer}
import org.apache.spark.sql.DataFrame

import scala.collection.mutable.ArrayBuffer

/**
  * Represents a fold in cross validation. There should be a set of folds for each
  * cell in the cross validation search grid.
  * @param model - The model produced by training on the fold.
  * @param trainingDf - The DataFrame used to train the model above.
  * @param validationDf - The DataFrame to evaluate the trained model,
  *                     which produces the score below.
  * @param score - A score representing the quality of model trained on this fold.
  */
case class CrossValidationFold(model: Transformer,
                               trainingDf: DataFrame,
                               validationDf: DataFrame,
                               score: Double)

/**
  * Event indicating that the user has performed a grid search and used
  * cross-validation to train an estimator.
  * @param inputDataFrame - The DataFrame that the user is training on.
  * @param crossValidations - A map that goes from the estimator (there should be one per cell of
  *                         of the cross validation search grid) to the folds trained with that
  *                         estimator.
  * @param seed - The seed used by the random number generator that splits the DataFrame.
  * @param evaluator - The evaluator used to judge the quality of the models trained on
  *                  each fold.
  * @param bestModel - The model produced by training on the input DataFrame using the best
  *                  estimator.
  * @param bestEstimator - The estimator that reflects the best hyperparameter setting
  *                      that was found through grid search cross validation.
  * @param numFolds - The number of folds used for cross validation.
  */
case class GridSearchCrossValidationEvent(inputDataFrame: DataFrame,
                                          crossValidations: Map[PipelineStage, ArrayBuffer[CrossValidationFold]],
                                          seed: Long,
                                          evaluator: Evaluator,
                                          bestModel: Transformer,
                                          bestEstimator: PipelineStage,
                                          numFolds: Int)  extends ModelDbEvent {

  /**
    * Create a sequence of CrossValidationEvent objects based on the constructor arguments. These can be used to compose
    * the GridSearchCrossValidationEvent.
    * @param mdbs - The syncer.
    * @return The sequence of CrossValidationEvent objects.
    */
  def makeCrossValidationEvents(mdbs: ModelDbSyncer): Seq[CrossValidationEvent] =
  crossValidations.keys.map { (estimator) =>
    val folds = crossValidations(estimator).map { (fold) =>
      modeldb.CrossValidationFold(SyncableTransformer(fold.model),
        SyncableDataFrame(fold.validationDf),
        SyncableDataFrame(fold.trainingDf),
        fold.score)
    }
    modeldb.CrossValidationEvent(
      SyncableDataFrame(inputDataFrame),
      SyncableEstimator(estimator),
      seed,
      SyncableEvaluator.getMetricNameLabelColPredictionCol(evaluator)._1,
      mdbs.featureTracker.getLabelColumns(bestModel),
      mdbs.featureTracker.getOutputCols(bestModel),
      mdbs.featureTracker.getFeatureCols(inputDataFrame, bestModel),
      folds,
      experimentRunId = mdbs.experimentRun.id,
      problemType = SyncableProblemType(bestModel)
    )
  }.toSeq

  /**
    * Create a GridSearchCrossValidationEvent.
    * @param mdbs - The syncer.
    * @param crossValidationEvents - A sequence of CrossValidationEvents (should be one per hyperparameter configuration
    *                              considered in the search). You can create these using the makeCrossValidationEvents
    *                              function.
    * @return The GridSearchCrossValidationEvent.
    */
  def makeGscve(mdbs: ModelDbSyncer,
                crossValidationEvents: Seq[CrossValidationEvent]): modeldb.GridSearchCrossValidationEvent =
    modeldb.GridSearchCrossValidationEvent(
      numFolds,
      modeldb.FitEvent(
        SyncableDataFrame(inputDataFrame),
        SyncableEstimator(bestEstimator),
        SyncableTransformer(bestModel),
        labelColumns = mdbs.featureTracker.getLabelColumns(bestModel),
        predictionColumns = mdbs.featureTracker.getOutputCols(bestModel),
        featureColumns = mdbs.featureTracker.getFeatureCols(inputDataFrame, bestModel),
        experimentRunId = mdbs.experimentRun.id,
        problemType = SyncableProblemType(bestModel)
      ),
      crossValidationEvents,
      experimentRunId = mdbs.experimentRun.id,
      problemType = SyncableProblemType(bestModel)
    )

  /**
    * Update the object-ID mappings of the syncer based on the response to the GridSearchCrossValidationEvent.
    * @param mdbs - The syncer that contains the object-ID mappings.
    * @param res - The response for the GridSearchCrossValidationEvent.
    * @param client - The client for communicating with the server. This will be used to sync any specific models
    *               (e.g. LinearModel, TreeModel) that are indicated in the response. We can do this only when
    *               we have the response because the specific model must be associated with the ID of a Transformer. We
    *               can get this ID from the response. Currently, however, the code to store the specific models is
    *               commented out. This is done for performance reason. If we don't do it, then the size of the
    *               TreeLink and TreeNode tables gets huge.
    */
  def associate(mdbs: ModelDbSyncer, res: GridSearchCrossValidationEventResponse, client: Option[FutureIface]): Unit = {
    // First associate the fit event.
    mdbs.associateObjectAndId(this, res.eventId)
      .associateObjectAndId(bestEstimator, res.fitEventResponse.specId)
      .associateObjectAndId(inputDataFrame, res.fitEventResponse.dfId)
      .associateObjectAndId(bestModel, res.fitEventResponse.modelId)

    // Now iterate through each cross validation in the grid.
    (crossValidations zip res.crossValidationEventResponses).foreach { pair =>
      val (estimator, folds) = pair._1
      val cver = pair._2

      // Associate each spec.
      mdbs.associateObjectAndId(cver.specId, estimator)

      // Iterate through each fold.
      (folds zip cver.foldResponses).foreach { pair =>
        val (fold, foldr) = pair
        // It's debatable whether we actually want to store these.
//        SyncableSpecificModel(foldr.modelId, fold.model, client)
        // Associate the model and validation dataframe produced by the fold.
        mdbs.associateObjectAndId(fold.model, foldr.modelId)
          .associateObjectAndId(fold.validationDf, foldr.validationId)
          .associateObjectAndId(fold.trainingDf, foldr.trainingId)
      }
    }
  }

  /**
    * Store the GridSearchCrossValidationEvent on the server.
    * @param client - The client that exposes the functions that we
    *               call to store objects in the ModelDB.
    * @param mdbs - The ModelDbSyncer, included so we can update the ID
    *             mappings after syncing.
    */
  override def sync(client: FutureIface, mdbs: Option[ModelDbSyncer]): Unit = {
    val crossValidationEvents = makeCrossValidationEvents(mdbs.get)

    val gscve = makeGscve(mdbs.get, crossValidationEvents)

    val res = Await.result(client.storeGridSearchCrossValidationEvent(gscve))

    // Store a specific model (e.g. TreeModel, LinearModel) if applicable.
    SyncableSpecificModel(
      res.fitEventResponse.modelId,
      bestModel,
      Some(client),
      mdbs.get.shouldStoreSpecificModels
    )

    associate(mdbs.get, res, Some(client))
  }
}

