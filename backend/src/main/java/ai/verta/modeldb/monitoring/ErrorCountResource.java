package ai.verta.modeldb.monitoring;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.prometheus.client.Counter;

public class ErrorCountResource {
  private static String subSystemName = "verta_backend";

  private ErrorCountResource() {}

  private static final Counter failedRequests =
      Counter.build()
          .subsystem(subSystemName)
          .name("failed_requests_total")
          .help("Total failed requests on the server.")
          .register();

  private static final Counter failed_4XX_Requests =
      Counter.build()
          .subsystem(subSystemName)
          .name("4XX_failed_requests_total")
          .help("Total 4XX failed requests on the server.")
          .register();

  private static final Counter failed_5XX_Requests =
      Counter.build()
          .subsystem(subSystemName)
          .name("5XX_failed_requests_total")
          .help("Total 5XX failed requests on the server.")
          .register();

  public static void inc(Throwable ex) {
    failedRequests.inc();
    if (ex instanceof StatusRuntimeException) {
      Status status = Status.fromThrowable(ex);
      registerFailedRequestCount(status);
    } else {
      failed_5XX_Requests.inc();
    }
  }

  private static void registerFailedRequestCount(Status status) {
    switch (status.getCode().value()) {
      case 0: // OK : 200 OK
        break;
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
        failed_4XX_Requests.inc();
        break;
      case 2: // UNKNOWN: 500 Internal Server Error
      case 4: // DEADLINE_EXCEEDED: 504 Gateway Timeout
      case 12: // UNIMPLEMENTED: 501 Not Implemented
      case 13: // INTERNAL: 500 Internal Server Error
      case 14: // UNAVAILABLE: 503 Service Unavailable
      case 15: // DATA_LOSS: 500 Internal Server Error
        failed_5XX_Requests.inc();
        break;
      default:
        failed_5XX_Requests.inc();
    }
  }
}
