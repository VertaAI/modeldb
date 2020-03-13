package example

import ai.verta.client._

import scala.concurrent.ExecutionContext
import scala.util.Try

object Hello extends App {
  implicit val ec = ExecutionContext.global

  val client = new Client(ClientConnection.fromEnvironment())
  try {
    println(client.getOrCreateProject("scala test")
      .flatMap(_.getOrCreateExperiment("experiment"))
      .flatMap(_.getOrCreateExperimentRun())
      .flatMap(run => {
        //        run.hyperparameters += (("foo", 2), ("bar", "baz"))
        run.logArtifactObj("dummy", new DummyArtifact("hello")).get
        Try(run.getArtifactObj("dummy").get.asInstanceOf[DummyArtifact])
      })
      .get)
  } finally {
    client.close()
  }
}
