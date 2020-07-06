package resolvers

import (
	"context"
	"strings"

	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/errors"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/common"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
	"go.uber.org/zap"
	"google.golang.org/protobuf/types/known/structpb"
)

func (r *workspaceResolver) resolveMDBPredicates(ctx context.Context, stringPredicates []*schema.StringPredicate, floatPredicates []*schema.FloatPredicate) ([]*modeldb.KeyValueQuery, error) {
	predicates := make([]*modeldb.KeyValueQuery, 0)

	for i, pred := range stringPredicates {
		op, ok := modeldb.OperatorEnum_Operator_value[pred.Operator.String()]
		if !ok {
			return nil, errors.UnknownOperator(ctx, pred.Operator.String())
		}

		if pred.Key == "owner" {
			if !r.Connections.HasUac() {
				stringPredicates[i] = nil
			} else {
				switch modeldb.OperatorEnum_Operator(op) {
				case modeldb.OperatorEnum_CONTAIN:
					res, err := r.Connections.UAC.GetUsersFuzzy(ctx, &uac.GetUsersFuzzy{
						Username: pred.Value,
					})
					if err != nil {
						r.Logger.Error("failed to get users", zap.Error(err))
						return nil, err
					}
					if len(res.GetUserInfos()) == 0 {
						return nil, errors.NoUserFound(ctx)
					}
					ids := make([]string, len(res.GetUserInfos()))
					for i, user := range res.GetUserInfos() {
						ids[i] = user.GetVertaInfo().GetUserId()
					}
					pred.Value = strings.Join(ids, ",")
					pred.Operator = schema.PredicateOperatorIn
				case modeldb.OperatorEnum_IN:
					usernames := strings.Split(pred.Value, ",")
					res, err := r.Connections.UAC.GetUsers(ctx, &uac.GetUsers{
						Usernames: usernames,
					})
					if err != nil {
						r.Logger.Error("failed to get users", zap.Error(err))
						return nil, err
					}
					ids := make([]string, len(res.GetUserInfos()))
					for i, user := range res.GetUserInfos() {
						ids[i] = user.GetVertaInfo().GetUserId()
					}
					pred.Value = strings.Join(ids, ",")
				case modeldb.OperatorEnum_EQ:
					fallthrough
				case modeldb.OperatorEnum_NE:
					res, err := r.Connections.UAC.GetUser(ctx, &uac.GetUser{
						Username: pred.Value,
					})
					if err != nil {
						r.Logger.Error("failed to get user", zap.Error(err))
						return nil, err
					}
					pred.Value = res.VertaInfo.GetUserId()
				case modeldb.OperatorEnum_LTE:
					fallthrough
				case modeldb.OperatorEnum_LT:
					fallthrough
				case modeldb.OperatorEnum_GTE:
					fallthrough
				case modeldb.OperatorEnum_GT:
					fallthrough
				case modeldb.OperatorEnum_NOT_CONTAIN:
					return nil, errors.UnknownOperator(ctx, pred.Operator.String())
				}
			}
		}
	}

	for _, pred := range stringPredicates {
		if pred != nil {
			op, ok := modeldb.OperatorEnum_Operator_value[pred.Operator.String()]
			if !ok {
				return nil, errors.UnknownOperator(ctx, pred.Operator.String())
			}
			predicates = append(predicates, &modeldb.KeyValueQuery{
				Key:      pred.Key,
				Operator: modeldb.OperatorEnum_Operator(op),
				Value: &structpb.Value{
					Kind: &structpb.Value_StringValue{
						StringValue: pred.Value,
					},
				},
				ValueType: common.ValueTypeEnum_STRING,
			})
		}
	}

	for _, pred := range floatPredicates {
		if pred != nil {
			op, ok := modeldb.OperatorEnum_Operator_value[pred.Operator.String()]
			if !ok {
				return nil, errors.UnknownOperator(ctx, pred.Operator.String())
			}
			predicates = append(predicates, &modeldb.KeyValueQuery{
				Key:      pred.Key,
				Operator: modeldb.OperatorEnum_Operator(op),
				Value: &structpb.Value{
					Kind: &structpb.Value_NumberValue{
						NumberValue: pred.Value,
					},
				},
				ValueType: common.ValueTypeEnum_NUMBER,
			})
		}
	}

	return predicates, nil
}
