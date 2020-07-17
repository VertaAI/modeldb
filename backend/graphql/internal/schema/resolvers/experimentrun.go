package resolvers

import (
	"context"
	"strconv"

	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/dataloaders"
	ai_verta_modeldb "github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb"
	ai_verta_uac "github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
	"go.uber.org/zap"
)

type experimentRunResolver struct{ *Resolver }

func (r *experimentRunResolver) Project(ctx context.Context, obj *ai_verta_modeldb.ExperimentRun) (*ai_verta_modeldb.Project, error) {
	return getProjectById(r.Logger.With(zap.String("experiment_run", obj.GetId())), ctx, r.Connections, obj.GetProjectId())
}
func (r *experimentRunResolver) Experiment(ctx context.Context, obj *ai_verta_modeldb.ExperimentRun) (*ai_verta_modeldb.Experiment, error) {
	res, err := r.Connections.Experiment.GetExperimentById(
		ctx,
		&ai_verta_modeldb.GetExperimentById{Id: obj.GetExperimentId()},
	)
	if err != nil {
		r.Logger.Error("failed to get experiment", zap.Error(err), zap.String("experiment_run", obj.GetId()))
		return nil, err
	}
	return res.GetExperiment(), nil
}
func (r *experimentRunResolver) DateCreated(ctx context.Context, obj *ai_verta_modeldb.ExperimentRun) (string, error) {
	return strconv.FormatInt(obj.GetDateCreated(), 10), nil
}
func (r *experimentRunResolver) DateUpdated(ctx context.Context, obj *ai_verta_modeldb.ExperimentRun) (string, error) {
	return strconv.FormatInt(obj.GetDateUpdated(), 10), nil
}
func (r *experimentRunResolver) Owner(ctx context.Context, obj *ai_verta_modeldb.ExperimentRun) (*ai_verta_uac.UserInfo, error) {
	return dataloaders.GetUserById(ctx, obj.GetOwner())
}
func (r *experimentRunResolver) Attributes(ctx context.Context, obj *ai_verta_modeldb.ExperimentRun) ([]schema.KeyValue, error) {
	return keyValueSliceConverter(ctx, obj.GetAttributes())
}
func (r *experimentRunResolver) Hyperparameters(ctx context.Context, obj *ai_verta_modeldb.ExperimentRun) ([]schema.KeyValue, error) {
	return keyValueSliceConverter(ctx, obj.GetHyperparameters())
}
func (r *experimentRunResolver) Metrics(ctx context.Context, obj *ai_verta_modeldb.ExperimentRun) ([]schema.KeyValue, error) {
	return keyValueSliceConverter(ctx, obj.GetMetrics())
}

// TODO: don't consume all errors to a single place
