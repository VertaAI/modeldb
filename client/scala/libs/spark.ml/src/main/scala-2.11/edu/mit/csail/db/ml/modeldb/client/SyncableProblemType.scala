package edu.mit.csail.db.ml.modeldb.client

import modeldb.ProblemType
import org.apache.spark.ml.PipelineModel
import org.apache.spark.ml.classification._
import org.apache.spark.ml.clustering._
import org.apache.spark.ml.recommendation.ALSModel
import org.apache.spark.ml.regression._
import org.apache.spark.ml.tuning.CrossValidatorModel

object SyncableProblemType {
  private def classification(numClasses: Int) = if (numClasses == 2)
    ProblemType.BinaryClassification
  else
    ProblemType.MulticlassClassification

  private def pipelineType(pm: PipelineModel): ProblemType =
    pm.stages.map(apply).foldLeft[ProblemType](ProblemType.Undefined)((oldProblemType, probType) => {
      if (probType.getValue() != ProblemType.Undefined.getValue())
        probType
      else
        oldProblemType
    })


  def apply(obj: Object): modeldb.ProblemType = obj match {
    case x: CrossValidatorModel => apply(x.bestModel)
    case x: PipelineModel => pipelineType(x)
    case x: LogisticRegressionModel => classification(x.numClasses)
    case x: DecisionTreeClassificationModel => classification(x.numClasses)
    case x: RandomForestClassificationModel => classification(x.numClasses)
    case x: GBTClassificationModel => classification(2) // GBT classification only works with 2 classes right now.
    case x: MultilayerPerceptronClassificationModel => classification(x.layers.last)
    case x: OneVsRestModel => classification(x.models.length)
    case x: NaiveBayesModel => classification(x.numClasses)
    case x: LinearRegressionModel => ProblemType.Regression
    case x: GeneralizedLinearRegressionModel => ProblemType.Regression
    case x: DecisionTreeRegressionModel => ProblemType.Regression
    case x: RandomForestRegressionModel => ProblemType.Regression
    case x: GBTRegressionModel => ProblemType.Regression
    case x: AFTSurvivalRegressionModel => ProblemType.Regression
    case x: IsotonicRegressionModel => ProblemType.Regression
    case x: KMeansModel => ProblemType.Clustering
    case x: DistributedLDAModel => ProblemType.Clustering
    case x: LocalLDAModel => ProblemType.Clustering
    case x: BisectingKMeansModel => ProblemType.Clustering
    case x: GaussianMixtureModel => ProblemType.Clustering
    case ALSModel => ProblemType.Recommendation
    case _ => ProblemType.Undefined
  }
}
