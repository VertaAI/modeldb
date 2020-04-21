package resolvers

import (
	"context"
	"strconv"

	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/dataloaders"
	ai_verta_uac "github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
	"go.uber.org/zap"
)

type teamResolver struct{ *Resolver }

func (r *teamResolver) OrganizationID(ctx context.Context, obj *ai_verta_uac.Team) (string, error) {
	return obj.GetOrgId(), nil
}
func (r *teamResolver) Organization(ctx context.Context, obj *ai_verta_uac.Team) (*ai_verta_uac.Organization, error) {
	return getOrganizationById(r.Logger.With(zap.String("team", obj.GetId())), ctx, r.Connections, obj.GetOrgId())
}
func (r *teamResolver) Owner(ctx context.Context, obj *ai_verta_uac.Team) (*ai_verta_uac.UserInfo, error) {
	return dataloaders.GetUserById(ctx, obj.GetOwnerId())
}
func (r *teamResolver) DateCreated(ctx context.Context, obj *ai_verta_uac.Team) (string, error) {
	return strconv.FormatUint(uint64(obj.GetCreatedTimestamp()), 10), nil
}
func (r *teamResolver) DateUpdated(ctx context.Context, obj *ai_verta_uac.Team) (string, error) {
	return strconv.FormatUint(uint64(obj.GetUpdatedTimestamp()), 10), nil
}
