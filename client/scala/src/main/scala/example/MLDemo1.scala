package example

import ai.verta.client._
import ai.verta.client.entities._
import ai.verta.repository._
import ai.verta.blobs._
import ai.verta.blobs.dataset._

import scala.concurrent.ExecutionContext
import scala.util.{Try, Success, Failure}

import java.io.File

import org.apache.spark.ml._
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.ml.feature._
import org.apache.spark.sql.types._
import org.apache.spark.sql.SaveMode
import org.apache.log4j.{Level, Logger}

object MLDemo extends App {
  case class Rating(userId: Int, movieId: Int, rating: Float, timestamp: Long)

  def parseRating(str: String): Rating = {
    val fields = str.split("::")
    assert(fields.size == 4)
    Rating(fields(0).toInt, fields(1).toInt, fields(2).toFloat, fields(3).toLong)
  }

  Logger.getLogger("org").setLevel(Level.ERROR)
  val sparkSession = SparkSession.builder.master("local")
    .appName("Spark recommendation example")
    .getOrCreate()
  sparkSession.sparkContext.setLogLevel("ERROR")
  import sparkSession.implicits._

  implicit val ec = ExecutionContext.global

  val client = new Client(ClientConnection.fromEnvironment())
  
  try {
    // download the data
    val raw_data_path = "example_data/sample_movielens_ratings.txt"
    val raw_data_url = "https://raw.githubusercontent.com/apache/spark/master/data/mllib/als/sample_movielens_ratings.txt"
    ExampleUtils.fileDownloader(raw_data_url, raw_data_path)

    val ratings = sparkSession.read.textFile(raw_data_path)
      .map(parseRating)
      .toDF()

      // save the data:
      ratings
        .write
        .mode(SaveMode.Overwrite)
        .parquet("ratings.parquet")

      // create a repository
      val repo = client.getOrCreateRepository("MovieLens").get

      // Create the data blob:
      val dataset = PathBlob("ratings.parquet", true).get

      // Save the blobs in a commit and save the commit to ModelDB:
      repo.getCommitByBranch()
        .flatMap(_.newBranch("data"))
        .flatMap(_.update("dataset", dataset))
        .flatMap(_.save("upload data")).get
  } finally {
    client.close()
    sparkSession.stop()

    ExampleUtils.deleteDirectory(new File("ratings.parquet"))
    ExampleUtils.deleteDirectory(new File("example_data"))
  }
}
