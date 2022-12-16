package ai.verta.modeldb.utils;

import static org.assertj.core.api.Assertions.assertThat;

import ai.verta.modeldb.common.CommonUtils;
import ai.verta.modeldb.common.exceptions.ModelDBException;
import com.google.rpc.Code;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;

@RunWith(MockitoJUnitRunner.class)
public class CommonUtilsTest {
  private static final Logger LOGGER = LogManager.getLogger(CommonUtilsTest.class);

  @Test
  public void getHydratedCollaboratorUserInfo() {
    var responseStatusException =
        CommonUtils.observeErrorWithResponse(
            new ModelDBException("internal exception"), "internal exception message", LOGGER);
    assertThat(responseStatusException.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(responseStatusException.getReason()).isEqualTo("Internal Server Error");

    responseStatusException =
        CommonUtils.observeErrorWithResponse(
            new ModelDBException("not found exception", Code.NOT_FOUND),
            "not found exception message",
            LOGGER);
    assertThat(responseStatusException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);

    responseStatusException =
        CommonUtils.observeErrorWithResponse(
            new StatusRuntimeException(Status.UNIMPLEMENTED),
            "unimplemented exception message",
            LOGGER);
    assertThat(responseStatusException.getStatus()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);

    responseStatusException =
        CommonUtils.observeErrorWithResponse(
            new StatusRuntimeException(Status.FAILED_PRECONDITION),
            "bad request exception message",
            LOGGER);
    assertThat(responseStatusException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);

    responseStatusException =
        CommonUtils.observeErrorWithResponse(
            new StatusRuntimeException(Status.ALREADY_EXISTS),
            "already exists exception message",
            LOGGER);
    assertThat(responseStatusException.getStatus()).isEqualTo(HttpStatus.CONFLICT);

    responseStatusException =
        CommonUtils.observeErrorWithResponse(
            new StatusRuntimeException(Status.PERMISSION_DENIED),
            "permission denied exception message",
            LOGGER);
    assertThat(responseStatusException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);

    responseStatusException =
        CommonUtils.observeErrorWithResponse(
            new StatusRuntimeException(Status.UNAVAILABLE),
            "permission denied exception message",
            LOGGER);
    assertThat(responseStatusException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

    responseStatusException =
        CommonUtils.observeErrorWithResponse(
            new StatusRuntimeException(Status.INVALID_ARGUMENT),
            "permission denied exception message",
            LOGGER);
    assertThat(responseStatusException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
