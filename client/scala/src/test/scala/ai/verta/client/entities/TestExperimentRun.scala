package ai.verta.client.entities

import ai.verta.client._
import ai.verta.repository._
import ai.verta.blobs._
import ai.verta.blobs.dataset._
import ai.verta.client.entities.utils.ValueType

import scala.language.reflectiveCalls
import scala.concurrent.ExecutionContext
import scala.util.{Try, Success, Failure}
import scala.collection.mutable

import java.io._
import java.nio.charset.StandardCharsets

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

  /** Helper function to convert byte stream to UTF-8 string
   *  From https://stackoverflow.com/questions/50392640/how-to-compare-two-bytearrayinputstream-in-junit-testing/50392948
   */
  def streamToString(inputStream: ByteArrayInputStream) = {
    val result = new ByteArrayOutputStream();
    val buffer = new Array[Byte](1024)

    var length = inputStream.read(buffer);
    while (length != -1) {
        result.write(buffer, 0, length);
        length = inputStream.read(buffer);
    }
    result.toString("UTF-8");
  }

  /** Test function to verify that getting should retrieve the correctly logged metadata
   *  @param logger function to log a single piece metadata
   *  @param multiLogger function to log multiple pieces of metadata
   *  @param getter function to retrieve a single piece of metadata
   *  @param allGetter function to retrieve all stored metadata
   *  @param someGetter (optional) function to retrieve stored metadata corresponding to a list of keys
   */
  def testMetadata(
    logger: ExperimentRun => ((String, ValueType) => Try[Unit]),
    multiLogger: ExperimentRun => (Map[String, ValueType] => Try[Unit]),
    getter: ExperimentRun => (String => Try[Option[ValueType]]),
    allGetter: ExperimentRun => (() => Try[Map[String, ValueType]]),
    someGetter: Option[ExperimentRun => (List[String] => Try[Map[String, ValueType]])] = None
  ) = {
    val f = fixture

    try {
      logger(f.expRun)("some", 0.5)
      logger(f.expRun)("int", 4)
      multiLogger(f.expRun)(Map("other" -> 0.3, "string" -> "desc"))

      assert(getter(f.expRun)("some").get.get.asDouble.get equals 0.5)
      assert(getter(f.expRun)("other").get.get.asDouble.get equals 0.3)
      assert(getter(f.expRun)("int").get.get.asBigInt.get equals 4)
      assert(getter(f.expRun)("string").get.get.asString.get equals "desc")

      if (someGetter.isDefined)
        assert(someGetter.get(f.expRun)(List("some", "other")).get  equals
          Map[String, ValueType]("some" -> 0.5, "other" -> 0.3)
        )

      assert(allGetter(f.expRun)().get equals
        Map[String, ValueType]("some" -> 0.5, "int" -> 4, "other" -> 0.3, "string" -> "desc")
      )
    } finally {
      cleanup(f)
    }
  }

  /** Test function to verify that getting a metadata with non-existing key should fail
   *  @param getter function to retrieve a single piece of metadata
   */
  def testNonExisting(getter: ExperimentRun => (String => Try[Option[ValueType]])) = {
    val f = fixture

    try {
      assert(getter(f.expRun)("non-existing").get.isEmpty)
    } finally {
      cleanup(f)
    }
  }

  /** Test function to verify that logging a metadata with an existing key should fail
   *  @param logger function to log a single piece metadata
   *  @param multiLogger function to log multiple pieces of metadata
   *  @param getter function to retrieve a single piece of metadata
   *  @param metadataName type of the metadata
   */
  def testAlreadyLogged(
    logger: ExperimentRun => ((String, ValueType) => Try[Unit]),
    multiLogger: ExperimentRun => (Map[String, ValueType] => Try[Unit]),
    getter: ExperimentRun => (String => Try[Option[ValueType]]),
    metadataName: String
  ) = {
    val f = fixture

    try {
      logger(f.expRun)("existing", 0.5)
      val logAttempt = logger(f.expRun)("existing", 0.5)
      assert(logAttempt.isFailure)
      assert(logAttempt match {case Failure(e) => e.getMessage contains f"${metadataName} being logged already exists"})

      val logAttempt2 = multiLogger(f.expRun)(Map("existing" -> 0.5, "other" -> 0.3))
      assert(logAttempt2.isFailure)
      assert(logAttempt2 match {case Failure(e) => e.getMessage contains f"${metadataName} being logged already exists"})
      assert(getter(f.expRun)("other").get.isEmpty)
    } finally {
      cleanup(f)
    }
  }

  /** Test function to verify that the map interface of a metadata works
   *  @param getMap function to get the map interface of metadata
   *  @param getter function to retrieve a single piece of metadata
   */
  def testMapInterface(
    getMap: ExperimentRun => (() => mutable.Map[String, ValueType]),
    getter: ExperimentRun => (String => Try[Option[ValueType]])
  ) = {
    val f = fixture

    try {
      val map = getMap(f.expRun)()
      map += ("some" -> 0.5)
      assert(map.get("some").get.asDouble.get == 0.5)
      assert(getter(f.expRun)("some").get.get.asDouble.get equals 0.5)
      assert(map.get("non-existing").isEmpty)
    } finally {
      cleanup(f)
    }
  }

  test("getMetric should retrieve the correct logged metric") {
    testMetadata(_.logMetric, _.logMetrics, _.getMetric, _.getMetrics)
  }

  test("logMetric(s) should fail when pass an existing key") {
    testAlreadyLogged(_.logMetric, _.logMetrics, _.getMetric,  "Metric")
  }

  test("getMetric should return None when a non-existing key is passed") {
    testNonExisting(_.getMetric)
  }

  test("metrics map should behave like other metric methods") {
    testMapInterface(_.metrics, _.getMetric)
  }

  test("getAttribute(s) should retrieve the correct logged attributes") {
    testMetadata(
      _.logAttribute,
      _.logAttributes,
      _.getAttribute,
      expRun => (() => expRun.getAttributes()),
      Some(_.getAttributes)
    )
  }

  test("getAttribute should return None when a non-existing key is passed") {
    testNonExisting(_.getAttribute)
  }

  test("logAttribute(s) should fail when pass an existing key") {
    testAlreadyLogged(_.logAttribute, _.logAttributes, _.getAttribute, "Attribute")
  }

  test("attributes map should behave like other attribute methods") {
    testMapInterface(_.attributes, _.getAttribute)
  }

  test("getHyperparameter(s) should retrieve the correct logged attributes") {
    testMetadata(_.logHyperparameter, _.logHyperparameters, _.getHyperparameter, _.getHyperparameters)
  }

  test("getHyperparameter should return None when a non-existing key is passed") {
    testNonExisting(_.getHyperparameter)
  }

  test("logHyperparameter(s) should fail when pass an existing key") {
    testAlreadyLogged(_.logHyperparameter, _.logHyperparameters, _.getHyperparameter, "Hyperparameter")
  }

  test("hyperparameters map should behave like other hyperparameter methods") {
    testMapInterface(_.hyperparameters, _.getHyperparameter)
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

      // Should not allow updating commit:
      val newCommit = commit.update("abc/def", pathBlob)
                            .flatMap(_.save("Add a blob")).get
      val logAttempt2 = f.expRun.logCommit(newCommit, Some(Map[String, String]("mnp/qrs" -> "abc/def")))
      assert(logAttempt2.isFailure)
      
      val newRetrievedCommit = f.expRun.getCommit().get.commit
      assert(newRetrievedCommit equals commit)
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

  test("get artifact object should get the correct logged object") {
    val f = fixture

    try {
      val artifact = new DummyArtifact("hello")
      f.expRun.logArtifactObj("dummy", artifact)
      assert(f.expRun.getArtifactObj("dummy").get equals artifact)
    } finally {
      cleanup(f)
    }
  }

  test("get artifact stream should get the correct logged stream of bytes") {
    val f = fixture

    try {
      val arr = new ByteArrayOutputStream()
      val stream = new ObjectOutputStream(arr)
      stream.writeObject("some weird object")
      stream.close
      f.expRun.logArtifact("stream", new ByteArrayInputStream(arr.toByteArray))

      assert(
        streamToString(new ByteArrayInputStream(arr.toByteArray)) ==
          streamToString(f.expRun.getArtifact("stream").get)
      )
    } finally {
      cleanup(f)
    }
  }

  test("log artifact with existing key should fail") {
    val f = fixture

    try {
      val artifact = new DummyArtifact("hello")
      f.expRun.logArtifactObj("existing", artifact)

      val newArtifact = new DummyArtifact("world")
      val logAttempt = f.expRun.logArtifactObj("existing", newArtifact)

      assert(logAttempt.isFailure)
      assert(logAttempt match {case Failure(e) => e.getMessage contains "Artifact being logged already exists"})

      val arr = new ByteArrayOutputStream()
      val stream = new ObjectOutputStream(arr)
      stream.writeObject("some weird object")
      stream.close
      val logAttempt2 = f.expRun.logArtifact("existing", new ByteArrayInputStream(arr.toByteArray))

      assert(logAttempt2.isFailure)
      assert(logAttempt2 match {case Failure(e) => e.getMessage contains "Artifact being logged already exists"})
    } finally {
      cleanup(f)
    }
  }
}
