package edu.mit.csail.db.ml.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * This is a utility class used to measure execution times.
 *
 * Currently, this is only used by the ModelDB evaluation code. It can also be useful for debugging.
 */
public class Timer {
  /**
   * This is a helper class that represents a single measurement. Notice that it is private and, thus, inaccessible
   * to anything outside the Timer class.
   */
  private static class Timing {
    /**
     * The text message associated with this measurement. For example, if we were measuring the
     * execution time of a method called myMethod, then "myMethod" would be a reasonable message.
     */
    public final String message;

    /**
     * The measured execution time, in milliseconds.
     */
    public final long timeMs;

    public Timing(String message, long timeMs) {
      this.message = message;
      this.timeMs = timeMs;
    }
  }

  /**
   * Stores all the measurements that this Timer has measured.
   */
  private static final ArrayList<Timing> timings = new ArrayList<>();

  /**
   * Forgets all the measurements that this Timer has measured.
   */
  public static void clear() {
    timings.clear();
  }

  /**
   * Write all the measurements to a CSV file at the given path.
   *
   * The CSV will have header columns "message" and "elapsedTimeMs", which are the string
   * message associated with each measurement and the actual measurement (in milliseconds), respectively.
   *
   * @param path - The filepath to write the CSV file to.
   * @throws IOException - Thrown if there are any exceptions when writing to the file.
   */
  public static void writeToFile(String path) throws IOException {
    PrintWriter writer = new PrintWriter(path, "UTF-8");
    writer.println("message, elapsedTimeMs");
    timings.forEach(timing -> writer.println(timing.message + ", " + timing.timeMs));
    writer.close();
  }

  /**
   * Measure the execution time of the given function and associate it with the given message.
   * @param message - A string message to associate with the function being executed. This will appear
   *                in the CSV file if you call writeToFile.
   * @param fn - The function whose execution time we aim to measure.
   * @throws Exception - Thrown if the given function, fn, throws an Exception.
   */
  public static void time(String message, ExceptionWrapper.CheckedRunnable fn) throws Exception {
    long start = System.currentTimeMillis();
    fn.run();
    long elapsed = System.currentTimeMillis() - start;
    timings.add(new Timing(message, elapsed));
  }
}
