import edu.mit.csail.db.ml.modeldb.client._
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.log4j.Logger
import org.apache.log4j.Level

object TestBase {
  lazy val getSession: SparkSession = {
      Logger.getLogger("org").setLevel(Level.OFF)
      Logger.getLogger("akka").setLevel(Level.OFF)
      new SparkContext(new SparkConf().setMaster("local[*]").setAppName("test")).setLogLevel("OFF")
      SparkSession.builder().appName("Unit tests").getOrCreate()
  }

  lazy val trainingData = getSession.createDataFrame(Seq(
    (0L, "a b c d e spark", 1.0),
    (1L, "b d", 0.0),
    (2L, "spark f g h", 1.0),
    (3L, "hadoop mapreduce", 0.0),
    (4L, "b spark who", 1.0),
    (5L, "g d a y", 0.0),
    (6L, "spark fly", 1.0),
    (7L, "was mapreduce", 0.0),
    (8L, "e spark program", 1.0),
    (9L, "a e c l", 0.0),
    (10L, "spark compile", 1.0),
    (11L, "hadoop software", 0.0)
  )).toDF("id", "text", "label")

  def makeSyncer(projectConfig: ProjectConfig,
                 experimentConfig: ExperimentConfig,
                 experimentRunConfig: ExperimentRunConfig): ModelDbTestSyncer = {
    val syncer = new ModelDbTestSyncer(projectConfig, experimentConfig, experimentRunConfig)
    ModelDbSyncer.setSyncer(syncer)
    syncer
  }

  def makeSyncer: ModelDbTestSyncer = makeSyncer(
    NewOrExistingProject("unit test",
      "harihar",
      "this example creates a cross validation"
    ),
    new DefaultExperiment,
    new NewExperimentRun
  )

  def reset(): Unit = {
    ModelDbSyncer.syncer match {
      case Some(s: ModelDbTestSyncer) => s.clear()
      case _ => {}
    }
    SyncableDataFramePaths.clear()
  }
}
