package edu.mit.csail.db.ml.modeldb.client

import edu.mit.csail.db.ml.modeldb.client.event.ModelDbEvent

class ModelDbTestSyncer(projectConfig: ProjectConfig,
                        experimentConfig: ExperimentConfig,
                        experimentRunConfig: ExperimentRunConfig)
  extends ModelDbSyncer(
    hostPortPair = None,
    projectConfig = projectConfig,
    experimentConfig = experimentConfig,
    experimentRunConfig = experimentRunConfig,
    shouldCountRows = true
  ) {

  override def sync(): Unit = {}
  def getBuffer: Seq[ModelDbEvent] = this.buffered.map(_.event)
  def clear(): Unit = {
    this.objectIdMappings.clear()
    this.objectTagMappings.clear()
    this.buffered.clear()
  }
  def numEvents: Int = this.buffered.size
  def hasEvent(atIndex: Int)(fn: (ModelDbEvent) => Boolean): Boolean =
    this.buffered
      .map(_.event)
      .zipWithIndex
      .exists((pair) => (atIndex == -1 || pair._2 == atIndex) && fn(pair._1))
  def hasEvent(fn: (ModelDbEvent) => Boolean): Boolean = hasEvent(-1)(fn)
}
