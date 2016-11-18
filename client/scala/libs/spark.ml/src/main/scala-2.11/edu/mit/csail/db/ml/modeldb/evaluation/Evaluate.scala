package edu.mit.csail.db.ml.modeldb.evaluation

case class Config(pathToData: String = "",
                  dataset: String = "",
                  workflow: String = "",
                  outfile: String = "",
                  syncer: Boolean = true)

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

      opt[Boolean]('s', "syncer")
        .action((syncer, config) => config.copy(syncer = syncer))
        .text("If 'true' (default), then ModelDB will be used. If 'false', ModelDB will not be used.")
    }

    parser.parse(args, Config()) match {
      case Some(config) =>
        if (config.dataset == "imdb" && config.workflow == "simple")
          IMDBSimple.run(config)
        else
          println("Failed to match any configuration", config)
      case None => println("Failed to parse command line arguments - make sure to enter them properly.")
    }
  }
}