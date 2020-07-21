package resolvers

import (
	"context"
	"strconv"

	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/dataloaders"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/errors"
	"github.com/VertaAI/modeldb/backend/graphql/internal/server/connections"
	ai_verta_modeldb "github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
	"go.uber.org/zap"
	"google.golang.org/grpc"
)

type projectResolver struct{ *Resolver }

func (r *projectResolver) DateCreated(ctx context.Context, obj *ai_verta_modeldb.Project) (string, error) {
	return strconv.FormatUint(obj.GetDateCreated(), 10), nil
}
func (r *projectResolver) DateUpdated(ctx context.Context, obj *ai_verta_modeldb.Project) (string, error) {
	return strconv.FormatUint(obj.GetDateUpdated(), 10), nil
}
func (r *projectResolver) ProjectVisibility(ctx context.Context, obj *ai_verta_modeldb.Project) (schema.ProjectVisibility, error) {
	return schema.ProjectVisibility(obj.GetProjectVisibility().String()), nil
}
func (r *projectResolver) Access(ctx context.Context, obj *ai_verta_modeldb.Project) (schema.AccessType, error) {
	if r.Connections.HasUac() {
		self, ok := ctx.Value("uac-self").(*uac.UserInfo)
		if !ok {
			r.Logger.Error(errors.FailedToFetchAuth(ctx).Message)
			return "", errors.FailedToFetchAuth(ctx)
		}
		if obj.GetOwner() == self.GetUserId() {
			return schema.AccessTypeOwner, nil
		}

		// TODO: add dataloader for efficiency
		collaborators, err := getCollaborators(r.Resolver, ctx, obj.GetId(), func(ctx context.Context, in *uac.GetCollaborator, opts ...grpc.CallOption) (*uac.GetCollaborator_Response, error) {
			return r.Connections.Collaborator.GetProjectCollaborators(ctx, in, opts...)
		})
		if err != nil {
			return "", nil
		}

		for _, c := range collaborators {
			if c.GetVertaId() == self.GetVertaInfo().GetUserId() {
				return schema.AccessType(c.GetCollaboratorType().String()), nil
			}
		}

		return schema.AccessTypeReadOnly, nil
	}
	return schema.AccessTypeOwner, nil
}
func (r *projectResolver) Owner(ctx context.Context, obj *ai_verta_modeldb.Project) (*uac.UserInfo, error) {
	return dataloaders.GetUserById(ctx, obj.GetOwner())
}
func (r *projectResolver) Attributes(ctx context.Context, obj *ai_verta_modeldb.Project) ([]schema.KeyValue, error) {
	return keyValueSliceConverter(ctx, obj.GetAttributes())
}
func (r *projectResolver) Collaborators(ctx context.Context, obj *ai_verta_modeldb.Project) ([]schema.Collaborator, error) {
	if r.Connections.HasUac() {
		return getConvertedCollaborators(r.Resolver, ctx, obj.GetId(), func(ctx context.Context, in *uac.GetCollaborator, opts ...grpc.CallOption) (*uac.GetCollaborator_Response, error) {
			return r.Connections.Collaborator.GetProjectCollaborators(ctx, in, opts...)
		})
	}
	return []schema.Collaborator{}, nil
}
func (r *projectResolver) Experiments(ctx context.Context, obj *ai_verta_modeldb.Project, next *string, query *schema.ExperimentsQuery) (*schema.Experiments, error) {
	// TODO: add pagination
	res, err := r.Connections.Experiment.GetExperimentsInProject(
		ctx,
		&ai_verta_modeldb.GetExperimentsInProject{ProjectId: obj.GetId()},
	)
	if err != nil {
		r.Logger.Error("failed to get experiments", zap.Error(err), zap.String("project", obj.GetId()))
		return nil, err
	}
	return &schema.Experiments{
		Experiments: res.GetExperiments(),
	}, nil
}
func (r *projectResolver) Runs(ctx context.Context, obj *ai_verta_modeldb.Project, next *string, query *schema.ExperimentRunsQuery) (*schema.ExperimentRuns, error) {
	// TODO: add pagination
	res, err := r.Connections.ExperimentRun.GetExperimentRunsInProject(
		ctx,
		&ai_verta_modeldb.GetExperimentRunsInProject{ProjectId: obj.GetId()},
	)
	if err != nil {
		r.Logger.Error("failed to get runs", zap.Error(err), zap.String("project", obj.GetId()))
		return nil, err
	}
	return &schema.ExperimentRuns{
		Runs: res.GetExperimentRuns(),
	}, nil
}

func getProjectById(logger *zap.Logger, ctx context.Context, connections *connections.Connections, id string) (*ai_verta_modeldb.Project, error) {
	res, err := connections.Project.GetProjectById(
		ctx,
		&ai_verta_modeldb.GetProjectById{Id: id},
	)
	if err != nil {
		logger.Error("failed to get project", zap.Error(err))
		return nil, err
	}
	return res.GetProject(), nil
}
