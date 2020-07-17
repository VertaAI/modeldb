package resolvers

import (
	"context"
	"strings"

	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/errors"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/common"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
	"go.uber.org/zap"
	"google.golang.org/protobuf/types/known/structpb"
)

func (r *Resolver) resolveMDBPredicates(ctx context.Context, stringPredicates []*schema.StringPredicate, floatPredicates []*schema.FloatPredicate) ([]*common.KeyValueQuery, error) {
	predicates := make([]*common.KeyValueQuery, 0)

	for i, pred := range stringPredicates {
		op, ok := common.OperatorEnum_Operator_value[pred.Operator.String()]
		if !ok {
			return nil, errors.UnknownOperator(ctx, pred.Operator.String())
		}

		if pred.Key == "owner" {
			if !r.Connections.HasUac() {
				stringPredicates[i] = nil
			} else {
				switch common.OperatorEnum_Operator(op) {
				case common.OperatorEnum_CONTAIN:
					res, err := r.Connections.UAC.GetUsersFuzzy(ctx, &uac.GetUsersFuzzy{
						Username: pred.Value,
					})
					if err != nil {
						// Not transforming this errror to a "NULL" string as it should not be 404ing
						// We want to see this error
						r.Logger.Error("failed to get users", zap.Error(err))
						return nil, err
					}
					// TODO: This was done to match existing webapp behavior, but this will also match a user named "NULL" and should be removed
					// VR-5254
					if len(res.GetUserInfos()) == 0 {
						pred.Value = "NULL"
					} else {
						ids := make([]string, len(res.GetUserInfos()))
						for i, user := range res.GetUserInfos() {
							ids[i] = user.GetVertaInfo().GetUserId()
						}
						pred.Value = strings.Join(ids, ",")
					}
					pred.Operator = schema.PredicateOperatorIn
				case common.OperatorEnum_IN:
					usernames := strings.Split(pred.Value, ",")
					res, err := r.Connections.UAC.GetUsers(ctx, &uac.GetUsers{
						Usernames: usernames,
					})
					if err != nil {
						r.Logger.Info("failed to get users", zap.Error(err))
						// TODO: This was done to match existing webapp behavior, but this will also match a user named "NULL" and should be removed
						// VR-5254
						pred.Value = "NULL"
					} else {
						ids := make([]string, len(res.GetUserInfos()))
						for i, user := range res.GetUserInfos() {
							ids[i] = user.GetVertaInfo().GetUserId()
						}
						pred.Value = strings.Join(ids, ",")
					}
				case common.OperatorEnum_EQ:
					fallthrough
				case common.OperatorEnum_NE:
					res, err := r.Connections.UAC.GetUser(ctx, &uac.GetUser{
						Username: pred.Value,
					})
					if err != nil {
						r.Logger.Info("failed to get users", zap.Error(err))
						// TODO: This was done to match existing webapp behavior, but this will also match a user named "NULL" and should be removed
						// VR-5254
						pred.Value = "NULL"
					} else {
						pred.Value = res.VertaInfo.GetUserId()
					}
				case common.OperatorEnum_LTE:
					fallthrough
				case common.OperatorEnum_LT:
					fallthrough
				case common.OperatorEnum_GTE:
					fallthrough
				case common.OperatorEnum_GT:
					fallthrough
				case common.OperatorEnum_NOT_CONTAIN:
					return nil, errors.UnknownOperator(ctx, pred.Operator.String())
				}
			}
		}
	}

	for _, pred := range stringPredicates {
		if pred != nil {
			op, ok := common.OperatorEnum_Operator_value[pred.Operator.String()]
			if !ok {
				return nil, errors.UnknownOperator(ctx, pred.Operator.String())
			}
			predicates = append(predicates, &common.KeyValueQuery{
				Key:      pred.Key,
				Operator: common.OperatorEnum_Operator(op),
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
			op, ok := common.OperatorEnum_Operator_value[pred.Operator.String()]
			if !ok {
				return nil, errors.UnknownOperator(ctx, pred.Operator.String())
			}
			predicates = append(predicates, &common.KeyValueQuery{
				Key:      pred.Key,
				Operator: common.OperatorEnum_Operator(op),
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
