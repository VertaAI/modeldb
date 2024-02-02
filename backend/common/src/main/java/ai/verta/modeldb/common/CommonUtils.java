package ai.verta.modeldb.common;

import ai.verta.common.Pagination;
import ai.verta.modeldb.common.exceptions.InternalErrorException;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.common.exceptions.UnavailableException;
import ai.verta.modeldb.common.futures.Handle;
import ai.verta.modeldb.common.query.OrderColumn;
import ai.verta.modeldb.common.query.QueryFilterContext;
import com.amazonaws.AmazonServiceException;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.statement.Query;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

  @SuppressWarnings({"squid:S112"})
  public static Message.Builder getProtoObjectFromString(
      String jsonString, Message.Builder builder) {
    try {
      JsonFormat.parser().merge(jsonString, builder);
      return builder;
    } catch (InvalidProtocolBufferException ex) {
      LOGGER.warn("Error generating builder for {}", jsonString, ex);
      throw new RuntimeException(ex);
    }
  }

  public static String getStringFromProtoObject(MessageOrBuilder object) {
    try {
      return JsonFormat.printer().preservingProtoFieldNames().print(object);
    } catch (InvalidProtocolBufferException ex) {
      LOGGER.warn("Error generating while convert MessageOrBuilder to string", ex);
      throw new RuntimeException(ex);
    }
  }

  public static void logAmazonServiceExceptionErrorCodes(Logger logger, AmazonServiceException e) {
    logger.debug("Amazon Service Status Code: {}", e.getStatusCode());
    logger.debug("Amazon Service Error Code: {}", e.getErrorCode());
    logger.debug("Amazon Service Error Type: {}", e.getErrorType());
    logger.debug("Amazon Service Error Message: {}", e.getErrorMessage());
  }

  public static boolean isEnvSet(String envVar) {
    String envVarVal = System.getenv(envVar);
    return envVarVal != null && !envVarVal.isEmpty();
  }

  public static void scheduleTask(
      Runnable task, long initialDelay, long frequency, TimeUnit timeUnit) {
    ScheduledExecutorService executor =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r);
              t.setName(task.getClass().getSimpleName());
              t.setDaemon(true);
              return t;
            });
    executor.scheduleAtFixedRate(task, initialDelay, frequency, timeUnit);
  }

  public static ModelDBException getInvalidFieldException(Exception ex) {
    if (ex instanceof IllegalArgumentException) {
      throw (IllegalArgumentException) ex;
    }

    String invalidFieldName;
    if (ex != null && ex.getMessage() != null && ex.getMessage().contains("Unknown column ")) {
      var invalidFieldNameArr = ex.getMessage().split("'");
      invalidFieldName = invalidFieldNameArr[1].substring(3);
    } else if (ex != null
        && ex.getMessage() != null
        && ex.getMessage().contains("Invalid column ")) {
      // Logic for MSSQL
      invalidFieldName = ex.getMessage();
      invalidFieldName = invalidFieldName.substring("Invalid column name '".length());
      invalidFieldName = invalidFieldName.substring(0, invalidFieldName.indexOf("'"));
    } else if (ex != null && ex.getMessage() != null && ex.getMessage().contains("Column ")) {
      // Logic for H2 Database
      invalidFieldName = ex.getMessage();
      invalidFieldName = invalidFieldName.substring("Column '*..".length());
      invalidFieldName = invalidFieldName.substring(0, invalidFieldName.indexOf("\""));
    } else {
      throw new ModelDBException(ex);
    }

    return new ModelDBException(
        "Invalid field found in the request : " + invalidFieldName, Code.INVALID_ARGUMENT);
  }

  public interface RetryCallInterface<T> {
    T retryCall(boolean retry);
  }

  public static <T> T retryOrThrowException(
      StatusRuntimeException ex,
      boolean retry,
      RetryCallInterface<T> retryCallInterface,
      Integer requestTimeout) {
    String errorMessage = ex.getMessage();
    LOGGER.debug(errorMessage);
    if (ex.getStatus().getCode().value() == Code.UNAVAILABLE_VALUE) {
      errorMessage = "UAC Service unavailable : " + errorMessage;
      if (retry && retryCallInterface != null) {
        try {
          Thread.sleep(requestTimeout.longValue() * 1000L);
          retry = false;
        } catch (InterruptedException e) {
          // Restore interrupted state...
          Thread.currentThread().interrupt();
          throw new InternalErrorException("Thread interrupted while UAC retrying call");
        }
        return retryCallInterface.retryCall(retry);
      }

      throw new UnavailableException(errorMessage);
    }
    throw ex;
  }

  public static <T extends GeneratedMessageV3> StatusRuntimeException logError(Throwable e) {
    Status status;
    StatusRuntimeException statusRuntimeException;
    boolean isClientError = false;
    if (e instanceof StatusRuntimeException) {
      statusRuntimeException = (StatusRuntimeException) e;
    } else if (e instanceof CompletionException) {
      CompletionException ex = (CompletionException) e;
      return logError(ex.getCause());
    } else {
      if (e == null) {
        var status1 =
            Status.newBuilder()
                .setCode(Code.INTERNAL_VALUE)
                .setMessage("Exception found null")
                .build();
        return StatusProto.toStatusRuntimeException(status1);
      }
      var throwable = findRootCause(e);
      // Condition 'throwable != null' covered by below condition 'throwable instanceof
      // SocketException'
      StackTraceElement[] stack = e.getStackTrace();
      if (throwable instanceof SocketException) {
        var errorMessage = "Database Connection not found: ";
        LOGGER.info("{} {}", errorMessage, e.getMessage());
        status =
            Status.newBuilder()
                .setCode(Code.UNAVAILABLE_VALUE)
                .setMessage(errorMessage + throwable.getMessage())
                .build();
      } else if (e instanceof ModelDBException) {
        var modelDBException = (ModelDBException) e;
        isClientError = isClientError(modelDBException.getCode().value());
        logBasedOnTheErrorCode(isClientError, modelDBException);
        status =
            Status.newBuilder()
                .setCode(modelDBException.getCode().value())
                .setMessage(modelDBException.getMessage())
                .build();
      } else if (e instanceof IllegalArgumentException) {
        var illegalArgumentException = (IllegalArgumentException) e;
        isClientError = isClientError(Code.INVALID_ARGUMENT_VALUE);
        logBasedOnTheErrorCode(isClientError, illegalArgumentException);
        status =
            Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(illegalArgumentException.getMessage())
                .build();
      } else {
        LOGGER.error(
            "Stacktrace with {} elements for {} {}", stack.length, e.getClass(), e.getMessage());
        status =
            Status.newBuilder()
                .setCode(Code.INTERNAL_VALUE)
                .setMessage("Internal server error")
                .build();
      }
      var n = 0;
      boolean isLongStack = stack.length > STACKTRACE_LENGTH;
      if (isLongStack) {
        for (; n < STACKTRACE_LENGTH + 1; ++n) {
          if (isClientError) {
            LOGGER.debug("{}: {}", n, stack[n]);
          } else {
            LOGGER.warn("{}: {}", n, stack[n]);
          }
        }
      }
      for (; n < stack.length; ++n) {
        if (stack[n].getClassName().startsWith("ai.verta") || !isLongStack) {
          if (isClientError) {
            LOGGER.debug("{}: {}", n, stack[n]);
          } else {
            LOGGER.warn("{}: {}", n, stack[n]);
          }
        }
      }
      statusRuntimeException = StatusProto.toStatusRuntimeException(status);
    }

    return statusRuntimeException;
  }

  public static <T extends GeneratedMessageV3> void observeError(
      StreamObserver<T> responseObserver, Throwable e) {
    responseObserver.onError(logError(e));
  }

  public static <T> void observeError(CompletableFuture<T> completableFuture, Throwable e) {
    completableFuture.completeExceptionally(observeErrorWithResponse(e, e.getMessage(), LOGGER));
  }

  public static ResponseStatusException observeErrorWithResponse(
      Throwable e, String message, Logger logger) {
    if (e instanceof ModelDBException
        && e.getCause() instanceof ExecutionException
        && e.getCause().getCause() != null) {
      return observeErrorWithResponse(e.getCause().getCause(), message, logger);
    }

    if (e instanceof CompletionException) {
      return observeErrorWithResponse(e.getCause(), message, logger);
    }
    final HttpStatus code;
    if (e instanceof StatusRuntimeException) {
      StatusRuntimeException ex = (StatusRuntimeException) e;
      code = ModelDBException.httpStatusFromCode(ex.getStatus().getCode());
      if (ex.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
        logger.debug(e.getMessage());
      } else {
        logger.error(e.getMessage());
      }
      message += " Details: " + e.getMessage();
      printStackTrace(logger, e);
    } else if (e instanceof IllegalArgumentException) {
      code = HttpStatus.BAD_REQUEST;
      String logMessage = "Common Service Exception occurred: {}";
      message = e.getMessage();
      logger.debug(logMessage, message);
    } else if (e instanceof ModelDBException modelDBException) {
      code = modelDBException.getHttpCode();
      // remove duplicate information from the way we are extracting causes/messages
      if (message.startsWith(e.getClass().getName()) && message.endsWith(e.getMessage())) {
        message = modelDBException.getMessage();
      } else {
        message += " Details: " + modelDBException.getMessage();
      }
      String logMessage = "Common Service Exception occurred: {}";
      if (modelDBException.getCodeValue() == Code.INTERNAL_VALUE) {
        // If getting 500 then we will not expose about error to the user.
        message = "Internal Server Error";
        logger.error(logMessage, e.getMessage());
        printStackTrace(logger, e);
      } else {
        if (modelDBException.getCodeValue() == Code.RESOURCE_EXHAUSTED_VALUE) {
          message = e.getMessage();
        }
        logger.debug(logMessage, e.getMessage());
      }
    } else {
      code = HttpStatus.INTERNAL_SERVER_ERROR;
      // If getting 500 then we will not expose about error to the user.
      message = "Internal Server Error";
      logger.error(e.getMessage());
      printStackTrace(logger, e);
    }
    return new ResponseStatusException(code, message, e);
  }

  public static <T extends GeneratedMessageV3> void observeError(
      StreamObserver<T> responseObserver, Exception e) {
    responseObserver.onError(logError(e));
  }

  public static void logBasedOnTheErrorCode(boolean isClientError, Throwable e) {
    if (isClientError) {
      LOGGER.debug("Exception occurred:{} {}", e.getClass(), e.getMessage());
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
    var rootCause = throwable;
    while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
      rootCause = rootCause.getCause();
    }
    return rootCause;
  }

  public static void printStackTrace(Logger logger, Throwable t) {
    StackTraceElement[] stack = t.getStackTrace();
    logger.error("Stacktrace with {} elements for {}", stack.length, t);
    int n = 0;
    boolean isLongStack = stack.length > STACKTRACE_LENGTH;
    if (isLongStack) {
      for (; n < STACKTRACE_LENGTH + 1; ++n) {
        logger.warn("{}: {}", n, stack[n].toString());
      }
    }
    for (; n < stack.length; ++n) {
      if (stack[n].getClassName().startsWith("ai.verta") || !isLongStack) {
        logger.warn("{}: {}", n, stack[n].toString());
      }
    }
  }

  public static Query buildQueryFromQueryContext(
      String alias,
      Pagination pagination,
      QueryFilterContext queryContext,
      Handle handle,
      String queryStr,
      boolean isMssql) {
    // Add the sorting tables
    for (final var item : new EnumerateList<>(queryContext.getOrderItems()).getList()) {
      if (item.getValue().getTable() != null) {
        queryStr +=
            String.format(
                " left join (%s) as join_table_%d on %s.id=join_table_%d.entityId ",
                item.getValue().getTable(), item.getIndex(), alias, item.getIndex());
      }
    }

    if (!queryContext.getConditions().isEmpty()) {
      queryStr += " WHERE " + String.join(" AND ", queryContext.getConditions());
    }

    if (!queryContext.getOrderItems().isEmpty()) {
      queryStr += " ORDER BY ";
      List<String> orderColumnQueryString = new ArrayList<>();
      for (final var item : new EnumerateList<>(queryContext.getOrderItems()).getList()) {
        if (item.getValue().getTable() != null) {
          for (OrderColumn orderColumn : item.getValue().getColumns()) {
            var orderColumnStr =
                String.format(" join_table_%d.%s ", item.getIndex(), orderColumn.getColumn());
            orderColumnStr += String.format(" %s ", orderColumn.getAscending() ? "ASC" : "DESC");
            orderColumnQueryString.add(orderColumnStr);
          }
        } else if (item.getValue().getColumn() != null) {
          var orderColumnStr = String.format(" %s ", item.getValue().getColumn());
          orderColumnStr += String.format(" %s ", item.getValue().getAscending() ? "ASC" : "DESC");
          orderColumnQueryString.add(orderColumnStr);
        }
      }
      queryStr += String.join(",", orderColumnQueryString);
    }

    // Backwards compatibility: fetch everything
    if (pagination.getPageNumber() != 0 && pagination.getPageLimit() != 0) {
      final var offset = (pagination.getPageNumber() - 1) * pagination.getPageLimit();
      final var limit = pagination.getPageLimit();
      if (isMssql) {
        queryStr += " OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY ";
      } else {
        queryStr += " LIMIT :limit OFFSET :offset";
      }
      queryContext.addBind(q -> q.bind("limit", limit));
      queryContext.addBind(q -> q.bind("offset", offset));
    }

    var query = handle.createQuery(queryStr);
    queryContext.getBinds().forEach(b -> b.accept(query));
    return query;
  }

  public static void cleanUpPIDFile() {
    var path = System.getProperty("user.dir") + "/" + "verta-backend.pid";
    File pidFile = new File(path);
    if (pidFile.exists()) {
      pidFile.deleteOnExit();
      LOGGER.trace("verta-backend.pid" + " file is deleted: {}", pidFile.exists());
    }
  }
}
