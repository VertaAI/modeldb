package errors

import (
	"context"
	"net/http"

	"github.com/99designs/gqlgen/graphql"
	"github.com/vektah/gqlparser/gqlerror"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

// TODO: capture swagger errors too
func Presenter(ctx context.Context, e error) *gqlerror.Error {
	// If we already have a gqlerror, just send it over
	if qlerr, ok := e.(*gqlerror.Error); ok {
		return qlerr
	}

	// Check if we have a gRPC error
	grpcStatus := status.Convert(e)
	if grpcStatus.Code() != codes.Unknown {
		var message string
		var httpCode int

		code := grpcStatus.Code()
		switch code {
		case codes.Canceled:
			httpCode = http.StatusGatewayTimeout
			message = "Request was cancelled"
		case codes.InvalidArgument:
			httpCode = http.StatusBadRequest
			message = "Invalid argument"
		case codes.DeadlineExceeded:
			httpCode = http.StatusGatewayTimeout
			message = "Deadline exceeded"
		case codes.NotFound:
			httpCode = http.StatusNotFound
			message = "Not found"
		case codes.PermissionDenied:
			httpCode = http.StatusForbidden
			message = "Permission denied"
		case codes.Aborted:
			httpCode = http.StatusGatewayTimeout
			message = "Aborted"
		case codes.Internal:
			httpCode = http.StatusInternalServerError
			message = "Internal server error, please report"
		case codes.Unavailable:
			httpCode = http.StatusBadGateway
			message = "Unavailable"
		case codes.Unauthenticated:
			httpCode = http.StatusUnauthorized
			message = "Unauthenticated"
		case codes.AlreadyExists:
			httpCode = http.StatusConflict
			message = "Already exists"
		default:
			httpCode = http.StatusInternalServerError
			message = "Unknown server error, please report"
		}

		return &gqlerror.Error{
			Message: message,
			Path:    graphql.GetResolverContext(ctx).Path(),
			Extensions: map[string]interface{}{
				"http-code":        httpCode,
				"original-message": grpcStatus.Message(),
			},
		}
	}

	return graphql.DefaultErrorPresenter(ctx, e)
}
