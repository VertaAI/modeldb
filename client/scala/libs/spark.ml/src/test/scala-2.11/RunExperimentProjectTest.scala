import edu.mit.csail.db.ml.modeldb.client.event.{ExperimentEvent, ExperimentRunEvent, ProjectEvent}
import org.scalatest.FunSuite

class RunExperimentProjectTest extends FunSuite {

  test("On creating a ModelDbSyncer the proper events should be stored") {
    val syncer = TestBase.getSyncer
    assert(syncer.numEvents == 3)
    assert(syncer.hasEvent {
      case x: ProjectEvent => x.project.id == -1
      case _ => false
    })
    assert(syncer.hasEvent {
      case x: ExperimentEvent => x.experiment.id == -1
      case _ => false
    })
    assert(syncer.hasEvent {
      case x: ExperimentRunEvent => x.experimentRun.id == -1
      case _ => false
    })
  }
}