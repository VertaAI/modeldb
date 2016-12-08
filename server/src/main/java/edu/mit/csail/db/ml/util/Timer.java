package edu.mit.csail.db.ml.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class Timer {
  private static class Timing {
    public final String message;
    public final long timeMs;
    public Timing(String message, long timeMs) {
      this.message = message;
      this.timeMs = timeMs;
    }
  }

  private static final ArrayList<Timing> timings = new ArrayList<>();

  public static void clear() {
    timings.clear();
  }

  public static void writeToFile(String path) throws IOException {
    PrintWriter writer = new PrintWriter(path, "UTF-8");
    writer.println("message, elapsedTimeMs");
    timings.forEach(timing -> writer.println(timing.message + ", " + timing.timeMs));
    writer.close();
  }

  public static void time(String message, ExceptionWrapper.CheckedRunnable fn) throws Exception {
    long start = System.currentTimeMillis();
    fn.run();
    long elapsed = System.currentTimeMillis() - start;
    timings.add(new Timing(message, elapsed));
  }
}
