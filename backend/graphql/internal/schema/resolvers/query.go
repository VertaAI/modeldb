package resolvers

import (
	"context"
	"strconv"

	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/dataloaders"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/models"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb"
	ai_verta_modeldb "github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb/versioning"
	ai_verta_uac "github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
	"go.uber.org/zap"
)

type queryResolver struct{ *Resolver }

func (r *queryResolver) Self(ctx context.Context) (*ai_verta_uac.UserInfo, error) {
	if r.Connections.HasUac() {
		res, err := r.Connections.UAC.GetCurrentUser(ctx, &ai_verta_uac.Empty{})
		if err != nil {
			r.Logger.Error("failed to get self", zap.Error(err))
			return nil, err
		}
		return res, nil
	}
	return dataloaders.GetUserById(ctx, "")
}

func (r *queryResolver) Organizations(ctx context.Context) ([]*ai_verta_uac.Organization, error) {
	if r.Connections.HasUac() {
		res, err := r.Connections.Organization.ListMyOrganizations(
			ctx,
			&ai_verta_uac.ListMyOrganizations{},
		)
		if err != nil {
			r.Logger.Error("failed to get organizations", zap.Error(err))
			return nil, err
		}
		return res.GetOrganizations(), nil
	}
	return []*ai_verta_uac.Organization{}, nil
}

func (r *queryResolver) Teams(ctx context.Context) ([]*ai_verta_uac.Team, error) {
	if r.Connections.HasUac() {
		res, err := r.Connections.Team.ListMyTeams(
			ctx,
			&ai_verta_uac.ListMyTeams{},
		)
		if err != nil {
			r.Logger.Error("failed to get teams", zap.Error(err))
			return nil, err
		}
		return res.GetTeams(), nil
	}
	return []*ai_verta_uac.Team{}, nil
}

func (r *queryResolver) Project(ctx context.Context, id string) (*ai_verta_modeldb.Project, error) {
	return getProjectById(r.Logger, ctx, r.Connections, id)
}

func (r *queryResolver) Experiment(ctx context.Context, id string) (*ai_verta_modeldb.Experiment, error) {
	res, err := r.Connections.Experiment.GetExperimentById(
		ctx,
		&ai_verta_modeldb.GetExperimentById{Id: id},
	)
	if err != nil {
		r.Logger.Error("failed to get experiment", zap.Error(err))
		return nil, err
	}
	return res.GetExperiment(), nil
}

func (r *queryResolver) Dataset(ctx context.Context, id string) (*modeldb.Dataset, error) {
	res, err := r.Connections.Dataset.GetDatasetById(
		ctx,
		&ai_verta_modeldb.GetDatasetById{Id: id},
	)
	if err != nil {
		r.Logger.Error("failed to get experiment", zap.Error(err))
		return nil, err
	}
	return res.GetDataset(), nil
}

func (r *queryResolver) Run(ctx context.Context, id string) (*ai_verta_modeldb.ExperimentRun, error) {
	res, err := r.Connections.ExperimentRun.GetExperimentRunById(
		ctx,
		&ai_verta_modeldb.GetExperimentRunById{Id: id},
	)
	if err != nil {
		r.Logger.Error("failed to get experiment run", zap.Error(err))
		return nil, err
	}
	return res.GetExperimentRun(), nil
}

func (r *queryResolver) Repository(ctx context.Context, id string) (*versioning.Repository, error) {
	idInt, err := strconv.ParseUint(id, 10, 64)
	if err != nil {
		r.Logger.Error("invalid id", zap.Error(err))
		return nil, err
	}
	res, err := r.Connections.Versioning.GetRepository(
		ctx,
		&versioning.GetRepositoryRequest{Id: &versioning.RepositoryIdentification{
			RepoId: idInt,
		}},
	)
	if err != nil {
		r.Logger.Error("failed to get repository", zap.Error(err))
		return nil, err
	}
	return res.GetRepository(), nil
}

func (r *queryResolver) Organization(ctx context.Context, id string) (*ai_verta_uac.Organization, error) {
	return getOrganizationById(r.Logger, ctx, r.Connections, id)
}

func (r *queryResolver) Workspace(ctx context.Context, name *string) (*models.Workspace, error) {
	if name == nil {
		if r.Connections.HasUac() {
			res, err := r.Connections.UAC.GetCurrentUser(ctx, &ai_verta_uac.Empty{})
			if err != nil {
				r.Logger.Error("failed to get self", zap.Error(err))
				return nil, err
			}
			return &models.Workspace{Name: res.GetVertaInfo().GetUsername()}, nil
		}
		return &models.Workspace{Name: "personal"}, nil
	} else {
		return &models.Workspace{Name: *name}, nil
	}
}
