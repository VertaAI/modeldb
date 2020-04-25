package resolvers

import (
	"context"

	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/dataloaders"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/errors"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/models"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/common"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
	ai_verta_uac "github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
	"go.uber.org/zap"
	"google.golang.org/grpc"
)

type userCollaboratorResolver struct{ *Resolver }

func (r *userCollaboratorResolver) User(ctx context.Context, obj *models.UserCollaborator) (*ai_verta_uac.UserInfo, error) {
	res, err := dataloaders.GetUserById(ctx, obj.GetVertaId())
	if err != nil {
		r.Logger.Error("failed to get user", zap.Error(err), zap.String("user", obj.GetUserId()))
		return nil, err
	}
	return res, nil
}
func (r *userCollaboratorResolver) Type(ctx context.Context, obj *models.UserCollaborator) (schema.AccessType, error) {
	return schema.AccessType(obj.GetCollaboratorType().String()), nil
}
func (r *userCollaboratorResolver) CanDeploy(ctx context.Context, obj *models.UserCollaborator) (bool, error) {
	return obj.GetCanDeploy() == common.TernaryEnum_TRUE, nil
}

type teamCollaboratorResolver struct{ *Resolver }

func (r *teamCollaboratorResolver) Team(ctx context.Context, obj *models.TeamCollaborator) (*uac.Team, error) {
	res, err := dataloaders.GetTeamById(ctx, obj.GetVertaId())
	if err != nil {
		r.Logger.Error("failed to get team", zap.Error(err), zap.String("team", obj.GetUserId()))
		return nil, err
	}
	return res, nil
}
func (r *teamCollaboratorResolver) Type(ctx context.Context, obj *models.TeamCollaborator) (schema.AccessType, error) {
	return schema.AccessType(obj.GetCollaboratorType().String()), nil
}
func (r *teamCollaboratorResolver) CanDeploy(ctx context.Context, obj *models.TeamCollaborator) (bool, error) {
	return obj.GetCanDeploy() == common.TernaryEnum_TRUE, nil
}

func getConvertedCollaborators(r *Resolver, ctx context.Context, id string, getter CollaboratorGetter) ([]schema.Collaborator, error) {
	collaborators, err := getCollaborators(r, ctx, id, getter)
	if err != nil {
		return nil, err
	}
	ret := make([]schema.Collaborator, len(collaborators))
	for i, c := range collaborators {
		switch c.GetAuthzEntityType() {
		case common.EntitiesEnum_TEAM:
			ret[i] = models.TeamCollaborator{c}
		case common.EntitiesEnum_USER:
			ret[i] = models.UserCollaborator{c}
		default:
			err := errors.UnknownCollaboratorType(c.GetAuthzEntityType())
			r.Logger.Error(err.Error())
			return nil, err
		}
	}
	return ret, nil
}

func getCollaborators(r *Resolver, ctx context.Context, id string, getter CollaboratorGetter) ([]*ai_verta_uac.GetCollaboratorResponse, error) {
	res, err := getter(
		ctx,
		&ai_verta_uac.GetCollaborator{EntityId: id},
	)
	if err != nil {
		r.Logger.Error("failed to get collaborators", zap.Error(err), zap.String("entity", id))
		return nil, err
	}
	return res.GetSharedUsers(), nil
}

type CollaboratorGetter func(ctx context.Context, in *uac.GetCollaborator, opts ...grpc.CallOption) (*uac.GetCollaborator_Response, error)
