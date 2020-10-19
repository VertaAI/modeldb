package resolvers

import (
	"context"
	"sync"

	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/dataloaders"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/errors"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/models"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/common"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
	"go.uber.org/zap"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

type userCollaboratorResolver struct{ *Resolver }

func (r *userCollaboratorResolver) User(ctx context.Context, obj *models.UserCollaborator) (*uac.UserInfo, error) {
	res, err := dataloaders.GetUserById(ctx, obj.GetVertaId())
	if err != nil {
		r.Logger.Error("failed to get user", zap.Error(err), zap.String("user", obj.GetVertaId()))
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
		r.Logger.Error("failed to get team", zap.Error(err), zap.String("team", obj.GetVertaId()))
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
			err := errors.UnknownCollaboratorType(ctx, c.GetAuthzEntityType())
			r.Logger.Error(err.Error())
			return nil, err
		}
	}
	return ret, nil
}

func getCollaborators(r *Resolver, ctx context.Context, id string, getter CollaboratorGetter) ([]*uac.GetCollaboratorResponse, error) {
	res, err := getter(
		ctx,
		&uac.GetCollaborator{EntityId: id},
	)
	if err != nil {
		r.Logger.Error("failed to get collaborators", zap.Error(err), zap.String("entity", id))
		return nil, err
	}
	return res.GetSharedUsers(), nil
}

type CollaboratorGetter func(ctx context.Context, in *uac.GetCollaborator, opts ...grpc.CallOption) (*uac.GetCollaborator_Response, error)

type collaboratorReference struct {
	teamID string
	userID string
}

func (r *Resolver) resolveCollaborator(ctx context.Context, ref schema.CollaboratorReference) (*collaboratorReference, error) {
	ret := collaboratorReference{}
	var gerr error

	var wg sync.WaitGroup

	wg.Add(3)

	go func() {
		defer wg.Done()
		if ref.UsernameOrEmail != nil && r.Connections.HasUac() {
			res, err := r.Connections.UAC.GetUser(ctx, &uac.GetUser{
				Email: *ref.UsernameOrEmail,
			})
			if err != nil && status.Code(err) != codes.NotFound {
				gerr = err
			} else if err == nil {
				ret.userID = res.GetVertaInfo().GetUserId()
			}
		}
	}()

	go func() {
		defer wg.Done()
		if ref.UsernameOrEmail != nil && r.Connections.HasUac() {
			res, err := r.Connections.UAC.GetUser(ctx, &uac.GetUser{
				Username: *ref.UsernameOrEmail,
			})
			if err != nil && status.Code(err) != codes.NotFound {
				gerr = err
			} else if err == nil {
				ret.userID = res.GetVertaInfo().GetUserId()
			}
		}
	}()

	go func() {
		defer wg.Done()
		if ref.TeamID != nil && r.Connections.HasUac() {
			res, err := r.Connections.Team.GetTeamById(ctx, &uac.GetTeamById{
				TeamId: *ref.TeamID,
			})
			if err != nil && status.Code(err) != codes.NotFound {
				gerr = err
			} else if err == nil {
				ret.teamID = res.GetTeam().GetId()
			}
		}
	}()

	wg.Wait()

	return &ret, gerr
}
