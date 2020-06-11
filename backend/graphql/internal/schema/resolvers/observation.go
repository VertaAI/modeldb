package resolvers

import (
	"context"

	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/common"
	ai_verta_modeldb "github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb"
)

type observationResolver struct{ *Resolver }

func (r *observationResolver) Attribute(ctx context.Context, obj *ai_verta_modeldb.Observation) (schema.KeyValue, error) {
	return keyValueConverter(ctx, obj.GetAttribute())
}
func (r *observationResolver) Artifact(ctx context.Context, obj *ai_verta_modeldb.Observation) (*common.Artifact, error) {
	return obj.GetArtifact(), nil
}
func (r *observationResolver) Timestamp(ctx context.Context, obj *ai_verta_modeldb.Observation) (*string, error) {
	if obj == nil {
		return nil, nil
	}
	ret := string(obj.Timestamp)
	return &ret, nil
}
