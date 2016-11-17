package edu.mit.csail.db.ml.modeldb.evaluation

import java.io.{File, PrintWriter}

import scala.collection.mutable.ArrayBuffer

case class Timing(message: String, timeInNanoseconds: Long)

class Timer {
  private val timings = ArrayBuffer[Timing]()

  def time[R](msg: String)(block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    timings.append(Timing(msg, System.nanoTime() - t0))
    result
  }

  def getTimings: Seq[Timing] = timings.map(x => x)

  def clearTimings(): Unit = timings.clear()

  def writeTimingsToFile(path: String): Unit = {
    val pw = new PrintWriter(new File(path))
    timings.foreach(t => pw.write(t.toString + "\n"))
    pw.close()
  }
}
