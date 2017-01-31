package edu.mit.csail.db.ml.modeldb.evaluation

import java.io.{File, PrintWriter}

import scala.collection.mutable.ArrayBuffer

/**
  * Represents a operation that was timed.
  * @param message - A message describing the operation.
  * @param timeInNanoseconds - The duration of the operation in nanoseconds.
  */
case class Timing(message: String, timeInNanoseconds: Long)

/**
  * This object makes it possible to measure the execution times of various operations.
  */
object Timer {
  /**
    * A buffer of the operations that have been timed so far.
    */
  private val timings = ArrayBuffer[Timing]()

  /**
    * Whether the timer is currently enabled.
    */
  private var activated = false

  /**
    * The timestamp at which the timer was activated.
    */
  private var activationTime = System.nanoTime()

  /**
    * Enables the timer so that it can measure execution times.
    */
  def activate() = if (!activated) {
    activated = true
    activationTime = System.nanoTime()
  }

  /**
    * Disables the timer so that it no longer measures execution times.
    */
  def deactivate() = if (activated) {
    activated = false
    timings.append(Timing("Full timer duration", System.nanoTime() - activationTime))
  }

  /**
    * Measure the execution time of an operation (represented by a block). The timing is added to the buffer
    * of timings.
    * @param msg - The message to associate with the operation.
    * @param block - The operation to execute.
    * @tparam R - The return type of the operation.
    * @return The return value of the operation.
    */
  def time[R](msg: String)(block: => R): R = {
    val t0 = if (activated) System.nanoTime() else 0
    val result = block    // call-by-name
    if (activated) timings.append(Timing(msg, System.nanoTime() - t0))
    result
  }

  /**
    * @return A copy of the buffer of timings.
    */
  def getTimings: Seq[Timing] = timings.map(x => x)

  /**
    * Empty the buffer of timings.
    */
  def clearTimings(): Unit = timings.clear()

  /**
    * Flush the buffer of timings to a file at the given path. The output will be a CSV where the two columns are
    * "Operation" and "Time in seconds". The former contains the message associated with the operation and the latter
    * contains the execution time of the operation in seconds.
    * @param path - The path to the file where the timings should be written.
    */
  def writeTimingsToFile(path: String): Unit = {
    if (path.nonEmpty) {
      val pw = new PrintWriter(new File(path))
      pw.write("Operation, Time in seconds\n")
      timings.foreach(t => pw.write(t.message + ", " + t.timeInNanoseconds / 1000000000.0 + "\n"))
      pw.close()

    }
  }
}
