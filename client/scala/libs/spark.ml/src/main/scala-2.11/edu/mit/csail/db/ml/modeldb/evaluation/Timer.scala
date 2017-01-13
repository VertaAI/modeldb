package edu.mit.csail.db.ml.modeldb.evaluation

import java.io.{File, PrintWriter}

import scala.collection.mutable.ArrayBuffer

case class Timing(message: String, timeInNanoseconds: Long)

object Timer {
  private val timings = ArrayBuffer[Timing]()

  private var activated = false
  private var activationTime = System.nanoTime()

  def activate() = if (!activated) {
    activated = true
    activationTime = System.nanoTime()
  }

  def deactivate() = if (activated) {
    activated = false
    timings.append(Timing("Full timer duration", System.nanoTime() - activationTime))
  }

  def time[R](msg: String)(block: => R): R = {
    val t0 = if (activated) System.nanoTime() else 0
    val result = block    // call-by-name
    if (activated) timings.append(Timing(msg, System.nanoTime() - t0))
    result
  }

  def getTimings: Seq[Timing] = timings.map(x => x)

  def clearTimings(): Unit = timings.clear()

  def writeTimingsToFile(path: String): Unit = {
    if (path.nonEmpty) {
      val pw = new PrintWriter(new File(path))
      pw.write("Operation, Time in seconds\n")
      timings.foreach(t => pw.write(t.message + ", " + t.timeInNanoseconds / 1000000000.0 + "\n"))
      pw.close()

    }
  }
}
