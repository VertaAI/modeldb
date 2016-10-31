package edu.mit.csail.db.ml.modeldb.client

import edu.mit.csail.db.ml.modeldb.client.event.ModelDbEvent

class ModelDbTestSyncer(projectConfig: ProjectConfig,
                        experimentConfig: ExperimentConfig,
                        experimentRunConfig: ExperimentRunConfig)
  extends ModelDbSyncer(
    hostPortPair = None,
    projectConfig = projectConfig,
    experimentConfig = experimentConfig,
    experimentRunConfig = experimentRunConfig
  ) {

  override def sync(): Unit = {}
  def getBuffer: Seq[ModelDbEvent] = this.buffered.map(_.event)
  def clearBuffer(): Unit = this.buffered.clear()
  def numEvents: Int = this.buffered.size
  def hasEvent(fn: (ModelDbEvent) => Boolean, atIndex: Int = -1): Boolean =
    this.buffered
      .map(_.event)
      .zipWithIndex
      .exists((pair) => (atIndex == -1 || pair._2 == atIndex) && fn(pair._1))
}
