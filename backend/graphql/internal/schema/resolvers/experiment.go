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

type experimentResolver struct{ *Resolver }

func (r *experimentResolver) Project(ctx context.Context, obj *ai_verta_modeldb.Experiment) (*ai_verta_modeldb.Project, error) {
	return getProjectById(r.Logger.With(zap.String("experiment", obj.GetId())), ctx, r.Connections, obj.GetProjectId())
}
func (r *experimentResolver) DateCreated(ctx context.Context, obj *ai_verta_modeldb.Experiment) (string, error) {
	return strconv.FormatInt(obj.GetDateCreated(), 10), nil
}
func (r *experimentResolver) DateUpdated(ctx context.Context, obj *ai_verta_modeldb.Experiment) (string, error) {
	return strconv.FormatInt(obj.GetDateUpdated(), 10), nil
}
func (r *experimentResolver) Owner(ctx context.Context, obj *ai_verta_modeldb.Experiment) (*ai_verta_uac.UserInfo, error) {
	return dataloaders.GetUserById(ctx, obj.GetOwner())
}
func (r *experimentResolver) Attributes(ctx context.Context, obj *ai_verta_modeldb.Experiment) ([]schema.KeyValue, error) {
	return keyValueSliceConverter(ctx, obj.GetAttributes())
}
func (r *experimentResolver) Runs(ctx context.Context, obj *ai_verta_modeldb.Experiment, next *string, query *schema.ExperimentRunsQuery) (*schema.ExperimentRuns, error) {
	// TODO: add pagination
	res, err := r.Connections.ExperimentRun.GetExperimentRunsInExperiment(
		ctx,
		&ai_verta_modeldb.GetExperimentRunsInExperiment{ExperimentId: obj.GetId()},
	)
	if err != nil {
		r.Logger.Error("failed to get experiment runs", zap.Error(err), zap.String("experiment", obj.GetId()))
		return nil, err
	}
	return &schema.ExperimentRuns{
		Runs: res.GetExperimentRuns(),
	}, nil
}
