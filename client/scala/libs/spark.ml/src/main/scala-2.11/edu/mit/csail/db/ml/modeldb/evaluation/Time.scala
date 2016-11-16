package edu.mit.csail.db.ml.modeldb.evaluation

object Time {
  def apply[R](msg: String)(block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    println(s"Elapsed time for $msg: " + (t1 - t0) + "ns")
    result
  }
}
