package edu.mit.csail.db.ml.modeldb.client.event

import com.twitter.util.Await
import edu.mit.csail.db.ml.modeldb.client._
import modeldb.ModelDBService.FutureIface
import org.apache.spark.ml
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
  override def sync(client: FutureIface, mdbs: Option[ModelDbSyncer]): Unit = {

    // First create the CrossValidationEvents.
    val crossValidationEvents = crossValidations.keys.map { (estimator) =>
      val folds = crossValidations(estimator).map { (fold) =>
        modeldb.CrossValidationFold(SyncableTransformer(fold.model),
          SyncableDataFrame(fold.validationDf),
          SyncableDataFrame(fold.trainingDf),
          fold.score)
      }
      modeldb.CrossValidationEvent(
        SyncableDataFrame(inputDataFrame),
        SyncableEstimator(inputDataFrame, estimator),
        seed,
        evaluator.getClass.getSimpleName, // TODO: Replace this with a more useful value.
        ml.SyncableEstimator.getLabelColumns(estimator),
        ml.SyncableEstimator.getPredictionCols(estimator),
        mdbs.get.getFeaturesForDf(inputDataFrame).getOrElse(ml.SyncableEstimator.getFeatureCols(estimator)),
        folds,
        experimentRunId = mdbs.get.experimentRun.id,
        problemType = SyncableProblemType(bestModel)
      )
    }.toSeq

    // Now create the GridSearchCrossValidationEvent.
    val gscve = modeldb.GridSearchCrossValidationEvent(
      numFolds,
      modeldb.FitEvent(
        SyncableDataFrame(inputDataFrame),
        SyncableEstimator(inputDataFrame, bestEstimator),
        SyncableTransformer(bestModel),
        labelColumns = ml.SyncableEstimator.getLabelColumns(bestEstimator),
        predictionColumns = ml.SyncableEstimator.getPredictionCols(bestEstimator),
        featureColumns =
          mdbs.get.getFeaturesForDf(inputDataFrame).getOrElse(ml.SyncableEstimator.getFeatureCols(bestEstimator)),
        experimentRunId = mdbs.get.experimentRun.id,
        problemType = SyncableProblemType(bestModel)
      ),
      crossValidationEvents,
      experimentRunId = mdbs.get.experimentRun.id,
      problemType = SyncableProblemType(bestModel)
    )

    val res = Await.result(client.storeGridSearchCrossValidationEvent(gscve))

    SyncableLinearModel(bestModel) match {
      case Some(lm) => Await.result(client.storeLinearModel(res.fitEventResponse.modelId, lm))
      case None => {}
    }

    // First associate the fit event.
    mdbs.get.associateObjectAndId(this, res.eventId)
      .associateObjectAndId(bestEstimator, res.fitEventResponse.specId)
      .associateObjectAndId(inputDataFrame, res.fitEventResponse.dfId)
      .associateObjectAndId(bestModel, res.fitEventResponse.modelId)

    // Now iterate through each cross validation in the grid.
    (crossValidations zip res.crossValidationEventResponses).foreach { pair =>
      val (estimator, folds) = pair._1
      val cver = pair._2

      // Associate each spec.
      mdbs.get.associateObjectAndId(cver.specId, estimator)

      // Iterate through each fold.
      (folds zip cver.foldResponses).foreach { pair =>
        val (fold, foldr) = pair
        // Store the model if it is linear.
        SyncableLinearModel(fold.model) match {
          case Some(lm) => Await.result(client.storeLinearModel(foldr.modelId, lm))
          case None => {}
        }
        // Associate the model and validation dataframe produced by the fold.
        mdbs.get.associateObjectAndId(fold.model, foldr.modelId)
          .associateObjectAndId(fold.validationDf, foldr.validationId)
          .associateObjectAndId(fold.trainingDf, foldr.trainingId)
      }
    }
  }
}

