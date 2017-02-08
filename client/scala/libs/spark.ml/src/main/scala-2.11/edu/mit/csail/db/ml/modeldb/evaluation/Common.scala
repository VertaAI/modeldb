package edu.mit.csail.db.ml.modeldb.evaluation

import edu.mit.csail.db.ml.modeldb.client.{DefaultExperiment, ModelDbSyncer, NewExperimentRun, NewOrExistingProject}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{IntegerType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.{SparkConf, SparkContext}

import scala.util.Try

/**
  * This object contains convenience functions that are useful in various places in the evaluation code.
  */
object Common {
  /**
    * Create a SparkSession with the given app name.
    * @param appName - The app name.
    * @return The SparkSession.
    */
  def makeSession(appName: String = "default app name"): SparkSession = {
    val conf = new SparkConf().setAppName(appName)
    val sc = new SparkContext(conf)
    val spark = SparkSession
      .builder()
      .appName(appName)
      .getOrCreate()
    spark
  }

  /**
    * Ensure that the given DataFrame has at least minNumRows. If not, append all the
    * rows of origDf to itself until the size matches or exceeds minNumRows.
    * @param origDf - The original DataFrame.
    * @param minNumRows - The minimum number of rows.
    * @return origDf, with some rows possibly copied multiple times to ensure that that total
    *         row count matches or exceeds minNumRows.
    */
  def ensureMinSize(origDf: DataFrame, minNumRows: Int): DataFrame = {
    val numRows = origDf.count

    if (numRows >= minNumRows)
      origDf
    else {
      var df = origDf.toDF()
      val numMultiplies = 1 + minNumRows / numRows
      for (i <- 1 to numMultiplies.toInt)
        df = df.union(origDf.toDF())
      assert(df.count > minNumRows)
      df
    }
  }

  /**
    * Create a ModelDbSyncer object.
    * @param appName - The name of the app.
    * @param appDesc - The description of the app.
    * @param shouldCountRows - Whether the number of rows in a DataFrame should be counted and stored in the database
    *                        (false improves performance).
    * @param shouldStoreGSCVE - Whether intermediate FitEvents, TransformEvents, and MetricEvents of a
    *                         GridSearchCrossValidationEvent should be stored in the database (false improves
    *                         performance).
    * @param shouldStoreSpecificModels - Whether specific (e.g. TreeModel, LinearModel) models should be stored
    *                                  in the database (false improves performance and storage usage).
    * @return The ModelDbSyncer.
    */
  def makeSyncer(appName: String = "default app name",
                 appDesc: String = "default description",
                 shouldCountRows: Boolean = false,
                 shouldStoreGSCVE: Boolean = false,
                 shouldStoreSpecificModels: Boolean = false): ModelDbSyncer = {
    val syncer = new ModelDbSyncer(
      projectConfig = NewOrExistingProject(
        appName,
        "hsubrama@mit.edu",
        appDesc
      ),
      experimentConfig = new DefaultExperiment,
      experimentRunConfig = new NewExperimentRun,
      shouldCountRows = shouldCountRows,
      shouldStoreGSCVE = shouldStoreGSCVE,
      shouldStoreSpecificModels = shouldStoreSpecificModels
    )
    ModelDbSyncer.setSyncer(syncer)
    syncer
  }

  /**
    * Read the IMDB dataset.
    * @param pathToData - Path to the IMDB CSV.
    * @param spark - The Spark session.
    * @return A DataFrame representing the IMDB data. It also includes columns called first_genre and
    *         second_genre.
    */
  def readImdb(pathToData: String, spark: SparkSession): DataFrame = {
    import spark.implicits._
    // Extract the genres from the genre column, which looks like this:
    // genre1|genre2|genre3|...
    val extractFirstGenre = udf((col: String) => col.split('|')(0))
    val extractSecondGenre = udf((col: String) => {
      val items = col.split('|').tail
      if (items.length == 0)
        "None"
      else
        items(0)
    })

    spark
      .read
      .option("header", true)
      .option("inferSchema", true)
      .option("ignoreLeadingWhiteSpace", true)
      .option("ignoreTrainingWhiteSpace", true)
      .option("nullValue", "None")
      .csv(pathToData)
      .withColumn("first_genre", extractFirstGenre($"genres"))
      .withColumn("second_genre", extractSecondGenre($"genres"))
      .na.fill(0)
  }

  /**
    * Read the animal shelter data into a DataFrame.
    * @param pathToData - The path to the CSV file.
    * @param spark - The Spark session.
    * @return A DataFrame representing the data. It includes a column called "AgeInYears", a "SimpleBreed" column that
    *         groups some of the least common breeds into a category called "other", and a "SimpleColor" column that
    *         groups some of the least common colors into a category called "other".
    */
  def readAnimalShelter(pathToData: String, spark: SparkSession): DataFrame = {
    import spark.implicits._
    // We'll read the AgeUponOutcome and convert that to the number of years.
    val parseAge = udf((ageStr: String) => {
      if (ageStr == "Unknown") {
        0
      } else {
        val split = ageStr.split(" ")
        val age = split(0).toInt
        val numPerYear = split(1) match {
          case "days" | "day" => 365
          case "month" | "months" => 12
          case "week" | "weeks" => 52
          case "years" | "year" => 1
          case _ => 1
        }
        age / 1.0 / numPerYear
      }
    })

    // Instead of using all the breeds, we'll use the give most common breeds.
    val groupBreed = udf((breedStr: String) =>
      if (Set("Domestic Shorthair Mix",
        "Pit Bull Mix",
        "Chihuahua Shorthair Mix",
        "Labrador Retriever Mix",
        "Domestic Medium Hair Mix").contains(breedStr))
        breedStr
      else
        "other"
    )

    // Instead of using all the colors, we'll use the ten most common colors.
    val groupColor = udf((colorStr: String) =>
      if (Set("Black/White",
        "Black",
        "Brown Tabby",
        "Brown Tabby/White",
        "White",
        "Brown/White",
        "Orange Tabby",
        "Tan/White",
        "Tricolor",
        "Blue/White").contains(colorStr))
        colorStr
      else
        "other"
    )

    spark
      .read
      .option("header", true)
      .option("inferSchema", true)
      .option("ignoreLeadingWhiteSpace", true)
      .option("ignoreTrailingWhiteSpace", true)
      .option("nullValue", "Unknown")
      .option("dateFormat", "yyyy-MM-dd")
      .csv(pathToData)
      .withColumn("AgeInYears", parseAge($"AgeuponOutcome"))
      .withColumn("SimpleBreed", groupBreed($"Breed"))
      .withColumn("SimpleColor", groupColor($"Color"))
  }

  /**
    * Read the housing regression dataset.
    * @param pathToData - The path to the CSV file.
    * @param spark - The Spark session.
    * @return A DataFrame representing the data.
    */
  def readHousingPrices(pathToData: String, spark: SparkSession): DataFrame = {
    import spark.implicits._
    // Read the data.
    val dfOrig = spark
      .read
      .option("header", true)
      .option("inferSchema", true)
      .option("ignoreLeadingWhiteSpace", true)
      .option("ignoreTrainingWhiteSpace", true)
      .option("nullValue", "Unknown")
      .csv(pathToData)
      .withColumn("label", $"SalePrice".cast("double"))

    val hasNAs = Set("GarageYrBlt", "MasVnrArea", "LotFrontage")
    val numericalIndices = dfOrig
      .schema
      .fields
      .zipWithIndex
      .filter{case (value, index) => value.dataType == IntegerType || hasNAs.contains(value.name)}
      .map{case (value, index) => index}
      .toSet
    spark.sqlContext.createDataFrame(dfOrig.rdd.map{ row =>
      Row.fromSeq(row.toSeq.zipWithIndex.map{ case (value, index) =>
        if (numericalIndices.contains(index) && (value == null || value.isInstanceOf[String]))
          Try(value.asInstanceOf[String].toInt).toOption.getOrElse(0)
        else
          value
      })
    }, StructType(
      dfOrig.schema.map(field =>
        StructField(field.name, if (hasNAs.contains(field.name)) IntegerType else field.dataType)))
    )
  }
}
