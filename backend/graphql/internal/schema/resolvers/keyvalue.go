package resolvers

import (
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/errors"
	ai_verta_common "github.com/VertaAI/modeldb/protos/gen/go/protos/public/common"
	proto3 "github.com/golang/protobuf/ptypes/struct"
)

func keyValueConverter(obj *ai_verta_common.KeyValue) (schema.KeyValue, error) {
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
	return nil, errors.UnknownTypeForValue(obj.GetValueType())
}

func keyValueSliceConverter(objs []*ai_verta_common.KeyValue) ([]schema.KeyValue, error) {
	ret := make([]schema.KeyValue, len(objs))
	for i, obj := range objs {
		newObj, err := keyValueConverter(obj)
		if err != nil {
			return nil, errors.AtPosition(err, i)
		}
		ret[i] = newObj
	}
	return ret, nil
}
