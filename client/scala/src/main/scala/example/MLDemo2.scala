package example

import ai.verta.client._
import ai.verta.client.entities._
import ai.verta.repository._
import ai.verta.blobs._
import ai.verta.blobs.dataset._

import scala.concurrent.ExecutionContext
import scala.util.{Try, Success, Failure}

import java.io.File

import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.recommendation.{ALS, ALSModel}
import org.apache.spark.ml._
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.ml.feature._
import org.apache.spark.sql.types._
import org.apache.spark.sql.SaveMode
import org.apache.log4j.{Level, Logger}

import org.apache.spark.sql.SparkSession

object MLDemo2 extends App {
  Logger.getLogger("org").setLevel(Level.ERROR)
  val sparkSession = SparkSession.builder.master("local")
    .appName("Spark recommendation example")
    .getOrCreate()
  sparkSession.sparkContext.setLogLevel("ERROR")

  implicit val ec = ExecutionContext.global

  val client = new Client(ClientConnection.fromEnvironment())

  try {
    // get the repository
    val repo = client.getOrCreateRepository("MovieLens").get
    val commit = repo.getCommitByBranch("data").get

    val ratingsBlob: Dataset = commit.get("dataset").get match { case path: Dataset => path }
    val pathToData = ratingsBlob.download(
      componentPath = Some("ratings.parquet"),
      downloadToPath = Some("ratings.parquet")
    ).get
    val ratings = sparkSession.read.parquet(pathToData)

    // split data into train and test set:
    val Array(training, test) = ratings.randomSplit(Array(0.8, 0.2))


    // declare some hyperparameters:
    val maxIter = 5 // how many iters to train the model
    val regParam = 0.01 // regularizer strength

    val als = new ALS()
      .setMaxIter(maxIter)
      .setRegParam(regParam)
      .setUserCol("userId")
      .setItemCol("movieId")
      .setRatingCol("rating")
    val model = als.fit(training)

    // Evaluate the model by computing the RMSE on the test data
    // Note we set cold start strategy to 'drop' to ensure we don't get NaN evaluation metrics
    model.setColdStartStrategy("drop")
    val predictions = model.transform(test).cache()

    val evaluator = new RegressionEvaluator()
      .setMetricName("rmse")
      .setLabelCol("rating")
      .setPredictionCol("prediction")
    val rmse = evaluator.evaluate(predictions)

    // Generate top 10 movie recommendations for each user
    val userRecs = model.recommendForAllUsers(10)
    // Generate top 10 user recommendations for each movie
    val movieRecs = model.recommendForAllItems(10)

    // Generate top 10 movie recommendations for a specified set of users
    val users = ratings.select(als.getUserCol).distinct().limit(3)
    val userSubsetRecs = model.recommendForUserSubset(users, 10)
    // Generate top 10 user recommendations for a specified set of movies
    val movies = ratings.select(als.getItemCol).distinct().limit(3)
    val movieSubSetRecs = model.recommendForItemSubset(movies, 10)

    userRecs.show()
    movieRecs.show()
    userSubsetRecs.show()
    movieSubSetRecs.show()

    val project = client.getOrCreateProject("MovieLens").get
    val expRun = project
      .getOrCreateExperiment("ALS experiment")
      .flatMap(_.getOrCreateExperimentRun()).get

    expRun.logHyperparameters(Map(
      "maxIter" -> maxIter,
      "regParam" -> regParam
    ))
    expRun.logMetric("RMSE", rmse)
    expRun.logCommit(commit, Some(Map("data" -> "dataset")))
  } finally {
    client.close()
    sparkSession.stop()

    ExampleUtils.deleteDirectory(new File("ratings.parquet"))
  }
}
