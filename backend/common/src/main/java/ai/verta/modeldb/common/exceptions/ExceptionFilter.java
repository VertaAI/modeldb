package ai.verta.modeldb.common.exceptions;

import ai.verta.modeldb.common.CommonUtils;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.StatusRuntimeException;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class ExceptionFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (RuntimeException ex) {
            StatusRuntimeException status = CommonUtils.logError(ex);
            final var httpCode = ModelDBException.httpStatusFromCode(status.getStatus().getCode());
            ((HttpServletResponse) servletResponse).sendError(httpCode.value(), status.getMessage());
        }
    }
}
