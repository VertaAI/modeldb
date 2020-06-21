package ai.verta.client.entities

import ai.verta.client._
import ai.verta.repository._
import ai.verta.blobs._
import ai.verta.blobs.dataset._
import ai.verta.client.entities.utils.ValueType

import scala.language.reflectiveCalls
import scala.concurrent.ExecutionContext
import scala.util.{Try, Success, Failure}

import org.scalatest.FunSuite
import org.scalatest.Assertions._

class TestExperimentRun extends FunSuite {
  implicit val ec = ExecutionContext.global

  def fixture =
    new {
      val client = new Client(ClientConnection.fromEnvironment())
      val repo = client.getOrCreateRepository("My Repo").get
      val project = client.getOrCreateProject("scala test").get
      val expRun = project.getOrCreateExperiment("experiment")
                          .flatMap(_.getOrCreateExperimentRun()).get
    }

  def cleanup(f: AnyRef{val client: Client; val repo: Repository; val project: Project; val expRun: ExperimentRun}) = {
    f.client.deleteRepository(f.repo.id)
    f.client.deleteProject(f.project.proj.id.get)
    f.client.close()
  }

  test("getMetric should retrieve the correct logged metric") {
    val f = fixture

    try {
       f.expRun.logMetric("some-metric", 0.5)
       f.expRun.logMetric("int-metric", 4)
       f.expRun.logMetrics(Map("other-metric" -> 0.3, "other-metric-2" -> 0.1))

       assertTypeError("f.expRun.logMetric(\"should-not-compile\", true)")

       assert(f.expRun.getMetric("some-metric").get.get.asDouble.get equals 0.5)
       assert(f.expRun.getMetric("other-metric").get.get.asDouble.get equals 0.3)
       assert(f.expRun.getMetric("other-metric-2").get.get.asDouble.get equals 0.1)
       assert(f.expRun.getMetric("int-metric").get.get.asBigInt.get equals 4)
    } finally {
      cleanup(f)
    }
  }

  test("getMetrics should retireve all the metrics logged") {
    val f = fixture

    try {
      val metrics: Map[String, ValueType] = Map("other-metric" -> 0.3, "other-metric-2" -> 0.1, "int-metric" -> 4)
      f.expRun.logMetrics(metrics)

      assert(f.expRun.getMetrics.get equals metrics)
    } finally {
      cleanup(f)
    }
  }

  test("logMetric(s) should fail when pass an existing key") {
    val f = fixture

    try {
      f.expRun.logMetric("existing", 0.5)
      val logAttempt = f.expRun.logMetric("existing", 0.5)
      assert(logAttempt.isFailure)
      assert(logAttempt match {case Failure(e) => e.getMessage contains "Metric being logged already exists"})

      val logAttempt2 = f.expRun.logMetrics(Map("existing" -> 0.5, "other-metric" -> 0.3))
      assert(logAttempt2.isFailure)
      assert(logAttempt2 match {case Failure(e) => e.getMessage contains "Metric being logged already exists"})
      assert(f.expRun.getMetric("other-metric").get.isEmpty)
    } finally {
      cleanup(f)
    }
  }

  test("getMetric should return None when a non-existing key is passed") {
    val f = fixture

    try {
      assert(f.expRun.getMetric("some-metric").get.isEmpty)
    } finally {
      cleanup(f)
    }
  }

  test("metrics map should behave like other metric methods") {
    val f = fixture

    try {
      val metrics = f.expRun.metrics()
      metrics += ("some-metric" -> 0.5)
      assert(metrics.get("some-metric").get.asDouble.get == 0.5)
      assert(f.expRun.getMetric("some-metric").get.get.asDouble.get equals 0.5)
      assert(metrics.get("other-metric").isEmpty)
    } finally {
      cleanup(f)
    }
  }

  test("getAttribute(s) should retrieve the correct logged attributes") {
    val f = fixture

    try {
      f.expRun.logAttribute("some-att", 0.5)
      f.expRun.logAttribute("int-att", 4)
      f.expRun.logAttributes(Map("other-att" -> 0.3, "string-att" -> "desc"))
      assertTypeError("f.expRun.logAtt(\"should-not-compile\", true)")

      assert(f.expRun.getAttribute("some-att").get.get.asDouble.get equals 0.5)
      assert(f.expRun.getAttribute("other-att").get.get.asDouble.get equals 0.3)
      assert(f.expRun.getAttribute("int-att").get.get.asBigInt.get equals 4)
      assert(f.expRun.getAttribute("string-att").get.get.asString.get equals "desc")

      assert(f.expRun.getAttributes(List("some-att", "other-att")).get equals
        Map[String, ValueType]("some-att" -> 0.5, "other-att" -> 0.3)
      )

      assert(f.expRun.getAttributes().get equals
        Map[String, ValueType]("some-att" -> 0.5, "int-att" -> 4, "other-att" -> 0.3, "string-att" -> "desc")
      )
    } finally {
      cleanup(f)
    }
  }

  test("getAttribute should return None when a non-existing key is passed") {
    val f = fixture

    try {
      assert(f.expRun.getAttribute("non-existing").get.isEmpty)
    } finally {
      cleanup(f)
    }
  }

  test("logAttribute(s) should fail when pass an existing key") {
    val f = fixture

    try {
      f.expRun.logAttribute("existing", 0.5)
      val logAttempt = f.expRun.logAttribute("existing", 0.5)
      assert(logAttempt.isFailure)
      assert(logAttempt match {case Failure(e) => e.getMessage contains "Attribute being logged already exists"})

      val logAttempt2 = f.expRun.logAttributes(Map("existing" -> 0.5, "other-att" -> 0.3))
      assert(logAttempt2.isFailure)
      assert(logAttempt2 match {case Failure(e) => e.getMessage contains "Attribute being logged already exists"})
      assert(f.expRun.getAttribute("other-att").get.isEmpty)
    } finally {
      cleanup(f)
    }
  }

  test("attributes map should behave like other attribute methods") {
    val f = fixture

    try {
      val attributes = f.expRun.attributes()
      attributes += ("some-att" -> 0.5)
      assert(attributes.get("some-att").get.asDouble.get == 0.5)
      assert(f.expRun.getAttribute("some-att").get.get.asDouble.get equals 0.5)
      assert(attributes.get("non-existing").isEmpty)
    } finally {
      cleanup(f)
    }
  }

  test("getTags should correctly retrieve the added tags") {
    val f = fixture

    try {
      assert(f.expRun.getTags.get equals Nil)
      assert(f.expRun.addTags(List("some-tag", "other-tag")).isSuccess)
      assert(f.expRun.getTags.get equals List("some-tag", "other-tag"))
    } finally {
      cleanup(f)
    }
  }

  test("getTags output should not contain deleted tags") {
    val f = fixture

    try {
      f.expRun.addTags(List("some-tag", "other-tag", "to-remove-tag-1", "to-remove-tag-2"))
      f.expRun.delTags(List("to-remove-tag-1", "to-remove-tag-2", "non-existing-tag"))
      assert(f.expRun.getTags.get equals List("some-tag", "other-tag"))
    } finally {
      cleanup(f)
    }
  }

  test("tags set should behave like other tag methods") {
    val f = fixture

    try {
      val tags = f.expRun.tags()
      tags += "some-tag"
      tags += "other-tag"
      assert(tags.contains("some-tag") && tags.contains("other-tag"))
      assert(f.expRun.getTags.get equals List("some-tag", "other-tag"))

      tags -= "other-tag"
      assert(!tags.contains("other-tag"))
      assert(f.expRun.getTags.get equals List("some-tag"))
    } finally {
      cleanup(f)
    }
  }

  test("getObservation(s) should get the correct series of logged observation") {
    val f = fixture

    try {
      f.expRun.logObservation("some-obs", 0.5)
      f.expRun.logObservation("some-obs", 0.4)
      f.expRun.logObservation("some-obs", 0.6)
      f.expRun.logObservation("single-obs", 7)
      f.expRun.logObservation("string-obs", "some string obs")
      assertTypeError("f.expRun.logObservation(\"some-observation\", true)")

      assert(f.expRun.getObservation("some-obs").get.map(_._2) equals List[ValueType](0.5, 0.4, 0.6))
      assert(f.expRun.getObservation("single-obs").get.map(_._2) equals List[ValueType](7))
      assert(f.expRun.getObservation("string-obs").get.map(_._2) equals List[ValueType]("some string obs"))
      assert(f.expRun.getObservation("non-existing-obs").get equals Nil)

      val allObservations = f.expRun.getObservations().get.mapValues(series => series.map(_._2))
      assert(allObservations equals
        Map[String, List[ValueType]]("some-obs" -> List(0.5, 0.4, 0.6), "single-obs" -> List(7), "string-obs" -> List("some string obs"))
      )
    } finally {
      cleanup(f)
    }
  }

  test("get commit should retrieve the right commit that was logged") {
    val f = fixture

    try {
      val commit = f.repo.getCommitByBranch().get
      val pathBlob = PathBlob(f"${System.getProperty("user.dir")}/src/test/scala/ai/verta/blobs/testdir").get

      val logAttempt = f.expRun.logCommit(commit)
      assert(logAttempt.isSuccess)

      val retrievedCommit = f.expRun.getCommit().get.commit
      assert(retrievedCommit equals commit)

      // update the logged commit:
      val newCommit = commit.update("abc/def", pathBlob)
                            .flatMap(_.save("Add a blob")).get
      f.expRun.logCommit(newCommit, Some(Map[String, String]("mnp/qrs" -> "abc/def")))
      val newExpRunCommit = f.expRun.getCommit().get
      assert(newCommit equals newExpRunCommit.commit)
      assert(!newExpRunCommit.commit.equals(commit))
      assert(newExpRunCommit.keyPaths.get equals Map[String, String]("mnp/qrs" -> "abc/def"))
    } finally {
      cleanup(f)
    }
  }


  test("get commit should fail if there isn't one assigned to the run") {
    val f = fixture

    try {
      val getAttempt = f.expRun.getCommit()
      assert(getAttempt.isFailure)
      assert(getAttempt match {case Failure(e) => e.getMessage contains "No commit is associated with this experiment run"})
    } finally {
      cleanup(f)
    }
  }
}
