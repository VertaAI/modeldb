package resolvers

import (
	"context"

	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	ai_verta_modeldb "github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb"
)

type artifactResolver struct{ *Resolver }

func (r *artifactResolver) Type(ctx context.Context, obj *ai_verta_modeldb.Artifact) (schema.ArtifactType, error) {
	return schema.ArtifactType(obj.GetArtifactType().String()), nil
}
