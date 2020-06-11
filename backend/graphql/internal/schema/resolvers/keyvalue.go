package resolvers

import (
	"context"

	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/errors"
	ai_verta_common "github.com/VertaAI/modeldb/protos/gen/go/protos/public/common"
	proto3 "github.com/golang/protobuf/ptypes/struct"
	"github.com/vektah/gqlparser/gqlerror"
)

func keyValueConverter(ctx context.Context, obj *ai_verta_common.KeyValue) (schema.KeyValue, *gqlerror.Error) {
	if x, ok := obj.GetValue().GetKind().(*proto3.Value_StringValue); ok && x != nil {
		return &schema.StringKeyValue{
			Key:   obj.GetKey(),
			Value: x.StringValue,
		}, nil
	}
	if x, ok := obj.GetValue().GetKind().(*proto3.Value_NumberValue); ok && x != nil {
		return &schema.FloatKeyValue{
			Key:   obj.GetKey(),
			Value: x.NumberValue,
		}, nil
	}
	return nil, errors.UnknownTypeForValue(ctx, obj.GetValueType())
}

func keyValueSliceConverter(ctx context.Context, objs []*ai_verta_common.KeyValue) ([]schema.KeyValue, *gqlerror.Error) {
	ret := make([]schema.KeyValue, len(objs))
	for i, obj := range objs {
		newObj, err := keyValueConverter(ctx, obj)
		if err != nil {
			return nil, errors.AtPosition(ctx, err.Message, i)
		}
		ret[i] = newObj
	}
	return ret, nil
}
