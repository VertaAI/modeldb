package resolvers

import (
	"context"

	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/common"
)

type artifactResolver struct{ *Resolver }

func (r *artifactResolver) Type(ctx context.Context, obj *common.Artifact) (schema.ArtifactType, error) {
	return schema.ArtifactType(obj.GetArtifactType().String()), nil
}
