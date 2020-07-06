package resolvers

import (
	"context"

	ai_verta_uac "github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
)

type userResolver struct{ *Resolver }

func (r *userResolver) ID(ctx context.Context, obj *ai_verta_uac.UserInfo) (string, error) {
	return obj.GetVertaInfo().GetUserId(), nil
}
func (r *userResolver) Name(ctx context.Context, obj *ai_verta_uac.UserInfo) (*string, error) {
	return &obj.FullName, nil
}
func (r *userResolver) Picture(ctx context.Context, obj *ai_verta_uac.UserInfo) (*string, error) {
	img := obj.GetImageUrl()
	return &img, nil
}
func (r *userResolver) Username(ctx context.Context, obj *ai_verta_uac.UserInfo) (string, error) {
	return obj.GetVertaInfo().GetUsername(), nil
}
