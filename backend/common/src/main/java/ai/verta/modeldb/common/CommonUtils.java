package ai.verta.modeldb.common;

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
