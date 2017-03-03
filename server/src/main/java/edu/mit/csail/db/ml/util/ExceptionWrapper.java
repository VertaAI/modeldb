package edu.mit.csail.db.ml.util;

import edu.mit.csail.db.ml.server.storage.ExperimentRunDao;
import edu.mit.csail.db.ml.server.storage.metadata.MetadataDb;
import modeldb.ServerLogicException;
import org.apache.thrift.TException;
import org.jooq.DSLContext;

/**
 * This class contains logic for running code and wrapping any thrown exceptions.
 */
public class ExceptionWrapper {
  /**
   * An interface for a function that takes no arguments, produce an output of type T, and potentially throws an
   * Exception.
   * @param <T> - The return type.
   */
  public interface CheckedSupplier<T> {
    T get() throws Exception;
  }

  /**
   * An interface for a function that takes no argument, returns no output, and potentially throws an Exception.
   */
  public interface CheckedRunnable {
    void run() throws Exception;
  }

  /**
   * Runs a given function and catches any exceptions. If there are no exceptions, the function returns, as usual.
   * If there are any exceptions, the following steps occur. First, a stacktrace is printed. Then, if the exception
   * is of type TException, it is re-thrown. Otherwise, its message and simple name are wrapped
   * in a ServerLogicException and the ServerLogicException is thrown.
   * @param fn - The function to execute.
   * @param <T> - The return type of the function.
   * @return The return value of the function.
   * @throws TException - The exception thrown by fn(). If fn() does not produce a TException, this will be a
   * ServerLogicException (a subclass of TException).
   */
  public static <T> T run(CheckedSupplier<T> fn) throws TException {
    try {
      return fn.get();
    } catch (Exception ex) {
      ex.printStackTrace();
      if (ex instanceof TException) {
        throw (TException) ex;
      } else {
        throw new ServerLogicException(ex.getClass().getSimpleName() + ": " + ex.getMessage());
      }
    }
  }

  /**
   * This is like run(CheckedSupplier<T> fn), but it first ensures that the given experiment run ID is valid.
   */
  public static <T> T run(int expRunId, DSLContext ctx, CheckedSupplier<T> fn) throws TException {
    return run(() -> {
      ExperimentRunDao.validateExperimentRunId(expRunId, ctx);
      return fn.get();
    });
  }

  public static <T> T run(int expRunId, DSLContext ctx, MetadataDb metadataDb, 
    CheckedSupplier<T> fn) throws TException {
    return run(() -> {
      ExperimentRunDao.validateExperimentRunId(expRunId, ctx);
      return fn.get();
    });
  }

  public static <T> T run(
    MetadataDb metadataDb, 
    CheckedSupplier<T> fn) throws TException {
    return run(() -> {
      return fn.get();
    });
  }
}
