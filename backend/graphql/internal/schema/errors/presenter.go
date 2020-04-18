package errors

import (
	"context"

	"github.com/99designs/gqlgen/graphql"
	"github.com/vektah/gqlparser/gqlerror"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

func Presenter(ctx context.Context, e error) *gqlerror.Error {
	// Check if we have a gRPC error
	grpcStatus := status.Convert(e)
	if grpcStatus.Code() != codes.Unknown {
		code := grpcStatus.Code()
		if code == codes.Canceled {
			return gqlerror.ErrorPathf(graphql.GetResolverContext(ctx).Path(), "Request was cancelled")
		}
		if code == codes.InvalidArgument {
			if grpcStatus.Message() != "" {
				return gqlerror.ErrorPathf(graphql.GetResolverContext(ctx).Path(), "Invalid argument: "+grpcStatus.Message())
			}
			return gqlerror.ErrorPathf(graphql.GetResolverContext(ctx).Path(), "Invalid argument")
		}
		if code == codes.DeadlineExceeded {
			return gqlerror.ErrorPathf(graphql.GetResolverContext(ctx).Path(), "Deadline exceeded")
		}
		if code == codes.NotFound {
			if grpcStatus.Message() != "" {
				return gqlerror.ErrorPathf(graphql.GetResolverContext(ctx).Path(), "Not found: "+grpcStatus.Message())
			}
			return gqlerror.ErrorPathf(graphql.GetResolverContext(ctx).Path(), "Not found")
		}
		if code == codes.PermissionDenied {
			return gqlerror.ErrorPathf(graphql.GetResolverContext(ctx).Path(), "Permission denied")
		}
		if code == codes.Aborted {
			return gqlerror.ErrorPathf(graphql.GetResolverContext(ctx).Path(), "Aborted")
		}
		if code == codes.Internal {
			if grpcStatus.Message() != "" {
				return gqlerror.ErrorPathf(graphql.GetResolverContext(ctx).Path(), "Internal server error, please report: "+grpcStatus.Message())
			}
			return gqlerror.ErrorPathf(graphql.GetResolverContext(ctx).Path(), "Internal server error, please report")
		}
		if code == codes.Unavailable {
			return gqlerror.ErrorPathf(graphql.GetResolverContext(ctx).Path(), "Unavailable")
		}
		if code == codes.Unauthenticated {
			return gqlerror.ErrorPathf(graphql.GetResolverContext(ctx).Path(), "Unauthenticated")
		}
		if code == codes.AlreadyExists {
			if grpcStatus.Message() != "" {
				return gqlerror.ErrorPathf(graphql.GetResolverContext(ctx).Path(), "Already exists: "+grpcStatus.Message())
			}
			return gqlerror.ErrorPathf(graphql.GetResolverContext(ctx).Path(), "Already exists")
		}
		if grpcStatus.Message() != "" {
			return gqlerror.ErrorPathf(graphql.GetResolverContext(ctx).Path(), "Unknown server error, please report: "+grpcStatus.Err().Error())
		}
		return gqlerror.ErrorPathf(graphql.GetResolverContext(ctx).Path(), "Unknown server error, please report")
	}

	return graphql.DefaultErrorPresenter(ctx, e)
}
