package errors

import (
	"context"
	"fmt"
	"net/http"

	"github.com/99designs/gqlgen/graphql"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/common"
	pcommon "github.com/VertaAI/modeldb/protos/gen/go/protos/public/common"
	"github.com/vektah/gqlparser/gqlerror"
)

func DeleteOutsideMutation(ctx context.Context) *gqlerror.Error {
	return &gqlerror.Error{
		Message: "Tried to delete outside of a mutation",
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusBadRequest,
		},
	}
}
func OwnerAccess(ctx context.Context) *gqlerror.Error {
	return &gqlerror.Error{
		Message: "Can't set collaborator type to owner",
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusBadRequest,
		},
	}
}
func UnknownVisibility(ctx context.Context, visibility common.VisibilityEnum_Visibility) *gqlerror.Error {
	return &gqlerror.Error{
		Message: fmt.Sprintf("Unknown visibility %s", visibility),
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusNotImplemented,
		},
	}
}
func CreateOutsideMutation(ctx context.Context) *gqlerror.Error {
	return &gqlerror.Error{
		Message: "Tried to create outside of a mutation",
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusBadRequest,
		},
	}
}
func UpdateOutsideMutation(ctx context.Context) *gqlerror.Error {
	return &gqlerror.Error{
		Message: "Tried to update outside of a mutation",
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusBadRequest,
		},
	}
}
func MergeOutsideMutation(ctx context.Context) *gqlerror.Error {
	return &gqlerror.Error{
		Message: "Tried to merge outside of a mutation",
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusBadRequest,
		},
	}
}
func InvalidNextToken(ctx context.Context) *gqlerror.Error {
	return &gqlerror.Error{
		Message: "Invalid next token",
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusBadRequest,
		},
	}
}
func NextOrQuery(ctx context.Context) *gqlerror.Error {
	return &gqlerror.Error{
		Message: "Only one of next or query must be present",
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusBadRequest,
		},
	}
}
func UnknownOperator(ctx context.Context, op string) error {
	return &gqlerror.Error{
		Message: fmt.Sprintf("Unknown operator \"%s\"", op),
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusBadRequest,
		},
	}
}
func UnknownWorkspaceType(ctx context.Context, workspaceType common.WorkspaceTypeEnum_WorkspaceType) error {
	return &gqlerror.Error{
		Message: fmt.Sprintf("Unknown workspace type \"%s\"", workspaceType),
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusBadRequest,
		},
	}
}
func NotSupportedOperator(ctx context.Context, op string) error {
	return &gqlerror.Error{
		Message: fmt.Sprintf("Operator \"%s\" is not supported for this kind of query", op),
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusBadRequest,
		},
	}
}
func EmptyReference(ctx context.Context) *gqlerror.Error {
	return &gqlerror.Error{
		Message: "Empty reference",
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusBadRequest,
		},
	}
}

func NoUserFound(ctx context.Context) *gqlerror.Error {
	return &gqlerror.Error{
		Message: "No user found",
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusNotFound,
		},
	}
}

func FailedToFetchAuth(ctx context.Context) *gqlerror.Error {
	return &gqlerror.Error{
		Message: "Failed to fetch auth information",
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusBadGateway,
		},
	}
}
func ModelDbInternalError(ctx context.Context) *gqlerror.Error {
	return &gqlerror.Error{
		Message: "Internal error in ModelDB; please report",
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusBadGateway,
		},
	}
}
func InvalidTypeFromModeldb(ctx context.Context) *gqlerror.Error {
	return &gqlerror.Error{
		Message: "Got invalid type from ModelDB; please report",
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusBadGateway,
		},
	}
}
func UnknownStatus(ctx context.Context) *gqlerror.Error {
	return &gqlerror.Error{
		Message: "Got unknown status; please report",
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusBadGateway,
		},
	}
}
func UnknownCollaboratorType(ctx context.Context, t pcommon.EntitiesEnum_EntitiesTypes) *gqlerror.Error {
	return &gqlerror.Error{
		Message: fmt.Sprintf("Unknown entity type \"%s\"; please report", pcommon.EntitiesEnum_EntitiesTypes_name[int32(t)]),
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusBadGateway,
		},
	}
}
func UnknownBuildStatus(ctx context.Context, status string) *gqlerror.Error {
	return &gqlerror.Error{
		Message: fmt.Sprintf("Got unknown build status \"%s\"; please report", status),
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusBadGateway,
		},
	}
}

func UnknownTypeForValue(ctx context.Context, v common.ValueTypeEnum_ValueType) *gqlerror.Error {
	return &gqlerror.Error{
		Message: fmt.Sprintf("Unknown type for value \"%s\"", v.String()),
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusBadGateway,
		},
	}
}

func AtPosition(ctx context.Context, msg string, i int) *gqlerror.Error {
	return &gqlerror.Error{
		Message: fmt.Sprintf("%s at position %d", msg, i+1),
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusBadGateway,
		},
	}
}

func InternalError(ctx context.Context) *gqlerror.Error {
	return &gqlerror.Error{
		Message: "Internal error; please report",
		Path:    graphql.GetResolverContext(ctx).Path(),
		Extensions: map[string]interface{}{
			"http-code": http.StatusInternalServerError,
		},
	}
}
