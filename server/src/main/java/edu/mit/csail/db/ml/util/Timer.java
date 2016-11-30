package edu.mit.csail.db.ml.util;

public class Timer {
  private final long startTime;
  private final String message;

  public Timer(String message) {
    this.message = message;
    startTime = System.currentTimeMillis();
  }

  public void stop() {
    long elapsedTime = System.currentTimeMillis() - startTime;
    System.out.println(message + ": " + elapsedTime + " ms");
  }
}
