package edu.mit.csail.db.ml.modeldb.evaluation

import org.apache.spark.ml.classification.{LogisticRegression, OneVsRest, RandomForestClassifier}
import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import edu.mit.csail.db.ml.modeldb.client.ModelDbSyncer._
import org.apache.spark.ml.Transformer

/**
  * A configuration object that is created by reading the command line arguments.
  * @param pathToData - The path to the Iris CSV file (https://archive.ics.uci.edu/ml/datasets/Iris).
  * @param randomForest - Whether a random forest should be trained. If false, a one-vs-rest logistic regression model
  *                     will be trained.
  * @param outfile - The path of the output file that should contain the serialized trained model.
  */
case class IrisConfig(pathToData: String = "",
                      randomForest: Boolean = true,
                      outfile: String = "")

/**
  * This workflow trains a model, either a random forest or one vs. rest logistic regression model, on the Iris dataset
  * and writes the output into a given file.
  */
object Iris {
  def runIris(config: IrisConfig): Unit = {
    val spark = Common.makeSession()

    // Read data.
    import spark.implicits._
    val df = spark
      .read
      .option("header", false)
      .option("inferSchema", true)
      .option("ignoreLeadingWhiteSpace", true)
      .option("ignoreTrainingWhiteSpace", true)
      .option("nullValue", "Unknown")
      .csv(config.pathToData)

    // Assemble the feature vectors.
    val asm = new VectorAssembler()
      .setInputCols(Array("_c0", "_c1", "_c2", "_c3"))
      .setOutputCol("features")
    val dfWithFeatures = asm.transform(df)

    // Index the label.
    val stringIndex = new StringIndexer()
      .setInputCol("_c4")
      .setOutputCol("label")
    val dfFinal = stringIndex.fit(dfWithFeatures).transform(dfWithFeatures)

    // Create the ModelDB Syncer.
    Common.makeSyncer()

    // Train the model.
    val model: Transformer = if (config.randomForest) {
      val rf = new RandomForestClassifier()
        .setLabelCol("label")
        .setFeaturesCol("features")
        .setNumTrees(200)
      rf.fitSync(dfFinal)
    } else {
      val lr = new LogisticRegression()
        .setFeaturesCol("features")
        .setLabelCol("label")
      val ovr = new OneVsRest()
        .setClassifier(lr)
      ovr.fitSync(dfFinal)
    }

    // Write model to file and show the predictions.
    model.saveSync("IrisModel" + (if (config.randomForest) "RandomForest" else "LogReg"))
    dfFinal.show()
  }

  /**
    * Runs the Iris workflow from the command line.
    */
  def main(args: Array[String]): Unit = {
    val parser = new scopt.OptionParser[IrisConfig]("Iris Program") {
      head("Iris Program")

      opt[String]('p', "path")
        .required()
        .valueName("<path_to_data_file>")
        .action((pathToData, config) => config.copy(pathToData = pathToData))
        .text("Path to the Iris data file (required)")

      opt[String]('o', "outfile")
        .required()
        .valueName("<outfile>")
        .action((outfile, config) => config.copy(outfile = outfile))
        .text("Path to file that will hold the output. It will be overwritten")

      opt[Boolean]('r', "randomForest")
        .action((randomForest, config) => config.copy(randomForest = randomForest))
        .text("If 'true' (default), then a Random Forest model will be used. Otherwise, " +
          "a one vs. rest logistic regression model will be used.")
    }

    parser.parse(args, IrisConfig()) match {
      case Some(config) => runIris(config)
      case None => println("Failed to parse command line arguments. Make sure your arguments are correct.")
    }
  }
}
