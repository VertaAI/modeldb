package resolvers

import (
	"context"
	"fmt"
	"strconv"

	"github.com/VertaAI/modeldb/backend/graphql/internal/schema"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/dataloaders"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/errors"
	"github.com/VertaAI/modeldb/backend/graphql/internal/schema/pagination"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/common"
	pcommon "github.com/VertaAI/modeldb/protos/gen/go/protos/public/common"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/modeldb"
	"github.com/VertaAI/modeldb/protos/gen/go/protos/public/uac"
	"go.uber.org/zap"
	"google.golang.org/grpc"
)

type datasetResolver struct{ *Resolver }

func (r *datasetResolver) DateCreated(ctx context.Context, obj *modeldb.Dataset) (string, error) {
	return strconv.FormatUint(obj.GetTimeCreated(), 10), nil
}
func (r *datasetResolver) DateUpdated(ctx context.Context, obj *modeldb.Dataset) (string, error) {
	return strconv.FormatUint(obj.GetTimeUpdated(), 10), nil
}
func (r *datasetResolver) Visibility(ctx context.Context, obj *modeldb.Dataset) (schema.DatasetVisibility, error) {
	return schema.DatasetVisibility(obj.GetDatasetVisibility().String()), nil
}
func (r *datasetResolver) AllowedActions(ctx context.Context, obj *modeldb.Dataset) (*schema.AllowedActions, error) {
	if r.Connections.HasUac() {
		res, err := r.Connections.Authorization.GetSelfAllowedActionsBatch(ctx, &uac.GetSelfAllowedActionsBatch{
			Resources: &uac.Resources{
				Service:     uac.ServiceEnum_MODELDB_SERVICE,
				ResourceIds: []string{obj.GetId()},
				ResourceType: &uac.ResourceType{
					Resource: &uac.ResourceType_ModeldbServiceResourceType{
						ModeldbServiceResourceType: pcommon.ModelDBResourceEnum_REPOSITORY,
					},
				},
			},
		})
		if err != nil {
			r.Logger.Error("failed to get allowed actions", zap.Error(err))
			return nil, err
		}

		var ret schema.AllowedActions

		if actions, ok := res.GetActions()[obj.GetId()]; ok {
			for _, act := range actions.GetActions() {
				switch act.GetModeldbServiceAction() {
				case uac.ModelDBActionEnum_CREATE:
					ret.Create = true
				case uac.ModelDBActionEnum_DELETE:
					ret.Delete = true
				case uac.ModelDBActionEnum_UPDATE:
					ret.Update = true
				case uac.ModelDBActionEnum_DEPLOY:
					ret.Deploy = true
				}
			}
		}

		return &ret, nil
	}
	return &schema.AllowedActions{
		Create: true,
		Update: true,
		Delete: true,
	}, nil
}
func (r *datasetResolver) Attributes(ctx context.Context, obj *modeldb.Dataset) ([]schema.KeyValue, error) {
	res, err := keyValueSliceConverter(ctx, obj.GetAttributes())
	if err != nil {
		r.Logger.Error("Failed to slice keyvalues", zap.Error(err))
		return nil, err
	}
	return res, nil
}
func (r *datasetResolver) Owner(ctx context.Context, obj *modeldb.Dataset) (*uac.UserInfo, error) {
	return dataloaders.GetUserById(ctx, obj.GetOwner())
}
func (r *datasetResolver) Collaborators(ctx context.Context, obj *modeldb.Dataset) ([]schema.Collaborator, error) {
	if r.Connections.HasUac() {
		return getConvertedCollaborators(r.Resolver, ctx, obj.Id, func(ctx context.Context, in *uac.GetCollaborator, opts ...grpc.CallOption) (*uac.GetCollaborator_Response, error) {
			return r.Connections.Collaborator.GetRepositoryCollaborators(ctx, in, opts...)
		})
	}
	return []schema.Collaborator{}, nil
}
func (r *datasetResolver) AddTags(ctx context.Context, obj *modeldb.Dataset, tags []string) (*modeldb.Dataset, error) {
	if !isMutation(ctx) {
		r.Logger.Info(errors.UpdateOutsideMutation(ctx).Message)
		return nil, errors.UpdateOutsideMutation(ctx)
	}
	_, err := r.Connections.Dataset.AddDatasetTags(ctx, &modeldb.AddDatasetTags{
		Id:   obj.GetId(),
		Tags: tags,
	})
	if err != nil {
		r.Logger.Error("failed to add tags", zap.Error(err))
		return nil, err
	}

	return r.Resolver.Query().Dataset(ctx, obj.GetId())
}
func (r *datasetResolver) DeleteTags(ctx context.Context, obj *modeldb.Dataset, tags []string) (*modeldb.Dataset, error) {
	if !isMutation(ctx) {
		r.Logger.Info(errors.UpdateOutsideMutation(ctx).Message)
		return nil, errors.UpdateOutsideMutation(ctx)
	}
	_, err := r.Connections.Dataset.DeleteDatasetTags(ctx, &modeldb.DeleteDatasetTags{
		Id:   obj.GetId(),
		Tags: tags,
	})
	if err != nil {
		r.Logger.Error("failed to delete tags", zap.Error(err))
		return nil, err
	}

	return r.Resolver.Query().Dataset(ctx, obj.GetId())
}
func (r *datasetResolver) DatasetVersions(ctx context.Context, obj *modeldb.Dataset, query *schema.DatasetVersionsQuery) (*schema.DatasetVersions, error) {
	if query != nil {
		var pageQuery *schema.PaginationQuery
		var ids []string
		var predicates []*common.KeyValueQuery
		if query != nil {
			pageQuery = query.Pagination
			ids = query.Ids

			var err error
			predicates, err = r.Resolver.resolveMDBPredicates(ctx, query.StringPredicates, query.FloatPredicates)
			if err != nil {
				r.Logger.Error("failed to resolve predicates", zap.Error(err))
				return nil, err
			}
		}

		workspaceName, err := r.Resolver.resolveWorkspaceName(ctx, obj.WorkspaceId, obj.WorkspaceType)
		if err != nil {
			return nil, err
		}

		nextObj, err := pagination.NewNext(r.Logger, ctx, nil, pageQuery)
		if err != nil {
			return nil, err
		}
		r.Logger.Info(fmt.Sprintf("Workspace type: %s", obj.WorkspaceType))

		res, err := r.Connections.DatasetVersion.FindDatasetVersions(ctx, &modeldb.FindDatasetVersions{
			DatasetId:         obj.Id,
			DatasetVersionIds: ids,
			WorkspaceName:     *workspaceName,
			Predicates:        predicates,
			PageNumber:        int32(nextObj.PageNumber),
			PageLimit:         int32(nextObj.PageLimit),
		})
		if err != nil {
			r.Logger.Error("failed to get dataset versions", zap.Error(err))
			return nil, err
		}

		nextObj.ProcessResponse(int64(res.GetTotalRecords()))

		return &schema.DatasetVersions{
			DatasetVersions: res.GetDatasetVersions(),
			Total:           int(res.GetTotalRecords()),
		}, nil
	}
	res, err := r.Connections.DatasetVersion.GetAllDatasetVersionsByDatasetId(ctx, &modeldb.GetAllDatasetVersionsByDatasetId{
		DatasetId: obj.Id,
	})
	if err != nil {
		r.Logger.Error("failed to load dataset versions", zap.Error(err))
		return nil, err
	}
	return &schema.DatasetVersions{
		DatasetVersions: res.GetDatasetVersions(),
	}, nil
}

func (r *datasetResolver) ChangeDescription(ctx context.Context, obj *modeldb.Dataset, description string) (*modeldb.Dataset, error) {
	if !isMutation(ctx) {
		r.Logger.Info(errors.UpdateOutsideMutation(ctx).Message)
		return nil, errors.UpdateOutsideMutation(ctx)
	}

	_, err := r.Connections.Dataset.UpdateDatasetDescription(ctx, &modeldb.UpdateDatasetDescription{
		Description: description,
		Id:          obj.GetId(),
	})

	if err != nil {
		r.Logger.Error("failed to change dataset description", zap.Error(err))
		return nil, err
	}

	return r.Resolver.Query().Dataset(ctx, obj.GetId())
}

func (r *datasetResolver) Delete(ctx context.Context, obj *modeldb.Dataset) (bool, error) {
	if !isMutation(ctx) {
		r.Logger.Info(errors.DeleteOutsideMutation(ctx).Message)
		return false, errors.DeleteOutsideMutation(ctx)
	}

	_, err := r.Connections.Dataset.DeleteDataset(ctx, &modeldb.DeleteDataset{
		Id: obj.GetId(),
	})

	if err != nil {
		r.Logger.Error("failed to delete dataset", zap.Error(err))
		return false, err
	}

	return true, nil
}
