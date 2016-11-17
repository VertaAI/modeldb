package edu.mit.csail.db.ml.modeldb.evaluation

case class Config(pathToData: String = "",
                  dataset: String = "",
                  workflow: String = "",
                  outfile: String ="")

object Evaluate {
  def main(args: Array[String]): Unit = {
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
    }

    parser.parse(args, Config()) match {
      case Some(config) => println(config)
      case None => println("Failed to parse command line arguments - make sure to enter them properly.")
    }
  }
}