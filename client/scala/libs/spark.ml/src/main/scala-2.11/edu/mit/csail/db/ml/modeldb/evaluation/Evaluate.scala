package edu.mit.csail.db.ml.modeldb.evaluation

// TODO: The minNumRows and associated duplication logic should be removed.
/**
  * Represents the configuration used to run the evaluation script.
  * @param pathToData - The path to the CSV file that contains the data. You can get the IMDB data at
  *                   https://www.kaggle.com/deepmatrix/imdb-5000-movie-dataset
  *                   You can get the animal shelter data at
  *                   https://www.kaggle.com/c/shelter-animal-outcomes/data
  *                   You can get the housing data at
  *                   https://www.kaggle.com/c/house-prices-advanced-regression-techniques/data
  * @param dataset - The dataset. This should be "housing", "animal", or "imdb".
  * @param workflow - The workflow. This should be "simple", "full", or "exploratory".
  * @param outfile - The path to the file that the output will be written to.
  * @param syncer - Whether the program should be run with the ModelDB Syncer enabled.
  * @param minNumRows - [Deprecated] The minimum number of rows to use in the dataset.
  */
case class Config(pathToData: String = "",
                  dataset: String = "",
                  workflow: String = "",
                  outfile: String = "",
                  syncer: Boolean = true,
                  minNumRows: Int = 1)

/**
  * Runs a program to evaluate the running time of ModelDB Server and Spark Client.
  */
object Evaluate {
  def main(args: Array[String]): Unit = {
    // Set up the command line argument parser.
    val parser = new scopt.OptionParser[Config]("ModelDB Evaluation Program") {
      head("ModelDB Evaluation")

      opt[String]('p', "path")
        .required()
        .valueName("<path_to_data_file>")
        .action((pathToData, config) => config.copy(pathToData = pathToData))
        .text("Path to the data file (required)")

      opt[String]('d', "dataset")
        .required()
        .valueName("<dataset>")
        .action((dataset, config) => config.copy(dataset = dataset.toLowerCase.replace("'", "")))
        .text("The dataset. This should be one of 'imdb', 'animal', or 'housing' " +
          "and should correspond to the path to the data file that you provided")

      opt[String]('w', "workflow")
        .required()
        .valueName("<workflow>")
        .action((workflow, config) => config.copy(workflow = workflow.toLowerCase.replace("'", "")))
        .text("The workflow. This should be one of 'simple', 'full', or 'exploratory'")

      opt[String]('o', "outfile")
        .required()
        .valueName("<outfile>")
        .action((outfile, config) => config.copy(outfile = outfile))
        .text("Path to file that will hold the output. It will be overwritten")

      opt[Boolean]('s', "syncer")
        .action((syncer, config) => config.copy(syncer = syncer))
        .text("If 'true' (default), then ModelDB will be used. If 'false', ModelDB will not be used.")

      opt[Int]('n', "min_num_rows")
        .action((min_num_rows, config) => config.copy(minNumRows = min_num_rows))
        .text("The minimum number of rows to use for the workflow. If this is less than the dataset size, then the " +
          "entire dataset will be used. If it is greater than the dataset size, then the dataset will be duplicated " +
          "and vertically concatenated with itself until the resulting row count exceeds the minimum number of rows")
    }

    // Parse the arguments and run the appropriate workflow.
    parser.parse(args, Config()) match {
      case Some(config) =>
        if (config.dataset == "imdb" && config.workflow == "simple")
          IMDBSimple.run(config)
        else if (config.dataset == "housing" && config.workflow == "simple")
          HousingSimple.run(config)
        else if (config.dataset == "animal" && config.workflow == "simple")
          AnimalSimple.run(config)
        else if (config.dataset == "imdb" && config.workflow == "full")
          IMDBFull.run(config)
        else if (config.dataset == "housing" && config.workflow == "full")
          HousingFull.run(config)
        else if (config.dataset == "animal" && config.workflow == "full")
          AnimalFull.run(config)
        else if (config.dataset == "imdb" && config.workflow == "exploratory")
          IMDBExploratory.run(config)
        else if (config.dataset == "animal" && config.workflow == "exploratory")
          AnimalExploratory.run(config)
        else if (config.dataset == "housing" && config.workflow == "exploratory")
          HousingExploratory.run(config)
        else
          println("Failed to match any configuration", config)
      case None => println("Failed to parse command line arguments - make sure to enter them properly.")
    }
  }
}