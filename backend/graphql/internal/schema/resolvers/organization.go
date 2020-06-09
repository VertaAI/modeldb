package resolvers

import (
	"context"
	"strconv"

	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/dataloaders"
	"github.com/VertaAI/modeldb/backend/graphql/internal/server/connections"
	ai_verta_uac "github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
	"go.uber.org/zap"
)

type organizationResolver struct{ *Resolver }

func (r *organizationResolver) Owner(ctx context.Context, obj *ai_verta_uac.Organization) (*ai_verta_uac.UserInfo, error) {
	return dataloaders.GetUserById(ctx, obj.GetOwnerId())
}
func (r *organizationResolver) DateCreated(ctx context.Context, obj *ai_verta_uac.Organization) (string, error) {
	return strconv.FormatUint(uint64(obj.GetCreatedTimestamp()), 10), nil
}
func (r *organizationResolver) DateUpdated(ctx context.Context, obj *ai_verta_uac.Organization) (string, error) {
	return strconv.FormatUint(uint64(obj.GetUpdatedTimestamp()), 10), nil
}
func (r *organizationResolver) Teams(ctx context.Context, obj *ai_verta_uac.Organization) ([]*ai_verta_uac.Team, error) {
	if r.Connections.HasUac() {
		res, err := r.Connections.Organization.ListTeams(
			ctx,
			&ai_verta_uac.ListTeams{OrgId: obj.GetId()},
		)
		if err != nil {
			r.Logger.Error("failed to get teams", zap.Error(err))
			return nil, err
		}

		ids := res.GetTeamIds()
		teams := make([]*ai_verta_uac.Team, len(ids))
		for i, id := range ids {
			team, err := dataloaders.GetTeamById(ctx, id)
			if err != nil {
				r.Logger.Error("failed to get team", zap.Error(err), zap.String("team", ids[i]))
				return nil, err
			}
			teams[i] = team
		}
		return teams, nil
	}
	return []*ai_verta_uac.Team{}, nil
}

func getOrganizationById(logger *zap.Logger, ctx context.Context, connections *connections.Connections, id string) (*ai_verta_uac.Organization, error) {
	if connections.HasUac() {
		res, err := connections.Organization.GetOrganizationById(
			ctx,
			&ai_verta_uac.GetOrganizationById{OrgId: id},
		)
		if err != nil {
			logger.Error("failed to get organization", zap.Error(err))
			return nil, err
		}
		return res.GetOrganization(), nil
	}
	return &ai_verta_uac.Organization{}, nil
}
