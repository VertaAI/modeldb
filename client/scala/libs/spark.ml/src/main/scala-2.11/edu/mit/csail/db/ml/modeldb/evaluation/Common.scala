package edu.mit.csail.db.ml.modeldb.evaluation

import edu.mit.csail.db.ml.modeldb.client.{DefaultExperiment, ModelDbSyncer, NewExperimentRun, NewOrExistingProject}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{IntegerType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.{SparkConf, SparkContext}

import scala.util.Try

object Common {
  def makeSession(appName: String = "default app name"): SparkSession = {
    val conf = new SparkConf().setAppName(appName)
    val sc = new SparkContext(conf)
    val spark = SparkSession
      .builder()
      .appName(appName)
      .getOrCreate()
    spark
  }

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
