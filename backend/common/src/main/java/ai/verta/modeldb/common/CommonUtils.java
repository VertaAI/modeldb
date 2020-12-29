package ai.verta.modeldb.common;

import ai.verta.modeldb.utils.ModelDBUtils;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommonUtils {
  private static final Logger LOGGER = LogManager.getLogger(CommonUtils.class);

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
          Status status =
              Status.newBuilder()
                  .setCode(Code.INTERNAL_VALUE)
                  .setMessage("Thread interrupted while UAC retrying call")
                  .build();
          throw StatusProto.toStatusRuntimeException(status);
        }
        return retryCallInterface.retryCall(retry);
      }

      Status status =
          Status.newBuilder().setCode(Code.UNAVAILABLE_VALUE).setMessage(errorMessage).build();
      throw StatusProto.toStatusRuntimeException(status);
    }
    throw ex;
  }
}
