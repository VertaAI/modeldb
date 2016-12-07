package edu.mit.csail.db.ml.util;

import edu.mit.csail.db.ml.server.storage.ExperimentRunDao;
import modeldb.ServerLogicException;
import org.apache.thrift.TException;
import org.jooq.DSLContext;

public class ExceptionWrapper {
  public interface CheckedSupplier<T> {
    T get() throws Exception;
  }

  public interface CheckedRunnable {
    void run() throws Exception;
  }

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

  public static <T> T run(int expRunId, DSLContext ctx, CheckedSupplier<T> fn) throws TException {
    return run(() -> {
      ExperimentRunDao.validateExperimentRunId(expRunId, ctx);
      return fn.get();
    });
  }
}
