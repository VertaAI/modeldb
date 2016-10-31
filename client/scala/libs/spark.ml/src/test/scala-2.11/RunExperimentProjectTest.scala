import edu.mit.csail.db.ml.modeldb.client.event.{ExperimentEvent, ExperimentRunEvent, ProjectEvent}
import edu.mit.csail.db.ml.modeldb.client.{NewExperimentRun, NewOrExistingExperiment, NewOrExistingProject}
import org.scalatest.{BeforeAndAfter, FunSuite}

class RunExperimentProjectTest extends FunSuite with BeforeAndAfter {

  before {
    TestBase.reset()
  }

  test("On creating a ModelDbSyncer the proper events should be stored") {
    val syncer = TestBase.getSyncer
    assert(syncer.numEvents == 3)
    // We expect a ProjectEvent, ExperimentEvent, and then an ExperimentRunEvent.
    // Each of them should be new, and the experiment should be default.
    assert(syncer.hasEvent {
      case x: ProjectEvent => x.project.id == -1
      case _ => false
    }, 0)
    assert(syncer.hasEvent {
      case x: ExperimentEvent => x.experiment.id == -1 && x.experiment.isDefault
      case _ => false
    }, 1)
    assert(syncer.hasEvent {
      case x: ExperimentRunEvent => x.experimentRun.id == -1
      case _ => false
    }, 2)
  }

  test("Allow creating a non-default experiment") {
    val syncer = TestBase.getSyncer(
      NewOrExistingProject("name", "author", "description"),
      NewOrExistingExperiment("expName", "expDesc"),
      NewExperimentRun("expRunDesc")
    )
    assert(syncer.numEvents == 3)
    assert(syncer.hasEvent {
      case x: ProjectEvent => x.project.id == -1 && x.project.name == "name"
      case _ => false
    }, 0)
    assert(syncer.hasEvent {
      case x: ExperimentEvent => x.experiment.id == -1 && !x.experiment.isDefault && x.experiment.name == "expName"
      case _ => false
    }, 1)
    assert(syncer.hasEvent {
      case x: ExperimentRunEvent => x.experimentRun.id == -1 && x.experimentRun.description == "expRunDesc"
      case _ => false
    }, 2)
  }
}