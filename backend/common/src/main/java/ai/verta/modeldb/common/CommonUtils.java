package ai.verta.modeldb.common;

import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import com.google.protobuf.GeneratedMessageV3;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.exception.LockAcquisitionException;

import java.net.SocketException;

public class CommonUtils {
  private static final Logger LOGGER = LogManager.getLogger(CommonUtils.class);
  private static final int STACKTRACE_LENGTH = 4;

  public static String appendOptionalTelepresencePath(String filePath) {
    String telepresenceRoot = System.getenv("TELEPRESENCE_ROOT");
    if (telepresenceRoot != null) {
      filePath = telepresenceRoot + filePath;
    }
    return filePath;
  }

  public static Integer getRegisteredBackgroundUtilsCount() {
    try {
      Integer backgroundUtilsCount =
          Integer.parseInt(System.getProperty(CommonConstants.BACKGROUND_UTILS_COUNT));
      LOGGER.trace("get runningBackgroundUtilsCount : {}", backgroundUtilsCount);
      return backgroundUtilsCount;
    } catch (NullPointerException ex) {
      LOGGER.trace("NullPointerException while get runningBackgroundUtilsCount");
      System.setProperty(CommonConstants.BACKGROUND_UTILS_COUNT, Integer.toString(0));
      return 0;
    }
  }

  public static void initializeBackgroundUtilsCount() {
    int backgroundUtilsCount = 0;
    try {
      if (System.getProperty(CommonConstants.BACKGROUND_UTILS_COUNT) == null) {
        LOGGER.trace("Initialize runningBackgroundUtilsCount : {}", backgroundUtilsCount);
        System.setProperty(
            CommonConstants.BACKGROUND_UTILS_COUNT, Integer.toString(backgroundUtilsCount));
      }
      LOGGER.trace(
          "Found runningBackgroundUtilsCount while initialization: {}",
          getRegisteredBackgroundUtilsCount());
    } catch (NullPointerException ex) {
      LOGGER.trace("NullPointerException while initialize runningBackgroundUtilsCount");
      System.setProperty(
          CommonConstants.BACKGROUND_UTILS_COUNT, Integer.toString(backgroundUtilsCount));
    }
  }

  /**
   * If service want to call other verta service internally then should to registered those service
   * here with count
   */
  public static void registeredBackgroundUtilsCount() {
    int backgroundUtilsCount = 0;
    if (System.getProperty(CommonConstants.BACKGROUND_UTILS_COUNT) != null) {
      backgroundUtilsCount = getRegisteredBackgroundUtilsCount();
    }
    backgroundUtilsCount = backgroundUtilsCount + 1;
    LOGGER.trace("After registered runningBackgroundUtilsCount : {}", backgroundUtilsCount);
    System.setProperty(
        CommonConstants.BACKGROUND_UTILS_COUNT, Integer.toString(backgroundUtilsCount));
  }

  public static void unregisteredBackgroundUtilsCount() {
    int backgroundUtilsCount = 0;
    if (System.getProperty(CommonConstants.BACKGROUND_UTILS_COUNT) != null) {
      backgroundUtilsCount = getRegisteredBackgroundUtilsCount();
      backgroundUtilsCount = backgroundUtilsCount - 1;
    }
    LOGGER.trace("After unregistered runningBackgroundUtilsCount : {}", backgroundUtilsCount);
    System.setProperty(
        CommonConstants.BACKGROUND_UTILS_COUNT, Integer.toString(backgroundUtilsCount));
  }

  public interface RetryCallInterface<T> {
    T retryCall(boolean retry);
  }

  public static Object retryOrThrowException(
      StatusRuntimeException ex,
      boolean retry,
      RetryCallInterface<?> retryCallInterface,
      Integer requestTimeout) {
    String errorMessage = ex.getMessage();
    LOGGER.debug(errorMessage);
    if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
      errorMessage = "UAC Service unavailable : " + errorMessage;
      if (retry && retryCallInterface != null) {
        try {
          Thread.sleep(requestTimeout * 1000);
          retry = false;
        } catch (InterruptedException e) {
          throw new InternalErrorException("Thread interrupted while UAC retrying call");
        }
        return retryCallInterface.retryCall(retry);
      }

      throw new UnavailableException(errorMessage);
    }
    throw ex;
  }

  public static StatusRuntimeException logError(Exception e) {
    return logError(e, null);
  }

  public static <T extends GeneratedMessageV3> StatusRuntimeException logError(
          Exception e, T defaultInstance) {
    Status status;
    StatusRuntimeException statusRuntimeException;
    if (e instanceof StatusRuntimeException) {
      statusRuntimeException = (StatusRuntimeException) e;
    } else {
      Throwable throwable = findRootCause(e);
      // Condition 'throwable != null' covered by below condition 'throwable instanceof
      // SocketException'
      StackTraceElement[] stack = e.getStackTrace();
      if (throwable instanceof SocketException) {
        String errorMessage = "Database Connection not found: ";
        LOGGER.info(errorMessage + "{}", e.getMessage());
        status =
                Status.newBuilder()
                        .setCode(Code.UNAVAILABLE_VALUE)
                        .setMessage(errorMessage + throwable.getMessage())
                        .build();
      } else if (e instanceof LockAcquisitionException) {
        String errorMessage = "Encountered deadlock in database connection.";
        LOGGER.info(errorMessage + "{}", e.getMessage());
        status =
                Status.newBuilder()
                        .setCode(Code.ABORTED_VALUE)
                        .setMessage(errorMessage + throwable.getMessage())
                        .build();
      } else if (e instanceof ModelDBException) {
        ModelDBException modelDBException = (ModelDBException) e;
        logBasedOnTheErrorCode(isClientError(modelDBException.getCode().value()), modelDBException);
        status =
                Status.newBuilder()
                        .setCode(modelDBException.getCode().value())
                        .setMessage(modelDBException.getMessage())
                        .build();
      } else {
        LOGGER.error(
                "Stacktrace with {} elements for {} {}", stack.length, e.getClass(), e.getMessage());
        status =
                Status.newBuilder()
                        .setCode(Code.INTERNAL_VALUE)
                        .setMessage(CommonConstants.INTERNAL_ERROR)
                        .build();
      }
      int n = 0;
      boolean isLongStack = stack.length > STACKTRACE_LENGTH;
      if (isLongStack) {
        for (; n < STACKTRACE_LENGTH + 1; ++n) {
          LOGGER.warn("{}: {}", n, stack[n].toString());
        }
      }
      for (; n < stack.length; ++n) {
        if (stack[n].getClassName().startsWith("ai.verta") || !isLongStack) {
          LOGGER.warn("{}: {}", n, stack[n].toString());
        }
      }
      statusRuntimeException = StatusProto.toStatusRuntimeException(status);
    }

    return statusRuntimeException;
  }

  public static <T extends GeneratedMessageV3> void observeError(
          StreamObserver<T> responseObserver, Exception e, T defaultInstance) {
    responseObserver.onError(logError(e, defaultInstance));
  }

  public static void logBasedOnTheErrorCode(boolean isClientError, Throwable e) {
    if (isClientError) {
      LOGGER.info("Exception occurred:{} {}", e.getClass(), e.getMessage());
    } else {
      LOGGER.warn("Exception occurred:{} {}", e.getClass(), e.getMessage());
    }
  }

  public static boolean isClientError(int grpcCodeValue) {
    switch (grpcCodeValue) {
      case 0: // OK : 200 OK
      case 1: // CANCELLED : 499 Client Closed Request
      case 3: // INVALID_ARGUMENT: 400 Bad Request
      case 5: // NOT_FOUND: 404 Not Found
      case 7: // PERMISSION_DENIED: 403 Forbidden
      case 6: // ALREADY_EXISTS: 409 Conflict
      case 8: // RESOURCE_EXHAUSTED: 429 Too Many Requests
      case 9: // FAILED_PRECONDITION: 400 Bad Request
      case 10: // ABORTED: 409 Conflict
      case 11: // OUT_OF_RANGE: 400 Bad Request
      case 16: // UNAUTHENTICATED: 401 Unauthorized
        return true;
      case 2: // UNKNOWN: 500 Internal Server Error
      case 4: // DEADLINE_EXCEEDED: 504 Gateway Timeout
      case 12: // UNIMPLEMENTED: 501 Not Implemented
      case 13: // INTERNAL: 500 Internal Server Error
      case 14: // UNAVAILABLE: 503 Service Unavailable
      case 15: // DATA_LOSS: 500 Internal Server Error
      default:
        return false;
    }
  }

  public static Throwable findRootCause(Throwable throwable) {
    if (throwable == null) {
      return null;
    }
    Throwable rootCause = throwable;
    while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
      rootCause = rootCause.getCause();
    }
    return rootCause;
  }
}
